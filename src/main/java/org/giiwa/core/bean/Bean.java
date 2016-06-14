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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.cache.DefaultCachable;

import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MapReduceCommand;
import com.mongodb.MapReduceOutput;
import com.mongodb.Mongo;
import com.mongodb.MongoOptions;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

import net.sf.json.JSONObject;

// TODO: Auto-generated Javadoc
/**
 * The {@code Bean} Class is base class for all class that database access, it
 * almost includes all methods that need for database <br>
 * all data access MUST be inherited from it
 * 
 */
public abstract class Bean extends DefaultCachable implements Map<String, Object> {

  /** The Constant serialVersionUID. */
  private static final long      serialVersionUID = 2L;

  /** The log. */
  protected static Log           log              = LogFactory.getLog(Bean.class);
  protected static Log           sqllog           = LogFactory.getLog("sql");

  /** The conf. */
  protected static Configuration conf;

  /**
   * indicated whether is debug model
   */
  public static boolean          DEBUG            = true;

  /** The mongo. */
  private static Map<String, DB> mongo            = new HashMap<String, DB>();

  public static boolean isConfigured() {
    return mongo.size() > 0;
  }

  /**
   * initialize the Bean with the configuration.
   * 
   * @param conf
   *          the conf
   */
  public static void init(Configuration conf) {
    Bean.conf = conf;

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

  /**
   * Delete the data in mongo by the query
   * 
   * @param collection
   *          the collection
   * @param query
   *          the query
   * @return the int
   */
  protected static int delete(String collection, DBObject query) {
    try {
      DBCollection db = Bean.getCollection(collection);
      db.remove(query);
      return 1;
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(e.getMessage(), e);
    }
    return 0;
  }

  protected static int delete(DBObject query, Class<? extends Bean> t) {
    String collection = getCollection(t);
    if (collection != null) {
      delete(collection, query);
    }
    return -1;
  }

  protected static int delete(String where, Object[] args, Class<? extends Bean> t) {
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
   * Inits the db.
   * 
   * @deprecated
   * @param database
   *          the database
   * @return the db
   */
  private static synchronized DB initDB(String database) {
    DB g = mongo.get(database);
    if (g == null) {
      String url = conf.getString("mongo[" + database + "].url", X.EMPTY);
      if (!X.EMPTY.equals(url)) {
        String hosts[] = url.split(";");

        ArrayList<ServerAddress> list = new ArrayList<ServerAddress>();
        for (String s : hosts) {
          try {
            String s2[] = s.split(":");
            String host;
            int port = 27017;
            if (s2.length > 1) {
              host = s2[0];
              port = Integer.parseInt(s2[1]);
            } else {
              host = s2[0];
            }

            list.add(new ServerAddress(host, port));
          } catch (Exception e) {
            if (log.isErrorEnabled())
              log.error(e.getMessage(), e);
          }
        }

        String dbname = conf.getString("mongo[" + database + "].db", X.EMPTY);
        if (!X.EMPTY.equals(dbname)) {
          MongoOptions mo = new MongoOptions();
          mo.connectionsPerHost = conf.getInt("mongo[" + database + "].conns", 50);
          mo.autoConnectRetry = true;
          // mo.socketTimeout = 2000;
          mo.connectTimeout = 2000;
          // mo.autoConnectRetry = true;
          Mongo mongodb = new Mongo(list, mo);
          g = mongodb.getDB(dbname);

          mongo.put(database, g);
        }
      }
    }

    return g;
  }

  /**
   * Checks for db.
   * 
   * @deprecated
   * @param database
   *          the database
   * @return true, if successful
   */
  protected static boolean hasDB(String database) {
    DB g = mongo.get(database);
    if (g == null) {
      g = initDB(database);
    }

    return g != null;
  }

  /**
   * get Mongo DB connection
   * 
   * @return DB
   */
  public static DB getDB() {
    return getDB("prod");
  }

  /**
   * get Mongo DB connection <br>
   * the configuration including:
   * 
   * <pre>
   * mongo[database].url=
   * mongo[database].db=
   * mongo[database].conns=(50)
   * mongo[database].user=(null)
   * mongo[database].password=(null)
   * </pre>
   * 
   * @param database
   *          the name of database, if "" or null, then "prod"
   * @return DB
   */
  public static DB getDB(String database) {
    DB g = null;
    if (X.isEmpty(database)) {
      database = "prod";
    }

    synchronized (mongo) {
      g = mongo.get(database);
      if (g == null) {
        String url = conf.getString("mongo[" + database + "].url", X.EMPTY);
        if (!X.EMPTY.equals(url)) {
          String hosts[] = url.split(";");

          ArrayList<ServerAddress> list = new ArrayList<ServerAddress>();
          for (String s : hosts) {
            try {
              String s2[] = s.split(":");
              String host;
              int port = 27017;
              if (s2.length > 1) {
                host = s2[0];
                port = Integer.parseInt(s2[1]);
              } else {
                host = s2[0];
              }

              list.add(new ServerAddress(host, port));
            } catch (Exception e) {
              if (log.isErrorEnabled())
                log.error(e.getMessage(), e);
            }
          }

          String dbname = conf.getString("mongo[" + database + "].db", X.EMPTY);
          if (!X.EMPTY.equals(dbname)) {
            MongoOptions mo = new MongoOptions();
            mo.connectionsPerHost = conf.getInt("mongo[" + database + "].conns", 50);
            // mo.autoConnectRetry = true;
            Mongo mongodb = new Mongo(list, mo);
            g = mongodb.getDB(dbname);

            String user = conf.getString("mongo[" + database + "].user", X.EMPTY);
            String pwd = conf.getString("mongo[" + database + "].password", X.EMPTY);
            if (!X.isEmpty(user)) {
              boolean b = g.authenticate(user, pwd.toCharArray());
              if (!b) {
                if (log.isErrorEnabled())
                  log.error("authentication error for [" + database + "]");
              }
            }

            mongo.put(database, g);
          }
        }
      }
    }

    return g;
  }

  /**
   * Gets the collection using the database connection
   * 
   * <br>
   * the configuration including:
   * 
   * <pre>
   * mongo[database].url=
   * mongo[database].db=
   * mongo[database].conns=(50)
   * mongo[database].user=(null)
   * mongo[database].password=(null)
   * </pre>
   * 
   * @param database
   *          the database
   * @param collection
   *          the collection
   * @return DBCollection
   */
  protected static DBCollection getCollection(String database, String collection) {
    DB g = getDB(database);

    DBCollection d = null;

    if (g != null) {
      d = g.getCollection(collection);
    }

    if (d == null) {
      if (log.isErrorEnabled())
        log.error(database + " was miss configured, please access http://[host:port]/configure to configure");
    }
    return d;
  }

  /**
   * Gets the collection using "prod", if the same thread required twice, then
   * return same connection but reference "+1" <br>
   * the configuration including:
   * 
   * <pre>
   * mongo[prod].url=
   * mongo[prod].db=
   * mongo[prod].conns=(50)
   * mongo[prod].user=(null)
   * mongo[prod].password=(null)
   * </pre>
   * 
   * @param name
   *          the name of the collection
   * @return DBCollection
   */
  protected static DBCollection getCollection(String name) {
    return getCollection("prod", name);
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
   * @param v
   *          the v
   * @return the double
   */
  public static double toDouble(Object v) {
    return X.toDouble(v);
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
  protected static boolean exists(String where, Object[] args, Class<? extends Bean> t) {
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
  protected static int update(String where, Object[] args, V sets, Class<? extends Bean> t) {
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
  protected static <T extends Bean> T load(String table, String where, Object[] args, Class<T> clazz) {
    return load(table, where, args, null, clazz, null);

  }

  protected static <T extends Bean> T load(String where, Object[] args, Class<T> t) {
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

  protected static boolean load(String where, Object[] args, Bean b) {
    return load(where, args, null, b);
  }

  protected static boolean load(String table, String where, Object[] args, Bean b) {
    return load(table, where, args, null, b, null);
  }

  protected static <T extends Bean> T load(String where, Object[] args, String orderby, Class<T> t) {
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
  protected static boolean load(String where, Object[] args, String orderby, Bean b) {
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
  protected static boolean load(String table, String where, Object[] args, String orderby, Bean b, String db) {
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
   * Load.
   * 
   * @param <T>
   *          the generic type
   * @param collection
   *          the collection
   * @param query
   *          the query
   * @param clazz
   *          the clazz
   * @return the t
   */
  protected static <T extends Bean> T load(String collection, DBObject query, Class<T> clazz) {
    try {
      return load(collection, query, clazz.newInstance());
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(e.getMessage(), e);
    }

    return null;
  }

  protected static <T extends Bean> T load(String collection, DBObject query, T b) {
    try {
      DBCollection db = Bean.getCollection(collection);
      DBObject d = db.findOne(query);
      if (d != null) {
        b.load(d);
        return b;
      }
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(e.getMessage(), e);
    }

    return null;
  }

  protected static <T extends Bean> T load(String collection, DBObject query, DBObject order, T b) {
    DBCursor cur = null;
    TimeStamp t = TimeStamp.create();
    try {
      DBCollection db = Bean.getCollection(collection);
      if (Bean.DEBUG)
        KeyField.create(collection, query, order);

      cur = db.find(query);
      if (order != null) {
        cur.sort(order);
      }

      if (cur.hasNext()) {
        DBObject d = cur.next();
        if (log.isDebugEnabled())
          log.debug("load - cost=" + t.past() + "ms, collection=" + collection + ", query=" + query + ", order=" + order
              + ", result=" + d.get(X._ID));

        b.load(d);
        return b;
      } else {
        if (log.isDebugEnabled())
          log.debug("load - cost=" + t.past() + "ms, collection=" + collection + ", query=" + query + ", order=" + order
              + ", result=" + null);
      }
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error("query=" + query + ", order=" + order, e);
    } finally {
      if (cur != null) {
        cur.close();
      }
    }

    return null;
  }

  /**
   * load the data list.
   * 
   * @param <T>
   *          the generic Bean Class
   * @param query
   *          the query
   * @param orderby
   *          the order
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @param clazz
   *          the Bean class
   * @return Beans
   */
  protected static <T extends Bean> Beans<T> load(DBObject query, DBObject orderby, int offset, int limit,
      Class<T> clazz) {
    String collection = getCollection(clazz);
    if (collection != null) {
      return load(collection, query, orderby, offset, limit, clazz);
    }
    return null;

  }

  /**
   * get the data from the collection.
   * 
   * @param <T>
   *          the generic Bean Class
   * @param query
   *          the query
   * @param order
   *          the order query
   * @param obj
   *          the Bean Class
   * @return T
   */
  protected static <T extends Bean> T load(DBObject query, DBObject order, T obj) {
    String collection = getCollection(obj.getClass());
    if (collection != null) {
      return load(collection, query, order, obj);
    }
    return null;

  }

  /**
   * load the data list.
   * 
   * @param <T>
   *          the generic Bean Class
   * @param collection
   *          the collection name
   * @param query
   *          the query
   * @param orderBy
   *          the order by query
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @param clazz
   *          the Bean Class
   * @return Beans
   */
  protected static <T extends Bean> Beans<T> load(String collection, DBObject query, DBObject orderBy, int offset,
      int limit, Class<T> clazz) {
    TimeStamp t = TimeStamp.create();
    DBCollection db = null;
    DBCursor cur = null;
    try {
      db = Bean.getCollection(collection);
      cur = db.find(query);

      if (Bean.DEBUG)
        KeyField.create(collection, query, orderBy);

      if (orderBy != null) {
        cur.sort(orderBy);
      }

      Beans<T> bs = new Beans<T>();
      bs.total = cur.count();
      cur.skip(offset);
      bs.list = new ArrayList<T>();

      if (limit < 0)
        limit = Integer.MAX_VALUE;

      while (cur.hasNext() && limit > 0) {
        DBObject d = cur.next();
        T b = clazz.newInstance();
        b.load(d);
        bs.list.add(b);
        limit--;
      }

      if (log.isDebugEnabled())
        log.debug("load - cost=" + t.past() + "ms, collection=" + collection + ", query=" + query + ", order=" + orderBy
            + ", result=" + (bs == null || bs.getList() == null ? "null" : bs.getList().size()));

      if (t.past() > 10000) {
        log.warn("load - cost=" + t.past() + "ms, collection=" + collection + ", query=" + query + ", order=" + orderBy
            + ", result=" + (bs == null || bs.getList() == null ? "null" : bs.getList().size()));
      }
      return bs;
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error("query=" + query + ", order=" + orderBy, e);

      // sort
      if (query != null && db != null) {
        Set<String> set = query.keySet();
        for (String name : set) {
          if (!name.startsWith("$")) {
            // db.createIndex(name);
            db.ensureIndex(name);
          }
        }
      }

      if (orderBy != null && orderBy.keySet().size() > 0 && db != null) {
        db.createIndex(orderBy);
      }

    } finally {
      if (cur != null)
        cur.close();
    }

    return null;
  }

  /**
   * load the data full into the t.
   * 
   * @param <T>
   *          the generic Bean Class
   * @param query
   *          the query
   * @param t
   *          the Bean Class
   * @return Bean if failed, return null
   */
  protected static <T extends Bean> T load(DBObject query, T t) {
    String collection = getCollection(t.getClass());
    if (collection != null) {
      try {
        return load(query, null, t);
      } catch (Exception e) {
        if (log.isErrorEnabled())
          log.error(e.getMessage(), e);
      }
    }
    return null;
  }

  /**
   * load the Bean by id.
   * 
   * @param <T>
   *          the generic Bean Class
   * @param id
   *          the object id, "_id".
   * @param t
   *          the Bean Class
   * @return Bean
   */
  protected static <T extends Bean> T load(Object id, Class<T> t) {
    return load(new BasicDBObject(X._ID, id), t);
  }

  /**
   * load the data by the query
   * 
   * @param <T>
   *          the generic Bean Class
   * @param query
   *          the query
   * @param t
   *          the Bean Class
   * @return Bean the instance of the Class
   */
  protected static <T extends Bean> T load(DBObject query, Class<T> t) {
    String collection = getCollection(t);
    if (collection != null) {
      try {
        T obj = t.newInstance();
        return load(query, null, obj);
      } catch (Exception e) {
        if (log.isErrorEnabled())
          log.error(e.getMessage(), e);
      }
    }
    return null;
  }

  /**
   * load the Bean by the query, and order
   * 
   * @param <T>
   *          the generic Bean Class
   * @param query
   *          the query
   * @param order
   *          the orderby
   * @param t
   *          the Class Bean
   * @return Bean
   */
  protected static <T extends Bean> T load(DBObject query, DBObject order, Class<T> t) {
    String collection = getCollection(t);
    if (collection != null) {
      try {
        T obj = t.newInstance();
        return load(query, order, obj);
      } catch (Exception e) {
        if (log.isErrorEnabled())
          log.error(e.getMessage(), e);
      }
    }
    return null;
  }

  /**
   * Load the data, and return the DBObject
   * 
   * @param collection
   *          the collection
   * @param query
   *          the query
   * @return the DB object
   */
  protected static DBObject load(String collection, DBObject query) {
    /**
     * create the sql statement
     */
    try {
      DBCollection c = Bean.getCollection(collection);
      if (c != null) {
        if (Bean.DEBUG)
          KeyField.create(collection, query, null);
        DBObject d = c.findOne(query);
        return d;
      }
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(e.getMessage(), e);
    }
    return null;
  }

  /**
   * Load data by default, get all fields and set in map
   * 
   * @param r
   *          the r
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
   * Load by default, get all columns to a map
   * 
   * @param d
   *          the d
   */
  protected void load(DBObject d) {

    for (String name : d.keySet()) {
      this.set(name, d.get(name));
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
  protected final static <T extends Bean> List<T> load(String table, String[] cols, String where, Object[] args,
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
  protected final static <T extends Bean> List<T> load(String[] cols, String where, Object[] args, Class<T> clazz) {
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
  protected final static <T extends Bean> List<T> load(String[] cols, String where, Object[] args, String orderby,
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
  protected static <T extends Bean> List<T> load(String table, String[] cols, String where, Object[] args,
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
  protected static <T extends Bean> Beans<T> load(String where, Object[] args, String orderby, int offset, int limit,
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
  protected static <T extends Bean> Beans<T> load(String table, String where, Object[] args, String orderby, int offset,
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
  protected static <T extends Bean> Beans<T> load(String table, String where, Object[] args, String orderby, int offset,
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
   * get the collection name that associated with the Bean.
   * 
   * @param clazz
   *          the Bean Class
   * @return String
   */
  final static protected String getCollection(Class<? extends Bean> clazz) {
    /**
     * get the require annotation onGet
     */
    DBMapping mapping = (DBMapping) clazz.getAnnotation(DBMapping.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + clazz + "] declaretion");
      return null;
    } else {
      return mapping.collection();
    }
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
    } else {
      if (!X.isEmpty(mapping.collection())) {
        return insertCollection(mapping.collection(), sets, mapping.db());
      }
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
   * insert into the collection according to the Mapping(collection) declaration
   * 
   * @param v
   *          the values
   * @param t
   *          the Class of Bean
   * @return int how many data impacted
   */
  final protected static int insertCollection(V v, Class<? extends Bean> t) {
    DBMapping mapping = (DBMapping) t.getAnnotation(DBMapping.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + t + "] declaretion");
      return -1;
    }

    if (!X.isEmpty(mapping.collection())) {
      return insertCollection(mapping.collection(), v, mapping.db());
    }
    return -1;
  }

  /**
   * update the collection according the Mapping(collection) declaration
   * 
   * @param id
   *          the "_id" of the data
   * @param v
   *          the values
   * @param t
   *          the Class of Bean
   * @return int how many data impacted
   */
  final protected static int updateCollection(Object id, V v, Class<? extends Bean> t) {
    return updateCollection(id, v, t, false);
  }

  /**
   * update the mongo data
   * 
   * @param id
   *          the "_id"
   * @param v
   *          the values
   * @param t
   *          the Class of Bean
   * @param adding
   *          if true and not exists, then insert one by values
   * @return int how many data impacted
   */
  final protected static int updateCollection(Object id, V v, Class<? extends Bean> t, boolean adding) {
    String collection = getCollection(t);
    if (collection != null && !"none".equals(collection)) {
      return updateCollection(collection, id, v, adding);
    }
    return -1;
  }

  /**
   * update the collection by query.
   * 
   * @param query
   *          the query
   * @param v
   *          the values
   * @param t
   *          the Bean Class
   * @return int of updated
   */
  final protected static int updateCollection(DBObject query, V v, Class<? extends Bean> t) {
    String collection = getCollection(t);
    if (collection != null && !"none".equals(collection)) {
      return updateCollection(collection, query, v);
    }
    return -1;
  }

  /**
   * insert into the collection
   * 
   * @param collection
   *          the collection name
   * @param v
   *          the values
   * @param db
   *          the pool name, mongo[db].url in "giiwa.properties"
   * @return int
   */
  final protected static int insertCollection(String collection, V v, String db) {
    BasicDBObject d = new BasicDBObject();

    v.set("created", System.currentTimeMillis()).set("updated", System.currentTimeMillis());
    for (String name : v.names()) {
      d.append(name, v.value(name));
    }

    try {
      WriteResult r = Bean.getCollection(X.isEmpty(db) ? "prod" : db, collection).insert(d);

      if (log.isDebugEnabled())
        log.debug("inserted collection=" + collection + ", d=" + d);
      return 1;
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(e.getMessage(), e);
    }
    return 0;
  }

  /**
   * update the data in collection
   * 
   * @param collection
   *          the collection name
   * @param id
   *          the object id
   * @param v
   *          the values
   * @return int
   */
  final protected static int updateCollection(String collection, Object id, V v) {
    return updateCollection(collection, id, v, false);
  }

  /**
   * update the mongo data
   * 
   * @param collection
   *          the name of collection
   * @param id
   *          the "_id"
   * @param v
   *          the values
   * @param adding
   *          if true and not exists, then insert a new data
   * @return int
   */
  final protected static int updateCollection(String collection, Object id, V v, boolean adding) {

    BasicDBObject q = new BasicDBObject().append(X._ID, id);
    return updateCollection(collection, q, v, adding);
  }

  /**
   * update the data by query.
   * 
   * @param collection
   *          the collection name
   * @param q
   *          the query
   * @param v
   *          the values
   * @return int of updated
   */
  final protected static int updateCollection(String collection, DBObject q, V v) {
    return updateCollection(collection, q, v, false);
  }

  /**
   * update mongo collection.
   * 
   * @param collection
   *          the collection name
   * @param q
   *          the update query
   * @param v
   *          the value
   * @param adding
   *          add if not exists
   * @return int of updated
   */
  final protected static int updateCollection(String collection, DBObject q, V v, boolean adding) {
    BasicDBObject d = new BasicDBObject();

    v.set("updated", System.currentTimeMillis());
    // int len = v.size();
    for (String name : v.names()) {
      d.append(name, v.value(name));
    }

    try {
      WriteResult r = Bean.getCollection(collection).update(q, new BasicDBObject().append("$set", d), adding, true,
          WriteConcern.SAFE);

      if (log.isDebugEnabled())
        log.debug("updated collection=" + collection + ", q=" + q + ", d=" + d + ", n=" + r.getN() + ",result=" + r);

      // r.getN();
      // r.getField("nModified");
      return r.getN();
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(e.getMessage(), e);
    }
    return 0;
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
  final protected static int insert(String table, V sets, String db) {
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
   * The Class V. of value, used to generate the "values" of SQL
   */
  public static final class V {

    /** The list. */
    // private List<Entity> list = new ArrayList<Entity>();
    private Map<String, Object> m = new LinkedHashMap<String, Object>();

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
     * Sets the value if not exists, ignore if exists.
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
    public V copy(Map<Object, Object> jo) {
      if (jo == null)
        return this;

      for (Object s : jo.keySet()) {
        if (jo.containsKey(s)) {
          Object o = jo.get(s);
          if (X.isEmpty(o)) {
            set(s.toString(), X.EMPTY);
          } else {
            set(s.toString(), o);
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
    public V copy(Map<Object, Object> jo, String... names) {
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

      if (names != null) {
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
     * copy the name in jo, the format of name is: ["name", "table field name"].
     *
     * @param jo
     *          the data map
     * @param names
     *          the name to copy, if null then copy all names
     * @return V
     */
    public V copy(Map<Object, Object> jo, String[]... names) {
      if (jo == null || names == null)
        return this;

      for (String s[] : names) {
        if (s.length > 1) {
          if (jo.containsKey(s[0])) {
            Object o = jo.get(s[0]);
            if (o == null || "".equals(o))
              continue;

            set(s[1], jo.get(s[0]));
          }
        } else {
          if (jo.containsKey(s[0])) {
            Object o = jo.get(s[0]);
            if (o == null || "".equals(o))
              continue;

            set(s[0], jo.get(s[0]));
          }
        }
      }

      return this;
    }

    /**
     * copy the checkbox value in V, "on"="on", otherwise="off".
     *
     * @param jo
     *          the json
     * @param names
     *          the name string
     * @return V
     */
    public V copyCheckbox(Map<Object, Object> jo, String... names) {
      if (jo == null || names == null || names.length == 0)
        return this;

      for (String s : names) {
        if (jo.containsKey(s)) {
          Object o = jo.get(s);
          if (X.isEmpty(o)) {
            set(s, "off");
          } else if ("on".equals(o)) {
            set(s, "on");
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
      if (m.containsKey(name)) {
        return m.get(name);
      }
      return null;
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
    public V copyInt(Map<Object, Object> jo, String... names) {
      if (jo == null || names == null)
        return this;

      for (String s : names) {
        if (jo.containsKey(s)) {
          set(s, Bean.toInt(jo.get(s)));
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
     * @param names
     *          the names
     * @return V
     */
    public V copyInt(Map<Object, Object> jo, String[]... names) {
      if (jo == null || names == null)
        return this;

      for (String s[] : names) {
        if (s.length > 1) {
          if (jo.containsKey(s[0])) {
            set(s[1], Bean.toInt(jo.get(s[0])));
          }
        } else {
          if (jo.containsKey(s[0])) {
            set(s[0], Bean.toInt(jo.get(s[0])));
          }
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
    public V copyLong(Map<Object, Object> jo, String... names) {
      if (jo == null || names == null)
        return this;

      for (String s : names) {
        if (jo.containsKey(s)) {
          set(s, Bean.toLong(jo.get(s)));
        }
      }

      return this;
    }

    /**
     * copy the value in jo, the format of name is: ["name", "table field name"
     * ].
     *
     * @param jo
     *          the map to copy
     * @param names
     *          the names
     * @return V
     */
    public V copyLong(Map<Object, Object> jo, String[]... names) {
      if (jo == null || names == null)
        return this;

      for (String s[] : names) {
        if (s.length > 1) {
          if (jo.containsKey(s[0])) {
            set(s[1], Bean.toLong(jo.get(s[0])));
          }
        } else {
          if (jo.containsKey(s[0])) {
            set(s[0], Bean.toLong(jo.get(s[0])));
          }
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
      Class<? extends Bean> t) {
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
      Class<? extends Bean> t) {
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
   * get the key-value in the bean to json.
   *
   * @param jo
   *          the map
   * @return boolean, return true if success
   */
  public boolean toJSON(Map<Object, Object> jo) {
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
  protected static <T extends Bean> T load(String table, String where, Object[] args, String orderby, Class<T> clazz,
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
   * create the data as json
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
   * the {@code W} Class used to create SQL "where" conditions
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
      return elist == null ? X.EMPTY : (elist.toString() + "=>{" + where() + ", " + Bean.toString(args()) + "}");
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

  /**
   * test whether the query is exists in
   * 
   * @param query
   *          the query
   * @param t
   *          the Class of Bean
   * @return boolean, return true if exists, otherwise return false
   */
  protected static boolean exists(DBObject query, Class<? extends Bean> t) {
    String collection = getCollection(t);
    if (collection != null) {
      TimeStamp t1 = TimeStamp.create();
      try {
        return Bean.load(collection, query) != null;
      } finally {
        if (log.isDebugEnabled())
          log.debug("exists cost=" + t1.past() + "ms,  collection=" + collection + ", query=" + query);
      }
    }
    return false;
  }

  /**
   * 
   * @param x
   * @param cols
   * @return
   */
  private static String _map(String x, String[][] cols) {
    String s = "function(){emit(";
    if (!X.isEmpty(x)) {
      s += "this." + x;

      s += ", {";
      for (String s1[] : cols) {
        s += "'" + s1[0] + "': this." + s1[0] + ",";
      }
      s += "'count':1});};";
    } else {
      s += "this." + cols[0][0];
      s += ", {'" + cols[0][0] + "': 1, 'count':1});};";
    }

    return s;
  }

  private static String _reduce(String[][] cols) {

    String s = "function(key, values) { var reduced ={";
    for (String s1[] : cols) {
      s += s1[0] + ":0,";
    }
    s += "count:0};  values.forEach(function(v){ ";
    for (String s1[] : cols) {
      s += "reduced." + s1[0] + " += v." + s1[0] + "; ";
      s += "reduced.count+=v.count;";
    }
    s += "}); return reduced;" + "}";

    return s;
  }

  /**
   * get the result by mapReduce in mongo.
   * 
   * @param <T>
   *          the generic Bean Class
   * @param x
   *          the column name
   * @param cols
   *          the "Y" definition, {{"a", "aver"},{"b", "sum"}, {"c", "count"}}
   * @param finaljs
   *          the final js body.
   * @param query
   *          the query
   * @param limit
   *          the limit
   * @param clazz
   *          the Bean class
   * @return Map
   */
  protected static <T extends Bean> Map<Object, Map<Object, Object>> mapreduce(String x, String[][] cols,
      String finaljs, DBObject query, int limit, Class<T> clazz) {

    String mapjs = _map(x, cols);
    String reducejs = _reduce(cols);

    try {
      DBCollection dc = getCollection(Bean.getCollection(clazz));
      MapReduceCommand cmd = new MapReduceCommand(dc, mapjs, reducejs, null, MapReduceCommand.OutputType.INLINE, query);
      // set limit rows;
      if (limit > 0) {
        cmd.setLimit(limit);
      }

      if (!X.isEmpty(finaljs)) {
        cmd.setFinalize(finaljs);
      }

      MapReduceOutput out = dc.mapReduce(cmd);
      // log.debug("out=" + out);

      Map<Object, Map<Object, Object>> m = new LinkedHashMap<Object, Map<Object, Object>>();
      for (DBObject o : out.results()) {
        Object id = o.get(X._ID);
        Map<Object, Object> d = (Map<Object, Object>) o.get("value");
        Map<Object, Object> v = new HashMap<Object, Object>();

        for (String[] s1 : cols) {
          String name = s1[0];
          String f = s1.length > 1 ? s1[1] : "count";
          int count = Bean.toInt(d.get("count"));
          if (count > 0) {
            if ("count".equals(f)) {
              v.put(name, count);
            } else if ("aver".equals(f)) {
              Object v1 = d.get(name);
              if (v1 instanceof Integer) {
                v.put(name, Bean.toInt(v1) / count);
              } else if (v1 instanceof Long) {
                v.put(name, Bean.toLong(v1) / count);
              } else if (v1 instanceof Float) {
                v.put(name, Bean.toFloat(v1) / count);
              } else if (v1 instanceof Double) {
                v.put(name, Bean.toDouble(v1) / count);
              }
            } else if ("sum".equals(f)) {
              Object v1 = d.get(name);
              v.put(name, v1);
            }
          } else {
            v.put(name, 0);
          }
        }

        m.put(id, v);
      }

      if (log.isDebugEnabled())
        log.debug("mapreduce: query=" + query + ", mapjs=" + mapjs + ", reducejs=" + reducejs + ", finaljs=" + finaljs
            + ", out=" + out + ", m=" + m);

      return m;
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error("\r\nmapjs=" + mapjs + "\r\nreducejs=" + reducejs + "\r\nfinaljs=" + finaljs, e);
    }
    return null;
  }

  /**
   * run the command in mongo.
   *
   * @param cmd
   *          the command
   * @return boolean, return true if "ok"
   */
  public static boolean run(String cmd) {
    CommandResult r = Bean.getDB().command(cmd);
    return r.ok();
  }

  /**
   * get all collections
   * 
   * @return Set
   */
  public static Set<String> getCollections() {
    return Bean.getDB().getCollectionNames();
  }

  /**
   * remove all the data from the collection.
   *
   * @param collection
   *          the collection
   */
  public static void clear(String collection) {
    try {
      Bean.getCollection(collection).remove(new BasicDBObject());
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(e.getMessage(), e);
    }
  }

  /**
   * get distinct value for key by the query.
   *
   * @param key
   *          the key that contain the value
   * @param q
   *          the query
   * @param t
   *          class
   * @return List of the value
   */
  @SuppressWarnings("unchecked")
  public static List<Object> distinct(String key, BasicDBObject q, Class<? extends Bean> t) {
    String collection = Bean.getCollection(t);
    if (!X.isEmpty(collection)) {
      TimeStamp t1 = TimeStamp.create();
      try {
        if (Bean.DEBUG)
          KeyField.create(collection, new BasicDBObject(q).append(key, 1), null);

        DBCollection c = Bean.getCollection(collection);
        return c.distinct(key, q);
      } catch (Exception e) {
        if (log.isErrorEnabled())
          log.error(e.getMessage(), e);
      } finally {
        if (log.isDebugEnabled())
          log.debug("disinct[" + key + "] cost=" + t1.past() + "ms,  collection=" + collection + ", query=" + q);
      }
    }
    return null;
  }

  /**
   * count the number by the query.
   *
   * @param q
   *          the q
   * @param t
   *          the t
   * @return long
   */
  public static long count(BasicDBObject q, Class<? extends Bean> t) {
    String collection = Bean.getCollection(t);
    if (!X.isEmpty(collection)) {
      TimeStamp t1 = TimeStamp.create();
      try {
        if (Bean.DEBUG)
          KeyField.create(collection, q, null);

        DBCollection c = Bean.getCollection(collection);
        return c.count(q);
      } finally {
        if (log.isDebugEnabled())
          log.debug("count(*) cost=" + t1.past() + "ms,  collection=" + collection + ", query=" + q);
      }
    }
    return 0;
  }

}
