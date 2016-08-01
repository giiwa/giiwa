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
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.db.DB;

// TODO: Auto-generated Javadoc
/**
 * The {@code Bean} Class is base class for all class that database access, it
 * almost includes all methods that need for database <br>
 * all data access MUST be inherited from it
 * 
 */
public class RDSHelper extends Helper {

  /**
   * indicated whether is debug model
   */
  public static boolean DEBUG = true;

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
  public static int update(String table, String sets, String where, Object[] whereArgs, String db) {
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

  public static int delete(String where, Object[] args, Class<? extends Bean> t) {
    /**
     * get the require annotation onGet
     */
    Table mapping = (Table) t.getAnnotation(Table.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + t + "] declaretion");
      return -1;
    }

    return delete(mapping.name(), where, args);
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
   * @return int
   */
  public static int delete(String table, String where, Object[] whereArgs) {
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
      c = getConnection();

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

  final public int insertOrUpdate(String where, Object[] args, V sets) {
    /**
     * get the require annotation onGet
     */
    Table mapping = (Table) this.getClass().getAnnotation(Table.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + this.getClass() + "] declaretion");
      return -1;
    }

    return insertOrUpdate(mapping.name(), where, args, sets);
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
   * @return int
   */
  public final static int insertOrUpdate(String table, String where, Object[] args, V sets) {
    int i = 0;
    try {
      if (exists(table, where, args)) {
        i = updateTable(table, where, args, sets);
      } else {
        i = insertTable(table, sets);
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
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
   * @throws Exception
   *           throw Exception
   */
  public static boolean exists(String where, Object[] args, Class<? extends Bean> t) throws Exception {
    /**
     * get the require annotation onGet
     */
    Table mapping = (Table) t.getAnnotation(Table.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + t + "] declaretion");
      return false;
    }

    return exists(mapping.name(), where, args);

  }

  /**
   * test exists.
   * 
   * @param table
   *          the table name
   * @param where
   *          the where conditions.
   * @param args
   *          the where args
   * @return boolean
   * @throws Exception
   *           throw Exception if occur DB error
   */
  public static boolean exists(String table, String where, Object[] args) throws Exception {
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
      c = getConnection();

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

      throw e;
    } finally {
      close(r, p, c);

      if (t.past() > 2 && sqllog.isDebugEnabled()) {
        sqllog.debug("cost:" + t.past() + "ms, sql=[" + sql + "]");
      }
    }
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
  public static int update(String where, Object[] args, V sets, Class<? extends Bean> t) {
    Table mapping = (Table) t.getAnnotation(Table.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + t + "] declaretion");
      return -1;
    }

    if (!X.isEmpty(mapping.name())) {
      return updateTable(mapping.name(), where, args, sets);
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
   * @return int
   */
  public static int updateTable(String table, String where, Object[] whereArgs, V sets) {
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

      c = getConnection();

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
   * @return int
   */
  public static int update(String sql, Object[] args) {
    /**
     * /** update it in database
     */
    Connection c = null;
    PreparedStatement p = null;

    try {
      c = getConnection();

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
  public static <T extends Bean> T load(String table, String where, Object[] args, Class<T> clazz) {
    return load(table, where, args, null, clazz);

  }

  public static <T extends Bean> T load(String where, Object[] args, Class<T> t) {
    /**
     * get the require annotation onGet
     */
    Table mapping = (Table) t.getAnnotation(Table.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + t + "] declaretion");
      return null;
    }

    return load(mapping.name(), where, args, null, t);

  }

  public static boolean load(String where, Object[] args, Bean b) {
    return load(where, args, null, b);
  }

  public static boolean load(String table, String where, Object[] args, Bean b) {
    return load(table, where, args, null, b);
  }

  public static <T extends Bean> T load(String where, Object[] args, String orderby, Class<T> t) {
    /**
     * get the require annotation onGet
     */
    Table mapping = (Table) t.getAnnotation(Table.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + t + "] declaretion");
      return null;
    }

    return load(mapping.name(), where, args, orderby, t);
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
  public static boolean load(String where, Object[] args, String orderby, Bean b) {
    /**
     * get the require annotation onGet
     */
    Table mapping = (Table) b.getClass().getAnnotation(Table.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + b.getClass() + "] declaretion");
      return false;
    }

    return load(mapping.name(), where, args, orderby, b);
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
   * @return boolean
   */
  public static boolean load(String table, String where, Object[] args, String orderby, Bean b) {
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
      c = getConnection();

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
  public final static <T extends Bean> List<T> load(String table, String[] cols, String where, Object[] args,
      Class<T> clazz) {
    return load(table, cols, where, args, null, -1, -1, clazz);
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
  public final static <T extends Bean> List<T> load(String[] cols, String where, Object[] args, Class<T> clazz) {
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
  public final static <T extends Bean> List<T> load(String[] cols, String where, Object[] args, String orderby,
      int offset, int limit, Class<T> t) {
    /**
     * get the require annotation onGet
     */
    Table mapping = (Table) t.getAnnotation(Table.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + t + "] declaretion");
      return null;
    }

    return load(mapping.name(), cols, where, args, orderby, offset, limit, t);
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
   * @return List
   */
  public static <T extends Bean> List<T> load(String table, String[] cols, String where, Object[] args, String orderby,
      int offset, int limit, Class<T> clazz) {
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
      c = getConnection();

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
  public static <T extends Bean> Beans<T> load(String where, Object[] args, String orderby, int offset, int limit,
      Class<T> t) {
    /**
     * get the require annotation onGet
     */
    Table mapping = (Table) t.getAnnotation(Table.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + t + "] declaretion");
      return null;
    }

    return load(mapping.name(), where, args, orderby, offset, limit, t);
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
   * @return Beans
   */
  public static <T extends Bean> Beans<T> load(String table, String where, Object[] args, String orderby, int offset,
      int limit, Class<T> clazz) {
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

      c = getConnection();

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
  public static <T extends Bean> Beans<T> load(String table, String where, Object[] args, String orderby, int offset,
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
   * batch insert.
   * 
   * @param sets
   *          the values collection
   * @param t
   *          the Bean Class
   * @return int
   */
  final public static int insert(Collection<V> sets, Class<?> t) {
    /**
     * get the require annotation onGet
     */
    Table mapping = (Table) t.getAnnotation(Table.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + t + "] declaretion");
      return -1;
    }

    return insert(mapping.name(), sets);

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
  final public static int insertTable(V sets, Class<? extends Bean> t) {
    Table mapping = (Table) t.getAnnotation(Table.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + t + "] declaretion");
      return -1;
    }

    if (!X.isEmpty(mapping.name())) {
      return insertTable(mapping.name(), sets);
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
   * @return int of how many data inserted
   */
  public static int insert(String table, Collection<V> list) {
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
      c = getConnection();

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
   * @return int
   */
  public static int insertTable(String table, V sets) {
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
      c = getConnection();

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
  public static <T> List<T> loadList(String table, String col, String where, Object[] args, Class<T> clazz, String db) {
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
  public static String getString(String table, String col, String where, Object[] args, String db) {
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
  public final static <T> List<T> getList(String col, String where, Object[] args, String orderby, int s, int n,
      Class<? extends Bean> t) {
    /**
     * get the require annotation onGet
     */
    Table mapping = (Table) t.getAnnotation(Table.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + t + "] declaretion");
      return null;
    }

    return getList(mapping.name(), col, where, args, orderby, s, n);
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
  public final static <T> T getOne(String col, String where, Object[] args, String orderby, int position,
      Class<? extends Bean> t) {
    /**
     * get the require annotation onGet
     */
    Table mapping = (Table) t.getAnnotation(Table.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + t + "] declaretion");
      return null;
    }

    return getOne(mapping.name(), col, where, args, orderby, position);
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
   * @return T
   */
  public static <T> T getOne(String table, String col, String where, Object[] args, String orderby, int position) {

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
      c = getConnection();

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
   * @return List
   */
  @SuppressWarnings("unchecked")
  public final static <T> List<T> getList(String table, String col, String where, Object[] args, String orderby, int s,
      int n) {

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
      c = getConnection();

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
   * load record in database.
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
   * @param clazz
   *          the Bean class
   * @return Bean
   */
  public static <T extends Bean> T load(String table, String where, Object[] args, String orderby, Class<T> clazz) {
    try {
      T b = (T) clazz.newInstance();

      if (load(table, where, args, orderby, b)) {
        return b;
      }
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(e.getMessage(), e);
    }
    return null;
  }

  public static boolean isConfigured() {
    return DB.isConfigured();
  }

  public static long count(String table, String where, Object[] args) {

    /**
     * create the sql statement
     */
    TimeStamp t = TimeStamp.create();

    StringBuilder sum = new StringBuilder();
    sum.append("select count(*) t from ").append(table);
    if (where != null) {
      sum.append(" where ").append(where);
    }

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
        return 0;

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
        return r.getInt("t");
      }
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(sum.toString() + toString(args), e);

    } finally {
      close(r, p, c);

      if (t.past() > 2 && sqllog.isDebugEnabled()) {
        sqllog.debug("cost:" + t.past() + "ms, sql=[" + sum + "]; [" + sum + "]");
      }
    }

    return 0;
  }

  public static List<Object> distinct(String table, String name, String where, Object[] args) {
    /**
     * create the sql statement
     */
    TimeStamp t = TimeStamp.create();

    StringBuilder sql = new StringBuilder();
    sql.append("select distinct(").append(name).append(") from ").append(table);
    if (where != null) {
      sql.append(" where ").append(where);
    }

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
      List<Object> list = new ArrayList<Object>();
      while (r.next()) {
        list.add(r.getObject(1));
      }

      if (log.isDebugEnabled())
        log.debug("load - cost=" + t.past() + "ms, collection=" + table + ", sql=" + sql + ", result=" + list);

      if (t.past() > 10000) {
        log.warn("load - cost=" + t.past() + "ms, collection=" + table + ", sql=" + sql + ", result=" + list);
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

}
