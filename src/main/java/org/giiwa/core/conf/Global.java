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

import org.giiwa.core.bean.*;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;

/**
 * The Class Global is extended of Config, it can be "overrided" by module or
 * configured, it stored in database
 * 
 * @author yjiang
 */
@Table(name = "gi_config")
public class Global extends Bean {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1L;

  @Column(name = X.ID)
  String                    id;

  @Column(name = "s")
  String                    s;

  @Column(name = "i")
  int                       i;

  @Column(name = "l")
  long                      l;

  private static Global     owner            = new Global();

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
  public static int getInt(String name, int defaultValue) {
    Global c = Helper.load(W.create(X.ID, name), Global.class);
    if (c != null) {
      return X.toInt(c.i, defaultValue);
    } else {
      return Config.getConfig().getInt(name, defaultValue);
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
  public static String getString(String name, String defaultValue) {
    Global c = Helper.load(W.create(X.ID, name), Global.class);
    if (c != null) {
      return c.s != null ? c.s : defaultValue;
    } else {
      return Config.getConfig().getString(name, defaultValue);
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
  public static long getLong(String name, long defaultValue) {
    Global c = Helper.load(W.create(X.ID, name), Global.class);
    if (c != null) {
      return c.l;
    } else {
      return Config.getConfig().getLong(name, defaultValue);
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

    if (o == null) {
      Helper.delete(W.create(X.ID, name), Global.class);
      return;
    }

    try {
      V v = V.create();
      if (o instanceof Integer) {
        v.set("i", o);
      } else if (o instanceof Long) {
        v.set("l", o);
      } else {
        v.set("s", o.toString());
      }

      if (Helper.exists(W.create(X.ID, name), Global.class)) {
        Helper.update(W.create(X.ID, name), v, Global.class);
      } else {
        Helper.insert(v.set(X.ID, name), Global.class);
      }
    } catch (Exception e1) {
      log.error(e1.getMessage(), e1);
    }
  }

  public String get(String name) {
    return getString(name, null);
  }

}
