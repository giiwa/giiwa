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
package org.giiwa.app.web.admin;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.giiwa.core.base.Exporter;
import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.X;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.conf.Config;
import org.giiwa.core.bean.Schema;
import org.giiwa.core.json.JSON;
import org.giiwa.framework.bean.GLog;
import org.giiwa.framework.bean.Temp;
import org.giiwa.framework.web.Controller;
import org.giiwa.framework.web.Path;

/**
 * backup management,<br>
 * required "access.config.admin"
 * 
 * @author joe
 *
 */
public class database extends Controller {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model.onGet()
	 */
	@Path(login = true, access = "access.config.admin")
	@Override
	public void onGet() {

		List<Class<? extends Bean>> l1 = Schema.beans;
		Map<String, JSON> l2 = new TreeMap<String, JSON>();
		for (Class<? extends Bean> c : l1) {
			String table = Helper.getTable(c);

			String name = table;
			if (!X.isEmpty(name) && !l2.containsKey(name)) {
				JSON j = JSON.create().append("name", c.getName()).append("table", table)
						.append("total", Helper.count(W.create(), c)).append("size", Helper.size(c));
				l2.put(name, j);
			}
		}
		this.set("list", l2.values());
		this.query.path("/admin/database");

		this.show("/admin/database.index.html");

	}

	/**
	 * Delete.
	 */
	@Path(path = "delete", login = true, access = "access.config.admin")
	public void delete() {

		String table = this.getString("table");
		int n = Helper.delete(W.create(), table, Helper.DEFAULT);
		GLog.oplog.warn(database.class, "delete", "table=" + table + ", n=" + n, login, this.getRemoteHost());
		this.response(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("delete.success")));
	}

	@Path(path = "drop", login = true, access = "access.config.admin")
	public void drop() {

		String t = Config.getConf().getString("drop.table");

		if (!X.isIn(t, "t", "yes", "1", "y", "true")) {
			this.response(JSON.create().append(X.STATE, 201).append(X.MESSAGE, "禁止删除，请配置[drop.table=y]"));
			return;
		}

		String table = this.getString("table");
		if (table.contains("*")) {
			List<JSON> l1 = Helper.listTables(Helper.DEFAULT);
			l1.forEach(j1 -> {
				String name = j1.getString("table_name");
				if (name.matches(table)) {
					Helper.drop(name, Helper.DEFAULT);
				}
			});
		} else {
			Helper.drop(table, Helper.DEFAULT);
		}
		GLog.oplog.warn(database.class, "drop", "table=" + table, login, this.getRemoteHost());
		this.response(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("delete.success")));
	}

	@Path(path = "er", login = true, access = "access.config.admin")
	public void er() {

		// GLog.applog.info(backup.class, "create", "method=" + method, login,
		// this.getRemoteHost());

		if (method.isPost()) {
			String[] ss = this.getStrings("name");
			if (ss != null && ss.length > 0) {
				Temp t = Temp.create("er.csv");
				Exporter<Bean> e = t.export("GBK", Exporter.FORMAT.csv);

				for (String s : ss) {

					Class<? extends Bean> c = _getBean(s);
					if (c == null)
						continue;

					Map<String, Class<?>> st = new TreeMap<String, Class<?>>();
					Beans<Bean> bs = Helper.load(s, W.create().sort("created", -1), 0, 10, Bean.class, Helper.DEFAULT);
					for (Bean b : bs) {
						Map<String, Object> m = b.getAll();
						for (String name : m.keySet()) {
							Class<?> c1 = m.get(name).getClass();
							Class<?> c2 = st.get(name);
							if (c2 == null) {
								st.put(name, c1);
							} else if (!X.isSame(c1, c2)) {
								st.put(name, Object.class);
							}
						}
					}

					// TODO
					try {
						e.print(new String[] { "" });
						e.print(new String[] { s });
						e.print(new String[] { lang.get("name." + c.getName()) });
						e.print(new String[] { "Field", "Type", "Memo" });
						for (String s1 : st.keySet()) {
							Class<?> c1 = st.get(s1);
							String t1 = "text";
							if (c1.equals(Integer.class)) {
								t1 = "int";
							} else if (c1.equals(Long.class)) {
								t1 = "bigint";
							} else if (c1.equals(Float.class)) {
								t1 = "float";
							} else if (c1.equals(Double.class)) {
								t1 = "double";
							} else if (c1.isArray()) {
								t1 = "list";
							}
							e.print(new String[] { s1, t1 });
						}
					} catch (Exception e1) {
						log.error(e1.getMessage(), e1);
					}
				}
				e.close();

				this.response(JSON.create().append(X.STATE, 200).append("file", t.getUri()));

			} else {

				this.response(JSON.create().append(X.STATE, 201).append(X.MESSAGE, lang.get("nonselect.error")));
			}
			return;
		}

		List<Class<? extends Bean>> l1 = Schema.beans;
		Map<String, JSON> l2 = new TreeMap<String, JSON>();
		for (Class<? extends Bean> c : l1) {
			String table = Helper.getTable(c);
			if (!X.isEmpty(table) && !l2.containsKey(table)) {
				JSON j = JSON.create().append("name", c.getName()).append("table", table).append("size",
						Helper.count(W.create(), c));
				l2.put(table, j);
			}
		}
		this.set("list", l2.values());
		this.show("/admin/backup.er.html");

	}

	private Class<? extends Bean> _getBean(String table) {
		List<Class<? extends Bean>> l1 = Schema.beans;
		for (Class<? extends Bean> c : l1) {
			if (X.isSame(table, Helper.getTable(c))) {
				return c;
			}
		}
		return null;
	}

}
