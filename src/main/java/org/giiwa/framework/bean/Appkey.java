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
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.bean.Table;
import org.giiwa.core.bean.X;

// TODO: Auto-generated Javadoc
/**
 * the Appkey. <br>
 * collection="gi_appkey"
 * 
 * @author wujun
 *
 */
@Table(name = "gi_appkey")
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
    try {
      if (!Helper.exists(W.create(X.ID, appkey), Appkey.class)) {
        return Helper.insert(
            v.set(X._ID, appkey).set("appkey", appkey).set("created", System.currentTimeMillis()), Appkey.class);
      }
    } catch (Exception e1) {
      log.error(e1.getMessage(), e1);
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
    return Helper.load(W.create(X._ID, appkey), Appkey.class);
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
    return Helper.update(W.create(X._ID, appkey), v, Appkey.class);
  }

  /**
   * Load.
   *
   * @param q
   *          the query and order
   * @param s
   *          the start number
   * @param n
   *          the number of items
   * @return Beans
   */
  public static Beans<Appkey> load(W q, int s, int n) {
    return Helper.load(q, s, n, Appkey.class);
  }

  /**
   * Delete.
   *
   * @param appkey
   *          the appkey
   */
  public static void delete(String appkey) {
    Helper.delete(W.create(X._ID, appkey), Appkey.class);
  }

}
