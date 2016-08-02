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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.db.DB;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

import net.sf.json.JSONObject;

/**
 * The {@code Bean} Class is base class for all class that database access, it
 * almost includes all methods that need for database <br>
 * all data access MUST be inherited from it
 * 
 */
public class Helper {

  /** The log. */
  protected static Log log    = LogFactory.getLog(Helper.class);
  protected static Log sqllog = LogFactory.getLog("sql");

  public static enum DBType {
    MONGO, RDS, NONE;
  };

  /** The conf. */
  protected static Configuration conf;

  /**
   * indicated whether is debug model
   */
  public static boolean          DEBUG = true;

  /**
   * initialize the Bean with the configuration.
   * 
   * @param conf
   *          the conf
   */
  public static void init(Configuration conf) {

    DB.init();

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

  }

  public static DBType primary = DBType.MONGO;

  public static int delete(Object id, Class<? extends Bean> t) {
    return delete(W.create(X._ID, id), t);
  }

  /**
   * delete the data , return the number that was deleted
   * 
   * @param q
   *          the query and order
   * @param t
   *          the Class of the Bean
   * @return int
   */
  public static int delete(W q, Class<? extends Bean> t) {
    String table = getTable(t);

    if (table != null) {
      if (primary == DBType.MONGO) {
        // insert into mongo
        return MongoHelper.delete(table, q.query());
      } else if (primary == DBType.RDS) {
        // insert into RDS
        return RDSHelper.delete(table, q.where(), q.args());
      } else {

        log.warn("no db configured, please configure the {giiwa}/giiwa.properites");
      }
    }
    return 0;
  }

  public static boolean exists(Object id, Class<? extends Bean> t) throws Exception {
    return exists(W.create(X._ID, id), t);
  }

  /**
   * test exists
   * 
   * @param q
   *          the query and order
   * @param t
   *          the class of Bean
   * @return true: exists, false: not exists
   * @throws Exception
   *           throw Exception if the class declaration error or not db
   *           configured
   */
  public static boolean exists(W q, Class<? extends Bean> t) throws Exception {
    String table = getTable(t);

    if (table != null) {
      if (primary == DBType.MONGO) {
        // insert into mongo
        return MongoHelper.exists(table, q.query());
      } else if (primary == DBType.RDS) {
        // insert into RDS
        return RDSHelper.exists(table, q.where(), q.args());
      }
    }
    throw new Exception("no db configured, please configure the {giiwa}/giiwa.properites");
  }

  /**
   * Values in SQL, used to insert or update data both RDBS and Mongo<br>
   * 
   */
  public static final class V {

    /** The list. */
    protected Map<String, Object> m = new HashMap<String, Object>();

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
   * this is for RDBS Query anly
   * 
   * @author joe
   * 
   */
  public final static class W {

    /***
     * "="
     */
    public static final int  OP_EQ   = 0;

    /**
     * "&gt;"
     */
    public static final int  OP_GT   = 1;

    /**
     * "&gt;="
     */
    public static final int  OP_GTE  = 2;

    /**
     * "&lt;"
     */
    public static final int  OP_LT   = 3;

    /**
     * "&lt;="
     */
    public static final int  OP_LTE  = 4;

    /**
     * "like"
     */
    public static final int  OP_LIKE = 5;

    /**
     * "!="
     */
    public static final int  OP_NEQ  = 7;

    /**
     * ""
     */
    public static final int  OP_NONE = 8;

    /**
     * "and"
     */
    private static final int AND     = 9;

    /**
     * "or"
     */
    private static final int OR      = 10;

    private List<W>          wlist   = new ArrayList<W>();
    private List<Entity>     elist   = new ArrayList<Entity>();
    private List<Entity>     order   = new ArrayList<Entity>();

    private int              cond    = AND;

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

    public List<Entity> getAll() {
      return elist;
    }

    /**
     * create the SQL "where".
     *
     * @return String
     */
    public String where() {
      if (elist.size() > 0 || wlist.size() > 0) {
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

        return sb.toString();
      }

      return null;
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

      if (order.size() > 0 || order.size() > 0) {
        StringBuilder sb = new StringBuilder("order by ");
        for (int i = 0; i < order.size(); i++) {
          Entity e = order.get(i);
          if (i > 0) {
            sb.append(",");
          }
          sb.append(e.name);
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
          case OP_GTE: {
            sb.append(">=?");
            break;
          }
          case OP_LT: {
            sb.append("<?");
            break;
          }
          case OP_LTE: {
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
            case OP_GTE: {
              s.append(">=");
              break;
            }
            case OP_LT: {
              s.append("<");
              break;
            }
            case OP_LTE: {
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

    BasicDBObject _parse(Entity e, BasicDBObject q) {
      switch (e.op) {
        case W.OP_EQ:
          q.append(e.name, e.value);
          break;
        case W.OP_GT:
          q.append(e.name, new BasicDBObject("$gt", e.value));
          break;
        case W.OP_GTE:
          q.append(e.name, new BasicDBObject("$gte", e.value));
          break;
        case W.OP_LIKE:
          Pattern p1 = Pattern.compile(e.value.toString(), Pattern.CASE_INSENSITIVE);
          q.append(e.name, p1);
          break;
        case W.OP_LT:
          q.append(e.name, new BasicDBObject("$lt", e.value));
          break;
        case W.OP_LTE:
          q.append(e.name, new BasicDBObject("$lte", e.value));
          break;
        case W.OP_NEQ:
          q.append(e.name, new BasicDBObject("$ne", e.value));
          break;
      }
      return q;
    }

    public BasicDBObject query() {
      BasicDBObject q = new BasicDBObject();
      if (elist.size() > 0) {
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
      }

      return q;
    }

    public BasicDBObject order() {
      BasicDBObject q = new BasicDBObject();
      if (order.size() > 0 || order.size() > 0) {
        for (Entity e : order) {
          q.append(e.name, e.value);
        }
      }

      return q;
    }

    public W sort(String name, int i) {
      order.add(new Entity(name, i, 0, 0));
      return this;
    }

  }

  public static <T extends Bean> T load(Object id, Class<T> t) {
    return load(W.create(X._ID, id), t);
  }

  public static <T extends Bean> T load(W q, Class<T> t) {
    String table = getTable(t);

    if (table != null) {
      if (primary == DBType.MONGO) {
        // insert into mongo
        return MongoHelper.load(table, q.query(), q.order(), t);

      } else if (primary == DBType.RDS) {
        // insert into RDS
        return RDSHelper.load(table, q.where(), q.args(), q.orderby(), t);
      } else {

        log.warn("no db configured, please configure the {giiwa}/giiwa.properites");
      }
    }
    return null;

  }

  public static int insert(V values, Class<? extends Bean> t) {
    String table = getTable(t);

    if (table != null) {
      if (primary == DBType.MONGO) {
        // insert into mongo
        return MongoHelper.insertCollection(table, values);
      } else if (primary == DBType.RDS) {

        // insert into RDS
        return RDSHelper.insertTable(table, values);
      } else {
        log.warn("no db configured, please configure the {giiwa}/giiwa.properites");
      }
    }

    return 0;
  }

  public static int update(Object id, V values, Class<? extends Bean> t) {
    return update(W.create(X._ID, id), values, t);
  }

  public static int update(W q, V values, Class<? extends Bean> t) {
    String table = getTable(t);

    if (table != null) {
      if (primary == DBType.MONGO) {
        // insert into mongo
        return MongoHelper.updateCollection(table, q.query(), values);

      } else if (primary == DBType.RDS) {
        // insert into RDS
        return RDSHelper.updateTable(table, q.where(), q.args(), values);
      } else {

        log.warn("no db configured, please configure the {giiwa}/giiwa.properites");
      }
    }
    return 0;
  }

  public static boolean isConfigured() {
    return RDSHelper.isConfigured() || MongoHelper.isConfigured();
  }

  public static <T extends Bean> Beans<T> load(String table, W q, int s, int n, Class<T> t) {
    if (primary == DBType.MONGO) {
      // insert into mongo
      return MongoHelper.load(table, q.query(), q.order(), s, n, t);
    } else if (primary == DBType.RDS) {
      // insert into RDS
      return RDSHelper.load(table, q.where(), q.args(), q.orderby(), s, n, t);
    }

    return null;
  }

  public static <T extends Bean> Beans<T> load(W q, int s, int n, Class<T> t) {
    String table = getTable(t);
    return load(table, q, s, n, t);
  }

  public static String getTable(Class<? extends Bean> t) {
    Table table = (Table) t.getAnnotation(Table.class);
    if (table == null || X.isEmpty(table.name())) {
      log.error("table missed/error in [" + t + "] declaretion");
      return null;
    }

    return table.name();
  }

  public static long count(W q, Class<? extends Bean> t) {
    String table = getTable(t);

    if (table != null) {
      if (primary == DBType.MONGO) {
        // insert into mongo
        return MongoHelper.count(table, q.query());

      } else if (primary == DBType.MONGO) {
        // insert into RDS
        return RDSHelper.count(table, q.where(), q.args());
      } else {
        log.warn("no db configured, please configure the {giiwa}/giiwa.properites");
      }
    }

    return 0;
  }

  public static List<Object> distinct(String name, W q, Class<? extends Bean> t) {
    String table = getTable(t);

    if (table != null) {
      if (primary == DBType.MONGO) {
        // insert into mongo
        return MongoHelper.distinct(table, name, q.query());

      } else if (primary == DBType.MONGO) {
        // insert into RDS
        return RDSHelper.distinct(table, name, q.where(), q.args());
      } else {
        log.warn("no db configured, please configure the {giiwa}/giiwa.properites");
      }
    }

    return null;
  }

  public static void main(String[] args) {

    W w = W.create("name", 1).sort("name", 1).sort("nickname", -1).sort("ddd", 1);
    w.and("aaa", 2);
    W w1 = W.create("a", 1).or("b", 2);
    w.and(w1);

    System.out.println(w.where());
    System.out.println(Bean.toString(w.args()));
    System.out.println(w.orderby());

    System.out.println("-----------");
    System.out.println(w.query());
    System.out.println(w.order());

  }
}
