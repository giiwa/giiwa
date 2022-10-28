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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.GLog;
import org.giiwa.dao.TimeStamp;
import org.giiwa.task.Task;

/**
 * A general pool class, that can be for database , or something else
 * 
 * @author joe
 *
 */
public class Pool<E> {

	static Log log = LogFactory.getLog(Pool.class);

	private ReentrantLock lock = new ReentrantLock();
	private Condition door = lock.newCondition();

	private List<E> idle = new ArrayList<E>();
	private int initial = 10;
	private int max = 10;
	private int created = 0;

	public long activetime = System.currentTimeMillis();

	private IPoolFactory<E> factory = null;

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
		p.initial = initial;
		p.max = max;
		p.factory = factory;
		p.factory.pool = p;

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
				try {
					lock.lock();

					created++;
					idle.add(t);

					door.signal();
				} finally {
					lock.unlock();
				}
			}
		}
	}

	/**
	 * release a object to the pool.
	 *
	 * @param t the t
	 */
	void release(E t) {

		activetime = System.currentTimeMillis();

		if (t == null)
			return;

		_monitor.remove(t);

		if (!factory.check(t)) {
			log.info("release a bad one");
			created--;
		} else {

			try {
				lock.lock();

				if (idle.size() >= max) {
					log.warn("error, the size[" + idle.size() + "] of exceed max[" + max + "], close this one");
					factory.destroy0(t);
					return;
				} else if (!idle.contains(t)) {
					idle.add(t);
				}
				door.signal();
			} finally {
				lock.unlock();
			}
		}
	}

	/**
	 * destroy the pool, and destroy all the object in the pool.
	 */
	public void destroy() {
		synchronized (idle) {
			for (int i = idle.size() - 1; i >= 0; i--) {
				factory.destroy0(idle.get(i));
			}
			idle.clear();
		}
	}

	private Map<Object, Exception> _monitor = new HashMap<Object, Exception>();

	/**
	 * get a object from the pool, if meet the max, then wait till timeout.
	 *
	 * @param timeout the timeout
	 * @return the e
	 * @throws Exception
	 */
	public E get(long timeout) throws Exception {

		activetime = System.currentTimeMillis();

		TimeStamp t = TimeStamp.create();

		long t1 = timeout;

		try {
			lock.lock();

//				log.info("idle=" + idle.size());

			while (t1 > 0) {
				if (!idle.isEmpty()) {
					E e = idle.remove(0);
					if (factory.check(e)) {
						_monitor.put(e, new Exception(Thread.currentThread().getName()));
						return e;
					} else {
						created--;
					}
				} else {
					t1 = timeout - t.pastms();
					if (t1 > 0) {

//							log.debug("t1=" + t1 + ", max=" + max + ", created=" + created);
//
						//
						if (created < max) {

							E e = factory.create0();
							if (e != null && factory.check(e)) {
								created++;
								_monitor.put(e, new Exception(Thread.currentThread().getName()));
								return e;
							} else {
								throw new Exception("create E failed, e=" + e + ", factory=" + factory);
							}
						}

						door.awaitNanos(TimeUnit.MILLISECONDS.toNanos(t1));
					}
				}
			}
		} finally {
			lock.unlock();
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
		return "Pool [idle=" + idle + ", initial=" + initial + ", created=" + created + ", max=" + max + ", monitor="
				+ _monitor + "]";
	}

}
