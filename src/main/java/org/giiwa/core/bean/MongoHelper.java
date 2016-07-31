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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoOptions;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

// TODO: Auto-generated Javadoc
/**
 * The {@code Bean} Class is base class for all class that database access, it
 * almost includes all methods that need for database <br>
 * all data access MUST be inherited from it
 * 
 */
public class MongoHelper extends Helper {

  /** The log. */
  protected static Log           log    = LogFactory.getLog(MongoHelper.class);
  protected static Log           sqllog = LogFactory.getLog("sql");

  /**
   * indicated whether is debug model
   */
  public static boolean          DEBUG  = true;

  /** The mongo. */
  private static Map<String, DB> mongo  = new HashMap<String, DB>();

  public static boolean isConfigured() {
    getDB();
    return mongo.size() > 0 || org.giiwa.core.db.DB.isConfigured();
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
      DBCollection db = MongoHelper.getCollection(collection);
      if (db != null) {
        db.remove(query);
        return 1;
      }
      return -1;
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(e.getMessage(), e);
    }
    return -1;
  }

  protected static int delete(DBObject query, Class<? extends Bean> t) {

    String collection = getCollection(t);
    if (collection != null) {
      delete(collection, query);
    }
    return -1;
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
      DBCollection db = MongoHelper.getCollection(collection);
      if (db != null) {
        DBObject d = db.findOne(query);
        if (d != null) {
          b.load(d);
          return b;
        }
      }
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(e.getMessage(), e);
    }

    return null;
  }

  public static <T extends Bean> T load(String collection, DBObject query, DBObject order, T b) {
    DBCursor cur = null;
    TimeStamp t = TimeStamp.create();
    try {
      DBCollection db = MongoHelper.getCollection(collection);
      if (db != null) {
        if (MongoHelper.DEBUG)
          KeyField.create(collection, query, order);

        DBObject d = null;
        if (order == null) {
          d = db.findOne(query);
        } else {
          d = db.findOne(query, null, order);
        }

        if (d != null) {
          if (log.isDebugEnabled())
            log.debug("load - cost=" + t.past() + "ms, collection=" + collection + ", query=" + query + ", order="
                + order + ", result=" + d.get(X._ID));

          b.load(d);
          return b;
        } else {
          if (log.isDebugEnabled())
            log.debug("load - cost=" + t.past() + "ms, collection=" + collection + ", query=" + query + ", order="
                + order + ", result=" + null);
        }
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
      db = MongoHelper.getCollection(collection);
      if (db != null) {
        cur = db.find(query);

        if (MongoHelper.DEBUG)
          KeyField.create(collection, query, orderBy);

        if (orderBy != null) {
          cur.sort(orderBy);
        }

        Beans<T> bs = new Beans<T>();
        // TODO, ignore this as big performance
        // bs.total = _count(cur, 0, (int) db.count());
        // log.debug("cost=" + t.past() +"ms, count=" + bs.total);

        cur.skip(offset);
        // log.debug("skip=" + t.past() +"ms, count=" + bs.total);
        bs.list = new ArrayList<T>();

        if (limit < 0)
          limit = Integer.MAX_VALUE;

        while (cur.hasNext() && limit > 0) {
          // log.debug("hasnext=" + t.past() +"ms, count=" + bs.total);
          DBObject d = cur.next();
          // log.debug("next=" + t.past() +"ms, count=" + bs.total);
          T b = clazz.newInstance();
          b.load(d);
          bs.list.add(b);
          limit--;
        }

        if (log.isDebugEnabled())
          log.debug("load - cost=" + t.past() + "ms, collection=" + collection + ", query=" + query + ", order="
              + orderBy + ", result=" + (bs == null || bs.getList() == null ? "null" : bs.getList().size()));

        if (t.past() > 10000) {
          log.warn("load - cost=" + t.past() + "ms, collection=" + collection + ", query=" + query + ", order="
              + orderBy + ", result=" + (bs == null || bs.getList() == null ? "null" : bs.getList().size()));
        }
        return bs;
      }
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

  public static <T extends Bean> T load(String collection, DBObject query, DBObject order, Class<T> t) {
    try {
      T obj = t.newInstance();
      return load(collection, query, order, obj);
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(e.getMessage(), e);
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
      DBCollection c = MongoHelper.getCollection(collection);
      if (c != null) {
        if (MongoHelper.DEBUG)
          KeyField.create(collection, query, null);

        DBObject d = c.findOne(query);
        return d;
      }
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(query, e);
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
    Table mapping = (Table) clazz.getAnnotation(Table.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + clazz + "] declaretion");
      return null;
    } else {
      return mapping.name();
    }
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
    Table mapping = (Table) t.getAnnotation(Table.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + t + "] declaretion");
      return -1;
    }

    if (!X.isEmpty(mapping.name())) {
      return insertCollection(mapping.name(), v);
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
  final protected static int insertCollection(String collection, V v) {

    DBCollection c = MongoHelper.getCollection("prod");
    if (c != null) {
      BasicDBObject d = new BasicDBObject();

      v.set("created", System.currentTimeMillis()).set("updated", System.currentTimeMillis());
      for (String name : v.names()) {
        d.append(name, v.value(name));
      }

      try {

        WriteResult r = c.insert(d);

        if (log.isDebugEnabled())
          log.debug("inserted collection=" + collection + ", d=" + d);
        return 1;
      } catch (Exception e) {
        if (log.isErrorEnabled())
          log.error(e.getMessage(), e);
      }
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
      DBCollection c = MongoHelper.getCollection(collection);
      WriteResult r = c.update(q, new BasicDBObject().append("$set", d), adding, true, WriteConcern.SAFE);

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
   * test whether the query is exists in
   * 
   * @param query
   *          the query
   * @param t
   *          the Class of Bean
   * @return boolean, return true if exists, otherwise return false
   * @throws Exception
   *           throw exception when occur database error
   */
  protected static boolean exists(DBObject query, Class<? extends Bean> t) throws Exception {
    String collection = getCollection(t);
    if (collection != null) {
      return exists(collection, query);
    }
    throw new Exception("the Class<" + t.getName() + "> doest annotated by @DBMapping()!");
  }

  protected static boolean exists(String collection, DBObject query) throws Exception {
    TimeStamp t1 = TimeStamp.create();
    try {
      return MongoHelper.load(collection, query) != null;
    } finally {
      if (log.isDebugEnabled())
        log.debug("exists cost=" + t1.past() + "ms,  collection=" + collection + ", query=" + query);
    }
  }

  /**
   * run the command of Mongo.
   *
   * @param cmd
   *          the command
   * @return boolean, return true if "ok"
   */
  public static boolean run(String cmd) {
    DB d = MongoHelper.getDB();
    if (d != null) {
      CommandResult r = d.command(cmd);
      return r.ok();
    }
    return false;
  }

  /**
   * get all collections
   * 
   * @return Set
   */
  public static Set<String> getCollections() {
    DB d = MongoHelper.getDB();
    if (d != null) {
      return d.getCollectionNames();
    }
    return null;
  }

  /**
   * remove all the data from the collection.
   *
   * @param collection
   *          the collection
   */
  public static void clear(String collection) {
    try {
      DBCollection c = MongoHelper.getCollection(collection);
      if (c != null) {
        c.remove(new BasicDBObject());
      }
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

    String collection = MongoHelper.getCollection(t);
    if (!X.isEmpty(collection)) {
      TimeStamp t1 = TimeStamp.create();
      try {
        if (MongoHelper.DEBUG)
          KeyField.create(collection, new BasicDBObject(q).append(key, 1), null);

        DBCollection c = MongoHelper.getCollection(collection);
        if (c != null) {
          return c.distinct(key, q);
        }
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
    String collection = MongoHelper.getCollection(t);
    if (!X.isEmpty(collection)) {
      TimeStamp t1 = TimeStamp.create();
      DBCursor c1 = null;
      try {
        if (MongoHelper.DEBUG)
          KeyField.create(collection, q, null);

        DBCollection c = MongoHelper.getCollection(collection);
        if (c != null) {
          c1 = c.find(q);
          return _count(c1, 0, (int) c.count());

        }

      } finally {
        if (c1 != null)
          c1.close();

        if (log.isDebugEnabled())
          log.debug("count, cost=" + t1.past() + "ms,  collection=" + collection + ", query=" + q);
      }
    }
    return 0;
  }

  private static int _count(DBCursor c, int start, int end) {
    if (start == end) {
      return (int) start;
    }

    TimeStamp t = TimeStamp.create();
    int count = (start + end) / 2;
    DBCursor c1 = c.getCollection().find(c.getQuery());
    try {
      // log.debug("find=" + t.past() + "ms,count=" + count);
      c1.skip(count);
      // log.debug("skip=" + t.past() + "ms,count=" + count);
      if (c1.hasNext()) {
        // log.debug("cost=" + t.past() + "ms,count=" + count);
        if (end - count == 1)
          return end;
        return _count(c, count, end);
      } else {
        log.debug("cost=" + t.past() + "ms,count=" + count);
        if (count - start <= 1)
          return count;
        return _count(c, start, count);
      }
    } finally {
      c1.close();
    }
  }

}
