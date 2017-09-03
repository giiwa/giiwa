/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
*/
package org.giiwa.framework.bean;

import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Column;
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.framework.web.Language;
import org.giiwa.core.bean.Table;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;

/**
 * The Class Stat is used to stat utility and persistence.
 * 
 * @author wujun
 *
 */
@Table(name = "gi_stat")
public class Stat extends Bean implements Comparable<Stat> {

	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	@Column(name = X.ID)
	protected String id; // 日期

	@Column(name = "module")
	protected String module; // 统计模块

	@Column(name = "date")
	protected String date; // 日期

	@Column(name = "size")
	protected String size; // minute, hour, day, week,
							// month, year

	public String getDate() {
		return date;
	}

	public String getModule() {
		return module;
	}

	/**
	 * Insert or update.
	 *
	 * @param module
	 *            the module
	 * @param date
	 *            the date
	 * @param size
	 *            the size
	 * @param n
	 *            the n
	 * @return the int
	 */
	public static int insertOrUpdate(String module, String date, String size, long... n) {
		String id = UID.id(date, module, size);

		try {
			if (!Helper.exists(W.create("date", date).and("id", id), Stat.class)) {
				V v = V.create("date", date).set(X.ID, id).set("id", id).set("size", size).set("module", module);
				for (int i = 0; i < n.length; i++) {
					v.set("n" + i, n[i]);
				}

				return Helper.insert(v, Stat.class);

			} else {
				/**
				 * only update if count > original
				 */
				V v = V.create();
				for (int i = 0; i < n.length; i++) {
					v.set("n" + i, n[i]);
				}
				return Helper.update(W.create("date", date).and("id", id), v, Stat.class);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return -1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Stat o) {
		if (this == o)
			return 0;

		int c = getDate().compareTo(o.getDate());
		return c;
	}

	/**
	 * Load.
	 *
	 * @param q
	 *            the q
	 * @param s
	 *            the s
	 * @param n
	 *            the n
	 * @return the beans
	 */
	public static Beans<Stat> load(W q, int s, int n) {
		return Helper.load(q, s, n, Stat.class);
	}

	/**
	 * Load.
	 *
	 * @param q
	 *            the q
	 * @return the stat
	 */
	public static Stat load(W q) {
		return Helper.load(q, Stat.class);
	}

	public static void log(String name, String size, long... n) {
		Language lang = Language.getLanguage();

		String date = null;

		if (X.isSame(size, "min")) {
			date = lang.format(System.currentTimeMillis(), "yyyyMMddmm");
		} else if (X.isSame(size, "hour")) {
			date = lang.format(System.currentTimeMillis(), "yyyyMMddHH");
		} else if (X.isSame(size, "day")) {
			date = lang.format(System.currentTimeMillis(), "yyyyMMdd");
		} else if (X.isSame(size, "month")) {
			date = lang.format(System.currentTimeMillis(), "yyyyMM");
		} else if (X.isSame(size, "year")) {
			date = lang.format(System.currentTimeMillis(), "yyyy");
		} else {
			log.error("not support the [" + size + "], supported: min, hour, day, month, year");
			return;
		}

		Stat s1 = Stat.load(W.create("module", name + ".snapshot").and("size", size).and("date", date, W.OP.neq)
				.sort("created", -1));
		long[] d = new long[n.length];
		for (int i = 0; i < d.length; i++) {
			d[i] = s1 == null ? n[i] : n[i] - s1.getLong("n" + i);
		}
		Stat.insertOrUpdate(name + ".snapshot", date, size, n);
		Stat.insertOrUpdate(name + ".delta", date, size, d);

	}

}
