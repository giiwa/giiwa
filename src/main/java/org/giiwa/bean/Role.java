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

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.dao.*;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;

/**
 * Role bean. <br>
 * table="gi_role"
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

	private static Log log = LogFactory.getLog(Role.class);

	public static final BeanDAO<Long, Role> dao = BeanDAO.create(Role.class);

	@Column(name = X.ID)
	public long id;

	@Column(name = "name")
	public String name;

	@Column(name = "url")
	public String url; // 首页

	// String memo;
	// long updated;

	/**
	 * Checks for.
	 * 
	 * @param a the a
	 * @return true, if successful
	 */
	public boolean has(String a) {
		List<?> list = getAccesses();
		if (log.isDebugEnabled())
			log.debug("list=" + list + ", a=" + a);
		return list == null ? false : list.contains(a);
	}

	public String getMemo() {
		return this.getString("memo");
	}

	@Override
	public String toString() {
		return "Role [id=" + id + ", url=" + url + "]";
	}

	/**
	 * Creates the.
	 * 
	 * @param name the name
	 * @param memo the memo
	 * @return the int
	 */
	public static long create(String name, String memo, V v) {
		Role r = Role.loadByName(name);
		if (r != null) {
			/**
			 * exists, create failded
			 */
			return r.getId();
		}

		long id = UID.next("role.id");
		try {
			while (dao.exists(id)) {
				id = UID.next("role.id");
			}
			if (dao.insert(v.force(X.ID, id).set("id", id).set("name", name).set("memo", memo).set(X.UPDATED,
					System.currentTimeMillis())) > 0) {
				return id;
			}
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
		}

		return -1;
	}

	private transient List<?> accesses;

	/**
	 * Gets the access.
	 * 
	 * @return the access
	 */
	public List<?> getAccesses() {
		if (accesses == null) {
			accesses = RoleAccess.dao.distinct("name", W.create("rid", this.getId()));
		}

		return accesses;
	}

	/**
	 * Sets the access.
	 * 
	 * @param rid  the rid
	 * @param name the name
	 */
	public static void setAccess(long rid, String name) {

		try {
			if (!RoleAccess.dao.exists(W.create("rid", rid).and("name", name))) {
				RoleAccess.dao.insert(V.create("rid", rid).set("name", name).set(X.ID, UID.id(rid, name)));

				dao.update(W.create(X.ID, rid), V.create(X.UPDATED, System.currentTimeMillis()));
			}
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
		}

	}

	/**
	 * Removes the access.
	 * 
	 * @param rid  the rid
	 * @param name the name
	 */
	public static void removeAccess(long rid, String name) {
		dao.delete(W.create("rid", rid).and("name", name));

		dao.update(W.create(X.ID, rid), V.create(X.UPDATED, System.currentTimeMillis()));

	}

	/**
	 * Load by name.
	 * 
	 * @param name the name
	 * @return the role
	 */
	public static Role loadByName(String name) {
		return dao.load(W.create("name", name));
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
	 * @param offset the offset
	 * @param limit  the limit
	 * @return the beans
	 */
	public static Beans<Role> load(int offset, int limit) {
		return dao.load(W.create().sort("name", 1), offset, limit);
	}

	public void setAccess(String[] accesses) {
		if (accesses != null) {
			RoleAccess.dao.delete(W.create("rid", this.getId()));

			for (String a : accesses) {
				RoleAccess.dao.insert(V.create("rid", this.getId()).set("name", a).set(X.ID, UID.id(this.getId(), a)));
			}
		}
	}

	@Table(name = "gi_roleaccess")
	public static class RoleAccess extends Bean {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public static final BeanDAO<String, RoleAccess> dao = BeanDAO.create(RoleAccess.class);

	}

	/**
	 * Load by access.
	 *
	 * @param access the access
	 * @param s      the s
	 * @param n      the n
	 * @return the beans
	 */
	public static Beans<Role> loadByAccess(String access, int s, int n) {
		Beans<RoleAccess> bs = RoleAccess.dao.load(W.create("name", access).sort("rid", 1), 0, 1000);

		W q = W.create();
		if (bs == null || bs.isEmpty()) {
			return null;
		}

		if (bs.size() > 1) {
			W w1 = W.create();
			for (RoleAccess a : bs) {
				w1.or(X.ID, a.getLong("rid"));
			}
			q.and(w1);
		} else if (bs.size() == 1) {
			q.and(X.ID, bs.get(0).getLong("rid"));
		}

		return dao.load(q.sort("name", 1), s, n);
	}

	public static void to(JSON j) {
		int s = 0;
		W q = W.create().sort(X.ID, 1);

		List<JSON> l1 = new ArrayList<JSON>();
		Beans<Role> bs = dao.load(q, s, 100);
		while (bs != null && !bs.isEmpty()) {
			for (Role e : bs) {
				l1.add(e.json());
			}
			s += bs.size();
			bs = dao.load(q, s, 100);
		}

		j.append("roles", l1);
	}

	public static int from(JSON j) {
		int total = 0;
		Collection<JSON> l1 = j.getList("roles");
		if (l1 != null) {
			for (JSON e : l1) {
				long id = e.getLong(X.ID);
				V v = V.fromJSON(e);
				v.remove(X.ID, "_id");
				Role s = Role.dao.load(id);
				if (s != null) {
					dao.update(id, v);
				} else {
					dao.insert(v.append(X.ID, id));
				}
				total++;
			}
		}
		return total;
	}

}
