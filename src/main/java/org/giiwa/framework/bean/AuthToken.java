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

import java.util.ArrayList;
import java.util.List;

import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Table;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;

import com.mongodb.BasicDBObject;

// TODO: Auto-generated Javadoc
/**
 * The AuthToken. <br>
 * collection="gi_authtoken"
 * 
 * @author wujun
 *
 */
@Table(collection = "gi_authtoken")
public class AuthToken extends Bean {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  /**
   * get the id
   * 
   * @return String
   */
  public String getId() {
    return this.getString(X._ID);
  }

  /**
   * get the uid
   * 
   * @return long
   */
  public long getUid() {
    return this.containsKey("uid") ? this.getLong("uid") : -1;
  }

  /**
   * get the token
   * 
   * @return String
   */
  public String getToken() {
    return this.getString("token");
  }

  /**
   * get the expired timestamp
   * 
   * @return long
   */
  public long getExpired() {
    return this.getLong("expired");
  }

  /**
   * get the user object
   * 
   * @return User
   */
  public User getUser() {
    User u = (User) this.get("user_obj");
    if (u == null && this.getUid() >= 0) {
      u = User.loadById(this.getUid());
      this.set("user_obj", u);
    }
    return u;
  }

  /**
   * get the session id
   * 
   * @return String
   */
  public String getSid() {
    return this.getString("sid");
  }

  /**
   * update the session token.
   *
   * @param uid
   *          the uid
   * @param sid
   *          the sid
   * @param ip
   *          the ip
   * @return the auth token
   */
  public static AuthToken update(long uid, String sid, String ip) {
    String id = UID.id(uid, sid);
    String token = UID.random(20);
    long expired = System.currentTimeMillis() + Global.l("token.expired", X.AWEEK);
    V v = V.create("uid", uid).set("sid", sid).set("token", token).set("expired", expired).set("ip", ip);

    try {
      if (Bean.exists(new BasicDBObject(X._ID, id), AuthToken.class)) {
        // update
        Bean.updateCollection(id, v, AuthToken.class);
      } else {
        // insert
        Bean.insertCollection(v.set(X._ID, id), AuthToken.class);
      }
    } catch (Exception e1) {
      log.error(e1.getMessage(), e1);
    }

    return load(id);
  }

  /**
   * load the authtoken.
   *
   * @param id
   *          the id
   * @return AuthToken
   */
  public static AuthToken load(String id) {
    return Bean.load(new BasicDBObject(X._ID, id), AuthToken.class);
  }

  /**
   * load the AuthToken by the session and token.
   *
   * @param sid
   *          the sid
   * @param token
   *          the token
   * @return AuthToken
   */
  public static AuthToken load(String sid, String token) {
    return Bean.load(new BasicDBObject("sid", sid).append("token", token).append("expired",
        new BasicDBObject("$gt", System.currentTimeMillis())), AuthToken.class);
  }

  /**
   * remove all the session and token for the uid, and return all the session id
   * for the user.
   *
   * @param uid
   *          the uid
   * @return List of session
   */
  public static List<String> remove(long uid) {
    List<String> list = new ArrayList<String>();
    BasicDBObject q = new BasicDBObject("uid", uid);
    BasicDBObject order = new BasicDBObject();
    int s = 0;
    Beans<AuthToken> bs = load(q, order, s, 10);
    while (bs != null && bs.getList() != null && bs.getList().size() > 0) {
      for (AuthToken t : bs.getList()) {
        String sid = t.getSid();
        list.add(sid);
      }
      s += bs.getList().size();
      bs = load(q, order, s, 10);
    }
    Bean.delete(new BasicDBObject("uid", uid), AuthToken.class);
    return list;
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
  public static Beans<AuthToken> load(BasicDBObject q, BasicDBObject order, int s, int n) {
    return Bean.load(q, order, s, n, AuthToken.class);
  }

  /**
   * load Beans by uid, a uid may has more AuthToken.
   *
   * @param uid
   *          the uid
   * @return Beans
   */
  public static Beans<AuthToken> load(long uid) {
    return Bean.load(
        new BasicDBObject("uid", uid).append("expired", new BasicDBObject("$gt", System.currentTimeMillis())),
        new BasicDBObject(X._ID, 1), 0, 100, AuthToken.class);
  }
}
