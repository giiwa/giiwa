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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.json.JSON;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

/**
 * The {@code Helper} Class is utility class for all database operation.
 * 
 */
public class Helper {

  /** The log utility. */
  protected static Log   log     = LogFactory.getLog(Helper.class);

  /**
   * the sqllog separate from the log utility, which used to record sql info
   * only
   */
  protected static Log   sqllog  = LogFactory.getLog("sql");

  private static Monitor monitor = null;

  protected static enum DBType {
    MONGO, RDS, NONE;
  };

  /**
   * the primary database when there are multiple databases
   */
  public static DBType           primary = DBType.MONGO;

  protected static final String  DEFAULT = "default";

  /** The conf. */
  protected static Configuration conf;

  /**
   * initialize the Bean with the configuration.
   * 
   * @param conf
   *          the conf
   */
  public static void init(Configuration conf) {

    RDB.init();

    Helper.conf = conf;

    if (RDSHelper.isConfigured() && MongoHelper.isConfigured()) {
      // need choose
      if ("mongo".equals(conf.getString("primary.db", "mongo"))) {
        primary = DBType.MONGO;
      } else {
        primary = DBType.RDS;
      }
    } else if (RDSHelper.isConfigured()) {
      primary = DBType.RDS;
    } else if (MongoHelper.isConfigured()) {
      primary = DBType.MONGO;
    } else {
      primary = DBType.NONE;
    }

    log.info("db.primary=" + primary + ", RDS=" + RDSHelper.isConfigured() + ", Mongo=" + MongoHelper.isConfigured());

  }

  /**
   * delete a data from database.
   *
   * @param id
   *          the value of "id"
   * @param t
   *          the subclass of Bean
   * @return the number was deleted
   */
  public static int delete(Object id, Class<? extends Bean> t) {
    return delete(W.create(X.ID, id), t);
  }

  /**
   * delete the data , return the number that was deleted.
   *
   * @param q
   *          the query
   * @param t
   *          the subclass of the Bean
   * @return the number was deleted
   */
  public static int delete(W q, Class<? extends Bean> t) {
    return delete(q, t, DEFAULT);
  }

  public static int delete(W q, Class<? extends Bean> t, String db) {
    String table = getTable(t);

    if (table != null) {
      if (monitor != null) {
        monitor.query(table, q);
      }

      if (primary == DBType.MONGO) {
        // insert into mongo
        return (int) MongoHelper.delete(table, q, db);
      } else if (primary == DBType.RDS) {
        // insert into RDS
        return RDSHelper.delete(table, q, db);
      } else {

        log.warn("no db configured, please configure the {giiwa}/giiwa.properites");
      }
    }
    return 0;
  }

  /**
   * test if exists for the object.
   *
   * @param id
   *          the value of "id"
   * @param t
   *          the subclass of Bean
   * @return true: exist
   * @throws SQLException
   *           throw exception if occur database error
   */
  public static boolean exists(Object id, Class<? extends Bean> t) throws SQLException {
    return exists(W.create(X.ID, id), t);
  }

  /**
   * test exists.
   *
   * @param q
   *          the query and order
   * @param t
   *          the class of Bean
   * @return true: exists, false: not exists
   * @throws SQLException
   *           throw Exception if the class declaration error or not db
   *           configured
   */
  public static boolean exists(W q, Class<? extends Bean> t) throws SQLException {
    return exists(q, t, DEFAULT);
  }

  public static boolean exists(W q, Class<? extends Bean> t, String db) throws SQLException {
    String table = getTable(t);

    if (table != null) {
      if (monitor != null) {
        monitor.query(table, q);
      }

      if (primary == DBType.MONGO) {
        // insert into mongo
        return MongoHelper.exists(table, q, db);
      } else if (primary == DBType.RDS) {
        // insert into RDS
        return RDSHelper.exists(table, q, db);
      }
    }
    throw new SQLException("no db configured, please configure the {giiwa}/giiwa.properites");
  }

  /**
   * Values in SQL, used to insert or update data both RDS and Mongo<br>
   * 
   */
  public static final class V {

    private final static Object   ignore = new Object();

    /** The list. */
    protected Map<String, Object> m      = new LinkedHashMap<String, Object>();

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
     * same as set(String name, Object v) <br>
     * Sets the value if not exists, ignored if name exists.
     * 
     * @param name
     *          the name
     * @param v
     *          the value object
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
     *          the name
     * @param v
     *          the value object
     * @return the V
     */
    public V append(String name, Object v) {
      return set(name, v);
    }

    /**
     * Ignore the fields.
     *
     * @param name
     *          the name
     * @return the v
     */
    public V ignore(String... name) {
      for (String s : name) {
        set(s, V.ignore);
      }
      return this;
    }

    /**
     * force set the name=value whatever the name exists or not
     * 
     * @param name
     *          the name
     * @param v
     *          the value object
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
     * Removes the.
     *
     * @param name
     *          the name
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
     * create and copy a new Value
     * 
     * @param v
     *          the Value object
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
   * the {@code W} Class used to create SQL "where" conditions<br>
   * this is for RDS and Mongo Query.
   *
   * @author joe
   */
  public final static class W {

    public enum OP {
      eq, gt, gte, lt, lte, like, neq, none
    };

    /**
     * "and"
     */
    private static final int AND   = 9;

    /**
     * "or"
     */
    private static final int OR    = 10;

    private List<W>          wlist = new ArrayList<W>();
    private List<Entity>     elist = new ArrayList<Entity>();
    private List<Entity>     order = new ArrayList<Entity>();

    private int              cond  = AND;

    private W() {
    }

    /**
     * remove the conditions from the query
     * 
     * @param names
     *          the names
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
      return elist == null ? X.EMPTY : (elist.toString() + "=>{" + where() + ", " + Helper.toString(args()) + "}");
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
     * create the SQL "where" with the tansfers
     * 
     * @param tansfers
     *          the words pair should be transfered
     * @return the SQL string
     */
    public String where(Map<String, String> tansfers) {
      StringBuilder sb = new StringBuilder();
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
     * create order string with the transfers
     * 
     * @param transfers
     *          the words pair that need transfer according database
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
     *          the name
     * @param v
     *          the v
     * @return W
     */
    public W and(String name, Object v) {
      return and(name, v, OP.eq);
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
     * get all keys
     * 
     * @return List keys
     */
    public List<String> keys() {
      List<String> list = new ArrayList<String>();

      if (elist.size() > 0 || wlist.size() > 0) {
        for (Entity e : elist) {
          list.add(e.name);
        }

        for (W w : wlist) {
          list.addAll(w.keys());
        }

        Collections.sort(list);
      }

      return list;
    }

    /**
     * set the namd and parameter with "op" conditions.
     *
     * @param name
     *          the name
     * @param v
     *          the value object
     * @param op
     *          the operation
     * @return the W
     */
    public W and(String name, Object v, OP op) {
      elist.add(new Entity(name, v, op, AND));
      return this;
    }

    /**
     * same as and(String name, Object v, OP op)
     * 
     * @param name
     *          the name
     * @param v
     *          the value object
     * @param op
     *          the operation
     * @return the W
     */
    public W append(String name, Object v, OP op) {
      return and(name, v, op);
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
      return or(name, v, OP.eq);
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
    public W or(String name, Object v, OP op) {

      elist.add(new Entity(name, v, op, OR));

      return this;
    }

    /**
     * copy the name and parameter from a JSON, with "and" and "op" conditions.
     *
     * @param jo
     *          the json
     * @param op
     *          the op
     * @param names
     *          the names
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
     * copy the value in jo, the format of name is: ["name", "table field name"
     * ].
     *
     * @param jo
     *          the json
     * @param op
     *          the op
     * @param names
     *          the names
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
     *          the name
     * @param v
     *          the v
     * @return W
     */
    public static W create(String name, Object v) {
      return create(name, v, OP.eq);
    }

    /**
     * create the W object with the parameters
     * 
     * @param name
     *          the field name
     * @param v
     *          the value object
     * @param op
     *          the operation
     * @return the W
     */
    public static W create(String name, Object v, OP op) {
      W w = new W();
      w.elist.add(new Entity(name, v, op, AND));
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

    public static class Entity {
      public String name;
      public Object value;
      public OP     op;   // operation EQ, GT, ...
      private int   cond; // condition AND, OR

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
     *          the e
     * @param q
     *          the q
     * @return the basic db object
     */
    BasicDBObject _parse(Entity e, BasicDBObject q) {
      OP op = e.op;
      if (op == OP.eq) {
        q.append(e.name, e.value);
      } else if (op == OP.gt) {
        q.append(e.name, new BasicDBObject("$gt", e.value));
      } else if (op == OP.gte) {
        q.append(e.name, new BasicDBObject("$gte", e.value));
      } else if (op == OP.lt) {
        q.append(e.name, new BasicDBObject("$lt", e.value));
      } else if (op == OP.lte) {
        q.append(e.name, new BasicDBObject("$lte", e.value));
      } else if (op == OP.like) {
        Pattern p1 = Pattern.compile(e.value.toString(), Pattern.CASE_INSENSITIVE);
        q.append(e.name, p1);
      } else if (op == OP.neq) {
        q.append(e.name, new BasicDBObject("$ne", e.value));
      }

      return q;
    }

    /**
     * Query.
     *
     * @return the basic db object
     */
    public BasicDBObject query() {
      BasicDBObject q = new BasicDBObject();
      for (Entity e : elist) {
        _parse(e, q);
      }

      for (W e : wlist) {
        // or the condition in here, is $or
        BasicDBList list = new BasicDBList();
        for (Entity e1 : e.elist) {
          list.add(_parse(e1, new BasicDBObject()));
        }

        q.append("$or", list);
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
     *          the name
     * @param i
     *          the i
     * @return the w
     */
    public W sort(String name, int i) {
      order.add(new Entity(name, i, OP.none, AND));
      return this;
    }

  }

  public static interface Monitor {

    /**
     * Query.
     *
     * @param table
     *          the table
     * @param w
     *          the w
     */
    public void query(String table, W w);
  }

  /**
   * load the data by id of X.ID
   * 
   * @param <T>
   *          the subclass of Bean
   * @param id
   *          the id
   * @param t
   *          the Class of Bean
   * @return the Bean
   */
  public static <T extends Bean> T load(Object id, Class<T> t) {
    return load(W.create(X.ID, id), t);
  }

  /**
   * load the data by the query.
   *
   * @param <T>
   *          the subclass of Bean
   * @param q
   *          the query
   * @param t
   *          the Class of Bean
   * @return the Bean
   */
  public static <T extends Bean> T load(W q, Class<T> t) {
    return load(q, t, DEFAULT);
  }

  public static <T extends Bean> T load(W q, Class<T> t, String db) {
    String table = getTable(t);
    return load(table, q, t, db);
  }

  /**
   * load data from the table.
   *
   * @param <T>
   *          the subclass of Bean
   * @param table
   *          the table name
   * @param q
   *          the query and order
   * @param t
   *          the subclass of Bean
   * @return the bean
   */
  public static <T extends Bean> T load(String table, W q, Class<T> t) {
    return load(table, q, t, DEFAULT);
  }

  public static <T extends Bean> T load(String table, W q, Class<T> t, String db) {

    if (table != null) {
      if (monitor != null) {
        monitor.query(table, q);
      }

      if (primary == DBType.MONGO) {
        // insert into mongo
        return MongoHelper.load(table, q, t, db);

      } else if (primary == DBType.RDS) {
        // insert into RDS
        return RDSHelper.load(table, q, t, db);
      } else {

        log.warn("no db configured, please configure the {giiwa}/giiwa.properites");
      }
    }
    return null;

  }

  /**
   * insert into the values by the Class of T.
   *
   * @param values
   *          the values
   * @param t
   *          the Class of Bean
   * @return the number of inserted, 0: failed
   */
  public static int insert(V values, Class<? extends Bean> t) {
    return insert(values, t, DEFAULT);
  }

  public static int insert(V values, Class<? extends Bean> t, String db) {
    String table = getTable(t);

    if (table != null) {
      values.set("created", System.currentTimeMillis()).set("updated", System.currentTimeMillis());
      if (primary == DBType.MONGO) {
        // insert into mongo
        return MongoHelper.insertCollection(table, values, db);
      } else if (primary == DBType.RDS) {

        // insert into RDS
        return RDSHelper.insertTable(table, values, db);
      } else {
        log.warn("no db configured, please configure the {giiwa}/giiwa.properites");
      }
    }

    return 0;

  }

  /**
   * update the values by the id, for the Class of Bean.
   *
   * @param id
   *          the id of the X.ID
   * @param values
   *          the values to update
   * @param t
   *          the Class of Bean
   * @return the number of updated
   */
  public static int update(Object id, V values, Class<? extends Bean> t) {
    return update(W.create(X.ID, id), values, t);
  }

  /**
   * update the values by the W for the Class of Bean.
   *
   * @param q
   *          the query
   * @param values
   *          the values to update
   * @param t
   *          the Class of Ban
   * @return the number of updated
   */
  public static int update(W q, V values, Class<? extends Bean> t) {
    return update(q, values, t, DEFAULT);
  }

  /**
   * update the values by the Q for the Class of Bean in the "db"
   * 
   * @param q
   *          the query
   * @param values
   *          the values
   * @param t
   *          the Class of Bean
   * @param db
   *          the database pool name
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
   *          the table
   * @param q
   *          the query
   * @param values
   *          the values
   * @return the number of updated
   */
  public static int update(String table, W q, V values) {
    return update(table, q, values, DEFAULT);
  }

  /**
   * update the table by the query with the values
   * 
   * @param table
   *          the table
   * @param q
   *          the query
   * @param values
   *          the values
   * @param db
   *          the database pool
   * @return the number of updated
   */
  public static int update(String table, W q, V values, String db) {

    if (table != null) {

      if (monitor != null) {
        monitor.query(table, q);
      }

      values.set("updated", System.currentTimeMillis());

      if (primary == DBType.MONGO) {
        // insert into mongo
        return (int) MongoHelper.updateCollection(table, q, values, db);

      } else if (primary == DBType.RDS) {
        // insert into RDS
        return RDSHelper.updateTable(table, q, values, db);
      } else {

        log.warn("no db configured, please configure the {giiwa}/giiwa.properites");
      }
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
    return RDSHelper.isConfigured() || MongoHelper.isConfigured();
  }

  /**
   * load the data from the table by query, ignore the table definition for the
   * Class.
   *
   * @param <T>
   *          the subclass of Bean
   * @param table
   *          the table name
   * @param q
   *          the query
   * @param s
   *          the start
   * @param n
   *          the number
   * @param t
   *          the Class of Bean
   * @return Beans of the T, the "total=-1" always
   */
  public static <T extends Bean> Beans<T> load(String table, W q, int s, int n, Class<T> t) {
    return load(table, q, s, n, t, DEFAULT);
  }

  public static <T extends Bean> Beans<T> load(String table, W q, int s, int n, Class<T> t, String db) {

    if (monitor != null) {
      monitor.query(table, q);
    }

    if (primary == DBType.MONGO) {
      // insert into mongo
      return MongoHelper.load(table, q.query(), q.order(), s, n, t, db);
    } else if (primary == DBType.RDS) {
      // insert into RDS
      return RDSHelper.load(table, q, s, n, t, db);
    }

    return null;

  }

  /**
   * load the data by query.
   *
   * @param <T>
   *          the subclass of Bean
   * @param q
   *          the query
   * @param s
   *          the start
   * @param n
   *          the number
   * @param t
   *          the Class of Bean
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
   *          the Class of Bean
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
   * count the data by the query.
   *
   * @param q
   *          the query
   * @param t
   *          the Class of Bean
   * @return the long of data number
   */
  public static long count(W q, Class<? extends Bean> t) {
    return count(q, t, DEFAULT);
  }

  /**
   * count the items in the db
   * 
   * @param q
   *          the query
   * @param t
   *          the Class of Bean
   * @param db
   *          the name of db pool
   * @return the number
   */
  public static long count(W q, Class<? extends Bean> t, String db) {
    String table = getTable(t);

    if (table != null) {
      if (monitor != null) {
        monitor.query(table, q);
      }

      if (primary == DBType.MONGO) {
        // insert into mongo
        return MongoHelper.count(table, q, db);

      } else if (primary == DBType.RDS) {
        // insert into RDS
        return RDSHelper.count(table, q, db);
      } else {
        log.warn("no db configured, please configure the {giiwa}/giiwa.properites");
      }
    }

    return 0;
  }

  /**
   * get the distinct list for the name, by the query.
   * 
   * @param <T>
   *          the base object
   * @param name
   *          the column name
   * @param q
   *          the query
   * @param b
   *          the Bean class
   * @param t
   *          the Class of T
   * @return the List of objects
   */
  public static <T> List<T> distinct(String name, W q, Class<? extends Bean> b, Class<T> t) {
    return distinct(name, q, b, t, DEFAULT);
  }

  @SuppressWarnings("unchecked")
  public static <T> List<T> distinct(String name, W q, Class<? extends Bean> b, Class<T> t, String db) {
    String table = getTable(b);

    if (table != null) {
      if (monitor != null) {
        monitor.query(table, q);
      }

      if (primary == DBType.MONGO) {
        // insert into mongo
        return MongoHelper.distinct(table, name, q, t, db);

      } else if (primary == DBType.RDS) {
        // insert into RDS
        return (List<T>) RDSHelper.distinct(table, name, q, db);
      } else {
        log.warn("no db configured, please configure the {giiwa}/giiwa.properites");
      }
    }

    return null;
  }

  /**
   * The main method.
   *
   * @param args
   *          the arguments
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

  }

  /**
   * 
   * @param m
   *          the monitor
   */
  public static void setMonitor(Monitor m) {
    monitor = m;
  }

}
