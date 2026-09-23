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
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
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
import org.giiwa.bean.GLog;
import org.giiwa.conf.Config;
import org.giiwa.conf.Global;
import org.giiwa.dao.sql.SQL;
import org.giiwa.json.JSON;
import org.giiwa.task.Consumer;
import org.giiwa.task.Function;
import org.giiwa.task.Task;
import org.giiwa.web.Controller;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;
import org.openjdk.nashorn.internal.runtime.Undefined;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

/**
 * 
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

	/**
	 * The conf.
	 * 
	 */
	protected static Configuration conf;

	/**
	 * 初始化数据库助手.
	 * 
	 * @deprecated by init2
	 * @param conf - 配置信息
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

		if (primary == null) {

//			if (MongoHelper.inst.isConfigured()) {
			primary = MongoHelper.inst;
//			}

			log.warn("db.primary missed, auto choose one, helper=" + primary);

		} else {
			log.info("db.primary=" + primary);
		}

	}

	private static final Map<String, Class<DBHelper>> _helpers = new HashMap<>();

	public static void addHelper(String drivername, Class<DBHelper> helper) {
		_helpers.put(drivername, helper);
	}

	/**
	 * 初始化数据库助手（新）
	 * 
	 * @param conf - 配置信息
	 * @return
	 */
	public synchronized static boolean init2(Configuration conf) {

		/**
		 * initialize the DB connections pool
		 */
		if (conf == null) {
			return false;
		}

		log.warn("Helper init2 ...");

		Helper.conf = conf;

		String url = conf.getString("db.url", X.EMPTY);
		if (X.isEmpty(url)) {
			log.error("db.url not congiured!");
			return false;
		}

		try {

			log.warn("db.url=" + url);

			RDSHelper.init();

			String user = conf.getString("db.user", X.EMPTY);
			String passwd = conf.getString("db.passwd", X.EMPTY);
			int conns = conf.getInt("db.conns", 50);
			log.info("db.conns=" + conns);

			boolean encoded = false;
			if (passwd != null) {
				if (passwd.startsWith("$$") && passwd.endsWith("$$")) {
					passwd = passwd.replaceAll("[\\$]", "");
					passwd = org.giiwa.web.Module.decode(passwd);
					encoded = true;
				}
			}

			if (url.startsWith("sdb://") || url.startsWith("mongodb://") || url.startsWith("jmdb://")
					|| url.startsWith("mysql://")) {

				int timeout = conf.getInt("db.timeout", 30000);

				primary = MongoHelper.create(url, user, passwd, conns, timeout);
				MongoHelper.inst = (MongoHelper) primary;

			} else {

				// int timeout = conf.getInt("db.timeout", 30000);
				String locale = conf.getString("db.locale", X.EMPTY);

				// test driver
//					Connection con = RDSHelper.getConnection(url, user, passwd, locale);
//					RDSHelper.close(con);
				// tested ok

				primary = RDSHelper.create(url, user, passwd, conns, locale);
				RDSHelper.inst = (RDSHelper) primary;

			}

			log.warn("db inited, primary=" + primary);

			if (Helper.isConfigured() && !encoded) {
				// encoding
//				log.info("encoding passwd ...1");
				String passwd1 = passwd;
				Task.schedule(t -> {
//					log.info("encoding passwd ...2");
					try {
						String passwd2 = "$$" + org.giiwa.web.Module.encode(passwd1) + "$$";
						conf.setProperty("db.passwd", passwd2);
//						log.info("encoded passwd=" + passwd2);
						Config.save2();
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				}, X.AMINUTE);
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return primary != null;

	}

	/**
	 * 删除数据，并记录性能
	 * 
	 * @param table - 表
	 * @param q     - 条件
	 * @return 删除条数
	 */
	public static int delete(String table, W q) {

		TimeStamp t1 = TimeStamp.create();
		try {
			if (table != null) {

//				if (monitor != null) {
//					monitor.query(table, q);
//				}

				return primary.delete(table, q);

			}
			return 0;
		} finally {
			write.add(t1.pastms(), "table=%s, q=%s", table, q);

		}
	}

	/**
	 * 修复表
	 * 
	 * @param table - 表
	 */
	public static void repair(String table) {
		primary.repair(table);
	}

	/**
	 * 检测数据是否存在
	 * 
	 * @param table - 表
	 * @param q     - 条件
	 * @return True - 存在
	 * @throws SQLException
	 */
	public static boolean exists(String table, W q) throws SQLException {

		TimeStamp t1 = TimeStamp.create();
		try {
			if (table != null) {

//				if (monitor != null) {
//					monitor.query(table, q);
//				}

				return primary.exists(table, q);
			}
		} finally {
			read.add(t1.pastms(), "table=%s, q=%s", table, q.toString());

		}
		throw new SQLException("no db configured, please configure the {giiwa}/giiwa.properites");
	}

	/**
	 * 检测数据是否存在
	 * 
	 * @param table - 表
	 * @param q     - 条件
	 * @return True - 存在
	 */
	public static boolean exists2(String table, W q) {

		TimeStamp t1 = TimeStamp.create();
		try {
			if (table != null) {

//				if (monitor != null) {
//					monitor.query(table, q);
//				}

				return primary.exists(table, q);
			}
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		} finally {
			read.add(t1.pastms(), "table=%s, q=%", table, q);
		}
		return false;
	}

	/**
	 * Value 类, 主要用于插入/更新数据
	 * 
	 */
	public static final class V implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public final static Object ignore = new Object();

		// value
		public Map<String, Object> m = new LinkedHashMap<String, Object>();
		// type
		public Map<String, String> t = new LinkedHashMap<String, String>();

		/**
		 * 转化成JSON对象
		 * 
		 * @return JSON - 转化结果
		 */
		public JSON json() {
			return JSON.fromObject(m);
		}

		/**
		 * 从Controller对象中复制V对象， 值为String
		 * 
		 * @param m     - Controller对象
		 * @param names - 参数
		 * @return V - this
		 */
		public V copy(Controller m, String... names) {
			if (X.isEmpty(names)) {
				// copy all
				for (String name : m.names()) {
					String v = m.get(name);
					if (v != null) {
						this.set(name, v);
					}
				}
			} else {
				for (String name : names) {
					String v = m.get(name);
					if (v != null) {
						this.set(name, v);
					}
				}
			}
			return this;
		}

		/**
		 * 从Controller对象中复制V对象， 值为Int
		 * 
		 * @param m     - Controller对象
		 * @param names - 参数
		 * @return V - this
		 */
		public V copyInt(Controller m, String... names) {
			if (!X.isEmpty(names)) {
				for (String name : names) {
					String v = m.get(name);
					if (v != null) {
						this.set(name, X.toInt(v));
					}
				}
			}
			return this;
		}

		/**
		 * 从Controller对象中复制V对象， 值为Long
		 * 
		 * @param m     - Controller对象
		 * @param names - 参数
		 * @return V - this
		 */
		public V copyLong(Controller m, String... names) {
			if (!X.isEmpty(names)) {
				for (String name : names) {
					String v = m.get(name);
					if (v != null) {
						this.set(name, X.toLong(v));
					}
				}
			}
			return this;
		}

		/**
		 * 从Controller对象中复制V对象， 值为List， 自动分割“，；[]”值
		 * 
		 * @param m     - Controller对象
		 * @param names - 参数
		 * @return V - this
		 */
		public V copyList(Controller m, String... names) {
			if (!X.isEmpty(names)) {
				for (String name : names) {
					String[] ss = m.getHtmls(name);
					if (ss != null && ss.length > 0) {
						List<String> l1 = null;
						for (String s : ss) {
							String[] ss1 = X.split(s, "[\\[\\] ,，；;]");
							if (ss1 != null) {
								if (l1 == null) {
									l1 = new ArrayList<String>();
								}
								for (String s1 : ss1) {
									if (!l1.contains(s1)) {
										l1.add(s1);
									}
								}
							}
						}
						this.set(name, l1);
					} else {
						String s = m.getHtml(name);
						if (s != null) {
							List<String> l1 = null;
							String[] ss1 = X.split(s, "[\\[\\] ,，；;]");
							if (ss1 != null) {
								if (l1 == null) {
									l1 = new ArrayList<String>();
								}
								for (String s1 : ss1) {
									if (!l1.contains(s1)) {
										l1.add(s1);
									}
								}
							}
							this.set(name, l1);
						}
					}
				}
			}
			return this;
		}

		/**
		 * 从Controller对象中复制V对象， 值为Html(原始值，不转义）
		 * 
		 * @param m     - Controller对象
		 * @param names - 参数
		 * @return V - this
		 */
		public V copyHtml(Controller m, String... names) {
			if (!X.isEmpty(names)) {
				for (String name : names) {
					String v = m.getHtml(name);
					if (v != null) {
						this.set(name, v);
					}
				}
			}
			return this;
		}

		/**
		 * 从JSON转换成V对象
		 *
		 * @param j - JSON对象
		 * @return 新的V
		 */
		public static V fromJSON(JSON j) {
			return V.create().copy(j);
		}

		/**
		 * 列表所有Key
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
		 * V对象的存储大小
		 *
		 * @return the int
		 */
		public int size() {
			int size = 0;
			for (String name : m.keySet()) {
				Object o = m.get(name);
				if (o != null && o != ignore) {
					if (o instanceof Integer || o instanceof Float || o instanceof Character || o instanceof Short) {
						size += 4;
					} else if (o instanceof Long || o instanceof Double) {
						size += 8;
					} else if (X.isArray(o)) {
						for (Object o1 : X.asList(o, s -> s)) {
							if (o1 instanceof Integer || o1 instanceof Float || o1 instanceof Character
									|| o1 instanceof Short) {
								size += 4;
							} else if (o1 instanceof Long || o1 instanceof Double) {
								size += 8;
							} else {
								size += o1.toString().getBytes().length;
							}
						}
					} else {
						size += o.toString().getBytes().length;
					}
				}
			}
			return size;
		}

		/**
		 * V对象中条目数
		 * 
		 * @return
		 */
		public int length() {
			return m.size();
		}

		/**
		 * 检测是否空，非V.ignore值
		 * 
		 * @return True - 空，或全是V.ignore
		 */
		public boolean isEmpty() {
			if (m.isEmpty()) {
				return true;
			}
			for (Object v : m.values()) {
				if (v != V.ignore) {
					return false;
				}
			}
			return true;
		}

		/**
		 * 转为字符串
		 *
		 * @return 字符串 {name=value, ...}
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
		 * 使用name=v创建一个新的V对象
		 *
		 * @param name - name
		 * @param v    - 值
		 * @return 新V对象
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
		@Deprecated
		public V put(String name, Object v) {
			return append(name, v, null);
		}

		@Deprecated
		public V put(String name, Object v, String type) {
			return append(name, v, type);
		}

		public V append(String name, Object v) {
			return append(name, v, null);
		}

		/**
		 * same as set(String name, Object v) <br>
		 * Sets the value if not exists, ignored if name exists.
		 * 
		 * @param name the name
		 * @param v    the value object
		 * @return the V
		 */
		public V append(String name, Object v, String type) {

			if (v instanceof Undefined) {
				v = null;
			}

			if (!X.isEmpty(name)) {
				name = name.toLowerCase();
				if (m.containsKey(name)) {
					return this;
				}
				if (v instanceof String && ((String) v).length() > 16 * X.MB) {
					throw new RuntimeException("the [" + name + "] size exceed max[16MB]");
				}
				m.put(name, v);
				if (type != null) {
					t.put(name, type);
				}
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
				if (v instanceof String && ((String) v).length() > 16 * X.MB) {
					throw new RuntimeException("the [" + name + "] size exceed max[16MB]");
				}
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
					if (o instanceof Undefined) {
						o = null;
					}
//					if (X.isEmpty(o)) {
//						set(s1, X.EMPTY);
//					} else {
//						if (o instanceof Integer || o instanceof Long || o instanceof Short || o instanceof Float
//								|| o instanceof Double || o instanceof ) {
					append(s1, o);
//						} else {
//							set(s1, o.toString());
//						}
//					}
				}
			}

			return this;
		}

		public V copy() {
			return V.create().copy(this);
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
					append(s, o, v.t.get(s));
				}
			} else {
				for (String name : v.m.keySet()) {
					Object o = v.m.get(name);
					append(name, o, v.t.get(name));
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

		public void type(String name, String type) {
			t.put(name, type);
		}

		public String type(String name) {
			return t.get(name);
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
						try {
							if (X.isSame(s1, s) || s.matches(s1)) {
								m.remove(s);
								break;
							}
						} catch (Exception err) {
							log.error(err.getMessage(), err);
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
	@Comment(text = "查询条件工具")
	public static class W implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * 条件
		 */
		public enum OP {
			eq, gt, gte, lt, lte, like, notlike, like_, like_$, neq, none, in, exists, nin, type, mod, all, size, near
		};

		private IAccess access;

		/**
		 * 设置权限过滤器， 只能设置一次
		 * 
		 * @param access， 权限过滤器
		 */
		public IAccess access(IAccess access) {
			if (this.access == null) {
				this.access = access;
			}
			return this.access;
		}

		/**
		 * "and"
		 */
		public static final int AND = 1;

		/**
		 * "or"
		 */
		public static final int OR = 2;

//		public static final int NOT = 4;

		private String connectsql;

		/**
		 * 查询条件
		 */
		private List<W> queryList = new ArrayList<W>();

		/**
		 * 排序条件
		 */
		private List<Entity> orderList = new ArrayList<Entity>();

		private String groupby;

		protected int cond = AND;

		public boolean not = false;

		private transient BeanDAO<?, ?> dao = null;
		public String table = null;
		private transient DBHelper helper = Helper.primary;

		@SuppressWarnings({ "rawtypes", "deprecation" })
		private Class t = Data.class;

		public boolean isAnd() {
			return cond == AND;
		}

		public boolean isOr() {
			return cond == OR;
		}

		/**
		 * @deprecated
		 * @return
		 * @throws SQLException
		 */
		public String getTable() throws SQLException {
			if (dao != null) {
				String table = dao.tableName();
				if (!X.isEmpty(table)) {
					return table;
				}
			}
			return table;
		}

		public String table() throws SQLException {
			if (dao != null) {
				String table = dao.tableName();
				if (!X.isEmpty(table)) {
					return table;
				}
			}
			return table;
		}

		/**
		 * 字段
		 */
		String fields;

		public String fields() {
			return fields;
		}

		public W fields(String s) {
			fields = s;
			return this;
		}

		@Comment(text = "设置查询列", demo = "select('a,b,c')")
		public W select(String s) {
			fields = s;
			return this;
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
			return orderList;
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
					q.orderList.add(e);
				}
			}

			return q;
		}

		private W() {
		}

		@Deprecated
		@Comment(hide = true)
		public W inc(String name, int n) throws SQLException {

			if (access != null) {
				access.checkWrite(table);
			}

			helper.inc(this.table, this, name, n, null);
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
		@Comment(text = "对象复制")
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

			for (Entity e : orderList) {
				w.orderList.add(e.copy());
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
		@Comment(text = "检查是否空条件")
		public boolean isEmpty() {
			return X.isEmpty(queryList) && X.isEmpty(groupby) && X.isEmpty(orderList);
		}

		/**
		 * create args for the SQL "where" <br>
		 * return the Object[].
		 *
		 * @return Object[]
		 */
		public Object[] args() {
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
			if (!X.isEmpty(orderList)) {
				sb.append(", sort=" + orderList);
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
		public String where() {
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
					if (clause.cond == AND) {
						sb.append(" and ");
					} else if (clause.cond == OR) {
						sb.append(" or ");
//					} else if (clause.cond == NOT) {
//						sb.append(" and not ");
					}
				}
				sb.append(clause.where(tansfers));
			}

			if (this.not) {
				return sb.length() == 0 ? X.EMPTY : "not (" + sb + ")";
			} else {
//				return sb.length() == 0 ? X.EMPTY : "(" + sb + ")";
				if (sb.indexOf(" ") > 0) {
					return "(" + sb + ")";
				}
				return sb.length() == 0 ? X.EMPTY : sb.toString();
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

		transient String _orderby;

		/**
		 * get the order by.
		 *
		 * @return String
		 */
		public String orderby() {
			if (_orderby == null) {
				_orderby = orderby(null);
			}
			return _orderby;
		}

		/**
		 * create order string with the transfers.
		 *
		 * @param transfers the words pair that need transfer according database
		 * @return the SQL order string
		 */
		String orderby(Map<String, String> transfers) {

			if (orderList.size() > 0) {
				StringBuilder sb = new StringBuilder("order by ");
				for (int i = 0; i < orderList.size(); i++) {
					Entity e = orderList.get(i);
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
		@Comment()
		public W and(@Comment(text = X.NAME) String name, @Comment(text = "value") Object v) {
			return and(name, v, 1);
		}

		@Comment()
		public W and(@Comment(text = X.NAME) String name, @Comment(text = "value") Object v,
				@Comment(text = "boost") int boost) {
			return and(name, v, OP.eq, boost);
		}

//		/**
//		 * set and "and (...)" conditions
//		 *
//		 * @param w the w
//		 * @return W
//		 */
//		@Comment()
//		public W and(@Comment(text = "query") W w) {
//			return and(w, W.AND);
//		}

//		@Comment(text = "and (not ....)")
//		public W andnot(@Comment(text = "query") W w) {
//			return and(w, W.NOT);
//		}

		/**
		 * 
		 * @param w
		 * @param cond, and, or, not
		 * @return
		 */
		@Comment()
		public W and(@Comment(text = "query") W w) {

			if (w.isEmpty())
				return this;

			w.cond = AND;

			// 复制查询条件
			if (this.queryList.isEmpty()) {
				// 自己空的，直接put所有子条件
				if (w instanceof Entity) {
					this.queryList.add(w);
				} else if (!w.queryList.isEmpty()) {
					this.queryList.addAll(w.queryList);
					this.not = w.not;
				}
			} else {
				if (this.queryList.size() > 1 && this.queryList.get(queryList.size() - 1).cond != w.cond) {
					// 本身多个条件， 最后一个cond与现在这个不相等（a or b -> and c) => (a or b) and c
					W e = W.create();
					e.queryList = this.queryList;
					this.queryList = new ArrayList<W>();
					this.queryList.add(e);
				}
				if (w instanceof Entity) {
					this.queryList.add(w);
				} else if (w.queryList.size() == 1) {
					// 去“括弧”
					this.queryList.add(w.queryList.get(0));
				} else if (w.queryList.size() > 1) {
					if (this.queryList.size() == 1 && w.queryList.get(1).cond == w.cond) {
						// 去“括弧”
						this.queryList.addAll(w.queryList);
					} else {
						this.queryList.add(w);
					}
				}
			}

			// 复制分组条件
			if (!X.isEmpty(w.groupby)) {
				if (X.isEmpty(this.groupby)) {
					this.groupby = w.groupby;
				} else {
					List<String> l1 = X.asList(X.split(this.groupby, "[, ]"), s -> s.toString());
					List<String> l2 = X.asList(X.split(w.groupby, "[, ]"), s -> s.toString());
					for (String s2 : l2) {
						if (!l1.contains(s2)) {
							l1.add(s2);
						}
					}
					this.groupby = X.join(l1, ",");
				}
			}

			// 复制排序条件
			if (!w.orderList.isEmpty()) {
				for (Entity e : w.orderList) {
					if (!this.ordered(e.name)) {
						this.orderList.add(e);
					}
				}
				w.orderList.clear();
			}

			return this;
		}

		private boolean ordered(String name) {
			if (this.orderList == null || this.orderList.isEmpty()) {
				return false;
			}

			for (Entity e : this.orderList) {
				if (X.isSame(e.name, name)) {
					return true;
				}
			}
			return false;
		}

		@Comment(text = "转为SQL，使用=")
		public String toSQL() {

			StringBuilder sql = new StringBuilder();

			this.sql(sql);

			return sql.toString();
		}

		@Comment(text = "转为SQL，使用==")
		public String toSQL2() {

			StringBuilder sql = new StringBuilder();

			this.sql2(sql);

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
//					} else if (clause.cond == NOT) {
//						sb.append(" and not ");
					}
				} else if (clause.not) {
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
//			if (this.queryList != null && this.queryList.isEmpty()) {
			if (!this.orderList.isEmpty()) {
				sb.append(" order by ");
				for (int i = 0, len = this.orderList.size(); i < len; i++) {
					if (i > 0) {
						sb.append(",");
					}
					Entity e = this.orderList.get(i);
					sb.append(e.name);
					if (X.isSame(e.value, -1)) {
						sb.append(" desc");
					}
				}
			}
//			}

//			sql.append("(").append(sb.toString()).append(")");
//			if (!sb.isEmpty()) {
			sql.append(sb.toString());
//			}

		}

		void sql2(StringBuilder sql) {

			StringBuilder sb = new StringBuilder();
			for (W clause : queryList) {
				if (sb.length() > 0) {
					if (clause.cond == AND) {
						sb.append(" and ");
					} else if (clause.cond == OR) {
						sb.append(" or ");
//					} else if (clause.cond == NOT) {
//						sb.append(" and not ");
					}
				} else if (clause.not) {
					sb.append(" not ");
				}

//				clause.where(tansfers);
				if (clause instanceof Entity) {
					clause.sql2(sb);
				} else {
					sb.append("(");
					clause.sql2(sb);
					sb.append(")");
				}
			}
//			if (this.queryList != null && this.queryList.isEmpty()) {
			if (!this.orderList.isEmpty()) {
				sb.append(" order by ");
				for (int i = 0, len = this.orderList.size(); i < len; i++) {
					if (i > 0) {
						sb.append(",");
					}
					Entity e = this.orderList.get(i);
					sb.append(e.name);
					if (X.isSame(e.value, -1)) {
						sb.append(" desc");
					}
				}
			}
//			}
//			sql.append("(").append(sb.toString()).append(")");
//			if (!sb.isEmpty()) {
			sql.append(sb.toString());
//			}

		}

		/**
		 * and a SQL
		 * 
		 * @param sql
		 * @return
		 * @throws SQLException
		 */
		@Comment(text = "and条件", demo = ".and('a=1 or b=2')")
		public W and(@Comment(text = "sql") String sql) throws SQLException {
//			try {
			W q = SQL.parse(sql);
			if (!q.isEmpty()) {
				// System.out.println(q);
				this.and(q);
				this.sort(q);
			}
//			} catch (Exception e) {
//				log.error(e.getMessage(), e);
//			}
			return this;
		}

		/**
		 * or a sql
		 * 
		 * @param sql
		 * @return
		 * @throws SQLException
		 */
		@Comment(text = "or条件", demo = ".or('a=1 and b=1')")
		public W or(@Comment(text = "sql") String sql) throws SQLException {
//			try {
			W q = SQL.parse(sql);
			if (!q.isEmpty()) {
//					if (this.isEmpty()) {
//						this.copy(q);
//					} else {
				this.or(q);
//					}
			}
//			} catch (Exception e) {
//				log.error(e.getMessage(), e);
//			}
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

			for (Entity e : q.orderList) {
				Entity e1 = e.copy();
//				e1.container = this;
				this.orderList.add(e1);
			}

			return this;
		}

//		/**
//		 * set a "or (...)" conditions
//		 *
//		 * @param w the w
//		 * @return W
//		 */
//		@Comment()
//		public W or(@Comment(text = "query") W w) {
//			return or(w, W.OR);
//		}

//		@Comment(text = "or (not ...)")
//		public W ornot(@Comment(text = "query") W w) {
//			return or(w, W.NOT);
//		}

		@Comment()
		public W or(@Comment(text = "query") W w) {
			if (w.isEmpty())
				return this;

			w.cond = OR;

			if (queryList.isEmpty()) {
				// 自己空的， 自己put所有子条件
				if (w instanceof Entity) {
					queryList.add(w);
				} else if (!w.queryList.isEmpty()) {
					queryList.addAll(w.queryList);
				}
			} else {
				// 先整理本身查询条件， 把or条件的包裹起来
				if (this.size() > 1 && queryList.get(queryList.size() - 1).cond != w.cond) {
					W e = W.create();
					e.queryList = queryList;
					this.queryList = new ArrayList<W>();
					this.queryList.add(e);
				}
				if (w instanceof Entity) {
					this.queryList.add(w);
				} else if (w.queryList.size() == 1) {
					// 去“括弧”
					this.queryList.add(w.queryList.get(0));
				} else if (w.queryList.size() > 1) {
					if (this.queryList.size() == 1 && w.queryList.get(1).cond == w.cond) {
						// 去“括弧”
						this.queryList.addAll(w.queryList);
					} else {
						this.queryList.add(w);
					}
				}
			}

			// 复制分组条件
			if (!X.isEmpty(w.groupby)) {
				if (X.isEmpty(this.groupby)) {
					this.groupby = w.groupby;
				} else {
					List<String> l1 = X.asList(X.split(this.groupby, "[, ]"), s -> s.toString());
					List<String> l2 = X.asList(X.split(w.groupby, "[, ]"), s -> s.toString());
					for (String s2 : l2) {
						if (!l1.contains(s2)) {
							l1.add(s2);
						}
					}
					this.groupby = X.join(l1, ",");
				}
			}

			// 复制排序字段
			if (!w.orderList.isEmpty()) {
				for (Entity e : w.orderList) {
					if (!this.ordered(e.name)) {
						this.orderList.add(e);
					}
				}
				w.orderList.clear();
			}

			return this;
		}

//		/**
//		 * 获取复杂优化索引排列， 性能比sortkeys 弱
//		 *
//		 * @return List keys
//		 */
//		public List<LinkedHashMap<String, Object>> sortkeys2() {
//
//			String sql = this.toSQL().trim().toLowerCase();
//			try {
//				if (X.isEmpty(sql)) {
//					return new ArrayList<>();
//				}
//				if (sql.startsWith("order ")) {
//					sql = "select * from t " + sql;
//				} else {
//					sql = "select * from t where " + sql;
//				}
//				JSqlParserSqlAnalyzer.SqlAnalyzeResult result = JSqlParserSqlAnalyzer.analyzeSql(sql);
//				return JSqlParserSqlAnalyzer.buildIndexScript(result);
//			} catch (Exception err) {
//				log.error(sql, err);
//			}
//			return new ArrayList<>();
//
//		}

		/**
		 * 获取复杂优化索引排列
		 * 
		 * @return
		 */
		public List<LinkedHashMap<String, Object>> sortkeys() {

			try {

				List<LinkedHashMap<String, Object>> scripts = this._sortkeys();

				if (this.orderList != null && !this.orderList.isEmpty()) {
					for (var f : this.orderList) {
						if (scripts.isEmpty()) {
							LinkedHashMap<String, Object> idMap = new LinkedHashMap<>();
							idMap.put(f.name, f.value);
							scripts.add(idMap);
						} else {
							for (var e : scripts) {
								if (!e.containsKey(f.name)) {
									e.put(f.name, f.value);
								}
							}
						}
					}
				}

				return scripts;
			} catch (Exception err) {
				log.error(this.toSQL(), err);
			}
			return new ArrayList<>();

		}

		protected List<LinkedHashMap<String, Object>> _sortkeys() {

			List<LinkedHashMap<String, Object>> list = new ArrayList<>();

			for (W clause : this.queryList) {
				List<LinkedHashMap<String, Object>> l1 = clause._sortkeys();
				if (list.isEmpty() || clause.cond == W.OR) {
					list.addAll(l1);
				} else {
					for (var e : list) {
						for (var e2 : l1) {
							for (var f : e2.keySet()) {
								if (!e.containsKey(f)) {
									e.put(f, e2.get(f));
								}
							}
						}
					}
				}
			}

			return list;
		}

		/**
		 * @param name
		 * @param v
		 * @return
		 */
		@Comment()
		public W and(@Comment(text = X.NAME) String[] name, @Comment(text = "value") Object v) {
			return and(name, v, W.OP.eq);
		}

		/**
		 * @param name
		 * @param v
		 * @param op
		 * @return
		 */
		@Comment()
		public W and(@Comment(text = X.NAME) String[] name, @Comment(text = "value") Object v,
				@Comment(text = "op") OP op) {
			return and(name, v, op, 1);
		}

		/**
		 * @param name
		 * @param v
		 * @param op
		 * @param boost
		 * @return
		 */
		@Comment(text = "and", demo = "..and(['a', 'b', 'c'], 'a', 10)")
		public W and(@Comment(text = X.NAME) String[] name, @Comment(text = "value") Object v,
				@Comment(text = "op") OP op, @Comment(text = "boost") int boost) {
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

		/**
		 * 一组and条件。多个字段列表时，内部或条件；多个值列表时，内部 and 条件。
		 * 
		 * @param name  字段/列表
		 * @param v     值/列表
		 * @param boost 分组权重
		 * @return
		 */
		@Comment(text = "and", demo = ".and(['a', 'b', 'c'], 'a', [1000, 100])")
		public W and(@Comment(text = X.NAME) Object name, @Comment(text = "value") Object v,
				@Comment(text = "boost") Object boost) {
			List<String> nn = X.asList(name, s -> s.toString());
			List<Object> vv = X.asList(v, s -> s);
			List<Integer> bb = X.asList(boost, s -> X.toInt(s));

			int len = vv.size();
			for (int i = 0; i < len; i++) {
				Object v1 = vv.get(i);
				W q = W.create();
				for (String n1 : nn) {
					q.or(n1, v1, bb.size() > i ? bb.get(i) : 1);
				}
				this.and(q);
			}
			return this;

		}

		@Comment(text = "or", demo = ".or(['a', 'b', 'c'], 'a', [1000, 100])")
		public W or(@Comment(text = X.NAME) Object name, @Comment(text = "value") Object v,
				@Comment(text = "boost") Object boost) {
			List<String> nn = X.asList(name, s -> s.toString());
			List<Object> vv = X.asList(v, s -> s);
			List<Integer> bb = X.asList(boost, s -> X.toInt(s));

			int len = vv.size();
			for (int i = 0; i < len; i++) {
				Object v1 = vv.get(i);
				W q = W.create();
				for (String n1 : nn) {
					q.or(n1, v1, bb.size() > i ? bb.get(i) : 1);
				}
				this.or(q);
			}
			return this;

		}

		@Comment(text = "and", demo = "..and(['a', 'b', 'c'], 'a', [1000, 100])")
		public W and(@Comment(text = X.NAME) String[] name, @Comment(text = "value") Object v,
				@Comment(text = "op") OP op, @Comment(text = "boost") int[] boost) {
			if (v instanceof String) {
				String[] ss = X.split(v.toString(), " ");
				for (String s1 : ss) {
					W q = W.create();
					String[] s2 = X.split(s1, "\\|");
					for (String s3 : s2) {
						for (int i = 0; i < name.length; i++) {
							q.or(name[i], s3, op, boost.length > i ? boost[i] : 1);
						}
					}
					this.and(q);
				}
			} else {
				W q = W.create();
				for (int i = 0; i < name.length; i++) {
					q.or(name[i], v, op, boost.length > i ? boost[i] : 1);
				}
				this.and(q);
			}
			return this;
		}

		/**
		 * @param name
		 * @param v
		 * @param op
		 * @return
		 */
		@Deprecated
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
//			} else if (X.isIn(op, "not like", "!like")) {
//				return and(W.create().and(name, v, W.OP.like), W.NOT);
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
		 * @param name
		 * @param v
		 * @param op
		 * @return
		 */
		@Deprecated
		public W or(String name, Object v, String op) {
			return or(name, v, op, 1);
		}

		/**
		 * 
		 * @param name
		 * @param v
		 * @param op
		 * @param boost
		 * @return
		 */
		@Deprecated
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
//			} else if (X.isIn(op, "not like", "!like")) {
//				return or(W.create().and(name, v, W.OP.like, boost), W.NOT);
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
		@Comment()
		public W and(@Comment(text = X.NAME) String name, @Comment(text = "value") Object v,
				@Comment(text = "op") OP op) {
			return and(name, v, op, 1);
		}

		@Comment()
		public W scan(@Comment(text = "jsfunc") Consumer<Entity> func) {

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

			for (int i = orderList.size() - 1; i >= 0; i--) {
				Entity e1 = orderList.get(i);

				Object value = e1.value;

				func.accept(e1);

				if (!X.isSame(value, e1.value)) {
					e1.replace(value);
				}
			}

			return this;
		}

		@Comment()
		public W scan(@Comment(text = "jsfunc") ScriptObjectMirror m) {
			return this.scan(e -> {
				m.call(e);
			});
		}

		@SuppressWarnings({ "rawtypes" })
		@Comment()
		public W and(@Comment(text = X.NAME) String name, @Comment(text = "value") Object v,
				@Comment(text = "op") OP op, @Comment(text = "boost") int boost) {
			if (X.isEmpty(name))
				return this;

			name = name.toLowerCase();

			if (v instanceof Undefined) {
				v = null;
			}

			if (v != null && X.isArray(v)) {

				List l1 = X.asList(v, s -> s);

				if (l1.isEmpty()) {
					this.and(name, null, op);
				} else if (l1.size() == 1) {
					this.and(name, l1.get(0), op);
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
				if (queryList.size() > 1 && queryList.get(queryList.size() - 1).cond != AND) {
					W e = W.create();
					e.queryList = queryList;
					this.queryList = new ArrayList<W>();
					this.queryList.add(e);
				}
				queryList.add(new Entity(name, v, op, AND, boost));
			}
			return this;
		}

		@Comment()
		public W and(@Comment(text = "names") Object name, @Comment(text = "op") String op,
				@Comment(text = "values") Object v, @Comment(text = "boosts") Object boost) {

			List<String> nn = X.asList(name, s -> s.toString());
			List<Object> vv = X.asList(v, s -> s);
			List<Integer> bb = X.asList(boost, s -> X.toInt(s));

			int len = nn.size();
			for (int i = 0; i < len; i++) {
				String s1 = nn.get(i);
				W q = W.create();
				for (Object s2 : vv) {
					q.or(s1, s2, OP.valueOf(op), bb.size() > i ? bb.get(i) : 1);
				}
				this.and(q);
			}
			return this;

		}

		@Comment()
		public W or(@Comment(text = X.NAME) String[] name, @Comment(text = "value") Object v) {
			return or(name, v, W.OP.eq);
		}

		@Comment()
		public W or(@Comment(text = X.NAME) String[] name, @Comment(text = "value") Object v,
				@Comment(text = "op") OP op) {
			return or(name, v, op, 1);
		}

		@Comment()
		public W or(@Comment(text = X.NAME) String[] name, @Comment(text = "value") Object v,
				@Comment(text = "op") OP op, @Comment(text = "boost") int boost) {
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
		@Comment()
		public W or(@Comment(text = X.NAME) String name, @Comment(text = "value") Object v) {
			return or(name, v, 1);
		}

		@Comment()
		public W or(@Comment(text = X.NAME) String name, @Comment(text = "value") Object v,
				@Comment(text = "boost") int boost) {
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
		@Comment()
		public W or(@Comment(text = X.NAME) String name, @Comment(text = "value") Object v,
				@Comment(text = "op") OP op) {
			return or(name, v, op, 1);
		}

		@SuppressWarnings({ "rawtypes" })
		@Comment()
		public W or(@Comment(text = X.NAME) String name, @Comment(text = "value") Object v, @Comment(text = "op") OP op,
				@Comment(text = "boost") int boost) {

			if (X.isEmpty(name))
				return this;

			if (this.queryList.isEmpty())
				return and(name, v, op, boost);

			name = name.toLowerCase();

			if (v instanceof Undefined) {
				v = null;
			}

			if (v != null && X.isArray(v)) {

				List l1 = X.asList(v, s -> s);

				if (l1.isEmpty()) {
					this.or(name, null, op);
				} else if (l1.size() == 1) {
					this.or(name, l1.get(0), op);
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
		 * copy the value in jo, the format of name is: [X.NAME, "table field name" ].
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

		@Comment(text = "creat XY object by xy")
		public XY xy(String xy, String distance) {
			return XY.create(xy, distance);
		}

		@Comment(text = "creat XY object by xy")
		public XY xy(@Comment(text = "x") double x, @Comment(text = "y") double y, String distance) {
			return XY.create(x, y, distance);
		}

		/**
		 * 二维地理位置查询
		 */
		public static class XY {

			public double x;
			public double y;
			public String xy;
			public String distance;

			public static XY create(double x, double y, String distance) {
				XY e = new XY();
				e.x = x;
				e.y = y;
				e.xy = x + "," + y;
				e.distance = distance;
				return e;
			}

			public static XY create(String xy, String distance) {
				XY e = new XY();
				e.xy = xy;
				String[] ss = X.split(xy, "[,]");
				e.x = X.toDouble(ss[0]);
				e.y = X.toDouble(ss[1]);
				e.distance = distance;
				return e;
			}

		}

		/**
		 * 向量条件
		 */
		public static class Vector {

			@SuppressWarnings("rawtypes")
			public List value;

			@SuppressWarnings("rawtypes")
			public static Vector create(List l1) {
				Vector e = new Vector();
				e.value = l1;
				return e;
			}

			@Override
			public String toString() {
				return "Vector [value=" + value + "]";
			}

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

//			public int getCondition() {
//				return cond;
//			}

			private List<Object> args(List<Object> list) {

				if (value instanceof Object[]) {
					for (Object o : (Object[]) value) {
						list.add(o);
					}
				} else if (op == OP.like) {
					String s = X.isEmpty(value) ? X.EMPTY : value.toString();
					if (!s.startsWith("%")) {
						s = "%" + s;
					}
					if (!s.endsWith("%")) {
						s += "%";
					}
					list.add(s);
				} else if (op == OP.like_) {
					String s = X.isEmpty(value) ? X.EMPTY : value.toString();
					if (!s.endsWith("%")) {
						s += "%";
					}
					list.add(s);
				} else if (op == OP.like_$) {
					String s = X.isEmpty(value) ? X.EMPTY : value.toString();
					if (!s.startsWith("%")) {
						s = "%" + s;
					}
					list.add(s);
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
				return new Entity(j1.getString(X.NAME), j1.get("value"), OP.valueOf(j1.getString("op")),
						j1.getInt("cond"), j1.getInt("boost", 1));
			}

			protected List<LinkedHashMap<String, Object>> _sortkeys() {

				List<LinkedHashMap<String, Object>> result = new ArrayList<>();

				LinkedHashMap<String, Object> idmap = new LinkedHashMap<>();
				if (op == OP.lt || op == OP.lte || X.isSame(value, -1)) {
					idmap.put(name, -1);
				} else {
					idmap.put(name, 1);
				}
				result.add(idmap);
				return result;

			}

			public BasicDBObject query() {

				if (value != null) {
					if (value instanceof Date) {
						value = ((Date) value).getTime();
					}
				}

				if (this.not) {
					if (op == OP.eq) {
						if (value == null) {
							BasicDBList l1 = new BasicDBList();
//							l1.add(new BasicDBObject(name, new BasicDBObject("$exists", false)));
//							l1.add(new BasicDBObject(name, value));
//							return new BasicDBObject("$not", new BasicDBObject("$or", l1));
							l1.add(new BasicDBObject(name, new BasicDBObject("$exists", true)));
							l1.add(new BasicDBObject(name, new BasicDBObject("$ne", value)));
							return new BasicDBObject("$and", l1);
						} else {
							return new BasicDBObject(name, new BasicDBObject("$ne", value));
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
						value = _escapeRegex(value.toString());
						String s = value.toString();
						if (!s.startsWith(".*") && !s.startsWith("^")) {
							s = ".*" + s;
						}
						if (!s.endsWith(".*") && !s.endsWith("$")) {
							s += ".*";
						}
						Pattern p1 = Pattern.compile(s);
						return new BasicDBObject(name, new BasicDBObject("$not", p1));
//					} else if (op == OP.like_) {
//						Pattern p1 = Pattern.compile("^" + value);
//						return new BasicDBObject(name, new BasicDBObject("$not", p1));
//					} else if (op == OP.like_$) {
//						Pattern p1 = Pattern.compile(value + "$");
//						return new BasicDBObject(name, new BasicDBObject("$not", p1));
					} else if (op == OP.neq) {
						if (value == null) {
							BasicDBList l1 = new BasicDBList();
//							l1.add(new BasicDBObject(name, new BasicDBObject("$exists", false)));
//							l1.add(new BasicDBObject(name, value));
//							return new BasicDBObject("$not", l1);
							l1.add(new BasicDBObject(name, new BasicDBObject("$exists", true)));
							l1.add(new BasicDBObject(name, new BasicDBObject("$ne", value)));
							return new BasicDBObject("$and", l1);
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
							BasicDBList l1 = new BasicDBList();
							l1.add(new BasicDBObject(name, new BasicDBObject("$exists", false)));
							l1.add(new BasicDBObject(name, value));
							return new BasicDBObject("$or", l1);
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
						value = _escapeRegex(value.toString());
						String s = value.toString();
						if (!s.startsWith(".*") && !s.startsWith("^")) {
							s = ".*" + s;
						}
						if (!s.endsWith(".*") && !s.endsWith("$")) {
							s += ".*";
						}
						Pattern p1 = Pattern.compile(s);
						return new BasicDBObject(name, p1);
//					} else if (op == OP.like_) {
//						value = _escapeRegex(value.toString());
//						Pattern p1 = Pattern.compile("^" + value + ".*");
//						return new BasicDBObject(name, p1);
//					} else if (op == OP.like_$) {
//						value = _escapeRegex(value.toString());
//						Pattern p1 = Pattern.compile(".*" + value + "$");
//						return new BasicDBObject(name, p1);
					} else if (op == OP.neq) {
						if (value == null) {
							BasicDBList l1 = new BasicDBList();
//							l1.add(new BasicDBObject(name, new BasicDBObject("$exists", false)));
//							l1.add(new BasicDBObject(name, value));
//							return new BasicDBObject("$not", l1);
							l1.add(new BasicDBObject(name, new BasicDBObject("$exists", true)));
							l1.add(new BasicDBObject(name, new BasicDBObject("$ne", value)));
							return new BasicDBObject("$and", l1);

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

			private String _escapeRegex(String s) {

				String str1 = "*.?+$^[](){}|\\/";

				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < s.length(); i++) {
					char c = s.charAt(i);
					if (c == '"') {
						sb.append("\\\\\"");
					} else {
						String ss = String.valueOf(c);
						if (str1.contains(ss)) {
							ss = "\\" + ss;
						}
						sb.append(ss);
					}
				}

				return sb.toString().replaceAll("%", ".*");
			}

			/**
			 * To json.
			 *
			 * @return the json
			 */
			public JSON toJSON() {
				JSON jo = JSON.create();
				jo.put(X.NAME, name);
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

			public void sql2(StringBuilder sql) {

//				if (value == null)
//					return;

				sql.append(name);

				if (op == OP.eq) {
					sql.append("==");
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

			public boolean isEmpty() {
				return false;
			}

			public String where(Map<String, String> tansfers) {
				// TODO, for "null" value, some db "is null"

				StringBuilder sb = new StringBuilder();

				if (this.not) {
					sb.append("not ");
				}
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
					} else if (op == OP.like) {
						sb.append(" not like ?");
					} else if (op == OP.neq) {
						sb.append(" <> ?");
					}

					sb.append(value);

					if (this.not) {
						tostring = "not " + sb.toString();
					} else {
						tostring = sb.toString();
					}
				}
				return tostring;
			}

			public Entity() {

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

		transient BasicDBObject _query;

		public BasicDBObject query() {
			if (_query == null) {
				BasicDBList list = new BasicDBList();

				for (W clause : queryList) {
					list.add(clause.query());
				}

				if (list.isEmpty()) {
					_query = new BasicDBObject();
				} else if (list.size() == 1) {
					if (this.not && !this.queryList.get(0).not) {
						// 外面有not， 里面没有
						_query = new BasicDBObject("$ne", list.get(0));
					} else {
						_query = (BasicDBObject) list.get(0);
					}
				} else {
					if (queryList.get(1).cond == OR) {
						if (this.not) {
							_query = new BasicDBObject("$not", new BasicDBObject().append("$or", list));
						} else {
							_query = new BasicDBObject().append("$or", list);
						}
					} else {
						if (this.not) {
							_query = new BasicDBObject("$not", new BasicDBObject().append("$and", list));
						} else {
							_query = new BasicDBObject().append("$and", list);
						}
					}
				}
			}
			return _query;
		}

		transient BasicDBObject _order;

		/**
		 * Order.
		 *
		 * @return the basic db object
		 */
		public BasicDBObject order() {
			if (_order == null) {
				BasicDBObject q = new BasicDBObject();
				if (orderList != null && orderList.size() > 0) {
					for (Entity e : orderList) {
						q.append(e.name, e.value);
					}
				}
				_order = q;
			}
			return _order;
		}

		/**
		 * Sort as asc
		 * 
		 * @param name
		 * @return
		 */
		@Comment()
		public W sort(@Comment(text = X.NAME) String name) {
			return sort(name, 1);
		}

		@Comment()
		public W sort(@Comment(text = "q") W q) {
			for (Entity e : q.orderList) {
				if (!this.ordered(e.name)) {
					this.orderList.add(e);
				}
			}
			return this;
		}

		@Comment(text = "clear sort")
		public W clearSort() {
			orderList.clear();
			return this;
		}

		@Comment()
		public W sort(@Comment(text = X.NAME) String name, @Comment(text = "type") String type) {
			return sort(name, 1, type);
		}

		/**
		 * Sort.
		 *
		 * @param name the name
		 * @param i    the i
		 * @return the w
		 */
		@Comment()
		public W sort(@Comment(text = X.NAME) String name, @Comment(text = "i") int i) {
			return sort(name, i, null);
		}

		@Comment()
		public W sort(@Comment(text = X.NAME) String name, @Comment(text = "i") int i,
				@Comment(text = "type") String type) {
			if (X.isEmpty(name))
				return this;

			for (Entity e : orderList) {
				if (X.isSame(e.name, name)) {
					return this;
				}
			}

			Entity e = new Entity(name, i, OP.eq, AND, 0);
			e.type = type;
			orderList.add(e);
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

		@Deprecated
		@Comment(hide = true)
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

		@Deprecated
		@Comment(hide = true)
		public boolean stream(Function<Data, Boolean> func) throws Exception {
			return stream(0, func);
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Deprecated
		@Comment(hide = true)
		public boolean stream(long offset, Function<Data, Boolean> func) throws Exception {

			if (access != null) {
				access.checkRead(table);
			}

			if (dao != null) {

				return dao.stream(this, offset, (Function) func);

			} else if (!X.isEmpty(table)) {
				W q = this;

				helper.getOptimizer().query(table, q);

				return helper.stream(table, q, offset, func, Data.class);
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
		@Deprecated
		@Comment(hide = true)
		public <T> T load() throws SQLException {

			if (access != null) {
				access.checkRead(table);
			}

			if (log.isDebugEnabled()) {
				log.debug("w=" + this);
			}

			T t1 = null;
			if (dao != null) {
				t1 = (T) dao.load(this);
			} else if (!X.isEmpty(table)) {
				W q = this;

				helper.getOptimizer().query(table, q);

				t1 = (T) helper.load(table, q, t, false);
				if (access != null && t1 != null) {
					access.decode(table, (Bean) t1);
//				} else if (log.isWarnEnabled()) {
//					log.warn("can not decode, access=" + access + ", t1=" + t1);
				}
				return t1;
			} else {
				throw new SQLException("not set table");
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
		@Deprecated
		@Comment(hide = true)
		public <T> T load(boolean trace) throws SQLException {

			if (access != null) {
				access.checkRead(table);
			}

			if (log.isDebugEnabled()) {
				log.debug("w=" + this);
			}

			T t1 = null;
			if (dao != null) {
				t1 = (T) dao.load(this);
			} else if (!X.isEmpty(table)) {
				W q = this;

				helper.getOptimizer().query(table, q);

				t1 = (T) helper.load(table, q, t, trace);
			} else {
				throw new SQLException("not set table");
			}

			if (access != null && t1 != null) {
				access.decode(table, t1);
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
		@Deprecated
		@Comment(hide = true)
		public <T> T load(Consumer<T> func) throws SQLException {

			if (access != null) {
				access.checkRead(table);
			}

			T t1 = null;
			if (dao != null) {
				t1 = (T) dao.load(this, (Consumer) func);
			} else if (!X.isEmpty(table)) {
				Lock door = Global.getLock("data." + table);
				door.lock();
				try {
					W q = this;

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

			if (access != null && t1 != null) {
				access.decode(table, t1);
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
		@Deprecated
		@Comment(hide = true)
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
		@Deprecated
		@Comment(hide = true)
		public <T> T get(String name) throws SQLException {
			Bean b = load();
			return b == null ? null : (T) b.get(name);
		}

		@Deprecated
		@Comment(hide = true)
		public long getLong(String name) throws SQLException {
			Bean b = load();
			return b == null ? 0 : b.getLong(name);
		}

		@Deprecated
		@Comment(hide = true)
		public long getLong(int offset, String name) throws SQLException {
			Bean b = load(offset);
			return b == null ? 0 : b.getLong(name);
		}

		@Deprecated
		@Comment(hide = true)
		public double getDouble(String name) throws SQLException {
			Bean b = load();
			return b == null ? 0 : b.getDouble(name);
		}

		@Deprecated
		@Comment(hide = true)
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
		@Deprecated
		@Comment(hide = true)
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
		@Deprecated
		@Comment(hide = true)
		public <T extends Bean> Beans<T> load(int s, int n) throws SQLException {

			if (access != null) {
				access.checkRead(table);
			}

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
				l1.forEach(e -> {
					access.decode(table, e);
				});
			}

			return l1;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Deprecated
		@Comment(hide = true)
		public <T extends Bean> Beans<T> load(int s, int n, Consumer<Beans<T>> func) throws SQLException {

			if (access != null) {
				access.checkRead(table);
			}

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

					helper.getOptimizer().query(table, q);

					l1 = helper.load(table, q, s, n, t);
					l1.table = table;
					l1.q = this;
					if (func != null && l1 != null && !l1.isEmpty()) {
						if (access != null) {
							l1.forEach(e -> {
								access.decode(table, e);
							});
						}
						func.accept(l1);
					}

				} finally {
					door.unlock();
				}
			} else {
				throw new SQLException("not set table");
			}

			return l1;
		}

		@Deprecated
		@Comment(text = "按照当前条件统计条数", hide = true)
		public long count() throws SQLException {

			if (access != null) {
				access.checkRead(table);
			}

			if (dao != null) {
				return dao.count(this);
			} else if (!X.isEmpty(table)) {
				W q = this;

				helper.getOptimizer().query(table, q);

				return helper.count(table, q);
			}

			throw new SQLException("not set table");

		}

		@Deprecated
		@Comment(hide = true)
		public <T> T sum(String name) throws SQLException {

			if (access != null) {
				access.checkRead(table);
			}

			if (dao != null) {
				return dao.sum(name, this);
			} else if (!X.isEmpty(table)) {
				W q = this;

				helper.getOptimizer().query(table, q);

				return helper.sum(table, q, name);
			}
			throw new SQLException("not set table");

		}

		@Deprecated
		@Comment(hide = true)
		public <T> T avg(String name) throws SQLException {

			if (access != null) {
				access.checkRead(table);
			}

			if (dao != null) {
				return dao.avg(name, this);
			} else if (!X.isEmpty(table)) {
				W q = this;

				helper.getOptimizer().query(table, q);

				return helper.avg(table, q, name);
			}
			throw new SQLException("not set table");

		}

		@SuppressWarnings("unchecked")
		@Deprecated
		@Comment(hide = true)
		public <T> T min(String name) throws SQLException {

			if (access != null) {
				access.checkRead(table);
			}

			if (dao != null) {
				return dao.min(name, this);
			} else if (!X.isEmpty(table)) {

				W q = this;

				q.sort(name);

				helper.getOptimizer().query(table, q);

				Data d = helper.load(table, q, Data.class, false);
				if (d != null) {
					return (T) d.get(name);
				}
				return null;
			}
			throw new SQLException("not set table");

		}

		@SuppressWarnings("unchecked")
		@Deprecated
		@Comment(hide = true)
		public <T> T max(String name) throws SQLException {

			if (access != null) {
				access.checkRead(table);
			}

			if (dao != null) {
				return dao.max(name, this);
			} else if (!X.isEmpty(table)) {
				W q = this;

				q.sort(name, -1);

				helper.getOptimizer().query(table, q);

				Data d = helper.load(table, q, Data.class, false);
				if (d != null) {
					return (T) d.get(name);
				}
				return null;
			}
			throw new SQLException("not set table");

		}

		@Deprecated
		@Comment(hide = true)
		public <T> T median(String name) throws SQLException {

			if (access != null) {
				access.checkRead(table);
			}

			if (dao != null) {
				return dao.median(name, this);
			} else if (!X.isEmpty(table)) {
				W q = this;

				helper.getOptimizer().query(table, q);

				return helper.median(table, q, name);

			}
			throw new SQLException("not set table");

		}

		@Deprecated
		@Comment(hide = true)
		public int delete() throws SQLException {

			if (access != null) {
				access.checkWrite(table);
			}

			if (dao != null) {
				return dao.delete(this);
			} else if (!X.isEmpty(table)) {

				W q = this;

				helper.getOptimizer().query(table, q);

				return helper.delete(table, q);
			}
			throw new SQLException("not set table");
		}

		@Deprecated
		@Comment(hide = true)
		public int update(Object o) throws SQLException {

			V v = null;
			if (o instanceof V) {
				v = (V) o;
			} else {
				JSON j1 = JSON.fromObject(o);
				v = V.fromJSON(j1);
			}

			if (access != null) {
				access.checkWrite(table);
				access.encode(table, v);
			}

			if (dao != null) {
				return dao.update(this, v);
			} else if (!X.isEmpty(table)) {

				W q = this;

				helper.getOptimizer().query(table, q);

				return helper.updateTable(table, q, v);
			}
			throw new SQLException("not set table");
		}

		@Deprecated
		@Comment(hide = true)
		public List<?> distinct(String name) throws SQLException {

			if (access != null) {
				access.checkRead(table);
			}

			if (dao != null) {
				return dao.distinct(name, this);
			} else if (!X.isEmpty(table)) {
				W q = this;

				helper.getOptimizer().query(table, q);

				return helper.distinct(table, name, q);
			}
			throw new SQLException("not set table");
		}

		@Deprecated
		@Comment(hide = true)
		public List<JSON> count(String group, int n) throws SQLException {

			if (access != null) {
				access.checkRead(table);
			}

			if (dao != null) {
				return dao.count(this, X.split(group, "[,]"), n);
			} else if (!X.isEmpty(table)) {
				W q = this;

				helper.getOptimizer().query(table, q);

				return helper.count(table, q, X.split(group, "[,]"), n);
			}
			throw new SQLException("not set table");

		}

		@Deprecated
		@Comment(hide = true)
		public List<JSON> count(String name, String group, int n) throws SQLException {

			if (access != null) {
				access.checkRead(table);
			}

			if (dao != null) {
				return dao.count(this, X.split(group, "[,]"), n);
			} else if (!X.isEmpty(table)) {
				W q = this;

				helper.getOptimizer().query(table, q);

				return helper.count(table, q, name, X.split(group, "[,]"), n);
			}
			throw new SQLException("not set table");

		}

		@Deprecated
		@Comment(hide = true)
		public List<JSON> sum(String name, String group) throws SQLException {

			if (access != null) {
				access.checkRead(table);
			}

			if (dao != null) {
				return dao.sum(this, name, X.split(group, "[,]"));
			} else if (!X.isEmpty(table)) {
				W q = this;

				helper.getOptimizer().query(table, q);

				return helper.sum(table, q, name, X.split(group, "[,]"));
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
		@Deprecated
		@Comment(hide = true)
		public List<JSON> aggregate(String name, String group) throws SQLException {

			if (access != null) {
				access.checkRead(table);
			}

			if (dao != null) {
				return dao.aggregate(this, X.split(name, "[,]"), X.split(group, "[,]"));
			} else if (!X.isEmpty(table)) {
				W q = this;

				helper.getOptimizer().query(table, q);

				return helper.aggregate(table, X.split(name, "[,]"), q, X.split(group, "[,]"));
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

		@Deprecated
		@Comment(hide = true)
		public List<JSON> avg(String name, String group) throws SQLException {

			if (access != null) {
				access.checkRead(table);
			}

			if (dao != null) {
				return dao.avg(this, name, X.split(group, "[,]"));
			} else if (!X.isEmpty(table)) {
				W q = this;

				helper.getOptimizer().query(table, q);

				return helper.avg(table, q, name, X.split(group, "[,]"));
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

		private int offset = 0;

		public W offset(int offset) {
			this.offset = offset;
			return this;
		}

		public int offset() {
			return offset;
		}

		private int limit = 10;

		public String command;

		public List<Object> params;

		public W limit(int limit) {
			this.limit = limit;
			return this;
		}

		public int limit() {
			return limit;
		}

		public W not() {
			if (this.not) {
				this.not = false;
			} else {
				this.not = true;
			}
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
		String table = q.table();
		if (X.isEmpty(table)) {
			table = getTable(t);
		}
		return load(table, q, t);
	}

	static <T extends Bean> T load(W q, Class<T> t, boolean trace) throws SQLException {
		String table = q.table();
		if (X.isEmpty(table)) {
			table = getTable(t);
		}
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

		if (primary == null) {
			return null;
		}

		TimeStamp t1 = TimeStamp.create();
		try {
//
//			if (monitor != null) {
//				monitor.query(table, q);
//			}

			return primary.load(table, q, t, trace);

		} finally {
			read.add(t1.pastms(), "table=%s, q=%s", table, q);
		}
	}

	public static <T extends Bean> Beans<T> load(String table, W q, int s, int n, Class<T> t) throws SQLException {

		if (!Helper.isConfigured()) {
			return null;
		}

		TimeStamp t1 = TimeStamp.create();
		try {

//			if (monitor != null) {
//				monitor.query(table, q);
//			}

			return primary.load(table, q, s, n, t);

		} finally {
			read.add(t1.pastms(), "table=%s, q=%s, offset=%d", table, q, s);
		}
	}

	/**
	 * batch insert
	 * 
	 * @param table
	 * @param values
	 * @return
	 * @throws SQLException
	 */
	public static int insert(String table, List<V> values) throws SQLException {

		TimeStamp t1 = TimeStamp.create();
		try {

//			for (V value : values) {
//				value.set(X.CREATED, Global.now()).set(X.UPDATED, Global.now());
//
//			}

			return primary.insertTable(table, values);
		} finally {
			write.add(t1.pastms(), "table=%s", table);
		}
	}

	/**
	 * insert
	 * 
	 * @param value the value
	 * @param table the table
	 * @param db    the db name
	 * @return the number inserted
	 * @throws SQLException
	 */
	public static int insert(String table, V value) throws SQLException {

		TimeStamp t1 = TimeStamp.create();
		try {
			return primary.insertTable(table, value);
		} finally {
//			Helper.Stat.write(table, t1.pastms());
			write.add(t1.pastms(), "table=%s", table);
		}
	}

	/**
	 * update the table by the query with the values.
	 *
	 * @param table  the table
	 * @param q      the query
	 * @param values the values
	 * @return the number of updated
	 * @throws SQLException
	 */
	public static int update(String table, W q, V values) throws SQLException {

		TimeStamp t1 = TimeStamp.create();
		try {
//
//			if (monitor != null) {
//				monitor.query(table, q);
//			}

			return primary.updateTable(table, q, values);

		} finally {
			write.add(t1.pastms(), "table=%s, q=%s", table, q);
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
	 * @throws SQLException
	 */
	public static int inc(String table, String name, int n, W q, V v) throws SQLException {

		TimeStamp t1 = TimeStamp.create();
		try {
//			if (monitor != null) {
//				monitor.query(table, q);
//			}

			return primary.inc(table, q, name, n, v);
		} finally {
			write.add(t1.pastms(), "table=%s, name=%s, q=%", table, name, q);

		}
	}

	public static int inc(String table, JSON incvalue, W q, V v) throws SQLException {

		TimeStamp t1 = TimeStamp.create();
		try {
//			if (monitor != null) {
//				monitor.query(table, q);
//			}

			return primary.inc(table, q, incvalue, v);
		} finally {
			write.add(t1.pastms(), "table=%s, name=%s, q=%", table, incvalue, q);

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

	/**
	 * count the table by query
	 * 
	 * @param table
	 * @param q
	 * @return
	 * @throws SQLException
	 */
	public static long count(String table, W q) throws SQLException {

		TimeStamp t1 = TimeStamp.create();
		try {

//			if (monitor != null) {
//				monitor.query(table, q);
//			}

			return primary.count(table, q);
		} finally {
			read.add(t1.pastms(), "table=%s, q=%s", table, q);
		}
	}

	/**
	 * count
	 * 
	 * @param table
	 * @param name
	 * @param q
	 * @return
	 * @throws SQLException
	 */
	public static long count(String table, String name, W q) throws SQLException {

		TimeStamp t1 = TimeStamp.create();
		try {

//			if (monitor != null) {
//				monitor.query(table, q);
//			}

			return primary.count(table, q, name);
		} finally {
			read.add(t1.pastms(), "table=%s, name=%s, q=%", table, name, q);
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

//			if (monitor != null) {
//				monitor.query(table, q);
//			}

			return primary.median(table, q, name);
		} finally {
			read.add(t1.pastms(), "table=%s, name=%s, q=%s", table, name, q);
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
			read.add(t1.pastms(), "table=%s, name=%s, q=%s", table, name, q);

//			if (t1.pastms() > Optimizer.MIN && monitor != null) {
//				monitor.query(table, q);
//			}

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
//
//			if (monitor != null) {
//				monitor.query(table, q);
//			}

			return primary.sum(table, q, name);
		} finally {
			read.add(t1.pastms(), "table=%s, name=%s, q=%s", table, name, q);
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
		@SuppressWarnings("deprecation")
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
		@SuppressWarnings("deprecation")
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
	 * @throws SQLException
	 */
	public static <T> T avg(String table, String name, W q) throws SQLException {

		TimeStamp t1 = TimeStamp.create();
		try {

//			if (monitor != null) {
//				monitor.query(table, q);
//			}

			return primary.avg(table, q, name);
		} finally {
			read.add(t1.pastms(), "table=%s, name=%s, q=%", table, name, q);

		}
	}

	public static List<JSON> avg(String table, String name, W q, String[] group) {

		TimeStamp t1 = TimeStamp.create();
		try {

//			if (monitor != null) {
//				monitor.query(table, q);
//			}

			return primary.avg(table, q, name, group);
		} finally {
			read.add(t1.pastms(), "table=%s, name=%s, q=%", table, name, q);

		}
	}

	/**
	 * distinct the name by query
	 * 
	 * @param table
	 * @param name
	 * @param q
	 * @return
	 * @throws SQLException
	 */
	public static List<?> distinct(String table, String name, W q) throws SQLException {

		TimeStamp t1 = TimeStamp.create();
		try {

//			if (monitor != null) {
//				monitor.query(table, q);
//			}

			return primary.distinct(table, name, q);

		} finally {
			read.add(t1.pastms(), "table=%s, name=%s, q=%", table, name, q);

		}
	}

	/**
	 * 
	 */
	public static void enableOptmizer() {
		monitor = new Optimizer(Helper.primary);
	}

	public static void createIndex(String table, LinkedHashMap<String, Object> ss, boolean unique) {

		TimeStamp t1 = TimeStamp.create();
		try {
			primary.createIndex(table, ss, unique);
		} finally {
			write.add(t1.pastms(), "table=%s", table);
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
			read.add(t1.pastms(), "table=%s", table);
		}

	}

	public static void dropIndex(String table, String name) {

		TimeStamp t1 = TimeStamp.create();
		try {
			primary.dropIndex(table, name);
		} finally {
			write.add(t1.pastms(), "table=%s, name=%s", table, name);
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
	 * 数据助手接口
	 * 
	 * @author wujun
	 * @since 3.3
	 *
	 */
	public interface DBHelper {

		/**
		 * 修复数据表
		 * 
		 * @param table - 数据表
		 * @since 3.3
		 */
		void repair(String table);

		/**
		 * 获取数据表的所有索引
		 * 
		 * @param table - 数据表
		 * @return
		 * @since 3.3
		 */
		List<Map<String, Object>> getIndexes(String table);

		/**
		 * 删除索引
		 * 
		 * @param table - 数据表
		 * @param name  - 索引名
		 * @since 3.3
		 */
		void dropIndex(String table, String name);

		/**
		 * 创建索引
		 * 
		 * @param table  - 数据表
		 * @param ss     - 索引字段
		 * @param unique - True，唯一
		 * @since 3.3
		 */
		void createIndex(String table, LinkedHashMap<String, Object> ss, boolean unique);

		/**
		 * 装载数据
		 * 
		 * @param <T>
		 * @param table - 数据表
		 * @param q     - 查询条件
		 * @param s     - 起始位置
		 * @param n     - 条数
		 * @param t     - 映射类
		 * @return
		 * @throws SQLException
		 * @since 3.3
		 */
		<T extends Bean> Beans<T> load(String table, W q, int s, int n, Class<T> t) throws SQLException;

		/**
		 * 流式访问数据
		 * 
		 * @param <T>
		 * @param table  - 数据表
		 * @param q      - 查询条件
		 * @param offset - 起始位置
		 * @param func   - 回调函数
		 * @param t      - 映射类
		 * @return
		 * @throws SQLException
		 * @since 3.3
		 */
		<T extends Bean> boolean stream(String table, W q, long offset, Function<T, Boolean> func, Class<T> t)
				throws SQLException;

		<T extends Bean> T load(String table, W q, Class<T> clazz);

		<T extends Bean> T load(String table, W q, Class<T> clazz, boolean trace);

		Connection getConnection() throws SQLException;

		int delete(String table, W q);

		void drop(String table);

		boolean exists(String table, W q);

		int insertTable(String table, V value) throws SQLException;

		int insertTable(String table, JSON value) throws SQLException;

		@SuppressWarnings("rawtypes")
		int insertTable(String table, List values) throws SQLException;

		int updateTable(String table, W q, V values) throws SQLException;

		int updateTable(String table, W q, JSON value) throws SQLException;

		int inc(String table, W q, String name, int n, V v) throws SQLException;

		int inc(String table, W q, String name, float n, V v) throws SQLException;

		int inc(String table, W q, JSON incvalue, V v) throws SQLException;

		long count(String table, W q, String name) throws SQLException;

		long count(String table, W q) throws SQLException;

		<T> T max(String table, W q, String name);

		<T> T min(String table, W q, String name);

		<T> T sum(String table, W q, String name);

		<T> T avg(String table, W q, String name) throws SQLException;

		<T> T std_deviation(String table, W q, String name);

		<T> T median(String table, W q, String name);

		List<?> distinct(String table, String name, W q) throws SQLException;

		List<JSON> sum(String table, W q, String name, String[] group);

		List<JSON> avg(String table, W q, String name, String[] group);

		List<JSON> count(String table, W q, String[] group, int n) throws SQLException;

		List<JSON> count(String table, W q, String name, String[] group, int n) throws SQLException;

		List<JSON> aggregate(String table, String[] func, W q, String[] group);

		List<JSON> listTables(String tablename, int n);

		void close();

		List<JSON> getMetaData(String tablename);

		void repair();

		List<JSON> listDB();

		void killOp(Object id);

		/**
		 * 复制数据
		 * 
		 * @param src    - 源数据表
		 * @param dest   - 目的数据表
		 * @param filter - 查询条件
		 * @since 3.3
		 */
		void copy(String src, String dest, W filter) throws SQLException;

		/**
		 * 创建表
		 * 
		 * @param tablename  - 数据表名称
		 * @param memo       - 备注
		 * @param cols       - 列定义
		 * @param properties - 扩展熟悉
		 * @throws SQLException
		 * @since 3.3
		 */
		void createTable(String tablename, String memo, List<JSON> cols, JSON properties) throws SQLException;

		/**
		 * 修改表， 删除列
		 * 
		 * @param tablename - 数据表
		 * @param colname   - 列名称
		 * @throws SQLException
		 * @since 3.3
		 */
		void delColumn(String tablename, String colname) throws SQLException;

		/**
		 * 修改表， 添加列
		 * 
		 * @param tablename - 数据表
		 * @param colname   - 列信息
		 * @throws SQLException
		 * @since 3.3
		 */
		void addColumn(String tablename, JSON col) throws SQLException;

		/**
		 * alter colname
		 * 
		 * @param tablename
		 * @param colname
		 * @throws SQLException
		 */
		void alterColumn(String tablename, JSON col) throws SQLException;

		/**
		 * alter table
		 * 
		 * @param tablename
		 * @param partitions
		 */
		void alterTable(String tablename, int partitions) throws SQLException;

		/**
		 * list columns
		 * 
		 * @param table
		 * @return
		 * @throws SQLException
		 * @since 3.3
		 */
		List<JSON> listColumns(String table) throws SQLException;

		/**
		 * 获取优化器
		 * 
		 * @return
		 * @since 3.3
		 */
		Optimizer getOptimizer();

		/**
		 * 当前操作
		 * 
		 * @return
		 * @since 3.3
		 */
		List<JSON> listOp();

		/**
		 * run sql or script, custom for database
		 * 
		 * @param sql
		 * @return
		 * @since 3.3
		 */
		Object run(String sql) throws SQLException;

		/**
		 * get the size for table
		 * 
		 * @param table
		 * @return
		 * @since 3.3
		 */
		long size(String table);

		/**
		 * get the stats for table
		 * 
		 * @param table
		 * @return
		 * @since 3.3
		 */
		JSON stats(String table);

		/**
		 * get the database status
		 * 
		 * @return
		 * @since 3.3
		 */
		JSON status();

		/**
		 * set table as distributed table by key
		 * 
		 * @param table
		 * @return
		 * @since 3.3
		 */
		boolean distributed(String table, String key);

		/**
		 * 获取数据库时间
		 * 
		 * @return
		 * @since 3.3
		 */
		long getTime();

		/**
		 * 分组统计最小值
		 * 
		 * @param table
		 * @param q
		 * @param name
		 * @param group
		 * @return
		 * @since 3.3
		 */
		List<JSON> min(String table, W q, String name, String[] group);

		/**
		 * 分组统计最大值
		 * 
		 * @param table
		 * @param q
		 * @param name
		 * @param group
		 * @return
		 * @since 3.3
		 */
		List<JSON> max(String table, W q, String name, String[] group);

		/**
		 * 获取结果流
		 * 
		 * @param <T>
		 * @param table
		 * @param q
		 * @param offset
		 * @param t1
		 * @return
		 * @throws SQLException
		 * @since 3.4
		 */
		public <T extends Bean> Stream<T> stream(String table, W q, long offset, Class<T> t1) throws SQLException;

		/**
		 * 表是否存在
		 * 
		 * @param tablename - 表名
		 * @return True：存在
		 */
		boolean exists(String tablename);

		/**
		 * 数据库查询
		 * 
		 * @param <T>
		 * @param sql - SQL语句
		 * @param t
		 * @return
		 */
		public <T extends Bean> Beans<T> query(String sql, Class<T> t) throws SQLException;

	}

	/**
	 * 结果流
	 * 
	 * @author joe
	 *
	 * @param <T>
	 * @since 3.4
	 */
	public interface Stream<T> extends Iterator<T>, Closeable {

		long size();

	}

	public static DBHelper getPrimary() {
		return primary;
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
			write.add(t1.pastms(), "table=%s", table);
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
	 * @throws SQLException
	 */
	public static List<JSON> count(String table, W q, String[] group, int n) throws SQLException {

		TimeStamp t1 = TimeStamp.create();
		try {

//			if (monitor != null) {
//				monitor.query(table, q);
//			}

			return primary.count(table, q, group, n);
		} finally {
			read.add(t1.pastms(), "table=%s, q=%", table, q);
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
	 * @throws SQLException
	 */
	public static List<JSON> count(String table, String name, W q, String[] group, int n) throws SQLException {
		TimeStamp t1 = TimeStamp.create();
		try {
//			if (monitor != null) {
//				monitor.query(table, q);
//			}

			return primary.count(table, q, name, group, n);
		} finally {
			read.add(t1.pastms(), "table=%s, name=%s, q=%", table, name, q);
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
//
//			if (monitor != null) {
//				monitor.query(table, q);
//			}

			return primary.sum(table, q, name, group);
		} finally {
			read.add(t1.pastms(), "table=%s, name=%s, q=%", table, name, q);
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

//			if (monitor != null) {
//				monitor.query(table, q);
//			}

			return primary.aggregate(table, func, q, group);
		} finally {
			read.add(t1.pastms(), "table=%s, func=%s, q=%", table, Arrays.toString(func), q);
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

	public static List<JSON> listTables(String tablename, int n) {
		TimeStamp t1 = TimeStamp.create();
		try {
			return primary.listTables(tablename, n);
		} finally {
			read.add(t1.pastms(), null);
		}
	}

	public static List<JSON> listDB() {
		TimeStamp t1 = TimeStamp.create();
		try {
			return primary.listDB();
		} finally {
			read.add(t1.pastms(), null);
		}
	}

	public static List<JSON> listOp() {
		TimeStamp t1 = TimeStamp.create();
		try {
			return primary.listOp();
		} finally {
			read.add(t1.pastms(), null);
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
			read.add(t1.pastms(), "table=%s", table);
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
			read.add(t1.pastms(), "table=%s", table);
		}
	}

	/**
	 * init table
	 * 
	 * @param name
	 */
	public static void init(String name) {

		for (String key : new String[] { X.ID }) {
			LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
			m.put(key, 1);
			Helper.createIndex(name, m, true);
		}

		for (String key : new String[] { X.CREATED, "updated" }) {
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
			read.add(t1.pastms(), null);
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

	public static boolean distributed(String table, String key) {
		return primary.distributed(table, key);
	}

	public static void killOp(Object id) {
		TimeStamp t1 = TimeStamp.create();
		try {
			primary.killOp(id);
		} finally {
			read.add(t1.pastms(), null);
		}

	}

	public static class Stat {

		public static void read(String table, long costms) {

			String name = "read/" + table;
			Counter e = counter.get(name);
			if (e == null) {
				e = new Counter(table);
				counter.put(name, e);
			}
			e.add(costms, "table=%s", table);
		}

		public static void write(String table, long costms) {

			String name = "write/" + table;
			Counter e = counter.get(name);
			if (e == null) {
				e = new Counter(table);
				counter.put(name, e);
			}
			e.add(costms, "table=%s", table);
		}

		/**
		 * 分布式读性能
		 * 
		 * @param table
		 * @return
		 */
		public static Counter.Stat read(String table) {

			// 读取分布式环境中，所有的数据
			Counter.Stat r = new Counter.Stat();
			r.name = table;

			try {

				Task.call("stat/read", table, req -> {

					String from = req.from;

					if (log.isDebugEnabled()) {
						log.debug("list stat [" + table + "], from=" + from);
					}

					try {

						Counter.Stat s = req.get();
						r.merge(s);

					} catch (Exception e) {
						GLog.applog.error("task", "stat", "from=" + from + ", error=" + e.getMessage(), e);
					}
					return true;
				});
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}

			return r;

		}

		/**
		 * 分布式写性能
		 * 
		 * @param table
		 * @return
		 */
		public static Counter.Stat write(String table) {

			// 读取分布式环境中，所有的数据
			Counter.Stat r = new Counter.Stat();
			r.name = table;

			try {

				Task.call("stat/write", table, req -> {

					String from = req.from;

					if (log.isDebugEnabled()) {
						log.debug("list stat [" + table + "], from=" + from);
					}

					try {

						Counter.Stat s = req.get();
						r.merge(s);

					} catch (Exception e) {
						GLog.applog.error("task", "stat", "from=" + from + ", error=" + e.getMessage(), e);
					}

					return true;
				});
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}

			return r;

		}

		/**
		 * 单节点读性能
		 * 
		 * @param table
		 * @return
		 */
		public static Counter.Stat read0(String table) {

			String name = "read/" + table;

			Counter e = counter.get(name);
			if (e == null) {
				e = new Counter(table);
				counter.put(name, e);
			}
			Counter.Stat s = e.get();
			return s;
		}

		/**
		 * 单节点写性能
		 * 
		 * @param table
		 * @return
		 */
		public static Counter.Stat write0(String table) {

			String name = "write/" + table;

			Counter e = counter.get(name);
			if (e == null) {
				e = new Counter(table);
				counter.put(name, e);
			}
			Counter.Stat s = e.get();
			return s;
		}

		static Map<String, Counter> counter = new HashMap<String, Counter>();
	}

	public static <T extends Bean> Beans<T> query(String sql, Class<T> t) throws SQLException {
		return primary.query(sql, t);
	}

}
