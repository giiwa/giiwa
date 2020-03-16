package org.giiwa.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.giiwa.misc.ClassUtil;

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

	}

	/**
	 * @deprecated
	 * @param packname
	 */
	public static void add(String packname) {

	}

	public static List<Class<? extends Bean>> beans = new ArrayList<Class<? extends Bean>>();

}
