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

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.helper.MongoHelper;
import org.giiwa.core.bean.helper.RDB;
import org.giiwa.core.bean.helper.RDSHelper;
import org.giiwa.core.json.JSON;
import org.giiwa.core.task.Task;
import org.giiwa.framework.web.Model;

import com.mongodb.BasicDBObject;

/**
 * The {@code Helper} Class is utility class for all database operation.
 * 
 */
public class Helper implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** The log utility. */
	protected static Log log = LogFactory.getLog(Helper.class);

	private static IOptimizer monitor = null;

	/**
	 * map<db_table, list>
	 */
	private static Map<String, List<ITrigger>> triggers = new HashMap<String, List<ITrigger>>();

	/**
	 * the primary database when there are multiple databases
	 */
	public static DBHelper primary = null;
	private static List<DBHelper> customs = null;

	public static final String DEFAULT = "default";

	/** The conf. */
	protected static Configuration conf;

	/**
	 * initialize the Bean with the configuration.
	 * 
	 * @param conf
	 *            the conf
	 */
	public static void init(Configuration conf) {

		/**
		 * initialize the DB connections pool
		 */
		if (conf == null)
			return;

		Helper.conf = conf;

		RDB.init();

		String p = conf.getString("primary.db", X.EMPTY);

		if (X.isSame("mongo", p)) {
			primary = MongoHelper.inst;
		} else if (X.isSame("rds", p)) {
			primary = RDSHelper.inst;
		}

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

	/**
	 * add a trigger on a db and table
	 * 
	 * @param db
	 *            the db
	 * @param table
	 *            the table
	 * @param t
	 *            the trigger
	 */
	public static void addTrigger(String db, String table, ITrigger t) {
		String name = db + "_" + table;
		List<ITrigger> l1 = triggers.get(name);
		if (l1 == null) {
			l1 = new ArrayList<ITrigger>();
			triggers.put(name, l1);
		}

		if (!l1.contains(t)) {
			l1.add(t);
		}
	}

	/**
	 * add trigger on "default" and the Bean
	 * 
	 * @param bean
	 *            the bean
	 * @param t
	 *            the trigger
	 */
	public static void addTrigger(Class<? extends Bean> bean, ITrigger t) {
		addTrigger(getDB(bean), bean, t);
	}

	/**
	 * add trigger on db and the Bean
	 * 
	 * @param db
	 *            the db name
	 * @param bean
	 *            the Bean
	 * @param t
	 *            the Trigger
	 */
	public static void addTrigger(String db, Class<? extends Bean> bean, ITrigger t) {
		String table = getTable(bean);
		addTrigger(db, table, t);
	}

	/**
	 * remove a trigger from all db and table
	 * 
	 * @param t
	 *            the trigger
	 */
	public static void removeTrigger(ITrigger t) {
		for (List<ITrigger> l1 : triggers.values()) {
			l1.remove(t);
		}
	}

	private static void beforeInsert(String db, String table, V v) {
		String name = db + "_" + table;
		List<ITrigger> l1 = triggers.get(name);
		if (!X.isEmpty(l1)) {
			ITrigger[] tt = l1.toArray(new ITrigger[l1.size()]);
			for (ITrigger t : tt) {
				t.beforeInsert(db, table, v);
			}
		}
	}

	private static void beforeUpdate(String db, String table, W q, V v) {
		String name = db + "_" + table;
		List<ITrigger> l1 = triggers.get(name);
		if (!X.isEmpty(l1)) {
			ITrigger[] tt = l1.toArray(new ITrigger[l1.size()]);
			for (ITrigger t : tt) {
				t.beforeUpdate(db, table, q, v);
			}
		}
	}

	private static void beforeDelete(String db, String table, W q) {
		String name = db + "_" + table;
		List<ITrigger> l1 = triggers.get(name);
		if (!X.isEmpty(l1)) {
			ITrigger[] tt = l1.toArray(new ITrigger[l1.size()]);
			for (ITrigger t : tt) {
				t.beforeDelete(db, table, q);
			}
		}
	}

	/**
	 * delete a data from database.
	 *
	 * @param id
	 *            the value of "id"
	 * @param t
	 *            the subclass of Bean
	 * @return the number was deleted
	 */
	public static int delete(Object id, Class<? extends Bean> t) {
		return delete(W.create(X.ID, id), t);
	}

	/**
	 * delete the data , return the number that was deleted.
	 *
	 * @param q
	 *            the query
	 * @param t
	 *            the subclass of the Bean
	 * @return the number was deleted
	 */
	public static int delete(W q, Class<? extends Bean> t) {
		return delete(q, t, getDB(t));
	}

	/**
	 * Delete.
	 *
	 * @param q
	 *            the q
	 * @param t
	 *            the t
	 * @param db
	 *            the db
	 * @return the int
	 */
	public static int delete(W q, Class<? extends Bean> t, String db) {
		String table = getTable(t);
		return delete(q, table, db);
	}

	/**
	 * delete the data in table
	 * 
	 * @param q
	 *            the query
	 * @param table
	 *            the table name
	 * @param db
	 *            the db name
	 * @return the items was deleted
	 */
	public static int delete(W q, String table, String db) {
		if (table != null) {
			if (monitor != null) {
				monitor.query(db, table, q);
			}

			/**
			 * trigger the listener
			 */
			beforeDelete(db, table, q);

			if (primary != null && primary.getDB(db) != null) {
				return primary.delete(table, q, db);
			} else if (!X.isEmpty(customs)) {
				for (DBHelper h : customs) {
					return h.delete(table, q, db);
				}
			}

		}
		return 0;
	}

	/**
	 * test if exists for the object.
	 *
	 * @param id
	 *            the value of "id"
	 * @param t
	 *            the subclass of Bean
	 * @return true: exist
	 * @throws SQLException
	 *             throw exception if occur database error
	 */
	public static boolean exists(Object id, Class<? extends Bean> t) throws SQLException {
		return exists(W.create(X.ID, id), t);
	}

	/**
	 * test exists.
	 *
	 * @param q
	 *            the query and order
	 * @param t
	 *            the class of Bean
	 * @return true: exists, false: not exists
	 * @throws SQLException
	 *             throw Exception if the class declaration error or not db
	 *             configured
	 */
	public static boolean exists(W q, Class<? extends Bean> t) throws SQLException {
		return exists(q, t, getDB(t));
	}

	/**
	 * Exists.
	 *
	 * @param q
	 *            the q
	 * @param t
	 *            the t
	 * @param db
	 *            the db
	 * @return true, if successful
	 * @throws SQLException
	 *             the SQL exception
	 */
	public static boolean exists(W q, Class<? extends Bean> t, String db) throws SQLException {
		String table = getTable(t);
		return exists(q, table, db);
	}

	/**
	 * exists testing
	 * 
	 * @param q
	 *            the query
	 * @param table
	 *            the table name
	 * @param db
	 *            the db name
	 * @return the boolean
	 * @throws SQLException
	 */
	public static boolean exists(W q, String table, String db) throws SQLException {

		if (table != null) {
			if (monitor != null) {
				monitor.query(db, table, q);
			}

			if (primary != null && primary.getDB(db) != null) {
				return primary.exists(table, q, db);
			} else if (!X.isEmpty(customs)) {
				for (DBHelper h : customs) {
					if (h.getDB(db) != null) {
						return h.exists(table, q, db);
					}
				}
			}
		}

		throw new SQLException("no db configured, please configure the {giiwa}/giiwa.properites");
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

		private final static Object ignore = new Object();

		/** The list. */
		protected Map<String, Object> m = new LinkedHashMap<String, Object>();

		/**
		 * To json.
		 *
		 * @return the json
		 */
		public JSON toJSON() {
			return JSON.fromObject(m);
		}

		/**
		 * copy the request parameters to V
		 * 
		 * @param m
		 * @param names
		 */
		public V copy(Model m, String... names) {
			if (X.isEmpty(names)) {
				// copy all
				for (String name : m.getNames()) {
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
		 * @param m
		 * @param names
		 */
		public V copyInt(Model m, String... names) {
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
		 * @param j
		 *            the j
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
			for (String s : s1) {
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

		/**
		 * To string.
		 *
		 * @return the string
		 */
		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
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
		 * @param name
		 *            the name
		 * @param v
		 *            the value
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
		 * Sets the value if not exists, ignored if name exists.
		 *
		 * @param name
		 *            the name
		 * @param v
		 *            the value
		 * @return the v
		 */
		public V set(String name, Object v) {
			if (name != null && v != null) {
				if (m.containsKey(name)) {
					return this;
				}
				m.put(name, v);
			}
			return this;
		}

		/**
		 * same as set(String name, Object v) <br>
		 * Sets the value if not exists, ignored if name exists.
		 * 
		 * @param name
		 *            the name
		 * @param v
		 *            the value object
		 * @return the V
		 */
		public V put(String name, Object v) {
			return set(name, v);
		}

		/**
		 * same as set(String name, Object v) <br>
		 * Sets the value if not exists, ignored if name exists.
		 * 
		 * @param name
		 *            the name
		 * @param v
		 *            the value object
		 * @return the V
		 */
		public V append(String name, Object v) {
			return set(name, v);
		}

		/**
		 * Ignore the fields.
		 *
		 * @param name
		 *            the name
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
		 * @param name
		 *            the name
		 * @param v
		 *            the value object
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
		 * @param jo
		 *            the json
		 * @return V
		 */
		public V copy(Map<String, Object> jo) {
			if (jo == null)
				return this;

			for (String s : jo.keySet()) {
				if (jo.containsKey(s)) {
					Object o = jo.get(s);
					String s1 = s.replaceAll("\\.", "_");
					if (X.isEmpty(o)) {
						set(s1, X.EMPTY);
					} else {
						set(s1, o);
					}
				}
			}

			return this;
		}

		/**
		 * copy all in json to this, if names is null, then nothing to copy.
		 *
		 * @param jo
		 *            the json
		 * @param names
		 *            the name string
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
		 * @param v
		 *            the original V
		 * @param names
		 *            the names to copy, if null, then copy all
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
		 * @param name
		 *            the string of name
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
		 * @param name
		 *            the name
		 * @return the v
		 */
		public V remove(String... name) {
			if (name != null && name.length > 0) {
				for (String s : name) {
					m.remove(s);
				}
			}
			return this;
		}

		/**
		 * create and copy a new Value.
		 *
		 * @param v
		 *            the Value object
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
	 * @param arr
	 *            the array objects
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
	public final static class W implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public enum OP {
			eq, gt, gte, lt, lte, like, neq, none,in,exists
		};

		/**
		 * "and"
		 */
		public static final int AND = 9;

		/**
		 * "or"
		 */
		public static final int OR = 10;

		private String connectsql;
		private List<W> wlist = new ArrayList<W>();
		private List<Entity> elist = new ArrayList<Entity>();
		private List<Entity> order = new ArrayList<Entity>();
		private String groupby;

		private int cond = AND;

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

		public List<Entity> getList() {
			return elist;
		}

		public List<W> getW() {
			return wlist;
		}

		public List<Entity> getOrder() {
			return order;
		}

		/**
		 * To json.
		 *
		 * @return the json
		 */
		public JSON toJSON() {
			JSON jo = JSON.create();

			List<JSON> l1 = new ArrayList<JSON>();
			for (W w : wlist) {
				JSON j1 = w.toJSON();
				l1.add(j1);
			}
			jo.put("w", l1);
			l1 = new ArrayList<JSON>();
			for (Entity e : elist) {
				JSON j1 = e.toJSON();
				l1.add(j1);
			}
			jo.put("e", l1);
			l1 = new ArrayList<JSON>();
			for (Entity e : order) {
				JSON j1 = e.toJSON();
				l1.add(j1);
			}
			jo.put("o", l1);
			return jo;
		}

		/**
		 * From json.
		 *
		 * @param jo
		 *            the jo
		 * @return the w
		 */
		public static W fromJSON(JSON jo) {
			W q = W.create();
			List<JSON> l1 = jo.getList("w");
			if (l1 != null && l1.size() > 0) {
				for (JSON j1 : l1) {
					W q1 = W.fromJSON(j1);
					q.wlist.add(q1);
				}
			}

			l1 = jo.getList("e");
			if (l1 != null && l1.size() > 0) {
				for (JSON j1 : l1) {
					Entity e = Entity.fromJSON(j1);
					q.elist.add(e);
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

		/**
		 * remove the conditions from the query.
		 *
		 * @param names
		 *            the names
		 * @return the W
		 */
		public W remove(String... names) {
			if (names != null) {
				for (String name : names) {
					for (int i = elist.size() - 1; i >= 0; i--) {
						Entity e = elist.get(i);
						if (X.isSame(name, e.name)) {
							elist.remove(i);
						}
					}
				}
			}
			return this;
		}

		/**
		 * clone a new W <br>
		 * return a new W.
		 *
		 * @return W
		 */
		public W copy() {
			W w = new W();
			w.cond = cond;

			for (W w1 : wlist) {
				w.wlist.add(w1.copy());
			}

			for (Entity e : elist) {
				w.elist.add(e.copy());
			}

			return w;
		}

		/**
		 * size of the W.
		 *
		 * @return int
		 */
		public int size() {
			int size = elist == null ? 0 : elist.size();
			for (W w : wlist) {
				size += w.size();
			}
			return size;
		}

		/**
		 * is empty
		 * 
		 * @return true if empty
		 */
		public boolean isEmpty() {
			return X.isEmpty(elist) && X.isEmpty(wlist) && X.isEmpty(order);
		}

		/**
		 * create args for the SQL "where" <br>
		 * return the Object[].
		 *
		 * @return Object[]
		 */
		public Object[] args() {
			if (elist.size() > 0 || wlist.size() > 0) {
				List<Object> l1 = new ArrayList<Object>();

				_args(l1);

				return l1.toArray(new Object[l1.size()]);
			}

			return null;
		}

		private void _args(List<Object> list) {
			for (Entity e : elist) {
				e.args(list);
			}

			for (W w1 : wlist) {
				w1._args(list);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return elist == null ? X.EMPTY : "{" + where() + "}=>" + Helper.toString(args()) + ", sort=" + order;
		}

		/**
		 * create the SQL "where".
		 *
		 * @return String
		 */
		public String where() {
			return where(null);
		}

		/**
		 * create the SQL "where" with the tansfers.
		 *
		 * @param tansfers
		 *            the words pair should be transfered
		 * @return the SQL string
		 */
		public String where(Map<String, String> tansfers) {
			StringBuilder sb = new StringBuilder();
			if (!X.isEmpty(connectsql)) {
				sb.append(connectsql);
			}

			for (Entity e : elist) {
				if (sb.length() > 0) {
					if (e.cond == AND) {
						sb.append(" and ");
					} else if (e.cond == OR) {
						sb.append(" or ");
					}
				}

				sb.append(e.where(tansfers));
			}

			for (W w : wlist) {
				if (sb.length() > 0) {
					if (w.cond == AND) {
						sb.append(" and ");
					} else if (w.cond == OR) {
						sb.append(" or ");
					}
				}

				sb.append(" (").append(w.where(tansfers)).append(") ");
			}

			return sb.toString();

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
		 * get the order by.
		 *
		 * @return String
		 */
		public String orderby() {
			return orderby(null);
		}

		/**
		 * create order string with the transfers.
		 *
		 * @param transfers
		 *            the words pair that need transfer according database
		 * @return the SQL order string
		 */
		public String orderby(Map<String, String> transfers) {

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
		 * @param name
		 *            the name
		 * @param v
		 *            the v
		 * @return W
		 */
		public W and(String name, Object v) {
			return and(name, v, OP.eq);
		}

		/**
		 * set and "and (...)" conditions
		 *
		 * @param w
		 *            the w
		 * @return W
		 */
		public W and(W w) {
			w.cond = AND;
			wlist.add(w);
			return this;
		}

		/**
		 * set a "or (...)" conditions
		 *
		 * @param w
		 *            the w
		 * @return W
		 */
		public W or(W w) {
			w.cond = OR;
			wlist.add(w);
			return this;
		}

		private void _and(List<LinkedHashMap<String, Integer>> l1, Entity e) {
			if (l1.isEmpty()) {
				l1.add(new LinkedHashMap<String, Integer>());
			}

			for (LinkedHashMap<String, Integer> r : l1) {
				if (!r.containsKey(e.name)) {
					r.put(e.name, 1);
				}
			}
		}

		private void _and(List<LinkedHashMap<String, Integer>> l1, W e) {
			if (l1.isEmpty()) {
				l1.add(new LinkedHashMap<String, Integer>());
			}

			List<LinkedHashMap<String, Integer>> l2 = e.sortkeys();
			for (LinkedHashMap<String, Integer> r : l1) {
				for (LinkedHashMap<String, Integer> r2 : l2) {
					for (String name : r2.keySet()) {
						if (!r.containsKey(name)) {
							r.put(name, 1);
						}
					}
				}
			}

		}

		private void _or(List<LinkedHashMap<String, Integer>> l1, Entity e) {
			LinkedHashMap<String, Integer> r = new LinkedHashMap<String, Integer>();
			r.put(e.name, 1);
			l1.add(r);
		}

		private void _or(List<LinkedHashMap<String, Integer>> l1, W e) {
			l1.addAll(e.sortkeys());
		}

		/**
		 * get all keys.
		 *
		 * @return List keys
		 */
		public List<LinkedHashMap<String, Integer>> sortkeys() {
			List<LinkedHashMap<String, Integer>> l1 = new ArrayList<LinkedHashMap<String, Integer>>();

			if (!X.isEmpty(elist))
				for (Entity e : elist) {
					if (e.cond == AND) {
						_and(l1, e);
					} else {
						_or(l1, e);
					}
				}

			if (!X.isEmpty(wlist))
				for (W w : wlist) {
					if (w.cond == AND) {
						_and(l1, w);
					} else {
						_or(l1, w);
					}
				}

			if (!X.isEmpty(order)) {
				LinkedHashMap<String, Integer> r = new LinkedHashMap<String, Integer>();
				for (Entity e : order.toArray(new Entity[order.size()])) {
					if (!r.containsKey(e.name)) {
						int i = X.toInt(e.value);
						if (i < 0) {
							r.put(e.name, -1);
						} else {
							r.put(e.name, 1);
						}
					}
				}
				l1.add(r);
			}

			return l1;
		}

		/**
		 * set the namd and parameter with "op" conditions.
		 *
		 * @param name
		 *            the name
		 * @param v
		 *            the value object
		 * @param op
		 *            the operation
		 * @return the W
		 */
		public W and(String name, Object v, OP op) {
			elist.add(new Entity(name, v, op, AND));
			return this;
		}

		/**
		 * same as and(String name, Object v, OP op).
		 *
		 * @param name
		 *            the name
		 * @param v
		 *            the value object
		 * @param op
		 *            the operation
		 * @return the W
		 */
		public W append(String name, Object v, OP op) {
			return and(name, v, op);
		}

		/**
		 * set name and parameter with "or" and "EQ" conditions.
		 *
		 * @param name
		 *            the name
		 * @param v
		 *            the v
		 * @return W
		 */
		public W or(String name, Object v) {
			return or(name, v, OP.eq);
		}

		/**
		 * set the name and parameter with "or" and "op" conditions.
		 *
		 * @param name
		 *            the name
		 * @param v
		 *            the v
		 * @param op
		 *            the op
		 * @return W
		 */
		public W or(String name, Object v, OP op) {

			elist.add(new Entity(name, v, op, OR));

			return this;
		}

		/**
		 * copy the name and parameter from a JSON, with "and" and "op" conditions.
		 *
		 * @param jo
		 *            the json
		 * @param op
		 *            the op
		 * @param names
		 *            the names
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
		 * @param jo
		 *            the json
		 * @param op
		 *            the op
		 * @param names
		 *            the names
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
		 * @param name
		 *            the name
		 * @param v
		 *            the v
		 * @return W
		 */
		public static W create(String name, Object v) {
			return create(name, v, OP.eq);
		}

		/**
		 * create the W object with the parameters.
		 *
		 * @param name
		 *            the field name
		 * @param v
		 *            the value object
		 * @param op
		 *            the operation
		 * @return the W
		 */
		public static W create(String name, Object v, OP op) {
			W w = new W();
			w.elist.add(new Entity(name, v, op, AND));
			return w;
		}

		/**
		 * create the W by single sql, this sql is only for RDS
		 * 
		 * @param connectsql
		 *            the sql without parameter
		 * @return the W
		 */
		public static W create(String connectsql) {
			W w = new W();
			w.connectsql = connectsql;
			return w;
		}

		/**
		 * get all the condition elements
		 * 
		 * @return the all entity
		 */
		public List<Entity> getAll() {
			return elist;
		}

		public static class Entity implements Serializable {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public String name;
			public Object value;
			public OP op; // operation EQ, GT, ...
			public int cond; // condition AND, OR

			private List<Object> args(List<Object> list) {
				if (value != null) {
					if (value instanceof Object[]) {
						for (Object o : (Object[]) value) {
							list.add(o);
						}
					} else if (op == OP.like) {
						list.add("%" + value + "%");
					} else {
						list.add(value);
					}
				}

				return list;
			}

			/**
			 * From json.
			 *
			 * @param j1
			 *            the j1
			 * @return the entity
			 */
			public static Entity fromJSON(JSON j1) {
				return new Entity(j1.getString("name"), j1.get("value"), OP.valueOf(j1.getString("op")),
						j1.getInt("cond"));
			}

			public Object getMongoQuery() {
				if (op == OP.eq) {
					return value;
				} else if (op == OP.gt) {
					return new BasicDBObject("$gt", value);
				} else if (op == OP.gte) {
					return new BasicDBObject("$gte", value);
				} else if (op == OP.lt) {
					return new BasicDBObject("$lt", value);
				} else if (op == OP.lte) {
					return new BasicDBObject("$lte", value);
				} else if (op == OP.like) {
					Pattern p1 = Pattern.compile(value.toString(), Pattern.CASE_INSENSITIVE);
					return p1;
				} else if (op == OP.neq) {
					return new BasicDBObject("$ne", value);
				}
				return value;
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
				return new Entity(name, value, op, cond);
			}

			private String where(Map<String, String> tansfers) {
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
				} else if (op == OP.like) {
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
			 * @see java.lang.Object#toString()
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

			private Entity(String name, Object v, OP op, int cond) {
				this.name = name;
				this.op = op;
				this.cond = cond;
				this.value = v;
			}
		}

		/**
		 * _parse.
		 *
		 * @param e
		 *            the e
		 * @param q
		 *            the q
		 * @return the basic db object
		 */
		BasicDBObject _parse(Entity e, BasicDBObject q) {
			q.append(e.name, e.getMongoQuery());
			return q;
		}

		/**
		 * Query.
		 *
		 * @return the basic db object
		 */
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public BasicDBObject query() {
			BasicDBObject q = new BasicDBObject();

			List<List> l1 = new ArrayList<List>();
			{
				List l2 = new ArrayList();
				for (Entity e : elist) {
					if (e.cond == OR && l2.size() > 0) {
						l1.add(l2);
						l2 = new ArrayList<Entity>();
					}
					l2.add(e);
				}
				for (W e : wlist) {
					if (e.cond == OR && l2.size() > 0) {
						l1.add(l2);
						l2 = new ArrayList<Entity>();
					}
					l2.add(e);
				}

				if (l2.size() > 0) {
					l1.add(l2);
				}
			}

			if (l1.size() > 1) {
				List<BasicDBObject> l3 = new ArrayList<BasicDBObject>();
				for (List l2 : l1) {
					BasicDBObject q1 = new BasicDBObject();
					for (Object e : l2) {
						if (e instanceof Entity) {
							_parse((Entity) e, q1);
						} else if (e instanceof W) {
							BasicDBObject q2 = ((W) e).query();
							q1.putAll(q2.toMap());
						}
					}
					l3.add(q1);
				}
				q.append("$or", l3);
			} else if (l1.size() > 0) {
				for (Object e : l1.get(0)) {
					if (e instanceof Entity) {
						_parse((Entity) e, q);
					} else if (e instanceof W) {
						BasicDBObject q2 = ((W) e).query();
						q.putAll(q2.toMap());
					}
				}
			}

			return q;
		}

		/**
		 * Order.
		 *
		 * @return the basic db object
		 */
		public BasicDBObject order() {
			BasicDBObject q = new BasicDBObject();
			if (order != null && order.size() > 0) {
				for (Entity e : order) {
					q.append(e.name, e.value);
				}
			}

			return q;
		}

		/**
		 * Sort.
		 *
		 * @param name
		 *            the name
		 * @param i
		 *            the i
		 * @return the w
		 */
		public W sort(String name, int i) {
			order.add(new Entity(name, i, OP.eq, AND));
			return this;
		}

	}

	public static interface IOptimizer {

		/**
		 * Query.
		 * 
		 * @param db
		 *            the db name
		 * @param table
		 *            the table
		 * @param w
		 *            the w
		 */
		public void query(String db, String table, W w);
	}

	/**
	 * load the data by id of X.ID
	 * 
	 * @param <T>
	 *            the subclass of Bean
	 * @param id
	 *            the id
	 * @param t
	 *            the Class of Bean
	 * @return the Bean
	 */
	public static <T extends Bean> T load(Object id, Class<T> t) {
		return load(W.create(X.ID, id), t);
	}

	/**
	 * load the data by the query.
	 *
	 * @param <T>
	 *            the subclass of Bean
	 * @param q
	 *            the query
	 * @param t
	 *            the Class of Bean
	 * @return the Bean
	 */
	public static <T extends Bean> T load(W q, Class<T> t) {
		return load(q, t, getDB(t));
	}

	/**
	 * Load.
	 *
	 * @param <T>
	 *            the generic type
	 * @param q
	 *            the q
	 * @param t
	 *            the t
	 * @param db
	 *            the db
	 * @return the t
	 */
	public static <T extends Bean> T load(W q, Class<T> t, String db) {
		String table = getTable(t);
		return load(table, q, t, db);
	}

	/**
	 * load data from the table.
	 *
	 * @param <T>
	 *            the subclass of Bean
	 * @param table
	 *            the table name
	 * @param q
	 *            the query and order
	 * @param t
	 *            the subclass of Bean
	 * @return the bean
	 */
	public static <T extends Bean> T load(String table, W q, Class<T> t) {
		return load(table, q, t, getDB(t));
	}

	/**
	 * Load.
	 *
	 * @param <T>
	 *            the generic type
	 * @param table
	 *            the table
	 * @param q
	 *            the q
	 * @param t
	 *            the t
	 * @param db
	 *            the db
	 * @return the t
	 */
	public static <T extends Bean> T load(String table, W q, Class<T> t, String db) {

		if (table != null) {
			if (monitor != null) {
				monitor.query(db, table, q);
			}

			if (primary != null && primary.getDB(db) != null) {
				return primary.load(table, q, t, db);
			} else if (!X.isEmpty(customs)) {
				for (DBHelper h : customs) {
					if (h.getDB(db) != null) {
						return h.load(table, q, t, db);
					}
				}
			}

			log.warn("no db configured, please configure the {giiwa}/giiwa.properites");
		}
		return null;

	}

	/**
	 * insert into the values by the Class of T.
	 *
	 * @param value
	 *            the value
	 * @param t
	 *            the Class of Bean
	 * @return the number of inserted, 0: failed
	 */
	public static int insert(V value, Class<? extends Bean> t) {
		return insert(value, t, getDB(t));
	}

	/**
	 * batch insert
	 * 
	 * @param values
	 *            the values
	 * @param t
	 *            the Class of Bean
	 * @return the number of inserted
	 */
	public static int insert(List<V> values, Class<? extends Bean> t) {
		return insert(values, t, getDB(t));
	}

	/**
	 * batch insert
	 * 
	 * @param values
	 *            the values
	 * @param t
	 *            the Class of Bean
	 * @param db
	 *            the DB name
	 * @return the number is inserted
	 */
	public static int insert(List<V> values, Class<? extends Bean> t, String db) {

		String table = getTable(t);
		return insert(values, table, db);
	}

	/**
	 * batch insert
	 * 
	 * @param values
	 *            the values
	 * @param table
	 *            the table name
	 * @param db
	 *            the db name
	 * @return the number inserted
	 */
	public static int insert(List<V> values, String table, String db) {

		if (table != null) {

			for (V value : values) {
				value.set(X.CREATED, System.currentTimeMillis()).set(X.UPDATED, System.currentTimeMillis());

				/**
				 * trigger the insert
				 */
				beforeInsert(db, table, value);
			}

			if (primary != null && primary.getDB(db) != null) {
				return primary.insertTable(table, values, db);
			} else if (!X.isEmpty(customs)) {
				for (DBHelper h : customs) {
					if (h.getDB(db) != null) {
						return h.insertTable(table, values, db);
					}
				}
			}

			log.warn("no db configured, please configure the {giiwa}/giiwa.properites");
		}

		return 0;

	}

	/**
	 * Insert.
	 *
	 * @param values
	 *            the values
	 * @param t
	 *            the t
	 * @param db
	 *            the db
	 * @return the int
	 */
	public static int insert(V value, Class<? extends Bean> t, String db) {
		String table = getTable(t);
		return insert(value, table, db);
	}

	/**
	 * insert
	 * 
	 * @param value
	 *            the value
	 * @param table
	 *            the table
	 * @param db
	 *            the db name
	 * @return the number inserted
	 */
	public static int insert(V value, String table, String db) {

		if (table != null) {
			value.set(X.CREATED, System.currentTimeMillis()).set(X.UPDATED, System.currentTimeMillis());

			/**
			 * trigger the insert
			 */
			beforeInsert(db, table, value);

			if (primary != null && primary.getDB(db) != null) {
				return primary.insertTable(table, value, db);
			} else if (!X.isEmpty(customs)) {
				for (DBHelper h : customs) {
					if (h.getDB(db) != null) {
						return h.insertTable(table, value, db);
					}
				}
			}

			log.warn("no db configured, please configure the {giiwa}/giiwa.properites");
		}

		return 0;

	}

	/**
	 * update the values by the id, for the Class of Bean.
	 *
	 * @param id
	 *            the id of the X.ID
	 * @param values
	 *            the values to update
	 * @param t
	 *            the Class of Bean
	 * @return the number of updated
	 */
	public static int update(Object id, V values, Class<? extends Bean> t) {
		return update(W.create(X.ID, id), values, t);
	}

	/**
	 * update the values by the W for the Class of Bean.
	 *
	 * @param q
	 *            the query
	 * @param values
	 *            the values to update
	 * @param t
	 *            the Class of Ban
	 * @return the number of updated
	 */
	public static int update(W q, V values, Class<? extends Bean> t) {
		return update(q, values, t, getDB(t));
	}

	/**
	 * update the values by the Q for the Class of Bean in the "db".
	 *
	 * @param q
	 *            the query
	 * @param values
	 *            the values
	 * @param t
	 *            the Class of Bean
	 * @param db
	 *            the database pool name
	 * @return the number of updated
	 */
	public static int update(W q, V values, Class<? extends Bean> t, String db) {
		String table = getTable(t);
		return update(table, q, values, db);
	}

	/**
	 * update the table by the query with the values.
	 *
	 * @param table
	 *            the table
	 * @param q
	 *            the query
	 * @param values
	 *            the values
	 * @return the number of updated
	 */
	public static int update(String table, W q, V values) {
		return update(table, q, values, DEFAULT);
	}

	/**
	 * update the table by the query with the values.
	 *
	 * @param table
	 *            the table
	 * @param q
	 *            the query
	 * @param values
	 *            the values
	 * @param db
	 *            the database pool
	 * @return the number of updated
	 */
	public static int update(String table, W q, V values, String db) {

		if (table != null) {

			if (monitor != null) {
				monitor.query(db, table, q);
			}

			values.set(X.UPDATED, System.currentTimeMillis());

			// log.debug("update 1 ...");

			/**
			 * trigger the update
			 */
			beforeUpdate(db, table, q, values);

			// log.debug("update 2 ...");

			if (primary != null && primary.getDB(db) != null) {
				return primary.updateTable(table, q, values, db);

			} else if (!X.isEmpty(customs)) {
				for (DBHelper h : customs) {
					if (h.getDB(db) != null) {
						return h.updateTable(table, q, values, db);
					}
				}
			}

			log.warn("no db configured, please configure the {giiwa}/giiwa.properites");
		}
		return 0;
	}

	/**
	 * Inc.
	 *
	 * @param q
	 *            the q
	 * @param name
	 *            the name
	 * @param n
	 *            the n
	 * @param t
	 *            the t
	 * @return the int
	 */
	public static int inc(W q, String name, int n, V v, Class<? extends Bean> t) {
		String table = getTable(t);
		return inc(table, q, name, n, v, getDB(t));
	}

	/**
	 * Inc.
	 *
	 * @param table
	 *            the table
	 * @param q
	 *            the q
	 * @param name
	 *            the name
	 * @param n
	 *            the n
	 * @return the int
	 */
	public static int inc(String table, W q, String name, int n, V v) {
		return inc(table, q, name, n, v, DEFAULT);
	}

	/**
	 * increase the value
	 * 
	 * @param table
	 *            the table
	 * @param q
	 *            the query
	 * @param name
	 *            the name
	 * @param n
	 *            the number
	 * @param db
	 *            the db name
	 * @return the new value
	 */
	public static int inc(String table, W q, String name, int n, V v, String db) {

		if (table != null) {

			if (monitor != null) {
				monitor.query(db, table, q);
			}

			if (primary != null && primary.getDB(db) != null) {
				return primary.inc(table, q, name, n, v, db);

			} else if (!X.isEmpty(customs)) {
				for (DBHelper h : customs) {
					if (h.getDB(db) != null) {
						return h.inc(table, q, name, n, v, db);
					}
				}
			}

			log.warn("no db configured, please configure the {giiwa}/giiwa.properites");
		}
		return 0;
	}

	/**
	 * test is configured RDS or Mongo
	 * 
	 * @return the boolean, <br>
	 *         true: configured RDS or Mongo; false: no DB configured
	 */
	public static boolean isConfigured() {
		return primary != null && primary.getDB(DEFAULT) != null;
	}

	/**
	 * load the data from the table by query, ignore the table definition for the
	 * Class.
	 *
	 * @param <T>
	 *            the subclass of Bean
	 * @param table
	 *            the table name
	 * @param q
	 *            the query
	 * @param s
	 *            the start
	 * @param n
	 *            the number
	 * @param t
	 *            the Class of Bean
	 * @return Beans of the T, the "total=-1" always
	 */
	public static <T extends Bean> Beans<T> load(String table, W q, int s, int n, Class<T> t) {
		return load(table, q, s, n, t, getDB(t));
	}

	/**
	 * Load.
	 *
	 * @param <T>
	 *            the generic type
	 * @param table
	 *            the table
	 * @param q
	 *            the q
	 * @param s
	 *            the s
	 * @param n
	 *            the n
	 * @param t
	 *            the t
	 * @param db
	 *            the db
	 * @return the beans
	 */
	public static <T extends Bean> Beans<T> load(String table, W q, int s, int n, Class<T> t, String db) {
		return _load(table, q, s, n, t, db, 1);
	}

	private static <T extends Bean> Beans<T> _load(final String table, final W q, final int s, final int n,
			final Class<T> t, final String db, int refer) {

		if (monitor != null) {
			monitor.query(db, table, q);
		}

		Beans<T> bs = null;
		if (primary != null && primary.getDB(db) != null) {
			bs = primary.load(table, q, s, n, t, db);
		} else if (!X.isEmpty(customs)) {
			for (DBHelper h : customs) {
				if (h.getDB(db) != null) {
					bs = h.load(table, q, s, n, t, db);
					break;
				}
			}
		}

		if (n > 0) {
			if (refer > 0 && bs != null && !X.isEmpty(bs) && bs.size() >= n && (s % n % 10 == 0)) {
				// not loop; has data; s % n % 10 == 0
				Task.create(new Runnable() {

					@Override
					public void run() {
						// Cause the DB to load the more data in memory
						_load(table, q, s + n * 10, n, t, db, 0);
					}

				}).schedule(1000);
			}
		}
		return bs;

	}

	/**
	 * load the data by query.
	 *
	 * @param <T>
	 *            the subclass of Bean
	 * @param q
	 *            the query
	 * @param s
	 *            the start
	 * @param n
	 *            the number
	 * @param t
	 *            the Class of Bean
	 * @return Beans of Class
	 */
	public static <T extends Bean> Beans<T> load(W q, int s, int n, Class<T> t) {
		String table = getTable(t);
		return load(table, q, s, n, t);
	}

	/**
	 * get the table name from the Class of Bean.
	 *
	 * @param t
	 *            the Class of Bean
	 * @return the String of the table name
	 */
	public static String getTable(Class<? extends Bean> t) {
		Table table = (Table) t.getAnnotation(Table.class);
		if (table == null || X.isEmpty(table.name())) {
			log.error("table missed/error in [" + t + "] declaretion");
			return null;
		}

		return table.name();
	}

	/**
	 * get the db name
	 * 
	 * @param t
	 *            the Bean class
	 * @return the db name
	 */
	public static String getDB(Class<? extends Bean> t) {
		Table table = (Table) t.getAnnotation(Table.class);
		if (table == null || X.isEmpty(table.name())) {
			return DEFAULT;
		}

		return table.db();
	}

	/**
	 * count the data by the query.
	 *
	 * @param q
	 *            the query
	 * @param t
	 *            the Class of Bean
	 * @return the long of data number
	 */
	public static long count(W q, Class<? extends Bean> t) {
		return count(q, t, getDB(t));
	}

	/**
	 * count the items in the db.
	 *
	 * @param q
	 *            the query
	 * @param t
	 *            the Class of Bean
	 * @param db
	 *            the name of db pool
	 * @return the number
	 */
	public static long count(W q, Class<? extends Bean> t, String db) {
		String table = getTable(t);
		return count(q, table, db);
	}

	/**
	 * count the items in table, db
	 * 
	 * @param q
	 *            the query
	 * @param table
	 *            the table name
	 * @param db
	 *            the db name
	 * @return long
	 */
	public static long count(W q, String table, String db) {
		if (table != null) {
			if (monitor != null) {
				monitor.query(db, table, q);
			}

			if (primary != null && primary.getDB(db) != null) {
				return primary.count(table, q, db);
			} else if (!X.isEmpty(customs)) {
				for (DBHelper h : customs) {
					if (h.getDB(db) != null) {
						return h.count(table, q, db);
					}
				}
			}

			log.warn("no db configured, please configure the {giiwa}/giiwa.properites");
		}

		return 0;
	}

	/**
	 * get the distinct list for the name, by the query.
	 * 
	 * @param <T>
	 *            the base object
	 * @param name
	 *            the column name
	 * @param q
	 *            the query
	 * @param b
	 *            the Bean class
	 * @param t
	 *            the Class of T
	 * @return the List of objects
	 */
	public static <T> List<T> distinct(String name, W q, Class<? extends Bean> b, Class<T> t) {
		return distinct(name, q, b, t, getDB(b));
	}

	/**
	 * Distinct.
	 *
	 * @param <T>
	 *            the generic type
	 * @param name
	 *            the name
	 * @param q
	 *            the q
	 * @param b
	 *            the b
	 * @param t
	 *            the t
	 * @param db
	 *            the db
	 * @return the list
	 */
	public static <T> List<T> distinct(String name, W q, Class<? extends Bean> b, Class<T> t, String db) {
		String table = getTable(b);
		return distinct(name, q, table, t, db);
	}

	/**
	 * get the distinct data
	 * 
	 * @param name
	 *            the column name
	 * @param q
	 *            the query
	 * @param table
	 *            the table name
	 * @param t
	 *            the result type
	 * @param db
	 *            the db name
	 * @return the list
	 */
	public static <T> List<T> distinct(String name, W q, String table, Class<T> t, String db) {

		if (table != null) {
			if (monitor != null) {
				monitor.query(db, table, q);
			}

			if (primary != null && primary.getDB(db) != null) {
				return primary.distinct(table, name, q, t, db);

			} else if (!X.isEmpty(customs)) {
				for (DBHelper h : customs) {
					if (h.getDB(db) != null) {
						return h.distinct(table, name, q, t, db);
					}
				}
			}

			log.warn("no db configured, please configure the {giiwa}/giiwa.properites");
		}

		return null;
	}

	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {

		W w = W.create("name", 1).sort("name", 1).sort("nickname", -1).sort("ddd", 1);
		w.and("aaa", 2);
		W w1 = W.create("a", 1).or("b", 2);
		w.and(w1);

		System.out.println(w.where());
		System.out.println(Helper.toString(w.args()));
		System.out.println(w.orderby());

		System.out.println("-----------");
		System.out.println(w.query());
		System.out.println(w.order());

		w = W.create("a", 1);
		w.and("b", new BasicDBObject("$exists", true));
		System.out.println("-----------");
		System.out.println(w.query());
		System.out.println(w.order());

		W q = W.create("a", 1).or("a", 2).or(W.create("c", 1, W.OP.gt).and("c", 3, W.OP.lt));
		System.out.println(q);
		System.out.println(q.query());

		W q1 = W.create("pid", 1).and("state", 1, W.OP.neq).and("cno", 1).and(W.create("role", 1).or("role", 2));
		System.out.println(q1);
		System.out.println(q1.query());

		W q2 = W.create("state", 1)
				.or(W.create("state", 2).and("enddate", System.currentTimeMillis() - X.AHOUR, W.OP.lt));
		System.out.println(q2);
		System.out.println(q2.query());
	}

	/**
	 * 
	 * @param m
	 *            the optimizer
	 */
	public static void setOptmizer(IOptimizer m) {
		monitor = m;
		log.info("optimizer=" + m);
	}

	/**
	 * create index on table
	 * 
	 * @param db
	 *            the db
	 * @param table
	 *            the table
	 * @param ss
	 *            the index info
	 */
	public static void createIndex(String db, String table, LinkedHashMap<String, Integer> ss) {

		if (primary != null && primary.getDB(db) != null) {
			primary.createIndex(table, ss, db);
		} else if (!X.isEmpty(customs)) {
			for (DBHelper h : customs) {
				if (h.getDB(db) != null) {
					h.createIndex(table, ss, db);
					break;
				}
			}
		}

	}

	/**
	 * get indexes on table
	 * 
	 * @param table
	 *            the table
	 * @param db
	 *            the db
	 * @return the list of indexes
	 */
	public static List<Map<String, Object>> getIndexes(String table, String db) {

		if (primary != null && primary.getDB(db) != null) {
			return primary.getIndexes(table, db);
		} else if (!X.isEmpty(customs)) {
			for (DBHelper h : customs) {
				if (h.getDB(db) != null) {
					return h.getIndexes(table, db);
				}
			}
		}

		return null;

	}

	/**
	 * drop indexes
	 * 
	 * @param table
	 *            the table
	 * @param name
	 *            the index name
	 * @param db
	 *            the db
	 */
	public static void dropIndex(String table, String name, String db) {

		if (primary != null && primary.getDB(db) != null) {
			primary.dropIndex(table, name, db);
		} else if (customs != null) {
			for (DBHelper h : customs) {
				h.dropIndex(table, name, db);
				break;
			}
		}

	}

	/**
	 * set primary db helper
	 * 
	 * @param helper
	 *            the db helper
	 */
	public static void setPrimary(DBHelper helper) {
		primary = helper;

		log.info("primary=" + primary);
	}

	/**
	 * add a custom db helper which works if primary has not that db
	 * 
	 * @param helper
	 *            the db helper
	 */
	public static void addCustomHelper(DBHelper helper) {
		if (customs == null) {
			customs = new ArrayList<DBHelper>();
		}
		if (!customs.contains(helper)) {
			customs.add(helper);
		}
	}

	/**
	 * the DBHelper interface
	 * 
	 * @author wujun
	 *
	 */
	public interface DBHelper {

		boolean isConfigured();

		List<Map<String, Object>> getIndexes(String table, String db);

		void dropIndex(String table, String name, String db);

		void createIndex(String table, LinkedHashMap<String, Integer> ss, String db);

		<T extends Bean> Beans<T> load(String table, W q, int s, int n, Class<T> t, String db);

		<T extends Bean> Cursor<T> query(String table, W q, int s, int n, Class<T> t, String db);

		<T extends Bean> T load(String table, W q, Class<T> clazz, String db);

		int delete(String table, W q, String db);

		Object getDB(String db);

		boolean exists(String table, W q, String db);

		int insertTable(String table, V value, String db);

		int insertTable(String table, List<V> values, String db);

		int updateTable(String table, W q, V values, String db);

		int inc(String table, W q, String name, int n, V v, String db);

		long count(String table, W q, String db);

		<T> List<T> distinct(String table, String name, W q, Class<T> t, String db);

		List<JSON> listTables(String db);

		void close();

		List<JSON> getMetaData(String tablename);
	}

	public static DBHelper getPrimary() {
		return primary;
	}

	public interface Cursor<E> extends Iterator<E> {

		void close();
	}

}
