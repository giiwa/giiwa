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
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.base.StringFinder;
import org.giiwa.core.conf.Global;
import org.giiwa.core.dle.Velocity;
import org.giiwa.core.json.JSON;
import org.giiwa.framework.bean.Data;
import org.giiwa.framework.web.Controller;

import com.mongodb.BasicDBList;
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

	public static IOptimizer monitor = null;

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
	 * @param conf the conf
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
	 * delete a data from database.
	 *
	 * @param id the value of "id"
	 * @param t  the subclass of Bean
	 * @return the number was deleted
	 */
	public static int delete(Object id, Class<? extends Bean> t) {
		return delete(W.create(X.ID, id), t);
	}

	/**
	 * delete the data , return the number that was deleted.
	 *
	 * @param q the query
	 * @param t the subclass of the Bean
	 * @return the number was deleted
	 */
	public static int delete(W q, Class<? extends Bean> t) {
		return delete(q, t, getDB(t));
	}

	/**
	 * Delete.
	 *
	 * @param q  the q
	 * @param t  the t
	 * @param db the db
	 * @return the int
	 */
	public static int delete(W q, Class<? extends Bean> t, String db) {
		String table = getTable(t);
		return delete(q, table, db);
	}

	/**
	 * delete the data in table
	 * 
	 * @param q     the query
	 * @param table the table name
	 * @param db    the db name
	 * @return the items was deleted
	 */
	public static int delete(W q, String table, String db) {
		if (table != null) {
			if (monitor != null) {
				monitor.query(db, table, q);
			}

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
	 * @param id the value of "id"
	 * @param t  the subclass of Bean
	 * @return true: exist
	 * @throws SQLException throw exception if occur database error
	 */
	public static boolean exists(Object id, Class<? extends Bean> t) throws SQLException {
		return exists(W.create(X.ID, id), t);
	}

	/**
	 * test exists.
	 *
	 * @param q the query and order
	 * @param t the class of Bean
	 * @return true: exists, false: not exists
	 * @throws SQLException throw Exception if the class declaration error or not db
	 *                      configured
	 */
	public static boolean exists(W q, Class<? extends Bean> t) throws SQLException {
		return exists(q, t, getDB(t));
	}

	/**
	 * Exists.
	 *
	 * @param q  the q
	 * @param t  the t
	 * @param db the db
	 * @return true, if successful
	 * @throws SQLException the SQL exception
	 */
	public static boolean exists(W q, Class<? extends Bean> t, String db) throws SQLException {
		String table = getTable(t);
		return exists(q, table, db);
	}

	/**
	 * exists testing
	 * 
	 * @param q     the query
	 * @param table the table name
	 * @param db    the db name
	 * @return the boolean
	 * @throws SQLException throw SQLException if happen error
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

		protected final static Object ignore = new Object();

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
		 * @param m     the model
		 * @param names the names string
		 * @return the V
		 */
		public V copy(Controller m, String... names) {
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
			if (!X.isEmpty(name)) {
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
				for (String s : name) {
					m.remove(s);
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
			eq, gt, gte, lt, lte, like, neq, none, in, exists, nin, type, mod, all, size, near
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
//		private List<W> wlist = new ArrayList<W>();
//		private List<Entity> elist = new ArrayList<Entity>();
		private List<W> queryList = new ArrayList<W>();
		private List<Entity> order = new ArrayList<Entity>();
		private String groupby;

		private int cond = AND;

		private transient BeanDAO<?, ?> dao = null;
		private transient String table = null;
		private transient DBHelper helper = Helper.primary;

		@SuppressWarnings("rawtypes")
		private Class t = Data.class;

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

			StringBuilder sb = new StringBuilder();
			sb.append("{" + where() + "}=>" + Helper.toString(args()));
			if (!X.isEmpty(order)) {
				sb.append(", sort=" + order);
			}
			if (!X.isEmpty(groupby)) {
				sb.append(", groupby=" + groupby);
			}

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
					}
				}
				sb.append(clause.where(tansfers));
			}

			return sb.length() == 0 ? X.EMPTY : "(" + sb + ")";

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
		 * create a empty q, only query the data of this node in clusters envir
		 * 
		 * @return the W
		 */
		public static W createLocal() {
			W q = create().and("_node", Global.id());
			return q;
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
			if (w.isEmpty())
				return this;

			w.cond = AND;
//			wlist.add(w);
			queryList.add(w);
			return this;
		}

		/**
		 * filter=
		 * 
		 * @param filter
		 * @return
		 */
		public W and(String filter) {
			try {
				W q = SQL.where2W(StringFinder.create(filter));
				if (!q.isEmpty()) {
					if (this.isEmpty()) {
						this.copy(q);
					} else {
						this.and(q);
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			return this;
		}

		public W copy(W q) {

			this.groupby = q.groupby;

			this.cond = q.cond;

//			for (W w1 : q.wlist) {
//				this.wlist.add(w1.copy());
//			}
//
//			for (Entity e : q.elist) {
//				this.elist.add(e.copy());
//			}

			for (W e : q.queryList) {
				this.queryList.add(e.copy());
			}

			for (Entity e : q.order) {
				this.order.add(e.copy());
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

			w.cond = OR;
//			wlist.add(w);
			queryList.add(w);
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
		List<LinkedHashMap<String, Integer>> sortkeys() {

			List<LinkedHashMap<String, Integer>> l1 = new ArrayList<LinkedHashMap<String, Integer>>();

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

			// index order too
			if (!X.isEmpty(order)) {
				if (l1.isEmpty()) {
					l1.add(new LinkedHashMap<String, Integer>());
				}
				for (Entity e : order.toArray(new Entity[order.size()])) {
					for (LinkedHashMap<String, Integer> m : l1) {
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
			}

			return l1;
		}

		/**
		 * @deprecated
		 * @param name
		 * @param v
		 * @return
		 */
		public W and(String[] name, Object v) {
			return and(name, v, W.OP.eq);
		}

		/**
		 * @deprecated
		 * @param name
		 * @param v
		 * @param op
		 * @return
		 */
		public W and(String[] name, Object v, OP op) {
			return and(name, v, op, 1);
		}

		/**
		 * @deprecated
		 * @param name
		 * @param v
		 * @param op
		 * @param boost
		 * @return
		 */
		public W and(String[] name, Object v, OP op, int boost) {
			W q = W.create();
			for (String s : name) {
				q.or(s, v, op, boost);
			}
			return and(q);
		}

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
			}
			return this;
		}

		public W or(String name, Object v, String op) {
			return or(name, v, op, 1);
		}

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

		@SuppressWarnings("rawtypes")
		public W and(String name, Object v, OP op, int boost) {
			if (X.isEmpty(name))
				return this;

			if (v != null && v instanceof Collection) {
				W q = W.create();
				Collection l1 = (Collection) v;

				for (Object o : l1) {
					if (o instanceof W) {
						o = ((W) o).query();
					}
					if (op.equals(OP.eq)) {
						q.or(name, o);
					} else if (op.equals(OP.neq)) {
						q.and(name, o, OP.neq);
					}
				}
				this.and(q);
			} else if (v != null && v.getClass().isArray()) {
				W q = W.create();
				Object[] l1 = (Object[]) v;
				for (Object o : l1) {
					if (o instanceof W) {
						o = ((W) o).query();
					}
					if (op.equals(OP.eq)) {
						q.or(name, o);
					} else if (op.equals(OP.neq)) {
						q.and(name, o, OP.neq);
					}
				}
				this.and(q);
			} else {
				if (v instanceof W) {
					v = ((W) v).query();
				}
//				elist.add(new Entity(name, v, op, AND, boost));
				queryList.add(new Entity(name, v, op, AND, boost));
			}
			return this;
		}

		/**
		 * same as and(String name, Object v, OP op).
		 * 
		 * @deprecated
		 * @param name the name
		 * @param v    the value object
		 * @param op   the operation
		 * @return the W
		 */
		public W append(String name, Object v, OP op) {
			return and(name, v, op);
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

		@SuppressWarnings("rawtypes")
		public W or(String name, Object v, OP op, int boost) {

			if (X.isEmpty(name))
				return this;

			if (v != null && v instanceof Collection) {
				W q = W.create();
				Collection l1 = (Collection) v;
				for (Object o : l1) {
					if (o instanceof W) {
						o = ((W) o).query();
					}
					if (op.equals(OP.eq)) {
						q.or(name, o);
					} else if (op.equals(OP.neq)) {
						q.and(name, o, OP.neq);
					}
				}
				this.and(q);
			} else if (v != null && v.getClass().isArray()) {
				W q = W.create();
				Object[] l1 = (Object[]) v;
				for (Object o : l1) {
					if (o instanceof W) {
						o = ((W) o).query();
					}
					if (op.equals(OP.eq)) {
						q.or(name, o);
					} else if (op.equals(OP.neq)) {
						q.and(name, o, OP.neq);
					}
				}
				this.or(q);
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

		/**
		 * create the W by single sql, this sql is only for RDS
		 * 
		 * @param connectsql the sql without parameter
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
//		public List<Entity> getAll() {
//			return elist;
//		}

		public static class Entity extends W {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public String name;
			public Object value;
			public OP op; // operation EQ, GT, ...
			public int cond; // condition AND, OR
			public int boost = 1;//

			public int getCondition() {
				return cond;
			}

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
			 * @param j1 the j1
			 * @return the entity
			 */
			public static Entity fromJSON(JSON j1) {
				return new Entity(j1.getString("name"), j1.get("value"), OP.valueOf(j1.getString("op")),
						j1.getInt("cond"), j1.getInt("boost", 1));
			}

			public BasicDBObject query() {
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
					Pattern p1 = Pattern.compile(value.toString(), Pattern.CASE_INSENSITIVE);
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
				this.name = name;
				this.op = op;
				this.cond = cond;
				this.value = v;
				this.boost = boost;
			}

			@Override
			public String where() {
				// TODO Auto-generated method stub
				return where(null);
			}
		}

		BasicDBObject query() {

			BasicDBList list = new BasicDBList();

			int cond = -1;
			for (W clause : queryList) {
				if (!list.isEmpty() && cond != clause.getCondition()) {
					if (cond == AND) {
						BasicDBObject q = new BasicDBObject("$and", list.clone());
						list.clear();
						list.add(q);
					} else if (cond == OR) {
						BasicDBObject q = new BasicDBObject("$or", list.clone());
						list.clear();
						list.add(q);
					}
				}

				if (clause instanceof W) {
					list.add(((W) clause).query());
				} else {
					list.add(clause);
				}
				cond = clause.getCondition();

			}

			if (list.isEmpty()) {
				return new BasicDBObject();
			} else if (list.size() == 1) {
				return (BasicDBObject) list.get(0);
			} else {
				if (cond == AND) {
					return new BasicDBObject().append("$and", list);
				} else if (cond == OR) {
					return new BasicDBObject().append("$or", list);
				}
			}
			return new BasicDBObject();
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

		public W copy(V v) {
			if (v != null && !v.isEmpty()) {
				for (String name : v.m.keySet()) {
					and(name, v.m.get(name));
				}
			}
			return this;
		}

		/**
		 * load a data
		 * 
		 * @param <T>
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public <T> T load() {
			if (log.isDebugEnabled())
				log.debug("w=" + this);

			if (dao != null) {
				return (T) dao.load(this);
			} else {
				return (T) helper.load(table, null, this, t, Helper.DEFAULT);
			}
		}

		/**
		 * load a data by skip offset
		 * 
		 * @param <T>
		 * @param offset
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public <T> T load(int offset) {
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
		public <T> T get(String name) {
			Bean b = load();
			return b == null ? null : b.get(name);
		}

		public long getLong(String name) {
			Bean b = load();
			return b == null ? 0 : b.getLong(name);
		}

		public long getLong(int offset, String name) {
			Bean b = load(offset);
			return b == null ? 0 : b.getLong(name);
		}

		public double getDouble(String name) {
			Bean b = load();
			return b == null ? 0 : b.getDouble(name);
		}

		public double getDouble(int offset, String name) {
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
		public <T> T get(int offset, String name) {
			Bean b = load(offset);
			return b == null ? null : b.get(name);
		}

		/**
		 * load data list
		 * 
		 * @param <T>
		 * @param s
		 * @param n
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public <T extends Bean> Beans<T> load(int s, int n) {
			if (log.isDebugEnabled())
				log.debug("w=" + this);

			if (dao != null) {
				return (Beans<T>) dao.load(this, s, n);
			} else {
				return helper.load(table, null, this, s, n, t, Helper.DEFAULT);
			}
		}

		public long count() {
			if (dao != null) {
				return dao.count(this);
			} else {
				return helper.count(table, this, Helper.DEFAULT);
			}
		}

		public <T> T sum(String name) {
			if (dao != null) {
				return dao.sum(name, this);
			} else {
				return helper.sum(table, this, name, Helper.DEFAULT);
			}
		}

		public <T> T avg(String name) {
			if (dao != null) {
				return dao.avg(name, this);
			} else {
				return helper.avg(table, this, name, Helper.DEFAULT);
			}
		}

		public <T> T min(String name) {
			if (dao != null) {
				return dao.min(name, this);
			} else {
				return helper.min(table, this, name, Helper.DEFAULT);
			}
		}

		public <T> T max(String name) {
			if (dao != null) {
				return dao.max(name, this);
			} else {
				return helper.max(table, this, name, Helper.DEFAULT);
			}
		}

		public int delete() {
			if (dao != null) {
				return dao.delete(this);
			} else {
				return helper.delete(table, this, Helper.DEFAULT);
			}
		}

		public List<?> distinct(String name) {
			if (dao != null) {
				return dao.distinct(name, this);
			} else {
				return helper.distinct(table, name, this, Helper.DEFAULT);
			}
		}

		public List<JSON> count(String group) {
			if (dao != null) {
				return dao.count(this, X.split(group, "[,]"));
			} else {
				return helper.count(table, this, X.split(group, "[,]"), Helper.DEFAULT);
			}
		}

		public List<JSON> sum(String name, String group) {
			if (dao != null) {
				return dao.sum(this, name, X.split(group, "[,]"));
			} else {
				return helper.sum(table, this, name, X.split(group, "[,]"), Helper.DEFAULT);
			}
		}

		public List<JSON> aggregate(String name, String group) {
			if (dao != null) {
				return dao.aggregate(this, X.split(name, "[,]"), X.split(group, "[,]"));
			} else {
				return helper.aggregate(table, X.split(name, "[,]"), this, X.split(group, "[,]"), Helper.DEFAULT);
			}
		}

		public List<JSON> min(String name, String group) {
			if (dao != null) {
				return dao.min(this, name, X.split(group, "[,]"));
			} else {
				return helper.min(table, this, name, X.split(group, "[,]"), Helper.DEFAULT);
			}
		}

		public List<JSON> max(String name, String group) {
			if (dao != null) {
				return dao.max(this, name, X.split(group, "[,]"));
			} else {
				return helper.max(table, this, name, X.split(group, "[,]"), Helper.DEFAULT);
			}
		}

		public List<JSON> avg(String name, String group) {
			if (dao != null) {
				return dao.avg(this, name, X.split(group, "[,]"));
			} else {
				return helper.avg(table, this, name, X.split(group, "[,]"), Helper.DEFAULT);
			}
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
			String table = Helper.getTable(clazz);
			if (!X.isEmpty(table)) {
				this.table = table;
			}
			return this;
		}

	}

	public static interface IOptimizer {

		/**
		 * Query.
		 * 
		 * @param db    the db name
		 * @param table the table
		 * @param q     the W
		 */
		public void query(String db, String table, W q);

	}

	/**
	 * load the data by id of X.ID
	 * 
	 * @param <T> the subclass of Bean
	 * @param id  the id
	 * @param t   the Class of Bean
	 * @return the Bean
	 */
	public static <T extends Bean> T load(Object id, Class<T> t) {
		return load(W.create(X.ID, id), t);
	}

	/**
	 * load the data by the query.
	 *
	 * @param <T> the subclass of Bean
	 * @param q   the query
	 * @param t   the Class of Bean
	 * @return the Bean
	 */
	public static <T extends Bean> T load(W q, Class<T> t) {
		return load(q, t, getDB(t));
	}

	public static <T extends Bean> T load(String[] fields, W q, Class<T> t) {
		return load(fields, q, t, getDB(t));
	}

	/**
	 * Load.
	 *
	 * @param <T> the generic type
	 * @param q   the q
	 * @param t   the t
	 * @param db  the db
	 * @return the t
	 */
	public static <T extends Bean> T load(W q, Class<T> t, String db) {
		String table = getTable(t);
		return load(table, null, q, t, db);
	}

	public static <T extends Bean> T load(String[] fields, W q, Class<T> t, String db) {
		String table = getTable(t);
		return load(table, fields, q, t, db);
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
		return load(table, null, q, t, getDB(t));
	}

	/**
	 * Load.
	 *
	 * @param <T>    the generic type
	 * @param table  the table
	 * @param fields the fields
	 * @param q      the query
	 * @param t      the Bean Class
	 * @param db     the db
	 * @return the T
	 */
	public static <T extends Bean> T load(String table, String[] fields, W q, Class<T> t, String db) {

		if (table != null) {
			if (monitor != null) {
				monitor.query(db, table, q);
			}

			if (primary != null && primary.getDB(db) != null) {
				return primary.load(table, fields, q, t, db);
			} else if (!X.isEmpty(customs)) {
				for (DBHelper h : customs) {
					if (h.getDB(db) != null) {
						return h.load(table, fields, q, t, db);
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
	 * @param value the value
	 * @param t     the Class of Bean
	 * @return the number of inserted, 0: failed
	 */
	public static int insert(V value, Class<? extends Bean> t) {
		return insert(value, t, getDB(t));
	}

	public static int insert(String table, V value) {
		return insert(value, table, DEFAULT);
	}

	/**
	 * batch insert
	 * 
	 * @param values the values
	 * @param t      the Class of Bean
	 * @return the number of inserted
	 */
	public static int insert(List<V> values, Class<? extends Bean> t) {
		return insert(values, t, getDB(t));
	}

	/**
	 * batch insert
	 * 
	 * @param values the values
	 * @param t      the Class of Bean
	 * @param db     the DB name
	 * @return the number is inserted
	 */
	public static int insert(List<V> values, Class<? extends Bean> t, String db) {

		String table = getTable(t);
		return insert(values, table, db);
	}

	/**
	 * batch insert
	 * 
	 * @param values the values
	 * @param table  the table name
	 * @param db     the db name
	 * @return the number inserted
	 */
	public static int insert(List<V> values, String table, String db) {

		if (table != null) {

			for (V value : values) {
				value.set(X.CREATED, System.currentTimeMillis()).set(X.UPDATED, System.currentTimeMillis());

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
	 * @param value the values
	 * @param t     the t
	 * @param db    the db
	 * @return the int
	 */
	public static int insert(V value, Class<? extends Bean> t, String db) {
		String table = getTable(t);
		return insert(value, table, db);
	}

	/**
	 * insert
	 * 
	 * @param value the value
	 * @param table the table
	 * @param db    the db name
	 * @return the number inserted
	 */
	public static int insert(V value, String table, String db) {

		if (table != null) {
			value.set(X.CREATED, System.currentTimeMillis()).set(X.UPDATED, System.currentTimeMillis());
			value.set("_node", Global.id());

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
	 * @param id     the id of the X.ID
	 * @param values the values to update
	 * @param t      the Class of Bean
	 * @return the number of updated
	 */
	public static int update(Object id, V values, Class<? extends Bean> t) {
		return update(W.create(X.ID, id), values, t);
	}

	/**
	 * update the values by the W for the Class of Bean.
	 *
	 * @param q      the query
	 * @param values the values to update
	 * @param t      the Class of Ban
	 * @return the number of updated
	 */
	public static int update(W q, V values, Class<? extends Bean> t) {
		return update(q, values, t, getDB(t));
	}

	/**
	 * update the values by the Q for the Class of Bean in the "db".
	 *
	 * @param q      the query
	 * @param values the values
	 * @param t      the Class of Bean
	 * @param db     the database pool name
	 * @return the number of updated
	 */
	public static int update(W q, V values, Class<? extends Bean> t, String db) {
		String table = getTable(t);
		return update(table, q, values, db);
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
		return update(table, q, values, DEFAULT);
	}

	/**
	 * update the table by the query with the values.
	 *
	 * @param table  the table
	 * @param q      the query
	 * @param values the values
	 * @param db     the database pool
	 * @return the number of updated
	 */
	public static int update(String table, W q, V values, String db) {

		if (table != null) {

			if (monitor != null) {
				monitor.query(db, table, q);
			}

			values.set(X.UPDATED, System.currentTimeMillis());

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
	 * @param q    the query
	 * @param name the name
	 * @param n    the number
	 * @param t    the Bean Class
	 * @return the int
	 */
	public static int inc(W q, String name, int n, V v, Class<? extends Bean> t) {
		String table = getTable(t);
		return inc(table, q, name, n, v, getDB(t));
	}

	/**
	 * Inc.
	 *
	 * @param table the table
	 * @param q     the q
	 * @param name  the name
	 * @param n     the n
	 * @return the int
	 */
	public static int inc(String table, W q, String name, int n, V v) {
		return inc(table, q, name, n, v, DEFAULT);
	}

	/**
	 * increase the value
	 * 
	 * @param table the table
	 * @param q     the query
	 * @param name  the name
	 * @param n     the number
	 * @param v     the value object
	 * @param db    the db name
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
	 * @param <T>   the subclass of Bean
	 * @param table the table name
	 * @param q     the query
	 * @param s     the start
	 * @param n     the number
	 * @param t     the Class of Bean
	 * @return Beans of the T, the "total=-1" always
	 */
	public static <T extends Bean> Beans<T> load(String table, W q, int s, int n, Class<T> t) {
		return load(table, q, s, n, t, getDB(t));
	}

	public static <T extends Bean> Beans<T> load(String table, String[] fields, W q, int s, int n, Class<T> t) {
		return load(table, fields, q, s, n, t, getDB(t));
	}

	/**
	 * Load.
	 *
	 * @param <T>   the generic type
	 * @param table the table
	 * @param q     the q
	 * @param s     the s
	 * @param n     the n
	 * @param t     the t
	 * @param db    the db
	 * @return the beans
	 */
	public static <T extends Bean> Beans<T> load(String table, W q, int s, int n, Class<T> t, String db) {
		return _load(table, null, q, s, n, t, db, 1);
	}

	public static <T extends Bean> Beans<T> load(String table, String[] fields, W q, int s, int n, Class<T> t,
			String db) {
		return _load(table, fields, q, s, n, t, db, 1);
	}

	private static <T extends Bean> Beans<T> _load(final String table, final String[] fields, final W q, final int s,
			final int n, final Class<T> t, final String db, int refer) {

		if (monitor != null) {
			monitor.query(db, table, q);
		}

		Beans<T> bs = null;
		if (primary != null && primary.getDB(db) != null) {
			bs = primary.load(table, fields, q, s, n, t, db);
		} else if (!X.isEmpty(customs)) {
			for (DBHelper h : customs) {
				if (h.getDB(db) != null) {
					bs = h.load(table, fields, q, s, n, t, db);
					break;
				}
			}
		}

		if (bs != null) {
			bs.q = q;
			bs.table = table;
			bs.db = db;
		}

		return bs;

	}

	/**
	 * load the data by query.
	 *
	 * @param <T> the subclass of Bean
	 * @param q   the query
	 * @param s   the start
	 * @param n   the number
	 * @param t   the Class of Bean
	 * @return Beans of Class
	 */
	public static <T extends Bean> Beans<T> load(W q, int s, int n, Class<T> t) {
		String table = getTable(t);
		return load(table, q, s, n, t);
	}

	public static <T extends Bean> BeanStream<T> stream(W q, int s, int n, Class<T> t) {
		String table = getTable(t);
		String db = getDB(t);

		if (monitor != null) {
			monitor.query(db, table, q);
		}

		Cursor<T> cur = null;
		if (primary != null && primary.getDB(db) != null) {
			cur = primary.query(table, q, s, n, t, db);
		} else if (!X.isEmpty(customs)) {
			for (DBHelper h : customs) {
				if (h.getDB(db) != null) {
					cur = h.query(table, q, s, n, t, db);
					break;
				}
			}
		}

		return BeanStream.create(cur);
	}

	public static <T extends Bean> Beans<T> load(String[] fields, W q, int s, int n, Class<T> t) {
		String table = getTable(t);
		return load(table, fields, q, s, n, t);
	}

	/**
	 * get the table name from the Class of Bean.
	 *
	 * @param t the Class of Bean
	 * @return the String of the table name
	 */
	public static String getTable(Class<? extends Bean> t) {
		Table table = (Table) t.getAnnotation(Table.class);
		if (table == null || X.isEmpty(table.name())) {
			// log.error("table missed/error in [" + t + "] declaretion", new Exception());
			return null;
		}

		return table.name();
	}

	/**
	 * get the db name
	 * 
	 * @param t the Bean class
	 * @return the db name
	 */
	public static String getDB(Class<? extends Bean> t) {
		Table table = (Table) t.getAnnotation(Table.class);
		if (table == null || X.isEmpty(table.db())) {
			return DEFAULT;
		}

		return table.db();
	}

	/**
	 * count the data by the query.
	 *
	 * @param q the query
	 * @param t the Class of Bean
	 * @return the long of data number
	 */
	public static long count(W q, Class<? extends Bean> t) {
		return count(q, t, getDB(t));
	}

	public static <T> T sum(W q, String name, Class<? extends Bean> t) {
		return sum(q, name, t, getDB(t));
	}

	public static <T> T max(W q, String name, Class<? extends Bean> t) {
		return max(q, name, t, getDB(t));
	}

	public static <T> T min(W q, String name, Class<? extends Bean> t) {
		return min(q, name, t, getDB(t));
	}

	public static <T> T avg(W q, String name, Class<? extends Bean> t) {
		return avg(q, name, t, getDB(t));
	}

	/**
	 * count the items in the db.
	 *
	 * @param q  the query
	 * @param t  the Class of Bean
	 * @param db the name of db pool
	 * @return the number
	 */
	public static long count(W q, Class<? extends Bean> t, String db) {
		String table = getTable(t);
		return count(q, table, db);
	}

	public static <T> T sum(W q, String name, Class<? extends Bean> t, String db) {
		String table = getTable(t);
		return sum(q, name, table, db);
	}

	public static <T> T max(W q, String name, Class<? extends Bean> t, String db) {
		String table = getTable(t);
		return max(q, name, table, db);
	}

	public static <T> T min(W q, String name, Class<? extends Bean> t, String db) {
		String table = getTable(t);
		return min(q, name, table, db);
	}

	public static <T> T avg(W q, String name, Class<? extends Bean> t, String db) {
		String table = getTable(t);
		return avg(q, name, table, db);
	}

	/**
	 * count the items in table, db
	 * 
	 * @param q     the query
	 * @param table the table name
	 * @param db    the db name
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

	public static <T> T median(W q, String name, String table, String db) {
		if (table != null) {
			if (monitor != null) {
				monitor.query(db, table, q);
			}

			if (primary != null && primary.getDB(db) != null) {
				return primary.median(table, q, name, db);
			} else if (!X.isEmpty(customs)) {
				for (DBHelper h : customs) {
					if (h.getDB(db) != null) {
						return h.median(table, q, name, db);
					}
				}
			}

			log.warn("no db configured, please configure the {giiwa}/giiwa.properites");
		}

		return null;
	}

	public static <T> T std_deviation(W q, String name, String table, String db) {
		if (table != null) {
			if (monitor != null) {
				monitor.query(db, table, q);
			}

			if (primary != null && primary.getDB(db) != null) {
				return primary.std_deviation(table, q, name, db);
			} else if (!X.isEmpty(customs)) {
				for (DBHelper h : customs) {
					if (h.getDB(db) != null) {
						return h.std_deviation(table, q, name, db);
					}
				}
			}

			log.warn("no db configured, please configure the {giiwa}/giiwa.properites");
		}

		return null;
	}

	public static <T> T sum(W q, String name, String table, String db) {
		if (table != null) {
			if (monitor != null) {
				monitor.query(db, table, q);
			}

			if (primary != null && primary.getDB(db) != null) {
				return primary.sum(table, q, name, db);
			} else if (!X.isEmpty(customs)) {
				for (DBHelper h : customs) {
					if (h.getDB(db) != null) {
						return h.sum(table, q, name, db);
					}
				}
			}

			log.warn("no db configured, please configure the {giiwa}/giiwa.properites");
		}

		return null;
	}

	public static <T> T max(W q, String name, String table, String db) {
		if (table != null) {
			if (monitor != null) {
				monitor.query(db, table, q);
			}

			if (primary != null && primary.getDB(db) != null) {
				return primary.max(table, q, name, db);
			} else if (!X.isEmpty(customs)) {
				for (DBHelper h : customs) {
					if (h.getDB(db) != null) {
						return h.max(table, q, name, db);
					}
				}
			}

			log.warn("no db configured, please configure the {giiwa}/giiwa.properites");
		}

		return null;
	}

	public static <T> T min(W q, String name, String table, String db) {
		if (table != null) {
			if (monitor != null) {
				monitor.query(db, table, q);
			}

			if (primary != null && primary.getDB(db) != null) {
				return primary.min(table, q, name, db);
			} else if (!X.isEmpty(customs)) {
				for (DBHelper h : customs) {
					if (h.getDB(db) != null) {
						return h.min(table, q, name, db);
					}
				}
			}

			log.warn("no db configured, please configure the {giiwa}/giiwa.properites");
		}

		return null;
	}

	public static <T> T avg(W q, String name, String table, String db) {
		if (table != null) {
			if (monitor != null) {
				monitor.query(db, table, q);
			}

			if (primary != null && primary.getDB(db) != null) {
				return primary.avg(table, q, name, db);
			} else if (!X.isEmpty(customs)) {
				for (DBHelper h : customs) {
					if (h.getDB(db) != null) {
						return h.avg(table, q, name, db);
					}
				}
			}

			log.warn("no db configured, please configure the {giiwa}/giiwa.properites");
		}

		return null;
	}

	/**
	 * get the distinct list for the name, by the query.
	 * 
	 * @param name the column name
	 * @param q    the query
	 * @param b    the Bean class
	 * @return the List of objects
	 */
	public static List<?> distinct(String name, W q, Class<? extends Bean> b) {
		return distinct(name, q, b, getDB(b));
	}

	/**
	 * Distinct.
	 *
	 * @param name the name
	 * @param q    the query
	 * @param b    the bean
	 * @param db   the db
	 * @return the list
	 */
	public static List<?> distinct(String name, W q, Class<? extends Bean> b, String db) {
		String table = getTable(b);
		return distinct(name, q, table, db);
	}

	/**
	 * get the distinct data
	 * 
	 * @param name  the column name
	 * @param q     the query
	 * @param table the table name
	 * @param db    the db name
	 * @return the list
	 */
	public static List<?> distinct(String name, W q, String table, String db) {

		if (table != null) {
			if (monitor != null) {
				monitor.query(db, table, q);
			}

			if (primary != null && primary.getDB(db) != null) {
				return primary.distinct(table, name, q, db);

			} else if (!X.isEmpty(customs)) {
				for (DBHelper h : customs) {
					if (h.getDB(db) != null) {
						return h.distinct(table, name, q, db);
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
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		/*
		 * W w = W.create("name", 1).sort("name", 1).sort("nickname", -1).sort("ddd",
		 * 1); w.and("aaa", 2); W w1 = W.create("a", 1).or("b", 2); w.and(w1);
		 * 
		 * System.out.println(w.where()); System.out.println(Helper.toString(w.args()));
		 * System.out.println(w.orderby());
		 * 
		 * System.out.println("-----------"); System.out.println(w.query());
		 * System.out.println(w.order());
		 * 
		 * w = W.create("a", 1); w.and("b", new BasicDBObject("$exists", true));
		 * System.out.println("-----------"); System.out.println(w.query());
		 * System.out.println(w.order());
		 * 
		 * W q = W.create("a", 1).or("a", 2).or(W.create("c", 1, W.OP.gt).and("c", 3,
		 * W.OP.lt)); System.out.println(q); System.out.println(q.query());
		 * 
		 * W q1 = W.create("pid", 1).and("state", 1, W.OP.neq).and("cno",
		 * 1).and(W.create("role", 1).or("role", 2)); System.out.println(q1);
		 * System.out.println(q1.query());
		 * 
		 * W q2 = W.create("state", 1) .or(W.create("state", 2).and("enddate",
		 * System.currentTimeMillis() - X.AHOUR, W.OP.lt)); System.out.println(q2);
		 * System.out.println(q2.query());
		 * 
		 * q1 = W.create("a", 1).and("b", 2).or("c", 1).and("d", 1);
		 * System.out.println(q1); System.out.println(q1.query());
		 * 
		 * q1 = W.create("a", 1).and("b", 2).or(W.create("c", 1).and("d", 1));
		 * System.out.println(q1); System.out.println(q1.query());
		 * 
		 * System.out.println("................."); W sql = W.create(); sql.and("a",
		 * "B").and("c", "d").or("e", "f").and("f", "g");
		 * System.out.println(sql.query()); System.out.println(sql.where()); sql =
		 * W.create(); sql.and("a", "c", W.OP.gt); sql.and(W.create().and("a", "b",
		 * W.OP.lt).and("d", "4")); sql.or("c", "d"); System.out.println(sql.query());
		 * System.out.println(sql.where());
		 * 
		 * sql = W.create(); sql.and("eventType", "1"); sql.and("msgType",
		 * "NOTIFICATION", W.OP.neq); if (true) { sql.and("convType", "PERSON");
		 * sql.and(W.create().or(W.create().and("fromAccount", "to").and("to", "from"))
		 * .or(W.create().and("fromAccount", "from").and("to", "to"))); } long time =
		 * System.currentTimeMillis(); if (time > 0) { sql.and("msgTimestamp", time,
		 * W.OP.lt); } System.out.println(sql.query()); System.out.println(sql.where());
		 * sql = W.create(); sql.and("eventType", "1"); sql.and("msgType",
		 * "NOTIFICATION", W.OP.neq); if (true) { sql.and("convType", "PERSON");
		 * sql.or(W.create().and("fromAccount", "from").and("to", "to"));
		 * sql.or(W.create().and("fromAccount", "to").and("to", "from")); } if (time >
		 * 0) { sql.append("msgTimestamp", time, W.OP.lt); }
		 * System.out.println(sql.query()); System.out.println(sql.where());
		 * System.out.println(sql.sortkeys());
		 * 
		 * sql = W.create(); sql.and("a", "b"); sql.or("c", "1"); sql.and(W.create("b",
		 * "x")); System.out.println(sql.query()); System.out.println(sql.where());
		 * System.out.println(sql.sortkeys());
		 * 
		 * q = W.create("a", 1).sort("b", -1); System.out.println(q.sortkeys());
		 * 
		 * String s = "(updated>10) and (created>10)";
		 * 
		 * try { q = SQL.where2W(StringFinder.create(s));
		 * 
		 * System.out.println(q.query());
		 * 
		 * } catch (Exception e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); }
		 */
//		W q = W.create();
//		q.and("cip_firstauthor", "\\(法\\)", W.OP.like);
//		System.out.println(q.query());
//
//		try {
//			StringFinder s = StringFinder.create("cip_firstauthor like '\\(法\\)'");
//			q = SQL.where2W(s);
//			System.out.println(q.query());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		Beans<User> l1 = User.dao.query().and("a", 1).sort("a", 1).load(0, 10);

		W q = W.create();
//		q.and("a", 1);
//		q.and("b = 2 and (c < 3 or c > 4) and d = '2'");
		q.and("b = 2 and (c < 3 or c > 4) and d = '2' or g = 1");
		q.sort("f", -1);
//		q.and("a like 'a'");

		System.out.println(q.toString());
		System.out.println(q.sortkeys());

	}

	/**
	 * 
	 */
	public static void enableOptmizer() {
		monitor = new Optimizer();
	}

	public static void disableOptmizer() {
		monitor = null;
	}

	/**
	 * create index on table
	 * 
	 * @param db    the db
	 * @param table the table
	 * @param ss    the index info
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
	 * @param table the table
	 * @param db    the db
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
	 * @param table the table
	 * @param name  the index name
	 * @param db    the db
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

		<T extends Bean> Beans<T> load(String table, String[] fields, W q, int s, int n, Class<T> t, String db);

		<T extends Bean> Cursor<T> query(String table, W q, int s, int n, Class<T> t, String db);

		<T extends Bean> T load(String table, String[] fields, W q, Class<T> clazz, String db);

		int delete(String table, W q, String db);

		void drop(String table, String db);

		Object getDB(String db);

		boolean exists(String table, W q, String db);

		int insertTable(String table, V value, String db);

		int insertTable(String table, List<V> values, String db);

		int updateTable(String table, W q, V values, String db);

		int inc(String table, W q, String name, int n, V v, String db);

		long count(String table, W q, String db);

		List<JSON> count(String table, W q, String[] group, String db);

		<T> T sum(String table, W q, String name, String db);

		List<JSON> sum(String table, W q, String name, String[] group, String db);

		<T> T max(String table, W q, String name, String db);

		List<JSON> max(String table, W q, String name, String[] group, String db);

		<T> T min(String table, W q, String name, String db);

		List<JSON> min(String table, W q, String name, String[] group, String db);

		<T> T avg(String table, W q, String name, String db);

		<T> T std_deviation(String table, W q, String name, String db);

		<T> T median(String table, W q, String name, String db);

		List<JSON> avg(String table, W q, String name, String[] group, String db);

		List<?> distinct(String table, String name, W q, String db);

		List<JSON> aggregate(String table, String[] func, W q, String[] group, String db);

		List<JSON> listTables(String db);

		void close();

		List<JSON> getMetaData(String tablename);

		void repair();
	}

	public static DBHelper getPrimary() {
		return primary;
	}

	public interface Cursor<E> extends Iterator<E> {

		void close();
	}

	public static void drop(String table, String db) {

		if (primary != null && primary.getDB(db) != null) {
			primary.drop(table, db);
		} else if (!X.isEmpty(customs)) {
			for (DBHelper h : customs) {
				if (h.getDB(db) != null) {
					h.drop(table, db);
				}
			}
		}

	}

	public static List<JSON> count(String tableName, W q, String[] name, String dbName) {
		if (primary != null && primary.getDB(dbName) != null) {
			return primary.count(tableName, q, name, dbName);
		} else if (!X.isEmpty(customs)) {
			for (DBHelper h : customs) {
				if (h.getDB(dbName) != null) {
					return h.count(tableName, q, name, dbName);
				}
			}
		}
		return null;
	}

	public static List<JSON> sum(String tableName, W q, String name, String[] group, String dbName) {
		if (primary != null && primary.getDB(dbName) != null) {
			return primary.sum(tableName, q, name, group, dbName);
		} else if (!X.isEmpty(customs)) {
			for (DBHelper h : customs) {
				if (h.getDB(dbName) != null) {
					return h.sum(tableName, q, name, group, dbName);
				}
			}
		}
		return null;
	}

	public static List<JSON> aggregate(String tableName, W q, String[] func, String[] group, String dbName) {
		if (primary != null && primary.getDB(dbName) != null) {
			return primary.aggregate(tableName, func, q, group, dbName);
		} else if (!X.isEmpty(customs)) {
			for (DBHelper h : customs) {
				if (h.getDB(dbName) != null) {
					return h.aggregate(tableName, func, q, group, dbName);
				}
			}
		}
		return null;
	}

	public static List<JSON> min(String tableName, W q, String name, String[] group, String dbName) {
		if (primary != null && primary.getDB(dbName) != null) {
			return primary.min(tableName, q, name, group, dbName);
		} else if (!X.isEmpty(customs)) {
			for (DBHelper h : customs) {
				if (h.getDB(dbName) != null) {
					return h.min(tableName, q, name, group, dbName);
				}
			}
		}
		return null;
	}

	public static List<JSON> max(String tableName, W q, String name, String[] group, String dbName) {
		if (primary != null && primary.getDB(dbName) != null) {
			return primary.max(tableName, q, name, group, dbName);
		} else if (!X.isEmpty(customs)) {
			for (DBHelper h : customs) {
				if (h.getDB(dbName) != null) {
					return h.max(tableName, q, name, group, dbName);
				}
			}
		}
		return null;
	}

	public static List<JSON> avg(String tableName, W q, String name, String[] group, String dbName) {
		if (primary != null && primary.getDB(dbName) != null) {
			return primary.avg(tableName, q, name, group, dbName);
		} else if (!X.isEmpty(customs)) {
			for (DBHelper h : customs) {
				if (h.getDB(dbName) != null) {
					return h.avg(tableName, q, name, group, dbName);
				}
			}
		}
		return null;
	}

}
