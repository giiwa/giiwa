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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.giiwa.core.bean.*;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.json.JSON;
import org.giiwa.framework.web.Model;

/**
 * Menu. <br>
 * collection="gi_menu"
 * 
 * @author yjiang
 * 
 */
@Table(name = "gi_menu")
public class Menu extends Bean {

  /**
  * 
  */
  private static final long serialVersionUID = 1L;

  // int id;

  /**
   * the name of the node, is the key of the display in language.
   *
   * @param arr
   *          the arr
   * @param tag
   *          the tag
   */

  /**
   * Insert or update.
   * 
   * @param arr
   *          the arr
   * @param tag
   *          the tag
   */
  public static void insertOrUpdate(List<JSON> arr, String tag) {
    if (arr == null) {
      return;
    }

    int len = arr.size();
    for (int i = 0; i < len; i++) {
      JSON jo = arr.get(i);

      /**
       * test and create from the "root"
       */

      jo.put("tag", tag);
      insertOrUpdate(jo, 0);
    }
  }

  public long getId() {
    return this.getLong(X.ID);
  }

  public String getName() {
    return this.getString("name");
  }

  public String getLoad() {
    return this.getString("load");
  }

  public int getChilds() {
    return this.getInt("childs");
  }

  public String getUrl() {
    return this.getString("url");
  }

  public String getTag() {
    return this.getString("tag");
  }

  public String getClasses() {
    return this.getString("classes");
  }

  public String getClick() {
    return this.getString("click");
  }

  public String getContent() {
    return this.getString("content");
  }

  public int getSeq() {
    return this.getInt("seq");
  }

  public String getAccess() {
    return this.getString("access");
  }

  /**
   * Insert or update.
   * 
   * @param jo
   *          the jo
   * @param parent
   *          the parent
   */
  public static void insertOrUpdate(JSON jo, long parent) {
    try {
      // log.info(jo);

      String name = jo.has("name") ? jo.getString("name") : null;
      if (!X.isEmpty(name)) {
        /**
         * create menu if not exists
         */
        V v = V.create().copy(jo, "url", "click", "classes", "content", "tag", "access", "seq", "tip", "style", "load");

        /**
         * create the access if not exists
         */
        if (jo.containsKey("access")) {
          String[] ss = jo.getString("access").split("[|&]");
          for (String s : ss) {
            Access.set(s);
          }
        }

        if (log.isDebugEnabled())
          log.debug(jo.toString());

        /**
         * create the menu item is not exists <br>
         * cleanup the click and load if not presented
         */
        v.set("click", X.EMPTY).set("load", X.EMPTY);
        Menu m = insertOrUpdate(parent, name, v);

        /**
         * get all childs from the json
         */
        if (jo.containsKey("childs")) {
          List<JSON> arr = jo.getList("childs");
          int len = arr.size();
          for (int i = 0; i < len; i++) {
            JSON j = arr.get(i);
            if (j != null) {
              if (jo.containsKey("tag")) {
                j.put("tag", jo.get("tag"));
              }
              insertOrUpdate(j, m.getId());
            }
          }
        }
      } else {
        // is role ?
        String role = jo.getString("role");
        String access = jo.getString("access");
        if (!X.isEmpty(role)) {
          String memo = jo.getString("memo");

          if (log.isInfoEnabled())
            log.info("create role: role=" + role + ", memo=" + memo);

          long rid = Role.create(role, memo);
          if (rid <= 0) {
            Role r = Role.loadByName(role);
            if (r != null) {
              rid = r.getId();
            }
          }
          if (rid > 0) {
            String[] ss = access.split("[|&]");
            for (String s : ss) {
              if (!X.isEmpty(s)) {
                Access.set(s);
                Role.setAccess(rid, s);
              }
            }
          } else {
            log.error("can not create or load the role: " + role);
            OpLog.warn("default", "init", "can not create or load the role:" + role, null, null);
          }
        }
      }

    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * test and create new menu if not exists
   * 
   * @param parent
   * @param name
   * @param url
   * @param classes
   * @param click
   * @param content
   * @return Menu
   */
  private static Menu insertOrUpdate(long parent, String name, V v) {
    String node = Model.node();

    W q = W.create().and("parent", parent).and("name", name).and("node", node);

    try {
      if (Helper.exists(q, Menu.class)) {
        /**
         * update
         */
        Helper.update(q, v, Menu.class);

      } else {
        long id = UID.next("menu.id");
        while (Helper.exists(W.create(X.ID, id), Menu.class)) {
          id = UID.next("menu.id");

          log.debug("id=" + id);
        }

        Helper.insert(v.set(X.ID, id).set("id", id).set("parent", parent).set("name", name).set("node", node),
            Menu.class);

      }
    } catch (Exception e1) {
      log.error(e1.getMessage(), e1);
    }

    long count = Helper.count(W.create("parent", parent), Menu.class);
    Helper.update(parent, V.create("childs", count), Menu.class);

    return Helper.load(q, Menu.class);
  }

  public String getTip() {
    return this.getString("tip");
  }

  /**
   * Submenu.
   * 
   * @param id
   *          the id
   * @return the beans
   */
  public static Beans<Menu> submenu(long id) {
    // load it
    Beans<Menu> bb = Helper.load(W.create("parent", id).sort("seq", -1), 0, -1, Menu.class);
    return bb;
  }

  /**
   * Load.
   * 
   * @param parent
   *          the parent
   * @param name
   *          the name
   * @return the menu
   */
  public static Menu load(long parent, String name) {
    String node = Model.node();
    Menu m = Helper.load(W.create("parent", parent).and("name", name).and("node", node), Menu.class);
    return m;
  }

  /**
   * Submenu.
   * 
   * @return the beans
   */
  public Beans<Menu> submenu() {
    return submenu(this.getId());
  }

  /**
   * Removes the.
   * 
   * @param id
   *          the id
   */
  public static void remove(long id) {
    Helper.delete(W.create(X.ID, id), Menu.class);

    /**
     * remove all the sub
     */
    Beans<Menu> bs = submenu(id);
    List<Menu> list = bs.getList();

    if (list != null) {
      for (Menu m : list) {
        remove(m.getId());
      }
    }
  }

  /**
   * Filter access.
   * 
   * @param list
   *          the list
   * @param me
   *          the me
   * @return the collection
   */
  public static Collection<Menu> filterAccess(List<Menu> list, User me) {
    if (list == null) {
      return null;
    }

    /**
     * filter according the access, and save seq
     */
    Map<Integer, Menu> map = new TreeMap<Integer, Menu>();

    for (Menu m : list) {
      String access = m.getAccess();
      boolean has = false;
      if (X.isEmpty(access)) {
        has = true;
      }

      if (!has && me != null) {
        if (access.indexOf("|") > 0) {
          String[] ss = access.split("\\|");
          if (me.hasAccess(ss)) {
            has = true;
          }
        } else if (access.indexOf("&") > 0) {
          String[] ss = access.split("\\&");
          for (String s : ss) {
            if (!me.hasAccess(s)) {
              has = false;
              break;
            }
          }
        } else if (me.hasAccess(access)) {
          has = true;
        }
      }

      if (has) {
        int seq = m.getSeq();
        while (map.containsKey(seq))
          seq++;
        map.put(seq, m);
      }
    }

    return map.values();
  }

  /**
   * Removes the.
   * 
   * @param tag
   *          the tag
   */
  public static void remove(String tag) {
    String node = Model.node();
    Helper.delete(W.create("tag", tag).and("node", node), Menu.class);
  }

  /**
   * Reset.
   */
  public static void reset() {
    String node = Model.node();
    Helper.update(W.create("node", node), V.create("seq", -1), Menu.class);
  }

  /**
   * Cleanup.
   */
  public static void cleanup() {
    String node = Model.node();
    Helper.delete(W.create("node", node).and("seq", 0, W.OP_LT), Menu.class);
  }

  public String getStyle() {
    return this.getString("style");
  }
}
