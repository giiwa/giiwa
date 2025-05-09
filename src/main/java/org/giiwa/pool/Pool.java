/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.pool;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.GLog;
import org.giiwa.conf.Global;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.task.Task;

/**
 * A general pool class, that can be for database , or something else
 * 
 * @author joe
 *
 */
public class Pool<E> {

	static Log log = LogFactory.getLog(Pool.class);

//	private ReentrantLock lock = new ReentrantLock();
//	private Condition door = lock.newCondition();

	private List<E> idle = new ArrayList<E>();
	private Map<E, _O> outside = new HashMap<E, _O>();

	private int initial = 10;
	private int max = 10;
	private int created = 0;
	private long MAX_TIME_OUTSIDE = X.AMINUTE;

	public long activetime = Global.now();

	private IPoolFactory<E> factory = null;

	private String name;

	/**
	 * create a pool by initial, max and factory.
	 *
	 * @param <E>     the element type
	 * @param initial the initial
	 * @param max     the max
	 * @param factory the factory
	 * @return the pool
	 */
	public static <E> Pool<E> create(int initial, int max, IPoolFactory<E> factory) {

		Pool<E> p = new Pool<E>();
		p.name = factory.toString();
		p.initial = Math.min(initial, max);
		p.max = max;
		p.factory = factory;
		p.factory.pool = p;

		// log.info("create RDSHelper, max=" + max + ", factory=" + factory, new
		// Exception("trace only"));

		Task.schedule(t -> {
			try {
				p.init();
			} catch (Exception e) {
				GLog.applog.error("pool", "init", e.getMessage(), e);
			}
		}, 0);

		return p;

	}

	private void init() throws Exception {
		for (int i = 0; i < initial; i++) {
			E t = factory.create0();
			if (t != null) {
				synchronized (this) {
					created++;
					idle.add(t);
				}
			}
		}
		synchronized (this) {
			this.notifyAll();
		}
	}

	/**
	 * release a object to the pool.
	 *
	 * @param t the t
	 */
	void release(E t) {

		activetime = Global.now();

		if (log.isDebugEnabled()) {
			log.debug("release t=" + t);
		}

		if (t == null)
			return;

		synchronized (this) {
			long otime = _remove(t);
			if (otime > -1) {
				if (otime > MAX_TIME_OUTSIDE || !factory.check0(t)) {
					factory.destroy0(t);
					log.warn("release a bad one, [" + t + "]");
					created--;
				} else {
					if (outside.size() + idle.size() >= max) {
						log.warn(
								"error, the size[" + idle.size() + "] of exceed max[" + max + "], close this one=" + t);
						factory.destroy0(t);
						return;
					} else if (!idle.contains(t)) {
						idle.add(t);
					}
				}
			}
			// still using by
		}
	}

	/**
	 * destroy the pool, and destroy all the object in the pool.
	 */
	public void destroy() {
		synchronized (this) {
			for (int i = idle.size() - 1; i >= 0; i--) {
				factory.destroy0(idle.get(i));
			}
			outside.clear();
			idle.clear();
			created = 0;
		}
	}

	/**
	 * get a object from the pool, if meet the max, then wait till timeout.
	 *
	 * @param timeout the timeout
	 * @return the e
	 * @throws Exception
	 */
	public E get(long timeout) throws Exception {

		activetime = Global.now();

		TimeStamp t = TimeStamp.create();

		long t1 = timeout;

		synchronized (this) {

			while (t1 > 0) {
				E e = _outside();
				if (e != null) {
					return e;
				}

				if (!idle.isEmpty()) {
					e = idle.remove(0);
					if (factory.check0(e)) {
						_add(e);
						return e;
					} else {
						if (log.isInfoEnabled()) {
							log.info("got bad one, destory, [" + name + "], max=" + max);
						}
						factory.destroy0(e);
						created--;
					}
				} else {
					if (created < max) {
						e = factory.create0();
						if (e != null) {
							created++;
							_add(e);
							return e;
						} else {
							throw new Exception("create E failed, [" + name + "], e=" + e + ", factory=" + factory);
						}
					} else {
						t1 = timeout - t.pastms();
						if (t1 > 0) {
							if (log.isDebugEnabled()) {
								log.debug("waiting for get, [" + name + "], max=" + max);
							}
							this.wait(t1);
						}
					}
				}
			}
		}

		log.warn("pool.get failed, " + name + ", idle=" + idle.size() + ", created=" + created + ", max=" + max
				+ ", outside=" + outside);

		return null;
	}

	private E _outside() {
		synchronized (this) {
			if (outside.isEmpty()) {
				if (created != idle.size()) {
					log.warn("outside error, empty, idle=" + idle.size() + ", created=" + created);
					created = idle.size();
				}
				return null;
			}

			// 不同线程不能共享，DM数据库连接共享会卡顿
			Thread th = Thread.currentThread();
			for (_O e : outside.values()) {
				if (th.getId() == e.th.getId()) {
					return e.get();
				}
			}
		}
		return null;
	}

	/**
	 * the pool factory interface using to create E object in pool
	 * 
	 * @author wujun
	 *
	 * @param <E> the Object
	 */
	public static abstract class IPoolFactory<E> {

		/**
		 * create a object.
		 *
		 * @return the e
		 */
		public abstract E create() throws Exception;

		/**
		 * check the E is available
		 * 
		 * @param t
		 * @return true if ok
		 */
		public abstract boolean check(E t);

		/**
		 * destroy a object.
		 *
		 * @param t the t
		 */
		public abstract void destroy(E t);

		@SuppressWarnings("unchecked")
		private boolean check0(E t) {
			WeakReference<Delegator> o = Delegator._cache.get(t);
			if (o != null && o.get() != null) {
				return check((E) o.get().obj);
			}
			return false;
		}

		@SuppressWarnings("unchecked")
		private void destroy0(E t) {
			WeakReference<Delegator> o = Delegator._cache.get(t);
			if (o != null && o.get() != null) {
				destroy((E) o.get().obj);
			}

			Delegator._cache.remove(t);
		}

		private Pool<E> pool;

		@SuppressWarnings("unchecked")
		private E create0() throws Exception {

			E e = create();
			Object e1 = Delegator.create(e, pool);

			return (E) e1;
		}

	}

	public int max() {
		return max;
	}

	public int avaliable() {
		return idle.size();
	}

	@Override
	public String toString() {
		return "Pool [" + name + "=(initial=" + initial + ", created=" + created + ", max=" + max + ")]";
	}

	private void _add(E e) {
		synchronized (this) {
			if (outside.containsKey(e)) {
				log.error("outside error, already in outside=" + outside);
			}
			if (outside.size() >= max) {
				log.warn("outside put, e=" + e + ", created=" + created + ", max=" + max, new Exception());
			}
			outside.put(e, new _O(e));
		}
	}

	private long _remove(E e) {
		synchronized (this) {
			_O o = outside.get(e);
			if (o == null) {
				log.warn("outside error, o=null, e=" + e.toString());
				outside.remove(e);
				// 这个连接失控了， 关掉
				return Long.MAX_VALUE;
			} else if (o.release()) {
				outside.remove(e);
				if (log.isDebugEnabled()) {
					log.debug("outside ok, removed, e=" + e.toString() + ", outside=" + outside + ", idle=" + idle
							+ ", created=" + created + ", max=" + max);
				}
				return o.time.pastms();
			} else {
				if (log.isDebugEnabled()) {
					log.debug("outside refer=" + o.refer + ", e=" + e.toString());
				}
				return -1;
			}
		}
	}

	class _O {

		TimeStamp time = TimeStamp.create();
		Thread th;
		E e;
		int refer = 1;

		_O(E e) {
			this.e = e;
			this.refer = 1;
			th = Thread.currentThread();
		}

		public synchronized E get() {
			refer++;
			return e;
		}

		public synchronized boolean release() {
			refer--;
			return refer == 0;
		}

		@Override
		public String toString() {
			return "_O[cost = " + time.past() + ", refer=" + refer + ", thread=" + th.getName() + "]";
		}

	}

}
