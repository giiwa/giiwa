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
import org.giiwa.core.cache.Cache;

import net.sf.json.JSONObject;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

// TODO: Auto-generated Javadoc
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
@DBMapping(collection = "gi_user")
public class User extends Bean {

  /**
  * 
  */
  private static final long serialVersionUID = 1L;

  /**
   * get the unique ID of the user
   * 
   * @return long
   */
  public long getId() {
    return this.getLong(X._ID);
  }

  /**
   * get the login name
   * 
   * @return String
   */
  public String getName() {
    return this.getString("name");
  }

  /**
   * get the nick name
   * 
   * @return String
   */
  public String getNickname() {
    return this.getString("nickname");
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
    return this.getString("title");
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
  @SuppressWarnings("unchecked")
  public boolean isRole(Role r) {
    List<Long> roles = (List<Long>) this.get("roles");
    return roles != null && roles.contains(r.getId());
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
        while (Bean.exists(new BasicDBObject(X._ID, id), User.class)) {
          id = UID.next("user.id");
        }
      } catch (Exception e1) {
        log.error(e1.getMessage(), e1);
      }
    }
    if (log.isDebugEnabled())
      log.debug("v=" + v);

    Bean.insertCollection(
        v.set(X._ID, id).set("created", System.currentTimeMillis()).set("updated", System.currentTimeMillis()),
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

    return Bean.load(new BasicDBObject("name", name).append("password", password)
        .append("deleted", new BasicDBObject("$ne", 1)).append("remote", new BasicDBObject("$ne", 1)), User.class);

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
    String uid = "user://name/" + name;
    User u1 = (User) Cache.get(uid);
    if (u1 != null) {
      return u1;
    }

    Beans<User> list = Bean.load(new BasicDBObject("name", name), new BasicDBObject("name", 1), 0, 100, User.class);

    if (list != null && list.getList() != null && list.getList().size() > 0) {
      for (User u : list.getList()) {

        /**
         * if the user has been locked, then not allow to login
         */
        if (u.isLocked() || u.isDeleted())
          continue;

        u.setExpired(60);
        Cache.set(uid, u);
        return u;
      }
    }

    return null;
  }

  /**
   * Load user by id.
   * 
   * @param id
   *          the user id
   * @return User
   */
  public static User loadById(long id) {
    String uid = "user://id/" + id;
    User u = (User) Cache.get(uid);
    if (u != null && !u.expired()) {
      return u;
    }

    u = Bean.load(new BasicDBObject(X._ID, id), User.class);
    if (u != null) {
      u.setExpired(60);
      u.recache();
    }

    return u;
  }

  private void recache() {
    // String uid = "user://id/" + getId();
    // Cache.set(uid, this);
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
    BasicDBObject q = new BasicDBObject();
    if (bs != null && bs.getList() != null) {
      if (bs.getList().size() > 1) {
        BasicDBList list = new BasicDBList();
        for (Role a : bs.getList()) {
          list.add(new BasicDBObject("role", a.getId()));
        }
        q.append("$or", list);
      } else if (bs.getList().size() == 1) {
        q.append("role", bs.getList().get(0).getId());
      }
    }

    q.append("deleted", new BasicDBObject("$ne", 1));

    Beans<User> us = Bean.load(q, new BasicDBObject("name", 1), 0, Integer.MAX_VALUE, User.class);
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
  @SuppressWarnings("unchecked")
  public void setRole(long rid) {
    List<Long> roles = (List<Long>) this.get("roles");
    if (roles == null) {
      roles = new ArrayList<Long>();
    }

    if (!roles.contains(rid)) {
      // add
      roles.add(rid);

      role = null;

      Bean.updateCollection(getId(), V.create("roles", roles).set("updated", System.currentTimeMillis()), User.class);
    }
  }

  /**
   * Removes the role.
   * 
   * @param rid
   *          the rid
   */
  @SuppressWarnings("unchecked")
  public void removeRole(long rid) {
    List<Long> roles = (List<Long>) this.get("roles");

    if (roles.contains(rid)) {
      // remove it
      roles.remove(rid);
      role = null;
      Bean.updateCollection(getId(), V.create("roles", roles).set("updated", System.currentTimeMillis()), User.class);
    }
  }

  /**
   * Removes the all roles.
   */
  public void removeAllRoles() {
    List<Long> roles = (List<Long>) this.get("roles");
    roles.clear();

    Bean.updateCollection(getId(), V.create("roles", roles).set("updated", System.currentTimeMillis()), User.class);
  }

  public void setSid(String sid) {
    set("sid", sid);

    Bean.updateCollection(getId(), V.create("sid", sid).set("updated", System.currentTimeMillis()), User.class);
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
  public static Beans<User> load(BasicDBObject q, int offset, int limit) {
    return Bean.load(q.append(X._ID, new BasicDBObject("$gt", 0)), new BasicDBObject("name", 1), offset, limit,
        User.class);
  }

  /**
   * Update the user with the V.
   *
   * @param v
   *          the values
   * @return int
   */
  public int update(V v) {
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
    return Bean.updateCollection(id, v.set("updated", System.currentTimeMillis()), User.class);
  }

  /***
   * replace all the roles for the user
   * 
   * @param roles
   *          the list of role id
   */
  public void setRoles(List<Long> roles) {
    Bean.updateCollection(getId(), V.create("roles", roles).set("updated", System.currentTimeMillis()), User.class);
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
    return Bean.updateCollection(getId(), V.create("sid", X.EMPTY).set("updated", System.currentTimeMillis()),
        User.class);
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
    Bean.updateCollection(new BasicDBObject("sid", sid), V.create("sid", X.EMPTY), User.class);

    return Bean.updateCollection(getId(),
        V.create("lastlogintime", System.currentTimeMillis()).set("logintimes", getInt("logintimes")).set("ip", ip)
            .set("failtimes", 0).set("locked", 0).set("lockexpired", 0).set("sid", sid)
            .set("updated", System.currentTimeMillis()),
        User.class);

  }

  /**
   * The {@code Lock} Class used to record login failure log, was used by
   * webgiiwa framework. <br>
   * collection="gi_userlock"
   * 
   * @author joe
   *
   */
  @DBMapping(collection = "gi_userlock")
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
      return Bean.insertCollection(V.create("uid", uid).set("sid", sid).set("host", host).set("useragent", useragent)
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
      return Bean.delete(new BasicDBObject("uid", uid), Lock.class);
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
      return Bean.delete(new BasicDBObject("uid", uid).append("sid", sid), Lock.class);
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
      Beans<Lock> bs = Bean.load(new BasicDBObject("uid", uid).append("created", new BasicDBObject("$gt", time)),
          new BasicDBObject("created", 1), 0, Integer.MAX_VALUE, Lock.class);
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
      Beans<Lock> bs = Bean.load(
          new BasicDBObject("uid", uid).append("created", new BasicDBObject("$gt", time)).append("sid", sid),
          new BasicDBObject("created", 1), 0, Integer.MAX_VALUE, Lock.class);
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
      Beans<Lock> bs = Bean.load(
          new BasicDBObject("uid", uid).append("created", new BasicDBObject("$gt", time)).append("host", host),
          new BasicDBObject("created", 1), 0, Integer.MAX_VALUE, Lock.class);
      return bs == null ? null : bs.getList();
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
    return Bean.delete(new BasicDBObject(X._ID, id), User.class);
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
    if (Bean.isConfigured()) {
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
  public static boolean exists(BasicDBObject q) {
    try {
      return Bean.exists(q, User.class);
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
      return Bean.exists(new BasicDBObject(X._ID, id), User.class);
    } catch (Exception e1) {
      log.error(e1.getMessage(), e1);
    }
    return false;
  }

}
