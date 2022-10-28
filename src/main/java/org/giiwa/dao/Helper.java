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

import java.io.Closeable;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.regex.Pattern;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Data;
import org.giiwa.conf.Global;
import org.giiwa.json.JSON;
import org.giiwa.misc.StringFinder;
import org.giiwa.task.Consumer;
import org.giiwa.task.Function;
import org.giiwa.web.Controller;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;
import org.openjdk.nashorn.internal.runtime.Undefined;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

/**
 * The {@code Helper} Class is utility class for all database operation.
 * 
 */
public final class Helper implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** The log utility. */
	protected static Log log = LogFactory.getLog(Helper.class);

	public static Optimizer monitor = null;

	/**
	 * the primary database when there are multiple databases
	 */
	public static DBHelper primary = null;

	/** The conf. */
	protected static Configuration conf;

	/**
	 * initialize the Bean with the configuration.
	 * 
	 * @deprecated
	 * @param conf the conf
	 */
	public static void init(Configuration conf) {

		/**
		 * initialize the DB connections pool
		 */
		if (conf == null) {
			return;
		}

		log.warn("DBHelper init ...");

		Helper.conf = conf;

		RDB.init();

		if (primary == null) {

			if (MongoHelper.inst.isConfigured()) {
				primary = MongoHelper.inst;
			} else if (RDSHelper.inst.isConfigured()) {
				primary = RDSHelper.inst;
			}

			log.warn("db.primary missed, auto choose one, helper=" + primary);

		} else {
			log.info("db.primary=" + primary);
		}

	}

	public static boolean init2(Configuration conf) {

		/**
		 * initialize the DB connections pool
		 */
		if (conf == null) {
			return false;
		}

		log.warn("DBHelper init2 ...");

		Helper.conf = conf;

		String url = conf.getString("db.url", X.EMPTY);
		if (X.isEmpty(url)) {
			log.error("db.url not congiured!");
			return false;
		}

		try {
			if (url.startsWith("mongodb://")) {

				String user = conf.getString("db.user", X.EMPTY);
				String passwd = conf.getString("db.passwd", X.EMPTY);
				int conns = conf.getInt("db.conns", 50);
				int timeout = conf.getInt("db.timeout", 30000);

				primary = MongoHelper.create(url, user, passwd, conns, timeout);
				MongoHelper.inst = (MongoHelper) primary;

			} else {

				primary = RDSHelper.create(null);
				RDSHelper.inst = (RDSHelper) primary;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return primary != null;

	}

	/**
	 * delete the data by query
	 * 
	 * @param table
	 * @param q
	 * @return
	 */
	public static int delete(String table, W q) {

		TimeStamp t1 = TimeStamp.create();
		try {
			if (table != null) {

				if (monitor != null) {
					monitor.query(table, q);
				}

				return primary.delete(table, q);

			}
			return 0;
		} finally {
			write.add(t1.pastms());

		}
	}

	public static void repair(String table) {
		primary.repair(table);
	}

	/**
	 * exists
	 * 
	 * @param table
	 * @param q
	 * @return
	 * @throws SQLException
	 */
	public static boolean exists(String table, W q) throws SQLException {

		TimeStamp t1 = TimeStamp.create();
		try {
			if (table != null) {

				if (monitor != null) {
					monitor.query(table, q);
				}

				return primary.exists(table, q);
			}
		} finally {
			read.add(t1.pastms());

		}
		throw new SQLException("no db configured, please configure the {giiwa}/giiwa.properites");
	}

	public static boolean exists2(String table, W q) {

		TimeStamp t1 = TimeStamp.create();
		try {
			if (table != null) {

				if (monitor != null) {
					monitor.query(table, q);
				}

				return primary.exists(table, q);
			}
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		} finally {
			read.add(t1.pastms());

		}
		return false;
	}

	/**
	 * Values in SQL, used to insert or update data both RDS and Mongo<br>
	 * .
	 */
	public static final class V implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		protected final static Object ignore = new Object();

		/** The list. */
		public Map<String, Object> m = new LinkedHashMap<String, Object>();

		/**
		 * convert the V to json and return
		 * 
		 * @return
		 */
		public JSON json() {
			return JSON.fromObject(m);
		}

		/**
		 * copy the request parameters to V
		 * 
		 * @param m     the model
		 * @param names the names string
		 * @return the V
		 */
		public V copy(Controller m, String... names) {
			if (X.isEmpty(names)) {
				// copy all
				for (String name : m.names()) {
					this.set(name, m.getString(name));
				}
			} else {
				for (String name : names) {
					this.set(name, m.getString(name));
				}
			}
			return this;
		}

		/**
		 * copy the request parameters to V
		 * 
		 * @param m     the model
		 * @param names the names string
		 */
		public V copyInt(Controller m, String... names) {
			if (!X.isEmpty(names)) {
				for (String name : names) {
					this.set(name, m.getInt(name));
				}
			}
			return this;
		}

		/**
		 * From json.
		 *
		 * @param j the j
		 * @return the v
		 */
		public static V fromJSON(JSON j) {
			return V.create().copy(j);
		}

		/**
		 * get the names.
		 *
		 * @return Collection
		 */
		public Set<String> names() {
			Set<String> s1 = new HashSet<String>(m.keySet());
			for (String s : s1.toArray(new String[s1.size()])) {
				if (m.get(s) == ignore) {
					s1.remove(s);
				}
			}
			return s1;
		}

		/**
		 * get the size of the values.
		 *
		 * @return the int
		 */
		public int size() {
			return m.size();
		}

		public boolean isEmpty() {
			return m.isEmpty();
		}

		/**
		 * To string.
		 *
		 * @return the string
		 */
		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object.toString()
		 */
		@Override
		public String toString() {
			return m.toString();
		}

		private V() {
		}

		/**
		 * Creates a V and set the init name=value.
		 *
		 * @param name the name
		 * @param v    the value
		 * @return the v
		 */
		public static V create(String name, Object v) {
			if (name != null && v != null) {
				return new V().set(name, v);
			} else {
				return new V();
			}
		}

		/**
		 * Sets the value if not exists, ignored if name exists. <br>
		 * 
		 * @param name the name
		 * @param v    the value
		 * @return the v
		 */
		public V set(String name, Object v) {
			return append(name, v);
		}

		/**
		 * same as set(String name, Object v) <br>
		 * Sets the value if not exists, ignored if name exists.
		 * 
		 * @param name the name
		 * @param v    the value object
		 * @return the V
		 */
		public V put(String name, Object v) {
			return set(name, v);
		}

		/**
		 * same as set(String name, Object v) <br>
		 * Sets the value if not exists, ignored if name exists.
		 * 
		 * @param name the name
		 * @param v    the value object
		 * @return the V
		 */
		public V append(String name, Object v) {

			if (v instanceof Undefined) {
				v = null;
			}

			if (!X.isEmpty(name)) {
				name = name.toLowerCase();
				if (m.containsKey(name)) {
					return this;
				}
				m.put(name, v);
			}
			return this;
		}

		/**
		 * same as copy(v);
		 * 
		 * @param v the value object
		 * @return the V
		 */
		public V append(V v) {
			return this.copy(v);
		}

		/**
		 * Ignore the fields.
		 *
		 * @param name the name
		 * @return the v
		 */
		public V ignore(String... name) {
			for (String s : name) {
				set(s, V.ignore);
			}
			return this;
		}

		/**
		 * force set the name=value whatever the name exists or not.
		 *
		 * @param name the name
		 * @param v    the value object
		 * @return the V
		 */
		public V force(String name, Object v) {
			if (name != null && v != null) {
				m.put(name, v);
			}
			return this;
		}

		/**
		 * copy all key-value in json to this.
		 *
		 * @param jo the json
		 * @return V
		 */
		public V copy(Map<String, Object> jo) {
			if (jo == null)
				return this;

			for (String s : jo.keySet()) {
				if (jo.containsKey(s)) {
					Object o = jo.get(s);

					if (o instanceof Undefined) {
						continue;
					}

					String s1 = s.replaceAll("\\.", "_");
					if (X.isEmpty(o)) {
						set(s1, X.EMPTY);
					} else {
//						if (o instanceof Integer || o instanceof Long || o instanceof Short || o instanceof Float
//								|| o instanceof Double || o instanceof ) {
						set(s1, o);
//						} else {
//							set(s1, o.toString());
//						}
					}
				}
			}

			return this;
		}

		public V copy() {
			return V.create().copy(m);
		}

		/**
		 * copy all in json to this, if names is null, then nothing to copy.
		 *
		 * @param jo    the json
		 * @param names the name string
		 * @return V
		 */
		public V copy(Map<String, Object> jo, String... names) {
			if (jo == null || names == null)
				return this;

			for (String s : names) {
				if (jo.containsKey(s)) {
					Object o = jo.get(s);
					if (o != null) {
						set(s, o);
					}
				}
			}

			return this;
		}

		/**
		 * copy the object to this, if names is null, then copy all in v.
		 *
		 * @param v     the original V
		 * @param names the names to copy, if null, then copy all
		 * @return V
		 */
		public V copy(V v, String... names) {
			if (v == null)
				return this;

			if (names != null && names.length > 0) {
				for (String s : names) {
					Object o = v.value(s);
					if (X.isEmpty(o)) {
						set(s, X.EMPTY);
					} else {
						set(s, o);
					}
				}
			} else {
				for (String name : v.m.keySet()) {
					Object o = v.m.get(name);
					if (X.isEmpty(o)) {
						set(name, X.EMPTY);
					} else {
						set(name, o);
					}
				}
			}

			return this;
		}

		/**
		 * get the value by name, return null if not presented.
		 *
		 * @param name the string of name
		 * @return Object, return null if not presented
		 */
		public Object value(String name) {
			return m.get(name);
		}

		/**
		 * Creates the empty V object.
		 *
		 * @return V
		 */
		public static V create() {
			return new V();
		}

		/**
		 * Removes the.
		 *
		 * @param name the name
		 * @return the v
		 */
		public V remove(String... name) {
			if (name != null && name.length > 0) {
				String[] ss = this.m.keySet().toArray(new String[this.m.size()]);
				for (String s : ss) {
					for (String s1 : name) {
						if (s.matches(s1)) {
							m.remove(s);
							break;
						}
					}
				}
			}
			return this;
		}

		/**
		 * create and copy a new Value.
		 *
		 * @param v the Value object
		 * @return V the new Value object
		 */
		public static V create(V v) {
			V v1 = V.create();
			v1.copy(v);
			return v1;
		}

		/**
		 * get all key,value
		 * 
		 * @return Map
		 */
		public Map<String, Object> getAll() {
			return m;
		}

	}

	/**
	 * convert the array objects to string.
	 * 
	 * @param arr the array objects
	 * @return the string
	 */
	public static String toString(Object[] arr) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		if (arr != null) {
			int len = arr.length;
			for (int i = 0; i < len; i++) {
				if (i > 0) {
					sb.append(",");
				}

				Object o = arr[i];
				if (o == null) {
					sb.append("null");
				} else if (o instanceof Integer) {
					sb.append(o);
				} else if (o instanceof Date) {
					sb.append("Date(").append(o).append(")");
				} else if (o instanceof Long) {
					sb.append(o);
				} else if (o instanceof Float) {
					sb.append(o);
				} else if (o instanceof Double) {
					sb.append(o);
				} else if (o instanceof Boolean) {
					sb.append("Bool(").append(o).append(")");
				} else {
					sb.append("\"").append(o).append("\"");
				}
			}
		}

		return sb.append("]").toString();
	}

	/**
	 * the {@code W} Class used to create SQL "where" conditions<br>
	 * this is for RDS and Mongo Query.
	 *
	 * @author joe
	 */
	public static class W implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public enum OP {
			eq, gt, gte, lt, lte, like, like_, like_$, neq, none, in, exists, nin, type, mod, all, size, near
		};

		public IAccess access;

		/**
		 * 设置权限过滤器， 只能设置一次
		 * 
		 * @param access， 权限过滤器
		 */
		public void access(IAccess access) {
			if (this.access == null) {
				this.access = access;
			}
		}

		/**
		 * "and"
		 */
		public static final int AND = 9;

		/**
		 * "or"
		 */
		public static final int OR = 10;

		public static final int NOT = 11;

		private String connectsql;
		private List<W> queryList = new ArrayList<W>();
		private List<Entity> order = new ArrayList<Entity>();
		private String groupby;

		public int cond = AND;

		private transient BeanDAO<?, ?> dao = null;
		private transient String table = null;
		private transient DBHelper helper = Helper.primary;

		@SuppressWarnings("rawtypes")
		private Class t = Data.class;

		public String getTable() throws SQLException {
			if (dao != null) {
				String table = dao.tableName();
				if (!X.isEmpty(table)) {
					return table;
				}
			}
			return table;
		}

		public boolean has(OP... ops) {
			for (W e : queryList) {
				if (e instanceof Entity) {
					for (OP o : ops) {
						if (((Entity) e).op == o) {
							return true;
						}
					}
				} else {
					return e.has(ops);
				}
			}
			return false;
		}

		public W helper(DBHelper h) {
			this.helper = h;
			return this;
		}

		public W groupby(String groupby) {
			this.groupby = groupby;
			return this;
		}

		public String groupby() {
			return this.groupby;
		}

		public int getCondition() {
			return cond;
		}

//		public List<Entity> getList() {
//			return elist;
//		}
//
//		public List<W> getW() {
//			return wlist;
//		}

		public List<Entity> getOrder() {
			return order;
		}

//		public void parse(JSON data) throws Exception {
//
//			for (W w : wlist) {
//				w.parse(data);
//			}
//
//			for (Entity e : elist) {
//				if (e.value instanceof String) {
//					e.value = Velocity.parse((String) e.value, data);
//				}
//			}
//
//			for (Entity e : order) {
//				if (e.value instanceof String) {
//					e.value = Velocity.parse((String) e.value, data);
//				}
//			}
//		}

		/**
		 * To json.
		 *
		 * @return the json
		 */
		public JSON toJSON() {
			JSON jo = JSON.create();

			List<JSON> l1 = new ArrayList<JSON>();
			for (W e : queryList) {
				if (e instanceof Entity) {
					JSON j1 = ((Entity) e).toJSON();
					l1.add(j1);
				} else {
					JSON j1 = e.toJSON();
					l1.add(j1);
				}
			}
			jo.put("w", l1);
//			l1 = new ArrayList<JSON>();
//			for (Entity e : elist) {
//				JSON j1 = e.toJSON();
//				l1.add(j1);
//			}
//			jo.put("e", l1);
//			l1 = new ArrayList<JSON>();
//			for (Entity e : order) {
//				JSON j1 = e.toJSON();
//				l1.add(j1);
//			}
			jo.put("o", l1);
			return jo;
		}

		/**
		 * From json.
		 *
		 * @param jo the jo
		 * @return the w
		 */
		public static W fromJSON(JSON jo) {
			W q = W.create();
			Collection<JSON> l1 = jo.getList("w");
			if (l1 != null && l1.size() > 0) {
				for (JSON j1 : l1) {
					W q1 = W.fromJSON(j1);
//					q.wlist.add(q1);
					q.queryList.add(q1);
				}
			}

			l1 = jo.getList("e");
			if (l1 != null && l1.size() > 0) {
				for (JSON j1 : l1) {
					Entity e = Entity.fromJSON(j1);
//					q.elist.add(e);
					q.queryList.add(e);
				}
			}

			l1 = jo.getList("o");
			if (l1 != null && l1.size() > 0) {
				for (JSON j1 : l1) {
					Entity e = Entity.fromJSON(j1);
					q.order.add(e);
				}
			}

			return q;
		}

		private W() {
		}

		public W inc(String name, int n) {
			Helper.inc(this.table, name, n, this, null);
			return this;
		}

		/**
		 * remove the conditions from the query.
		 *
		 * @param names the names
		 * @return the W
		 */
		public W remove(String... names) {
//			if (names != null) {
//				for (String name : names) {
//					for (int i = elist.size() - 1; i >= 0; i--) {
//						Entity e = elist.get(i);
//						if (X.isSame(name, e.name)) {
//							elist.remove(i);
//						}
//					}
//				}
//			}
			if (names != null) {
				for (String name : names) {
					for (int i = queryList.size() - 1; i >= 0; i--) {
						W c = queryList.get(i);
						if (c instanceof Entity) {
							Entity e = (Entity) c;
							if (X.isSame(name, e.name)) {
								queryList.remove(i);
							}
						}
					}
				}
			}
			return this;
		}

		public void close() {
			if (helper != null) {
				helper.close();
				helper = null;
			}
		}

		/**
		 * clone a new W <br>
		 * return a new W.
		 *
		 * @return W
		 */
		public W copy() {

			W w = new W();
			w.dao = this.dao;
			w.table = this.table;
			w.helper = this.helper;
			w.groupby = groupby;
			w.access = this.access;

			w.cond = cond;

			for (W e : queryList) {
				w.queryList.add(e.copy());
			}

			for (Entity e : order) {
				w.order.add(e.copy());
			}

			return w;
		}

		/**
		 * size of the W.
		 *
		 * @return int
		 */
		public int size() {
			int size = 0;
			for (W w : queryList) {
				if (w instanceof Entity) {
					size++;
				} else {
					size += w.size();
				}
			}
			return size;
		}

		/**
		 * is empty
		 * 
		 * @return true if empty
		 */
		public boolean isEmpty() {
			return X.isEmpty(queryList) && X.isEmpty(order);
		}

		/**
		 * create args for the SQL "where" <br>
		 * return the Object[].
		 *
		 * @return Object[]
		 */
		Object[] args() {
			if (!queryList.isEmpty()) {
				List<Object> l1 = new ArrayList<Object>();

				_args(l1);

				return l1.toArray(new Object[l1.size()]);
			}

			return null;
		}

		private void _args(List<Object> list) {
			for (W e : queryList) {
				if (e instanceof Entity) {
					((Entity) e).args(list);
				} else {
					e._args(list);
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object.toString()
		 */
		public String toString() {

//			return this.query().toString();

			StringBuilder sb = new StringBuilder();
			sb.append("{" + where() + "=>" + Helper.toString(args()));
			if (!X.isEmpty(order)) {
				sb.append(", sort=" + order);
			}
			if (!X.isEmpty(groupby)) {
				sb.append(", groupby=" + groupby);
			}
			sb.append("}");
			return sb.toString();
		}

		/**
		 * create the SQL "where".
		 *
		 * @return String
		 */
		String where() {
			return where(null);
		}

		/**
		 * create the SQL "where" with the tansfers.
		 *
		 * @param tansfers the words pair should be transfered
		 * @return the SQL string
		 */
		String where(Map<String, String> tansfers) {

			StringBuilder sb = new StringBuilder();
			if (!X.isEmpty(connectsql)) {
				sb.append(connectsql);
			}

			for (W clause : queryList) {
				if (sb.length() > 0) {
					if (clause.getCondition() == AND) {
						sb.append(" and ");
					} else if (clause.getCondition() == OR) {
						sb.append(" or ");
					} else if (clause.getCondition() == NOT) {
						sb.append(" and not ");
					}
				}
				sb.append(clause.where(tansfers));
			}

			if (this.getCondition() == NOT) {
				return sb.length() == 0 ? X.EMPTY : "!(" + sb + ")";
			} else {
				return sb.length() == 0 ? X.EMPTY : "(" + sb + ")";
			}

		}

		/**
		 * create a empty.
		 *
		 * @return W
		 */
		public static W create() {
			return new W();
		}

		/**
		 * create a Where for the table
		 * 
		 * @param table the table name
		 * @return W
		 */
		public static W table(String table) {
			W w = new W();
			w.table = table;
			return w;
		}

		/**
		 * get the order by.
		 *
		 * @return String
		 */
		String orderby() {
			return orderby(null);
		}

		/**
		 * create order string with the transfers.
		 *
		 * @param transfers the words pair that need transfer according database
		 * @return the SQL order string
		 */
		String orderby(Map<String, String> transfers) {

			if (order.size() > 0) {
				StringBuilder sb = new StringBuilder("order by ");
				for (int i = 0; i < order.size(); i++) {
					Entity e = order.get(i);
					if (i > 0) {
						sb.append(",");
					}
					if (transfers != null && transfers.containsKey(e.name)) {
						sb.append(transfers.get(e.name));
					} else {
						sb.append(e.name);
					}
					if (X.toInt(e.value) < 0) {
						sb.append(" desc");
					}
				}
				return sb.toString();
			}
			return null;
		}

		/**
		 * set the name and parameter with "and" and "EQ" conditions.
		 *
		 * @param name   the name
		 * @param v      the v
		 * @param boost, the weight of the
		 * @return W
		 */
		public W and(String name, Object v) {
			return and(name, v, 1);
		}

		public W and(String name, Object v, int boost) {
			return and(name, v, OP.eq, boost);
		}

		/**
		 * set and "and (...)" conditions
		 *
		 * @param w the w
		 * @return W
		 */

		public W and(W w) {
			return and(w, W.AND);
		}

		/**
		 * 
		 * @param w
		 * @param cond, and, or, not
		 * @return
		 */
		public W and(W w, int cond) {

			if (w.isEmpty())
				return this;

			if (w.queryList.size() == 1 && (cond == W.AND || cond == W.OR)) {
				queryList.add(w.queryList.get(0));
				return this;
			}

			w.cond = cond;
//			if (w instanceof Entity) {
//				((Entity) w).container = this;
//			}
			queryList.add(w);

			return this;
		}

		public String toSQL() {

			StringBuilder sql = new StringBuilder();

			this.sql(sql);

			return sql.toString();
		}

		void sql(StringBuilder sql) {

			StringBuilder sb = new StringBuilder();
			for (W clause : queryList) {
				if (sb.length() > 0) {
					if (clause.cond == AND) {
						sb.append(" and ");
					} else if (clause.cond == OR) {
						sb.append(" or ");
					} else if (clause.cond == NOT) {
						sb.append(" and not ");
					}
				} else if (clause.cond == NOT) {
					sb.append(" not ");
				}

//				clause.where(tansfers);
				if (clause instanceof Entity) {
					clause.sql(sb);
				} else {
					sb.append("(");
					clause.sql(sb);
					sb.append(")");
				}
			}
			if (!this.order.isEmpty()) {
				sb.append(" order by ");
				for (int i = 0, len = this.order.size(); i < len; i++) {
					if (i > 0) {
						sb.append(",");
					}
					Entity e = this.order.get(i);
					sb.append(e.name);
					if (X.isSame(e.value, -1)) {
						sb.append(" desc");
					}
				}
			}
//			sql.append("(").append(sb.toString()).append(")");
			sql.append(sb.toString());

		}

		/**
		 * and a SQL
		 * 
		 * @param sql
		 * @return
		 */
		public W and(String sql) {
			try {
				W q = SQL.where2W(sql);
				if (!q.isEmpty()) {
//					if (this.isEmpty()) {
//						this.and(q);
//					} else {
					this.and(q);
//					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			return this;
		}

		/**
		 * or a sql
		 * 
		 * @param sql
		 * @return
		 */
		public W or(String sql) {
			try {
				W q = SQL.where2W(StringFinder.create(sql));
				if (!q.isEmpty()) {
//					if (this.isEmpty()) {
//						this.copy(q);
//					} else {
					this.or(q);
//					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			return this;
		}

		public W copy(W q) {

			this.groupby = q.groupby;

			this.cond = q.cond;

			for (W e : q.queryList) {
//				W e1 = e.copy();
//				if (e instanceof Entity) {
//					((Entity) e).container = this;
//				}
				this.queryList.add(e);
			}

			for (Entity e : q.order) {
				Entity e1 = e.copy();
//				e1.container = this;
				this.order.add(e1);
			}

			return this;
		}

		/**
		 * set a "or (...)" conditions
		 *
		 * @param w the w
		 * @return W
		 */
		public W or(W w) {
			if (w.isEmpty())
				return this;

			if (this.queryList.isEmpty())
				return and(w);

			if (w.queryList.size() == 1) {
				W q1 = w.queryList.get(0);
				q1.cond = OR;
				queryList.add(q1);
				return this;
			}

			w.cond = OR;
//			if (w instanceof Entity) {
//				((Entity) w).container = this;
//			}
			queryList.add(w);
			return this;
		}

		private void _and(List<LinkedHashMap<String, Object>> l1, Entity e) {
			if (e.op == W.OP.like || e.op == W.OP.like_ || e.op == W.OP.like_$) {
				return;
			}

			if (l1.isEmpty()) {
				l1.add(new LinkedHashMap<String, Object>());
			}

			for (LinkedHashMap<String, Object> r : l1) {
				if (!r.containsKey(e.name)) {
					if (X.isSame(e.value, -1)) {
						r.put(e.name, -1);
					} else {
						r.put(e.name, 1);
					}
				}
			}
		}

		private void _and(List<LinkedHashMap<String, Object>> l1, W e) {
			if (l1.isEmpty()) {
				l1.add(new LinkedHashMap<String, Object>());
			}

			List<LinkedHashMap<String, Object>> l2 = e.sortkeys();

			for (LinkedHashMap<String, Object> r : l1) {
				for (LinkedHashMap<String, Object> r2 : l2) {
					for (String name : r2.keySet()) {
						if (!r.containsKey(name)) {
							r.put(name, 1);
						}
					}
				}
			}

		}

		private void _or(List<LinkedHashMap<String, Object>> l1, Entity e) {

			if (e.op == W.OP.like || e.op == W.OP.like_ || e.op == W.OP.like_$) {
				return;
			}

			LinkedHashMap<String, Object> r = new LinkedHashMap<String, Object>();
			if (X.isSame(e.value, -1)) {
				r.put(e.name, -1);
			} else {
				r.put(e.name, 1);
			}
			l1.add(r);
		}

		private void _or(List<LinkedHashMap<String, Object>> l1, W e) {
			l1.addAll(e.sortkeys());
		}

		/**
		 * get all keys.
		 *
		 * @return List keys
		 */
		public List<LinkedHashMap<String, Object>> sortkeys() {

			List<LinkedHashMap<String, Object>> l1 = new ArrayList<LinkedHashMap<String, Object>>();

//			if (!X.isEmpty(elist))
//				for (Entity e : elist) {
//					if (e.cond == AND) {
//						_and(l1, e);
//					} else {
//						_or(l1, e);
//					}
//				}
//
//			if (!X.isEmpty(wlist))
//				for (W w : wlist) {
//					if (w.cond == AND) {
//						_and(l1, w);
//					} else {
//						_or(l1, w);
//					}
//				}

			if (!queryList.isEmpty()) {
				for (W e : queryList) {
					if (e instanceof Entity) {
						if (e.cond == AND) {
							_and(l1, (Entity) e);
						} else {
							_or(l1, (Entity) e);
						}
					} else {
						if (e.cond == AND) {
							_and(l1, e);
						} else {
							_or(l1, e);
						}
					}
				}
			}

			List<LinkedHashMap<String, Object>> l2 = new ArrayList<LinkedHashMap<String, Object>>();
			if (!l1.isEmpty()) {
				for (LinkedHashMap<String, Object> e : l1) {
					l2.add(X.clone(e));
				}
			}

			// index order too
			if (!X.isEmpty(order)) {
				if (l1.isEmpty()) {
					l1.add(new LinkedHashMap<String, Object>());
				}

				for (Entity e : order.toArray(new Entity[order.size()])) {

					for (LinkedHashMap<String, Object> m : l1) {
						if (!m.containsKey(e.name)) {
							if (X.isSame(e.name, "geo")) {
								m.put(e.name, 2);
							} else {
								int i = X.toInt(e.value);
								if (i < 0) {
									m.put(e.name, -1);
								} else {
									m.put(e.name, 1);
								}
							}
						}
					}
				}
				l2.addAll(l1);
			}

			return l2;
		}

		/**
		 * @param name
		 * @param v
		 * @return
		 */
		public W and(String[] name, Object v) {
			return and(name, v, W.OP.eq);
		}

		/**
		 * @param name
		 * @param v
		 * @param op
		 * @return
		 */
		public W and(String[] name, Object v, OP op) {
			return and(name, v, op, 1);
		}

		/**
		 * @param name
		 * @param v
		 * @param op
		 * @param boost
		 * @return
		 */
		public W and(String[] name, Object v, OP op, int boost) {
			if (v instanceof String) {
				String[] ss = X.split(v.toString(), "[ ]");
				for (String s1 : ss) {
					W q = W.create();
					String[] s2 = X.split(s1, "[\\|]");
					for (String s3 : s2) {
						for (String s : name) {
							q.or(s, s3, op, boost);
						}
					}
					this.and(q);
				}
			} else {
				W q = W.create();
				for (String s : name) {
					q.or(s, v, op, boost);
				}
				this.and(q);
			}
			return this;
		}

		public W and(String[] name, Object v, int[] boost) {
			return and(name, v, OP.eq, boost);
		}

		/**
		 * 
		 * @param boost
		 * @return
		 */
		public W boost(int... boost) {
			if (queryList != null) {
				int n = queryList.size();
				n = Math.min(n, boost.length);
				for (int i = 0; i < n; i++) {
					W w = queryList.get(i);
					if (w instanceof Entity) {
						Entity e = (Entity) w;
						e.boost = boost[i];
					}
				}
			}
			return this;
		}

		public W and(String[] name, Object v, OP op, int[] boost) {
			if (v instanceof String) {
				String[] ss = X.split(v.toString(), " ");
				for (String s1 : ss) {
					W q = W.create();
					String[] s2 = X.split(s1, "\\|");
					for (String s3 : s2) {
						for (int i = 0; i < name.length; i++) {
							q.or(name[i], s3, op, boost[i]);
						}
					}
					this.and(q);
				}
			} else {
				W q = W.create();
				for (int i = 0; i < name.length; i++) {
					q.or(name[i], v, op, boost[i]);
				}
				this.and(q);
			}
			return this;
		}

		/**
		 * @Deprecated
		 * @param name
		 * @param v
		 * @param op
		 * @return
		 */
		public W and(String name, Object v, String op) {
			if (X.isSame(">", op) || X.isSame("gt", op)) {
				return and(name, v, W.OP.gt);
			} else if (X.isSame(">=", op) || X.isSame("gte", op)) {
				return and(name, v, W.OP.gte);
			} else if (X.isSame("<", op) || X.isSame("lt", op)) {
				return and(name, v, W.OP.lt);
			} else if (X.isSame("<=", op) || X.isSame("lte", op)) {
				return and(name, v, W.OP.lte);
			} else if (X.isSame("!=", op) || X.isSame("neq", op)) {
				return and(name, v, W.OP.neq);
			} else if (X.isSame("like", op)) {
				return and(name, v, W.OP.like);
			} else if (X.isSame("like_", op)) {
				return and(name, v, W.OP.like_);
			} else if (X.isSame("like_$", op)) {
				return and(name, v, W.OP.like_$);
			}
			return this;
		}

		/**
		 * @Deprecated
		 * @param name
		 * @param v
		 * @param op
		 * @return
		 */
		public W or(String name, Object v, String op) {
			return or(name, v, op, 1);
		}

		/**
		 * @Deprecated
		 * @param name
		 * @param v
		 * @param op
		 * @param boost
		 * @return
		 */
		public W or(String name, Object v, String op, int boost) {
			if (X.isSame(">", op) || X.isSame("gt", op)) {
				return or(name, v, W.OP.gt, boost);
			} else if (X.isSame(">=", op) || X.isSame("gte", op)) {
				return or(name, v, W.OP.gte, boost);
			} else if (X.isSame("<", op) || X.isSame("lt", op)) {
				return or(name, v, W.OP.lt, boost);
			} else if (X.isSame("<=", op) || X.isSame("lte", op)) {
				return or(name, v, W.OP.lte, boost);
			} else if (X.isSame("!=", op) || X.isSame("neq", op)) {
				return or(name, v, W.OP.neq, boost);
			} else if (X.isSame("like", op)) {
				return or(name, v, W.OP.like, boost);
			}
			return this;
		}

		/**
		 * set the namd and parameter with "op" conditions.
		 *
		 * @param name the name
		 * @param v    the value object
		 * @param op   the operation
		 * @return the W
		 */
		public W and(String name, Object v, OP op) {
			return and(name, v, op, 1);
		}

		public W scan(Consumer<Entity> func) {

			for (int i = queryList.size() - 1; i >= 0; i--) {
				W e = queryList.get(i);
				if (e instanceof Entity) {

					Entity e1 = (Entity) e;

					e1._idx = i;
					e1._queryList = queryList;

					String name = e1.name;
					Object value = e1.value;

					func.accept(e1);

					if (!X.isSame(e1.value, value) || !X.isSame(name, e1.name)) {
						e1.replace(e1.value);
					}

				} else {
					e.scan(func);

					if (e.isEmpty()) {
						queryList.remove(i);
					}
				}

			}
			return this;
		}

		public W scan(ScriptObjectMirror m) {
			return this.scan(e -> {
				m.call(e);
			});
		}

		@SuppressWarnings({ "rawtypes" })
		public W and(String name, Object v, OP op, int boost) {
			if (X.isEmpty(name))
				return this;

			name = name.toLowerCase();

			if (v instanceof Undefined) {
				v = null;
			}

			if (v != null && v instanceof Collection) {
				Collection l1 = (Collection) v;

				if (l1.isEmpty()) {
					this.and(name, null, op);
				} else if (l1.size() == 1) {
					this.and(name, l1.iterator().next(), op);
				} else {
					W q = W.create();
					for (Object o : l1) {
						if (o instanceof W) {
							o = ((W) o).query();
						}
						if (op.equals(OP.neq)) {
							q.and(name, o, OP.neq);
						} else {
							q.or(name, o, op);
						}
					}
					this.and(q);
				}
			} else if (v != null && v.getClass().isArray()) {
				Object[] l1 = (Object[]) v;

				if (l1 == null || l1.length == 0) {
					this.and(name, null, op);
				} else if (l1.length == 1) {
					this.and(name, l1[0], op);
				} else {
					W q = W.create();
					for (Object o : l1) {
						if (o instanceof W) {
							o = ((W) o).query();
						}
						if (op.equals(OP.neq)) {
							q.and(name, o, OP.neq);
						} else {
							q.or(name, o, op);
						}
					}
					this.and(q);
				}
			} else {
				if (v instanceof W) {
					v = ((W) v).query();
				}
				queryList.add(new Entity(name, v, op, AND, boost));
			}
			return this;
		}

		public W or(String[] name, Object v) {
			return or(name, v, W.OP.eq);
		}

		public W or(String[] name, Object v, OP op) {
			return or(name, v, op, 1);
		}

		public W or(String[] name, Object v, OP op, int boost) {
			for (String s : name) {
				this.or(s, v, op, boost);
			}
			return this;
		}

		/**
		 * set name and parameter with "or" and "EQ" conditions.
		 *
		 * @param name the name
		 * @param v    the v
		 * @return W
		 */
		public W or(String name, Object v) {
			return or(name, v, 1);
		}

		public W or(String name, Object v, int boost) {
			return or(name, v, OP.eq, boost);
		}

		/**
		 * set the name and parameter with "or" and "op" conditions.
		 *
		 * @param name the name
		 * @param v    the v
		 * @param op   the op
		 * @return W
		 */
		public W or(String name, Object v, OP op) {
			return or(name, v, op, 1);
		}

		@SuppressWarnings({ "rawtypes" })
		public W or(String name, Object v, OP op, int boost) {

			if (X.isEmpty(name))
				return this;

			if (this.queryList.isEmpty())
				return and(name, v, op, boost);

			name = name.toLowerCase();

			if (v instanceof Undefined) {
				v = null;
			}

			if (v != null && v instanceof Collection) {
				Collection l1 = (Collection) v;
				if (l1.isEmpty()) {
					this.or(name, null, op);
				} else if (l1.size() == 1) {
					this.or(name, l1.iterator().next(), op);
				} else {
					W q = W.create();
					for (Object o : l1) {
						if (o instanceof W) {
							o = ((W) o).query();
						}
						if (op.equals(OP.neq)) {
							q.and(name, o, OP.neq);
						} else {
							q.or(name, o, op);
						}
					}
					this.or(q);
				}
			} else if (v != null && v.getClass().isArray()) {
				Object[] l1 = (Object[]) v;
				if (l1 == null || l1.length == 0) {
					this.or(name, null, op);
				} else if (l1.length == 1) {
					this.or(name, l1[0], op);
				} else {
					W q = W.create();
					for (Object o : l1) {
						if (o instanceof W) {
							o = ((W) o).query();
						}
						if (op.equals(OP.neq)) {
							q.and(name, o, OP.neq);
						} else {
							q.or(name, o, op);
						}
					}
					this.or(q);
				}
			} else {
				if (v instanceof W) {
					v = ((W) v).query();
				}
//				elist.add(new Entity(name, v, op, OR, boost));
				queryList.add(new Entity(name, v, op, OR, boost));
			}
			return this;
		}

		/**
		 * copy the name and parameter from a JSON, with "and" and "op" conditions.
		 *
		 * @param jo    the json
		 * @param op    the op
		 * @param names the names
		 * @return W
		 */
		public W copy(JSON jo, OP op, String... names) {
			if (jo != null && names != null && names.length > 0) {
				for (String name : names) {
					if (jo.has(name)) {
						String s = jo.getString(name);
						if (s != null && !"".equals(s)) {
							and(name, s, op);
						}
					}
				}
			}

			return this;
		}

		/**
		 * copy the value in jo, the format of name is: ["name", "table field name" ].
		 *
		 * @param jo    the json
		 * @param op    the op
		 * @param names the names
		 * @return W
		 */
		public W copy(JSON jo, OP op, String[]... names) {
			if (jo != null && names != null && names.length > 0) {
				for (String name[] : names) {
					if (name.length > 1) {
						if (jo.has(name[0])) {
							String s = jo.getString(name[0]);
							if (s != null && !"".equals(s)) {
								and(name[1], s, op);
							}
						}
					} else if (jo.has(name[0])) {
						String s = jo.getString(name[0]);
						if (s != null && !"".equals(s)) {
							and(name[0], s, op);
						}
					}
				}
			}

			return this;
		}

		/**
		 * create a new W with name and parameter, "and" and "EQ" conditions.
		 * 
		 * @param name the name
		 * @param v    the v
		 * @return W
		 */
		public static W create(String name, Object v) {
			return create(name, v, OP.eq);
		}

		/**
		 * create the W object with the parameters.
		 * 
		 * @Deprecated
		 * @param name the field name
		 * @param v    the value object
		 * @param op   the operation
		 * @return the W
		 */
		public static W create(String name, Object v, OP op) {
			W w = new W();
			w.and(name, v, op);
			return w;
		}

		public static class Entity extends W {

			public List<W> _queryList;

			public int _idx;

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public String name;
			public Object value;
			public String type; // datatype: long, string
			public OP op; // operation EQ, GT, ...
//			public int cond; // condition AND, OR
			public int boost = 1;//

			public void op(String op) {
				this.op = W.OP.valueOf(op);
			}

//			public W container() {
//				return container;
//			}

			/**
			 * replace the name and value
			 * 
			 * @param name
			 * @param value
			 */
			public void replace(Object value) {
//				this.value = value;
//				final int i = container.queryList.indexOf(this);
//				container.queryList.remove(i);

//				if (this.cond == W.AND) {

				List<String> l1 = X.asList(X.split(name, "[,;\"\'\\[\\]]"), s -> s.toString());
				if (l1.size() == 1) {

					this.name = name.toString();
					if (value == null) {
						this.value = value;
					} else {
						List<?> l2 = X.asList(value, s -> s);
						if (l2.size() == 1) {
							this.value = l2.get(0);
						} else {
							W q = W.create();
							for (Object s : l2) {
								q.or(this.name, s, this.op);
							}
							_queryList.remove(this._idx);
							q.cond = this.cond;
							_queryList.add(this._idx, q);
						}
					}
//						container.and(name.toString(), value, this.op);
				} else {

					if (value == null) {
						W q = W.create();
						for (String s : l1) {
							q.or(s, value, this.op);
						}
						_queryList.remove(this._idx);
						q.cond = this.cond;
						_queryList.add(this._idx, q);
					} else {
						List<?> l2 = X.asList(value, s -> s);
						if (l2.size() == 1) {
							W q = W.create();
							for (String s : l1) {
								q.or(s, l2.get(0), this.op);
							}
							_queryList.remove(this._idx);
							q.cond = this.cond;
							_queryList.add(this._idx, q);
						} else {
							W q = W.create();
							for (String s : l1) {
								for (Object v : l2) {
									q.or(s, v, this.op);
								}
							}
							_queryList.remove(this._idx);
							q.cond = this.cond;
							_queryList.add(this._idx, q);
						}
					}

//						container.and(q);
				}

//				} else if (this.cond == W.OR) {
//
//					List<String> l1 = X.asList(name, s -> s.toString());
//					if (l1.size() == 1) {
//						container.or(name.toString(), value, this.op);
//					} else {
//						W q = W.create();
//						for (String s : l1) {
//							q.or(s, value, this.op);
//						}
//						container.or(q);
//					}
//
//				} else if (this.cond == W.NOT) {
//
//					List<String> l1 = X.asList(name, s -> s.toString());
//					if (l1.size() == 1) {
//						container.and(W.create(name.toString(), value, this.op), W.NOT);
//					} else {
//						W q = W.create();
//						for (String s : l1) {
//							q.or(s, value, this.op);
//						}
//						container.and(q, W.NOT);
//					}
//
//				}
			}

			public void remove() {
				_queryList.remove(_idx);
			}

			public int getCondition() {
				return cond;
			}

			private List<Object> args(List<Object> list) {

				if (value instanceof Object[]) {
					for (Object o : (Object[]) value) {
						list.add(o);
					}
				} else if (op == OP.like) {
					list.add("%" + value + "%");
				} else if (op == OP.like_) {
					list.add(value + "%");
				} else if (op == OP.like_$) {
					list.add("%" + value);
				} else {
					list.add(value);
				}

				return list;
			}

			/**
			 * From json.
			 *
			 * @param j1 the j1
			 * @return the entity
			 */
			public static Entity fromJSON(JSON j1) {
				return new Entity(j1.getString("name"), j1.get("value"), OP.valueOf(j1.getString("op")),
						j1.getInt("cond"), j1.getInt("boost", 1));
			}

			public BasicDBObject query() {
				if (this.cond == NOT) {
					if (op == OP.eq) {
						if (value == null) {
							return new BasicDBObject(name,
									new BasicDBObject("$not", new BasicDBObject("$exists", false)));
						} else {
							return new BasicDBObject(name, new BasicDBObject("$not", value));
						}
					} else if (op == OP.gt) {
						return new BasicDBObject(name, new BasicDBObject("$not", new BasicDBObject("$gt", value)));
					} else if (op == OP.gte) {
						return new BasicDBObject(name, new BasicDBObject("$not", new BasicDBObject("$gte", value)));
					} else if (op == OP.lt) {
						return new BasicDBObject(name, new BasicDBObject("$not", new BasicDBObject("$lt", value)));
					} else if (op == OP.lte) {
						return new BasicDBObject(name, new BasicDBObject("$not", new BasicDBObject("$lte", value)));
					} else if (op == OP.like) {
						Pattern p1 = Pattern.compile(value.toString());
						return new BasicDBObject(name, new BasicDBObject("$not", p1));
					} else if (op == OP.like_) {
						Pattern p1 = Pattern.compile("^" + value);
						return new BasicDBObject(name, new BasicDBObject("$not", p1));
					} else if (op == OP.like_$) {
						Pattern p1 = Pattern.compile(value + "$");
						return new BasicDBObject(name, new BasicDBObject("$not", p1));
					} else if (op == OP.neq) {
						if (value == null) {
							return new BasicDBObject(name,
									new BasicDBObject("$not", new BasicDBObject("$exists", true)));
						} else {
							return new BasicDBObject(name, new BasicDBObject("$not", new BasicDBObject("$ne", value)));
						}
					} else if (op == OP.exists) {
						return new BasicDBObject(name, new BasicDBObject("$not", new BasicDBObject("$exists", value)));
					} else if (op == OP.in) {
						return new BasicDBObject(name, new BasicDBObject("$not", new BasicDBObject("$in", value)));
					} else if (op == OP.nin) {
						return new BasicDBObject(name, new BasicDBObject("$not", new BasicDBObject("$nin", value)));
					} else if (op == OP.type) {
						return new BasicDBObject(name, new BasicDBObject("$not", new BasicDBObject("$type", value)));
					} else if (op == OP.mod) {
						return new BasicDBObject(name, new BasicDBObject("$not", new BasicDBObject("$mod", value)));
					} else if (op == OP.all) {
						return new BasicDBObject(name, new BasicDBObject("$not", new BasicDBObject("$all", value)));
					} else if (op == OP.size) {
						return new BasicDBObject(name, new BasicDBObject("$not", new BasicDBObject("$size", value)));
					} else if (op == OP.near) {
						return new BasicDBObject(name, new BasicDBObject("$not", new BasicDBObject("$near", value)));
					}
				} else {
					if (op == OP.eq) {
						if (value == null) {
							return new BasicDBObject(name, new BasicDBObject("$exists", false));
						} else {
							return new BasicDBObject(name, value);
						}
					} else if (op == OP.gt) {
						return new BasicDBObject(name, new BasicDBObject("$gt", value));
					} else if (op == OP.gte) {
						return new BasicDBObject(name, new BasicDBObject("$gte", value));
					} else if (op == OP.lt) {
						return new BasicDBObject(name, new BasicDBObject("$lt", value));
					} else if (op == OP.lte) {
						return new BasicDBObject(name, new BasicDBObject("$lte", value));
					} else if (op == OP.like) {
						Pattern p1 = Pattern.compile(value.toString());
						return new BasicDBObject(name, p1);
					} else if (op == OP.like_) {
						Pattern p1 = Pattern.compile("^" + value);
						return new BasicDBObject(name, p1);
					} else if (op == OP.like_$) {
						Pattern p1 = Pattern.compile(value + "$");
						return new BasicDBObject(name, p1);
					} else if (op == OP.neq) {
						if (value == null) {
							return new BasicDBObject(name, new BasicDBObject("$exists", true));
						} else {
							return new BasicDBObject(name, new BasicDBObject("$ne", value));
						}
					} else if (op == OP.exists) {
						return new BasicDBObject(name, new BasicDBObject("$exists", value));
					} else if (op == OP.in) {
						return new BasicDBObject(name, new BasicDBObject("$in", value));
					} else if (op == OP.nin) {
						return new BasicDBObject(name, new BasicDBObject("$nin", value));
					} else if (op == OP.type) {
						return new BasicDBObject(name, new BasicDBObject("$type", value));
					} else if (op == OP.mod) {
						return new BasicDBObject(name, new BasicDBObject("$mod", value));
					} else if (op == OP.all) {
						return new BasicDBObject(name, new BasicDBObject("$all", value));
					} else if (op == OP.size) {
						return new BasicDBObject(name, new BasicDBObject("$size", value));
					} else if (op == OP.near) {
						return new BasicDBObject(name, new BasicDBObject("$near", value));
					}
				}
				return new BasicDBObject();
			}

			/**
			 * To json.
			 *
			 * @return the json
			 */
			public JSON toJSON() {
				JSON jo = JSON.create();
				jo.put("name", name);
				jo.put("value", value);
				jo.put("op", op.toString());
				jo.put("cond", cond);
				return jo;
			}

			/**
			 * Copy.
			 *
			 * @return the entity
			 */
			public Entity copy() {
				return new Entity(name, value, op, cond, boost);
			}

			public void sql(StringBuilder sql) {

//				if (value == null)
//					return;

				sql.append(name);

				if (op == OP.eq) {
					sql.append("=");
				} else if (op == OP.gt) {
					sql.append(">");
				} else if (op == OP.gte) {
					sql.append(">=");
				} else if (op == OP.lt) {
					sql.append("<");
				} else if (op == OP.lte) {
					sql.append("<=");
				} else if (op == OP.like || op == OP.like_ || op == OP.like_$) {
					sql.append(" like ");
				} else if (op == OP.neq) {
					sql.append(" <> ");
				}

				if (value instanceof String) {
					sql.append("'").append(value).append("'");
				} else {
					sql.append(value);
				}

//				return sb.toString();
			}

			public String where(Map<String, String> tansfers) {
				// TODO, for "null" value, some db "is null"

				StringBuilder sb = new StringBuilder();

				if (tansfers != null && tansfers.containsKey(name)) {
					sb.append(tansfers.get(name));
				} else {
					sb.append(name);
				}

				if (op == OP.eq) {
					sb.append("=?");
				} else if (op == OP.gt) {
					sb.append(">?");
				} else if (op == OP.gte) {
					sb.append(">=?");
				} else if (op == OP.lt) {
					sb.append("<?");
				} else if (op == OP.lte) {
					sb.append("<=?");
				} else if (op == OP.like || op == OP.like_ || op == OP.like_$) {
					sb.append(" like ?");
				} else if (op == OP.neq) {
					sb.append(" <> ?");
				}

				return sb.toString();
			}

			transient String tostring;

			/*
			 * (non-Javadoc)
			 * 
			 * @see java.lang.Object.toString()
			 */
			public String toString() {
				if (tostring == null) {
					StringBuilder sb = new StringBuilder(name);

					if (op == OP.eq) {
						sb.append("=?");
					} else if (op == OP.gt) {
						sb.append(">?");
					} else if (op == OP.gte) {
						sb.append(">=?");
					} else if (op == OP.lt) {
						sb.append("<?");
					} else if (op == OP.lte) {
						sb.append("<=?");
					} else if (op == OP.like) {
						sb.append(" like ?");
					} else if (op == OP.neq) {
						sb.append(" <> ?");
					}

					sb.append(value);

					tostring = sb.toString();
				}
				return tostring;
			}

			private Entity(String name, Object v, OP op, int cond, int boost) {
//				this.container = container;
				this.name = name;
				this.cond = cond;
				this.boost = boost;
				this.op = op;
				this.value = v;
			}

			@Override
			public String where() {
				return where(null);
			}
		}

		BasicDBObject query() {

			BasicDBList list = new BasicDBList();

			int cond = -1;
			for (W clause : queryList) {
				if (!list.isEmpty() && cond != clause.getCondition()) {
					if (cond == AND) {
						if (list.size() > 1) {
							BasicDBObject q = new BasicDBObject("$and", list.clone());
							list.clear();
							list.add(q);
						}
					} else if (cond == OR) {
						if (list.size() > 1) {
							BasicDBObject q = new BasicDBObject("$or", list.clone());
							list.clear();
							list.add(q);
						}
					}
				}

				int c1 = clause.getCondition();
				if (c1 == W.NOT) {
					if (clause instanceof W) {
						BasicDBObject o = ((W) clause).query();
						String key = o.keySet().iterator().next();
						Object v1 = o.get(key);
						if (v1 instanceof List) {
							BasicDBObject q = new BasicDBObject("$not", o);
							list.add(q);
						} else {
							BasicDBObject q = new BasicDBObject(key, new BasicDBObject("$not", v1));
							list.add(q);
						}
					} else {
						BasicDBObject q = new BasicDBObject("$not", clause);
						list.add(q);
					}

				} else {
					cond = c1;

					if (clause instanceof W) {
						list.add(((W) clause).query());
					} else {
						list.add(clause);
					}
				}
			}

			if (list.isEmpty()) {
				return new BasicDBObject();
			} else if (list.size() == 1) {
				return (BasicDBObject) list.get(0);
			} else {
				if (cond == OR) {
					return new BasicDBObject().append("$or", list);
				} else {

					return new BasicDBObject().append("$and", list);
				}
			}
		}

		/**
		 * Order.
		 *
		 * @return the basic db object
		 */
		BasicDBObject order() {
			BasicDBObject q = new BasicDBObject();
			if (order != null && order.size() > 0) {
				for (Entity e : order) {
					q.append(e.name, e.value);
				}
			}

			return q;
		}

		/**
		 * Sort as asc
		 * 
		 * @param name
		 * @return
		 */
		public W sort(String name) {
			return sort(name, 1);
		}

		public W clearSort() {
			order.clear();
			return this;
		}

		public W sort(String name, String type) {
			return sort(name, 1, type);
		}

		/**
		 * Sort.
		 *
		 * @param name the name
		 * @param i    the i
		 * @return the w
		 */
		public W sort(String name, int i) {
			if (X.isEmpty(name))
				return this;
			order.add(new Entity(name, i, OP.eq, AND, 0));
			return this;
		}

		public W sort(String name, int i, String type) {
			if (X.isEmpty(name))
				return this;

			Entity e = new Entity(name, i, OP.eq, AND, 0);
			e.type = type;
			order.add(e);
			return this;
		}

		public W copy(V v) {
			if (v != null && !v.isEmpty()) {
				for (String name : v.m.keySet()) {
					and(name, v.m.get(name));
				}
			}
			return this;
		}

		public boolean exists() throws SQLException {

			if (log.isDebugEnabled())
				log.debug("w=" + this);

			if (dao != null) {
				return dao.exists(this);
			} else if (!X.isEmpty(table)) {

				helper.getOptimizer().query(table, this);

				return helper.exists(table, this);
			} else {
				throw new SQLException("not set table");
			}

		}

		public <T extends Bean> boolean stream(Function<T, Boolean> func) throws Exception {
			return stream(0, func);
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		public <T extends Bean> boolean stream(long offset, Function<T, Boolean> func) throws Exception {

			if (dao != null) {

				return dao.stream(this, offset, (Function) func);

			} else if (!X.isEmpty(table)) {
				W q = this;
				if (access != null) {
					W q1 = access.filter("db", table);
					if (q1 != null) {
						if (q.isEmpty()) {
							q = q1;
						} else if (!q1.isEmpty()) {
							q = W.create().and(q).and(q1);
						}
					}
				}

				helper.getOptimizer().query(table, q);

				Cursor c1 = helper.cursor(table, q, offset, Data.class);
				if (c1 != null) {
					try {
						while (c1.hasNext()) {
							T e = (T) c1.next();
							if (access != null) {
								access.read("db", table, e);
							}
							if (!func.apply(e)) {
								return false;
							}
						}
					} catch (Exception e) {
						throw e;
					} finally {
						X.close(c1);
					}
				}
				return true;
			}
			return false;
		}

		/**
		 * load a data
		 * 
		 * @param <T>
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public <T> T load() throws SQLException {

			if (log.isDebugEnabled()) {
				log.debug("w=" + this);
			}

			T t1 = null;
			if (dao != null) {
				t1 = (T) dao.load(this);
			} else if (!X.isEmpty(table)) {
				W q = this;
				if (access != null) {
					W q1 = access.filter("db", table);
					if (q1 != null) {
						if (q.isEmpty()) {
							q = q1;
						} else if (!q1.isEmpty()) {
							q = W.create().and(q).and(q1);
						}
					}
				}

				helper.getOptimizer().query(table, q);

				t1 = (T) helper.load(table, q, t, false);
			} else {
				throw new SQLException("not set table");
			}

			if (access != null) {
				access.read("db", table, t1);
			}

			return t1;

		}

		/**
		 * load data and trace
		 * 
		 * @param <T>
		 * @param trace, true trace or no
		 * @return
		 * @throws SQLException
		 */
		@SuppressWarnings("unchecked")
		public <T> T load(boolean trace) throws SQLException {

			if (log.isDebugEnabled()) {
				log.debug("w=" + this);
			}

			T t1 = null;
			if (dao != null) {
				t1 = (T) dao.load(this);
			} else if (!X.isEmpty(table)) {
				W q = this;
				if (access != null) {
					W q1 = access.filter("db", table);
					if (q1 != null) {
						if (q.isEmpty()) {
							q = q1;
						} else if (!q1.isEmpty()) {
							q = W.create().and(q).and(q1);
						}
					}
				}

				helper.getOptimizer().query(table, q);

				t1 = (T) helper.load(table, q, t, trace);
			} else {
				throw new SQLException("not set table");
			}

			if (access != null) {
				access.read("db", table, t1);
			}

			return t1;

		}

		/**
		 * atomic load the data and call back the func
		 * 
		 * @param <T>
		 * @param func
		 * @return
		 * @throws SQLException
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public <T> T load(Consumer<T> func) throws SQLException {

			T t1 = null;
			if (dao != null) {
				t1 = (T) dao.load(this, (Consumer) func);
			} else if (!X.isEmpty(table)) {
				Lock door = Global.getLock("data." + table);
				door.lock();
				try {
					W q = this;
					if (access != null) {
						W q1 = access.filter("db", table);
						if (q1 != null) {
							if (q.isEmpty()) {
								q = q1;
							} else if (!q1.isEmpty()) {
								q = W.create().and(q).and(q1);
							}
						}
					}

					helper.getOptimizer().query(table, q);

					t1 = (T) helper.load(table, q, t, false);
					if (t1 != null && func != null) {
						func.accept(t1);
					}
				} finally {
					door.unlock();
				}
			} else {
				throw new SQLException("not set table");
			}

			if (access != null) {
				access.read("db", table, t1);
			}

			return t1;

		}

		/**
		 * load a data by skip offset
		 * 
		 * @param <T>
		 * @param offset
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public <T> T load(int offset) throws SQLException {
			Beans<?> l1 = load(offset, 1);
			return (T) (l1 == null || l1.isEmpty() ? null : l1.get(0));
		}

		/**
		 * get the value of the field
		 * 
		 * @param <T>
		 * @param name the field name
		 * @return the T
		 */
		@SuppressWarnings("unchecked")
		public <T> T get(String name) throws SQLException {
			Bean b = load();
			return b == null ? null : (T) b.get(name);
		}

		public long getLong(String name) throws SQLException {
			Bean b = load();
			return b == null ? 0 : b.getLong(name);
		}

		public long getLong(int offset, String name) throws SQLException {
			Bean b = load(offset);
			return b == null ? 0 : b.getLong(name);
		}

		public double getDouble(String name) throws SQLException {
			Bean b = load();
			return b == null ? 0 : b.getDouble(name);
		}

		public double getDouble(int offset, String name) throws SQLException {
			Bean b = load(offset);
			return b == null ? 0 : b.getDouble(name);
		}

		public List<W> getQuery() {
			return queryList;
		}

		/**
		 * get the value of the field, offset the row
		 * 
		 * @param <T>
		 * @param offset the row offset
		 * @param name   the field name
		 * @return the Object
		 */
		@SuppressWarnings("unchecked")
		public <T> T get(int offset, String name) throws SQLException {
			Bean b = load(offset);
			return b == null ? null : (T) b.get(name);
		}

		/**
		 * load data list
		 * 
		 * @param <T>
		 * @param s
		 * @param n
		 * @return
		 * @throws SQLException
		 */
		@SuppressWarnings("unchecked")
		public <T extends Bean> Beans<T> load(int s, int n) throws SQLException {

			if (log.isDebugEnabled()) {
				log.debug("w=" + this);
			}

			Beans<T> l1 = null;

			if (dao != null) {
				try {
					l1 = (Beans<T>) dao.load(this, s, n);
				} finally {
					if (l1 != null) {
						l1.dao = dao;
					}
				}
			} else if (!X.isEmpty(table)) {
				W q = this;
				if (access != null) {
					W q1 = access.filter("db", table);
					if (q1 != null) {
						if (q.isEmpty()) {
							q = q1;
						} else if (!q1.isEmpty()) {
							q = W.create().and(q).and(q1);
						}
					}
				}

				helper.getOptimizer().query(table, q);

				try {
					l1 = helper.load(table, q, s, n, t);
				} finally {
					if (l1 != null) {
						l1.table = table;
						l1.q = this;
					}
				}
			} else {
				throw new SQLException("not set table");
			}

			if (access != null) {
				access.read("db", table, l1);
			}

			return l1;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public <T extends Bean> Beans<T> load(int s, int n, Consumer<Beans<T>> func) throws SQLException {
			if (log.isDebugEnabled())
				log.debug("w=" + this);

			Beans<T> l1 = null;

			if (dao != null) {
				l1 = (Beans<T>) dao.load(this, s, n, (Consumer) func);
				if (l1 != null)
					l1.dao = dao;
			} else if (!X.isEmpty(table)) {

				Lock door = Global.getLock("data." + table);
				door.lock();
				try {
					W q = this;
					if (access != null) {
						W q1 = access.filter("db", table);
						if (q1 != null) {
							if (q.isEmpty()) {
								q = q1;
							} else if (!q1.isEmpty()) {
								q = W.create().and(q).and(q1);
							}
						}
					}

					helper.getOptimizer().query(table, q);

					l1 = helper.load(table, q, s, n, t);
					l1.table = table;
					l1.q = this;
					if (func != null && l1 != null && !l1.isEmpty()) {
						func.accept(l1);
					}

				} finally {
					door.unlock();
				}
			} else {
				throw new SQLException("not set table");
			}

			if (access != null) {
				access.read("db", table, l1);
			}

			return l1;
		}

		public long count() throws SQLException {
			if (dao != null) {
				return dao.count(this);
			} else if (!X.isEmpty(table)) {
				W q = this;
				if (access != null) {
					W q1 = access.filter("db", table);
					if (q1 != null) {
						if (q.isEmpty()) {
							q = q1;
						} else if (!q1.isEmpty()) {
							q = W.create().and(q).and(q1);
						}
					}
				}

				helper.getOptimizer().query(table, q);

				return helper.count(table, q);
			}

			throw new SQLException("not set table");

		}

		public <T> T sum(String name) throws SQLException {
			if (dao != null) {
				return dao.sum(name, this);
			} else if (!X.isEmpty(table)) {
				if (access == null || access.read("db", table, name)) {
					W q = this;
					if (access != null) {
						W q1 = access.filter("db", table);
						if (q1 != null) {
							if (q.isEmpty()) {
								q = q1;
							} else if (!q1.isEmpty()) {
								q = W.create().and(q).and(q1);
							}
						}
					}

					helper.getOptimizer().query(table, q);

					return helper.sum(table, q, name);
				} else {
					throw new SQLException("no privilege!");
				}
			}
			throw new SQLException("not set table");

		}

		public <T> T avg(String name) throws SQLException {
			if (dao != null) {
				return dao.avg(name, this);
			} else if (!X.isEmpty(table)) {
				if (access == null || access.read("db", table, name)) {
					W q = this;
					if (access != null) {
						W q1 = access.filter("db", table);
						if (q1 != null) {
							if (q.isEmpty()) {
								q = q1;
							} else if (!q1.isEmpty()) {
								q = W.create().and(q).and(q1);
							}
						}
					}

					helper.getOptimizer().query(table, q);

					return helper.avg(table, q, name);
				} else {
					throw new SQLException("no privilege!");
				}
			}
			throw new SQLException("not set table");

		}

		@SuppressWarnings("unchecked")
		public <T> T min(String name) throws SQLException {
			if (dao != null) {
				return dao.min(name, this);
			} else if (!X.isEmpty(table)) {
				if (access == null || access.read("db", table, name)) {
					W q = this;
					if (access != null) {
						W q1 = access.filter("db", table);
						if (q1 != null) {
							if (q.isEmpty()) {
								q = q1;
							} else if (!q1.isEmpty()) {
								q = W.create().and(q).and(q1);
							}
						}
					}

					q.sort(name);

					helper.getOptimizer().query(table, q);

					Data d = helper.load(table, q, Data.class, false);
					if (d != null) {
						return (T) d.get(name);
					}
					return null;
				} else {
					throw new SQLException("no privilege!");
				}
			}
			throw new SQLException("not set table");

		}

		@SuppressWarnings("unchecked")
		public <T> T max(String name) throws SQLException {
			if (dao != null) {
				return dao.max(name, this);
			} else if (!X.isEmpty(table)) {
				if (access == null || access.read("db", table, name)) {
					W q = this;
					if (access != null) {
						W q1 = access.filter("db", table);
						if (q1 != null) {
							if (q.isEmpty()) {
								q = q1;
							} else if (!q1.isEmpty()) {
								q = W.create().and(q).and(q1);
							}
						}
					}

					q.sort(name, -1);

					helper.getOptimizer().query(table, q);

					Data d = helper.load(table, q, Data.class, false);
					if (d != null) {
						return (T) d.get(name);
					}
					return null;
				} else {
					throw new SQLException("no privilege!");
				}
			}
			throw new SQLException("not set table");

		}

		public <T> T median(String name) throws SQLException {
			if (dao != null) {
				return dao.median(name, this);
			} else if (!X.isEmpty(table)) {
				if (access == null || access.read("db", table, name)) {
					W q = this;
					if (access != null) {
						W q1 = access.filter("db", table);
						if (q1 != null) {
							if (q.isEmpty()) {
								q = q1;
							} else if (!q1.isEmpty()) {
								q = W.create().and(q).and(q1);
							}
						}
					}

					helper.getOptimizer().query(table, q);

					return helper.median(table, q, name);

				} else {
					throw new SQLException("no privilege!");
				}
			}
			throw new SQLException("not set table");

		}

		public int delete() throws SQLException {
			if (dao != null) {
				return dao.delete(this);
			} else if (!X.isEmpty(table)) {
				if (access == null || access.checkWrite("db", table, null)) {

					W q = this;
					if (access != null) {
						W q1 = access.filter("db", table);
						if (q1 != null) {
							if (q.isEmpty()) {
								q = q1;
							} else if (!q1.isEmpty()) {
								q = W.create().and(q).and(q1);
							}
						}
					}

					helper.getOptimizer().query(table, q);

					return helper.delete(table, q);
				} else {
					throw new SQLException("no privilege!");
				}
			}
			throw new SQLException("not set table");
		}

		public int update(Object o) throws SQLException {

			V v = null;
			if (o instanceof V) {
				v = (V) o;
			} else {
				JSON j1 = JSON.fromObject(o);
				v = V.fromJSON(j1);
			}

			if (dao != null) {
				return dao.update(this, v);
			} else if (!X.isEmpty(table)) {

				if (access == null || access.checkWrite("db", table, v)) {
					W q = this;
					if (access != null) {
						W q1 = access.filter("db", table);
						if (q1 != null) {
							if (q.isEmpty()) {
								q = q1;
							} else if (!q1.isEmpty()) {
								q = W.create().and(q).and(q1);
							}
						}
					}

					helper.getOptimizer().query(table, q);

					return helper.updateTable(table, q, v);
				} else {
					throw new SQLException("no privilege!");
				}
			}
			throw new SQLException("not set table");
		}

		public List<?> distinct(String name) throws SQLException {
			if (dao != null) {
				return dao.distinct(name, this);
			} else if (!X.isEmpty(table)) {
				if (access == null || access.read("db", table, name)) {
					W q = this;
					if (access != null) {
						W q1 = access.filter("db", table);
						if (q1 != null) {
							if (q.isEmpty()) {
								q = q1;
							} else if (!q1.isEmpty()) {
								q = W.create().and(q).and(q1);
							}
						}
					}

					helper.getOptimizer().query(table, q);

					return helper.distinct(table, name, q);
				} else {
					throw new SQLException("no privilege!");
				}
			}
			throw new SQLException("not set table");
		}

		public List<JSON> count(String group, int n) throws SQLException {
			if (dao != null) {
				return dao.count(this, X.split(group, "[,]"), n);
			} else if (!X.isEmpty(table)) {
				if (access == null || access.read("db", table, group)) {
					W q = this;
					if (access != null) {
						W q1 = access.filter("db", table);
						if (q1 != null) {
							if (q.isEmpty()) {
								q = q1;
							} else if (!q1.isEmpty()) {
								q = W.create().and(q).and(q1);
							}
						}
					}

					helper.getOptimizer().query(table, q);

					return helper.count(table, q, X.split(group, "[,]"), n);
				} else {
					throw new SQLException("no privilege!");
				}
			}
			throw new SQLException("not set table");

		}

		public List<JSON> count(String name, String group, int n) throws SQLException {
			if (dao != null) {
				return dao.count(this, X.split(group, "[,]"), n);
			} else if (!X.isEmpty(table)) {
				if (access == null || access.read("db", table, group)) {
					W q = this;
					if (access != null) {
						W q1 = access.filter("db", table);
						if (q1 != null) {
							if (q.isEmpty()) {
								q = q1;
							} else if (!q1.isEmpty()) {
								q = W.create().and(q).and(q1);
							}
						}
					}

					helper.getOptimizer().query(table, q);

					return helper.count(table, q, name, X.split(group, "[,]"), n);
				} else {
					throw new SQLException("no privilege!");
				}
			}
			throw new SQLException("not set table");

		}

		public List<JSON> sum(String name, String group) throws SQLException {
			if (dao != null) {
				return dao.sum(this, name, X.split(group, "[,]"));
			} else if (!X.isEmpty(table)) {
				if (access == null || access.read("db", table, Arrays.asList(name, group))) {
					W q = this;
					if (access != null) {
						W q1 = access.filter("db", table);
						if (q1 != null) {
							if (q.isEmpty()) {
								q = q1;
							} else if (!q1.isEmpty()) {
								q = W.create().and(q).and(q1);
							}
						}
					}

					helper.getOptimizer().query(table, q);

					return helper.sum(table, q, name, X.split(group, "[,]"));
				} else {
					throw new SQLException("no privilege!");
				}
			}
			throw new SQLException("not set table");

		}

		/**
		 * 分类统计
		 * 
		 * @param name
		 * @param group
		 * @return
		 * @throws SQLException
		 */
		public List<JSON> aggregate(String name, String group) throws SQLException {
			if (dao != null) {
				return dao.aggregate(this, X.split(name, "[,]"), X.split(group, "[,]"));
			} else if (!X.isEmpty(table)) {
				if (access == null || access.read("db", table, Arrays.asList(name, group))) {
					W q = this;
					if (access != null) {
						W q1 = access.filter("db", table);
						if (q1 != null) {
							if (q.isEmpty()) {
								q = q1;
							} else if (!q1.isEmpty()) {
								q = W.create().and(q).and(q1);
							}
						}
					}

					helper.getOptimizer().query(table, q);

					return helper.aggregate(table, X.split(name, "[,]"), q, X.split(group, "[,]"));
				} else {
					throw new SQLException("no privilege!");
				}
			}
			throw new SQLException("not set table");

		}

//		public List<JSON> min(String name, String group) throws SQLException {
//			if (dao != null) {
//				return dao.min(this, name, X.split(group, "[,]"));
//			} else if (!X.isEmpty(table)) {
//				if (access == null || access.read(table, Arrays.asList(name, group))) {
//					W q = this;
//					if (access != null) {
//						W q1 = access.filter(table);
//						if (q1 != null) {
//							if (q.isEmpty()) {
//								q = q1;
//							} else if (!q1.isEmpty()) {
//								q = W.create().and(q).and(q1);
//							}
//						}
//					}
//
//					// TODO
//
////					List<JSON> l1 = JSON.createList();
////					for (String s : X.split(group, "[,]")) {
////						Data d = helper.load(table, null, q.copy().sort(name), Data.class, Helper.DEFAULT);
////						if (d != null) {
////							l1.add(JSON.create().append(s, d.get(name)));
////						}
////					}
////					return l1;
//
//				} else {
//					throw new SQLException("no privilege!");
//				}
//			}
//			throw new SQLException("not set table");
//
//		}

//		public List<JSON> max(String name, String group) throws SQLException {
//			if (dao != null) {
//				return dao.max(this, name, X.split(group, "[,]"));
//			} else if (!X.isEmpty(table)) {
//				if (access == null || access.read(table, Arrays.asList(name, group))) {
//					W q = this;
//					if (access != null) {
//						W q1 = access.filter(table);
//						if (q1 != null) {
//							if (q.isEmpty()) {
//								q = q1;
//							} else if (!q1.isEmpty()) {
//								q = W.create().and(q).and(q1);
//							}
//						}
//					}
//
//					// TODO
//
////					return helper.max(table, q, name, X.split(group, "[,]"), Helper.DEFAULT);
//				} else {
//					throw new SQLException("no privilege!");
//				}
//			}
//			throw new SQLException("not set table");
//
//		}

		public List<JSON> avg(String name, String group) throws SQLException {
			if (dao != null) {
				return dao.avg(this, name, X.split(group, "[,]"));
			} else if (!X.isEmpty(table)) {
				if (access == null || access.read("db", table, Arrays.asList(name, group))) {
					W q = this;
					if (access != null) {
						W q1 = access.filter("db", table);
						if (q1 != null) {
							if (q.isEmpty()) {
								q = q1;
							} else if (!q1.isEmpty()) {
								q = W.create().and(q).and(q1);
							}
						}
					}

					helper.getOptimizer().query(table, q);

					return helper.avg(table, q, name, X.split(group, "[,]"));
				} else {
					throw new SQLException("no privilege!");
				}
			}
			throw new SQLException("not set table");

		}

		W dao(BeanDAO<?, ?> dao) {
			this.dao = dao;
			return this;
		}

		public W query(String table) {
			this.table = table;
			return this;
		}

		public <T extends Bean> W bean(Class<T> clazz) {
			this.t = clazz;
			return this;
		}

	}

	/**
	 * load the data by id of X.ID
	 * 
	 * @param <T> the subclass of Bean
	 * @param id  the id
	 * @param t   the Class of Bean
	 * @return the Bean
	 * @throws SQLException
	 */
	public static <T extends Bean> T load(Object id, Class<T> t) throws SQLException {
		return load(W.create().and(X.ID, id), t);
	}

	/**
	 * Load.
	 * 
	 * @param <T> the generic type
	 * @param q   the q
	 * @param t   the t
	 * @param db  the db
	 * @return the t
	 * @throws SQLException
	 */
	static <T extends Bean> T load(W q, Class<T> t) throws SQLException {
		String table = getTable(t);
		return load(table, q, t);
	}

	static <T extends Bean> T load(W q, Class<T> t, boolean trace) throws SQLException {
		String table = getTable(t);
		return load(table, q, t);
	}

	/**
	 * load data from the table.
	 *
	 * @param <T>   the subclass of Bean
	 * @param table the table name
	 * @param q     the query and order
	 * @param t     the subclass of Bean
	 * @return the bean
	 */
	public static <T extends Bean> T load(String table, W q, Class<T> t) {
		return load(table, q, t, false);
	}

	public static <T extends Bean> T load(String table, W q, Class<T> t, boolean trace) {

		TimeStamp t1 = TimeStamp.create();
		try {

			if (monitor != null) {
				monitor.query(table, q);
			}

			return primary.load(table, q, t, trace);

		} finally {
			read.add(t1.pastms());
		}
	}

	public static <T extends Bean> Beans<T> load(String table, W q, int s, int n, Class<T> t) throws SQLException {

		if (!Helper.isConfigured()) {
			return null;
		}

		TimeStamp t1 = TimeStamp.create();
		try {

			if (monitor != null) {
				monitor.query(table, q);
			}

			return primary.load(table, q, s, n, t);

		} finally {
			read.add(t1.pastms());
		}
	}

	/**
	 * batch insert
	 * 
	 * @param table
	 * @param values
	 * @return
	 */
	public static int insert(String table, List<V> values) {

		TimeStamp t1 = TimeStamp.create();
		try {

			for (V value : values) {
				value.set(X.CREATED, System.currentTimeMillis()).set(X.UPDATED, System.currentTimeMillis());

			}

			return primary.insertTable(table, values);
		} finally {
			write.add(t1.pastms());
		}
	}

	/**
	 * insert
	 * 
	 * @param value the value
	 * @param table the table
	 * @param db    the db name
	 * @return the number inserted
	 */
	public static int insert(String table, V value) {

		TimeStamp t1 = TimeStamp.create();
		try {

			return primary.insertTable(table, value);

		} finally {
			write.add(t1.pastms());
		}
	}

	/**
	 * update the table by the query with the values.
	 *
	 * @param table  the table
	 * @param q      the query
	 * @param values the values
	 * @return the number of updated
	 */
	public static int update(String table, W q, V values) {

		TimeStamp t1 = TimeStamp.create();
		try {

			if (monitor != null) {
				monitor.query(table, q);
			}

			return primary.updateTable(table, q, values);

		} finally {
			write.add(t1.pastms());
		}
	}

	/**
	 * increase
	 * 
	 * @param table
	 * @param name
	 * @param n
	 * @param q
	 * @param v
	 * @return
	 */
	public static int inc(String table, String name, int n, W q, V v) {

		TimeStamp t1 = TimeStamp.create();
		try {
			if (monitor != null) {
				monitor.query(table, q);
			}

			return primary.inc(table, q, name, n, v);
		} finally {
			write.add(t1.pastms());

		}
	}

	/**
	 * test is configured RDS or Mongo
	 * 
	 * @return the boolean, <br>
	 *         true: configured RDS or Mongo; false: no DB configured
	 */
	public static boolean isConfigured() {
		return primary != null;
	}

	public static <T extends Bean> Cursor<T> stream(String table, W q, long offset, Class<T> t) {

		TimeStamp t1 = TimeStamp.create();
		try {

			if (monitor != null) {
				monitor.query(table, q);
			}

			Cursor<T> cur = null;
			cur = primary.cursor(table, q, offset, t);

			return cur;
		} finally {
			read.add(t1.pastms());

		}

	}

	/**
	 * count the table by query
	 * 
	 * @param table
	 * @param q
	 * @return
	 */
	public static long count(String table, W q) {

		TimeStamp t1 = TimeStamp.create();
		try {

			if (monitor != null) {
				monitor.query(table, q);
			}

			return primary.count(table, q);
		} finally {
			read.add(t1.pastms());

		}
	}

	/**
	 * count
	 * 
	 * @param table
	 * @param name
	 * @param q
	 * @return
	 */
	public static long count(String table, String name, W q) {

		TimeStamp t1 = TimeStamp.create();
		try {

			if (monitor != null) {
				monitor.query(table, q);
			}

			return primary.count(table, q, name);
		} finally {
			read.add(t1.pastms());

		}
	}

	/**
	 * 
	 * @param <T>
	 * @param q
	 * @param name
	 * @param table
	 * @param db
	 * @return
	 */
	public static <T> T median(String table, String name, W q) {
		TimeStamp t1 = TimeStamp.create();
		try {

			if (monitor != null) {
				monitor.query(table, q);
			}

			return primary.median(table, q, name);
		} finally {
			read.add(t1.pastms());

		}
	}

	/**
	 * @param <T>
	 * @param q
	 * @param name
	 * @param table
	 * @param db
	 * @return
	 */
	public static <T> T std_deviation(W q, String name, String table) {
		TimeStamp t1 = TimeStamp.create();
		try {
			return primary.std_deviation(table, q, name);
		} finally {
			read.add(t1.pastms());

			if (t1.pastms() > Optimizer.MIN && monitor != null) {
				monitor.query(table, q);
			}

		}
	}

	/**
	 * sum
	 * 
	 * @param <T>
	 * @param table
	 * @param name
	 * @param q
	 * @return
	 */
	public static <T> T sum(String table, String name, W q) {

		TimeStamp t1 = TimeStamp.create();
		try {

			if (monitor != null) {
				monitor.query(table, q);
			}

			return primary.sum(table, q, name);
		} finally {
			read.add(t1.pastms());

		}
	}

	/**
	 * max
	 * 
	 * @param <T>
	 * @param table
	 * @param name
	 * @param q
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T max(String table, String name, W q) {
		Data d = load(table, q.copy().sort(name, -1), Data.class);
		if (d != null) {
			return (T) d.get(name);
		}
		return null;
	}

	/**
	 * min
	 * 
	 * @param <T>
	 * @param q
	 * @param name
	 * @param table
	 * @param db
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T min(String table, String name, W q) {
		Data d = load(table, q.copy().sort(name), Data.class);
		if (d != null) {
			return (T) d.get(name);
		}
		return null;
//		return min(q, name, table, Helper.DEFAULT);
	}

	/**
	 * average
	 * 
	 * @param <T>
	 * @param table
	 * @param name
	 * @param q
	 * @return
	 */
	public static <T> T avg(String table, String name, W q) {

		TimeStamp t1 = TimeStamp.create();
		try {

			if (monitor != null) {
				monitor.query(table, q);
			}

			return primary.avg(table, q, name);
		} finally {
			read.add(t1.pastms());

		}
	}

	public static List<JSON> avg(String table, String name, W q, String[] group) {

		TimeStamp t1 = TimeStamp.create();
		try {

			if (monitor != null) {
				monitor.query(table, q);
			}

			return primary.avg(table, q, name, group);
		} finally {
			read.add(t1.pastms());

		}
	}

	/**
	 * distinct the name by query
	 * 
	 * @param table
	 * @param name
	 * @param q
	 * @return
	 */
	public static List<?> distinct(String table, String name, W q) {

		TimeStamp t1 = TimeStamp.create();
		try {

			if (monitor != null) {
				monitor.query(table, q);
			}

			return primary.distinct(table, name, q);

		} finally {
			read.add(t1.pastms());

		}
	}

	/**
	 * 
	 */
	public static void enableOptmizer() {
		if (Global.getInt("db.optimizer", 1) == 1) {
			monitor = new Optimizer(Helper.primary);
		} else {
			monitor = null;
		}
	}

	public static void createIndex(String table, LinkedHashMap<String, Object> ss, boolean unique) {

		TimeStamp t1 = TimeStamp.create();
		try {

			primary.createIndex(table, ss, unique);
		} finally {
			write.add(t1.pastms());
		}
	}

	/**
	 * get indexes
	 * 
	 * @param table
	 * @return
	 */
	public static List<Map<String, Object>> getIndexes(String table) {

		TimeStamp t1 = TimeStamp.create();
		try {

			return primary.getIndexes(table);
		} finally {
			read.add(t1.pastms());
		}

	}

	public static void dropIndex(String table, String name) {

		TimeStamp t1 = TimeStamp.create();
		try {
			primary.dropIndex(table, name);
		} finally {
			write.add(t1.pastms());
		}
	}

	/**
	 * set primary db helper
	 * 
	 * @param helper the db helper
	 */
	public static void setPrimary(DBHelper helper) {
		primary = helper;

		log.info("primary=" + primary);
	}

	/**
	 * add a custom db helper which works if primary has not that db
	 * 
	 * @param helper the db helper
	 */
	public static void setHelper(DBHelper helper) {
		primary = helper;
	}

	/**
	 * the DBHelper interface
	 * 
	 * @author wujun
	 *
	 */
	public interface DBHelper {

		boolean isConfigured();

		void repair(String table);

		List<Map<String, Object>> getIndexes(String table);

		void dropIndex(String table, String name);

		void createIndex(String table, LinkedHashMap<String, Object> ss, boolean unique);

		<T extends Bean> Beans<T> load(String table, W q, int s, int n, Class<T> t) throws SQLException;

		<T extends Bean> Cursor<T> cursor(String table, W q, long offset, Class<T> t);

		<T extends Bean> T load(String table, W q, Class<T> clazz);

		<T extends Bean> T load(String table, W q, Class<T> clazz, boolean trace);

		int delete(String table, W q);

		void drop(String table);

		boolean exists(String table, W q);

		int insertTable(String table, V value);

		int insertTable(String table, List<V> values);

		int updateTable(String table, W q, V values);

		int inc(String table, W q, String name, int n, V v);

		long count(String table, W q, String name);

		long count(String table, W q);

		List<JSON> count(String table, W q, String[] group, int n);

		List<JSON> count(String table, W q, String name, String[] group, int n);

		<T> T max(String table, W q, String name);

		<T> T min(String table, W q, String name);

		<T> T sum(String table, W q, String name);

		List<JSON> sum(String table, W q, String name, String[] group);

		<T> T avg(String table, W q, String name);

		<T> T std_deviation(String table, W q, String name);

		<T> T median(String table, W q, String name);

		List<JSON> avg(String table, W q, String name, String[] group);

		List<?> distinct(String table, String name, W q);

		List<JSON> aggregate(String table, String[] func, W q, String[] group);

		List<JSON> listTables();

		void close();

		List<JSON> getMetaData(String tablename);

		void repair();

		List<JSON> listDB();

		void killOp(Object id);

		/**
		 * 获取优化器
		 * 
		 * @return
		 */
		Optimizer getOptimizer();

		/**
		 * 当前操作
		 * 
		 * @return
		 */
		List<JSON> listOp();

		/**
		 * 数据表存储大小
		 * 
		 * @param table
		 * @return
		 */
		long size(String table);

		/**
		 * 数据库统计信息
		 * 
		 * @param table
		 * @return
		 */
		JSON stats(String table);

		/**
		 * 数据库性能状态
		 * 
		 * @return
		 */
		JSON status();

		/**
		 * 设置分布式表
		 * 
		 * @param table
		 * @return
		 */
		boolean distributed(String table, String key);

		boolean createTable(String table, JSON cols);

		long getTime();

	}

	public static DBHelper getPrimary() {
		return primary;
	}

	public static abstract class Cursor<E> implements Iterator<E>, Closeable {

		public abstract void close();

		public void forEach(Function<E, Boolean> func) {

			try {
				while (this.hasNext()) {
					if (!func.apply(this.next())) {
						return;
					}
				}
			} finally {
				this.close();
			}
		}

	}

	/**
	 * drop table
	 * 
	 * @param table
	 */
	public static void drop(String table) {

		TimeStamp t1 = TimeStamp.create();
		try {
			primary.drop(table);
		} finally {
			write.add(t1.pastms());
		}
	}

	/**
	 * count
	 * 
	 * @param table
	 * @param q
	 * @param group
	 * @param n
	 * @return
	 */
	public static List<JSON> count(String table, W q, String[] group, int n) {

		TimeStamp t1 = TimeStamp.create();
		try {

			if (monitor != null) {
				monitor.query(table, q);
			}

			return primary.count(table, q, group, n);
		} finally {
			read.add(t1.pastms());
		}
	}

	/**
	 * count
	 * 
	 * @param table
	 * @param name
	 * @param q
	 * @param group
	 * @param n
	 * @return
	 */
	public static List<JSON> count(String table, String name, W q, String[] group, int n) {
		TimeStamp t1 = TimeStamp.create();
		try {
			if (monitor != null) {
				monitor.query(table, q);
			}

			return primary.count(table, q, name, group, n);
		} finally {
			read.add(t1.pastms());
		}
	}

	public static long getTime() {
		return primary.getTime();
	}

	/**
	 * sum
	 * 
	 * @param table
	 * @param name
	 * @param q
	 * @param group
	 * @return
	 */
	public static List<JSON> sum(String table, String name, W q, String[] group) {

		TimeStamp t1 = TimeStamp.create();
		try {

			if (monitor != null) {
				monitor.query(table, q);
			}

			return primary.sum(table, q, name, group);
		} finally {
			read.add(t1.pastms());
		}
	}

	/**
	 * aggregate
	 * 
	 * @param table
	 * @param func
	 * @param q
	 * @param group
	 * @return
	 */
	public static List<JSON> aggregate(String table, String[] func, W q, String[] group) {

		TimeStamp t1 = TimeStamp.create();
		try {

			if (monitor != null) {
				monitor.query(table, q);
			}

			return primary.aggregate(table, func, q, group);
		} finally {
			read.add(t1.pastms());
		}
	}

	/**
	 * min
	 * 
	 * @param table
	 * @param name
	 * @param q
	 * @param group
	 * @return
	 */
	public static List<JSON> min(String table, String name, W q, String[] group) {
//		return min(table, q, name, group, Helper.DEFAULT);
		// TODO
		return null;
	}

	private static Counter read = new Counter("read");
	private static Counter write = new Counter("write");

	public static Counter.Stat statRead() {
		return read.get();
	}

	public static Counter.Stat statWrite() {
		return write.get();
	}

	public static List<JSON> listTables() {
		TimeStamp t1 = TimeStamp.create();
		try {
			return primary.listTables();
		} finally {
			read.add(t1.pastms());
		}
	}

	public static List<JSON> listDB() {
		TimeStamp t1 = TimeStamp.create();
		try {
			return primary.listDB();
		} finally {
			read.add(t1.pastms());
		}
	}

	public static List<JSON> listOp() {
		TimeStamp t1 = TimeStamp.create();
		try {
			return primary.listOp();
		} finally {
			read.add(t1.pastms());
		}
	}

	/**
	 * size of the table
	 * 
	 * @param table
	 * @return
	 */
	public static long size(String table) {

		TimeStamp t1 = TimeStamp.create();
		try {
			return primary.size(table);
		} finally {
			read.add(t1.pastms());
		}
	}

	public static JSON dbstats() {
		return stats(null);
	}

	public static JSON tablestats(String table) {
		return stats(table);
	}

	public static JSON stats(String table) {

		TimeStamp t1 = TimeStamp.create();
		try {

			return primary.stats(table);
		} finally {
			read.add(t1.pastms());
		}
	}

	public static void init(String name) {

		for (String key : new String[] { "id" }) {
			LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
			m.put(key, 1);
			Helper.createIndex(name, m, true);
		}

		for (String key : new String[] { "created", "updated" }) {
			LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
			m.put(key, 1);
			Helper.createIndex(name, m, false);

			m = new LinkedHashMap<String, Object>();
			m.put(key, -1);
			Helper.createIndex(name, m, false);
		}

	}

	public static JSON status() {
		TimeStamp t1 = TimeStamp.create();
		try {

			return primary.status();
		} finally {
			read.add(t1.pastms());
		}
	}

	public static void optimize(String name, W q) {
		if (monitor != null) {
			monitor.optimize(name, q);
		}
	}

	public static String getTable(Class<? extends Bean> t) throws SQLException {
		Table table = (Table) t.getAnnotation(Table.class);
		if (table == null || X.isEmpty(table.name())) {
			throw new SQLException("table missed/error in [" + t + "] declaretion");
		}

		return table.name();
	}

	public static boolean createTable(String table, JSON cols) {
		return primary.createTable(table, cols);
	}

	public static boolean distributed(String table, String key) {
		return primary.distributed(table, key);
	}

	public static void killOp(Object id) {
		TimeStamp t1 = TimeStamp.create();
		try {
			primary.killOp(id);
		} finally {
			read.add(t1.pastms());
		}

	}

}
