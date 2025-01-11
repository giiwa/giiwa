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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.dao.Bean;
import org.giiwa.dao.BeanDAO;
import org.giiwa.dao.Beans;
import org.giiwa.dao.Column;
import org.giiwa.dao.Helper;
import org.giiwa.dao.Table;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.web.Language;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;

@Table(name = "gi_history", memo = "GI-数据痕迹")
public final class History extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(History.class);

	public static final BeanDAO<Long, History> dao = BeanDAO.create(History.class);

	@Column(memo = "主键", unique = true)
	long id;

	@Column(name = "_table", memo = "数据表", size = 100)
	String table;

	@Column(memo = "数据ID", size = 50)
	String dataid;

	@Column(memo = "用户ID")
	long uid;

	@Column(memo = "IP地址", size = 50)
	String ip;

	@Column(name = "data", memo = "数据", value = "json", size = 2048)
	String data_obj;

	transient User uid_obj;

	public User getUid_obj() {
		if (uid_obj == null) {
			uid_obj = User.dao.load(uid);
		}
		return uid_obj;
	}

	public static boolean create(Bean p, V v, long uid) {
		return create(p, v, uid, null);
	}

	public static boolean create(Bean p, V v, long uid, String ip) {
		/**
		 * diff each data in V
		 */
		if (p == null || v == null) {
			return false;
		}

		try {
			String table = Helper.getTable(p.getClass());
			if (X.isEmpty(table)) {
				return false;
			}

			Object dataid = p.get(X.ID);
			if (dataid == null) {
				return false;
			}

			dataid = dataid.toString();

			StringBuilder sb = new StringBuilder();
			for (String name : v.names()) {
				if (X.isIn(name, "updated", "created", "_id", "id")) {
					continue;
				}
				Object v0 = p == null ? null : p.get(name);
				Object v1 = v.value(name);
				if (!X.isSame(v0, v1)) {
					if (sb.length() > 0) {
						sb.append("\n\n");
					}
					sb.append("=== [" + name + "] ===\n");
					sb.append(v1);
					sb.append("\n=== end ===");
				}
			}
			if (sb.length() > 0) {
				_create(V.create("_table", table).append("dataid", dataid).append("data", sb.toString())
						.append("uid", uid).append("ip", ip));
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return true;
	}

	public static boolean create(Bean p, long uid, String ip) {
		/**
		 * diff each data in V
		 */
		if (p == null) {
			return false;
		}

		try {
			String table = Helper.getTable(p.getClass());
			if (X.isEmpty(table)) {
				return false;
			}

			Object dataid = p.get(X.ID);
			if (dataid == null) {
				return false;
			}

			dataid = dataid.toString();

			_create(V.create("_table", table).append("dataid", dataid).append("data", p.json().toString())
					.append("uid", uid).append("ip", ip));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return true;
	}

	public static boolean changed(Bean p, V v) {
		for (String name : v.names()) {
			if (X.isIn(name, "updated", "created", "_id", "id"))
				continue;
			Object v0 = p == null ? null : p.get(name);
			Object v1 = v.value(name);
			if (!X.isSame(v0, v1)) {
				return true;
			}
		}
		return false;
	}

	public static boolean create(String table, V v, long uid) {
		/**
		 * compare each data in V
		 */
		if (X.isEmpty(table) || v == null) {
			return false;
		}

		Object dataid = v.value(X.ID);
		if (dataid == null) {
			return false;
		}
		dataid = dataid.toString();

		StringBuilder sb = new StringBuilder();
		for (String name : v.names()) {
			if (X.isIn(name, "updated", "created", "_id", "id")) {
				continue;
			}
			Object v1 = v.value(name);
			if (!X.isEmpty(v1)) {
				if (sb.length() > 0) {
					sb.append("\n\n");
				}
				sb.append("=== [" + name + "] ===\n");
				sb.append(v1);
				sb.append("\n=== end ===");
			}
		}
		if (sb.length() > 0) {
			_create(V.create("_table", table).append("dataid", dataid).append("data", sb.toString()).append("uid",
					uid));
		}

		return true;
	}

	private static long _create(V v) {

		try {
			long id = dao.next();
			while (dao.exists(id)) {
				id = dao.next();
			}
			dao.insert(v.force(X.ID, id));
			return id;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return -1;
	}

	public static boolean create(BeanDAO<?, ? extends Bean> b, V v, long uid) {
		/**
		 * compare each data in V
		 */
		try {
			String table = b.tableName();
			if (X.isEmpty(table) || v == null) {
				return false;
			}

			Object dataid = v.value(X.ID);
			if (dataid == null) {
				return false;
			}
			dataid = dataid.toString();

			StringBuilder sb = new StringBuilder();
			for (String name : v.names()) {
				if (X.isIn(name, "updated", "created", "_id", "id")) {
					continue;
				}

				Object v1 = v.value(name);
				if (!X.isEmpty(v1)) {
					if (sb.length() > 0) {
						sb.append("\n\n");
					}
					sb.append("=== [" + name + "] ===\n");
					sb.append(v1);
					sb.append("\n=== end ===");
				}
			}
			if (sb.length() > 0) {
				_create(V.create("_table", table).append("dataid", dataid).append("data", sb.toString()).append("uid",
						uid));
			}

			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return false;
	}

	public static Beans<History> load(String table, String dataid, int s, int n) {
		return dao.load(W.create().and("_table", table).and("dataid", dataid).sort("created", -1), s, n);
	}

	public static Beans<History> load(BeanDAO<?, ? extends Bean> b, String dataid, int s, int n) {
		try {
			return dao.load(W.create().and("_table", b.tableName()).and("dataid", dataid).sort("created", -1), s, n);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public JSON refine(Language lang) {

		JSON j1 = this.json();
		if (this.getUid_obj() != null) {
			j1.append("uid_obj", this.getUid_obj().json());
		}

		j1.append("created_name", lang.format(this.getCreated(), "yyyy-MM-dd HH:mm:ss"));
		return j1;
	}

}
