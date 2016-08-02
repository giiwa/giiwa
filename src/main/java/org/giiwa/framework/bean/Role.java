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
import org.giiwa.core.cache.Cache;

/**
 * Role. <br>
 * collection="gi_role"
 * 
 * @author yjiang
 * 
 */
@Table(name = "gi_role")
public class Role extends Bean {

  /**
  * 
  */
  private static final long serialVersionUID = 1L;

  @Column(name = X.ID)
  private long              id;

  @Column(name = "name")
  private String            name;

  // String memo;
  // long updated;

  /**
   * Checks for.
   * 
   * @param a
   *          the a
   * @return true, if successful
   */
  public boolean has(Access a) {
    List<String> list = getAccesses();

    return list == null ? false : list.contains(a.getName());
  }

  public String getMemo() {
    return this.getString("memo");
  }

  /**
   * Creates the.
   * 
   * @param name
   *          the name
   * @param memo
   *          the memo
   * @return the int
   */
  public static long create(String name, String memo) {
    Role r = Role.loadByName(name);
    if (r != null) {
      /**
       * exists, create failded
       */
      return r.getId();
    }

    long id = UID.next("role.id");
    try {
      while (Helper.exists(W.create(X.ID, id), Role.class)) {
        id = UID.next("role.id");
      }
      if (Helper.insert(V.create(X.ID, id).set("id", id).set("name", name).set("memo", memo).set("updated",
          System.currentTimeMillis()), Role.class) > 0) {
        return id;
      }
    } catch (Exception e1) {
      log.error(e1.getMessage(), e1);
    }

    return -1;
  }

  /**
   * Gets the access.
   * 
   * @return the access
   */
  public List<String> getAccesses() {

    if (!this.containsKey("accesses")) {
      List<Object> list = Helper.distinct("name", W.create("rid", this.getId()), RoleAccess.class);

      this.set("accesses", list);

      recache();
    }

    return (List<String>) this.get("accesses");
  }

  private void recache() {
    Cache.set("role://" + this.getId(), this);
  }

  /**
   * Sets the access.
   * 
   * @param rid
   *          the rid
   * @param name
   *          the name
   */
  public static void setAccess(long rid, String name) {

    try {
      if (!Helper.exists(W.create("rid", rid).and("name", name), RoleAccess.class)) {
        Helper.insert(V.create("rid", rid).set("name", name).set(X.ID, UID.id(rid, name)), RoleAccess.class);
        Helper.update(W.create(X.ID, rid), V.create("updated", System.currentTimeMillis()), Role.class);
      }
    } catch (Exception e1) {
      log.error(e1.getMessage(), e1);
    }

  }

  /**
   * Removes the access.
   * 
   * @param rid
   *          the rid
   * @param name
   *          the name
   */
  public static void removeAccess(long rid, String name) {
    Helper.delete(W.create("rid", rid).and("name", name), RoleAccess.class);

    Helper.update(W.create(X.ID, rid), V.create("updated", System.currentTimeMillis()), Role.class);

  }

  /**
   * Load all.
   * 
   * @param roles
   *          the roles
   * @return the list
   */
  public static List<Role> loadAll(List<Long> roles) {
    List<Role> list = new ArrayList<Role>();
    if (roles != null) {
      for (long rid : roles) {
        Role r = Role.load(rid);
        if (r != null) {
          list.add(r);
        }
      }
    }
    return list;
  }

  /**
   * Exist.
   *
   * @param rid
   *          the rid
   * @return true, if successful
   */
  public static boolean exist(long rid) {
    try {
      return Helper.exists(rid, Role.class);
    } catch (Exception e1) {
      log.error(e1.getMessage(), e1);
    }
    return false;
  }

  private static Role load(long rid) {
    Role r = (Role) Cache.get("role://" + rid);

    if (r == null) {
      r = Helper.load(rid, Role.class);
    }

    if (r != null) {
      r.setExpired(60);
      Cache.set("role://" + rid, r);
    }

    return r;
  }

  /**
   * Load by name.
   * 
   * @param name
   *          the name
   * @return the role
   */
  public static Role loadByName(String name) {
    return Helper.load(W.create("name", name), Role.class);
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  /**
   * Load.
   * 
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the beans
   */
  public static Beans<Role> load(int offset, int limit) {
    return Helper.load(W.create().sort("name", 1), offset, limit, Role.class);
  }

  /**
   * Load by id.
   * 
   * @param id
   *          the id
   * @return the role
   */
  public static Role loadById(long id) {
    return Helper.load(id, Role.class);
  }

  /**
   * Update.
   * 
   * @param v
   *          the v
   * @return the int
   */
  public int update(V v) {
    return Helper.update(this.getId(), v.set("updated", System.currentTimeMillis()), Role.class);
  }

  public void setAccess(String[] accesses) {
    if (accesses != null) {
      Helper.delete(W.create("rid", this.getId()), RoleAccess.class);

      for (String a : accesses) {
        Helper.insert(V.create("rid", this.getId()).set("name", a).set(X.ID, UID.id(this.getId(), a)),
            RoleAccess.class);
      }
    }
  }

  /**
   * Delete.
   * 
   * @param id
   *          the id
   * @return the int
   */
  public static int delete(long id) {
    return Helper.delete(id, Role.class);
  }

  @Table(name = "gi_roleaccess")
  private static class RoleAccess extends Bean {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

  }

  /**
   * Load by access.
   *
   * @param access
   *          the access
   * @param s
   *          the s
   * @param n
   *          the n
   * @return the beans
   */
  public static Beans<Role> loadByAccess(String access, int s, int n) {
    Beans<RoleAccess> bs = Helper.load(W.create("name", access).sort("rid", 1), 0, Integer.MAX_VALUE, RoleAccess.class);

    W q = W.create();
    if (bs != null && bs.getList() != null) {
      if (bs.getList().size() > 1) {
        W w1 = W.create();
        for (RoleAccess a : bs.getList()) {
          w1.or(X.ID, a.getLong("rid"));
        }
        q.and(w1);
      } else if (bs.getList().size() == 1) {
        q.and(X.ID, bs.getList().get(0).getLong("rid"));
      }
    }

    return Helper.load(q.sort("name", 1), s, n, Role.class);
  }
}
