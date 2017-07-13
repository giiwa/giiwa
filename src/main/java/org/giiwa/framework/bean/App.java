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

import java.sql.SQLException;
import java.util.Base64;

import org.giiwa.core.base.Digest;
import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Column;
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.bean.Table;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;
import org.giiwa.core.cache.Cache;
import org.giiwa.core.json.JSON;

// TODO: Auto-generated Javadoc
/**
 * The App bean, used to store appid and secret table="gi_app"
 * 
 * @author wujun
 *
 */
@Table(name = "gi_app")
public class App extends Bean {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  @Column(name = X.ID, index = true, unique = true)
  private long              id;

  @Column(name = "appid", index = true, unique = true)
  private String            appid;

  @Column(name = "memo")
  private String            memo;

  @Column(name = "secret")
  private String            secret;

  @Column(name = "ip")
  private String            ip;

  @Column(name = "lastime")
  private long              lastime;

  @Column(name = "expired")
  private long              expired;

  @Column(name = "role")
  private long              role;

  public void touch(String ip) {
    update(appid, V.create("lastime", System.currentTimeMillis()).set("ip", ip));
  }

  public long getId() {
    return id;
  }

  public String getMemo() {
    return memo;
  }

  public boolean hasAccess(String... name) {
    Role r = getRole_obj();
    if (r != null) {
      for (String s : name) {
        if (r.has(s))
          return true;
      }
    }
    return false;
  }

  transient Role role_obj;

  public Role getRole_obj() {
    if (role_obj == null) {
      role_obj = Role.loadById(role);
    }
    return role_obj;
  }

  public long getRole() {
    return role;
  }

  public String getAppid() {
    return appid;
  }

  public String getSecret() {
    return secret;
  }

  public String getIp() {
    return ip;
  }

  public long getLastime() {
    return lastime;
  }

  public long getExpired() {
    return expired;
  }

  /**
   * data = Base64(AES(params)) <br>
   * decode, params=AES(Base64(data));
   * 
   * @param data
   *          the data
   * @return JSON
   */
  public JSON parseParameters(String data) {
    try {
      byte[] bb = Base64.getDecoder().decode(data);

      data = new String(Digest.aes_decrypt(bb, secret));
      String[] ss = X.split(data, "&");
      JSON jo = JSON.create();
      for (String s : ss) {
        String[] s1 = X.split(s, "=");
        if (s1.length == 2) {
          jo.put(s1[0], s1[1]);
        }
      }
      return jo;
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return null;
  }

  /**
   * data = Base64(AES(params)) <br>
   * decode, params=AES(Base64(data));
   * 
   * @param jo
   *          the json data
   * @param secret
   *          the secret
   * @return the string
   */
  public static String generateParameter(JSON jo, String secret) {
    try {
      StringBuilder sb = new StringBuilder();
      for (String name : jo.keySet()) {
        if (sb.length() > 0) {
          sb.append("&");
        }
        sb.append(name).append("=").append(jo.getString(name));
      }

      byte[] bb = Digest.aes_encrypt(sb.toString().getBytes(), secret);
      String data = Base64.getEncoder().encodeToString(bb);
      return data;
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return null;
  }

  /**
   * Exists.
   *
   * @param appid
   *          the appid
   * @return true, if successful
   * @throws SQLException
   *           the SQL exception
   */
  public static boolean exists(String appid) throws SQLException {
    return Helper.exists(W.create("appid", appid), App.class);
  }

  /**
   * Creates the.
   *
   * @param v
   *          the v
   * @return the int
   */
  public static int create(V v) {
    try {
      long id = UID.next("app.id");
      if (Helper.exists(id, App.class)) {
        id = UID.next("app.id");
      }
      return Helper.insert(v.set(X.ID, id), App.class);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return -1;
  }

  /**
   * Load.
   *
   * @param id
   *          the id
   * @return the app
   */
  public static App load(long id) {
    return Helper.load(id, App.class);
  }

  /**
   * Delete.
   *
   * @param appid
   *          the appid
   */
  public static void delete(String appid) {
    Cache.remove("app/" + appid);
    Helper.delete(W.create("appid", appid), App.class);
  }

  /**
   * Load.
   *
   * @param appid
   *          the appid
   * @return the app
   */
  public static App load(String appid) {
    App a = Cache.get("app/" + appid);
    if (a == null || a.expired()) {
      a = load(W.create("appid", appid));
      a.setExpired(System.currentTimeMillis() + X.AMINUTE);
      Cache.set("app/" + appid, a);
    }
    return a;
  }

  /**
   * Load.
   *
   * @param q
   *          the q
   * @return the app
   */
  public static App load(W q) {
    return Helper.load(q, App.class);
  }

  /**
   * Load.
   *
   * @param q
   *          the q
   * @param s
   *          the s
   * @param n
   *          the n
   * @return the beans
   */
  public static Beans<App> load(W q, int s, int n) {
    return Helper.load(q, s, n, App.class);
  }

  /**
   * Update.
   *
   * @param appid
   *          the appid
   * @param v
   *          the values
   * @return the int
   */
  public static int update(String appid, V v) {
    Cache.remove("node/" + appid);
    return Helper.update(W.create("appid", appid), v, App.class);
  }

  public static class Param {
    V v = V.create();

    /**
     * Creates the.
     *
     * @return the param
     */
    public static Param create() {
      return new Param();
    }

    /**
     * Builds the.
     *
     * @return the v
     */
    public V build() {
      return v;
    }

    /**
     * Appid.
     *
     * @param appid
     *          the appid
     * @return the param
     */
    public Param appid(String appid) {
      v.set("appid", appid);
      return this;
    }

    /**
     * Secret.
     *
     * @param secret
     *          the secret
     * @return the param
     */
    public Param secret(String secret) {
      v.set("secret", secret);
      return this;
    }

    /**
     * Expired.
     *
     * @param expired
     *          the expired
     * @return the param
     */
    public Param expired(long expired) {
      v.set("expired", expired);
      return this;
    }

    /**
     * Lastime.
     *
     * @param lastime
     *          the lastime
     * @return the param
     */
    public Param lastime(long lastime) {
      v.set("lastime", lastime);
      return this;
    }

    /**
     * Ip.
     *
     * @param ip
     *          the ip
     * @return the param
     */
    public Param ip(String ip) {
      v.set("ip", ip);
      return this;
    }

    /**
     * Memo.
     *
     * @param memo
     *          the memo
     * @return the param
     */
    public Param memo(String memo) {
      v.set("memo", memo);
      return this;
    }

    public Param role(long role) {
      v.set("role", role);
      return this;
    }

  }

  public static void main(String[] args) {
    App a = new App();
    a.appid = "1";
    a.secret = "123123";

    JSON j1 = JSON.create();
    j1.put("name", "1");
    j1.put("key", "122");

    try {
      String data = a.generateParameter(j1, a.secret);
      System.out.println("data=" + data);
      JSON jo = a.parseParameters(data);
      System.out.println("jo=" + jo);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
