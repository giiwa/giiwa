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
package org.giiwa.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.locks.Lock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.GLog;
import org.giiwa.cache.TimingCache;
import org.giiwa.conf.Global;
import org.giiwa.dao.Helper.Cursor;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.task.Consumer;
import org.giiwa.task.Function;

/**
 * the DAO helper class, used to access database
 * 
 * @author joe
 *
 * @param <I> the type of primary key
 * @param <T> the Bean
 */
public final class BeanDAO<I, T extends Bean> {

	/** The log utility */
	protected static Log log = LogFactory.getLog(BeanDAO.class);

	Class<T> t;

	private BeanDAO(Class<T> t) {
		this.t = t;
	}

	public T loadBy(String sql) throws SQLException {
		W q = W.create();
		q.and(sql);
		return load(q);
	}

	/**
	 * load a Bean from the database
	 * 
	 * @param q the condition
	 * @return
	 * @throws SQLException
	 */
	public T load(W q) {
		try {
			return Helper.load(q, t);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public T load(W q, boolean trace) {
		try {
			return Helper.load(q, t);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public T load(String sql, Consumer<T> func) throws SQLException {
		W q = W.create();
		q.and(sql);
		return load(q, func);
	}

	/**
	 * atomic load the T and call back the func
	 * 
	 * @param q
	 * @param func
	 * @return
	 * @throws SQLException
	 */
	public T load(W q, Consumer<T> func) throws SQLException {

		Lock door = Global.getLock("data." + tableName());

		door.lock();
		try {
			T t = load(q);
			if (func != null && t != null) {
				func.accept(t);
			}
			return t;
		} finally {
			door.unlock();
		}
	}

	/**
	 * get the tablename of the Bean
	 * 
	 * @return
	 * @throws SQLException
	 */
	public String tableName() throws SQLException {
		return Helper.getTable(t);
	}

	/**
	 * load a Bean by key, it will load from cache first, if not exists then get
	 * from database
	 * 
	 * @param id the key of the Bean
	 * @return
	 * @throws SQLException
	 */
	public T load(I id) {
		try {
			T t1 = TimingCache.get(t, id);
			if (t1 == null) {
				t1 = Helper.load(id, t);
				TimingCache.set(t, id, t1);
			}
			return copy(t1);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public Beans<T> load(String sql, int s, int n) throws SQLException {
		W q = W.create();
		q.and(sql);
		return load(q, s, n);
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

		try {
			Beans<T> bs = Helper.load(tableName(), q, s, n, t);
			if (bs != null) {
				bs.q = q;
				bs.dao = this;
			}
			return bs;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return Beans.create();
	}

	/**
	 * atomic load the data and call back the func
	 * 
	 * @param q
	 * @param s
	 * @param n
	 * @param func
	 * @return
	 * @throws SQLException
	 */
	public Beans<T> load(W q, int s, int n, Consumer<Beans<T>> func) throws SQLException {

		Lock door = Global.getLock("data." + tableName());
		door.lock();
		try {
			Beans<T> bs = load(q, s, n);
			if (func != null && bs != null && !bs.isEmpty()) {
				func.accept(bs);
			}
			return bs;
		} finally {
			door.unlock();
		}
	}

	public boolean stream(W q, Function<T, Boolean> func) throws Exception {
		return stream(q, 0, func);
	}

	/**
	 * load Beans as stream, MUST close the stream after use
	 * 
	 * @param q conditions
	 * @param s the offset
	 * @param n the limit
	 * @return
	 */
	public boolean stream(W q, long offset, Function<T, Boolean> func) throws Exception {

		String table = tableName();

		Cursor<T> bs = Helper.stream(table, q, offset, t);
		if (bs != null) {
			try {
				while (bs.hasNext()) {

					T e = bs.next();

					if (log.isDebugEnabled())
						log.debug("e=" + e);

					if (q.access != null) {
						q.access.read("db", table, e);
					}
					if (!func.apply(e)) {
						return false;
					}

				}

				if (log.isDebugEnabled())
					log.debug("end of stream for [" + table + "]");

			} finally {
				X.close(bs);
			}
		} else {
			log.info("load null, q=" + q);
		}
		return true;
	}

	/**
	 * check exists the data?
	 * 
	 * @param q the condition
	 * @return true if exists, otherwise false
	 * @throws SQLException
	 */
	public boolean exists(W q) throws SQLException {
		return Helper.exists(tableName(), q);
	}

	public boolean exists2(W q) {
		try {
			return Helper.exists(tableName(), q);
		} catch (Exception e) {
			return false;
		}
	}

	public boolean exists(I id) throws SQLException {
		if (TimingCache.exists(t, id))
			return true;

		return Helper.exists(tableName(), W.create().and(X.ID, id));
	}

	public boolean exists2(I id) {
		if (TimingCache.exists(t, id))
			return true;

		try {
			return exists(id);
		} catch (Throwable e) {
			return false;
		}
	}

	/**
	 * update the data in database, and remove all data in cache
	 * 
	 * @param q the conditions
	 * @param v the value
	 * @return
	 * @throws SQLException
	 */
	public int update(W q, V v) {

		try {
			TimingCache.remove(t);
			return Helper.update(tableName(), q, v);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return 0;
	}

	/**
	 * update the data in database, and remove all data in cache
	 * 
	 * @param id the key
	 * @param v  the value
	 * @return
	 * @throws SQLException
	 */
	public int update(I id, V v) {
		try {
			return update(W.create().and(X.ID, id), v);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return 0;
	}

	/**
	 * insert a data into database
	 * 
	 * @param v the value
	 * @return
	 * @throws SQLException
	 */
	public int insert(V v) {
		try {
			TimingCache.remove(t);
			if (X.isEmpty(v.value(X.ID))) {
				log.error("v=" + v, new Exception("id missed in V"));
			}
			return Helper.insert(tableName(), v);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return 0;
	}

	/**
	 * delete a data in database, and remove all data in cache
	 * 
	 * @param id the key
	 * @return
	 * @throws SQLException
	 */
	public int delete(I id) {
		return delete(W.create().and(X.ID, id));
	}

	public long next() throws SQLException {
		String name = "table." + this.tableName() + ".id";
		return UID.next(name);

	}

	/**
	 * delete data in database, and remove all data in cache
	 * 
	 * @param q the condition
	 * @return
	 * @throws SQLException
	 */
	public int delete(W q) {
		try {
			TimingCache.remove(t);
			return Helper.delete(tableName(), q);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return 0;
	}

	/**
	 * count the matches row
	 * 
	 * @param q the condition
	 * @return
	 * @throws SQLException
	 */
	public long count(W q) {
		try {
			return Helper.count(tableName(), q);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return 0;
	}

	public long count(String name, W q) throws SQLException {
		return Helper.count(tableName(), name, q);
	}

	public long size() throws SQLException {
		return Helper.size(tableName());
	}

	/**
	 * summary the field
	 * 
	 * @param name the field name
	 * @param q    the condition
	 * @return
	 * @throws SQLException
	 */
	public <E> E sum(String name, W q) {
		try {
			return Helper.sum(tableName(), name, q);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * get the max data
	 * 
	 * @param name the field name
	 * @param q    the condition
	 * @return
	 * @throws SQLException
	 */
	public <E> E max(String name, W q) {
		try {
			return Helper.max(this.tableName(), name, q);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public <E> E median(String name, W q) {
		try {
			return Helper.median(this.tableName(), name, q);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * get the min data
	 * 
	 * @param name the field name
	 * @param q    the condition
	 * @return
	 * @throws SQLException
	 */
	public <E> E min(String name, W q) throws SQLException {
		return Helper.min(this.tableName(), name, q);
	}

	/**
	 * average the field
	 * 
	 * @param name the field name
	 * @param q    the condition
	 * @return
	 * @throws SQLException
	 */
	public <E> E avg(String name, W q) throws SQLException {
		return Helper.avg(tableName(), name, q);
	}

	/**
	 * get the distinct name
	 * 
	 * @param name the field name
	 * @param q    the condition
	 * @return
	 * @throws SQLException
	 */
	public List<?> distinct(String name, W q) {
		try {
			return Helper.distinct(tableName(), name, q);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * increase the filed and update the data
	 * 
	 * @param q    the condition
	 * @param name the field name
	 * @param n    the number
	 * @param v    the value
	 * @return
	 * @throws SQLException
	 */
	public int inc(W q, String name, int n, V v) {
		try {
			TimingCache.remove(t);
			return Helper.inc(tableName(), name, n, q, v);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return 0;
	}

	@SuppressWarnings("unchecked")
	public T copy(Bean src) {
		if (src == null)
			return null;

		try {
			Bean e = t.getDeclaredConstructor().newInstance();
			e.putAll(src.getAll());
			return (T) e;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * create a DAO object
	 * 
	 * @param <D> the Object
	 * @param <E> the subclass of bean
	 * @param t   the Class of Bean
	 * @return the BeanDAO
	 */
	public static <D, E extends Bean> BeanDAO<D, E> create(Class<E> t) {
		return new BeanDAO<D, E>(t);
	}

	/**
	 * create a new object
	 * 
	 * @return the T
	 */
	public T newInstance() {
		try {
			return t.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * cleanup the data by setting global setting
	 * 
	 * @throws SQLException
	 */
	public int cleanup() {
		try {
			int n = this.delete(W.create().and("created",
					System.currentTimeMillis() - X.ADAY * Global.getInt("glog.keep.days", 30), W.OP.lt));

			return n;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return 0;
	}

	/**
	 * cleanup the data by setting and invoke the func by each
	 * 
	 * @param func the consumer func
	 * @throws SQLException
	 */
	public void cleanup(Function<T, Boolean> func) throws SQLException {

		W q = W.create()
				.and("created", System.currentTimeMillis() - X.ADAY * Global.getInt("glog.keep.days", 30), W.OP.lt)
				.sort("created", 1);

		if (func != null) {
			try {
				this.stream(q, 0, t -> {
					return func.apply(t);
				});
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

		int n = this.delete(q);
		if (n > 0) {
			GLog.applog.info("dao", "cleanup", tableName() + " cleanup=" + n, null, null);
		}
	}

	/**
	 * generate the query object
	 * 
	 * @return
	 */
	public W query() {
		W q = W.create();
		q.dao(this);

		return q;
	}

	public List<JSON> count(W q, String[] group, int n) {
		try {
			return Helper.count(this.tableName(), q, group, n);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
//		return Helper.count(this.tableName(), q, group, n, this.dbName());
	}

	public List<JSON> count(W q, String name, String[] group, int n) {
		try {
			return Helper.count(this.tableName(), name, q, group, n);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
//		return Helper.count(this.tableName(), q, group, n, this.dbName());
	}

	public List<JSON> sum(W q, String name, String[] group) {
		try {
			return Helper.sum(this.tableName(), name, q, group);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public List<JSON> aggregate(W q, String[] name, String[] group) {
		try {
			return Helper.aggregate(this.tableName(), name, q, group);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public List<JSON> avg(W q, String name, String[] group) {

		try {
			return Helper.avg(this.tableName(), name, q, group);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;

	}

	public void optimize(W q) {
		try {
			Helper.optimize(this.tableName(), q);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public boolean tryLock() {

		try {
			Lock door = Global.getLock("dao/" + this.tableName());
			return door.tryLock();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return false;
	}

	public void unlock() {

		try {
			Lock door = Global.getLock("dao/" + this.tableName());
			door.unlock();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}

	public void lock() {

		try {
			Lock door = Global.getLock("dao/" + this.tableName());
			door.lock();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}

}
