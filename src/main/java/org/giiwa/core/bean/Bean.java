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

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.cache.DefaultCachable;

import com.mongodb.DBObject;

import net.sf.json.JSONObject;

/**
 * The {@code Bean} Class is base class for all class that database access, it
 * almost includes all methods that need for database <br>
 * all data access MUST be inherited from it
 * 
 */
public abstract class Bean extends DefaultCachable implements Map<String, Object> {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 3L;

  /** The log. */
  protected static Log      log              = LogFactory.getLog(Bean.class);
  protected static Log      sqllog           = LogFactory.getLog("sql");

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
    if (data != null && data.size() > 0 && jo != null) {
      for (String name : data.keySet()) {
        Object o = data.get(name);
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
    return "Bean@" + data;
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
   * set the extra value.
   *
   * @param name
   *          the name
   * @param value
   *          the value
   */
  public final void set(String name, Object value) {
    if (data == null) {
      data = new HashMap<String, Object>();
    }

    data.put(name, value);

    // looking for all the fields
    Field f1 = _getField(name);
    if (f1 != null) {
      try {
        // log.debug("f1=" + f1 + ", value=" + value);
        f1.setAccessible(true);
        f1.set(this, value);
      } catch (Exception e) {
        log.error(name + "=" + value, e);
      }
      // } else {
      // log.debug("not found the column=" + name);
    }
  }

  private Field _getField(String columnname) {
    Class<? extends Bean> c1 = this.getClass();
    Map<String, Field> m = _fields.get(c1);
    if (m == null) {
      m = new HashMap<String, java.lang.reflect.Field>();

      Field[] ff = c1.getDeclaredFields();
      for (Field f : ff) {
        Column f1 = f.getAnnotation(Column.class);
        if (f1 != null) {
          m.put(f1.name(), f);
        }
      }

      _fields.put(c1, m);
    }

    return m.get(columnname);

  }

  private static Map<Class<? extends Bean>, Map<String, Field>> _fields = new HashMap<Class<? extends Bean>, Map<String, Field>>();

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
    if (data == null) {
      return null;
    }

    String s = name.toString();
    Field f = _getField(s);
    if (f != null) {
      try {
        return f.get(this);
      } catch (Exception e) {
        log.error(name, e);
      }
    }

    if (data.containsKey(s)) {
      return data.get(s);
    }

    String[] ss = s.split("\\.");
    Map<String, Object> m = data;
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
    if (data == null) {
      return null;
    }

    String n1 = name.toString();
    try {
      Class<?> c1 = this.getClass();
      java.lang.reflect.Field f1 = c1.getField(n1);
      if (f1 != null) {
        return f1.get(this);
      }
    } catch (Exception e) {

    }

    if (data.containsKey(name.toString())) {
      Object o = data.get(name.toString());
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
    return data == null ? 0 : data.size();
  }

  /**
   * test is empty bean
   */
  @Override
  public final boolean isEmpty() {
    return data == null ? true : data.isEmpty();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Map#containsKey(java.lang.Object)
   */
  @Override
  public final boolean containsKey(Object key) {
    return data == null ? false : data.containsKey(key);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Map#containsValue(java.lang.Object)
   */
  @Override
  public final boolean containsValue(Object value) {
    return data == null ? false : data.containsValue(value);
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
    return data == null ? null : data.remove(key);
  }

  /**
   * put all the data in the map to Bean.
   *
   * @param m
   *          the m
   */
  @Override
  public final void putAll(Map<? extends String, ? extends Object> m) {
    for (String s : m.keySet()) {
      set(s, m.get(s));
    }
  }

  /**
   * remove all data from the bean.
   */
  @Override
  public final void clear() {
    if (data != null) {
      data.clear();
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
    if (data != null) {
      names.addAll(data.keySet());

      for (String s : data.keySet()) {
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
    return data == null ? null : data.values();
  }

  /**
   * get all the Entries except "_obj" field.
   *
   * @return the sets the
   */
  @Override
  public final Set<Entry<String, Object>> entrySet() {
    Set<Entry<String, Object>> ss = new HashSet<Entry<String, Object>>();
    if (data != null) {
      for (Entry<String, Object> e : data.entrySet()) {
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
    return X.toInt(get(name), 0);
  }

  /**
   * by default, get long from the map.
   *
   * @param name
   *          the name
   * @return long
   */
  public long getLong(String name) {
    return X.toLong(get(name), 0);
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
    return X.toFloat(get(name), 0);
  }

  /**
   * by default, get the double from the map.
   *
   * @param name
   *          the name
   * @return double
   */
  public final double getDouble(String name) {
    return X.toDouble(get(name), 0);
  }

  /**
   * get all extra value
   * 
   * @return Map
   */
  public Map<String, Object> getAll() {
    return data;
  }

  /**
   * remove all extra value.
   */
  public final void removeAll() {
    if (data != null) {
      data.clear();
    }
  }

  /**
   * remove value by names.
   *
   * @param names
   *          the names
   */
  public final void remove(String... names) {
    if (data != null && names != null) {
      for (String name : names) {
        data.remove(name);
      }
    }
  }

  private Map<String, Object> data = null;

  /**
   * create the data as json.<br>
   * drop all the data which the name endsWith("_obj")
   * 
   * @return JSONObject
   */
  @SuppressWarnings("unchecked")
  public final JSONObject getJSON() {
    if (data == null) {
      return null;
    }

    JSONObject jo = new JSONObject();

    toJSON(jo);

    return jo;
  }

  /**
   * Load by default, get all columns to a map<br>
   * it will invoked when load data from the MongoDB<br>
   * by default, will load all data in Bean Map.
   * 
   * @param d
   *          the DBObject
   */
  protected void load(DBObject d) {

    for (String name : d.keySet()) {
      this.set(name, d.get(name));
    }

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

      String name = m.getColumnName(i);
      this.set(name, o);

    }
  }

}
