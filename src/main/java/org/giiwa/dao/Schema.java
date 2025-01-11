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
package org.giiwa.dao;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.Lock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Data;
import org.giiwa.cache.Cache;
import org.giiwa.conf.Global;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.misc.ClassUtil;
import org.giiwa.task.Task;
import org.giiwa.web.Language;

/**
 * 
 * 自动生成数据库模型
 * 
 * @author joe
 *
 */
public final class Schema implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static boolean _inited = false;

	private static Log log = LogFactory.getLog(Schema.class);

	private static List<String> _packages = new ArrayList<String>();

	public static void add(String packname) {
		if (!_packages.contains(packname)) {
			_packages.add(packname);
		}
	}

	public synchronized static void init() {

		if (_inited) {
			return;
		}

		_inited = true;

		// scan all Bean
		List<Class<Bean>> l1 = ClassUtil.listSubType(_packages, Bean.class);
		if (l1 != null) {
			for (Class<Bean> t : l1) {

				Table table = (Table) t.getAnnotation(Table.class);
				if (table == null || X.isEmpty(table.name())) {
					continue;
				}

				if (!beans.contains(t)) {
					Helper.init(table.name());
					beans.add(t);
				}
			}

			Collections.sort(beans, new Comparator<Class<? extends Bean>>() {

				@Override
				public int compare(Class<? extends Bean> o1, Class<? extends Bean> o2) {
					return o1.getName().compareTo(o2.getName());
				}

			});
		}

		load(Language.getLanguage());

	}

	public static List<Class<? extends Bean>> beans = new ArrayList<Class<? extends Bean>>();

	public static Class<? extends Bean> bean(String table) throws SQLException {

		init();

		List<Class<? extends Bean>> l1 = Schema.beans;

		log.info("l1=" + l1);

		for (Class<? extends Bean> c : l1) {
			if (X.isSame(table, Helper.getTable(c))) {
				return c;
			}
		}
		return null;
	}

	/**
	 * load all Bean by tag
	 * 
	 * @param tag
	 * @return
	 */
	public static List<Class<? extends Bean>> loadByTag(String tag) {

		Schema.init();

		List<Class<? extends Bean>> l1 = Schema.beans;
		List<Class<? extends Bean>> l2 = new ArrayList<Class<? extends Bean>>();

		for (Class<? extends Bean> c : l1) {
			Table t1 = c.getAnnotation(Table.class);
			if (t1 != null && X.isIn(tag, X.split(t1.tag(), "[, ]"))) {
				l2.add(c);
			}
		}
		return l2;

	}

	@SuppressWarnings({ "unused" })
	public static JSON format(JSON e, Language lang) throws SQLException {

		String tablename = e.getString("name");

		Class<? extends Bean> c = null;// bean(tablename);

		Table table = c == null ? null : (Table) c.getAnnotation(Table.class);
		if (table == null) {

			// log.error("table missed/error in [" + t + "] declaretion", new Exception());
			JSON j1 = JSON.create().append("table", tablename).append("display", tablename);

			JSON stat = Helper.stats(tablename);

			j1.append("count", Helper.primary.count(tablename, W.create()))
					.append("totalsize", stat == null ? null : stat.getLong("totalSize"))
					.append("indexsize", stat == null ? null : stat.getLong("totalIndexSize"));

			Data d = Helper.primary.load(tablename, W.create().sort("updated", -1), Data.class);
			if (d != null) {
				j1.append("updated", d.getUpdated());
			}

			return j1;
		}

		String display = table.memo();
		if (X.isEmpty(display)) {
			display = lang.get("name." + c.getName());
		}

		JSON stat = Helper.primary.stats(table.name());

		JSON j1 = JSON.create().append("name", c.getName()).append("table", table.name()).append("display", display);
		j1.append("count", Helper.primary.count(table.name(), W.create()))
				.append("totalsize", stat == null ? null : stat.getLong("totalSize"))
				.append("indexsize", stat == null ? null : stat.getLong("totalIndexSize"));

		Data d = Helper.primary.load(tablename, W.create().sort("updated", -1), Data.class);
		if (d != null) {
			j1.append("updated", d.getUpdated());
		}

		return j1;
	}

	public static void clean() {
		String name = "db//schema";
		Cache.remove(name);
	}

	public static List<JSON> load(Language lang) {

		String name = "db//schema";
		List<JSON> l3 = null;
		try {
			l3 = Cache.get(name);
		} catch (Throwable e) {
			// ignore
		}
		
		if (l3 == null) {
			Lock door = Global.getLock(name);
			if (door.tryLock()) {
				try {
					List<JSON> l4 = JSON.createList();

					List<JSON> l1 = Helper.primary.listTables(null, 10000);
					if (l1 != null) {
						Task.forEach(l1, e -> {
							try {
								JSON j1 = Schema.format(e, lang);
								l4.add(j1);
							} catch (Exception err) {
								log.error(err.getMessage(), err);
							}
						});
					}
					Collections.sort(l4, new Comparator<JSON>() {

						@Override
						public int compare(JSON o1, JSON o2) {
							return o1.getString("table").compareTo(o2.getString("table"));
						}

					});
//					log.warn("l4=" + l4);

					Cache.set(name, l4, X.AMINUTE);
					l3 = l4;
				} finally {
					door.unlock();
				}
			}
		}
		return l3;

	}

}
