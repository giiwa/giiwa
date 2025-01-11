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

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.giiwa.bean.GLog;
import org.giiwa.bean.Temp;
import org.giiwa.conf.Config;
import org.giiwa.dao.Bean;
import org.giiwa.dao.Column;
import org.giiwa.dao.Helper;
import org.giiwa.dao.Schema;
import org.giiwa.dao.Table;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.misc.Exporter;
import org.giiwa.task.Task;
import org.giiwa.web.Controller;
import org.giiwa.web.Path;

/**
 * backup management,<br>
 * required "access.config.admin"
 * 
 * @author joe
 *
 */
public class database extends Controller {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model.onGet()
	 */
	@Path(login = true, access = "access.config.admin")
	@Override
	public void onGet() {

		this.show("/admin/database.index.html");

	}

	@Path(login = true, path = "tablelist", access = "access.config.admin")
	public void tablelist() {

		List<JSON> l2 = Schema.load(lang);

		this.set("list", l2);

		this.show("/admin/database.tablelist.html");

	}

	/**
	 * Delete.
	 */
	@Path(path = "delete", login = true, access = "access.config.admin", oplog = true)
	public void delete() {

		String table = this.getString("table");
		Task t = new Task() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public String getName() {
				return "db.delete." + table;
			}

			@Override
			public void onExecute() {
				String[] ss = X.split(table, "[,;]");
				for (String s : ss) {
					if (X.isEmpty(s))
						continue;

					int n = Helper.primary.delete(s, W.create());
					GLog.oplog.warn(database.this, "delete", "table=" + s + ", n=" + n);
				}
			}

		};
		if (!Task.isScheduled(t.getName())) {
			t.schedule(0);
			this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("delete.success")));
		} else {
			this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("in.delete")));
		}
	}

	@Path(path = "drop", login = true, access = "access.config.admin", oplog = true)
	public void drop() {

		String t = Config.getConf().getString("drop.table");

		if (!X.isIn(t, "t", "yes", "1", "y", "true")) {
			this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE, "禁止删除，请配置[drop.table=y]"));
			return;
		}

		String table = this.getString("table");

		Task t1 = new Task() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void onExecute() {
				String[] ss = X.split(table, "[,;]");
				for (String s : ss) {
					if (X.isEmpty(s)) {
						continue;
					}

					Helper.primary.drop(s);
					GLog.oplog.warn(database.this, "drop", "table=" + table);
				}

				Schema.clean();

			}

			@Override
			public String getName() {
				return "db.drop." + table;
			}

		};
		if (!Task.isScheduled(t1.getName())) {
			t1.schedule(0);
			this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("delete.success")));
		} else {
			this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("in.delete")));
		}

	}

	@Path(path = "repair", login = true, access = "access.config.admin", oplog = true)
	public void repair() {

		String table = this.getString("table");

		Task t1 = new Task() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void onExecute() {
				String[] ss = X.split(table, "[,;]");
				for (String s : ss) {
					if (X.isEmpty(s)) {
						continue;
					}

					Helper.primary.repair(table);
					GLog.oplog.warn(database.this, "repair", "table=" + table);
				}

				Schema.clean();

			}

			@Override
			public String getName() {
				return "db.repair." + table;
			}

		};
		if (!Task.isScheduled(t1.getName())) {
			t1.schedule(0);
			this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("repair.success")));
		} else {
			this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("in.repair")));
		}

	}

	@Path(path = "er", login = true, access = "access.config.admin", oplog = true)
	public void er() {

		// GLog.applog.info(backup.class, "create", "method=" + method, login,
		// this.getRemoteHost());

		if (method.isPost()) {
			String[] ss = this.getStrings("name");
			if (ss != null && ss.length > 0) {

				Temp t = Temp.create("er.csv");

				Exporter<Bean> ex = null;

				try {
					ex = Exporter.create(t.getOutputStream(), Exporter.FORMAT.csv, true);

					for (String s : ss) {

						Class<? extends Bean> c = Schema.bean(s);
						if (c == null) {
							ex.print(new String[] { "" });
							ex.print(new String[] { "##", s });
							ex.print(new String[] { lang.get("column.name"), lang.get("column.type"),
									lang.get("column.memo"), lang.get("column.value") });
							continue;
						}

						Table table = (Table) c.getAnnotation(Table.class);
						String display = table.memo();
						if (X.isEmpty(display)) {
							display = lang.get("name." + c.getName());
						}
						Bean b = c.getDeclaredConstructor().newInstance();
						Map<String, Field> st = b.getFields();
						st = new TreeMap<String, Field>(st);

						ex.print(new String[] { "" });
						ex.print(new String[] { display, s });
						ex.print(new String[] { lang.get("column.name"), lang.get("column.type"),
								lang.get("column.memo"), lang.get("column.value") });

						for (String s1 : st.keySet()) {

							Field f1 = st.get(s1);
							Column c1 = f1.getAnnotation(Column.class);
							String t1 = f1.getType().getName();
							int i = t1.lastIndexOf(".");
							if (i > 0) {
								t1 = t1.substring(i + 1);
							}

							ex.print(new String[] { s1, t1, c1 == null ? X.EMPTY : c1.memo(),
									c1 == null ? X.EMPTY : c1.value() });
						}
					}
				} catch (Exception e1) {
					log.error(e1.getMessage(), e1);
				} finally {
					X.close(ex);
				}

				this.send(JSON.create().append(X.STATE, 200).append("file", t.getUri(lang)));

			} else {

				this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE, lang.get("nonselect.error")));
			}
			return;
		}

//		Schema.init();

		List<Class<? extends Bean>> l1 = Schema.beans;
		Map<String, JSON> l2 = new TreeMap<String, JSON>();
		for (Class<? extends Bean> c : l1) {

			try {
				String table = Helper.getTable(c);
				if (!X.isEmpty(table) && !l2.containsKey(table)) {
					JSON j = JSON.create().append("name", c.getName()).append("table", table).append("size",
							Helper.primary.count(table, W.create()));
					l2.put(table, j);
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}

		}
		this.set("list", l2.values());
		this.show("/admin/backup.er.html");

	}

	@Path(path = "status", login = true, access = "access.config.admin")
	public void _status() {

		JSON j1 = Helper.primary.status();

		this.print(j1.toPrettyString());

		j1 = Helper.primary.stats(null);

		this.print(j1.toPrettyString());

	}

}
