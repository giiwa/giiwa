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
package org.giiwa.core.bean;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.conf.Global;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.GLog;
import org.giiwa.framework.bean.User;

/**
 * the DAO helper class, used to access database
 * 
 * @author joe
 *
 * @param <I> the type of primary key
 * @param <T> the Bean
 */
public class BeanDAO<I, T extends Bean> {

	/**
	 * default 5 seconds
	 */
	public static final long EXPIREDTIME = 5000; // 5 seconds

	/**
	 * default 10 items
	 */
	public static final long MAXCACHENUMBER = 10; // 10

	/** The log utility */
	protected static Log log = LogFactory.getLog(BeanDAO.class);

	Class<T> t;

	private BeanDAO(Class<T> t) {
		this.t = t;
	}

	/**
	 * load a Bean from the database
	 * 
	 * @param q the condition
	 * @return
	 */
	public T load(W q) {
		return Helper.load(q, t);
	}

	/**
	 * get the tablename of the Bean
	 * 
	 * @return
	 */
	public String tableName() {
		return Helper.getTable(t);
	}

	/**
	 * load a Bean only fields
	 * 
	 * @param fields the fields
	 * @param q      the condition
	 * @return
	 */
	public T load(String[] fields, W q) {
		return Helper.load(fields, q, t);
	}

	private Cache _cache = new Cache();

	/**
	 * load a Bean by key, it will load from cache first, if not exists then get
	 * from database
	 * 
	 * @param id the key of the Bean
	 * @return
	 */
	public T load(I id) {
		return _cache.get(id);
		// return Helper.load(id, t);
	}

	/**
	 * load Beans only the fields
	 * 
	 * @param fields the fields
	 * @param q      the conditions
	 * @param s      the offset
	 * @param n      the limit
	 * @return
	 */
	public Beans<T> load(String[] fields, W q, int s, int n) {
		Beans<T> bs = Helper.load(fields, q, s, n, t);
		if (bs != null) {
			bs.q = q;
			bs.dao = this;
		}
		return bs;
	}

	/**
	 * load Beans from database
	 * 
	 * @param q the conditions
	 * @param s the offset
	 * @param n the limit
	 * @return
	 */
	public Beans<T> load(W q, int s, int n) {
		Beans<T> bs = Helper.load(q, s, n, t);
		if (bs != null) {
			bs.q = q;
			bs.dao = this;
		}
		return bs;
	}

	/**
	 * load Beans as stream, MUST close the stream after use
	 * 
	 * @param q conditions
	 * @param s the offset
	 * @param n the limit
	 * @return
	 */
	public BeanStream<T> stream(W q, int s, int n) {
		BeanStream<T> bs = Helper.stream(q, s, n, t);
		return bs;
	}

	/**
	 * check exists the data?
	 * 
	 * @param q the condition
	 * @return true if exists, otherwise false
	 * @throws SQLException
	 */
	public boolean exists(W q) throws SQLException {
		return Helper.exists(q, t);
	}

	public boolean exists(I id) throws SQLException {
		if (_cache.exists(id))
			return true;

		return Helper.exists(id, t);
	}

	/**
	 * update the data in database, and remove all data in cache
	 * 
	 * @param q the conditions
	 * @param v the value
	 * @return
	 */
	public int update(W q, V v) {
		_cache.remove();
		return Helper.update(q, v, t);
	}

	/**
	 * update the data in database, and remove all data in cache
	 * 
	 * @param id the key
	 * @param v  the value
	 * @return
	 */
	public int update(I id, V v) {
		_cache.remove();
		return Helper.update(id, v, t);
	}

	/**
	 * insert a data into database
	 * 
	 * @param v the value
	 * @return
	 */
	public int insert(V v) {
		_cache.remove();
		if (X.isEmpty(v.value(X.ID))) {
			log.error("v=" + v, new Exception("id missed in V"));
		}
		return Helper.insert(v, t);
	}

	/**
	 * delete a data in database, and remove all data in cache
	 * 
	 * @param id the key
	 * @return
	 */
	public int delete(I id) {
		_cache.remove();
		return Helper.delete(id, t);
	}

	/**
	 * delete data in database, and remove all data in cache
	 * 
	 * @param q the condition
	 * @return
	 */
	public int delete(W q) {
		_cache.remove();
		return Helper.delete(q, t);
	}

	/**
	 * count the matches row
	 * 
	 * @param q the condition
	 * @return
	 */
	public long count(W q) {
		return Helper.count(q, t);
	}

	/**
	 * summary the field
	 * 
	 * @param name the field name
	 * @param q    the condition
	 * @return
	 */
	public <E> E sum(String name, W q) {
		return Helper.sum(q, name, t);
	}

	/**
	 * get the max data
	 * 
	 * @param name the field name
	 * @param q    the condition
	 * @return
	 */
	public <E> E max(String name, W q) {
		return Helper.max(q, name, t);
	}

	/**
	 * get the min data
	 * 
	 * @param name the field name
	 * @param q    the condition
	 * @return
	 */
	public <E> E min(String name, W q) {
		return Helper.min(q, name, t);
	}

	/**
	 * average the field
	 * 
	 * @param name the field name
	 * @param q    the condition
	 * @return
	 */
	public <E> E avg(String name, W q) {
		return Helper.avg(q, name, t);
	}

	/**
	 * get the distinct name
	 * 
	 * @param name the field name
	 * @param q    the condition
	 * @return
	 */
	public List<?> distinct(String name, W q) {
		return Helper.distinct(name, q, t);
	}

	/**
	 * increase the filed and update the data
	 * 
	 * @param q    the condition
	 * @param name the field name
	 * @param n    the number
	 * @param v    the value
	 * @return
	 */
	public int inc(W q, String name, int n, V v) {
		_cache.remove();
		return Helper.inc(q, name, n, v, t);
	}

	/**
	 * copy the source Bean and create a new one, no store in database
	 * 
	 * @param src the source Bean
	 * @return
	 */
	public T copy(Bean src) {
		try {
			T b = t.newInstance();
			Map<String, Object> m = src.getAll();
			for (String name : m.keySet()) {
				b.set(name, m.get(name));
			}
			return b;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public static void main(String[] args) {
		System.out.println(User.dao.t);
	}

	/**
	 * create a DAO object
	 * 
	 * @param t the Class of Bean
	 * @return
	 */
	public static <D, E extends Bean> BeanDAO<D, E> create(Class<E> t) {
		return new BeanDAO<D, E>(t);
	}

	private class Cache {

		private Map<I, O> m = new HashMap<I, O>();

		@SuppressWarnings("unchecked")
		void set(I id, T t) {
			while (m.size() > MAXCACHENUMBER) {
				// remove eldest

				List<O> l1 = new ArrayList<O>(m.values());
				Collections.sort(l1);
				m.clear();
				for (int i = 0; i < MAXCACHENUMBER / 2; i++) {
					O o = l1.get(i);
					m.put((I) o.id, o);
				}
			}
			if (t == null) {
				m.remove(id);
			} else {
				m.put(id, new O(id, t));
			}
		}

		public boolean exists(I id) {
			return m.containsKey(id);
		}

		public void remove() {
			m.clear();
		}

		T get(I id) {
			O o = m.get(id);
			if (o != null && System.currentTimeMillis() - o.time < EXPIREDTIME) {
				o.time = System.currentTimeMillis();
				return (T) copy(o.t);
			}

			T t1 = Helper.load(id, t);
			if (t1 != null) {
				set(id, t1);
			}
			return t1;
		}

	}

	private class O implements Comparable<O> {
		long time = System.currentTimeMillis();
		T t;
		Object id;

		O(Object id, T t) {
			this.t = t;
			this.id = id;
		}

		@Override
		public int compareTo(O o) {
			return time > o.time ? -1 : 1;
		}

	}

	/**
	 * create a new object
	 * 
	 * @return
	 */
	public T newInstance() {
		try {
			return t.newInstance();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * cleanup the data by setting global setting
	 */
	public void cleanup() {
		int n = this.delete(W.create("created",
				System.currentTimeMillis() - X.ADAY * Global.getInt("glog.keep.days", 30), W.OP.lt));
		if (n > 0) {
			GLog.applog.info("dao", "cleanup", tableName() + " cleanup=" + n, null, null);
		}
	}

	/**
	 * cleanup the data by setting and invoke the func by each
	 * 
	 * @param func the consumer func
	 */
	public void cleanup(Consumer<T> func) {

		W q = W.create("created", System.currentTimeMillis() - X.ADAY * Global.getInt("glog.keep.days", 30), W.OP.lt)
				.sort("created", 1);

		if (func != null) {
			Task.mapreduce(this.stream(q, -1, -1), t -> {
				func.accept(t);
				return null;
			});
		}

		// int s = 0;
		// Beans<T> bs = this.load(q, s, 100);
		// while (bs != null && !bs.isEmpty()) {
		// for (T t : bs) {
		// func.accept(t);
		// }
		// s += bs.size();
		// bs = this.load(q, s, 100);
		// }

		int n = this.delete(q);
		if (n > 0) {
			GLog.applog.info("dao", "cleanup", tableName() + " cleanup=" + n, null, null);
		}
	}

	public W query() {
		W q = W.create();
		q.dao(this);

		return q;
	}

}
