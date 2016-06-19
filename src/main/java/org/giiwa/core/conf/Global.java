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
package org.giiwa.core.conf;

import java.util.*;

import org.giiwa.core.bean.*;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

// TODO: Auto-generated Javadoc
/**
 * The Class SystemConfig is extended of Config, it can be "overrided" by module
 * or configured, it stored in database
 * 
 * @author yjiang
 */
@DBMapping(collection = "gi_config")
public class Global extends Bean {

  /** The Constant serialVersionUID. */
  private static final long   serialVersionUID = 1L;

  Object                      var;

  private static Global owner            = new Global();

  public static Global getInstance() {
    return owner;
  }

  /**
   * get the int value.
   *
   * @param name
   *          the name
   * @param defaultValue
   *          the default value
   * @return the int
   */
  public static int i(String name, int defaultValue) {
    Global c = getConfig(name);
    if (c != null) {
      return X.toInt(c.var, defaultValue);
    }

    c = Bean.load(new BasicDBObject(X._ID, name), Global.class);
    if (c != null) {
      data.put(name, c);
      return X.toInt(c.var, defaultValue);
    } else {
      c = new Global();
      c.var = conf.getInt(name, defaultValue);
      data.put(name, c);
      return X.toInt(c.var, defaultValue);
    }
  }

  /**
   * get the setting by name
   * 
   * @param name
   *          the name
   * @return Object of the value
   */
  public Object get(String name) {
    Global c = getConfig(name);
    if (c != null) {
      return c.var;
    }

    c = Bean.load(new BasicDBObject(X._ID, name), Global.class);
    if (c != null) {
      data.put(name, c);
      return c.var;
    }
    return X.EMPTY;
  }

  /**
   * get the double value.
   *
   * @param name
   *          the name
   * @param defaultValue
   *          the default value
   * @return the double
   */
  public static double d(String name, double defaultValue) {
    Global c = getConfig(name);
    if (c != null) {
      return X.toDouble(c.var, defaultValue);
    }

    c = Bean.load(new BasicDBObject(X._ID, name), Global.class);
    if (c != null) {
      data.put(name, c);
      return X.toDouble(c.var, defaultValue);
    } else {
      c = new Global();
      c.var = conf.getDouble(name, defaultValue);
      data.put(name, c);
      return X.toDouble(c.var, defaultValue);
    }
  }

  /**
   * get the string value.
   *
   * @param name
   *          the name
   * @param defaultValue
   *          the default value
   * @return the string
   */
  public static String s(String name, String defaultValue) {
    Global c = getConfig(name);
    if (c != null) {
      return c.var != null ? c.var.toString() : null;
    }

    c = Bean.load(new BasicDBObject(X._ID, name), Global.class);
    if (c != null) {
      data.put(name, c);
      return c.var != null ? c.var.toString() : null;
    } else {
      c = new Global();
      c.var = conf.getString(name, defaultValue);
      data.put(name, c);
      return c.var != null ? c.var.toString() : null;
    }
  }

  /**
   * get the long value.
   *
   * @param name
   *          the name
   * @param defaultValue
   *          the default value
   * @return the long
   */
  public static long l(String name, long defaultValue) {
    Global c = getConfig(name);
    if (c != null) {
      return X.toLong(c.var, defaultValue);
    }

    c = Bean.load(new BasicDBObject(X._ID, name), Global.class);
    if (c != null) {
      data.put(name, c);
      return X.toLong(c.var, defaultValue);
    } else {
      c = new Global();
      c.var = conf.getLong(name, defaultValue);
      data.put(name, c);
      return X.toLong(c.var, defaultValue);
    }
  }

  /**
   * get the current time.
   *
   * @return long of current time
   * @deprecated
   */
  public static long now() {
    return System.currentTimeMillis();
  }

  /**
   * Sets the value of the name in database, it will remove the configuration
   * value if value is null.
   *
   * @param name
   *          the name
   * @param o
   *          the value
   */
  public synchronized static void setConfig(String name, Object o) {
    if (X.isEmpty(name)) {
      return;
    }

    data.remove(name);

    if (o == null) {
      Bean.delete(new BasicDBObject(X._ID, name), Global.class);
      return;
    }

    try {
      if (Bean.exists(new BasicDBObject(X._ID, name), Global.class)) {
        Bean.updateCollection(name, V.create("var", o), Global.class);
      } else {
        Bean.insertCollection(V.create("var", o).set(X._ID, name), Global.class);
      }
    } catch (Exception e1) {
      log.error(e1.getMessage(), e1);
    }
  }

  @Override
  protected void load(DBObject d) {
    var = d.get("var");
  }

  /**
   * Gets the.
   * 
   * @param name
   *          the name
   * @return the system config
   */
  private static Global getConfig(String name) {
    Global c = data.get(name);
    if (c != null && c.age() < X.AMINUTE * 10) {
      return c;
    }

    return null;
  }

  /** The data. */
  transient static private Map<String, Global> data = new HashMap<String, Global>();

}
