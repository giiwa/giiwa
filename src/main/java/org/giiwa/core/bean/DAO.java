/*
 * Copyright 2015 Giiwa, Inc. and/or its affiliates.
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.cache.DefaultCachable;

import net.sf.json.JSONObject;

// TODO: Auto-generated Javadoc
/**
 * The {@code Bean} Class is base class for all class that database access, it
 * almost includes all methods that need for database <br>
 * all data access MUST be inherited from it
 * 
 */
public abstract class DAO extends DefaultCachable implements Map<String, Object> {

  /** The Constant serialVersionUID. */
  private static final long      serialVersionUID = 2L;

  /** The log. */
  protected static Log           log              = LogFactory.getLog(DAO.class);
  protected static Log           sqllog           = LogFactory.getLog("sql");

  /** The conf. */
  protected static Configuration conf;

  /**
   * indicated whether is debug model
   */
  public static boolean          DEBUG            = true;

  /**
   * initialize the Bean with the configuration.
   * 
   * @param conf
   *          the conf
   */
  public static void init(Configuration conf) {
    DAO.conf = conf;

  }

  /**
   * update the data in db.
   * 
   * @param table
   *          the table name
   * @param sets
   *          the sets SQL sentence
   * @param where
   *          where conditions
   * @param whereArgs
   *          the where args.
   * @param db
   *          the db name
   * @return int
   */
  protected static int update(String table, String sets, String where, Object[] whereArgs, String db) {
    /**
     * create the sql statement
     */
    StringBuilder sql = new StringBuilder();
    sql.append("update ").append(table).append(" set ").append(sets);

    if (where != null) {
      sql.append(" where ").append(where);
    }

    /**
     * update it in database
     */
    Connection c = null;
    PreparedStatement p = null;
    ResultSet r = null;

    try {
      if (X.isEmpty(db)) {
        c = getConnection();
      } else {
        c = getConnection(db);
      }
      if (c == null)
        return -1;

      p = c.prepareStatement(sql.toString());

      int order = 1;
      if (whereArgs != null) {
        for (int i = 0; i < whereArgs.length; i++) {
          Object o = whereArgs[i];

          setParameter(p, order++, o);
        }
      }

      return p.executeUpdate();

    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(sql.toString() + toString(whereArgs), e);
    } finally {
      close(c, p, r);
    }

    return 0;
  }

  /**
   * Sets the parameter.
   * 
   * @param p
   *          the p
   * @param i
   *          the i
   * @param o
   *          the o
   * @throws SQLException
   *           the SQL exception
   */
  private static void setParameter(PreparedStatement p, int i, Object o) throws SQLException {
    if (o == null) {
      p.setObject(i, null);
    } else if (o instanceof Integer) {
      p.setInt(i, (Integer) o);
    } else if (o instanceof Date) {
      p.setTimestamp(i, new java.sql.Timestamp(((Date) o).getTime()));
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

  protected static int delete(String where, Object[] args, Class<? extends DAO> t) {
    /**
     * get the require annotation onGet
     */
    DBMapping mapping = (DBMapping) t.getAnnotation(DBMapping.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + t + "] declaretion");
      return -1;
    }

    return delete(mapping.table(), where, args, mapping.db());
  }

  /**
   * Delete the data in table.
   * 
   * @param table
   *          the table name
   * @param where
   *          the where conditions
   * @param whereArgs
   *          the where args.
   * @param db
   *          the db name
   * @return int
   */
  protected static int delete(String table, String where, Object[] whereArgs, String db) {
    /**
     * create the sql statement
     */
    StringBuilder sql = new StringBuilder();
    sql.append("delete from ").append(table);
    if (where != null) {
      sql.append(" where ").append(where);
    }

    /**
     * update it in database
     */
    Connection c = null;
    PreparedStatement p = null;

    try {
      if (X.isEmpty(db)) {
        c = getConnection();
      } else {
        c = getConnection(db);
      }
      if (c == null)
        return -1;

      p = c.prepareStatement(sql.toString());

      if (whereArgs != null) {
        int order = 1;

        for (int i = 0; i < whereArgs.length; i++) {
          Object o = whereArgs[i];

          setParameter(p, order++, o);

        }
      }

      return p.executeUpdate();

    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(sql.toString() + toString(whereArgs), e);
    } finally {
      close(p, c);
    }

    return 0;
  }

  /**
   * Gets the connection.
   * 
   * @return Connection
   * @throws SQLException
   *           the SQL exception
   */
  public static Connection getConnection() throws SQLException {
    try {
      long tid = Thread.currentThread().getId();
      if (outdoor.size() > 0) {
        Connection[] cs = null;
        synchronized (outdoor) {
          cs = outdoor.keySet().toArray(new Connection[outdoor.size()]);
        }

        for (Connection c : cs) {
          Long[] dd = outdoor.get(c);
          if (dd != null && dd[0] == tid) {
            dd[2]++;
            synchronized (outdoor) {
              outdoor.put(c, dd);
            }
            return c;
          }
        }
      }
      Connection c = org.giiwa.core.db.DB.getConnection();
      synchronized (outdoor) {
        outdoor.put(c, new Long[] { tid, System.currentTimeMillis(), 0L });
      }
      return c;
    } catch (SQLException e1) {
      /**
       * print the fuck who hold the connections;
       */
      if (log.isErrorEnabled()) {
        StringBuilder sb = new StringBuilder();
        sb.append("====================begin of thread dump=============================\r\n");
        sb.append("outdoor:" + outdoor.size());

        Map<Thread, StackTraceElement[]> m = Thread.getAllStackTraces();
        for (Iterator<Thread> it = m.keySet().iterator(); it.hasNext();) {
          Thread t = it.next();
          long tid = t.getId();
          Long[][] d0 = null;

          // TODO, there is big performance issue
          synchronized (outdoor) {
            d0 = outdoor.values().toArray(new Long[outdoor.size()][]);
          }

          for (Long[] dd : d0) {
            if (dd[0] == tid) {
              StackTraceElement[] st = m.get(t);
              sb.append(t.getName()).append(" - ").append(t.getState()).append(" - ")
                  .append((System.currentTimeMillis() - dd[1]) / 1000).append("ms/").append(dd[2]).append(t.toString())
                  .append("\r\n");
              for (StackTraceElement e : st) {
                sb.append("\t").append(e.getClassName()).append(".").append(e.getMethodName()).append("(")
                    .append(e.getLineNumber()).append(")").append("\r\n");
              }
              break;
            }
          }

        }
        sb.append("====================end of thread dump=============================");

        log.error(sb.toString());
      }
      throw e1;
    }
  }

  private static Map<Connection, Long[]> outdoor = new HashMap<Connection, Long[]>();

  /**
   * Gets the SQL connection by name.
   *
   * @param name
   *          the name of the connection pool
   * @return the connection
   * @throws SQLException
   *           the SQL exception
   */
  public static Connection getConnection(String name) throws SQLException {
    return org.giiwa.core.db.DB.getConnection(name);
  }

  /**
   * Close the objects, the object cloud be ResultSet, Statement,
   * PreparedStatement, Connection <br>
   * if the connection was required twice in same thread, then the reference
   * "-1", if "=0", then close it.
   *
   * @param objs
   *          the objects of "ResultSet, Statement, Connection"
   */
  final public static void close(Object... objs) {
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
          Long[] dd = outdoor.get(c);
          if (dd == null || dd[2] <= 0) {
            try {
              if (!c.getAutoCommit()) {
                c.commit();
              }
            } catch (Exception e1) {
            } finally {
              c.close();
            }
            synchronized (outdoor) {
              outdoor.remove(c);
            }
          } else {
            dd[2]--;
            synchronized (outdoor) {
              outdoor.put(c, dd);
            }
          }
        }
      } catch (SQLException e) {
        if (log.isErrorEnabled())
          log.error(e.getMessage(), e);
      }
    }
  }

  /**
   * parse a object to a integer.
   * 
   * @deprecated please refer X.toInt()
   * @param v
   *          the v
   * @param defaultValue
   *          the default value
   * @return the int
   */
  public static int toInt(Object v, int defaultValue) {
    return X.toInt(v, defaultValue);
  }

  /**
   * parse a object to float, the default value is "0".
   * 
   * @deprecated please refer X.toFloat()
   * @param v
   *          the v
   * @return the float
   */
  public static float toFloat(Object v) {
    return toFloat(v, 0);
  }

  /**
   * parse a object to a float with defaultvalue.
   * 
   * @deprecated please refer X.toFloat()
   * @param v
   *          the v
   * @param defaultValue
   *          the default value
   * @return the float
   */
  public static float toFloat(Object v, float defaultValue) {
    return X.toFloat(v, defaultValue);
  }

  /**
   * To double.
   * 
   * @deprecated please refer X.toDouble()
   * @param v
   *          the v
   * @return the double
   */
  public static double toDouble(Object v) {
    return X.toDouble(v, -1);
  }

  final protected int insertOrUpdate(String where, Object[] args, V sets) {
    /**
     * get the require annotation onGet
     */
    DBMapping mapping = (DBMapping) this.getClass().getAnnotation(DBMapping.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + this.getClass() + "] declaretion");
      return -1;
    }

    return insertOrUpdate(mapping.table(), where, args, sets, mapping.db());
  }

  /**
   * Insert or update.
   * 
   * @param table
   *          the table name
   * @param where
   *          the where conditions
   * @param args
   *          the where args
   * @param sets
   *          the values
   * @param db
   *          the db name
   * @return int
   */
  protected final static int insertOrUpdate(String table, String where, Object[] args, V sets, String db) {
    int i = 0;
    if (exists(table, where, args, db)) {
      i = update(table, where, args, sets, db);
    } else {
      i = insert(table, sets, db);
    }

    return i;
  }

  /**
   * test the data exists.
   * 
   * @param where
   *          the where conditions
   * @param args
   *          the where args
   * @param t
   *          the Bean class
   * @return boolean
   */
  protected static boolean exists(String where, Object[] args, Class<? extends DAO> t) {
    /**
     * get the require annotation onGet
     */
    DBMapping mapping = (DBMapping) t.getAnnotation(DBMapping.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + t + "] declaretion");
      return false;
    }

    return exists(mapping.table(), where, args, mapping.db());

  }

  /**
   * Exists.
   * 
   * @param table
   *          the table
   * @param where
   *          the where
   * @param args
   *          the args
   * @return true, if successful
   */
  /**
   * test exists.
   * 
   * @param table
   *          the table name
   * @param where
   *          the where conditions.
   * @param args
   *          the where args
   * @param db
   *          the db name
   * @return boolean
   */
  protected static boolean exists(String table, String where, Object[] args, String db) {
    /**
     * create the sql statement
     */
    TimeStamp t = TimeStamp.create();

    StringBuilder sql = new StringBuilder();
    sql.append("select 1 from ").append(table);

    if (where != null) {
      sql.append(" where ").append(where);
    }
    sql.append(" limit 1");

    /**
     * search it in database
     */
    Connection c = null;
    PreparedStatement p = null;
    ResultSet r = null;

    try {
      if (X.isEmpty(db)) {
        c = getConnection();
      } else {
        c = getConnection(db);
      }
      if (c == null)
        return false;

      p = c.prepareStatement(sql.toString());

      int order = 1;
      if (args != null) {
        for (int i = 0; i < args.length; i++) {
          Object o = args[i];

          setParameter(p, order++, o);
        }
      }

      r = p.executeQuery();
      return r.next();

    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(sql.toString() + toString(args), e);
    } finally {
      close(r, p, c);

      if (t.past() > 2 && sqllog.isDebugEnabled()) {
        sqllog.debug("cost:" + t.past() + "ms, sql=[" + sql + "]");
      }
    }
    return false;
  }

  /**
   * update the data using values
   * 
   * @param where
   *          the where conditions.
   * @param args
   *          the where args
   * @param sets
   *          the values
   * @param t
   *          the Bean class
   * @return int
   */
  protected static int update(String where, Object[] args, V sets, Class<? extends DAO> t) {
    DBMapping mapping = (DBMapping) t.getAnnotation(DBMapping.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + t + "] declaretion");
      return -1;
    }

    if (!X.isEmpty(mapping.table())) {
      return update(mapping.table(), where, args, sets, mapping.db());
    }

    return -1;
  }

  /**
   * Update the data.
   * 
   * @param table
   *          the table name
   * @param where
   *          the where condition
   * @param whereArgs
   *          the where args
   * @param sets
   *          the values
   * @param db
   *          the db name
   * @return int
   */
  protected static int update(String table, String where, Object[] whereArgs, V sets, String db) {
    /**
     * create the sql statement
     */
    StringBuilder sql = new StringBuilder();
    sql.append("update ").append(table).append(" set ");

    StringBuilder s = new StringBuilder();
    for (String name : sets.names()) {
      if (s.length() > 0)
        s.append(",");
      s.append(name).append("=?");
    }
    sql.append(s);

    if (where != null) {
      sql.append(" where ").append(where);
    }

    /**
     * update it in database
     */
    Connection c = null;
    PreparedStatement p = null;

    int updated = 0;
    try {

      if (X.isEmpty(db)) {
        c = getConnection();
      } else {
        c = getConnection(db);
      }
      if (c == null)
        return -1;

      p = c.prepareStatement(sql.toString());

      int order = 1;
      for (Object v : sets.values()) {
        setParameter(p, order++, v);
      }

      if (whereArgs != null) {
        for (int i = 0; i < whereArgs.length; i++) {
          Object o = whereArgs[i];

          setParameter(p, order++, o);
        }
      }

      updated = p.executeUpdate();

    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(sql.toString() + toString(whereArgs) + sets.toString(), e);
    } finally {
      close(p, c);
    }

    return updated;

  }

  /**
   * Update the data using the sql sentence.
   * 
   * @deprecated
   * @param sql
   *          the sql sentence
   * @param args
   *          the args
   * @param db
   *          the db name
   * @return int
   */
  protected static int update(String sql, Object[] args, String db) {
    /**
     * /** update it in database
     */
    Connection c = null;
    PreparedStatement p = null;

    try {
      if (X.isEmpty(db)) {
        c = getConnection();
      } else {
        c = getConnection(db);
      }
      if (c == null)
        return -1;

      p = c.prepareStatement(sql);

      int order = 1;
      if (args != null) {
        for (int i = 0; i < args.length; i++) {
          Object o = args[i];

          setParameter(p, order++, o);
        }
      }

      return p.executeUpdate();

    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(sql + toString(args), e);
    } finally {
      close(p, c);
    }

    return 0;

  }

  /**
   * Load.
   * 
   * @param <T>
   *          the generic type
   * @param table
   *          the table
   * @param where
   *          the where
   * @param args
   *          the args
   * @param clazz
   *          the clazz
   * @return the t
   */
  protected static <T extends DAO> T load(String table, String where, Object[] args, Class<T> clazz) {
    return load(table, where, args, null, clazz, null);

  }

  protected static <T extends DAO> T load(String where, Object[] args, Class<T> t) {
    /**
     * get the require annotation onGet
     */
    DBMapping mapping = (DBMapping) t.getAnnotation(DBMapping.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + t + "] declaretion");
      return null;
    }

    return load(mapping.table(), where, args, null, t, mapping.db());

  }

  protected static boolean load(String where, Object[] args, DAO b) {
    return load(where, args, null, b);
  }

  protected static boolean load(String table, String where, Object[] args, DAO b) {
    return load(table, where, args, null, b, null);
  }

  protected static <T extends DAO> T load(String where, Object[] args, String orderby, Class<T> t) {
    /**
     * get the require annotation onGet
     */
    DBMapping mapping = (DBMapping) t.getAnnotation(DBMapping.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + t + "] declaretion");
      return null;
    }

    return load(mapping.table(), where, args, orderby, t, mapping.db());
  }

  /**
   * load the data in this Bean
   * 
   * @param where
   *          the where conditions
   * @param args
   *          the where args.
   * @param orderby
   *          the order sentence.
   * @param b
   *          the Bean
   * @return boolean
   */
  protected static boolean load(String where, Object[] args, String orderby, DAO b) {
    /**
     * get the require annotation onGet
     */
    DBMapping mapping = (DBMapping) b.getClass().getAnnotation(DBMapping.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + b.getClass() + "] declaretion");
      return false;
    }

    return load(mapping.table(), where, args, orderby, b, mapping.db());
  }

  /**
   * Load the data, the data will be load(ResultSet r) method.
   * 
   * @param table
   *          the table name
   * @param where
   *          the where conditions
   * @param args
   *          the where args
   * @param orderby
   *          the order sentence
   * @param b
   *          the Bean
   * @param db
   *          the db name
   * @return boolean
   */
  protected static boolean load(String table, String where, Object[] args, String orderby, DAO b, String db) {
    /**
     * create the sql statement
     */
    TimeStamp t = TimeStamp.create();

    StringBuilder sql = new StringBuilder();
    sql.append("select * from ").append(table);

    if (where != null) {
      sql.append(" where ").append(where);
    }
    if (orderby != null) {
      sql.append(" ").append(orderby).append(" ");
    }
    sql.append(" limit 1");

    /**
     * search it in database
     */
    Connection c = null;
    PreparedStatement p = null;
    ResultSet r = null;

    try {
      if (X.isEmpty(db)) {
        c = getConnection();
      } else {
        c = getConnection(db);
      }
      if (c == null)
        return false;

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
      if (log.isErrorEnabled())
        log.error(sql + toString(args), e);
    } finally {
      close(r, p, c);

      if (t.past() > 2 && sqllog.isDebugEnabled()) {
        sqllog.debug("cost: " + t.past() + "ms, sql=[" + sql + "]");
      }
    }

    return false;
  }

  /**
   * Load data by default, get all fields and set in map.<br>
   * it will be invoked when load data from RDBS DB <br>
   * By default, it will load all data in Bean Map.
   * 
   * @param r
   *          the ResultSet of RDBS
   * @throws SQLException
   *           the SQL exception
   */
  protected void load(ResultSet r) throws SQLException {
    ResultSetMetaData m = r.getMetaData();
    int cols = m.getColumnCount();
    for (int i = 1; i <= cols; i++) {
      Object o = r.getObject(i);
      if (o instanceof java.sql.Date) {
        o = ((java.sql.Date) o).toString();
      } else if (o instanceof java.sql.Time) {
        o = ((java.sql.Time) o).toString();
      } else if (o instanceof java.sql.Timestamp) {
        o = ((java.sql.Timestamp) o).toString();
      } else if (o instanceof java.math.BigDecimal) {
        o = o.toString();
      }
      this.set(m.getColumnName(i), o);
    }
  }


  /**
   * Load the data from the RDBMS table, by the where and
   * 
   * @param <T>
   *          the generic type
   * @param table
   *          the table
   * @param cols
   *          the cols
   * @param where
   *          the where
   * @param args
   *          the args
   * @param clazz
   *          the Class Bean
   * @return the list
   */
  protected final static <T extends DAO> List<T> load(String table, String[] cols, String where, Object[] args,
      Class<T> clazz) {
    return load(table, cols, where, args, null, -1, -1, clazz, null);
  }

  /**
   * load the data from RDBMS table which associated with Bean.
   * 
   * @param <T>
   *          the generic Bean Class
   * @param cols
   *          the column name array
   * @param where
   *          the where conditions
   * @param args
   *          the where args
   * @param clazz
   *          the Bean Class
   * @return List
   */
  protected final static <T extends DAO> List<T> load(String[] cols, String where, Object[] args, Class<T> clazz) {
    return load(cols, where, args, null, -1, -1, clazz);
  }

  /**
   * load the list of beans, by the where
   * 
   * @param <T>
   *          the generic Bean Class
   * @param cols
   *          the column name array
   * @param where
   *          the where conditions
   * @param args
   *          the where args
   * @param orderby
   *          the order by sentence
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @param t
   *          the Bean Class
   * @return List
   */
  protected final static <T extends DAO> List<T> load(String[] cols, String where, Object[] args, String orderby,
      int offset, int limit, Class<T> t) {
    /**
     * get the require annotation onGet
     */
    DBMapping mapping = (DBMapping) t.getAnnotation(DBMapping.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + t + "] declaretion");
      return null;
    }

    return load(mapping.table(), cols, where, args, orderby, offset, limit, t, mapping.db());
  }

  /**
   * Load the list data from the RDBMS table
   * 
   * @param <T>
   *          the generic type
   * @param table
   *          the table name
   * @param cols
   *          the column name array
   * @param where
   *          the where conditions
   * @param args
   *          the where args
   * @param orderby
   *          the order by sentence
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @param clazz
   *          the Bean Class
   * @param db
   *          the db name
   * @return List
   */
  protected static <T extends DAO> List<T> load(String table, String[] cols, String where, Object[] args,
      String orderby, int offset, int limit, Class<T> clazz, String db) {
    /**
     * create the sql statement
     */
    TimeStamp t = TimeStamp.create();

    StringBuilder sql = new StringBuilder();
    sql.append("select ");
    if (cols != null) {
      for (int i = 0; i < cols.length - 1; i++) {
        sql.append(cols[i]).append(", ");
      }

      sql.append(cols[cols.length - 1]);

    } else {
      sql.append("*");
    }

    sql.append(" from ").append(table);
    if (where != null) {
      sql.append(" where ").append(where);
    }

    if (orderby != null) {
      sql.append(" ").append(orderby);
    }

    // TODO, oracle not support limit, using rownum
    if (limit > 0) {
      sql.append(" limit ").append(limit);
    }

    if (offset > 0) {
      sql.append(" offset ").append(offset);
    }
    // }

    // log.debug("sql:" + sql.toString());

    /**
     * search it in database
     */
    Connection c = null;
    PreparedStatement p = null;
    ResultSet r = null;

    try {
      if (X.isEmpty(db)) {
        c = getConnection();
      } else {
        c = getConnection(db);
      }
      if (c == null)
        return null;

      p = c.prepareStatement(sql.toString());

      int order = 1;
      if (args != null) {
        for (int i = 0; i < args.length; i++) {
          Object o = args[i];

          setParameter(p, order++, o);
        }
      }

      r = p.executeQuery();
      List<T> list = new ArrayList<T>();
      while (r.next()) {
        T b = clazz.newInstance();
        b.load(r);
        list.add(b);
      }

      return list;
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(sql.toString() + toString(args), e);
    } finally {
      close(r, p, c);

      if (t.past() > 2 && sqllog.isDebugEnabled()) {
        sqllog.debug("cost:" + t.past() + "ms, sql=[" + sql + "]");
      }
    }
    return null;
  }

  /**
   * load the list data from the RDBMS table that associated with the Bean.
   * 
   * @param <T>
   *          the generic Bean Class
   * @param where
   *          the where conditions
   * @param args
   *          the where args
   * @param orderby
   *          the order by sentence
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @param t
   *          the Bean Class
   * @return Beans
   */
  protected static <T extends DAO> Beans<T> load(String where, Object[] args, String orderby, int offset, int limit,
      Class<T> t) {
    /**
     * get the require annotation onGet
     */
    DBMapping mapping = (DBMapping) t.getAnnotation(DBMapping.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + t + "] declaretion");
      return null;
    }

    return load(mapping.table(), where, args, orderby, offset, limit, t, mapping.db());
  }

  /**
   * Load the list data from the RDBMS table
   * 
   * @param <T>
   *          the generic Bean Class
   * @param table
   *          the table name
   * @param where
   *          the where conditions
   * @param args
   *          the where args
   * @param orderby
   *          the order by sentence
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @param clazz
   *          the Bean Class
   * @param db
   *          the db name
   * @return Beans
   */
  protected static <T extends DAO> Beans<T> load(String table, String where, Object[] args, String orderby, int offset,
      int limit, Class<T> clazz, String db) {
    /**
     * create the sql statement
     */
    TimeStamp t = TimeStamp.create();

    StringBuilder sql = new StringBuilder();
    StringBuilder sum = new StringBuilder();
    sql.append("select * from ").append(table);
    sum.append("select count(*) t from ").append(table);
    if (where != null) {
      sql.append(" where ").append(where);
      sum.append(" where ").append(where);
    }

    if (orderby != null) {
      sql.append(" ").append(orderby);
    }

    if (limit > 0) {
      sql.append(" limit ").append(limit);
    }

    if (offset > 0) {
      sql.append(" offset ").append(offset);
    }

    // log.debug("sql:" + sql.toString());

    /**
     * search it in database
     */
    Connection c = null;
    PreparedStatement p = null;
    ResultSet r = null;

    try {
      if (X.isEmpty(db)) {
        c = getConnection();
      } else {
        c = getConnection(db);
      }
      if (c == null)
        return null;

      p = c.prepareStatement(sum.toString());

      int order = 1;
      if (args != null) {
        for (int i = 0; i < args.length; i++) {
          Object o = args[i];

          setParameter(p, order++, o);
        }
      }

      r = p.executeQuery();
      Beans<T> rs = new Beans<T>();
      if (r.next()) {
        rs.total = r.getInt("t");
      }
      r.close();
      r = null;
      p.close();
      p = null;

      if (rs.total > 0) {
        p = c.prepareStatement(sql.toString());

        order = 1;
        if (args != null) {
          for (int i = 0; i < args.length; i++) {
            Object o = args[i];

            setParameter(p, order++, o);
          }
        }

        r = p.executeQuery();
        rs.list = new ArrayList<T>();
        while (r.next()) {
          T b = clazz.newInstance();
          b.load(r);
          rs.list.add(b);
        }
      }

      if (log.isDebugEnabled())
        log.debug("load - cost=" + t.past() + "ms, collection=" + table + ", sql=" + sql + ", result=" + rs);

      if (t.past() > 10000) {
        log.warn("load - cost=" + t.past() + "ms, collection=" + table + ", sql=" + sql + ", result=" + rs);
      }

      return rs;
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(sql.toString() + toString(args), e);

    } finally {
      close(r, p, c);

      if (t.past() > 2 && sqllog.isDebugEnabled()) {
        sqllog.debug("cost:" + t.past() + "ms, sql=[" + sql + "]; [" + sum + "]");
      }
    }
    return null;
  }

  /**
   * get the data.
   * 
   * @param <T>
   *          the generic Bean Class
   * @param table
   *          the table name
   * @param where
   *          the where conditions.
   * @param args
   *          the where args.
   * @param orderby
   *          the order by sentence
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @param clazz
   *          the Bean Class
   * @param c
   *          the connection
   * @return Beans
   */
  protected static <T extends DAO> Beans<T> load(String table, String where, Object[] args, String orderby, int offset,
      int limit, Class<T> clazz, Connection c) {
    /**
     * create the sql statement
     */
    TimeStamp t = TimeStamp.create();

    StringBuilder sql = new StringBuilder();
    StringBuilder sum = new StringBuilder();
    sql.append("select * from ").append(table);
    sum.append("select count(*) t from ").append(table);
    if (where != null) {
      sql.append(" where ").append(where);
      sum.append(" where ").append(where);
    }

    if (orderby != null) {
      sql.append(" ").append(orderby);
    }

    if (limit > 0) {
      sql.append(" limit ").append(limit);
    }

    if (offset > 0) {
      sql.append(" offset ").append(offset);
    }

    // log.debug("sql:" + sql.toString());

    /**
     * search it in database
     */
    PreparedStatement p = null;
    ResultSet r = null;

    try {
      if (c == null)
        return null;

      p = c.prepareStatement(sum.toString());

      int order = 1;
      if (args != null) {
        for (int i = 0; i < args.length; i++) {
          Object o = args[i];

          setParameter(p, order++, o);
        }
      }

      r = p.executeQuery();
      Beans<T> rs = new Beans<T>();
      if (r.next()) {
        rs.total = r.getInt("t");
      }
      r.close();
      r = null;
      p.close();
      p = null;

      if (rs.total > 0) {
        p = c.prepareStatement(sql.toString());

        order = 1;
        if (args != null) {
          for (int i = 0; i < args.length; i++) {
            Object o = args[i];

            setParameter(p, order++, o);
          }
        }

        r = p.executeQuery();
        rs.list = new ArrayList<T>();
        while (r.next()) {
          T b = clazz.newInstance();
          b.load(r);
          rs.list.add(b);
        }
      }

      if (log.isDebugEnabled())
        log.debug("load - cost=" + t.past() + "ms, collection=" + table + ", sql=" + sql + ", result=" + rs);

      if (t.past() > 10000) {
        log.warn("load - cost=" + t.past() + "ms, collection=" + table + ", sql=" + sql + ", result=" + rs);
      }

      return rs;
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(sql.toString() + toString(args), e);

    } finally {
      close(r, p);

      if (t.past() > 2 && sqllog.isDebugEnabled()) {
        sqllog.debug("cost:" + t.past() + "ms, sql=[" + sql + "]; [" + sum + "]");
      }
    }
    return null;
  }

  /**
   * according the Mapping(table, collection) declaration to insert in table or
   * collection.
   * 
   * @param sets
   *          the values
   * @param t
   *          the Bean Class
   * @return int
   */
  final protected static int insert(V sets, Class<?> t) {

    DBMapping mapping = (DBMapping) t.getAnnotation(DBMapping.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + t + "] declaretion");
      return -1;
    }

    if (!X.isEmpty(mapping.table())) {
      return insert(mapping.table(), sets, mapping.db());
    }
    return -1;
  }

  /**
   * batch insert.
   * 
   * @param sets
   *          the values collection
   * @param t
   *          the Bean Class
   * @return int
   */
  final protected static int insert(Collection<V> sets, Class<?> t) {
    /**
     * get the require annotation onGet
     */
    DBMapping mapping = (DBMapping) t.getAnnotation(DBMapping.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + t + "] declaretion");
      return -1;
    }

    return insert(mapping.table(), sets, mapping.db());

  }

  /**
   * insert to the table according the Map(table) declaration
   * 
   * @param sets
   *          the values
   * @param t
   *          the Bean class
   * @return int
   */
  final protected static int insertTable(V sets, Class<?> t) {
    DBMapping mapping = (DBMapping) t.getAnnotation(DBMapping.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + t + "] declaretion");
      return -1;
    }

    if (!X.isEmpty(mapping.table())) {
      return insert(mapping.table(), sets, mapping.db());
    }
    return -1;
  }

  /**
   * batch insert the values into the table.
   * 
   * @param table
   *          the table name
   * @param list
   *          the list of values
   * @param db
   *          the db name
   * @return int of how many data inserted
   */
  protected static int insert(String table, Collection<V> list, String db) {
    if (list == null || list.size() == 0)
      return 0;

    /**
     * create the sql statement
     */
    StringBuilder sql = new StringBuilder();
    sql.append("insert into ").append(table).append(" (");
    StringBuilder s = new StringBuilder();
    int total = 0;
    V ss = list.iterator().next();
    for (String name : ss.names()) {
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

    /**
     * insert it in database
     */
    Connection c = null;
    PreparedStatement p = null;

    try {
      if (X.isEmpty(db)) {
        c = getConnection();
      } else {
        c = getConnection(db);
      }
      if (c == null)
        return -1;

      p = c.prepareStatement(sql.toString());

      for (V sets : list) {
        int order = 1;
        for (Object v : sets.m.values()) {
          setParameter(p, order++, v);
        }

        p.addBatch();
      }

      int[] ii = p.executeBatch();
      int r = 0;
      for (int i : ii) {
        r += i;
      }
      return r;
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(sql.toString() + list.toString(), e);
    } finally {
      close(p, c);
    }
    return 0;
  }

  /**
   * Insert values into the table.
   * 
   * @param table
   *          the table name
   * @param sets
   *          the values
   * @param db
   *          the db name
   * @return int
   */
  private static int insert(String table, V sets, String db) {
    /**
     * create the sql statement
     */
    StringBuilder sql = new StringBuilder();
    sql.append("insert into ").append(table).append(" (");
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

    /**
     * insert it in database
     */
    Connection c = null;
    PreparedStatement p = null;

    try {
      if (X.isEmpty(db)) {
        c = getConnection();
      } else {
        c = getConnection(db);
      }
      if (c == null)
        return -1;

      p = c.prepareStatement(sql.toString());

      int order = 1;
      for (Object v : sets.values()) {
        setParameter(p, order++, v);
      }

      return p.executeUpdate();

    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(sql.toString() + sets.toString(), e);
    } finally {
      close(p, c);
    }
    return 0;
  }

  /**
   * Values in SQL, used to insert or update data both RDBS and Mongo<br>
   * 
   */
  public static final class V {

    /** The list. */
    private Map<String, Object> m = new HashMap<String, Object>();

    /**
     * get the names.
     *
     * @return Collection
     */
    public Set<String> names() {
      return m.keySet();
    }

    /**
     * get the values.
     *
     * @return Collection
     */
    public Collection<Object> values() {
      return m.values();
    }

    /**
     * get the size of the values.
     *
     * @return the int
     */
    public int size() {
      return m.size();
    }

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
     *          the name
     * @param v
     *          the value
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
     *          the name
     * @param v
     *          the value
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
     * set the value, if exists and force, then replace it.
     *
     * @param name
     *          the name
     * @param v
     *          the value
     * @param force
     *          if true, then replace the old one
     * @return V
     */
    public V set(String name, Object v, boolean force) {
      if (name != null && v != null) {
        m.put(name, v);
      }
      return this;
    }

    /**
     * copy all key-value in json to this.
     *
     * @param jo
     *          the json
     * @return V
     */
    public V copy(Map<String, Object> jo) {
      if (jo == null)
        return this;

      for (String s : jo.keySet()) {
        if (jo.containsKey(s)) {
          Object o = jo.get(s);
          if (X.isEmpty(o)) {
            set(s, X.EMPTY);
          } else {
            set(s, o);
          }
        }
      }

      return this;
    }

    /**
     * copy all in json to this, if names is null, then nothing to copy.
     *
     * @param jo
     *          the json
     * @param names
     *          the name string
     * @return V
     */
    public V copy(Map<String, Object> jo, String... names) {
      if (jo == null || names == null)
        return this;

      for (String s : names) {
        if (jo.containsKey(s)) {
          Object o = jo.get(s);
          if (X.isEmpty(o)) {
            set(s, X.EMPTY);
          } else {
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
     *          the original V
     * @param names
     *          the names to copy, if null, then copy all
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
     *          the string of name
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
     * copt integer data to V from json, it the names is null, then copy
     * nothing.
     *
     * @param jo
     *          the json
     * @param names
     *          the names to copy
     * @return V
     */
    public V copyInt(Map<String, Object> jo, String... names) {
      if (jo == null || names == null)
        return this;

      for (String s : names) {
        if (jo.containsKey(s)) {
          set(s, DAO.toInt(jo.get(s)));
        }
      }

      return this;
    }

    /**
     * copy long data from the map to self.
     *
     * @param jo
     *          the map
     * @param names
     *          the names to copy, if null, copy nothing
     * @return V
     */
    public V copyLong(Map<String, Object> jo, String... names) {
      if (jo == null || names == null)
        return this;

      for (String s : names) {
        if (jo.containsKey(s)) {
          set(s, DAO.toLong(jo.get(s)));
        }
      }

      return this;
    }

    /**
     * Removes the.
     *
     * @param name
     *          the name
     * @return the v
     */
    public V remove(String name) {
      m.remove(name);
      return this;
    }

  }

  /**
   * Load list result, string, integer, or base data type
   * 
   * @deprecated
   * @param <T>
   *          the generic Bean Class
   * @param table
   *          the table name
   * @param col
   *          the column name
   * @param where
   *          the where conditions
   * @param args
   *          the where args
   * @param clazz
   *          the Bean Class
   * @param db
   *          the db name
   * @return List
   */
  protected static <T> List<T> loadList(String table, String col, String where, Object[] args, Class<T> clazz,
      String db) {
    /**
     * create the sql statement
     */
    TimeStamp t = TimeStamp.create();

    StringBuilder sql = new StringBuilder();
    sql.append("select ").append(col).append(" from ").append(table);

    if (where != null) {
      sql.append(" where ").append(where);
    }

    /**
     * search it in database
     */
    Connection c = null;
    PreparedStatement p = null;
    ResultSet r = null;

    try {
      if (X.isEmpty(db)) {
        c = getConnection();
      } else {
        c = getConnection(db);
      }
      if (c == null)
        return null;

      p = c.prepareStatement(sql.toString());

      int order = 1;
      if (args != null) {
        for (int i = 0; i < args.length; i++) {
          Object o = args[i];

          setParameter(p, order++, o);
        }
      }

      List<T> list = new ArrayList<T>();
      r = p.executeQuery();
      while (r.next()) {
        T b = (T) (r.getObject(1));
        list.add(b);
      }
      return list;
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(sql.toString() + toString(args), e);
    } finally {
      close(r, p, c);

      if (t.past() > 2 && sqllog.isDebugEnabled()) {
        sqllog.debug("cost:" + t.past() + "ms, sql=[" + sql + "]");
      }
    }
    return null;
  }

  /**
   * get a string value from a col from the table.
   * 
   * @param table
   *          the table name
   * @param col
   *          the column name
   * @param where
   *          the where condition
   * @param args
   *          the where args
   * @param db
   *          the db name
   * @return String
   */
  protected static String getString(String table, String col, String where, Object[] args, String db) {
    /**
     * create the sql statement
     */
    StringBuilder sql = new StringBuilder();
    sql.append("select ").append(col).append(" from ").append(table);

    if (where != null) {
      sql.append(" where ").append(where);
    }
    sql.append(" limit 1");

    /**
     * search it in database
     */
    Connection c = null;
    PreparedStatement p = null;
    ResultSet r = null;

    try {
      if (X.isEmpty(db)) {
        c = getConnection();
      } else {
        c = getConnection(db);
      }
      if (c == null)
        return null;

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
      if (log.isErrorEnabled())
        log.error(sql.toString() + toString(args), e);

    } finally {
      close(r, p, c);
    }

    return null;
  }

  /**
   * get the list of the column
   * 
   * @deprecated
   * @param <T>
   *          the generic Bean Class
   * @param col
   *          the column name
   * @param where
   *          the where condition
   * @param args
   *          the where args
   * @param orderby
   *          the order by sentence
   * @param s
   *          the offset
   * @param n
   *          the limit
   * @param t
   *          the Bean class
   * @return List
   */
  protected final static <T> List<T> getList(String col, String where, Object[] args, String orderby, int s, int n,
      Class<? extends DAO> t) {
    /**
     * get the require annotation onGet
     */
    DBMapping mapping = (DBMapping) t.getAnnotation(DBMapping.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + t + "] declaretion");
      return null;
    }

    return getList(mapping.table(), col, where, args, orderby, s, n, mapping.db());
  }

  /**
   * get one field.
   * 
   * @param <T>
   *          the generic Bean Class
   * @param col
   *          the column name
   * @param where
   *          the where conditions
   * @param args
   *          the where args
   * @param orderby
   *          the order by sentence
   * @param position
   *          the offset
   * @param t
   *          the Bean class
   * @return T
   */
  protected final static <T> T getOne(String col, String where, Object[] args, String orderby, int position,
      Class<? extends DAO> t) {
    /**
     * get the require annotation onGet
     */
    DBMapping mapping = (DBMapping) t.getAnnotation(DBMapping.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + t + "] declaretion");
      return null;
    }

    return getOne(mapping.table(), col, where, args, orderby, position, mapping.db());
  }

  /**
   * getOne by the query.
   * 
   * @param <T>
   *          the generic Bean Class
   * @param table
   *          the table name
   * @param col
   *          the column anme
   * @param where
   *          the where conditions
   * @param args
   *          the where args
   * @param orderby
   *          the order sentence
   * @param position
   *          the offset
   * @param db
   *          the db name
   * @return T
   */
  protected static <T> T getOne(String table, String col, String where, Object[] args, String orderby, int position,
      String db) {

    /**
     * create the sql statement
     */
    StringBuilder sql = new StringBuilder();
    sql.append("select ").append(col).append(" from ").append(table);

    if (where != null) {
      sql.append(" where ").append(where);
    }
    if (orderby != null) {
      sql.append(" ").append(orderby);
    }
    sql.append(" limit 1");
    if (position > 0) {
      sql.append(" offset ").append(position);
    }

    /**
     * search it in database
     */
    Connection c = null;
    PreparedStatement p = null;
    ResultSet r = null;

    try {
      if (X.isEmpty(db)) {
        c = getConnection();
      } else {
        c = getConnection(db);
      }
      if (c == null)
        return null;

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
      if (log.isErrorEnabled())
        log.error(sql.toString() + toString(args), e);

    } finally {
      close(r, p, c);
    }

    return null;
  }

  /**
   * get the list of the col.
   * 
   * @deprecated
   * @param <T>
   *          the generic Bean Class
   * @param table
   *          the table name
   * @param col
   *          the column name
   * @param where
   *          the where conditions
   * @param args
   *          the where args
   * @param orderby
   *          the order sentence
   * @param s
   *          the offset
   * @param n
   *          the limit
   * @param db
   *          the db name
   * @return List
   */
  @SuppressWarnings("unchecked")
  protected final static <T> List<T> getList(String table, String col, String where, Object[] args, String orderby,
      int s, int n, String db) {

    /**
     * create the sql statement
     */
    StringBuilder sql = new StringBuilder();
    sql.append("select ").append(col).append(" from ").append(table);

    if (where != null) {
      sql.append(" where ").append(where);
    }
    if (orderby != null) {
      sql.append(" ").append(orderby);
    }
    sql.append(" limit ").append(n);
    if (s > 0) {
      sql.append(" offset ").append(s);
    }

    /**
     * search it in database
     */
    Connection c = null;
    PreparedStatement p = null;
    ResultSet r = null;

    try {
      if (X.isEmpty(db)) {
        c = getConnection();
      } else {
        c = getConnection(db);
      }
      if (c == null)
        return null;

      p = c.prepareStatement(sql.toString());

      int order = 1;
      if (args != null) {
        for (int i = 0; i < args.length; i++) {
          Object o = args[i];

          setParameter(p, order++, o);
        }
      }

      r = p.executeQuery();
      List<T> list = new ArrayList<T>();
      while (r.next()) {
        list.add((T) r.getObject(1));
      }
      return list;

    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(sql.toString() + toString(args), e);

    } finally {
      close(r, p, c);
    }

    return null;
  }

  /**
   * refill the bean from json.
   *
   * @param jo
   *          the map
   * @return boolean
   */
  public boolean fromJSON(Map<Object, Object> jo) {
    return false;
  }

  /**
   * get the key-value in the bean to json.<br>
   * drop all the data which the name endsWith("_obj")
   *
   * @param jo
   *          the map
   * @return boolean, return true if success
   */
  public boolean toJSON(Map<String, Object> jo) {
    if (extra != null && extra.size() > 0 && jo != null) {
      for (String name : extra.keySet()) {
        Object o = extra.get(name);
        if (o == null || name.endsWith("_obj")) {
          continue;
        }

        jo.put(name, o);
      }

      return true;
    }
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "Bean@" + extra;
  }

  /**
   * convert the object to integer, default "0", this convert is safe and trying
   * to convert more digital to integer.
   *
   * @deprecated please refer X.toInt()
   * @param v
   *          the object
   * @return int of the value
   */
  public static int toInt(Object v) {
    return toInt(v, 0);
  }

  /**
   * convert the array bytes to string.
   * 
   * @param arr
   *          the array bytes
   * @return the string
   */
  public static String toString(byte[] arr) {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    if (arr != null) {
      int len = arr.length;
      for (int i = 0; i < len; i++) {
        if (i > 0) {
          sb.append(" ");
        }

        sb.append(Integer.toHexString((int) arr[i] & 0xff));
      }
    }

    return sb.append("]").toString();
  }

  /**
   * convert the array objects to string.
   * 
   * @param arr
   *          the array objects
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
   * convert the v to long data, this is safe convert, and trying convert more
   * data to long.
   *
   * @deprecated please refer X.toLong()
   * @param v
   *          the value
   * @return long
   */
  public static long toLong(Object v) {
    return toLong(v, 0);
  }

  /**
   * convert the v to long, if failed using defaultValue, please refer X.toLong
   *
   * @deprecated please refer X.toLong()
   * @param v
   *          the v
   * @param defaultValue
   *          the default value
   * @return long
   */
  public static long toLong(Object v, long defaultValue) {
    return X.toLong(v, defaultValue);
  }

  /**
   * load record in database.
   * 
   * @deprecated
   * @param <T>
   *          the generic Bean Class
   * @param table
   *          the table name
   * @param where
   *          the where conditions
   * @param args
   *          the where args
   * @param orderby
   *          the order by sentence
   * @param clazz
   *          the Bean class
   * @param db
   *          the db name
   * @return Bean
   */
  protected static <T extends DAO> T load(String table, String where, Object[] args, String orderby, Class<T> clazz,
      String db) {
    try {
      T b = (T) clazz.newInstance();

      if (load(table, where, args, orderby, b, db)) {
        return b;
      }
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(e.getMessage(), e);
    }
    return null;
  }

  /**
   * set the extra value.
   *
   * @param name
   *          the name
   * @param value
   *          the value
   */
  public final void set(String name, Object value) {
    if (extra == null) {
      extra = new HashMap<String, Object>();
    }

    extra.put(name, value);

  }

  /**
   * get the extra value by name from map <br>
   * the name can be : "name" <br>
   * "name.subname" to get the value in sub-map <br>
   * "name.subname[i]" to get the value in sub-map array <br>
   *
   * @param name
   *          the name
   * @return Object
   */
  @SuppressWarnings("unchecked")
  public final Object get(Object name) {
    if (extra == null) {
      return null;
    }

    String s = name.toString();
    if (extra.containsKey(s)) {
      return extra.get(s);
    }

    String[] ss = s.split("\\.");
    Map<String, Object> m = extra;
    Object o = null;
    for (String s1 : ss) {
      if (m == null) {
        return null;
      }

      o = m.get(s1);
      if (o == null)
        return null;
      if (o instanceof Map) {
        m = (Map<String, Object>) o;
      } else {
        m = null;
      }
    }

    return o;
  }

  /**
   * get the value at index("i").
   *
   * @param name
   *          the name
   * @param i
   *          the i
   * @return Object
   */
  @SuppressWarnings("rawtypes")
  public final Object get(Object name, int i) {
    if (extra == null) {
      return null;
    }

    if (extra.containsKey(name.toString())) {
      Object o = extra.get(name.toString());
      if (o instanceof List) {
        List l1 = (List) o;
        if (i >= 0 && i < l1.size()) {
          return l1.get(i);
        }
      } else if (i == 0) {
        return o;
      }
    }

    return null;
  }

  /**
   * get the size of the names.
   *
   * @return the int
   */
  @Override
  public final int size() {
    return extra == null ? 0 : extra.size();
  }

  /**
   * test is empty bean
   */
  @Override
  public final boolean isEmpty() {
    return extra == null ? true : extra.isEmpty();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Map#containsKey(java.lang.Object)
   */
  @Override
  public final boolean containsKey(Object key) {
    return extra == null ? false : extra.containsKey(key);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Map#containsValue(java.lang.Object)
   */
  @Override
  public final boolean containsValue(Object value) {
    return extra == null ? false : extra.containsValue(value);
  }

  /**
   * put the key-value in bean.
   *
   * @param key
   *          the key
   * @param value
   *          the value
   * @return the object
   */
  @Override
  public final Object put(String key, Object value) {
    set(key, value);
    return value;
  }

  /**
   * remove the key from the bean.
   *
   * @param key
   *          the key
   * @return the object
   */
  @Override
  public final Object remove(Object key) {
    return extra == null ? null : extra.remove(key);
  }

  /**
   * put all the data in the map to Bean.
   *
   * @param m
   *          the m
   */
  @Override
  public final void putAll(Map<? extends String, ? extends Object> m) {

    if (extra == null) {
      extra = new HashMap<String, Object>();
    }
    extra.putAll(m);
  }

  /**
   * remove all data from the bean.
   */
  @Override
  public final void clear() {
    if (extra != null) {
      extra.clear();
    }

  }

  /**
   * get the names from the bean.
   *
   * @return the sets the
   */
  @Override
  public final Set<String> keySet() {
    Set<String> names = new HashSet<String>();
    if (extra != null) {
      names.addAll(extra.keySet());
      for (String s : extra.keySet()) {
        if (s.endsWith("_obj")) {
          names.remove(s);
        }
      }
    }
    return names;
  }

  /**
   * get all the values.
   *
   * @return the collection
   */
  @Override
  public final Collection<Object> values() {
    return extra == null ? null : extra.values();
  }

  /**
   * get all the Entries except "_obj" field.
   *
   * @return the sets the
   */
  @Override
  public final Set<Entry<String, Object>> entrySet() {
    Set<Entry<String, Object>> ss = new HashSet<Entry<String, Object>>();
    if (extra != null) {
      for (Entry<String, Object> e : extra.entrySet()) {
        if (!e.getKey().endsWith("_obj") && e.getValue() != null) {
          ss.add(e);
        }
      }
    }
    return ss;
  }

  /**
   * by default, get integer from the map.
   *
   * @param name
   *          the name
   * @return int
   */
  public final int getInt(String name) {
    return toInt(get(name));
  }

  /**
   * by default, get long from the map.
   *
   * @param name
   *          the name
   * @return long
   */
  public long getLong(String name) {
    return toLong(get(name));
  }

  /**
   * by default, get the string from the map.
   *
   * @param name
   *          the name
   * @return String
   */
  public final String getString(String name) {
    Object o = get(name);
    if (o == null) {
      return null;
    } else if (o instanceof String) {
      return (String) o;
    } else {
      return o.toString();
    }
  }

  /**
   * by default, get the float from the map.
   *
   * @param name
   *          the name
   * @return float
   */
  public final float getFloat(String name) {
    return toFloat(get(name));
  }

  /**
   * by default, get the double from the map.
   *
   * @param name
   *          the name
   * @return double
   */
  public final double getDouble(String name) {
    return toDouble(get(name));
  }

  /**
   * get all extra value
   * 
   * @return Map
   */
  public Map<String, Object> getAll() {
    return extra;
  }

  /**
   * remove all extra value.
   */
  public final void removeAll() {
    if (extra != null) {
      extra.clear();
    }
  }

  /**
   * remove value by names.
   *
   * @param names
   *          the names
   */
  public final void remove(String... names) {
    if (extra != null && names != null) {
      for (String name : names) {
        extra.remove(name);
      }
    }
  }

  private Map<String, Object> extra = null;

  /**
   * create the data as json.<br>
   * drop all the data which the name endsWith("_obj")
   * 
   * @return JSONObject
   */
  public final JSONObject getJSON() {
    if (extra == null) {
      return null;
    }

    JSONObject jo = new JSONObject();

    toJSON(jo);

    return jo;
  }

  /**
   * the {@code W} Class used to create SQL "where" conditions<br>
   * this is for RDBS Query anly
   * 
   * @author joe
   * 
   */
  public final static class W {

    /***
     * "="
     */
    public static final int  OP_EQ    = 0;

    /**
     * "&gt;"
     */
    public static final int  OP_GT    = 1;

    /**
     * "&gt;="
     */
    public static final int  OP_GT_EQ = 2;

    /**
     * "&lt;"
     */
    public static final int  OP_LT    = 3;

    /**
     * "&lt;="
     */
    public static final int  OP_LT_EQ = 4;

    /**
     * "like"
     */
    public static final int  OP_LIKE  = 5;

    /**
     * "!="
     */
    public static final int  OP_NEQ   = 7;

    /**
     * ""
     */
    public static final int  OP_NONE  = 8;

    /**
     * "and"
     */
    private static final int AND      = 9;

    /**
     * "or"
     */
    private static final int OR       = 10;

    List<W>                  wlist    = new ArrayList<W>();

    List<Entity>             elist    = new ArrayList<Entity>();

    int                      cond     = AND;

    private W() {
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

    transient Object[] args;

    /**
     * create args for the SQL "where" <br>
     * return the Object[].
     *
     * @return Object[]
     */
    public Object[] args() {
      if (args == null && (elist.size() > 0 || wlist.size() > 0)) {
        List<Object> l1 = new ArrayList<Object>();

        args(l1);

        args = l1.toArray(new Object[l1.size()]);
      }

      return args;
    }

    private void args(List<Object> list) {
      for (Entity e : elist) {
        e.args(list);
      }

      for (W w1 : wlist) {
        w1.args(list);
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
      return elist == null ? X.EMPTY : (elist.toString() + "=>{" + where() + ", " + DAO.toString(args()) + "}");
    }

    public List<Entity> getAll() {
      return elist;
    }

    private transient String where;

    /**
     * create the SQL "where".
     *
     * @return String
     */
    public String where() {
      if (where == null && (elist.size() > 0 || wlist.size() > 0)) {
        StringBuilder sb = new StringBuilder();
        for (Entity e : elist) {
          if (sb.length() > 0) {
            if (e.cond == AND) {
              sb.append(" and ");
            } else if (e.cond == OR) {
              sb.append(" or ");
            }
          }

          sb.append(e.where());
        }

        for (W w : wlist) {
          if (sb.length() > 0) {
            if (w.cond == AND) {
              sb.append(" and ");
            } else if (w.cond == OR) {
              sb.append(" or ");
            }
          }

          sb.append(" (").append(w.where()).append(") ");
        }

        where = sb.toString();
      }

      return where;
    }

    /**
     * create a empty.
     *
     * @return W
     */
    public static W create() {
      return new W();
    }

    transient String orderby;

    /**
     * set the order by, as "order by xxx desc, xxx".
     *
     * @param orderby
     *          the orderby
     * @return W
     */
    public W order(String orderby) {
      this.orderby = orderby;
      return this;
    }

    /**
     * get the order by.
     *
     * @return String
     */
    public String orderby() {
      return orderby;
    }

    /**
     * set the sql and parameter.
     *
     * @param sql
     *          the sql
     * @param v
     *          the v
     * @return W
     */
    public W set(String sql, Object v) {
      return and(sql, v, W.OP_NONE);
    }

    /**
     * set the name and parameter with "and" and "EQ" conditions.
     *
     * @param name
     *          the name
     * @param v
     *          the v
     * @return W
     */
    public W and(String name, Object v) {
      return and(name, v, W.OP_EQ);
    }

    /**
     * set and "and (...)" conditions
     *
     * @param w
     *          the w
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
     *          the w
     * @return W
     */
    public W or(W w) {
      w.cond = OR;
      wlist.add(w);
      return this;
    }

    /**
     * set the namd and parameter with "op" conditions.
     *
     * @param name
     *          the name
     * @param v
     *          the v
     * @param op
     *          the op
     * @return W
     */
    public W and(String name, Object v, int op) {
      where = null;
      args = null;

      elist.add(new Entity(name, v, op, AND));
      return this;
    }

    /**
     * set name and parameter with "or" and "EQ" conditions.
     *
     * @param name
     *          the name
     * @param v
     *          the v
     * @return W
     */
    public W or(String name, Object v) {
      return or(name, v, W.OP_EQ);
    }

    /**
     * set the name and parameter with "or" and "op" conditions.
     *
     * @param name
     *          the name
     * @param v
     *          the v
     * @param op
     *          the op
     * @return W
     */
    public W or(String name, Object v, int op) {
      where = null;
      args = null;

      elist.add(new Entity(name, v, op, OR));

      return this;
    }

    /**
     * copy the name and parameter from a JSON, with "and" and "op" conditions.
     *
     * @param jo
     *          the jo
     * @param op
     *          the op
     * @param names
     *          the names
     * @return W
     */
    public W copy(JSONObject jo, int op, String... names) {
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
     * copy the value in jo, the format of name is: ["name", "table field name"
     * ].
     *
     * @param jo
     *          the jo
     * @param op
     *          the op
     * @param names
     *          the names
     * @return W
     */
    public W copy(JSONObject jo, int op, String[]... names) {
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
     * copy the name and int parameter from the JSON, with "and" and "op"
     * conditions.
     *
     * @param jo
     *          the jo
     * @param op
     *          the op
     * @param names
     *          the names
     * @return W
     */
    public W copyInt(JSONObject jo, int op, String... names) {
      if (jo != null && names != null && names.length > 0) {
        for (String name : names) {
          if (jo.has(name)) {
            String s = jo.getString(name);
            if (s != null && !"".equals(s)) {
              and(name, toInt(s), op);
            }
          }
        }
      }

      return this;
    }

    /**
     * copy the value of jo, the format of name is: ["name", "table field name"
     * ].
     *
     * @param jo
     *          the jo
     * @param op
     *          the op
     * @param names
     *          the names
     * @return W
     */
    public W copyInt(JSONObject jo, int op, String[]... names) {
      if (jo != null && names != null && names.length > 0) {
        for (String name[] : names) {
          if (name.length > 1) {
            if (jo.has(name[0])) {
              String s = jo.getString(name[0]);
              if (s != null && !"".equals(s)) {
                and(name[1], toInt(s), op);
              }
            }
          } else if (jo.has(name[0])) {
            String s = jo.getString(name[0]);
            if (s != null && !"".equals(s)) {
              and(name[0], toInt(s), op);
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
     *          the name
     * @param v
     *          the v
     * @return W
     */
    public static W create(String name, Object v) {
      W w = new W();
      w.elist.add(new Entity(name, v, OP_EQ, AND));
      return w;
    }

    private static class Entity {
      String name;
      Object value;
      int    op;
      int    cond;

      private List<Object> args(List<Object> list) {
        if (value != null) {
          if (value instanceof Object[]) {
            for (Object o : (Object[]) value) {
              list.add(o);
            }
          } else {
            list.add(value);
          }
        }

        return list;
      }

      public Entity copy() {
        return new Entity(name, value, op, cond);
      }

      private String where() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        switch (op) {
          case OP_EQ: {
            sb.append("=?");
            break;
          }
          case OP_GT: {
            sb.append(">?");
            break;
          }
          case OP_GT_EQ: {
            sb.append(">=?");
            break;
          }
          case OP_LT: {
            sb.append("<?");
            break;
          }
          case OP_LT_EQ: {
            sb.append("<=?");
            break;
          }
          case OP_LIKE: {
            sb.append(" like ?");
            break;
          }
          case OP_NEQ: {
            sb.append(" <> ?");
            break;
          }
        }

        return sb.toString();
      }

      // public String getName() {
      // return name;
      // }
      //
      // public Object getValue() {
      // return value;
      // }
      //
      // public int getOp() {
      // return op;
      // }

      transient String tostring;

      public String toString() {
        if (tostring == null) {
          StringBuilder s = new StringBuilder(name);
          switch (op) {
            case OP_EQ: {
              s.append("=");
              break;
            }
            case OP_GT: {
              s.append(">");
              break;
            }
            case OP_GT_EQ: {
              s.append(">=");
              break;
            }
            case OP_LT: {
              s.append("<");
              break;
            }
            case OP_LT_EQ: {
              s.append("<=");
              break;
            }
            case OP_NEQ: {
              s.append("<>");
              break;
            }
            case OP_LIKE: {
              s.append(" like ");
            }
          }
          s.append(value);

          tostring = s.toString();
        }
        return tostring;
      }

      private Entity(String name, Object v, int op, int cond) {
        this.name = name;
        this.op = op;
        this.cond = cond;

        if (op == OP_LIKE) {
          this.value = "%" + v + "%";
        } else {
          this.value = v;
        }
      }
    }
  }

}
