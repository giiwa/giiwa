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
package org.giiwa.bean;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.*;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.engine.Velocity;
import org.giiwa.json.JSON;
import org.giiwa.web.Module;

/**
 * Menu bean. <br>
 * table="gi_menu"
 * 
 * @author yjiang
 * 
 */
@Table(name = "gi_menu", memo = "GI-菜单")
public final class Menu extends Bean {

	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(Menu.class);

	public static final BeanDAO<Long, Menu> dao = BeanDAO.create(Menu.class);

	// int id;

	/**
	 * Insert or update.
	 * 
	 * @param arr the arr
	 * @param tag the tag
	 */
	public static void insertOrUpdate(List<JSON> arr, Module mo) {
		if (arr == null) {
			return;
		}

		int len = arr.size();

		for (int i = 0; i < len; i++) {
			JSON jo = arr.get(i);

			/**
			 * test and create from the "root"
			 */

			jo.put("tag", mo.getName());
			insertOrUpdate(jo, 0, mo);
		}
	}

	public long getId() {
		return this.getLong(X.ID);
	}

	public String getName() {
		return this.getString("name");
	}

	public String getLoad1() {
		return this.getString("load1");
	}

	public int getChilds() {
		return this.getInt("childs");
	}

	public String getUrl() {
		return this.getString(X.URL);
	}

	public String getTag() {
		return this.getString("tag");
	}

	public String getClasses() {
		return this.getString("classes");
	}

	public String getShow1() {
		return this.getString("show1");
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
	 * @param jo     the jo
	 * @param parent the parent
	 */
	public static void insertOrUpdate(JSON jo, long parent, Module mo) {
		try {
			// log.info(jo);

			String name = jo.has("name") ? jo.getString("name") : null;
			if (!X.isEmpty(name)) {
				/**
				 * create menu if not exists
				 */
				V v = V.create().copy(jo, X.URL, "click", "classes", "content", "tag", "access", "seq", "tip", "style",
						"load", "show");

				if (!X.isEmpty(v.value("load"))) {
					v.set("load1", v.value("load"));
					v.remove("load");
				}

				if (!X.isEmpty(v.value("show"))) {
					v.set("show1", v.value("show"));
					v.remove("show");
				} else {
					v.set("show1", X.EMPTY);
				}

				/**
				 * create the access if not exists
				 */
				if (jo.containsKey("access")) {
					String access = jo.getString("access");
					String[] ss = X.split(access, "[,]");
					if (ss != null) {
						for (String s : ss) {
							Access.set(s);
						}
					}
				}

				if (log.isDebugEnabled())
					log.debug(jo.toString());

				/**
				 * create the menu item is not exists <br>
				 * cleanup the click and load if not presented
				 */
				v.set("click", X.EMPTY).set("load1", X.EMPTY);
				Menu m = insertOrUpdate(parent, name, v);

				/**
				 * get all childs from the json
				 */
				if (jo.containsKey("childs")) {
					Collection<JSON> arr = jo.getList("childs");
					if (arr != null) {

						for (JSON j : arr) {
							if (j != null) {

								if (jo.containsKey("tag")) {
									j.put("tag", jo.get("tag"));
								}
								insertOrUpdate(j, m.getId(), mo);
							}
						}
					}
				}
			} else {
				// is role ?
				String role = jo.getString("role");

				String access = jo.getString("access");
				if (!X.isEmpty(role)) {
					String memo = jo.getString("memo");

					if (log.isDebugEnabled()) {
						log.info("create role: role=" + role + ", memo=" + memo);
					}

					long rid = Role.create(role, memo,
							V.create().append("url", jo.getString("url")).append("seq", mo.id));
					if (rid <= 0) {
						Role r = Role.loadByName(role);
						if (r != null) {
							rid = r.getId();
						}
					}
					if (rid > 0) {
						String[] ss = X.split(access, "[,]");
						if (ss != null) {
							for (String s : ss) {
								if (!X.isEmpty(s)) {
									Access.set(s);
									Role.setAccess(rid, s);
								}
							}
						}
					} else {
						log.error("can not create or load the role: " + role);
						GLog.applog.warn("default", "init", "can not create or load the role:" + role, null, null);
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
		String node = Local.id();

		W q = W.create().and("parent", parent).and("name", name).and("node", node);

		try {
			if (dao.exists(q)) {
				/**
				 * update
				 */
				dao.update(q, v);

			} else {
				long id = UID.next("menu.id");
				while (dao.exists(id)) {
					id = UID.next("menu.id");

					if (log.isDebugEnabled())
						log.debug("id=" + id);
				}

				dao.insert(v.set(X.ID, id).set("id", id).set("parent", parent).set("name", name).set("node", node));

			}
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
		}

		long count = dao.count(W.create().and("parent", parent));
		dao.update(parent, V.create("childs", count));

		return dao.load(q);
	}

	public String getTip() {
		return this.getString("tip");
	}

	/**
	 * Submenu.
	 * 
	 * @param id the id
	 * @return the beans
	 */
	public static Beans<Menu> submenu(long id) {
		// load it
		Beans<Menu> bb = dao.load(W.create().and("parent", id).sort("seq", -1), 0, Integer.MAX_VALUE);
		return bb;
	}

	/**
	 * Load.
	 * 
	 * @param parent the parent
	 * @param name   the name
	 * @return the menu
	 */
	public static Menu load(long parent, String name) {
		String node = Local.id();
		Menu m = dao.load(W.create().and("parent", parent).and("name", name).and("node", node));
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
	 * @param id the id
	 */
	public static void remove(long id) {
		dao.delete(id);

		/**
		 * remove all the sub
		 */
		Beans<Menu> bs = submenu(id);
		List<Menu> list = bs;

		if (list != null) {
			for (Menu m : list) {
				remove(m.getId());
			}
		}
	}

	/**
	 * Filter access.
	 * 
	 * @param list the list
	 * @param me   the me
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

			String s = m.getShow1();
			if (has && !X.isEmpty(s)) {
				Map<String, Object> m1 = new HashMap<String, Object>();
				m1.put("me", me);
				m1.put("global", Global.getInstance());
				m1.put("local", Local.getInstance());
				try {
					has = Velocity.test(s, m1);
				} catch (Exception e) {
					log.error(s, e);
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
	 * @param tag the tag
	 */
	public static void remove(String tag) {
		String node = Local.id();
		dao.delete(W.create().and("tag", tag).and("node", node));
	}

	/**
	 * Reset.
	 */
	public static void reset() {
		String node = Local.id();
		// log.debug("node=" + node);
		dao.update(W.create().and("node", node), V.create("seq", -1));
	}

	/**
	 * Cleanup.
	 */
	public static void deleteall() {
		String node = Local.id();
		dao.delete(W.create().and("node", node).and("seq", 0, W.OP.lt));
	}

	public String getStyle() {
		return this.getString("style");
	}
}
