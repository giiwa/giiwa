package org.giiwa.core.bean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.giiwa.core.base.ClassUtil;

public class Schema {

	public static void add(String packname) {

		List<Class<Bean>> l1 = ClassUtil.listSubType(packname, Bean.class);
		if (l1 != null) {
			for (Class<Bean> t : l1) {
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

	public static List<Class<? extends Bean>> beans = new ArrayList<Class<? extends Bean>>();

}
