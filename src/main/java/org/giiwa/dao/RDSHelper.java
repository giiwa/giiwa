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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Data;
import org.giiwa.conf.Config;
import org.giiwa.conf.Global;
import org.giiwa.dao.Helper.Cursor;
import org.giiwa.dao.Helper.DBHelper;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.pool.Pool;
import org.giiwa.pool.Pool.IPoolFactory;

import com.mongodb.BasicDBObject;

/**
 * The {@code RDSHelper} Class is base class for all class that database access,
 * it almost includes all methods that need for database <br>
 * all data access MUST be inherited from it
 * 
 */
public final class RDSHelper implements Helper.DBHelper {

	private static Log log = LogFactory.getLog(RDSHelper.class);

	/**
	 * indicated whether is debug model
	 */
	public static boolean DEBUG = true;
	public static final int MAXROWS = 10000;

	public static RDSHelper inst = new RDSHelper();

	private static Map<String, String> oracle = new HashMap<String, String>();
	static {
		oracle.put("uid", "\"uid\"");
		oracle.put("access", "\"access\"");
	}

	public void close() {
		if (_conn != null && this != Helper.primary) {
			_conn.destroy();
			_conn = null;
		}
	}

	private String _where(W q, Connection c) throws SQLException {
		if (q == null || c == null) {
			return null;
		}

		if (isOracle(c)) {
			return q.where(oracle);
		}

		return q.where();
	}

	private String _orderby(W q, Connection c) throws SQLException {
		if (q == null || c == null) {
			return null;
		}

		if (isOracle(c)) {
			return q.orderby(oracle);
		}

		return q.orderby();
	}

	/**
	 * Sets the parameter.
	 * 
	 * @param p the p
	 * @param i the i
	 * @param o the o
	 * @throws SQLException the SQL exception
	 */
	private static void setParameter(PreparedStatement p, int i, Object o) throws SQLException {
		if (o == null) {
			p.setObject(i, null);
		} else if (o instanceof Integer) {
			p.setInt(i, (Integer) o);
		} else if (o instanceof Date) {
			p.setDate(i, new java.sql.Date(((Date) o).getTime()));
		} else if (o instanceof java.sql.Date) {
			p.setDate(i, (java.sql.Date) o);
		} else if (o instanceof Long) {
			p.setLong(i, (Long) o);
		} else if (o instanceof Float) {
			p.setFloat(i, (Float) o);
		} else if (o instanceof Double) {
			p.setDouble(i, (Double) o);
		} else if (o instanceof Boolean) {
			p.setBoolean(i, (Boolean) o);
		} else if (o instanceof Timestamp) {
			p.setTimestamp(i, (Timestamp) o);
		} else {
			p.setString(i, o.toString());
		}
	}

	/**
	 * Delete the data in table.
	 *
	 * @param table the table name
	 * @param q     the query object
	 * @param db    the db
	 * @return int
	 */
	public int delete(String table, W q) {

		/**
		 * update it in database
		 */
		Connection c = null;
		PreparedStatement p = null;
		int n = -1;

		StringBuilder sql = new StringBuilder();

		try {
			c = getConnection();

			if (c == null)
				throw new SQLException("get connection failed!");

			/**
			 * create the sql statement
			 */
			if (this.isClickhouse(c)) {
				sql.append("alter table " + table + " delete ");
			} else {
				sql.append("delete from ").append(table);
			}

			String where = _where(q, c);
			Object[] args = q.args();

			if (!X.isEmpty(where)) {
				sql.append(" where ").append(where);
			}

//			sql.append(";commit;");
			p = c.prepareStatement(sql.toString());

			if (args != null) {
				int order = 1;

				for (int i = 0; i < args.length; i++) {
					Object o = args[i];

					setParameter(p, order++, o);

				}
			}

			n = p.executeUpdate();

			if (log.isDebugEnabled()) {
				log.debug("delete, table=" + table + ", q=" + q + ", deleted=" + n);
			}

		} catch (Exception e) {
			if (!tableNotExists(e) && !columnNotExists(e)) {
				log.error(sql.toString(), e);
			} else {
				// ignore the exception
			}

		} finally {
			close(p, c);
		}

		return n;
	}

	/**
	 * Gets the connection.
	 * 
	 * @return Connection
	 * @throws SQLException the SQL exception
	 */
	public Connection getConnection() throws Exception {
		try {
			if (_conn != null) {
				return _conn.get(X.AMINUTE);
			}

			BasicDataSource ds = getDataSource("default");
			if (ds != null) {
				Connection c = ds.getConnection();
				if (c != null) {
					c.setAutoCommit(true);
				}
				return c;
			}

			return null;

		} catch (SQLException e1) {
			log.error(e1.getMessage(), e1);

			throw e1;
		}
	}

	/**
	 * Close the objects, the object cloud be ResultSet, Statement,
	 * PreparedStatement, Connection <br>
	 * if the connection was required twice in same thread, then the reference "-1",
	 * if "=0", then close it.
	 *
	 * @param objs the objects of "ResultSet, Statement, Connection"
	 */
	public void close(Object... objs) {
		for (Object o : objs) {
			try {
				if (o == null)
					continue;

				if (o instanceof ResultSet) {
					((ResultSet) o).close();
				} else if (o instanceof Statement) {
					((Statement) o).close();
				} else if (o instanceof PreparedStatement) {
					((PreparedStatement) o).close();
				} else if (o instanceof Connection) {

					// local.remove();
					Connection c = (Connection) o;
					if (c == _conn) {
//						 forget this
						continue;
					}

					// Long[] dd = outdoor.get(c);
					// if (dd == null || dd[2] <= 0) {

					try {
						if (!c.getAutoCommit()) {
							c.commit();
						}
					} catch (Exception e1) {
					} finally {
						c.close();
					}
				}
			} catch (SQLException e) {
				if (log.isErrorEnabled())
					log.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * test exists.
	 *
	 * @param table the table name
	 * @param q     the query object
	 * @return boolean
	 */
	public boolean exists(String table, W q) {
		/**
		 * create the sql statement
		 */
		TimeStamp t = TimeStamp.create();

		/**
		 * search it in database
		 */
		Connection c = null;
		PreparedStatement p = null;
		ResultSet r = null;
		StringBuilder sql = new StringBuilder();

		try {
			c = getConnection();

			if (c == null)
				throw new SQLException("get connection failed!");

			sql.append("select 1 from ").append(table);

			String where = _where(q, c);
			Object[] args = q.args();

			if (!X.isEmpty(where)) {
				sql.append(" where ").append(where);
			}

			if (isOracle(c)) {
				if (X.isEmpty(where)) {
					sql.append(" where ");
				} else {
					sql.append(" and ");
				}
				sql.append(" rownum=1");
			} else {
				sql.append(" limit 1");
			}

			p = c.prepareStatement(sql.toString());

			int order = 1;
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					Object o = args[i];
					try {
						setParameter(p, order++, o);
					} catch (Exception e) {
						log.error("i=" + i + ", o=" + o, e);
					}
				}
			}

			r = p.executeQuery();
			return r.next();

		} catch (Exception e) {
			if (!tableNotExists(e) && !columnNotExists(e)) {
				log.error(sql.toString(), e);
			}
		} finally {
			close(r, p, c);

			if (log.isDebugEnabled())
				log.debug("cost:" + t.past() + ", sql=[" + q);
		}

		return false;
	}

	/**
	 * Update the data.
	 *
	 * @param table the table name
	 * @param q     the query object
	 * @param v     the values
	 * @param db    the db
	 * @return int
	 */
	public int updateTable(String table, W q, V v) {

		if (v == null || v.isEmpty())
			return 0;

		v.append(X.UPDATED, Global.now());

		/**
		 * update it in database
		 */
		Connection c = null;
		PreparedStatement p = null;
		StringBuilder sql = new StringBuilder();

		int updated = 0;
		try {

			c = getConnection();

			if (c == null)
				throw new SQLException("get connection failed!");

			// _check(c, table, v);

			/**
			 * create the sql statement
			 */
			if (this.isClickhouse(c)) {
				sql.append("alter table ").append(table).append(" update ");
			} else {
				sql.append("update ").append(table).append(" set ");
			}

			StringBuilder s = new StringBuilder();
			for (String name : v.names()) {
				if (s.length() > 0)
					s.append(",");
				if (isOracle(c) && oracle.containsKey(name)) {
					s.append(oracle.get(name));
				} else {
					s.append(name);
				}
				s.append("=?");
			}
			sql.append(s);

			String where = _where(q, c);
			Object[] args = q.args();

			if (!X.isEmpty(where)) {
				sql.append(" where ").append(where);
			} else if (this.isClickhouse(c)) {
				sql.append(" where all");
			}

			p = c.prepareStatement(sql.toString());

			int order = 1;
			for (String name : v.names()) {
				Object v1 = v.value(name);
				try {
					setParameter(p, order++, v1);
				} catch (Exception e) {
					log.error(name + "=" + v1, e);
				}
			}

			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					Object o = args[i];

					setParameter(p, order++, o);
				}
			}

			updated = p.executeUpdate();

		} catch (Exception e) {

			if (columnNotExists(e)) {
				// column missed
				V v1 = v.copy();
				q.scan(e1 -> {
					if (e1.value != null) {
						v1.append(e1.name, e1.value);
					}
				});
				if (_alertTable(table, v, c)) {
					return updateTable(table, q, v);
				}
			} else {
				log.error(sql.toString(), e);
			}
		} finally {
			close(p, c);
		}

		return updated;

	}

	/**
	 * Load the data, the data will be load(ResultSet r) method.
	 *
	 * @param fields the fields, * if null
	 * @param table  the table name
	 * @param q      the query object
	 * @param b      the Bean
	 * @param db     the db
	 * @return boolean
	 */
	public boolean load(String table, W q, Bean b, boolean trace) {
		/**
		 * create the sql statement
		 */
		TimeStamp t = TimeStamp.create();

		/**
		 * search it in database
		 */
		Connection c = null;
		PreparedStatement p = null;
		ResultSet r = null;
		StringBuilder sql = new StringBuilder();

		try {
			c = getConnection();

			if (c == null) {
				throw new SQLException("get connection failed!");
			}

			sql.append("select * from ").append(table);

			String where = _where(q, c);
			Object[] args = q.args();
			String orderby = _orderby(q, c);

			if (!X.isEmpty(where)) {
				sql.append(" where ").append(where);
			}

			if (isOracle(c)) {
				if (X.isEmpty(where)) {
					sql.append(" where ");
				} else {
					sql.append(" and ");
				}
				sql.append(" rownum=1");

				if (!X.isEmpty(orderby)) {
					sql.append(" ").append(orderby);
				}

			} else {
				if (!X.isEmpty(orderby)) {
					sql.append(" ").append(orderby);
				}
				sql.append(" limit 1");
			}

			p = c.prepareStatement(sql.toString());

			int order = 1;
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					Object o = args[i];

					setParameter(p, order++, o);
				}
			}

			r = p.executeQuery();
			if (r.next()) {
				b.load(r);

				return true;
			}

		} catch (Exception e) {

			if (tableNotExists(e)) {
				// create table
				_createTable(table, V.create().append("created", System.currentTimeMillis()).append("updated",
						System.currentTimeMillis()), c);

			} else if (!columnNotExists(e)) {
				log.error(sql, e);
			}

		} finally {
			close(r, p, c);

			if (trace) {
				log.debug("trace, load, cost = " + t.past() + ", sql=" + q + ", result=" + b);
			} else if (log.isDebugEnabled()) {
				log.debug("cost = " + t.past() + ", sql=" + q + ", result=" + b);
			}
		}

		return false;
	}

	/**
	 * load the list of beans, by the where.
	 *
	 * @param <T>    the generic Bean Class
	 * @param cols   the column name array
	 * @param q      the query object
	 * @param offset the offset
	 * @param limit  the limit
	 * @param t      the Bean Class
	 * @return List
	 * @throws SQLException
	 */
	public final <T extends Bean> Beans<T> load(W q, int offset, int limit, Class<T> t) throws SQLException {
		/**
		 * get the require annotation onGet
		 */
		Table mapping = (Table) t.getAnnotation(Table.class);
		if (mapping == null) {
			if (log.isErrorEnabled())
				log.error("mapping missed in [" + t + "] declaretion");
			return null;
		}

		return load(mapping.name(), q, offset, limit, t);
	}

	@Override
	public String toString() {
		return "RDSHelper [_conn=" + _conn + "]";
	}

	/**
	 * Load the list data from the RDBMS table.
	 *
	 * @param <T>    the generic type
	 * @param table  the table name
	 * @param fields the column name array
	 * @param q      the query object
	 * @param offset the offset
	 * @param limit  the limit
	 * @param clazz  the Bean Class
	 * @return List
	 */
	public <T extends Bean> Beans<T> load(String table, W q, int offset, int limit, Class<T> clazz)
			throws SQLException {
		/**
		 * create the sql statement
		 */
		TimeStamp t = TimeStamp.create();

		// log.debug("sql:" + sql.toString());

		/**
		 * search it in database
		 */
		Connection c = null;
		PreparedStatement p = null;
		ResultSet r = null;

		StringBuilder sql = new StringBuilder();

		try {

			c = getConnection();

			if (c == null) {
				throw new SQLException("get connection failed, _conn=" + _conn);
			}

			sql.append("select ");

			String where = _where(q, c);
			Object[] args = q.args();
			String orderby = _orderby(q, c);

			if (isOracle(c) || isDM(c)) {

				if (limit > 0) {
					// select * from (select rownum rn,a.* from table_name a where rownum < limit)
					// where rn >= offset;
					sql.append(" * from (select rownum rn,a.* from ").append(table).append(" a ");

					if (!X.isEmpty(where)) {
						sql.append(" where ");
						sql.append("(").append(where).append(") ");
					}

					if (offset < 0) {
						offset = 0;
					}

					if (limit > 0) {
						int n = limit + offset + 1;
						if (n > 0) {
							if (!X.isEmpty(where)) {
								sql.append("and rownum<").append(n);
							} else {
								sql.append("where rownum<").append(n);
							}
						}
					}

					if (!X.isEmpty(orderby)) {
						sql.append(" ").append(orderby);
					}

					// 注意， 这不能用 rownum, 因为外层也有自己的rownum
					sql.append(") where rn>").append(offset);

				} else {

					sql.append("*");

					sql.append(" from ").append(table);
					if (!X.isEmpty(where)) {
						sql.append(" where ").append(where);
					}

					if (!X.isEmpty(orderby)) {
						sql.append(" ").append(orderby);
					}
				}

			} else {

				sql.append("*");

				sql.append(" from ").append(table);
				if (!X.isEmpty(where)) {
					sql.append(" where ").append(where);
				}

				if (!X.isEmpty(orderby)) {
					sql.append(" ").append(orderby);
				}
				if (limit > 0) {
					sql.append(" limit ").append(limit);
				}
				if (offset > 0) {
					sql.append(" offset ").append(offset);
				}

			}

			if (log.isDebugEnabled()) {
				log.debug("sql=" + sql.toString());
			}

			p = c.prepareStatement(sql.toString());
			p.setQueryTimeout(60);

			int order = 1;
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					Object o = args[i];

					setParameter(p, order++, o);
				}
			}

			r = p.executeQuery();
			long rowid = offset;
			Beans<T> list = new Beans<T>();
			while (r.next()) {
				T b = clazz.getDeclaredConstructor().newInstance();
				b.load(r);
				b._rowid = rowid++;

				list.add(b);
			}

			return list;
		} catch (Exception e) {

			if (tableNotExists(e)) {

//				log.error(sql, e);

				// create table
				_createTable(table, V.create().append("created", System.currentTimeMillis()).append("updated",
						System.currentTimeMillis()), c);

			} else if (!columnNotExists(e)) {
				log.error(table + " - " + q, e);
				throw new SQLException(e);
			}

		} finally {
			close(r, p, c);

			if (log.isDebugEnabled())
				log.debug("cost:" + t.pastms() + "ms, sql=" + q);
		}

		return null;
	}

	/**
	 * advance load API.
	 *
	 * @param <T>    the base object
	 * @param select the select section, etc: "select a.* from tbluser a, tblrole b
	 *               where a.uid=b.uid"
	 * @param q      the additional query condition;
	 * @param offset the offset
	 * @param limit  the limit
	 * @param clazz  the Class Bean
	 * @return List the list of Bean
	 */
	public <T extends Bean> Beans<T> loadBy(String select, W q, int offset, int limit, Class<T> clazz) {
		/**
		 * create the sql statement
		 */
		TimeStamp t = TimeStamp.create();

		/**
		 * search it in database
		 */
		Connection c = null;
		PreparedStatement p = null;
		ResultSet r = null;

		try {
			c = getConnection();

			if (c == null) {
				throw new SQLException("get connection failed!");
			}

			StringBuilder sql = new StringBuilder();
			sql.append(select);
			String where = _where(q, c);
			Object[] args = q.args();
			String orderby = _orderby(q, c);

			if (!X.isEmpty(where)) {
				if (select.indexOf(" where ") < 0) {
					sql.append(" where ").append(where);
				} else {
					sql.append(" and (").append(where).append(")");
				}
			}

			if (isOracle(c)) {
				if (X.isEmpty(where)) {
					sql.append(" where ");
				} else {
					sql.append(" and ");
				}
				if (offset < 0) {
					offset = MAXROWS;
				}
				sql.append(" rownum>").append(offset);
				if (offset > 0) {
					int n = offset + limit + 1;
					if (n > 0) {
						sql.append(" and rownum<").append(n);
					}
				}
				if (!X.isEmpty(orderby)) {
					sql.append(" ").append(orderby);
				}
			} else {
				if (!X.isEmpty(orderby)) {
					sql.append(" ").append(orderby);
				}
				if (limit > 0) {
					sql.append(" limit ").append(limit);
				}
				if (offset > 0) {
					sql.append(" offset ").append(offset);
				}
			}

			p = c.prepareStatement(sql.toString());

			int order = 1;
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					Object o = args[i];

					setParameter(p, order++, o);
				}
			}

			r = p.executeQuery();
			Beans<T> list = new Beans<T>();
			while (r.next()) {
				T b = clazz.getDeclaredConstructor().newInstance();
				b.load(r);
				list.add(b);
			}

			return list;
		} catch (Exception e) {

			log.error(q.toString(), e);

		} finally {
			close(r, p, c);

			if (log.isDebugEnabled())
				log.debug("cost:" + t.past() + ", sql=" + q);
		}

		return Beans.create();
	}

	/**
	 * Insert values into the table.
	 *
	 * @param table the table name
	 * @param sets  the values
	 * @param db    the db
	 * @return int
	 */
	public int insertTable(String table, V sets) {

		if (sets == null || sets.isEmpty()) {
			log.warn("not data to insert, ignore");
			return 0;
		}

		long t1 = Global.now();
		sets.append(X.CREATED, t1).append(X.UPDATED, t1);
		sets.append("_node", Global.id());

		/**
		 * insert it in database
		 */
		Connection c = null;

		try {
			c = getConnection();

			if (c == null)
				throw new SQLException("get connection failed!");

			return insertTable(table, sets, c);

		} catch (Exception e) {
			if (log.isErrorEnabled()) {
				log.error(sets.toString(), e);
			}

		} finally {
			close(c);
		}
		return 0;

	}

	private int insertTable(String table, V sets, Connection c) {

		if (sets == null || sets.isEmpty()) {
			log.warn("not data to insert, ignore");
			return 0;
		}

		/**
		 * insert it in database
		 */
		PreparedStatement p = null;
		StringBuilder sql = new StringBuilder();

		try {

			if (c == null)
				throw new SQLException("get connection failed!");

			// _check(c, table, sets);

			/**
			 * create the sql statement
			 */
			sql.append("insert into ").append(table).append(" (");
			StringBuilder s = new StringBuilder();
			int total = 0;
			boolean isoracle = isOracle(c);
			for (String name : sets.names()) {
				if (s.length() > 0)
					s.append(",");
				if (isoracle && oracle.containsKey(name)) {
					s.append(oracle.get(name));
				} else {
					s.append(name);
				}
				total++;
			}
			sql.append(s).append(") values( ");

			for (int i = 0; i < total - 1; i++) {
				sql.append("?, ");
			}
			sql.append("?)");

			p = c.prepareStatement(sql.toString());

			int order = 1;
			for (String name : sets.names()) {
				Object v = sets.value(name);
				setParameter(p, order++, v);
			}

			return p.executeUpdate();

		} catch (Exception e) {

			// if the table not exists, create it
			if (columnNotExists(e)) {

				log.warn(sql.toString() + "\n" + sets.toString(), e);

				// column missed
				if (_alertTable(table, sets, c)) {
					return insertTable(table, sets, c);
				}
			} else if (tableNotExists(e)) {
				// table missed
				log.warn(sql.toString() + "\n" + sets.toString(), e);

				if (_createTable(table, sets, c)) {
					return insertTable(table, sets, c);
				}

			} else {
				log.error(sql.toString() + "\n" + sets.toString(), e);
			}

		} finally {
			close(p);
		}
		return 0;
	}

	private static boolean tableNotExists(Exception e) {
		return X.isCauseBy(e,
				".*(Table.*not found|Table.*doesn't exist|Invalid table|Table.*does not exist|无效的表|表.*不存在|关系.*不存在).*");
	}

	private static boolean columnNotExists(Exception e) {
		return X.isCauseBy(e,
				".*(Column.*not found|No such column|Missing columns|There is no column|Unknown column|Invalid column|无效的列|字段不存在|字段.*不存在).*");
	}

	private static String _name(String name, Connection c) {
		return name;
	}

	private boolean _alertTable(String table, V v, Connection c) {

		Statement stat = null;

		try {

			stat = c.createStatement();

			Map<String, String> cols = _columns(table, c);

			for (String name : v.names()) {
				if (!cols.containsKey(name.toLowerCase())) {

					StringBuilder sql = new StringBuilder("alter table ").append(table).append(" add ");

					if (this.isClickhouse(c)) {
						sql.append(" column ");
					}

					try {
						if (v.value(name) != null) {

							sql.append(_name(name, c)).append(" ").append(_type(v.value(name), c));

							stat.execute(sql.toString());
						}
					} catch (Exception e) {
						log.error(sql.toString(), e);
						v.remove(name);
					}
				}
			}

			return true;

		} catch (Exception e) {

			log.error(e.getMessage(), e);

		} finally {
			close(stat);
		}

		return false;
	}

	private Map<String, String> _columns(String table, Connection c) {
		ResultSet r = null;
		Statement stat = null;
		Map<String, String> l1 = new HashMap<String, String>();

		try {
			stat = c.createStatement();

			r = stat.executeQuery("select * from " + table + " where 2=1");

			ResultSetMetaData md = r.getMetaData();
			for (int i = 0; i < md.getColumnCount(); i++) {
				String name = md.getColumnName(i + 1);
				l1.put(name.toLowerCase(), md.getColumnTypeName(i + 1));
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			close(r, stat);
		}
		return l1;
	}

	private boolean _createTable(String table, V v, Connection c) {

		Statement stat = null;

		StringBuilder sql = new StringBuilder("create table ").append(table).append(" ( ");

		try {
			stat = c.createStatement();

			int i = 0;
			for (String name : v.names()) {
				if (i > 0) {
					sql.append(", ");
				}
				sql.append(_name(name, c)).append(" ").append(_type(v.value(name), c));

				i++;
			}
			sql.append(" ) ");

			if (this.isClickhouse(c)) {
				sql.append("engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192");
			}

			stat.execute(sql.toString());

			return true;
		} catch (Exception e) {
			log.error(sql.toString(), e);

		} finally {
			close(stat);
		}
		return false;
	}

	private String _type(Object v, Connection c) {
		if (v == null) {
			if (this.isClickhouse(c)) {
				return "Nullable(String)";
			}
			return "VARCHAR";
		} else if (v instanceof Long || v instanceof Integer) {
			return "bigint";
		} else if (v instanceof Float || v instanceof Double) {
			return "double precision";
		} else {
			if (v instanceof Collection || v.getClass().isArray()) {
				if (this.isPG(c)) {
					List<Object> l1 = X.asList(v, s -> s);
					if (l1 != null && !l1.isEmpty()) {
						Object o = l1.get(0);
						if (o instanceof Long || o instanceof Integer) {
							return "bigint[]";
						} else if (o instanceof Float || o instanceof Double) {
							return "float[]";
						} else {
							return "text[]";
						}
					}
				}
			}
			if (this.isClickhouse(c)) {
				return "Nullable(String)";
			}

			return "VARCHAR";
		}
	}

	/**
	 * get a string value from a col from the table.
	 * 
	 * @param table the table name
	 * @param col   the column name
	 * @param q     the query object
	 * @param db    the db name
	 * @return String
	 */
	public String getString(String table, String col, W q, String db) {

		/**
		 * search it in database
		 */
		Connection c = null;
		PreparedStatement p = null;
		ResultSet r = null;

		try {
			c = getConnection();

			if (c == null)
				throw new SQLException("get connection failed!");

			/**
			 * create the sql statement
			 */
			StringBuilder sql = new StringBuilder();

			sql.append("select ").append(_name(col, c)).append(" from ").append(table);

			String where = _where(q, c);
			Object[] args = q.args();

			if (!X.isEmpty(where)) {
				sql.append(" where ").append(where);
			}

			if (isOracle(c)) {
				if (X.isEmpty(where)) {
					sql.append(" where ");
				} else {
					sql.append(" and ");
				}
				sql.append(" rownum=1");
			} else {
				sql.append(" limit 1");
			}

			p = c.prepareStatement(sql.toString());

			int order = 1;
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					Object o = args[i];

					setParameter(p, order++, o);
				}
			}

			r = p.executeQuery();
			if (r.next()) {
				return r.getString(col);
			}

		} catch (Exception e) {

			log.error(q, e);

		} finally {
			close(r, p, c);
		}

		return null;
	}

	/**
	 * get one field.
	 * 
	 * @param <T>      the generic Bean Class
	 * @param col      the column name
	 * @param q        the query object
	 * @param position the offset
	 * @param t        the Bean class
	 * @return T
	 */
	public final <T> T getOne(String col, W q, int position, Class<? extends Bean> t) {
		/**
		 * get the require annotation onGet
		 */
		Table mapping = (Table) t.getAnnotation(Table.class);
		if (mapping == null) {
			if (log.isErrorEnabled())
				log.error("mapping missed in [" + t + "] declaretion");
			return null;
		}

		return getOne(mapping.name(), col, q, position);
	}

	/**
	 * getOne by the query.
	 * 
	 * @param <T>      the generic Bean Class
	 * @param table    the table name
	 * @param col      the column anme
	 * @param q        the query object
	 * @param position the offset
	 * @return T
	 */
	@SuppressWarnings("unchecked")
	public <T> T getOne(String table, String col, W q, int position) {

		/**
		 * search it in database
		 */
		Connection c = null;
		PreparedStatement p = null;
		ResultSet r = null;

		try {
			c = getConnection();

			if (c == null)
				throw new SQLException("get connection failed!");

			/**
			 * create the sql statement
			 */
			StringBuilder sql = new StringBuilder();
			sql.append("select ").append(col).append(" from ").append(table);
			String where = _where(q, c);
			Object[] args = q.args();
			String orderby = _orderby(q, c);

			if (!X.isEmpty(where)) {
				sql.append(" where ").append(where);
			}

			if (isOracle(c)) {
				if (X.isEmpty(where)) {
					sql.append(" where ");
				} else {
					sql.append(" and ");
				}
				sql.append(" rownum=").append(position);
				if (!X.isEmpty(orderby)) {
					sql.append(" ").append(orderby);
				}

			} else {
				if (!X.isEmpty(orderby)) {
					sql.append(" ").append(orderby);
				}

				sql.append(" limit 1");
				if (position > 0) {
					sql.append(" offset ").append(position);
				}
			}

			p = c.prepareStatement(sql.toString());

			int order = 1;
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					Object o = args[i];

					setParameter(p, order++, o);
				}
			}

			r = p.executeQuery();
			if (r.next()) {
				return (T) r.getObject(1);
			}

		} catch (Exception e) {

			log.error(q, e);

		} finally {
			close(r, p, c);
		}

		return null;
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
	 * test if confiured RDS
	 * 
	 * @return true: configured, false: no
	 */
	public boolean isConfigured() {
		return getDataSource("default") != null;
	}

	/**
	 * count the data.
	 *
	 * @param table the table name
	 * @param q     the query object
	 * @param db    the db
	 * @return the number of data
	 */
	public long count(String table, W q) {
		return count(table, q, "*");
	}

	/**
	 * get distinct data list.
	 *
	 * @param table the table name
	 * @param name  the column name
	 * @param q     the query object
	 * @param db    the db
	 * @return the list of Object
	 */
	public List<?> distinct(String table, String name, W q) {
		/**
		 * create the sql statement
		 */
		TimeStamp t = TimeStamp.create();

		// log.debug("sql:" + sql.toString());

		/**
		 * search it in database
		 */
		Connection c = null;
		PreparedStatement p = null;
		ResultSet r = null;

		try {

			c = getConnection();

			if (c == null)
				throw new SQLException("get connection failed!");

			StringBuilder sql = new StringBuilder();

			sql.append("select distinct(").append(_name(name, c)).append(") from ").append(table);
			String where = _where(q, c);
			Object[] args = q.args();

			if (!X.isEmpty(where)) {
				sql.append(" where ").append(where);
			}

			p = c.prepareStatement(sql.toString());

			int order = 1;
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					Object o = args[i];

					setParameter(p, order++, o);
				}
			}

			r = p.executeQuery();
			List<Object> list = new ArrayList<Object>();
			while (r.next()) {
				list.add(r.getObject(1));
			}

			if (log.isDebugEnabled())
				log.debug("load - cost=" + t.past() + ", collection=" + table + ", sql=" + sql + ", result=" + list);

			if (t.pastms() > 10000) {
				log.warn("load - cost=" + t.past() + ", collection=" + table + ", sql=" + sql + ", result=" + list);
			}

			return list;
		} catch (Exception e) {

			if (tableNotExists(e)) {
				// create table
				_createTable(table, V.create().append("created", System.currentTimeMillis()).append("updated",
						System.currentTimeMillis()), c);

			} else if (!columnNotExists(e)) {
				log.error(q, e);
			}

		} finally {
			close(r, p, c);
		}
		return null;
	}

	/**
	 * backup the data to file.
	 *
	 * @param filename the filename
	 * @param cc       the table
	 */
	public void backup(ZipOutputStream zip, String[] cc) {

		Connection c = null;
		ResultSet r1 = null;

		try {
			zip.putNextEntry(new ZipEntry("rds.db"));
			PrintStream out = new PrintStream(zip);

			c = getConnection();
			if (cc == null) {
				DatabaseMetaData m1 = c.getMetaData();
				r1 = m1.getTables(null, null, null, new String[] { "TABLE" });
				while (r1.next()) {
					_backup(out, c, r1.getString("TABLE_NAME"));
				}
			} else {
				for (String s : cc) {
					_backup(out, c, s);
				}
			}
			zip.closeEntry();

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			close(r1, c);
		}

	}

	private void _backup(PrintStream out, Connection c, String tablename) {

		if (log.isDebugEnabled())
			log.debug("backuping " + tablename);

		Statement stat = null;
		ResultSet r = null;
		try {
			stat = c.createStatement();
			r = stat.executeQuery("select * from " + tablename);

			int rows = 0;
			while (r.next()) {
				rows++;
				ResultSetMetaData m1 = r.getMetaData();

				JSON jo = new JSON();
				jo.put("_table", tablename);
				for (int i = 1; i <= m1.getColumnCount(); i++) {
					Object o = r.getObject(i);
					if (o != null) {
						jo.put(m1.getColumnName(i), o);
					}
				}

				out.println(Base64.getEncoder().encodeToString(jo.toString().getBytes()));
			}

			if (log.isDebugEnabled())
				log.debug("backup " + tablename + ", rows=" + rows);

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			close(r, stat, c);
		}
	}

	/**
	 * recover the data from the file.
	 *
	 * @param file the file
	 */
	public void recover(InputStream zip) {

		Connection c = null;
		ResultSet r1 = null;
		Statement stat = null;

		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(zip));

			c = getConnection();

			String line = in.readLine();
			while (line != null) {
				_recover(line, c);
				line = in.readLine();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			close(r1, stat, c);
		}
	}

	private void _recover(String json, Connection c) {
		try {
//			JSON jo = JSON.fromObject(json);
			JSON jo = JSON.fromObject(Base64.getDecoder().decode(json));
			V v = V.create().copy(jo);
			String tablename = jo.getString("_table");
			v.remove("_table");

			delete(tablename, W.create().and(X.ID, jo.get(X.ID)));
			insertTable(tablename, v, c);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	transient Boolean _oracle = null;
	transient Boolean _pg = null;

	private boolean isOracle(Connection c) {
		_checkDriver(c);
		return _oracle;
	}

	private boolean isPG(Connection c) {
		_checkDriver(c);
		return _pg;
	}

	private void _checkDriver(Connection c) {
		try {
			if (_oracle == null || _mysql == null) {
				String s = c.getMetaData().getDatabaseProductName();

				log.warn("driver=" + s);

				_oracle = s.indexOf("Oracle") > -1;
				_mysql = s.indexOf("MySQL") > -1;
				_mariadb = s.indexOf("Mariadb") > -1;
				_dm = s.indexOf("DM") > -1;
				_pg = s.indexOf("PostgreSQL") > -1;
				_clickhouse = s.indexOf("ClickHouse") > -1;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	transient Boolean _mysql = null;
	transient Boolean _mariadb = null;
	transient Boolean _dm = null;
	transient Boolean _clickhouse = null;

	@SuppressWarnings("unused")
	private boolean isMysql(Connection c) {
		_checkDriver(c);
		return _mysql;
	}

	private boolean isDM(Connection c) {
		_checkDriver(c);
		return _dm;
	}

	private boolean isClickhouse(Connection c) {
		_checkDriver(c);
		return _clickhouse;
	}

	/**
	 * inc.
	 *
	 * @param table the table
	 * @param q     the q
	 * @param name  the name
	 * @param n     the n
	 * @param db    the db
	 * @return the int
	 */
	public int inc(String table, W q, String name, int n, V sets) {

		Connection c = null;

		try {
			c = getConnection();
			if (c == null)
				throw new SQLException("get connection failed!");

			int n1 = _inc(table, q, name, n, c);
			if (sets != null && !sets.isEmpty()) {
				this.updateTable(table, q, sets);
			}
			return n1;
		} catch (Exception e) {

			if (tableNotExists(e)) {
				// create table
				_createTable(table, V.create().append(name, n).append("created", System.currentTimeMillis())
						.append("updated", System.currentTimeMillis()), c);

			} else if (!columnNotExists(e)) {

				log.error(e.getMessage(), e);
			}
		} finally {
			close(c);
		}
		return -1;

	}

	private int _inc(String table, W q, String name, int n, Connection c) {
		/**
		 * update it in database
		 */
		PreparedStatement p = null;
		ResultSet r = null;
		StringBuilder sql = new StringBuilder();

		try {

			/**
			 * create the sql statement
			 */
			sql.append("update ").append(table).append(" set ").append(name).append("=").append(name).append("+?");

//			boolean isoracle = isOracle(c);

			String where = _where(q, c);
			Object[] args = q.args();

			if (!X.isEmpty(where)) {
				sql.append(" where ").append(where);
			}

			p = c.prepareStatement(sql.toString());

			int order = 1;
			setParameter(p, order++, n);

			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					Object o = args[i];

					setParameter(p, order++, o);
				}
			}

			return p.executeUpdate();

		} catch (Exception e) {

			log.error(sql.toString(), e);

			if (columnNotExists(e)) {

				// column missed
				if (_alertTable(table, V.create().append(name, n), c)) {
					return _inc(table, q, name, n, c);
				}

			}

		} finally {
			close(p, r);
		}

		return 0;
	}

	public BasicDataSource getDB(String name) {
		BasicDataSource external = dss.get(name);
		if (external == null && conf != null) {
			String url = conf.getString("db[" + name + "].url", null);
			String username = conf.getString("db[" + name + "].user", null);
			String passwd = conf.getString("db[" + name + "].passwd", null);

			if (!X.isEmpty(url)) {

				String D = _getDiver(url);

				int N = conf.getInt("db[" + name + "].conns", MAX_ACTIVE_NUMBER);

				external = _get(D, username, passwd, url, N);
				log.info(name + ".driver=" + D);

				dss.put(name, external);
			}
		}

		return external;
	}

	public void createIndex(String table, LinkedHashMap<String, Object> ss, boolean unique) {

		Connection c = null;
		Statement stat = null;
		StringBuilder sb = new StringBuilder();
		try {
			c = getConnection();
			stat = c.createStatement();
			String indexname = table + "_index_" + UID.id(ss.toString());

			if (this.isClickhouse(c)) {
				if (ss.size() <= 1) {
					// not need
					return;
				}
				sb.append("alter table " + table);
				sb.append(" add index " + indexname);
				sb.append("(");

			} else {
				sb.append("create ");
				if (unique) {
					sb.append(" unique ");
				}
				sb.append(" index ").append(indexname);
				sb.append(" on ").append(table).append("(");
			}

			StringBuilder sb1 = new StringBuilder();
			int n = 0;
			for (String s : ss.keySet()) {
				if (sb1.length() > 0)
					sb1.append(",");
				sb1.append(s);
				if (!this.isClickhouse(c)) {
					if (X.toInt(ss.get(s)) < 1) {
						sb1.append(" ").append("desc");
					}
				}
				n++;
				if (n > 4) {
					break;
				}
			}
			sb.append(sb1.toString()).append(")");

			if (this.isClickhouse(c)) {
				sb.append("	type minmax GRANULARITY 3");
			}

			stat.executeUpdate(sb.toString());

			if (log.isDebugEnabled()) {
				log.debug("createIndex, sql=" + sb.toString());
			}

		} catch (Exception e) {
			if (e.getMessage().indexOf("already exists") == -1) {
				log.error(sb.toString(), e);
			}

		} finally {
			close(stat, c);
		}

	}

	@Override
	public List<Map<String, Object>> getIndexes(String table) {

		List<Map<String, Object>> l1 = new ArrayList<Map<String, Object>>();

		Connection c = null;
		ResultSet r = null;

		try {

			c = getConnection();

			DatabaseMetaData d1 = c.getMetaData();
//			r = d1.getIndexInfo(null, null, null, false, true);
//			while (r.next()) {
//				Bean b = new Bean();
//				b.load(r);
//				String name = r.getString("index_name").toLowerCase();
//				log.debug("index=" + name + ", column=" + r.getString("column_name").toLowerCase() + ", r=" + b.json());
//			}
//			r.close();

			if (this.isOracle(c) || this.isDM(c)) {
				r = d1.getIndexInfo(null, null, null, false, true);
			} else {
				r = d1.getIndexInfo(null, null, table, false, true);
			}

			Map<String, Map<String, Object>> indexes = new TreeMap<String, Map<String, Object>>();
			while (r.next()) {

				String table1 = r.getString("table_name");
				if (!X.isSame(table, table1)) {
					continue;
				}

				String name = r.getString("index_name").toLowerCase();
				Map<String, Object> m = indexes.get(name);
				if (m == null) {
					m = new LinkedHashMap<String, Object>();
					indexes.put(name, m);
				}

				// ResultSetMetaData m1 = r.getMetaData();
				// for (int i = 1; i <= m1.getColumnCount(); i++) {
				// }
				m.put(r.getString("column_name").toLowerCase(), X.isSame(r.getString("asc_or_desc"), "A") ? 1 : -1);
			}

			for (String name : indexes.keySet()) {
				Map<String, Object> m1 = new LinkedHashMap<String, Object>();
				m1.put("name", name);
				m1.put("key", indexes.get(name));
				l1.add(m1);
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);

		} finally {
			close(r, c);
		}

		return l1;
	}

	@Override
	public void dropIndex(String table, String name) {
		Connection c = null;
		Statement stat = null;
		try {

			c = getConnection();
			stat = c.createStatement();
			stat.executeUpdate("drop index " + name);

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			close(stat, c);
		}
	}

	@Override
	public int insertTable(String table, List<V> values) {

		if (values == null || values.isEmpty())
			return 0;

		/**
		 * insert it in database
		 */
		PreparedStatement p = null;
		Connection c = null;
		try {
			if (X.isEmpty(values))
				return 0;

			c = getConnection();

			// _check(c, table, values.get(0));

			/**
			 * create the sql statement
			 */
			StringBuilder sql = new StringBuilder();
			sql.append("insert into ").append(table).append(" (");
			StringBuilder s = new StringBuilder();
			int total = 0;
			boolean isoracle = isOracle(c);

			for (String name : values.get(0).names()) {
				if (s.length() > 0)
					s.append(",");
				if (isoracle && oracle.containsKey(name)) {
					s.append(oracle.get(name));
				} else {
					s.append(name);
				}
				total++;
			}
			sql.append(s).append(") values( ");

			for (int i = 0; i < total - 1; i++) {
				sql.append("?, ");
			}
			sql.append("?)");

			p = c.prepareStatement(sql.toString());

			int order = 1;
			for (V v : values) {

				if (v == null || v.isEmpty())
					return 0;

				long t1 = Global.now();
				v.append(X.CREATED, t1).append(X.UPDATED, t1);
				v.append("_node", Global.id());

				for (String name : v.names()) {
					Object v1 = v.value(name);
					setParameter(p, order++, v1);
				}

				p.addBatch();
			}

			return X.sum(p.executeBatch());

		} catch (Exception e) {

			log.error(values.toString(), e);

		} finally {
			close(p, c);
		}
		return 0;
	}

	private Pool<Connection> _conn = null;

	public static DBHelper create(String url, String user, String passwd, int conns, int timeout, String locale) {

		Pool<Connection> pool = Pool.create(conns, conns, new IPoolFactory<Connection>() {

			@Override
			public boolean check(Connection c) {
				try {
					return !c.isClosed();
				} catch (SQLException e) {
					log.error(e.getMessage(), e);
				}
				return false;
			}

			@Override
			public Connection create() {
				try {
					if (!X.isEmpty(url)) {
						return getConnection(url, user, passwd, locale);
					}
				} catch (SQLException e) {
					log.error("url=" + url + ", locale=" + locale, e);
				}
				return null;
			}

			@Override
			public void destroy(Connection c) {
				try {
					c.close();
				} catch (SQLException e) {
					log.error(e.getMessage(), e);
				}
			}

		});

		return create(pool);

	}

	public static DBHelper create(Pool<Connection> c) {

		RDSHelper d = new RDSHelper();
		d._conn = c;

		if (log.isDebugEnabled()) {
			log.debug("create DBHelper from pool, c=" + c);
		}

		return d;
	}

	@Override
	public List<JSON> listTables() {

		List<JSON> list = new ArrayList<JSON>();

		Connection c = null;
		ResultSet r = null;
		try {
			c = getConnection();

			DatabaseMetaData md = c.getMetaData();
			r = md.getTables(null, null, null, null);

			while (r.next()) {
				Bean b = new Bean();
				b.load(r);
				list.add(b.json());
			}

			Collections.sort(list, new Comparator<JSON>() {

				@Override
				public int compare(JSON o1, JSON o2) {
					return o1.getString("table_name").compareToIgnoreCase(o2.getString("table_name"));
				}

			});

		} catch (Exception e) {
			log.error(e.getMessage(), e);

		} finally {
			close(r, c);
		}
		return list;
	}

	@Override
	public <T extends Bean> Cursor<T> cursor(String table, W q, long offset, Class<T> t) {
		/**
		 * search it in database
		 */
		Connection c = null;
		PreparedStatement p = null;
		ResultSet r = null;

		StringBuilder sql = new StringBuilder();

		try {
			c = getConnection();

			if (c == null)
				throw new SQLException("get connection failed!");

			String where = _where(q, c);
			Object[] args = q.args();
			String orderby = _orderby(q, c);

			if (this.isOracle(c) || this.isDM(c)) {
				// select * from (select rownum rn,a.* from table_name a where rownum < limit)
				// where rn >= offset;
				sql.append("select * from (select rownum rn,a.* from ").append(table).append(" a ");

				if (!X.isEmpty(where)) {
					sql.append(" where ");
					sql.append("(").append(where).append(") ");
				}

				if (!X.isEmpty(orderby)) {
					sql.append(" ").append(orderby);
				}
				sql.append(") where rn>").append(offset);

			} else {
				sql.append("select * from ").append(table);

				if (!X.isEmpty(where)) {
					sql.append(" where ").append(where);
				}

				if (!X.isEmpty(orderby)) {
					sql.append(" ").append(orderby);
				}

				if (offset > 0) {
					sql.append(" offset ").append(offset);
				}
			}

			if (log.isDebugEnabled())
				log.debug("sql=" + sql.toString());

			p = c.prepareStatement(sql.toString());

			int order = 1;
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					Object o = args[i];

					setParameter(p, order++, o);
				}
			}

			r = p.executeQuery();

			return _cursor(t, r, p, c); // close the connection in cursor
		} catch (Exception e) {

			if (tableNotExists(e)) {
				// create table
				_createTable(table, V.create().append("created", System.currentTimeMillis()).append("updated",
						System.currentTimeMillis()), c);

			} else if (!columnNotExists(e)) {
				log.error(sql.toString(), e);
			}

			close(r, p, c);
		}

		return null;
	}

	private <T extends Bean> Cursor<T> _cursor(final Class<T> t, final ResultSet r, final PreparedStatement p,
			final Connection c) {

		return new Cursor<T>() {

			@Override
			public boolean hasNext() {
				try {
					return r.next();
				} catch (SQLException e) {
					log.error(e.getMessage(), e);
				}
				return false;
			}

			@Override
			public T next() {
				try {
					T t1 = t.getDeclaredConstructor().newInstance();
					t1.load(r);
					return t1;
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
				return null;
			}

			@Override
			public void close() {
				RDSHelper.this.close(r, p, c);
			}

		};
	}

	@Override
	public List<JSON> getMetaData(String tablename) {
		Connection c = null;
		Statement stat = null;
		ResultSet r = null;
		try {
			c = getConnection();

			stat = c.createStatement();
			r = stat.executeQuery("select * from " + tablename + " where 2=1");
			ResultSetMetaData r1 = r.getMetaData();
			List<JSON> list = new ArrayList<JSON>();
			for (int i = 1; i <= r1.getColumnCount(); i++) {
				JSON jo = JSON.create();
				jo.put("name", r1.getColumnName(i));
				jo.put("type", r1.getColumnTypeName(i));
				jo.put("size", r1.getColumnDisplaySize(i));
				list.add(jo);
			}
			return list;
		} catch (Exception e) {
			log.error(tablename, e);

		} finally {
			close(r, stat, c);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T sum(String table, W q, String name) {

		/**
		 * create the sql statement
		 */
		TimeStamp t = TimeStamp.create();

		// log.debug("sql:" + sql.toString());

		/**
		 * search it in database
		 */
		Connection c = null;
		PreparedStatement p = null;
		ResultSet r = null;
		Object n = 0;
		try {

			c = getConnection();

			if (c == null) {
				throw new SQLException("get connection failed!");
			} else {
				StringBuilder sum = new StringBuilder();
				sum.append("select sum(" + name + ") t from ").append(table);
				String where = _where(q, c);
				Object[] args = q.args();

				if (!X.isEmpty(where)) {
					sum.append(" where ").append(where);
				}

				p = c.prepareStatement(sum.toString());

				int order = 1;
				if (args != null) {
					for (int i = 0; i < args.length; i++) {
						Object o = args[i];

						setParameter(p, order++, o);
					}
				}

				r = p.executeQuery();
				if (r.next()) {
					n = r.getObject("t");
				}
			}
		} catch (Exception e) {

			if (!tableNotExists(e) && !columnNotExists(e)) {
				log.error(q, e);
			}

		} finally {
			close(r, p, c);

			if (log.isDebugEnabled())
				log.debug("sum, cost:" + t.past() + ", sql=" + q + ", n=" + n);
		}

		return (T) n;
	}

	/**
	 * 
	 * 
	 * @param url
	 * @param username
	 * @param passwd
	 * @param locale
	 * @return
	 * @throws SQLException
	 */
	public static Connection getConnection(String url, String username, String passwd, String locale)
			throws SQLException {

		String D = _getDiver(url);

		if (log.isDebugEnabled()) {
			log.debug("driver=" + D + ", url=" + url + ", user=" + username + ", password=" + passwd);
		}

		if (!X.isEmpty(D)) {
			Locale oldlocale = Locale.getDefault();
			try {
				if (!X.isEmpty(locale)) {
					if (X.isSame(locale, "en")) {
						Locale.setDefault(Locale.US);
					} else if (X.isSame(locale, "zh")) {
						Locale.setDefault(Locale.CHINA);
					}
					// Locale.setDefault(new Locale(locale));
				}

				synchronized (Class.class) {
					Class.forName(D);
				}

				DriverManager.setLoginTimeout(10);
				Connection conn = DriverManager.getConnection(url, username, passwd);

				if (log.isDebugEnabled())
					log.debug("got connection for [" + url + ", locale=" + Locale.getDefault() + "]");

				return conn;
			} catch (Exception e) {
				throw new SQLException(e);
			} finally {
				Locale.setDefault(oldlocale);
			}
		} else {
			throw new SQLException("unknown URL");
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T avg(String table, W q, String name) {

		/**
		 * create the sql statement
		 */
		TimeStamp t = TimeStamp.create();

		// log.debug("sql:" + sql.toString());

		/**
		 * search it in database
		 */
		Connection c = null;
		PreparedStatement p = null;
		ResultSet r = null;
		Object n = 0;
		try {

			c = getConnection();

			if (c == null) {
				throw new SQLException("get connection failed!");
			} else {
				StringBuilder sum = new StringBuilder();
				sum.append("select avg(" + name + ") t from ").append(table);
				String where = _where(q, c);
				Object[] args = q.args();

				if (!X.isEmpty(where)) {
					sum.append(" where ").append(where);
				}

				p = c.prepareStatement(sum.toString());

				int order = 1;
				if (args != null) {
					for (int i = 0; i < args.length; i++) {
						Object o = args[i];

						setParameter(p, order++, o);
					}
				}

				r = p.executeQuery();
				if (r.next()) {
					n = r.getObject("t");
				}
			}
		} catch (Exception e) {
			if (tableNotExists(e)) {
				// create table
				_createTable(table, V.create().append("created", System.currentTimeMillis()).append("updated",
						System.currentTimeMillis()), c);

			} else if (!columnNotExists(e)) {
				log.error(q, e);
			}

		} finally {
			close(r, p, c);

			if (log.isDebugEnabled())
				log.debug("avg, cost:" + t.past() + ", sql=" + q + ", n=" + n);
		}

		return (T) n;
	}

	@Override
	public void repair() {
		// not support

	}

	@Override
	public void drop(String table) {
		Connection c = null;
		Statement stat = null;
		try {

			c = getConnection();
			stat = c.createStatement();
			stat.executeUpdate("drop table " + table);

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			close(stat, c);
		}
	}

	@Override
	public List<JSON> count(String table, W q, String[] group, int n) {
		return count(table, q, "*", group, n);
	}

	@Override
	public List<JSON> sum(String table, W q, String name, String[] group) {
		/**
		 * create the sql statement
		 */
		TimeStamp t = TimeStamp.create();

		// log.debug("sql:" + sql.toString());

		/**
		 * search it in database
		 */
		Connection c = null;
		PreparedStatement p = null;
		ResultSet r = null;
		long n = 0;
		try {

			c = getConnection();

			if (c == null) {
				throw new SQLException("get connection failed!");
			} else {
				StringBuilder sum = new StringBuilder();
				sum.append("select");
				for (int i = 0; i < group.length; i++) {
					sum.append(group[i]);
				}
				sum.append(",sum(" + name + ") t from ").append(table);
				String where = _where(q, c);
				Object[] args = q.args();

				if (!X.isEmpty(where)) {
					sum.append(" where ").append(where);
				}

				sum.append(" groug by ");
				for (int i = 0; i < group.length; i++) {
					sum.append(group[i]).append(",");
				}

				BasicDBObject sort = q.order();
				if (sort != null && !sort.isEmpty()) {
					sum.append("order by ");
					int i = 0;
					for (String s : sort.keySet()) {
						if (i > 0)
							sum.append(",");

						if (X.isSame(s, "sum")) {
							sum.append("t");
						} else {
							sum.append(s.replaceAll("_id.", X.EMPTY));
						}

						if (sort.getInt(s) == -1) {
							sum.append(" desc");
						}
					}
				}

				p = c.prepareStatement(sum.toString());

				int order = 1;
				if (args != null) {
					for (int i = 0; i < args.length; i++) {
						Object o = args[i];

						setParameter(p, order++, o);
					}
				}

				r = p.executeQuery();
				List<JSON> l1 = JSON.createList();
				while (r.next()) {

					JSON j1 = JSON.create();
					for (String s : group) {
						j1.append(s, r.getObject(s));
					}

					JSON j = JSON.create();
					j.append("_id", j1).append("sum", r.getLong("t"));

					l1.add(j);
				}
				return l1;
			}
		} catch (Exception e) {
			log.error(q, e);

		} finally {
			close(r, p, c);

			if (log.isDebugEnabled())
				log.debug("cost:" + t.past() + ", sql=" + q + ", n=" + n);
		}

		return null;
	}

	@Override
	public List<JSON> avg(String table, W q, String name, String[] group) {
		/**
		 * create the sql statement
		 */
		TimeStamp t = TimeStamp.create();

		// log.debug("sql:" + sql.toString());

		/**
		 * search it in database
		 */
		Connection c = null;
		PreparedStatement p = null;
		ResultSet r = null;
		long n = 0;
		try {

			c = getConnection();

			if (c == null) {
				throw new SQLException("get connection failed!");
			} else {
				StringBuilder sum = new StringBuilder();
				sum.append("select");
				for (int i = 0; i < group.length; i++) {
					sum.append(group[i]);
				}
				sum.append(",avg(").append(name).append(") t from ").append(table);
				String where = _where(q, c);
				Object[] args = q.args();

				if (!X.isEmpty(where)) {
					sum.append(" where ").append(where);
				}

				sum.append(" groug by ");
				for (int i = 0; i < group.length; i++) {
					sum.append(group[i]).append(",");
				}

				BasicDBObject sort = q.order();
				if (sort != null && !sort.isEmpty()) {
					sum.append("order by ");
					int i = 0;
					for (String s : sort.keySet()) {
						if (i > 0)
							sum.append(",");

						if (X.isSame(s, "avg")) {
							sum.append("t");
						} else {
							sum.append(s.replaceAll("_id.", X.EMPTY));
						}

						if (sort.getInt(s) == -1) {
							sum.append(" desc");
						}
					}
				}

				p = c.prepareStatement(sum.toString());

				int order = 1;
				if (args != null) {
					for (int i = 0; i < args.length; i++) {
						Object o = args[i];

						setParameter(p, order++, o);
					}
				}

				r = p.executeQuery();
				List<JSON> l1 = JSON.createList();
				while (r.next()) {

					JSON j1 = JSON.create();
					for (String s : group) {
						j1.append(s, r.getObject(s));
					}

					JSON j = JSON.create();
					j.append("_id", j1).append("avg", r.getLong("t"));

					l1.add(j);
				}
				return l1;
			}
		} catch (Exception e) {
			log.error(q, e);

		} finally {
			close(r, p, c);

			if (log.isDebugEnabled())
				log.debug("cost:" + t.past() + ", sql=" + q + ", n=" + n);
		}

		return null;
	}

	@Override
	public List<JSON> aggregate(String table, String[] func, W q, String[] group) {
		/**
		 * create the sql statement
		 */
		TimeStamp t = TimeStamp.create();

		// log.debug("sql:" + sql.toString());

		/**
		 * search it in database
		 */
		Connection c = null;
		PreparedStatement p = null;
		ResultSet r = null;
		long n = 0;
		try {

			c = getConnection();

			if (c == null) {
				throw new SQLException("get connection failed!");
			} else {
				StringBuilder sum = new StringBuilder();
				sum.append("select");
				for (int i = 0; i < group.length; i++) {
					sum.append(group[i]);
				}
				sum.append(",avg(*) t from ").append(table);
				String where = _where(q, c);
				Object[] args = q.args();

				if (!X.isEmpty(where)) {
					sum.append(" where ").append(where);
				}

				sum.append(" groug by ");
				for (int i = 0; i < group.length; i++) {
					sum.append(group[i]).append(",");
				}

				BasicDBObject sort = q.order();
				if (sort != null && !sort.isEmpty()) {
					sum.append("order by ");
					int i = 0;
					for (String s : sort.keySet()) {
						if (i > 0)
							sum.append(",");

						if (X.isSame(s, "avg")) {
							sum.append("t");
						} else {
							sum.append(s.replaceAll("_id.", X.EMPTY));
						}

						if (sort.getInt(s) == -1) {
							sum.append(" desc");
						}
					}
				}

				p = c.prepareStatement(sum.toString());

				int order = 1;
				if (args != null) {
					for (int i = 0; i < args.length; i++) {
						Object o = args[i];

						setParameter(p, order++, o);
					}
				}

				r = p.executeQuery();
				List<JSON> l1 = JSON.createList();
				while (r.next()) {

					JSON j1 = JSON.create();
					for (String s : group) {
						j1.append(s, r.getObject(s));
					}

					JSON j = JSON.create();
					j.append("_id", j1).append("avg", r.getLong("t"));

					l1.add(j);
				}
				return l1;
			}
		} catch (Exception e) {
			log.error(q, e);
		} finally {
			close(r, p, c);

			if (log.isDebugEnabled())
				log.debug("cost:" + t.past() + ", sql=" + q + ", n=" + n);
		}

		return null;
	}

	@Override
	public <T> T std_deviation(String table, W q, String name) {
		return null;
	}

	@Override
	public <T> T median(String table, W q, String name) {
		return null;
	}

	@Override
	public List<JSON> listDB() {
		return null;
	}

	@Override
	public List<JSON> listOp() {
		return null;
	}

	@Override
	public long size(String table) {
		return 0;
	}

	@Override
	public long count(String table, W q, String name) {

		/**
		 * create the sql statement
		 */
		TimeStamp t = TimeStamp.create();

		// log.debug("sql:" + sql.toString());

		/**
		 * search it in database
		 */
		Connection c = null;
		PreparedStatement p = null;
		ResultSet r = null;

		StringBuilder sum = new StringBuilder();

		long n = 0;
		try {

			c = getConnection();

			if (c == null) {
				throw new SQLException("get connection failed!");
			} else {
				sum.append("select count(" + name + ") t from ").append(table);
				String where = _where(q, c);
				Object[] args = q.args();

				if (!X.isEmpty(where)) {
					sum.append(" where ").append(where);
				}

				p = c.prepareStatement(sum.toString());

				int order = 1;
				if (args != null) {
					for (int i = 0; i < args.length; i++) {
						Object o = args[i];

						setParameter(p, order++, o);
					}
				}

				r = p.executeQuery();
				if (r.next()) {
					n = r.getInt("t");
				}
			}
		} catch (Exception e) {

			if (tableNotExists(e)) {
				// create table
				_createTable(table, V.create().append("created", System.currentTimeMillis()).append("updated",
						System.currentTimeMillis()), c);

			} else if (!columnNotExists(e)) {
				log.error(sum.toString(), e);
			}

		} finally {
			close(r, p, c);

			if (log.isDebugEnabled())
				log.debug("cost:" + t.past() + ", sql=" + q + ", n=" + n);
		}

		return n;
	}

	@Override
	public List<JSON> count(String table, W q, String name, String[] group, int n1) {

		/**
		 * create the sql statement
		 */
		TimeStamp t = TimeStamp.create();

		// log.debug("sql:" + sql.toString());

		/**
		 * search it in database
		 */
		Connection c = null;
		PreparedStatement p = null;
		ResultSet r = null;
		long n = 0;
		try {

			c = getConnection();

			if (c == null) {
				throw new SQLException("get connection failed!");
			} else {
				StringBuilder sum = new StringBuilder();
				sum.append("select");
				for (int i = 0; i < group.length; i++) {
					sum.append(group[i]);
				}
				sum.append(",count(" + name + ") t from ").append(table);
				String where = _where(q, c);
				Object[] args = q.args();

				if (!X.isEmpty(where)) {
					sum.append(" where ").append(where);
				}

				sum.append(" groug by ");
				for (int i = 0; i < group.length; i++) {
					sum.append(group[i]).append(",");
				}

				BasicDBObject sort = q.order();
				if (sort != null && !sort.isEmpty()) {
					sum.append("order by ");
					int i = 0;
					for (String s : sort.keySet()) {
						if (i > 0)
							sum.append(",");

						if (X.isSame(s, "count")) {
							sum.append("t");
						} else {
							sum.append(s.replaceAll("_id.", X.EMPTY));
						}

						if (sort.getInt(s) == -1) {
							sum.append(" desc");
						}
					}
				}

				p = c.prepareStatement(sum.toString());

				int order = 1;
				if (args != null) {
					for (int i = 0; i < args.length; i++) {
						Object o = args[i];

						setParameter(p, order++, o);
					}
				}

				r = p.executeQuery();
				List<JSON> l1 = JSON.createList();
				while (r.next()) {

					JSON j1 = JSON.create();
					for (String s : group) {
						j1.append(s, r.getObject(s));
					}

					JSON j = JSON.create();
					j.append("_id", j1).append("count", r.getLong("t"));

					l1.add(j);

					if (l1.size() >= n1)
						break;

				}
				return l1;
			}
		} catch (Exception e) {
			if (tableNotExists(e)) {
				// create table
				_createTable(table, V.create().append("created", System.currentTimeMillis()).append("updated",
						System.currentTimeMillis()), c);

			} else if (!columnNotExists(e)) {
				log.error(q, e);
			}

		} finally {
			close(r, p, c);

			if (log.isDebugEnabled())
				log.debug("cost:" + t.past() + ", sql=" + q + ", n=" + n);
		}

		return null;
	}

	@Override
	public void repair(String table) {

	}

	@Override
	public JSON stats(String table) {
		return null;
	}

	@Override
	public JSON status() {
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T max(String table, W q, String name) {

		Data e = this.load(table, q.sort(name, -1), Data.class, false);
		if (e != null) {
			return (T) e.get(name);
		}
		return null;

	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T min(String table, W q, String name) {

		Data e = this.load(table, q.sort(name), Data.class, false);
		if (e != null) {
			return (T) e.get(name);
		}
		return null;

	}

	private transient Optimizer _optimizer;

	@Override
	public Optimizer getOptimizer() {
		if (_optimizer == null) {
			_optimizer = new Optimizer(this);
		}
		return _optimizer;
	}

	@Override
	public <T extends Bean> T load(String table, W q, Class<T> clazz) {
		return load(table, q, clazz, false);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Bean> T load(String table, W q, Class<T> clazz, boolean trace) {
		try {
			Bean b = clazz.getDeclaredConstructor().newInstance();
			if (load(table, q, b, trace)) {
				return (T) b;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public boolean distributed(String table, String key) {

		/**
		 * search it in database
		 */
		Connection c = null;
		Statement p = null;
		ResultSet r = null;
		StringBuilder sql = new StringBuilder();

		try {
			c = getConnection();

			if (c == null) {
				throw new SQLException("get connection failed!");
			}

			if (this.isPG(c)) {
				sql.append("select create_distributed_table('").append(table).append("', '").append(key).append("')");
				p = c.createStatement();
				r = p.executeQuery(sql.toString());

				return true;
			}

		} catch (Exception e) {

			if (!tableNotExists(e) && !columnNotExists(e)) {
				log.error(sql, e);
			}

		} finally {
			close(r, p, c);
		}

		return false;
	}

	@Override
	public boolean createTable(String table, JSON cols) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public long getTime() {
		return 0;
	}

	/** The conf. */
	private static Configuration conf;

	/**
	 * initialize the DB object from the "giiwa.properties"
	 */
	public static synchronized void init() {

		conf = Config.getConf();
		if (conf == null) {
			return;
		}

		getDataSource("default");

	}

	public static BasicDataSource getDataSource(String name) {

		BasicDataSource external = dss.get(name);
		if (external == null && conf != null) {
			String url = conf.getString("db[" + name + "].url", null);
			String username = conf.getString("db[" + name + "].user", null);
			String passwd = conf.getString("db[" + name + "].passwd", null);

			if (!X.isEmpty(url)) {

				String D = _getDiver(url);

				int N = conf.getInt("db[" + name + "].conns", MAX_ACTIVE_NUMBER);

				external = _get(D, username, passwd, url, N);
				log.info(name + ".driver=" + D);

				dss.put(name, external);
			}
		}

		return external;
	}

	/** The dss. */
	private static Map<String, BasicDataSource> dss = new TreeMap<String, BasicDataSource>();

	private static BasicDataSource _get(String D, String username, String passwd, String url, int N) {
		BasicDataSource external = new BasicDataSource();
		external.setDriverClassName(D.trim());

		if (!X.isEmpty(username)) {
			external.setUsername(username.trim());
		}
		if (!X.isEmpty(passwd)) {
			external.setPassword(passwd.trim());
		}

		external.setUrl(url.trim());

//		external.setMaxActive(N);
		external.setMaxTotal(N);
		external.setDefaultAutoCommit(true);
		external.setMaxIdle(N);
//		external.setMaxWait(MAX_WAIT_TIME);
		external.setMaxWaitMillis(MAX_WAIT_TIME);
		external.setDefaultAutoCommit(true);
		external.setDefaultReadOnly(false);
		external.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		external.setValidationQuery(null);// VALIDATION_SQL);
		external.setPoolPreparedStatements(true);

		return external;
	}

	private static String _getDiver(String url) {
//		if (url.startsWith("jdbc:hsqldb:")) {
//			return "org.hsqldb.jdbcDriver";
//		} else if (url.startsWith("jdbc:h2:")) {
//			return "org.h2.Driver";
//		} else if (url.startsWith("jdbc:derby:")) {
//			return "org.apache.derby.jdbc.EmbeddedDriver";
//		} else if (url.startsWith("jdbc:firebirdsql:")) {
//			return "org.firebirdsql.jdbc.FBDriver";
		if (url.startsWith("jdbc:sqlite:")) {
			return "org.sqlite.JDBC";
		} else if (url.startsWith("jdbc:mongodb:")) {
			return "com.dbschema.MongoJdbcDriver";

		} else if (url.startsWith("jdbc:clickhouse:")) {
			// jdbc:clickhouse://localhost:8123/test
			return "cc.blynk.clickhouse.ClickHouseDriver";

		} else if (url.startsWith("jdbc:hive2:")) {
			// Hive
			return "org.apache.hive.jdbc.HiveDriver";

		} else if (url.startsWith("jdbc:postgresql:")) {
			// PostgreSQL
			return "org.postgresql.Driver";
		} else if (url.startsWith("jdbc:pivotal:greenplum:")) {
			// Greenplum
			return "com.pivotal.jdbc.GreenplumDriver";

		} else if (url.startsWith("jdbc:dm:")) {
			// DB
			return "dm.jdbc.driver.DmDriver";

		} else if (url.startsWith("jdbc:TAOS:")) {
			// TDengine
			return "com.taosdata.jdbc.TSDBDriver";

		} else if (url.startsWith("jdbc:mysql:")) {
			// MySQL
			return "com.mysql.cj.jdbc.Driver";
		} else if (url.startsWith("jdbc:mariadb:")) {
			// MariaDB
			return "org.mariadb.jdbc.Driver";
		} else if (url.startsWith("jdbc:oracle:")) {
			// Oracle
			return "oracle.jdbc.OracleDriver";
		} else if (url.startsWith("jdbc:db2:")) {
			// DB2
			return "com.ibm.db2.jcc.DB2Driver";

		} else if (url.startsWith("jdbc:informix-sqli:")) {

			return "com.informix.jdbc.IfxDriver";
		} else if (url.startsWith("jdbc:microsoft:sqlserver:")) {
			// SQLServer
			return "com.microsoft.jdbc.sqlserver.SQLServerDriver";
		} else if (url.startsWith("jdbc:sqlserver:")) {

			return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
		} else if (url.startsWith("jdbc:sybase:")) {
			// Sybase
			return "net.sourceforge.jtds.jdbc.Driver";
//		} else if (url.startsWith("jdbc:odbc:")) {
//			return "sun.jdbc.odbc.JdbcOdbcDriver";
		}
		return null;
	}

	/** The max active number. */
	private static int MAX_ACTIVE_NUMBER = 10;

	/** The max wait time. */
	private static int MAX_WAIT_TIME = 10 * 1000;

	@Override
	public void killOp(Object id) {
		// TODO Auto-generated method stub

	}

}
