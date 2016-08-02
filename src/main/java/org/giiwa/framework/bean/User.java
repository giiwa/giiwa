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

import java.util.*;

import org.giiwa.core.bean.*;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;

import net.sf.json.JSONObject;

/**
 * 
 * The {@code User} Class is base user class, all the login/access controlled in
 * webgiiwa was depended on the user, it contains all the user-info, and is
 * expandable. <br>
 * collection="gi_user" <br>
 * MOST important field
 * 
 * <pre>
 * id: long, global unique,
 * name: login name, global unique
 * password: string of hashed
 * nickname: string of nickname
 * title: title of the user
 * roles: the roles of the user, user can has multiple roles
 * hasAccess: test whether has the access token for the user
 * </pre>
 * 
 * @author yjiang
 * 
 */
@Table(name = "gi_user")
public class User extends Bean {

  /**
  * 
  */
  private static final long serialVersionUID = 1L;

  @Column(name = X.ID)
  private long              id;

  @Column(name = "name")
  private String            name;

  @Column(name = "nickname")
  private String            nickname;

  @Column(name = "title")
  private String            title;

  @Column(name = "password")
  private String            password;

  /**
   * get the unique ID of the user
   * 
   * @return long
   */
  public long getId() {
    return id;
  }

  /**
   * get the login name
   * 
   * @return String
   */
  public String getName() {
    return name;
  }

  /**
   * get the nick name
   * 
   * @return String
   */
  public String getNickname() {
    return nickname;
  }

  /**
   * get the phone number
   * 
   * @return String
   */
  public String getPhone() {
    return this.getString("phone");
  }

  /**
   * get the email address
   * 
   * @return String
   */
  public String getEmail() {
    return this.getString("email");
  }

  /**
   * get the title of the user
   * 
   * @return String
   */
  public String getTitle() {
    return title;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.giiwa.core.bean.Bean#toString()
   */
  public String toString() {
    return "User@{id=" + this.getId() + ",name=" + this.getString("name") + "}";
  }

  /**
   * Instantiates a new user.
   */
  public User() {

  }

  /**
   * Checks if is role.
   * 
   * @param r
   *          the r
   * @return true, if is role
   */
  public boolean isRole(Role r) {
    try {
      return Helper.exists(W.create("uid", this.getId()).and("rid", r.getId()), UserRole.class);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return false;
  }

  /**
   * Creates a user with the values, <br>
   * if the values contains "password" field, it will auto encrypt the password
   * field.
   *
   * @param v
   *          the values
   * @return long of the user id, if failed, return -1
   */
  public static long create(V v) {

    String s = (String) v.value("password");
    if (s != null) {
      v.set("password", encrypt(s), true);
    }

    Long id = (Long) v.value("id");
    if (id == null) {
      id = UID.next("user.id");
      try {
        while (Helper.exists(id, User.class)) {
          id = UID.next("user.id");
        }
      } catch (Exception e1) {
        log.error(e1.getMessage(), e1);
      }
    }
    if (log.isDebugEnabled())
      log.debug("v=" + v);

    Helper.insert(v.set(X.ID, id).set("created", System.currentTimeMillis()).set("updated", System.currentTimeMillis()),
        User.class);

    return id;
  }

  /**
   * create a user from the jo.
   *
   * @param jo
   *          the jo
   * @return int
   * @deprecated
   */
  public static long copy(JSONObject jo) {

    V v = V.create();
    for (Object name : jo.keySet()) {
      v.set(name.toString(), jo.get(name));
    }

    return User.create(v);

  }

  /**
   * Load user by name and password.
   *
   * @param name
   *          the name of the user
   * @param password
   *          the password
   * @return User, if not match anyoone, return null
   */
  public static User load(String name, String password) {

    password = encrypt(password);

    log.debug("name=" + name + ", passwd=" + password);
    // System.out.println("name=" + name + ", passwd=" + password);

    return Helper.load(W.create("name", name).and("password", password).and("deleted", 1, W.OP_NEQ), User.class);

  }

  public boolean isDeleted() {
    return getInt("deleted") == 1;
  }

  /**
   * Load user by name.
   *
   * @param name
   *          the name of the name
   * @return User
   */
  public static User load(String name) {

    return Helper.load(W.create("name", name).and("deleted", 1, W.OP_NEQ).sort("name", 1), User.class);

  }

  /**
   * Load user by id.
   * 
   * @param id
   *          the user id
   * @return User
   */
  public static User loadById(long id) {

    return Helper.load(id, User.class);
  }

  /**
   * Load users by access token name.
   *
   * @param access
   *          the access token name
   * @return list of user who has the access token
   */
  public static List<User> loadByAccess(String access) {

    Beans<Role> bs = Role.loadByAccess(access, 0, 1000);
    W q = W.create();

    if (bs != null && bs.getList() != null) {
      if (bs.getList().size() > 1) {
        W list = W.create();
        for (Role a : bs.getList()) {
          list.or("rid", a.getId());
        }
        q.and(list);
      } else if (bs.getList().size() == 1) {
        q.and("rid", bs.getList().get(0).getId());
      }

    }

    Beans<UserRole> b2 = Helper.load(q, 0, 1000, UserRole.class);
    q = W.create();
    if (b2 != null && b2.getList() != null) {
      if (b2.getList().size() > 1) {
        W list = W.create();
        for (UserRole a : b2.getList()) {
          list.or("id", a.getLong("uid"));
        }
        q.and(list);
      } else if (b2.getList().size() == 1) {
        q.and("id", b2.getList().get(0).getLong("uid"));
      }
    }

    q.and("deleted", 1, W.OP_NEQ);

    Beans<User> us = Helper.load(q.sort("name", 1), 0, Integer.MAX_VALUE, User.class);
    return us == null ? null : us.getList();

  }

  /**
   * Validate the user with the password.
   *
   * @param password
   *          the password
   * @return true, if the password was match
   */
  public boolean validate(String password) {

    /**
     * if the user has been locked, then not allow to login
     */
    if (this.isLocked())
      return false;

    password = encrypt(password);
    return get("password") != null && get("password").equals(password);
  }

  /**
   * whether the user has been locked
   * 
   * @return boolean
   */
  public boolean isLocked() {
    return getInt("locked") > 0;
  }

  /**
   * Checks whether has the access token.
   * 
   * @param name
   *          the name of the access token
   * @return true, if has anyone
   */
  public boolean hasAccess(String... name) {
    if (this.getId() == 0L) {
      return true;
    }

    if (role == null) {
      getRole();
    }

    return role.hasAccess(name);
  }

  transient Roles role = null;

  /**
   * get the roles for the user
   * 
   * @return Roles
   */
  @SuppressWarnings("unchecked")
  public Roles getRole() {
    if (role == null) {
      List<Long> roles = (List<Long>) this.get("roles");
      role = new Roles(roles);
    }
    return role;
  }

  /**
   * set a role to a user with role id
   * 
   * @param rid
   *          the role id
   */
  public void setRole(long rid) {
    try {
      if (!Helper.exists(W.create("uid", this.getId()).and("rid", rid), UserRole.class)) {
        Helper.insert(V.create("uid", this.getId()).set("rid", rid), UserRole.class);
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Removes the role.
   * 
   * @param rid
   *          the rid
   */
  public void removeRole(long rid) {
    Helper.delete(W.create("uid", this.getId()).and("rid", rid), UserRole.class);
  }

  /**
   * Removes the all roles.
   */
  public void removeAllRoles() {
    Helper.delete(W.create("uid", this.getId()), UserRole.class);

  }

  private static String encrypt(String passwd) {
    if (X.isEmpty(passwd)) {
      return X.EMPTY;
    }
    return UID.id(passwd);
  }

  /**
   * Load the users by the query.
   *
   * @param q
   *          the query of the condition
   * @param offset
   *          the start number
   * @param limit
   *          the number
   * @return Beans
   */
  public static Beans<User> load(W q, int offset, int limit) {
    return Helper.load(q.and(X.ID, 0, W.OP_GT).sort("name", 1), offset, limit, User.class);
  }

  /**
   * Update the user with the V.
   *
   * @param v
   *          the values
   * @return int
   */
  public int update(V v) {
    for (String name : v.names()) {
      this.set(name, v.value(name));
    }
    return update(this.getId(), v);
  }

  /**
   * update the user by the values, <br>
   * if the values contains "password" field, it will auto encrypt the password
   * field.
   *
   * @param id
   *          the user id
   * @param v
   *          the values
   * @return int, 0 no user updated
   */
  public static int update(long id, V v) {

    String passwd = (String) v.value("password");
    if (!X.isEmpty(passwd)) {
      passwd = encrypt(passwd);
      v.set("password", passwd, true);
    } else {
      v.remove("password");
    }
    return Helper.update(id, v.set("updated", System.currentTimeMillis()), User.class);
  }

  /***
   * replace all the roles for the user
   * 
   * @param roles
   *          the list of role id
   */
  public void setRoles(List<Long> roles) {
    this.removeAllRoles();
    for (long rid : roles) {
      this.setRole(rid);
    }
  }

  /**
   * record the login failure, and record the user lock info.
   *
   * @param ip
   *          the ip that login come from
   * @param sid
   *          the session id
   * @param useragent
   *          the browser agent
   * @return int of the locked times
   */
  public int failed(String ip, String sid, String useragent) {
    set("failtimes", getInt("failtimes") + 1);

    return Lock.locked(getId(), sid, ip, useragent);
  }

  /**
   * record the logout info in database for the user.
   *
   * @return the int
   */
  public int logout() {
    return Helper.update(getId(), V.create("sid", X.EMPTY).set("updated", System.currentTimeMillis()), User.class);
  }

  /**
   * record login info in database for the user.
   *
   * @param sid
   *          the session id
   * @param ip
   *          the ip that the user come fram
   * @return the int
   */
  public int logined(String sid, String ip) {

    // update
    set("logintimes", getInt("logintimes") + 1);

    Lock.removed(getId(), sid);

    /**
     * cleanup the old sid for the old logined user
     */
    Helper.update(W.create("sid", sid), V.create("sid", X.EMPTY), User.class);

    return Helper.update(getId(),
        V.create("lastlogintime", System.currentTimeMillis()).set("logintimes", getInt("logintimes")).set("ip", ip)
            .set("failtimes", 0).set("locked", 0).set("lockexpired", 0).set("sid", sid)
            .set("updated", System.currentTimeMillis()),
        User.class);

  }

  @Table(name = "gi_userrole")
  public static class UserRole extends Bean {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

  }

  /**
   * The {@code Lock} Class used to record login failure log, was used by
   * webgiiwa framework. <br>
   * collection="gi_userlock"
   * 
   * @author joe
   *
   */
  @Table(name = "gi_userlock")
  public static class Lock extends Bean {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Locked.
     *
     * @param uid
     *          the uid
     * @param sid
     *          the sid
     * @param host
     *          the host
     * @param useragent
     *          the useragent
     * @return the int
     */
    public static int locked(long uid, String sid, String host, String useragent) {
      return Helper.insert(V.create("uid", uid).set("sid", sid).set("host", host).set("useragent", useragent)
          .set("created", System.currentTimeMillis()), Lock.class);
    }

    /**
     * Removed.
     *
     * @param uid
     *          the uid
     * @return the int
     */
    public static int removed(long uid) {
      return Helper.delete(W.create("uid", uid), Lock.class);
    }

    /**
     * Removed.
     *
     * @param uid
     *          the uid
     * @param sid
     *          the sid
     * @return the int
     */
    public static int removed(long uid, String sid) {
      return Helper.delete(W.create("uid", uid).and("sid", sid), Lock.class);
    }

    /**
     * Load.
     *
     * @param uid
     *          the uid
     * @param time
     *          the time
     * @return the list
     */
    public static List<Lock> load(long uid, long time) {
      Beans<Lock> bs = Helper.load(W.create("uid", uid).and("created", time, W.OP_GT).sort("created", 1), 0,
          Integer.MAX_VALUE, Lock.class);
      return bs == null ? null : bs.getList();
    }

    /**
     * Load by sid.
     *
     * @param uid
     *          the uid
     * @param time
     *          the time
     * @param sid
     *          the sid
     * @return the list
     */
    public static List<Lock> loadBySid(long uid, long time, String sid) {
      Beans<Lock> bs = Helper.load(
          W.create("uid", uid).and("created", time, W.OP_GT).and("sid", sid).sort("created", 1), 0, Integer.MAX_VALUE,
          Lock.class);
      return bs == null ? null : bs.getList();
    }

    /**
     * Load by host.
     *
     * @param uid
     *          the uid
     * @param time
     *          the time
     * @param host
     *          the host
     * @return the list
     */
    public static List<Lock> loadByHost(long uid, long time, String host) {
      Beans<Lock> bs = Helper.load(
          W.create("uid", uid).and("created", time, W.OP_GT).and("host", host).sort("created", 1), 0, Integer.MAX_VALUE,
          Lock.class);
      return bs == null ? null : bs.getList();
    }

    /**
     * delete all user lock info for the user id
     * 
     * @param uid
     *          the user id
     * @return the number deleted
     */
    public static int cleanup(long uid) {
      return Helper.delete(W.create("uid", uid), Lock.class);
    }

    public long getUid() {
      return getLong("uid");
    }

    public long getCreated() {
      return getLong("created");
    }

    public String getSid() {
      return getString("sid");
    }

    public String getHost() {
      return getString("host");
    }

    public String getUseragent() {
      return getString("useragent");
    }

  }

  /**
   * Delete the user by ID.
   *
   * @param id
   *          the id of the user
   * @return int how many was deleted
   */
  public static int delete(long id) {
    return Helper.delete(id, User.class);
  }

  public List<AuthToken> getTokens() {
    List<AuthToken> list = (List<AuthToken>) this.get("token_obj");
    if (list == null) {
      Beans<AuthToken> bs = AuthToken.load(this.getId());
      if (bs != null && bs.getList() != null) {
        list = bs.getList();
        this.set("token_obj", list);
      }
    }
    return list;

  }

  /**
   * check the database, if there is no "config.admin" user, then create the
   * "admin" user, with "admin" as password
   */
  public static void checkAndInit() {
    if (Helper.isConfigured()) {
      List<User> list = User.loadByAccess("access.config.admin");
      if (list == null || list.size() == 0) {
        User.create(V.create("id", 0L).set("name", "admin").set("password", "admin").set("title", "Admin"));
      }
    }
  }

  /**
   * test the user exists for the query.
   *
   * @param q
   *          the query
   * @return boolean
   */
  public static boolean exists(W q) {
    try {
      return Helper.exists(q, User.class);
    } catch (Exception e1) {
      log.error(e1.getMessage(), e1);
    }
    return false;
  }

  /**
   * test the user exists for the id.
   *
   * @param id
   *          the id
   * @return boolean
   */
  public static boolean exists(long id) {
    try {
      return Helper.exists(id, User.class);
    } catch (Exception e1) {
      log.error(e1.getMessage(), e1);
    }
    return false;
  }

}
