package org.giiwa.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.giiwa.bean.Data;
import org.giiwa.cache.Cache;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.misc.ClassUtil;
import org.giiwa.task.Task;
import org.giiwa.web.Language;

public class Schema {

	public static void init() {

		// scan all Bean
		List<Class<Bean>> l1 = ClassUtil.listSubType("", Bean.class);
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

	/**
	 * @deprecated
	 * @param packname
	 */
	public static void add(String packname) {

	}

	public static List<Class<? extends Bean>> beans = new ArrayList<Class<? extends Bean>>();

	public static Class<? extends Bean> bean(String table) {
		List<Class<? extends Bean>> l1 = Schema.beans;
		for (Class<? extends Bean> c : l1) {
			if (X.isSame(table, Helper.getTable(c))) {
				return c;
			}
		}
		return null;
	}

	public static JSON format(JSON e, Language lang) {

		String tablename = e.getString("table_name");

		Class<? extends Bean> c = bean(tablename);

		Table table = c == null ? null : (Table) c.getAnnotation(Table.class);
		if (table == null) {

			// log.error("table missed/error in [" + t + "] declaretion", new Exception());
			JSON j1 = JSON.create().append("table", tablename).append("display", tablename);
			j1.append("total", Helper.count(W.create(), tablename, Helper.DEFAULT)).append("size",
					Helper.size(tablename, Helper.DEFAULT));

			Data d = Helper.load(tablename, W.create().sort("updated", -1), Data.class);
			if (d != null) {
				j1.append("updated", d.getUpdated());
			}

			return j1;
		}

		String display = table.memo();
		if (X.isEmpty(display)) {
			display = lang.get("name." + c.getName());
		}

		JSON j1 = JSON.create().append("name", c.getName()).append("table", table.name()).append("display", display);
		j1.append("total", Helper.count(W.create(), c)).append("size", Helper.size(c));

		Data d = Helper.load(tablename, W.create().sort("updated", -1), Data.class);
		if (d != null) {
			j1.append("updated", d.getUpdated());
		}

		return j1;
	}

	public static List<JSON> load(Language lang) {

		List<JSON> l = Cache.get("db.schema");

		if (l == null) {
			Task.schedule("db.schema.reload", () -> {
				List<JSON> l3 = JSON.createList();

				List<JSON> l1 = Helper.listTables(Helper.DEFAULT);
				l1.forEach(e -> {
					JSON j1 = Schema.format(e, lang);
					l3.add(j1);
				});
				Collections.sort(l3, new Comparator<JSON>() {

					@Override
					public int compare(JSON o1, JSON o2) {
						return o1.getString("table").compareTo(o2.getString("table"));
					}

				});

				Cache.set("db.schema", l3, X.AMINUTE * 10);
			});
		}

		return l;

	}

}
