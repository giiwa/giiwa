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
import org.giiwa.core.bean.Column;
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.bean.Table;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;

// TODO: Auto-generated Javadoc
/**
 * The AuthToken. <br>
 * collection="gi_authtoken"
 * 
 * @author wujun
 *
 */
@Table(name = "gi_authtoken")
public class AuthToken extends Bean {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  @Column(name = X.ID)
  private String            id;

  @Column(name = "uid")
  private long              uid;

  @Column(name = "token")
  private String            token;

  @Column(name = "expired")
  private long              expired;

  @Column(name = "sid")
  private String            sid;

  /**
   * get the id
   * 
   * @return String
   */
  public String getId() {
    return id;
  }

  /**
   * get the uid
   * 
   * @return long
   */
  public long getUid() {
    return uid;
  }

  /**
   * get the token
   * 
   * @return String
   */
  public String getToken() {
    return token;
  }

  /**
   * get the expired timestamp
   * 
   * @return long
   */
  public long getExpired() {
    return expired;
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
    return sid;
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
      if (Helper.exists(id, AuthToken.class)) {
        // update
        Helper.update(id, v, AuthToken.class);
      } else {
        // insert
        Helper.insert(v.set(X.ID, id), AuthToken.class);
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
    return Helper.load(id, AuthToken.class);
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
    return Helper.load(W.create("sid", sid).and("token", token).and("expired", System.currentTimeMillis(), W.OP_GT),
        AuthToken.class);
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
    W q = W.create("uid", uid);
    int s = 0;
    Beans<AuthToken> bs = load(q, s, 10);
    while (bs != null && bs.getList() != null && bs.getList().size() > 0) {
      for (AuthToken t : bs.getList()) {
        String sid = t.getSid();
        list.add(sid);
      }
      s += bs.getList().size();
      bs = load(q, s, 10);
    }
    Helper.delete(W.create("uid", uid), AuthToken.class);
    return list;
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
  public static Beans<AuthToken> load(W q, int s, int n) {
    return Helper.load(q, s, n, AuthToken.class);
  }

  /**
   * load Beans by uid, a uid may has more AuthToken.
   *
   * @param uid
   *          the uid
   * @return Beans
   */
  public static Beans<AuthToken> load(long uid) {
    return Helper.load(W.create("uid", uid).and("expired", System.currentTimeMillis(), W.OP_GT), 0, 100,
        AuthToken.class);
  }
}
