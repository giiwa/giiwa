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

import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Data;
import org.giiwa.bean.GLog;
import org.giiwa.conf.Config;
import org.giiwa.conf.Global;
import org.giiwa.dao.Helper.DBHelper;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.dao.driver.MariaDB;
import org.giiwa.dao.driver.MySQL;
import org.giiwa.dao.driver.PG;
import org.giiwa.json.JSON;
import org.giiwa.misc.ClassUtil;
import org.giiwa.pool.Pool;
import org.giiwa.pool.Pool.IPoolFactory;
import org.giiwa.task.Function;
import org.giiwa.task.Task;

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

//	public static RDSHelper inst = new RDSHelper();

	private Pool<Connection> _conn = null;
	private _Driver driver;
	public String dbname;
	public String schema;
	public String username;
	public String url;

	@Override
	public void close() {
		if (_conn != null) {
			_conn.destroy();
			_conn = null;
		}
	}

	private String _where(W q, Connection c) throws SQLException {
		if (q == null || c == null) {
			return null;
		}

		return q.where();
	}

	private String _orderby(W q, Connection c) throws SQLException {
		if (q == null || c == null) {
			return null;
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
	private void _setParameter(PreparedStatement p, int i, Object o, Connection c) throws SQLException {
		if (o == null) {
			p.setObject(i, null);
		} else if (o instanceof Integer) {
			p.setInt(i, (Integer) o);
		} else if (o instanceof Date) {
//			p.setDate(i, new java.sql.Date(((Date) o).getTime()));
			p.setTimestamp(i, new Timestamp(((Date) o).getTime()));
		} else if (o instanceof java.sql.Date) {
//			p.setDate(i, (java.sql.Date) o);
			p.setTimestamp(i, new Timestamp(((java.sql.Date) o).getTime()));
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
		} else if (o instanceof UUID) {
			p.setObject(i, o);
		} else if (X.isArray(o)) {
			List<?> l1 = X.asList(o, s -> s);
			p.setString(i, l1.toString());
//			p.setObject(i, driver.createArrayOf(c, l1));
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
	@Override
	public int delete(String tablename, W q) {

		/**
		 * update it in database
		 */
		Connection c = null;
		PreparedStatement p = null;
		int n = -1;

		TimeStamp t = TimeStamp.create();

		StringBuilder sql = new StringBuilder();

		try {
			c = _getConnection();

			if (c == null)
				throw new SQLException("get connection failed!");

			/**
			 * create the sql statement
			 */
			tablename = driver.fullname(dbname, schema, tablename);
			sql.append("delete from ").append(tablename);

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

					_setParameter(p, order++, o, c);

				}
			}

			n = p.executeUpdate();

			if (log.isDebugEnabled()) {
				log.debug("delete, tablename=" + tablename + ", q=" + q + ", deleted=" + n);
			}

			if (t.pastms() > 30000) {
				log.warn("slow30, cost=" + t.past());
			}

		} catch (Exception e) {
			if (!_tableNotExists(e) && !_columnNotExists(e)) {
				log.error(sql.toString(), e);
			} else {
				// ignore the exception
			}

		} finally {
			close(p, c);

			Helper.Stat.write(tablename, t.pastms());

		}

		return n;
	}

	/**
	 * Gets the connection.
	 * 
	 * @return Connection
	 * @throws SQLException the SQL exception
	 */
	private Connection _getConnection() throws SQLException {

		Connection c = null;
		try {
			if (_conn != null) {
				c = _conn.get(X.AMINUTE);
			}

			_checkDriver(c);

			return c;

		} catch (Exception e) {
			throw new SQLException(e);
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
	public static void close(Object... objs) {
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

					Connection c = (Connection) o;
					try {
						if (!c.getAutoCommit()) {
							c.commit();
						}
					} catch (Throwable e1) {
						log.error(e1.getMessage(), e1);
					} finally {
						c.close();
					}

//					log.warn("afert clode, active=" + ds.getNumActive() + ", idle=" + ds.getNumIdle());

				}
			} catch (Throwable e) {
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
	@Override
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
			c = _getConnection();

			if (c == null)
				throw new SQLException("get connection failed!");

			table = driver.fullname(dbname, schema, table);

			sql.append("select 1 from ").append(table);

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
					try {
						_setParameter(p, order++, o, c);
					} catch (Exception e) {
						log.error("i=" + i + ", o=" + o, e);
					}
				}
			}

			r = p.executeQuery();

			if (t.pastms() > 30000) {
				log.warn("slow30, cost=" + t.past());
			}

			return r.next();

		} catch (Exception e) {
			if (!_tableNotExists(e) && !_columnNotExists(e)) {
				log.error(sql.toString(), e);
			}
		} finally {
			close(r, p, c);

			if (log.isDebugEnabled())
				log.debug("cost:" + t.past() + ", sql=[" + q);

			Helper.Stat.read(table, t.pastms());

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
	 * @throws Exception
	 */
	@Override
	public int updateTable(final String table, W q, V v) throws SQLException {

		Connection c = null;

		TimeStamp t0 = TimeStamp.create();

		try {
			c = _getConnection();

			if (c == null)
				throw new SQLException("get connection failed!");
			int n = _updateTable(table, q, v, 0, c);

			if (t0.pastms() > 30000) {
				log.warn("slow30, cost=" + t0.past() + ", table=" + table + ", q=" + q + ", v=" + v);
			}

			return n;
		} finally {
			close(c);
		}
	}

	private int _updateTable(final String table, W q, V v, int retries, Connection c) throws SQLException {

		if (v == null || v.isEmpty())
			return 0;

//		if (retries > 10) {
//			log.error("table=" + table + ", v=" + v, new Exception("exit as retires =" + retries));
//			return -1;
//		}

		Object o1 = v.value(X.UPDATED);
		if (o1 != V.ignore) {
			v.force(X.UPDATED, Global.now());
		}
		v.remove(X.CREATED);

		/**
		 * update it in database
		 */
		PreparedStatement p = null;
		StringBuilder sql = new StringBuilder();

		TimeStamp t = TimeStamp.create();

		int updated = 0;
		try {

			String table1 = driver.fullname(dbname, schema, table);

			/**
			 * create the sql statement
			 */
			sql.append("update ").append(table1).append(" set ");

			StringBuilder s = new StringBuilder();
			for (String name : v.names()) {
				if (s.length() > 0)
					s.append(",");
				s.append(name);
				s.append("=?");
			}
			sql.append(s);

			String where = _where(q, c);
			Object[] args = q.args();

			if (!X.isEmpty(where)) {
				sql.append(" where ").append(where);
			}

//			log.info("sql=" + sql);

			p = c.prepareStatement(sql.toString());

			int order = 1;
			for (String name : v.names()) {
				Object v1 = v.value(name);
				try {
					_setParameter(p, order++, v1, c);
				} catch (Exception e) {
					log.error(name + "=" + v1, e);
				}
			}

			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					Object o = args[i];

					_setParameter(p, order++, o, c);
				}
			}

			updated = p.executeUpdate();

		} catch (Exception e) {

			if (retries < 10 && _columnNotExists(e)) {
				// column missed
				V v1 = v.copy();
				q.scan(e1 -> {
					if (e1.value != null) {
						v1.append(e1.name, e1.value);
					}
				});
				if (_alertTable(table, v1, c)) {
					return _updateTable(table, q, v, retries + 1, c);
				}
			} else {
				log.error(sql.toString() + ", q=" + q + ", v=" + v + ", retries=" + retries, e);
				throw e;
			}
		} finally {
			close(p);

			Helper.Stat.write(table, t.pastms());

			if (t.pastms() > 1000 && log.isWarnEnabled()) {
				log.warn("cost=" + t.past() + ", update [" + table + "], q=" + q + ", v=" + v);
			}

		}

		return updated;

	}

	private boolean load(String table, W q, Bean b, boolean trace) throws Exception {
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
			c = _getConnection();

			if (c == null) {
				throw new SQLException("get connection failed!");
			}

			table = driver.fullname(dbname, schema, table);

			if (X.isEmpty(q.fields())) {
				sql.append("select * from ").append(table);
			} else {
				sql.append("select " + q.fields() + " from ").append(table);
			}

			String where = _where(q, c);
			Object[] args = q.args();
			String orderby = _orderby(q, c);

			if (!X.isEmpty(where)) {
				sql.append(" where ").append(where);
			}

			if (!X.isEmpty(orderby)) {
				sql.append(" ").append(orderby);
			}

			p = c.prepareStatement(sql.toString());

			int order = 1;
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					Object o = args[i];

					_setParameter(p, order++, o, c);
				}
			}

			r = p.executeQuery();

			if (t.pastms() > 30000) {
				log.warn("slow30, cost=" + t.past() + ", table=" + table + ", q=" + q);
			}

			if (r.next()) {
				b.load(r);

				return true;
			}

		} catch (Exception e) {
			log.error("cost=" + t.past() + ", sql=" + sql.toString() + ", q=" + q, e);
			throw e;
//			if (tableNotExists(e)) {
//				// create table
//				_createTable(table, V.create().append("created", System.currentTimeMillis()).append("updated",
//						System.currentTimeMillis()), c);
//
//			} else if (!columnNotExists(e)) {
//				log.error(sql, e);
//				throw e;
//			}

		} finally {
			close(r, p, c);

			if (trace) {
				log.debug("trace, load, cost = " + t.past() + ", sql=" + q + ", result=" + b);
			} else if (log.isDebugEnabled()) {
				log.debug("cost = " + t.past() + ", sql=" + q + ", result=" + b);
			}

			Helper.Stat.read(table, t.pastms());

		}

		return false;
	}

	@Override
	public String toString() {
		return "RDSHelper [dbname=" + dbname + ", pool=" + _conn + "]";
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
	@Override
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

		String sql = null;

		try {

			c = _getConnection();

			if (c == null) {
				throw new SQLException("get connection failed, _conn=" + _conn);
			}

			String where = _where(q, c);
			Object[] args = q.args();
			String orderby = _orderby(q, c);

			table = driver.fullname(dbname, schema, table);

			sql = driver.load(table, q.fields(), where, orderby, offset, limit);

			if (log.isDebugEnabled()) {
				log.debug("sql=" + sql.toString());
			}

//			log.info("sql=" + sql.toString());

			p = c.prepareStatement(sql.toString());

			if (!(driver instanceof PG)) {
				p.setQueryTimeout(300);// seconds
			}

			int order = 1;
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					Object o = args[i];
					_setParameter(p, order++, o, c);
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

			if (t.pastms() > 30000) {
				log.warn("slow30, cost=" + t.past());
			}

			return list;
		} catch (Exception e) {
//
//			if (tableNotExists(e)) {
//
//				// create table
//				_createTable(table, V.create().append("created", System.currentTimeMillis()).append("updated",
//						System.currentTimeMillis()), c);
//
//			} else if (!columnNotExists(e)) {
//				log.error("sql=" + sql, e);
//				GLog.applog.error("db", "load", sql, e);
			throw new SQLException(e);
//			}

		} finally {
			close(r, p, c);

			if (log.isDebugEnabled())
				log.debug("cost:" + t.pastms() + "ms, sql=" + q);

			Helper.Stat.read(table, t.pastms());

		}

//		return null;
	}

	/**
	 * Insert values into the table.
	 *
	 * @param table the table name
	 * @param sets  the values
	 * @param db    the db
	 * @return int
	 * @throws SQLException
	 */
	@Override
	public int insertTable(String table, V v) throws SQLException {

		if (v == null || v.isEmpty()) {
			log.warn("not data to insert, ignore");
			return 0;
		}

		long t1 = Global.now();
		if (v.value(X.CREATED) != V.ignore) {
			v.force(X.CREATED, t1);
		}
		if (v.value(X.UPDATED) != V.ignore) {
			v.force(X.UPDATED, t1);
		}

		/**
		 * insert it in database
		 */
		Connection c = null;
		TimeStamp t = TimeStamp.create();

		try {
			c = _getConnection();

			if (c == null)
				throw new SQLException("get connection failed!");

			int n = insertTable(table, v, c, 0);

			if (t.pastms() > 30000) {
				log.warn("slow30, cost=" + t.past());
			}

			return n;

		} catch (SQLException e) {
			if (log.isErrorEnabled()) {
				log.error(v.toString(), e);
			}
			throw e;
		} finally {
			close(c);

			Helper.Stat.write(table, t.pastms());

//			if (t.pastms() > 1000 && log.isWarnEnabled()) {
//				log.warn("cost=" + t.past() + ", insert [" + table + "], v=" + v);
//			}

		}

	}

	private int insertTable(final String table, V sets, Connection c, int retries) throws SQLException {

		if (sets == null || sets.isEmpty()) {
			log.warn("not data to insert, ignore");
			return 0;
		}

		if (retries > 10) {
			log.error("table=" + table + ", v=" + sets, new Exception("exit as retires =" + retries));
			return -1;
		}

		/**
		 * insert it in database
		 */
		PreparedStatement p = null;
		StringBuilder sql = new StringBuilder();

		try {

			if (c == null)
				throw new SQLException("get connection failed!");

			String table1 = driver.fullname(dbname, schema, table);

			/**
			 * create the sql statement
			 */
			sql.append("insert into ").append(table1).append(" (");
			StringBuilder s = new StringBuilder();
			int total = 0;

			for (String name : sets.names()) {
				if (s.length() > 0)
					s.append(",");
				s.append(name);
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
				_setParameter(p, order++, v, c);
			}

			return p.executeUpdate();

		} catch (Exception e) {

			// if the table not exists, create it
			if (_columnNotExists(e)) {

				log.warn(sql.toString() + "\n" + sets.toString(), e);

				// column missed
				if (_alertTable(table, sets, c)) {
					if (retries > 9) {
						log.error("table=" + table + ", v=" + sets, new Exception("exit as retires =" + retries));
					} else {
						return insertTable(table, sets, c, retries + 1);
					}
				}
			} else if (_tableNotExists(e)) {
				// table missed
				log.warn(sql.toString() + "\n" + sets.toString(), e);

				if (_createTable(table, sets, c)) {
					if (retries > 9) {
						log.error("table=" + table + ", v=" + sets, new Exception("exit as retires =" + retries));
					}
					return insertTable(table, sets, c, retries + 1);
				}

			} else {
				log.error(sql.toString() + "\n" + sets.toString(), e);
				throw e;
			}

		} finally {
			close(p);
		}
		return 0;
	}

	private boolean _tableNotExists(Throwable e) {
		return X.isCauseBy(e,
				".*(table.*does not exist|relation.*does not exist|Table.*not found|Table.*doesn't exist|Invalid table|Table.*does not exist|无效的表|表.*不存在|关系.*不存在).*");
	}

	private boolean _columnNotExists(Throwable e) {
		return X.isCauseBy(e,
				".*(column.*does not exist|Column.*not found|No such column|Missing columns|There is no column|Unknown column|Invalid column|无效的列|字段不存在|字段.*不存在).*",
				UndeclaredThrowableException.class);
	}

	private boolean _alertTable(final String table, V v, Connection c) {

		Statement stat = null;

		try {

			stat = c.createStatement();

			Map<String, String> cols = _columns(table, c);

			// log.info("table=" + table + ", cols=" + cols + ", v=" + v);

			String table1 = driver.fullname(dbname, schema, table);
			for (String name : v.names()) {
				if (cols.containsKey(name.toLowerCase()) || cols.containsKey(name.toUpperCase())) {
					continue;
				}

				StringBuilder sql = new StringBuilder("alter table ").append(table1).append(" add ");
				try {
					sql.append(name + " " + _type(v, name));
					log.warn("add column, sql=" + sql);
					stat.execute(sql.toString());
				} catch (Error e) {
					log.error(sql.toString(), e);
					v.remove(name);
				}
			}

			return true;

		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		} finally {
			close(stat);
		}

		return false;
	}

	private Map<String, String> _columns(final String table, Connection c) {

		ResultSet r = null;
		Statement stat = null;
		Map<String, String> l1 = new HashMap<String, String>();

		try {
			stat = c.createStatement();

			String table1 = driver.fullname(dbname, schema, table);
			r = stat.executeQuery("select * from " + table1 + " where 2=1");

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

	private boolean _createTable(final String tablename, V v, Connection c) throws SQLException {

		Statement stat = null;

		StringBuilder sql = new StringBuilder();

		try {
			stat = c.createStatement();

			String tablename1 = driver.fullname(dbname, schema, tablename);
			sql.append("create table ").append(tablename1).append(" ( ");

			int i = 0;
			for (String name : v.names()) {

				if (i > 0) {
					sql.append(", ");
				}
				sql.append(name + " " + _type(v, name));

				i++;
			}
			sql.append(" ) ");

			stat.execute(sql.toString());

			return true;
		} catch (SQLException e) {
			log.error("sql=" + sql.toString(), e);
			throw e;
		} finally {
			close(stat);
		}
	}

	private String _type(V val, String name) {

		String type = val.type(name);

		if (type != null) {
			// type = "text:128"
			String[] ss = X.split(type, ":");
			if (X.isIn(ss[0], "text")) {
				return "VARCHAR(" + X.toInt(ss.length > 1 ? ss[1] : null, 128) + ")";
			} else if (X.isIn(ss[0], "long", "int")) {
				return "bigint";
			} else if (X.isIn(ss[0], "double", "float")) {
				return "double";// precision";
			}
		}

		Object v = val.value(name);

		if (v == null) {
			return "VARCHAR(128)";
		} else if (v instanceof Long || v instanceof Integer) {
			return "bigint";
		} else if (v instanceof Float || v instanceof Double) {
			return "double";// precision";
		} else if (X.isArray(v)) {
			if (driver instanceof MySQL) {
				return "LONGTEXT";
			}
			return "TEXT";
		} else {
			if (v.toString().length() >= 1024) {
				if (driver instanceof MySQL) {
					return "LONGTEXT";
				}
				return "TEXT";
			}
			return "VARCHAR(128)";
		}

	}

	/**
	 * count the data.
	 *
	 * @param table the table name
	 * @param q     the query object
	 * @param db    the db
	 * @return the number of data
	 * @throws SQLException
	 */
	@Override
	public long count(String table, W q) throws SQLException {
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
	 * @throws SQLException
	 */
	@Override
	public List<?> distinct(String table, String name, W q) throws SQLException {
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

			c = _getConnection();

			if (c == null)
				throw new SQLException("get connection failed!");

			StringBuilder sql = new StringBuilder();
			table = driver.fullname(dbname, schema, table);
			sql.append("select distinct(" + name + ") from " + table);
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

					_setParameter(p, order++, o, c);
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

			if (t.pastms() > 30000) {
				log.warn("slow30, cost=" + t.past());
			}

			return list;
//		} catch (SQLException e) {
//
//			if (tableNotExists(e)) {
//				// create table
//				_createTable(table, V.create().append("created", System.currentTimeMillis()).append("updated",
//						System.currentTimeMillis()), c);
//
//			} else if (!columnNotExists(e)) {
//				log.error(q, e);
//				throw e;
//			}

		} finally {
			close(r, p, c);

			Helper.Stat.read(table, t.pastms());

		}
//		return null;
	}

	private synchronized void _checkDriver(Connection c) {
		if (driver == null) {
			try {

				String s = (c.getMetaData().getDatabaseProductName() + "//" + c.getMetaData().getDriverName() + "//"
						+ url).toLowerCase();

				for (_Driver e : _drivers) {
					if (e.check(s)) {
						driver = e;
						break;
					}
				}
				log.warn("driverinfo=" + s + ", driver=" + driver);

			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
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
	 * @throws SQLException
	 */
	@Override
	public int inc(final String table, W q, String name, int n, V sets) throws SQLException {

		Connection c = null;

		TimeStamp t = TimeStamp.create();

		try {
			c = _getConnection();
			if (c == null)
				throw new SQLException("get connection failed!");

			int n1 = _inc(table, q, name, n, c, 0);
			if (sets != null && !sets.isEmpty()) {
				this.updateTable(table, q, sets);
			}

			if (t.pastms() > 30000) {
				log.warn("slow30, cost=" + t.past());
			}

			return n1;
//		} catch (SQLException e) {
//
//			if (tableNotExists(e)) {
//				// create table
//				_createTable(table, V.create().append(name, n).append("created", System.currentTimeMillis())
//						.append("updated", System.currentTimeMillis()), c);
//
//			} else if (!columnNotExists(e)) {
//				log.error(e.getMessage(), e);
//				throw e;
//			} else {
//				throw e;
//			}
		} finally {
			close(c);

			Helper.Stat.write(table, t.pastms());

		}
//		return -1;

	}

	private int _inc(final String table, W q, String name, int n, Connection c, int retries) {
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
			String table1 = driver.fullname(dbname, schema, table);
			sql.append("update ").append(table1).append(" set ").append(name).append("=").append(name).append("+?");

//			boolean isoracle = isOracle(c);

			String where = _where(q, c);
			Object[] args = q.args();

			if (!X.isEmpty(where)) {
				sql.append(" where ").append(where);
			}

			log.info("sql=" + sql);

			p = c.prepareStatement(sql.toString());

			int order = 1;
			_setParameter(p, order++, n, c);

			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					Object o = args[i];

					_setParameter(p, order++, o, c);
				}
			}

			return p.executeUpdate();

		} catch (Exception e) {

			if (_columnNotExists(e)) {

				// column missed
				if (_alertTable(table, V.create().append(name, n), c)) {
					if (retries < 2) {
						return _inc(table, q, name, n, c, retries + 1);
					}
				}
			}

			log.error(sql.toString(), e);

		} finally {
			close(p, r);
		}

		return 0;
	}

	private int _inc(final String table, W q, JSON incvalue, Connection c) {
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
			String table1 = driver.fullname(dbname, schema, table);
			sql.append("update ").append(table1).append(" set ");

			boolean first = true;
			for (String name : incvalue.keySet()) {
				if (!first) {
					sql.append(", ");
				}
				sql.append(name).append("=").append(name).append("+?");
				first = false;
			}

//			boolean isoracle = isOracle(c);

			String where = _where(q, c);
			Object[] args = q.args();

			if (!X.isEmpty(where)) {
				sql.append(" where ").append(where);
			}

			p = c.prepareStatement(sql.toString());

			int order = 1;
			for (String name : incvalue.keySet()) {
				_setParameter(p, order++, incvalue.getInt(name), c);
			}

			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					Object o = args[i];

					_setParameter(p, order++, o, c);
				}
			}

			return p.executeUpdate();

		} catch (Exception e) {

			log.error(sql.toString(), e);

			if (_columnNotExists(e)) {

				// column missed
				for (String name : incvalue.keySet()) {
					if (_alertTable(table, V.create().append(name, incvalue.getInt(name)), c)) {
						return _inc(table, q, name, incvalue.getInt(name), c, 0);
					}
				}

			}

		} finally {
			close(p, r);
		}

		return 0;
	}

	@Override
	public void createIndex(String table, LinkedHashMap<String, Object> ss, boolean unique) {

		Connection c = null;
		Statement stat = null;

		TimeStamp t0 = TimeStamp.create();

		try {
			c = _getConnection();
			stat = c.createStatement();
			String sql = driver.createIndex(dbname, schema, table, ss, unique);
			if (log.isInfoEnabled()) {
				log.info("create index, sql=" + sql.toString());
			}
			stat.executeUpdate(sql);

			if (t0.pastms() > 30000) {
				log.warn("slow30, cost=" + t0.past());
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			close(stat, c);
		}

	}

	@Override
	public List<Map<String, Object>> getIndexes(String table) {

		Connection c = null;

		TimeStamp t0 = TimeStamp.create();

		try {

			c = _getConnection();

			List<Map<String, Object>> l1 = driver.getIndexes(c, dbname, schema, table);

			if (t0.pastms() > 30000) {
				log.warn("slow30, cost=" + t0.past());
			}
			return l1;

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			close(c);
		}
		return null;

	}

	@Override
	public void dropIndex(String table, String name) {
		Connection c = null;
		Statement stat = null;

		TimeStamp t0 = TimeStamp.create();

		try {

			c = _getConnection();
			stat = c.createStatement();
			stat.executeUpdate("drop index " + name);

			if (t0.pastms() > 30000) {
				log.warn("slow30, cost=" + t0.past());
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			close(stat, c);
		}
	}

	@Override
	public int insertTable(String table, List<V> values) throws SQLException {

		if (values == null || values.isEmpty())
			return 0;

		/**
		 * insert it in database
		 */
		PreparedStatement p = null;
		Connection c = null;

		TimeStamp t = TimeStamp.create();
		try {
			if (X.isEmpty(values))
				return 0;

			c = _getConnection();

			table = driver.fullname(dbname, schema, table);

			/**
			 * create the sql statement
			 */
			StringBuilder sql = new StringBuilder();
			sql.append("insert into ").append(table).append(" (");
			StringBuilder s = new StringBuilder();
			int total = 0;
//			boolean isoracle = isOracle(c);

			for (String name : values.get(0).names()) {
				if (s.length() > 0)
					s.append(",");
//				if (isoracle && oracle.containsKey(name)) {
//					s.append(oracle.get(name));
//				} else {
				s.append(name);
//				}
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
				if (v.value(X.CREATED) != V.ignore) {
					v.force(X.CREATED, t1);
				}
				if (v.value(X.UPDATED) != V.ignore) {
					v.force(X.UPDATED, t1);
				}
//				v.append("_node", Global.id());

				for (String name : v.names()) {
					Object v1 = v.value(name);
					_setParameter(p, order++, v1, c);
				}

				p.addBatch();
			}

			int n = X.sum(p.executeBatch());

			if (t.pastms() > 30000) {
				log.warn("slow30, cost=" + t.past());
			}

			return n;

		} catch (SQLException e) {

			log.error(values.toString(), e);
			throw e;

		} finally {
			close(p, c);

			Helper.Stat.write(table, t.pastms());

			if (t.pastms() > 1000 && log.isWarnEnabled()) {
				log.warn("cost=" + t.past() + ", insert [" + table + "], v=" + values);
			}

		}

	}

	public static DBHelper create(String url, String user, String passwd, int conns, String locale) {

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
						Connection c = getConnection(url, user, passwd, locale);
						c.setAutoCommit(true);
						return c;
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

		String dbname = url;
		int i = dbname.indexOf("?");
		if (i > 0) {
			dbname = dbname.substring(0, i);
		}
		i = dbname.lastIndexOf("/");
		if (i > 0) {
			dbname = dbname.substring(i + 1);
		}
		return create(pool, dbname, user, url);

	}

	public static RDSHelper create(Pool<Connection> c, String dbname, String username) {
		return create(c, dbname, username, null);
	}

	public static RDSHelper create(Pool<Connection> c, String dbname, String username, String url) {

		RDSHelper d = new RDSHelper();
		d._conn = c;
		d.dbname = dbname;
		d.username = username;
		d.url = url;

		if (log.isInfoEnabled()) {
			log.info("create DBHelper from pool, dbname=" + dbname + ", username=" + username + ", con=" + c);
		}

		return d;
	}

	@Override
	public List<JSON> listTables(String tablename, int n) {

		Connection con = null;
		TimeStamp t0 = TimeStamp.create();

		try {

			con = this._getConnection();
			if (con == null) {
				log.warn("get connection failed!");
				return null;
			}

			List<JSON> l1 = driver.listTables(con, dbname, schema, username, tablename, n);

			Collections.sort(l1, new Comparator<JSON>() {

				@Override
				public int compare(JSON o1, JSON o2) {
					return o1.getString("name").compareToIgnoreCase(o2.getString("name"));
				}

			});

			if (t0.pastms() > 30000) {
				log.warn("slow30, cost=" + t0.past());
			}

			return l1;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			close(con);
		}
		return null;

	}

	@Override
	public <T extends Bean> boolean stream(String table, W q, long offset, Function<T, Boolean> func, Class<T> t1)
			throws SQLException {
		/**
		 * search it in database
		 */
		Connection c = null;
		PreparedStatement p = null;
		ResultSet r = null;

		String sql = null;

		try {
			c = _getConnection();

			if (c == null)
				throw new SQLException("get connection failed!");

			table = driver.fullname(dbname, schema, table);

			String where = _where(q, c);
			Object[] args = q.args();
			String orderby = _orderby(q, c);

			sql = driver.cursor(table, where, orderby, offset);

			if (log.isDebugEnabled())
				log.debug("sql=" + sql.toString());

			p = c.prepareStatement(sql.toString());

			int order = 1;
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					Object o = args[i];

					_setParameter(p, order++, o, c);
				}
			}

			r = p.executeQuery();

			while (r.next()) {

				if (offset > 0) {
					offset--;
					continue;
				}

				try {
					T d = t1.getDeclaredConstructor().newInstance();
					d.load(r);
					if (!func.apply(d)) {
						return false;
					}
				} catch (Exception e) {
					log.error(e.getMessage(), e);
					return false;
				}

			}

			return true;
		} finally {
			close(r, p, c);
		}

	}

	@Override
	public List<JSON> getMetaData(String tablename) {
		Connection c = null;
		Statement stat = null;
		ResultSet r = null;

		TimeStamp t0 = TimeStamp.create();

		try {
			c = _getConnection();

			stat = c.createStatement();

			tablename = driver.fullname(dbname, schema, tablename);

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

			if (t0.pastms() > 30000) {
				log.warn("slow30, cost=" + t0.past());
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

			c = _getConnection();

			if (c == null) {
				throw new SQLException("get connection failed!");
			}

			table = driver.fullname(dbname, schema, table);

			StringBuilder sql = new StringBuilder();
			sql.append("select sum(" + name + ") t from ").append(table);
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

					_setParameter(p, order++, o, c);
				}
			}

			r = p.executeQuery();
			if (r.next()) {
				n = r.getObject("t");
			}

			if (t.pastms() > 30000) {
				log.warn("slow30, cost=" + t.past());
			}

		} catch (Exception e) {

			if (!_tableNotExists(e) && !_columnNotExists(e)) {
				log.error(q, e);
			}

		} finally {
			close(r, p, c);

			if (log.isDebugEnabled())
				log.debug("sum, cost:" + t.past() + ", sql=" + q + ", n=" + n);

			Helper.Stat.read(table, t.pastms());

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

		log.info("driver=" + D + ", url=" + url + ", user=" + username);

		if (!X.isEmpty(D)) {
			Locale oldlocale = Locale.getDefault();
			try {
				if (!X.isEmpty(locale)) {
					if (X.isSame(locale, "en")) {
						Locale.setDefault(Locale.US);
					} else if (X.isSame(locale, "zh")) {
						Locale.setDefault(Locale.CHINA);
					}
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
				log.error(url, e);
				throw new SQLException(e);
			} finally {
				Locale.setDefault(oldlocale);
			}
		} else {
			throw new SQLException("unknown URL");
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T avg(String table, W q, String name) throws SQLException {

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

			c = _getConnection();

			if (c == null) {
				throw new SQLException("get connection failed!");
			}

			table = driver.fullname(dbname, schema, table);

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

					_setParameter(p, order++, o, c);
				}
			}

			r = p.executeQuery();
			if (r.next()) {
				n = r.getObject("t");
			}

//		} catch (SQLException e) {
//			if (tableNotExists(e)) {
//				// create table
//				_createTable(table, V.create().append("created", System.currentTimeMillis()).append("updated",
//						System.currentTimeMillis()), c);
//
//			} else if (!columnNotExists(e)) {
//				log.error(q, e);
//				throw e;
//			} else {
//				throw e;
//			}

			if (t.pastms() > 30000) {
				log.warn("slow30, cost=" + t.past());
			}

		} finally {
			close(r, p, c);

			if (log.isDebugEnabled())
				log.debug("avg, cost:" + t.past() + ", sql=" + q + ", n=" + n);

			Helper.Stat.read(table, t.pastms());

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

		TimeStamp t0 = TimeStamp.create();

		try {

			c = _getConnection();
			stat = c.createStatement();

			table = driver.fullname(dbname, schema, table);

			stat.executeUpdate("drop table " + table);

			if (t0.pastms() > 30000) {
				log.warn("slow30, cost=" + t0.past());
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			close(stat, c);
		}
	}

	@Override
	public List<JSON> count(String table, W q, String[] group, int n) throws SQLException {
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

			c = _getConnection();

			if (c == null) {
				throw new SQLException("get connection failed!");
			}

			table = driver.fullname(dbname, schema, table);

			StringBuilder sql = new StringBuilder();
			sql.append("select");
			for (int i = 0; i < group.length; i++) {
				sql.append(group[i]);
			}
			sql.append(",sum(" + name + ") t from ").append(table);
			String where = _where(q, c);
			Object[] args = q.args();

			if (!X.isEmpty(where)) {
				sql.append(" where ").append(where);
			}

			sql.append(" groug by ");
			for (int i = 0; i < group.length; i++) {
				sql.append(group[i]).append(",");
			}

			BasicDBObject sort = q.order();
			if (sort != null && !sort.isEmpty()) {
				sql.append("order by ");
				int i = 0;
				for (String s : sort.keySet()) {
					if (i > 0)
						sql.append(",");

					if (X.isSame(s, "sum")) {
						sql.append("t");
					} else {
						sql.append(s.replaceAll("_id.", X.EMPTY));
					}

					if (X.toInt(sort.get(s)) == -1) {
						sql.append(" desc");
					}
				}
			}

			p = c.prepareStatement(sql.toString());

			int order = 1;
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					Object o = args[i];

					_setParameter(p, order++, o, c);
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

			if (t.pastms() > 30000) {
				log.warn("slow30, cost=" + t.past());
			}

			return l1;

		} catch (Exception e) {
			log.error(q, e);

		} finally {
			close(r, p, c);

			if (log.isDebugEnabled())
				log.debug("cost:" + t.past() + ", sql=" + q + ", n=" + n);

			Helper.Stat.read(table, t.pastms());

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

			c = _getConnection();

			if (c == null) {
				throw new SQLException("get connection failed!");
			}

			table = driver.fullname(dbname, schema, table);

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

					if (X.toInt(sort.get(s)) == -1) {
						sum.append(" desc");
					}
				}
			}

			p = c.prepareStatement(sum.toString());

			int order = 1;
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					Object o = args[i];

					_setParameter(p, order++, o, c);
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

			if (t.pastms() > 30000) {
				log.warn("slow30, cost=" + t.past());
			}

			return l1;

		} catch (Exception e) {
			log.error(q, e);

		} finally {
			close(r, p, c);

			if (log.isDebugEnabled())
				log.debug("cost:" + t.past() + ", sql=" + q + ", n=" + n);

			Helper.Stat.read(table, t.pastms());

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

			c = _getConnection();

			if (c == null) {
				throw new SQLException("get connection failed!");
			}

			table = driver.fullname(dbname, schema, table);

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

					if (X.toInt(sort.get(s)) == -1) {
						sum.append(" desc");
					}
				}
			}

			p = c.prepareStatement(sum.toString());

			int order = 1;
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					Object o = args[i];

					_setParameter(p, order++, o, c);
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

			if (t.pastms() > 30000) {
				log.warn("slow30, cost=" + t.past());
			}

			return l1;
		} catch (Exception e) {
			log.error(q, e);
		} finally {
			close(r, p, c);

			if (log.isDebugEnabled())
				log.debug("cost:" + t.past() + ", sql=" + q + ", n=" + n);

			Helper.Stat.read(table, t.pastms());

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
		Connection c = null;
		TimeStamp t0 = TimeStamp.create();
		try {
			c = _getConnection();

			if (c == null) {
				throw new SQLException("get connection failed!");
			}

			long n = driver.size(c, dbname, schema, table);
			if (t0.pastms() > 30000) {
				log.warn("slow30, cost=" + t0.past());
			}
			return n;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			close(c);

		}
		return -1;
	}

	@Override
	public long count(String table, W q, String name) throws SQLException {

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

		long n = 0;
		try {

			c = _getConnection();

			if (c == null) {
				throw new SQLException("get connection failed!");
			}

			table = driver.fullname(dbname, schema, table);

			sql.append("select count(" + name + ") t from ").append(table);
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

					_setParameter(p, order++, o, c);
				}
			}

			r = p.executeQuery();
			if (r.next()) {
				n = r.getInt("t");
			}

			if (t.pastms() > 30000) {
				log.warn("slow30, cost=" + t.past() + ", sql=" + sql);
			}


		} finally {
			close(r, p, c);

			if (log.isDebugEnabled())
				log.debug("cost:" + t.past() + ", sql=" + q + ", n=" + n);

			Helper.Stat.read(table, t.pastms());

		}

		return n;
	}

	@Override
	public List<JSON> count(String table, W q, String name, String[] group, int n1) throws SQLException {

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

			c = _getConnection();

			if (c == null) {
				throw new SQLException("get connection failed!");
			}

			table = driver.fullname(dbname, schema, table);

			StringBuilder sql = new StringBuilder();
			sql.append("select ");
			for (int i = 0; i < group.length; i++) {
				sql.append(group[i]);
			}
			sql.append(",count(" + name + ") t from ").append(table);
			String where = _where(q, c);
			Object[] args = q.args();

			if (!X.isEmpty(where)) {
				sql.append(" where ").append(where);
			}

			sql.append(" group by ");
			for (int i = 0; i < group.length; i++) {
				if (i > 0) {
					sql.append(",");
				}
				sql.append(group[i]);
			}

			log.info("sql=" + sql.toString());

			p = c.prepareStatement(sql.toString());

			int order = 1;
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					Object o = args[i];

					_setParameter(p, order++, o, c);
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

			if (t.pastms() > 30000) {
				log.warn("slow30, cost=" + t.past());
			}

			return l1;
//		} catch (SQLException e) {
//			if (tableNotExists(e)) {
//				// create table
//				_createTable(table, V.create().append("created", System.currentTimeMillis()).append("updated",
//						System.currentTimeMillis()), c);
//
//			} else if (!columnNotExists(e)) {
//				log.error(q, e);
//				throw e;
//			} else {
//				throw e;
//			}

		} finally {
			close(r, p, c);

			if (log.isDebugEnabled())
				log.debug("cost:" + t.past() + ", sql=" + q + ", n=" + n);

			Helper.Stat.read(table, t.pastms());

		}

//		return null;
	}

	@Override
	public void repair(String table) {

	}

	@Override
	public JSON stats(String table) {

		if (X.isEmpty(table)) {
			return null;
		}

		// get stats for table
		Connection c = null;

		TimeStamp t0 = TimeStamp.create();

		try {
			c = _getConnection();

			if (c == null) {
				throw new SQLException("get connection failed!");
			}

			JSON j1 = driver.stats(c, dbname, schema, table);

			if (t0.pastms() > 30000) {
				log.warn("slow30, cost=" + t0.past() + ", table=" + table + ", r=" + j1);
			}
			return j1;

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			close(c);
		}
		return null;
	}

	@Override
	public JSON status() {
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T max(String table, W q, String name) {

		q = q.copy().clearSort();
		Data e = this.load(table, q.sort(name, -1), Data.class, false);
		if (e != null) {
			return (T) e.get(name);
		}
		return null;

	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T min(String table, W q, String name) {

		q = q.copy().clearSort();
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
		String sql = null;

		TimeStamp t0 = TimeStamp.create();

		try {
			c = _getConnection();

			if (c == null) {
				throw new SQLException("get connection failed!");
			}

			sql = driver.distributed(table, key);
			if (!X.isEmpty(sql)) {
				p = c.createStatement();
				r = p.executeQuery(sql);
				return true;
			}

			if (t0.pastms() > 30000) {
				log.warn("slow30, cost=" + t0.past());
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			close(r, p, c);

		}

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

		if (conf == null) {
			conf = Config.getConf();
		}

		// search all driver
		List<Class<_Driver>> l1 = ClassUtil.listSubType(Arrays.asList("org.giiwa.dao.driver"), _Driver.class);
		if (l1 != null) {
			l1.forEach(c -> {

				if (Modifier.isAbstract(c.getModifiers())) {
					return;
				}

				try {
					for (_Driver e : _drivers) {
						if (X.isSame(e, e.getClass())) {
							continue;
						}
					}

					_Driver h1 = c.getDeclaredConstructor().newInstance();
					_drivers.add(h1);
				} catch (Exception e) {
					log.error(c.toString(), e);
				}

			});
		}

	}

	private static List<_Driver> _drivers = new ArrayList<_Driver>();

	public static RDSHelper inst;

	private static String _getDiver(String url) {

		if (url.startsWith("jdbc:postgresql:")) {
			// PostgreSQL
			return "org.postgresql.Driver";

		} else if (url.startsWith("jdbc:dm:")) {
			// DM
			return "dm.jdbc.driver.DmDriver";

		} else if (url.startsWith("jdbc:mysql:")) {
			// MySQL
			return "com.mysql.cj.jdbc.Driver";
		} else if (url.startsWith("jdbc:mariadb:")) {
			// MariaDB
			return "org.mariadb.jdbc.Driver";
//			if (url.startsWith("jdbc:hsqldb:")) {
//			return "org.hsqldb.jdbcDriver";
//		} else if (url.startsWith("jdbc:h2:")) {
//			return "org.h2.Driver";
//		} else if (url.startsWith("jdbc:derby:")) {
//			return "org.apache.derby.jdbc.EmbeddedDriver";
//		} else if (url.startsWith("jdbc:firebirdsql:")) {
//			return "org.firebirdsql.jdbc.FBDriver";
		} else if (url.startsWith("jdbc:mongodb:")) {
			return "com.dbschema.MongoJdbcDriver";

		} else if (url.startsWith("jdbc:oceanbase:")) {
			// conn=jdbc:oceanbase://127.1:2883/tpcc?useUnicode=true&characterEncoding=utf-8&rewriteBatchedStatements=true&allowMultiQueries=true
			return "com.alipay.oceanbase.jdbc.Driver";

		} else if (url.startsWith("jdbc:sqlite:")) {
			return "org.sqlite.JDBC";
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
			// jdbc:sqlserver://localhost:1433;encrypt=true;user=MyUserName;password=*****;
			return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
		} else if (url.startsWith("jdbc:sybase:")) {
			// Sybase
			return "net.sourceforge.jtds.jdbc.Driver";
//		} else if (url.startsWith("jdbc:odbc:")) {
//			return "sun.jdbc.odbc.JdbcOdbcDriver";
		}
		return null;
	}

	@Override
	public void killOp(Object id) {
		// TODO Auto-generated method stub

	}

	@Override
	public int inc(final String table, W q, JSON incvalue, V sets) throws SQLException {

		Connection c = null;

		TimeStamp t = TimeStamp.create();

		try {
			c = _getConnection();
			if (c == null)
				throw new SQLException("get connection failed!");

			int n1 = _inc(table, q, incvalue, c);
			if (sets != null && !sets.isEmpty()) {
				this.updateTable(table, q, sets);
			}

			if (t.pastms() > 30000) {
				log.warn("slow30, cost=" + t.past());
			}

			return n1;
//		} catch (SQLException e) {
//
//			if (tableNotExists(e)) {
//				// create table
//				for (String name : incvalue.keySet()) {
//					_createTable(table,
//							V.create().append(name, incvalue.getInt(name)).append("created", System.currentTimeMillis())
//									.append("updated", System.currentTimeMillis()),
//							c);
//				}
//			} else if (!columnNotExists(e)) {
//				log.error(e.getMessage(), e);
//				throw e;
//			} else {
//				throw e;
//			}
		} finally {

			close(c);

			Helper.Stat.write(table, t.pastms());

		}
//		return -1;

	}

	@Override
	public void copy(String src, String dest, W filter) throws SQLException {

		Task.schedule(t -> {
			Connection c = null;
			PreparedStatement p1 = null;
			PreparedStatement p2 = null;
			ResultSet r = null;

			StringBuilder sql = new StringBuilder();

			TimeStamp t0 = TimeStamp.create();

			try {
				c = _getConnection();

				if (c == null)
					throw new SQLException("get connection failed!");

				String src1 = driver.fullname(dbname, schema, src);
				String dest1 = driver.fullname(dbname, schema, dest);

				sql.append("select * from " + src1);
				Object[] args = null;
				if (filter != null && !filter.isEmpty()) {
					// todo
					String where = _where(filter, c);
					args = filter.args();
					if (!X.isEmpty(where)) {
						sql.append(" where ").append(where);
					}
				}
				p1 = c.prepareStatement(sql.toString());
				if (args != null) {
					int order = 1;
					for (int i = 0; i < args.length; i++) {
						Object o = args[i];
						_setParameter(p1, order++, o, c);
					}
				}

				r = p1.executeQuery();

				int columns = -1;
				while (r.next()) {

					if (p2 == null) {

						ResultSetMetaData md = r.getMetaData();
						columns = md.getColumnCount();

						sql.append("insert into ").append(dest1).append(" (");

						StringBuilder s1 = new StringBuilder();
						for (int i = 1; i < columns + 1; i++) {
							if (s1.length() > 0)
								s1.append(",");
							s1.append(md.getColumnName(i));
						}
						sql.append(s1).append(") values( ");

						for (int i = 1; i < columns; i++) {
							sql.append("?, ");
						}
						sql.append("?)");

						p2 = c.prepareStatement(sql.toString());
					}

					p2.clearParameters();
					for (int i = 1; i < columns + 1; i++) {
						Object v = r.getObject(i);
						p2.setObject(i, v);
					}
					p2.executeUpdate();

				}

				if (t0.pastms() > 30000) {
					log.warn("slow30, cost=" + t0.past());
				}

			} catch (Exception e) {
				log.error(e.getMessage(), e);
				GLog.applog.error("db", "copy", e.getMessage(), e);
			} finally {
				close(r, p1, p2, c);
			}
		});

	}

	@Override
	public void addColumn(String tablename, JSON col) throws SQLException {

		Connection c = null;
		Statement stat = null;
		TimeStamp t0 = TimeStamp.create();

		try {
			c = this._getConnection();
			stat = c.createStatement();

			tablename = driver.fullname(dbname, schema, tablename);
			String sql = driver.addColumn(tablename, col);

			log.info(sql);
			stat.execute(sql);

			driver.comment(c, dbname, schema, tablename, null, Arrays.asList(col));

			if (t0.pastms() > 30000) {
				log.warn("slow30, cost=" + t0.past());
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			close(stat, c);
		}

	}

	@Override
	public void alterColumn(String tablename, JSON col) throws SQLException {

		Connection c = null;
		Statement stat = null;

		TimeStamp t0 = TimeStamp.create();

		try {
			c = this._getConnection();
			stat = c.createStatement();

			String name = col.getString("name");
			String type = col.getString("type");
			int size = col.getInt("size");

			List<String> sql = driver.alertColumn(dbname, schema, tablename, name, type, size);
			for (String s : sql) {
				log.warn("alter table = " + s);
				stat.execute(s);
			}

			driver.comment(c, dbname, schema, tablename, null, Arrays.asList(col));

			if (t0.pastms() > 30000) {
				log.warn("slow30, cost=" + t0.past());
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			close(stat, c);
		}

	}

	@Override
	public void createTable(final String tablename, String memo, List<JSON> cols) throws SQLException {
		// create table
		Connection con = null;
		Statement stat = null;

		StringBuilder sql = new StringBuilder();

		TimeStamp t0 = TimeStamp.create();

		try {
			con = this._getConnection();
			if (con == null)
				throw new SQLException("get connection failed!");

			stat = con.createStatement();

			String tablename1 = driver.fullname(dbname, schema, tablename);

			List<JSON> l2 = null;
			try {

				l2 = driver.listColumns(con, dbname, schema, tablename);
//				stat.executeQuery("select 1 from " + tablename1 + " where 2=1");
				// exists
				if (l2 != null && !l2.isEmpty()) {
//					log.info("table [" + tablename1 + "] exists! cols=" + l2);

					// check columns
					for (JSON j1 : cols) {
						String name = j1.getString("name");
						JSON j2 = X.get(l2, "name", name);

						// log.info("name=" + name + ", j2=" + j2);
						if (j2 == null) {
							this.addColumn(tablename1, j1);
						} else {
							String type1 = j1.getString("type");
							String type2 = j2.getString("type");
							int size1 = j1.getInt("size");
							int size2 = j2.getInt("size");

							if (!X.isSame(type1, type2)) {
								// type different
								log.warn("alert table " + tablename + ", column " + name + ", type1=" + type1
										+ ", type2=" + type2);
								this.alterColumn(tablename, j1);
							} else {
								// check size
								if (X.isIn(type1, "text")) {
									if (size1 != 0 && size2 != 0) {
										if (size1 != size2 && (size1 < 1024 || size2 < 1024)) {

											log.warn("alert table " + tablename + ", column " + name + ", type=" + type1
													+ ", size1=" + size1 + ", size2=" + size2);

											this.alterColumn(tablename, j1);
										}
									}
								} else if (X.isIn(type1, "float", "double")) {
									if (size1 != size2) {
										log.warn("alert table " + tablename + ", column " + name + ", type=" + type1
												+ ", size1=" + size1 + ", size2=" + size2 + ", j1=" + j1 + ", j2="
												+ j2);
										this.alterColumn(tablename, j1);
									}
								}
							}
						}
					}

					// check and update the comments
					driver.comment(con, dbname, schema, tablename, memo, cols);

					return;
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}

			log.info("creating table [" + tablename1 + "] ...");
			sql.append("create table " + tablename1 + " ( ");

			int i = 0;
			for (JSON col : cols) {
				if (i > 0) {
					sql.append(", ");
				}
				String name = col.getString("name");
				String type = col.getString("type");
				int size = col.getInt("size");
				String display = col.getString("display").replaceAll("'", "\"");
				int key = col.getInt("key");

				String s = driver.createColumn(name, type, size, key, display);
				sql.append(s);

				i++;
			}
			sql.append(" )");
			if (!X.isEmpty(memo)) {
				if (driver instanceof MySQL || driver instanceof MariaDB) {
					sql.append("comment '" + memo.replaceAll("'", "\"") + "'");
				}

			}

			log.info(sql.toString());

			stat.execute(sql.toString());

			driver.comment(con, dbname, schema, tablename1, memo, cols);

			if (t0.pastms() > 30000) {
				log.warn("slow30, cost=" + t0.past());
			}

		} catch (SQLException e) {
			log.error(sql.toString(), e);
			throw e;
		} finally {
			close(stat, con);

		}

	}

	@Override
	public void delColumn(String tablename, String colname) throws SQLException {
		Statement stat = null;
		Connection con = null;
		TimeStamp t0 = TimeStamp.create();
		try {
			con = this._getConnection();
			stat = con.createStatement();
			tablename = driver.fullname(dbname, schema, tablename);
			stat.execute("alter table " + tablename + " drop column " + colname);

			if (t0.pastms() > 30000) {
				log.warn("slow30, cost=" + t0.past());
			}

		} finally {
			close(stat, con);
		}
	}

	@Override
	public List<JSON> listColumns(String table) throws SQLException {

		Connection con = null;

		TimeStamp t0 = TimeStamp.create();

		try {

			con = this._getConnection();

			List<JSON> l1 = driver.listColumns(con, dbname, schema, table);

			if (t0.pastms() > 30000) {
				log.warn("slow30, cost=" + t0.past());
			}

			return l1;

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			close(con);

		}
		return null;
	}

	public static interface _Driver {

		boolean check(String driverinfo);

		long size(Connection c, String dbname, String schema, String table);

		void comment(Connection c, String dbname, String schema, String tablename, String memo, List<JSON> cols)
				throws SQLException;

		String createIndex(String dbname, String schema, String table, LinkedHashMap<String, Object> key,
				boolean unique);

		String fullname(String dbname, String schema, String name);

		List<JSON> listTables(Connection con, String dbname, String schema, String username, String tablename, int n)
				throws SQLException;

		List<JSON> listColumns(Connection con, String dbname, String schema, String table) throws SQLException;

		List<String> alertColumn(String dbname, String schema, String tablename, String name, String type, int size);

		String createColumn(String name, String type, int size, int key, String memo);

		String addColumn(String tablename, JSON col);

		String distributed(String table, String key);

		JSON stats(Connection c, String dbname, String schema, String table) throws SQLException;

		String cursor(String table, String where, String orderby, long offset);

		List<Map<String, Object>> getIndexes(Connection c, String dbname, String schema, String tablename)
				throws SQLException;

		String load(String table, String fields, String where, String orderby, int offset, int limit);

		Object createArrayOf(Connection c, List<?> l1) throws SQLException;

		String type(String type, int size);

		String alterTable(String dbname, String schema, String tablename, int partitions);

	}

	public static abstract class _AbstractDriver implements _Driver {

		protected static Log log = RDSHelper.log;
//		protected static RDSHelper inst = RDSHelper.inst;

		@Override
		public Object createArrayOf(Connection c, List<?> l1) throws SQLException {
			return X.join(l1, ",");
		}

		public long size(Connection c, String dbname, String schema, String table) {
			return 0;
		}

		@Override
		public String createIndex(String dbname, String schema, final String table, LinkedHashMap<String, Object> key,
				boolean unique) {

			String indexname = table + "_index_";
			if (indexname.length() > 50) {
				indexname = indexname.substring(0, 49) + "_";
			}
			indexname += UID.id(key.toString());

			StringBuilder sql = new StringBuilder();
			sql.append("create ");
			if (unique) {
				sql.append(" unique ");
			}
			sql.append(" index ").append(indexname);

			String table1 = fullname(dbname, schema, table);
			sql.append(" on ").append(table1).append("(");

			StringBuilder sb1 = new StringBuilder();
			for (String s : key.keySet()) {
				if (sb1.length() > 0)
					sb1.append(",");
				sb1.append(s);
				if (X.toInt(key.get(s)) < 1) {
					sb1.append(" ").append("desc");
				}
			}
			sql.append(sb1.toString()).append(")");

			return sql.toString();

		}

		@Override
		public String fullname(String dbname, String schema, String name) {
			return name;
		}

		@Override
		public List<JSON> listTables(Connection c, String dbname, String schema, String username, String tablename,
				int n) throws SQLException {

			List<JSON> list = JSON.createList();

			ResultSet r = null;
			try {

				DatabaseMetaData md = c.getMetaData();
				r = md.getTables(null, null, null, null);

				while (r.next()) {
					String name = r.getString("table_name");
					if (name != null && (X.isEmpty(tablename) || name.matches(tablename))) {
						Bean b = new Bean();
						b.load(r);
						list.add(b.json());
						if (n > 0 && list.size() > n) {
							break;
						}
					}
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
				close(r);
			}
			return list;
		}

		@Override
		public List<JSON> listColumns(Connection con, String dbname, String schema, String table) throws SQLException {

			List<JSON> l1 = JSON.createList();

			ResultSet r = null;

			try {
				if (table.contains(".")) {
					int i = table.lastIndexOf(".");
					table = table.substring(i + 1);
				}

				Set<String> names = new HashSet<String>();
				DatabaseMetaData meta = con.getMetaData();
				Set<String> keys = new HashSet<String>();
				r = meta.getPrimaryKeys(con.getCatalog(), "%", table);
				while (r.next()) {
					keys.add(r.getString("column_name"));
				}
				close(r);

				r = meta.getColumns(con.getCatalog(), "%", table, "%");
				while (r.next()) {

					JSON j1 = JSON.create();

					String name = r.getString("column_name");
					j1.append("name", name.toLowerCase());
					if (names.contains(name)) {
						continue;
					}
					names.add(name);
					String display = r.getString("remarks");
					if (X.isEmpty(display)) {
						display = name;
					}
					j1.append("display", display);

					j1.append("key", keys.contains(j1.getString("name")) ? 1 : 0);
					j1.append("nullable", r.getString("is_nullable"));

					String type1 = r.getString("type_name").toUpperCase();
					int i = type1.lastIndexOf("(");
					int size = X.toInt(r.getObject("column_size"));
					if (i > 0) {
						int j = type1.indexOf(")", i + 1);
						if (j > 0) {
							String[] ss = X.split(type1.substring(i + 1, j), ",");
							size = X.toInt(ss[ss.length - 1]);
						}
						type1 = type1.substring(0, i);
					}
					j1.append("type", type(type1));
					j1.append("type1", type1);
					if (X.isIn(j1.getString("type"), "double")) {
						size = X.toInt(r.getObject("decimal_digits"));
					}

					j1.append("size", size);

					l1.add(j1);
				}
			} finally {
				close(r);
			}
			return l1;
		}

		@Override
		public List<String> alertColumn(String dbname, String schema, String tablename, String name, String type,
				int size) {
			return Arrays.asList("alter table " + tablename + " drop column " + name,
					"alter table " + tablename + " add column " + name + " " + type(type, size));
		}

		@Override
		public String createColumn(String name, String type, int size, int key, String memo) {
			return name + " " + type(type, size);
		}

		@Override
		public String alterTable(String dbname, String schema, String tablename, int partitions) {
			return null;
		}

		@Override
		public String addColumn(String tablename, JSON col) {

			String name = col.getString("name");
			String type = col.getString("type");
			int size = col.getInt("size");

			String sql = "alter table " + tablename + " add column " + name + " " + type(type, size);
			return sql;
		}

		@Override
		public String load(String table, String fields, String where, String orderby, int offset, int limit) {
			StringBuilder sql = new StringBuilder();
			sql.append("select ");

			if (X.isEmpty(fields)) {
				sql.append(" * ");
			} else {
				sql.append(" " + fields + " ");
			}

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

			return sql.toString();
		}

		@Override
		public List<Map<String, Object>> getIndexes(Connection c, String dbname, String schema, String tablename)
				throws SQLException {

			List<Map<String, Object>> l1 = new ArrayList<Map<String, Object>>();

			ResultSet r = null;
			try {
				DatabaseMetaData d1 = c.getMetaData();

				r = d1.getIndexInfo(null, null, tablename, false, true);

				Map<String, Map<String, Object>> indexes = new TreeMap<String, Map<String, Object>>();
				while (r.next()) {

					String table1 = r.getString("table_name");
					if (!X.isSame(tablename, table1)) {
						continue;
					}

					String name = r.getString("index_name").toLowerCase();
					Map<String, Object> m = indexes.get(name);
					if (m == null) {
						m = new LinkedHashMap<String, Object>();
						indexes.put(name, m);
					}

					m.put(r.getString("column_name").toLowerCase(), X.isSame(r.getString("asc_or_desc"), "A") ? 1 : -1);
				}

				for (String name : indexes.keySet()) {
					Map<String, Object> m1 = new LinkedHashMap<String, Object>();
					m1.put("name", name);
					m1.put("key", indexes.get(name));
					l1.add(m1);
				}

			} finally {
				close(r);
			}

			log.info("got index, table=" + tablename + ", result=" + l1);

			return l1;
		}

		@Override
		public String cursor(String table, String where, String orderby, long offset) {

			StringBuilder sql = new StringBuilder();

			sql.append("select * from " + table);

			if (!X.isEmpty(where)) {
				sql.append(" where " + where);
			}

			if (!X.isEmpty(orderby)) {
				sql.append(" ").append(orderby);
			}

			if (offset > 0) {
				sql.append(" offset ").append(offset);
			}

			return sql.toString();
		}

		@Override
		public JSON stats(Connection c, String dbname, String schema, String table) throws SQLException {
			return null;
		}

		@Override
		public String distributed(String table, String key) {
			// TODO Auto-generated method stub
			return null;
		}

		protected Object getdata(ResultSet r, String... name) {
			for (String s : name) {
				try {
					return r.getObject(s);
				} catch (Exception e) {
					// ignore
					log.error(e.getMessage(), e);
				}
			}
			return null;
		}

		@Override
		public String type(String type, int size) {

			if (X.isIn(type, "string", "varchar", "text", "char")) {
				if (size <= 0) {
					return "varchar(512)";
				} else if (size < 1024) {
					return "varchar(" + size + ")";
				}
				return "text";
			} else if (X.isIn(type, "long", "int")) {
				return "bigint";
			} else if (X.isIn(type, "double", "float")) {
				return "decimal(15, " + size + ")";
			} else if (X.isIn(type, "date", "time", "datetime", "timestamp")) {
				return "datetime";
			} else if (X.isIn(type, "file", "url", "image", "video", "audio")) {
				return "varchar(512)";
			} else if (X.isIn(type, "texts", "longs", "doubles", "images", "videos", "audios", "files")) {
				return "text";
			}

			return type;

		}

		@Override
		public void comment(Connection c, String dbname, String schema, String tablename, String memo, List<JSON> cols)
				throws SQLException {

		}

		protected String type(String type) {
			type = _typemapping.get(type);
			if (type == null) {
				type = "text";
			}
			return type;
		}

		private static Map<String, String> _typemapping = new HashMap<String, String>();

		static {
			_typemapping.put("NUMBER", "double");
			_typemapping.put("DECIMAL", "double");
			_typemapping.put("NUMERIC", "double");
			_typemapping.put("FLOAT", "double");
			_typemapping.put("DOUBLE", "double");
			_typemapping.put("REAL", "double");
			_typemapping.put("VARCHAR", "text");
			_typemapping.put("VARCHAR2", "text");
			_typemapping.put("TEXT", "text");
			_typemapping.put("LONGTEXT", "text");
			_typemapping.put("MEDIUMTEXT", "text");
			_typemapping.put("TINYTEXT", "text");
			_typemapping.put("BINARY", "text"); // TDengie
			_typemapping.put("INT UNSIGNED", "long");
			_typemapping.put("INT", "long");
			_typemapping.put("SHORT", "long");
			_typemapping.put("BLOB", "file");
			_typemapping.put("CLOB", "file");
			_typemapping.put("TIMESTAMP", "datetime");
			_typemapping.put("DATETIME", "datetime");
			_typemapping.put("DATE", "date");
			_typemapping.put("TIME", "time");
			_typemapping.put("BIT", "long");
			_typemapping.put("BIGINT", "long");
			_typemapping.put("INT64", "long");
			_typemapping.put("INT8", "long");
			_typemapping.put("INT4", "long");
		}

	}

	@Override
	public Object run(String sql) throws SQLException {

		/**
		 * search it in database
		 */
		Connection c = null;
		Statement stat = null;
		ResultSet r = null;

		TimeStamp t0 = TimeStamp.create();

		try {
			c = _getConnection();

			if (c == null) {
				throw new SQLException("get connection failed!");
			}

			stat = c.createStatement();

			r = stat.executeQuery(sql);
			List<JSON> l1 = JSON.createList();
			while (r.next()) {

				Bean b = new Bean();
				b.load(r);
				l1.add(b.json());
			}

			if (t0.pastms() > 30000) {
				log.warn("slow30, cost=" + t0.past());
			}

			return l1;
		} finally {
			close(r, stat, c);

		}

	}

	@Override
	public Connection getConnection() {
		try {
			return _getConnection();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void alterTable(String tablename, int partitions) throws SQLException {

		/**
		 * alter it in database
		 */
		Connection c = null;
		Statement stat = null;

		try {
			c = _getConnection();

			if (c == null)
				throw new SQLException("get connection failed!");

			/**
			 * create the sql statement
			 */
			String sql = driver.alterTable(dbname, schema, tablename, partitions);

			if (!X.isEmpty(sql)) {
				stat = c.createStatement();
				stat.execute(sql);
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			close(stat, c);
		}

	}

	@Override
	public int insertTable(String table, JSON v) throws SQLException {

		if (v == null || v.isEmpty()) {
			return 0;
		}

		long t1 = Global.now();
		if (v.get(X.CREATED) != V.ignore) {
			v.put(X.CREATED, t1);
		}
		if (v.get(X.UPDATED) != V.ignore) {
			v.put(X.UPDATED, t1);
		}

		/**
		 * insert it in database
		 */
		Connection c = null;
		TimeStamp t = TimeStamp.create();
		PreparedStatement p = null;
		StringBuilder sql = new StringBuilder();

		try {
			c = _getConnection();

			if (c == null)
				throw new SQLException("get connection failed!");

			String table1 = driver.fullname(dbname, schema, table);

			/**
			 * create the sql statement
			 */
			sql.append("insert into ").append(table1).append(" (");
			StringBuilder s = new StringBuilder();
			int total = 0;

			for (Map.Entry<String, Object> e : v.entrySet()) {
				if (e.getValue() == V.ignore) {
					continue;
				}
				if (s.length() > 0)
					s.append(",");
				s.append(e.getKey());
				total++;
			}
			sql.append(s).append(") values( ");

			for (int i = 0; i < total - 1; i++) {
				sql.append("?, ");
			}
			sql.append("?)");

			p = c.prepareStatement(sql.toString());

			int order = 1;
			for (Map.Entry<String, Object> e : v.entrySet()) {
				Object v1 = e.getValue();
				if (v1 == V.ignore) {
					continue;
				}
				_setParameter(p, order++, v1, c);
			}

			return p.executeUpdate();

		} catch (SQLException e) {
			if (log.isErrorEnabled()) {
				log.error(v.toString(), e);
			}
			throw e;
		} finally {
			close(c);

			Helper.Stat.write(table, t.pastms());

		}

	}

	@Override
	public int updateTable(String table, W q, JSON v) throws SQLException {

		if (v == null || v.isEmpty())
			return 0;

		Connection c = null;

		PreparedStatement p = null;

		try {
			c = _getConnection();

			if (c == null)
				throw new SQLException("get connection failed!");

			Object o1 = v.get(X.UPDATED);
			if (o1 != V.ignore) {
				v.put(X.UPDATED, Global.now());
			}
			v.remove(X.CREATED);

			/**
			 * update it in database
			 */
			StringBuilder sql = new StringBuilder();

			String table1 = driver.fullname(dbname, schema, table);

			/**
			 * create the sql statement
			 */
			sql.append("update ").append(table1).append(" set ");

			StringBuilder s = new StringBuilder();
			for (Map.Entry<String, Object> e : v.entrySet()) {
				if (e.getValue() == V.ignore) {
					continue;
				}

				if (s.length() > 0)
					s.append(",");
				s.append(e.getKey());
				s.append("=?");
			}
			sql.append(s);

			String where = _where(q, c);
			Object[] args = q.args();

			if (!X.isEmpty(where)) {
				sql.append(" where ").append(where);
			}

			p = c.prepareStatement(sql.toString());

			int order = 1;
			for (Map.Entry<String, Object> e : v.entrySet()) {
				Object v1 = e.getValue();
				if (v1 == V.ignore) {
					continue;
				}

				_setParameter(p, order++, v1, c);
			}

			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					Object o = args[i];
					_setParameter(p, order++, o, c);
				}
			}

			return p.executeUpdate();

		} finally {
			close(p, c);
		}

	}

}
