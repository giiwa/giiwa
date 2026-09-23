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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.GLog;
import org.giiwa.conf.Global;
import org.giiwa.dao.TimeStamp;
import org.giiwa.task.Task;

/**
 * A general pool class, that can be for database , or something else
 * 
 * @author joe
 *
 */
public final class Pool<E> {

	private static Log log = LogFactory.getLog(Pool.class);

	// 空闲的
	private Set<E> idle = new LinkedHashSet<E>();

	// 已经外借的
	private Set<E> outside = new HashSet<E>();

	private int initial = 10;
	private int max = 10;
	private int created = 0;

	// 创建链接的时长
	private Map<E, Long> ages = new HashMap<E, Long>();

	// 连接最大生命周期
	private long _age = -1;// -1: 无限期

	public long activetime = Global.now();

	private IPoolFactory<E> factory = null;

	private String name;

	/**
	 * 创建链接池
	 *
	 * @param <E>     链接类型
	 * @param initial 初始化链接数
	 * @param max     最大链接数
	 * @param factory 链接创建方法
	 * @return the pool
	 */
	public static <E> Pool<E> create(int initial, int max, IPoolFactory<E> factory) {

		Pool<E> p = new Pool<E>();
		p.name = factory.toString();
		p.initial = Math.min(initial, max);
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
				synchronized (this) {
					created++;
					idle.add(t);
				}
			}
		}
		synchronized (this) {
			this.notify();
		}
	}

	/**
	 * release a object to the pool.
	 *
	 * @param t the t
	 */
	void release(E e) {

		activetime = Global.now();

		if (log.isDebugEnabled()) {
			log.debug("release t=" + e);
		}

		if (e == null)
			return;

		synchronized (this) {
			// 防止重复release
			if (outside.remove(e)) {
				Long createTs = ages.get(e);
				boolean validAge = _age <= 0 || (createTs != null && System.currentTimeMillis() - createTs < _age);

				if (factory.check0(e) && validAge) {
					idle.add(e);
				} else {
					factory.destroy0(e);
					ages.remove(e);
					created--;
				}
			}
		}
	}

	/**
	 * destroy the pool, and destroy all the object in the pool.
	 */
	public void destroy() {
		synchronized (this) {
			for (E e : idle) {
				factory.destroy0(e);
			}
			outside.clear();
			idle.clear();
			created = 0;
			ages.clear();
		}
	}

	/**
	 * 设置每个链接的最大age
	 * 
	 * @param age - 毫秒
	 * @return
	 */
	public Pool<E> age(long age) {
		this._age = age;
		return this;
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

				if (!idle.isEmpty()) {
					E e = idle.iterator().next();
					idle.remove(e);

					Long createTs = ages.get(e);
					boolean validAge = _age <= 0 || (createTs != null && System.currentTimeMillis() - createTs < _age);

					if (factory.check0(e) && validAge) {
						// 链接是正常的，age小于设定的
						_outside(e);
						return e;
					} else {
						if (log.isInfoEnabled()) {
							log.info("got bad one, destory, [" + name + "], max=" + max);
						}
						factory.destroy0(e);
						ages.remove(e);
						created--;
					}
				} else {
					if (created < max) {
						E e = factory.create0();
						if (e != null) {
							created++;
							_outside(e);
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
							if (!_check_outside()) {
								try {
									this.wait(t1);
								} catch (InterruptedException ie) {
									Thread.currentThread().interrupt();
									throw new Exception("pool get thread interrupted", ie);
								}
							}
						}
					}
				}

				// 检查outside， 是否存在过期连接
				_check_outside();

			}
		}

		log.warn("pool.get failed, " + name + ", idle=" + idle.size() + ", created=" + created + ", max=" + max
				+ ", outsideSize=" + outside.size());

		return null;
	}

	private boolean _check_outside() {

		if (_age <= 0) {
			return false;
		}
		boolean hasClean = false;

		synchronized (this) {
			// 拷贝借出集合快照，只处理占用中的连接
			Set<E> snapshot = new HashSet<>(outside);
			long now = System.currentTimeMillis();
			for (E e : snapshot) {
				Long createTs = ages.get(e);
				if (createTs == null)
					continue;
				if (now - createTs > _age) {
					hasClean = true;
					log.warn("force destroy overtime leased connection: " + e);
					outside.remove(e);
					factory.destroy0(e);
					ages.remove(e);
					created--;
				}
			}
		}
		return hasClean;

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

	public int available() {
		return idle.size();
	}

	@Override
	public String toString() {
		return "Pool [" + name + "=(initial=" + initial + ", created=" + created + ", max=" + max + ")]";
	}

	private void _outside(E e) {
		synchronized (this) {
			if (outside.contains(e)) {
				log.error("outside error, already in outside=" + outside);
			}
			if (outside.size() >= max) {
				log.warn("outside put, e=" + e + ", created=" + created + ", max=" + max, new Exception());
			}
			outside.add(e);
			ages.put(e, System.currentTimeMillis());
		}
	}

}
