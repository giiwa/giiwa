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
package org.giiwa.core.conf;

import java.util.HashMap;
import java.util.Map;

import org.giiwa.core.bean.*;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.task.Task;
import org.giiwa.framework.web.Model;

/**
 * The Class Global is extended of Config, it can be "overrided" by module or
 * configured, it stored in database
 * 
 * @author yjiang
 */
@Table(name = "gi_config")
public final class Global extends Bean {

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
    if (!Helper.isConfigured()) {
      return X.toInt(cache.get(name), defaultValue);
    }

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
    if (!Helper.isConfigured()) {
      return cache.get(name) == null ? defaultValue : cache.get(name).toString();
    }

    Global c = Helper.load(W.create(X.ID, name), Global.class);
    if (c != null) {
      return c.s != null ? c.s : defaultValue;
    } else {
      return Config.getConfig().getString(name, defaultValue);
    }
  }

  private static Map<String, Object> cache = new HashMap<String, Object>();

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
    if (!Helper.isConfigured()) {
      return X.toLong(cache.get(name), defaultValue);
    }

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

    if (!Helper.isConfigured()) {
      cache.put(name, o);
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

  /**
   * Gets the string value
   *
   * @param name
   *          the name
   * @return the string
   */
  public String get(String name) {
    return getString(name, null);
  }

  /**
   * Lock a global lock
   *
   * @param name
   *          the name of lock
   * @param timeout
   *          the timeout
   * @return true, if successful
   */
  public static synchronized boolean lock(String name, long timeout) {

    name = "lock." + name;
    heartbeat.schedule(10);

    if (locked.containsKey(name)) {
      return true;
    }

    if (timeout <= 0) {
      return false;
    }

    try {
      long now = System.currentTimeMillis();

      String node = Model.node();

      Global f = Helper.load(name, Global.class);

      if (f == null) {
        String linkid = UID.random();

        Helper.insert(V.create(X.ID, name).set("s", node).set("linkid", linkid), Global.class);
        f = Helper.load(name, Global.class);
        if (f == null) {
          log.error("occur error when create unique id, name=" + name);
          return false;
        } else if (!X.isSame(f.getString("linkid"), linkid)) {
          synchronized (name) {
            name.wait(1000);
          }
          return lock(name, timeout - System.currentTimeMillis() + now);
        }

        locked.put(name, Thread.currentThread());

        return true;
      } else {
        String s = f.getString("s");
        // 10 seconds
        if (X.isEmpty(s) || System.currentTimeMillis() - f.getUpdated() > 10000) {
          if (Helper.update(W.create(X.ID, name).and("s", s), V.create("s", node), Global.class) > 0) {
            locked.put(name, Thread.currentThread());

            return true;
          } else {
            synchronized (name) {
              name.wait(1000);
            }
            return lock(name, timeout - System.currentTimeMillis() + now);
          }
        }
      }

    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    return false;
  }

  /**
   * Release the global lock
   *
   * @param name
   *          the name of lock
   * @return true, if successful
   */
  public static synchronized boolean release(String name) {
    name = "lock." + name;
    try {
      String node = Model.node();
      return Helper.update(W.create(X.ID, name).and("s", node), V.create("s", X.EMPTY), Global.class) > 0;

    } finally {
      locked.remove(name);
    }
  }

  private static Task heartbeat = new LockHeartbeat();

  private static class LockHeartbeat extends Task {

    @Override
    public void onExecute() {
      if (locked.size() > 0) {
        String[] names = locked.keySet().toArray(new String[locked.size()]);
        String node = Model.node();

        for (String name : names) {
          if (Helper.update(W.create(X.ID, name).and("s", node), V.create("updated", System.currentTimeMillis()),
              Global.class) <= 0) {
            // the lock has been acquired by other
            Thread t = locked.get(name);
            t.interrupt();
            locked.remove(name);
          }
        }
      }
    }

    @Override
    public void onFinish() {
      this.schedule(3000);
    }

  }

  private static Map<String, Thread> locked = new HashMap<String, Thread>();

}
