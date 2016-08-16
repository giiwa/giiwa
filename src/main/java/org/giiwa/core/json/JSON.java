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
package org.giiwa.core.json;

import java.io.Reader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.X;

import com.google.gson.Gson;

/**
 * The Class JSON, simple JSON object, using Gson to parse and format
 *
 * @author wujun
 */
public class JSON extends HashMap<String, Object> {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private static Log        log              = LogFactory.getLog(JSON.class);

  /**
   * From object.
   *
   * @param json
   *          the json
   * @return the json
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static JSON fromObject(Object json) {
    JSON j = null;
    if (json instanceof Map) {
      j = JSON.create((Map) json);
    } else if (json instanceof String) {
      Gson g = new Gson();
      j = g.fromJson((String) json, JSON.class);
    } else if (json instanceof Reader) {
      Gson g = new Gson();
      j = g.fromJson((Reader) json, JSON.class);
    } else if (json instanceof byte[]) {
      Gson g = new Gson();
      byte[] b1 = (byte[]) json;
      j = g.fromJson(new String(b1), JSON.class);
    }

    if (j != null) {
      for (String name : j.keySet().toArray(new String[j.size()])) {
        Object o = j.get(name);
        if (o == null) {
          j.remove(name);
        } else if (o instanceof List) {
          j.put(name, fromObjects(o));
        } else if (o instanceof Map) {
          j.put(name, fromObject(o));
        }
      }
    }

    return j;
  }

  /**
   * From objects.
   *
   * @param jsons
   *          the jsons
   * @return the list
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static List<JSON> fromObjects(Object jsons) {
    List list = null;
    if (jsons instanceof List) {
      list = (List) jsons;
    } else if (jsons instanceof String) {
      Gson g = new Gson();
      list = g.fromJson((String) jsons, List.class);
    } else if (jsons instanceof Reader) {
      Gson g = new Gson();
      list = g.fromJson((Reader) jsons, List.class);
    } else if (jsons instanceof byte[]) {
      Gson g = new Gson();
      byte[] b1 = (byte[]) jsons;
      list = g.fromJson(new String(b1), List.class);
    }

    if (list != null) {
      for (int i = 0; i < list.size(); i++) {
        Object o = list.get(i);
        if (o instanceof List) {
          list.set(i, fromObjects(o));
        } else {
          list.set(i, fromObject(o));
        }
      }
    }
    return list;
  }

  @SuppressWarnings("rawtypes")
  public static <T> T fromObject(Object json, Class<T> t) {
    if (json instanceof String) {
      Gson g = new Gson();
      return g.fromJson((String) json, t);
    } else if (json instanceof Reader) {
      Gson g = new Gson();
      return g.fromJson((Reader) json, t);
    } else if (json instanceof byte[]) {
      Gson g = new Gson();
      byte[] b1 = (byte[]) json;
      return g.fromJson(new String(b1), t);
    } else if (json instanceof Map) {
      try {
        Map m = (Map) json;
        T t1 = t.newInstance();
        Field[] fs = t.getDeclaredFields();

        for (Field f : fs) {
          String name = f.getName();
          if (m.containsKey(name)) {
            f.setAccessible(true);
            f.set(t1, m.get(name));
          }
        }
        return t1;
      } catch (Exception e) {
        log.error(json, e);
      }
    }
    return null;
  }

  /**
   * Creates the.
   *
   * @return the json
   */
  public static JSON create() {
    return new JSON();
  }

  @Override
  public String toString() {
    Gson g = new Gson();
    return g.toJson(this);
  }

  /**
   * Creates the.
   *
   * @param m
   *          the m
   * @return the json
   */
  public static JSON create(Map<String, Object> m) {
    JSON j = create();
    j.putAll(m);
    return j;
  }

  /**
   * Checks for.
   *
   * @param name
   *          the name
   * @return true, if successful
   */
  public boolean has(String name) {
    return this.containsKey(name);
  }

  /**
   * Gets the string.
   *
   * @param name
   *          the name
   * @return the string
   */
  public String getString(String name) {
    return getString(name, X.EMPTY);
  }

  /**
   * Gets the string.
   *
   * @param name
   *          the name
   * @param defaultValue
   *          the default value
   * @return the string
   */
  public String getString(String name, String defaultValue) {
    Object v = this.get(name);
    if (v == null) {
      return defaultValue;
    }
    return v.toString();
  }

  /**
   * Gets the int.
   *
   * @param name
   *          the name
   * @return the int
   */
  public int getInt(String name) {
    return getInt(name, 0);
  }

  /**
   * Gets the int.
   *
   * @param name
   *          the name
   * @param defaultValue
   *          the default value
   * @return the int
   */
  public int getInt(String name, int defaultValue) {
    return X.toInt(this.get(name), defaultValue);
  }

  /**
   * Gets the long.
   *
   * @param name
   *          the name
   * @return the long
   */
  public long getLong(String name) {
    return getLong(name, 0);
  }

  /**
   * Gets the long.
   *
   * @param name
   *          the name
   * @param defaultValue
   *          the default value
   * @return the long
   */
  public long getLong(String name, long defaultValue) {
    return X.toLong(this.get(name), defaultValue);
  }

  /**
   * Gets the float.
   *
   * @param name
   *          the name
   * @return the float
   */
  public float getFloat(String name) {
    return getFloat(name, 0f);
  }

  /**
   * Gets the float.
   *
   * @param name
   *          the name
   * @param defaultValue
   *          the default value
   * @return the float
   */
  public float getFloat(String name, float defaultValue) {
    return X.toFloat(this.get(name), defaultValue);
  }

  /**
   * Gets the double.
   *
   * @param name
   *          the name
   * @return the double
   */
  public double getDouble(String name) {
    return getDouble(name, 0);
  }

  /**
   * Gets the double.
   *
   * @param name
   *          the name
   * @param defaultValue
   *          the default value
   * @return the double
   */
  public double getDouble(String name, double defaultValue) {
    return X.toDouble(this.get(name), defaultValue);
  }

  /**
   * Gets the list.
   *
   * @param name
   *          the name
   * @return the list
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public List<JSON> getList(String name) {
    Object o = this.get(name);
    if (o != null && o instanceof List) {
      return (List) o;
    }
    return null;
  }

  /**
   * Gets the objects.
   *
   * @param name
   *          the name
   * @return the objects
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public List<Object> getObjects(String name) {
    Object o = this.get(name);
    if (o != null && o instanceof List) {
      return (List) o;
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
    String ss = "{a:'a',b:1}";
    JSON j = JSON.fromObject(ss);
    System.out.println(j);

    ss = "{a:'a',b:1, c:{a:1, b:'a'}}";
    j = JSON.fromObject(ss);
    System.out.println(j);

    ss = "[{a:'a',b:1}]";
    List<JSON> l1 = JSON.fromObjects(ss);
    System.out.println(l1);

  }

}
