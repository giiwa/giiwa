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
package org.giiwa.framework.bean;

import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.DBMapping;
import org.giiwa.core.bean.X;

import com.mongodb.BasicDBObject;

// TODO: Auto-generated Javadoc
/**
 * the Appkey.
 * <br>
 * collection="gi_appkey"
 * 
 * @author wujun
 *
 */
@DBMapping(collection = "gi_appkey")
public class Appkey extends Bean {

  /**
  * 
  */
  private static final long serialVersionUID = 1L;

  public String getLogout() {
    return getString("logout");
  }

  public String getAppkey() {
    return getString("appkey");
  }

  public String getSecret() {
    return getString("secret");
  }

  /**
   * Creates the.
   * 
   * @param appkey
   *          the appkey
   * @param v
   *          the v
   * @return the int
   */
  public static int create(String appkey, V v) {
    if (!Bean.exists(new BasicDBObject().append(X.ID, appkey), Appkey.class)) {
      return Bean.insertCollection(
          v.set(X._ID, appkey).set("appkey", appkey).set("created", System.currentTimeMillis()), Appkey.class);
    }
    return 0;
  }

  /**
   * Load.
   * 
   * @param appkey
   *          the appkey
   * @return the app
   */
  public static Appkey load(String appkey) {
    return Bean.load(new BasicDBObject().append(X._ID, appkey), Appkey.class);
  }

  public boolean isLocked() {
    return getInt("locked") > 0;
  }

  public long getLastlogin() {
    return getLong("lastlogin");
  }

  /**
   * Update.
   * 
   * @param appkey
   *          the appkey
   * @param v
   *          the v
   * @return the int
   */
  public static int update(String appkey, V v) {
    return Bean.updateCollection(appkey, v, Appkey.class);
  }

  /**
   * Load.
   *
   * @param q
   *          the q
   * @param order
   *          the order
   * @param s
   *          the s
   * @param n
   *          the n
   * @return Beans
   */
  public static Beans<Appkey> load(BasicDBObject q, BasicDBObject order, int s, int n) {
    return Bean.load(q, order, s, n, Appkey.class);
  }

  /**
   * Delete.
   *
   * @param appkey
   *          the appkey
   */
  public static void delete(String appkey) {
    Bean.delete(new BasicDBObject(X._ID, appkey), Appkey.class);
  }

}
