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
public class ConfigGlobal extends Bean {

  /** The Constant serialVersionUID. */
  private static final long   serialVersionUID = 1L;

  Object                      var;

  private static ConfigGlobal owner            = new ConfigGlobal();

  public static ConfigGlobal getInstance() {
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
    ConfigGlobal c = getConfig(name);
    if (c != null) {
      return Bean.toInt(c.var);
    }

    c = Bean.load(new BasicDBObject(X._ID, name), ConfigGlobal.class);
    if (c != null) {
      data.put(name, c);
      return Bean.toInt(c.var);
    } else {
      c = new ConfigGlobal();
      c.var = conf.getInt(name, defaultValue);
      data.put(name, c);
      return Bean.toInt(c.var);
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
    ConfigGlobal c = getConfig(name);
    if (c != null) {
      return c.var;
    }

    c = Bean.load(new BasicDBObject(X._ID, name), ConfigGlobal.class);
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
    ConfigGlobal c = getConfig(name);
    if (c != null) {
      return Bean.toDouble(c.var);
    }

    c = Bean.load(new BasicDBObject(X._ID, name), ConfigGlobal.class);
    if (c != null) {
      data.put(name, c);
      return Bean.toDouble(c.var);
    } else {
      c = new ConfigGlobal();
      c.var = conf.getDouble(name, defaultValue);
      data.put(name, c);
      return Bean.toDouble(c.var);
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
    ConfigGlobal c = getConfig(name);
    if (c != null) {
      return c.var != null ? c.var.toString() : null;
    }

    c = Bean.load(new BasicDBObject(X._ID, name), ConfigGlobal.class);
    if (c != null) {
      data.put(name, c);
      return c.var != null ? c.var.toString() : null;
    } else {
      c = new ConfigGlobal();
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
    ConfigGlobal c = getConfig(name);
    if (c != null) {
      return Bean.toLong(c.var);
    }

    c = Bean.load(new BasicDBObject(X._ID, name), ConfigGlobal.class);
    if (c != null) {
      data.put(name, c);
      return Bean.toLong(c.var);
    } else {
      c = new ConfigGlobal();
      c.var = conf.getLong(name, defaultValue);
      data.put(name, c);
      return Bean.toLong(c.var);
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
      Bean.delete(new BasicDBObject(X._ID, name), ConfigGlobal.class);
      return;
    }

    if (Bean.exists(new BasicDBObject(X._ID, name), ConfigGlobal.class)) {
      Bean.updateCollection(name, V.create("var", o), ConfigGlobal.class);
    } else {
      Bean.insertCollection(V.create("var", o).set(X._ID, name), ConfigGlobal.class);
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
  private static ConfigGlobal getConfig(String name) {
    ConfigGlobal c = data.get(name);
    if (c != null && c.age() < X.AMINUTE * 10) {
      return c;
    }

    return null;
  }

  /** The data. */
  transient static private Map<String, ConfigGlobal> data = new HashMap<String, ConfigGlobal>();

}
