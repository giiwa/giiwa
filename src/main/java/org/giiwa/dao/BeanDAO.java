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

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.GLog;
import org.giiwa.cache.TimingCache;
import org.giiwa.conf.Global;
import org.giiwa.dao.Helper.DBHelper;
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
	private DBHelper helper;

	private BeanDAO(Class<T> t, Function<Long, W> cleanfunc) {

		this.t = t;
		this.cleanupfunc = cleanfunc;
//		_ensureIndex();

	}

//	private void _ensureIndex() {
//		// ensureIndex
//		try {
//			String tablename = this.tableName();
//			for (String key : new String[] { "id" }) {
//				LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
//				m.put(key, 1);
//				Helper.createIndex(tablename, m, true);
//			}
//
//			for (String key : new String[] { "created", "updated" }) {
//				LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
//				m.put(key, 1);
//				Helper.createIndex(tablename, m, false);
//			}
//
//			for (String key : new String[] { "created", "updated" }) {
//				LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
//				m.put(key, -1);
//				Helper.createIndex(tablename, m, false);
//			}
//		} catch (Exception e) {
//			// ignore
////			log.error(e.getMessage(), e);
//		}
//
//	}

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
			_check(q);
			if (helper == null) {
				return Helper.load(q, t);
			} else {
				return helper.load(tableName(), q, t);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public T load(W q, boolean trace) {
		try {
			_check(q);
			if (helper == null) {
				return Helper.load(q, t);
			} else {
				helper.load(tableName(), q, t);
			}
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

	private String _tablename;

	/**
	 * get the tablename of the Bean
	 * 
	 * @return
	 * @throws SQLException
	 */
	public String tableName() throws SQLException {
		if (_tablename == null) {
			_tablename = Helper.getTable(t);
		}
		return _tablename;
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
		return load(id, true);
	}

	public T load(I id, boolean fromcache) {
		try {
			T t1 = fromcache ? TimingCache.get(t, id) : null;
			if (t1 == null) {
				if (helper == null) {
					t1 = Helper.load(id, t);
				} else {
					t1 = helper.load(tableName(), W.create().and(X.ID, id), t);
				}
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
			_check(q);
			Beans<T> bs = helper == null ? Helper.load(tableName(), q, s, n, t) : helper.load(tableName(), q, s, n, t);
			if (bs != null) {
				q.table = tableName();
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

	/**
	 * performance issue
	 * 
	 * @param q
	 * @param func
	 * @return
	 * @throws Exception
	 */
	@Deprecated
	public boolean stream(W q, Function<T, Boolean> func) throws Exception {
		return stream(q, 0, func);
	}

	/**
	 * load Beans as stream
	 * 
	 * @param q conditions
	 * @param s the offset
	 * @param n the limit
	 * @return
	 */
	@Deprecated
	public boolean stream(W q, long offset, Function<T, Boolean> func) throws Exception {

		_check(q);

		String table = tableName();

		if (helper != null) {
			return helper.stream(table, q, offset, func, t);
		} else {
			return Helper.primary.stream(table, q, offset, func, t);
		}

	}

	/**
	 * check exists the data?
	 * 
	 * @param q the condition
	 * @return true if exists, otherwise false
	 * @throws SQLException
	 */
	public boolean exists(W q) throws SQLException {
		_check(q);
		return helper == null ? Helper.exists(tableName(), q) : helper.exists(tableName(), q);
	}

	public boolean exists2(W q) {
		try {
			_check(q);
			return exists(q);
		} catch (Exception e) {
			return false;
		}
	}

	public boolean exists(I id) throws SQLException {
		if (TimingCache.exists(t, id))
			return true;

		return exists(W.create().and(X.ID, id));
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

			_check(v);

			return helper == null ? Helper.update(tableName(), q, v) : helper.updateTable(tableName(), q, v);
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

			_check(v);

			return helper == null ? Helper.insert(tableName(), v) : helper.insertTable(tableName(), v);
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
		try {
			String name = "table." + this.tableName() + ".id";
			return UID.next(name);
		} catch (Exception e) {
			throw new SQLException(e);
		}

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
			_check(q);
			return helper == null ? Helper.delete(tableName(), q) : helper.delete(tableName(), q);
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
			_check(q);
			return helper == null ? Helper.count(tableName(), q) : helper.count(tableName(), q);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return 0;
	}

	public long count(String name, W q) throws SQLException {
		_check(q);
		return helper == null ? Helper.count(tableName(), name, q) : helper.count(tableName(), q, name);
	}

	public long size() throws SQLException {
		return helper == null ? Helper.size(tableName()) : helper.size(tableName());
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
			_check(q);
			return helper == null ? Helper.sum(tableName(), name, q) : helper.sum(tableName(), q, name);
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
			_check(q);
			return helper == null ? Helper.max(this.tableName(), name, q) : helper.max(tableName(), q, name);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public <E> E median(String name, W q) {
		try {
			_check(q);
			return helper == null ? Helper.median(this.tableName(), name, q) : helper.median(tableName(), q, name);
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
		_check(q);
		return helper == null ? Helper.min(this.tableName(), name, q) : helper.min(tableName(), q, name);
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
		_check(q);
		return helper == null ? Helper.avg(tableName(), name, q) : helper.avg(tableName(), q, name);
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
			_check(q);
			return helper == null ? Helper.distinct(tableName(), name, q) : helper.distinct(tableName(), name, q);
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

			_check(q);
			_check(v);

			return helper == null ? Helper.inc(tableName(), name, n, q, v) : helper.inc(tableName(), q, name, n, v);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return 0;
	}

	public int inc(W q, JSON incvalue, V v) {
		try {
			TimingCache.remove(t);

			_check(q);
			_check(v);
			_check(incvalue);

			return helper == null ? Helper.inc(tableName(), incvalue, q, v) : helper.inc(tableName(), q, incvalue, v);
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
		return create(t, null);
	}

	/**
	 * create a DAO object
	 * 
	 * @param <D> the Object
	 * @param <E> the subclass of bean
	 * @param t   the Class of Bean
	 * @return the BeanDAO
	 */
	public static <D, E extends Bean> BeanDAO<D, E> create(Class<E> t, Function<Long, W> cleanfunc) {

		BeanDAO<D, E> dao = new BeanDAO<D, E>(t, cleanfunc);

		if (Helper.primary instanceof RDSHelper) {
			// RDBMS
			Table table = (Table) t.getAnnotation(Table.class);
			if (table != null && !X.isEmpty(table.name())) {
				dao.createTable(table.name(), table.memo(), JSON.create().append("distributed", table.distributed()?1:0));
			}
		} else {
			log.info("bean [" + t + "], using " + Helper.primary);
		}

		return dao;
	}

	public void setHelper(DBHelper helper) {
		this.helper = helper;
		if (this.helper != null && this.helper instanceof RDSHelper) {
			// RDBMS
			Table table = (Table) t.getAnnotation(Table.class);
			if (table != null && !X.isEmpty(table.name())) {
				this.createTable(table.name(), table.memo(), JSON.create().append("distributed", table.distributed()?1:0));
			}
		}
	}

	Function<Long, W> cleanupfunc;

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
			// check the op is running, if so, ignore, this may cost long time
			List<JSON> l1 = Helper.listOp();
			boolean exists = false;
			if (l1 != null) {
				for (JSON j1 : l1) {
					String op = j1.getString("op");
					if (X.isIn(op, "remove", "validate")) {
						String table = j1.getString("table");
						int i = table.lastIndexOf(".");
						if (i > 0) {
							table = table.substring(i + 1);
						}
						if (X.isSame(table, this.tableName())) {
							exists = true;
							break;
						}
					}
				}
			}
			if (!exists) {

				W q = null;
				if (cleanupfunc != null) {
					q = cleanupfunc.apply(Global.now() - X.ADAY * Global.getInt("glog.keep.days", 30));
				} else {
					q = W.create().and("created", Global.now() - X.ADAY * Global.getInt("glog.keep.days", 30), W.OP.lt);
				}

				this.optimize(q);

				int n = this.delete(q);
				return n;
			} else {
				log.warn("exists remove operation on table[" + this.tableName() + "], ignore!");
			}
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

		W q = W.create().and("created", Global.now() - X.ADAY * Global.getInt("glog.keep.days", 30), W.OP.lt)
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
			_check(q);
			return helper == null ? Helper.count(this.tableName(), q, group, n)
					: helper.count(tableName(), q, group, n);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
//		return Helper.count(this.tableName(), q, group, n, this.dbName());
	}

	public List<JSON> count(W q, String name, String[] group, int n) {
		try {
			_check(q);
			return helper == null ? Helper.count(this.tableName(), name, q, group, n)
					: helper.count(tableName(), q, name, group, n);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
//		return Helper.count(this.tableName(), q, group, n, this.dbName());
	}

	public List<JSON> sum(W q, String name, String[] group) {
		try {

			_check(q);

			return helper == null ? Helper.sum(this.tableName(), name, q, group)
					: helper.sum(tableName(), q, name, group);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public List<JSON> aggregate(W q, String[] name, String[] group) {
		try {
			_check(q);
			return helper == null ? Helper.aggregate(this.tableName(), name, q, group)
					: helper.aggregate(tableName(), name, q, group);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public List<JSON> avg(W q, String name, String[] group) {

		try {
			_check(q);
			return helper == null ? Helper.avg(this.tableName(), name, q, group)
					: helper.avg(tableName(), q, name, group);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;

	}

	public void optimize(W q) {
		try {
			_check(q);
			if (helper == null) {
				Helper.optimize(this.tableName(), q);
			} else {
				helper.getOptimizer().optimize(tableName(), q);
			}
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

	private Map<String, Field> _fields;

	private Map<String, Field> _check() {
		if (_fields == null) {
			try {
				Bean b = t.getDeclaredConstructor().newInstance();
				_fields = b.getFields();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return _fields;
	}

	public <E extends Bean> void createTable(String tablename, String memo, JSON prop) {

		try {

			Map<String, Field> st = _check();

			log.info("bean [" + t + "], fields=" + st.keySet());

			if (st != null && !st.isEmpty()) {
				List<JSON> l1 = JSON.createList();
				Set<String> keys = new HashSet<String>();
				for (String name : st.keySet()) {
					Field f1 = st.get(name);
					Column c1 = f1.getAnnotation(Column.class);
					if (c1 != null) {
						JSON j1 = JSON.create();
						j1.append("name", X.isEmpty(c1.name()) ? name : c1.name());
						if (keys.contains(j1.getString("name"))) {
							continue;
						}

						j1.append("display", X.isEmpty(c1.memo()) ? name : c1.memo());
						String type = f1.getType().getName();
						int i = type.lastIndexOf(".");
						if (i > 0) {
							type = type.substring(i + 1);
						}
						if (X.isIn(type, "int", "long", "integer", "short", "byte", "char")) {
							type = "long";
						} else if (X.isIn(type, "string")) {
							type = "text";
							int size = c1.size();
							if (size <= 0) {
								size = 512;
							}
							j1.append("size", size);
						} else if (X.isIn(type, "float", "double")) {
							type = "double";
							int size = c1.size();
							if (size <= 0) {
								size = 4;
							}
							j1.append("size", size);
						} else if (List.class.isAssignableFrom(f1.getType()) || f1.getType().isArray()) {
							type = "text";
							int size = c1.size();
							if (size <= 0) {
								size = 2048;
							}
							j1.append("size", size);
						}
						j1.append("type", type);
						if (c1.unique()) {
							j1.append("key", 1);
						}
						keys.add(j1.getString("name"));
						l1.add(j1);
					}

				}
				if (!l1.isEmpty()) {
					if (helper == null) {
						Helper.primary.createTable(tablename, memo, l1, prop);
					} else {
						helper.createTable(tablename, memo, l1, prop);
					}
				} else {
					log.info("bean [" + t + "], empty cols!");
				}
			}
		} catch (Exception e) {
			log.error("bean [" + t + "], error!", e);
		}
	}

	private void _check(W q) {

		if (q == null) {
			return;
		}

		Map<String, Field> st = _check();
		if (st == null) {
			return;
		}

		q.scan(e -> {
			Field f1 = st.get(e.name);
			if (f1 != null) {
				Column c1 = f1.getAnnotation(Column.class);
				if (c1 == null || X.isEmpty(c1.name()) || X.isSame(e.name, c1.name())) {
					return;
				}
				e.name = c1.name();
			}

		});

		// log.info("q=" + q);

	}

	private void _check(V v) {
		if (v == null) {
			return;
		}

		Map<String, Field> st = _check();
		String[] ss = v.names().toArray(new String[v.size()]);
		for (String name : ss) {
			Field f1 = st.get(name);
			if (f1 != null) {
				Column c1 = f1.getAnnotation(Column.class);
				if (c1 == null || X.isEmpty(c1.name()) || X.isSame(name, c1.name())) {
					continue;
				}
				Object o = v.value(name);
				v.remove(name);
				v.set(c1.name(), o);
			}
		}

	}

	private void _check(JSON j1) {
		if (j1 == null) {
			return;
		}

		Map<String, Field> st = _check();
		String[] ss = j1.keySet().toArray(new String[j1.size()]);
		for (String name : ss) {
			Field f1 = st.get(name);
			if (f1 != null) {
				Column c1 = f1.getAnnotation(Column.class);
				if (c1 == null || X.isEmpty(c1.name()) || X.isSame(name, c1.name())) {
					continue;
				}
				Object o = j1.get(name);
				j1.remove(name);
				j1.put(c1.name(), o);
			}
		}
	}

}
