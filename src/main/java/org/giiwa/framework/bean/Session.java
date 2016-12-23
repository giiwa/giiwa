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
package org.giiwa.framework.bean;

import java.io.Serializable;
import java.util.*;

import org.apache.commons.logging.*;
import org.giiwa.core.bean.X;
import org.giiwa.core.cache.*;
import org.giiwa.core.conf.Global;

/**
 * Session of http request
 * 
 * @author yjiang
 * 
 */
public class Session implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  static Log                log              = LogFactory.getLog(Session.class);

  String                    sid;

  Map<String, Object>       a                = new TreeMap<String, Object>();

  long                      expired          = -1;

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return new StringBuilder("Session@{sid=").append(sid).append(",data=").append(a).append("}").toString();
  }

  /**
   * Exists.
   * 
   * @param sid
   *          the sid
   * @return true, if successful
   */
  public static boolean exists(String sid) {
    Session o = Cache.get("session/" + sid);
    return o != null && !o.expired();
  }

  /**
   * Delete.
   * 
   * @param sid
   *          the sid
   */
  public static void delete(String sid) {
    Cache.remove("session/" + sid);
  }

  /**
   * Load.
   * 
   * @param sid
   *          the sid
   * @return the session
   */
  public static Session load(String sid) {

    Session o = (Session) Cache.get("session/" + sid);

    if (o == null || o.expired()) {
      o = new Session();

      /**
       * set the session expired time
       */
      o.sid = sid;

    }

    return o;
  }

  /**
   * 
   * @return boolean
   */
  public boolean expired() {
    return expired > 0 && System.currentTimeMillis() > expired;
  }

  /**
   * Checks for.
   * 
   * @param key
   *          the key
   * @return true, if successful
   */
  public boolean has(String key) {
    return a.containsKey(key);
  }

  /**
   * Removes the.
   * 
   * @param key
   *          the key
   * @return the session
   */
  public Session remove(String key) {
    a.remove(key);
    return this;
  }

  /**
   * Store the session with configured expired
   * 
   * @return the session
   */
  public Session store() {
    long expired = Global.getLong("session.alive", X.AWEEK);
    if (expired > 0) {
      expired = System.currentTimeMillis() + expired;
    } else {
      expired = -1;
    }
    return store(expired);
  }

  /**
   * store the session with the expired
   * 
   * @param expired
   *          the expired timestamp, ms in future
   * @return Session
   */
  public Session store(long expired) {

    this.expired = expired;

    if (!Cache.set("session/" + sid, this)) {
      log.error("set session failed !", new Exception("store session failed"));
    }

    return this;
  }

  /**
   * Sets the.
   * 
   * @param key
   *          the key
   * @param o
   *          the o
   * @return the session
   */
  public Session set(String key, Object o) {
    a.put(key, o);
    return this;
  }

  /**
   * Sid.
   * 
   * @return the string
   */
  public String sid() {
    return sid;
  }

  /**
   * Gets the.
   * 
   * @param key
   *          the key
   * @return the object
   */
  public Object get(String key) {
    return (Object) a.get(key);
  }

  /**
   * Gets the int.
   * 
   * @param key
   *          the key
   * @return the int
   */
  public int getInt(String key) {
    Integer i = (Integer) a.get(key);
    if (i != null) {
      return i;
    }
    return 0;
  }

  /**
   * Clear.
   */
  public void clear() {
    a.clear();
  }

}
