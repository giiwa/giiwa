/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
*/
package org.giiwa.framework.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.BeanDAO;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Column;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.conf.Global;
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

	public static final BeanDAO<Long, Stat> dao = BeanDAO.create(Stat.class);

	private static Language lang = Language.getLanguage();

	public static enum SIZE {
		min, hour, day, week, month, season, year
	};

	public static enum TYPE {
		delta, snapshot;
	}

	@Column(name = X.ID)
	protected long id;

	@Column(name = "module")
	protected String module; // 统计模块

	@Column(name = "date")
	protected String date; // 日期

	@Column(name = "size")
	protected String size;
	// min, hour, day, week,month, year

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
	public static int insertOrUpdate(String module, String date, SIZE size, V v, long... n) {
		if (v == null) {
			v = V.create();
		} else {
			v = v.copy();
		}

		try {
			W q = W.create().copy(v).and("date", date).and("size", size.toString()).and("module", module);

			if (!dao.exists(q)) {

				long id = UID.next("stat.id");
				while (dao.exists(id)) {
					id = UID.next("stat.id");
				}
				v.append("date", date).force(X.ID, id).append("size", size.toString()).append("module", module);
				for (int i = 0; i < n.length; i++) {
					v.set("n" + i, n[i]);
				}

				return dao.insert(v);

			} else {
				/**
				 * only update if count > original
				 */
				for (int i = 0; i < n.length; i++) {
					v.set("n" + i, n[i]);
				}
				return dao.update(q, v);
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

	public static void delta(String name, SIZE[] sizes, V v, long... n) {
		if (sizes != null) {
			for (SIZE size : sizes) {
				delta(name, size, v, n);
			}
		}
	}

	public static long[] time(SIZE size, String date) {
		if (SIZE.min == size) {
			long t = lang.parse(date, "yyyy-MM-dd/HH");
			t = Stat.tohour(t);
			return new long[] { t, t + X.AHOUR };
		} else if (SIZE.hour == size) {
			long t = lang.parse(date, "yyyy-MM-dd");
			t = Stat.today(t);
			return new long[] { t, t + X.ADAY };
		} else if (SIZE.day == size) {
			long t = lang.parse(date, "yyyy-MM");
			t = Stat.tomonth(t);
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(t);
			c.add(Calendar.MONTH, 1);
			return new long[] { t, c.getTimeInMillis() };
		} else if (SIZE.month == size) {
			long t = lang.parse(date, "yyyy");
			t = Stat.toyear(t);
			return new long[] { t, t + X.AYEAR };
		}

		return new long[] { 0, 0 };
	}

	public static void delta(String name, SIZE size, V v, long... n) {
		delta(System.currentTimeMillis(), name, size, W.create().copy(v), v, n);
	}

	public static void delta(String name, SIZE size, W q, V v, long... n) {
		delta(System.currentTimeMillis(), name, size, q, v, n);
	}

	public static void delta(long time, String name, SIZE size, W q, V v, long... n) {

		String date = format(time, size);

		Stat s1 = dao.load(q.and("module", name + "." + Stat.TYPE.snapshot).and("size", size.toString())
				.and("date", date, W.OP.neq).sort("created", -1));
		long[] d = new long[n.length];
		for (int i = 0; i < d.length; i++) {
			d[i] = s1 == null ? n[i] : n[i] + s1.getLong("n" + i);
		}
		Stat.insertOrUpdate(name + "." + Stat.TYPE.snapshot, date, size, v, d);
		Stat.insertOrUpdate(name + "." + Stat.TYPE.delta, date, size, v, n);

	}

	public static void snapshot(String name, SIZE[] sizes, W q, V v, long... n) {
		if (sizes != null) {
			for (SIZE size : sizes) {
				snapshot(name, size, q, v, n);
			}
		}
	}

	public static void snapshot(String name, SIZE size, W q, V v, long... n) {
		snapshot(System.currentTimeMillis(), name, size, q, v, n);
	}

	public static String format(long time, SIZE size) {

		if (SIZE.min == size) {
			return lang.format(time, "yyyy-MM-dd/HH:mm");
		} else if (SIZE.hour == size) {
			return lang.format(time, "yyyy-MM-dd/HH");
		} else if (SIZE.day == size) {
			return lang.format(time, "yyyy-MM-dd");
		} else if (SIZE.week == size) {
			return lang.format(time, "yyyy|ww");
		} else if (SIZE.month == size) {
			return lang.format(time, "yyyy-MM");
		} else if (SIZE.season == size) {
			int season = X.toInt(lang.format(time, "MM")) / 3 + 1;
			return lang.format(time, "yyyy") + "/0" + season;
		} else if (SIZE.year == size) {
			return lang.format(time, "yyyy");
		} else {
			return lang.format(time, "yyyy-MM-dd");
		}
	}

	public static long parse(long time, SIZE size) {

		if (SIZE.min == size) {
			return time / X.AMINUTE * X.AMINUTE;
		} else if (SIZE.hour == size) {
			return time / X.AHOUR * X.AHOUR;
		} else if (SIZE.day == size) {
			return time / X.ADAY * X.ADAY;
		} else if (SIZE.week == size) {
			return time / X.AWEEK * X.AWEEK;
		} else if (SIZE.month == size) {
			return time / X.AMONTH * X.AMONTH;
		} else if (SIZE.season == size) {
			return time / X.AMONTH / 3 * X.AMINUTE * 3;
		} else if (SIZE.year == size) {
			return time / X.AYEAR * X.AYEAR;
		}

		return time;
	}

	/**
	 * 
	 * @param name
	 * @param size
	 * @param n
	 */
	public static void snapshot(long time, String name, SIZE size, W q, V v, long... n) {

		String date = format(time, size);

		Stat s1 = dao.load(q.and("module", name + "." + TYPE.snapshot).and("size", size.toString())
				.and("date", date, W.OP.neq).sort("created", -1));
		long[] d = new long[n.length];
		for (int i = 0; i < d.length; i++) {
			d[i] = s1 == null ? n[i] : n[i] - s1.getLong("n" + i);
		}
		Stat.insertOrUpdate(name + "." + Stat.TYPE.snapshot, date, size, v, n);
		Stat.insertOrUpdate(name + "." + Stat.TYPE.delta, date, size, v, d);

	}

	public static Beans<Stat> load(String name, TYPE type, SIZE size, W q, int s, int n) {
		if (q == null) {
			q = W.create();
		}
		q.and("module", name + "." + type).and("size", size.toString());

		return dao.load(q, s, n);
	}

	public static Stat load(String name, TYPE type, SIZE size, W q) {
		if (q == null) {
			q = W.create();
		}
		q.and("module", name + "." + type).and("size", size.toString());

		return dao.load(q);
	}

	public static long max(String field, String name, SIZE size, W q) {
		if (q == null) {
			q = W.create();
		}
		q.and("module", name + "." + TYPE.delta).and("size", size.toString());
		return X.toLong((Object) dao.max(field, q));
	}

	public static long sum(String field, String name, SIZE size, W q) {
		if (q == null) {
			q = W.create();
		}
		q.and("module", name + "." + TYPE.delta).and("size", size.toString());
		return X.toLong((Object) dao.sum(field, q));
	}

	public static long avg(String field, String name, SIZE size, W q) {
		if (q == null) {
			q = W.create();
		}
		q.and("module", name + "." + TYPE.delta).and("size", size.toString());
		return X.toLong((Object) dao.avg(field, q));
	}

	public static long min(String field, String name, SIZE size, W q) {
		if (q == null) {
			q = W.create();
		}
		q.and("module", name + "." + TYPE.delta).and("size", size.toString());
		return X.toLong((Object) dao.min(field, q));
	}

	/**
	 * the start time of today
	 * 
	 * @return
	 */
	public static long today(long time) {
		return lang.parse(lang.format(time, "yyyy-MM-dd"), "yyyy-MM-dd");
	}

	public static long today() {
		return today(System.currentTimeMillis());
	}

	public static long tohour() {
		return tohour(System.currentTimeMillis());
	}

	public static long tohour(long time) {
		return lang.parse(lang.format(time, "yyyyMMddHH"), "yyyyMMddHH");
	}

	public static long tomin() {
		return tomin(System.currentTimeMillis());
	}

	public static long tomin(long time) {
		return lang.parse(lang.format(time, "yyyyMMddHHmm"), "yyyyMMddHHmm");
	}

	/**
	 * the start time of this week
	 * 
	 * @return
	 */
	public static long toweek() {
		return toweek(System.currentTimeMillis());
	}

	public static long toweek(long time) {
		return lang.parse(lang.format(time, "yyyy-w"), "yyyy-w");
	}

	/**
	 * the start time of this month
	 * 
	 * @return
	 */
	public static long tomonth() {
		return tomonth(System.currentTimeMillis());
	}

	public static long tomonth(long time) {
		return lang.parse(lang.format(time, "yyyy-MM"), "yyyy-MM");
	}

	/**
	 * the start time of this season
	 * 
	 * @return
	 */
	public static long toseason() {
		return toseason(System.currentTimeMillis());
	}

	public static long toseason(long time) {
		int season = X.toInt(lang.format(time, "MM")) / 3;
		return lang.parse(lang.format(time, "yyyy") + "-" + (season * 3), "yyyy-MM");
	}

	/**
	 * the start time of this year
	 * 
	 * @return
	 */
	public static long toyear() {
		return toyear(System.currentTimeMillis());
	}

	public static long toyear(long time) {
		return lang.parse(lang.format(time, "yyyy"), "yyyy");
	}

	@Override
	public void cleanup() {
		// min, hour, day, week,month, year

		int days = Global.getInt("glog.keep.days", 7);

		int n = dao.delete(W.create("size", "min").and("created", System.currentTimeMillis() - X.ADAY, W.OP.lt));
		n += dao.delete(W.create("size", "hour").and("created", System.currentTimeMillis() - days * X.ADAY, W.OP.lt));
		n += dao.delete(W.create("size", "day").and("created", System.currentTimeMillis() - days * X.AMONTH, W.OP.lt));
		n += dao.delete(W.create("size", "month").and("created", System.currentTimeMillis() - days * X.AYEAR, W.OP.lt));
		n += dao.delete(
				W.create("size", "season").and("created", System.currentTimeMillis() - days * X.AYEAR, W.OP.lt));
		n += dao.delete(W.create("size", "year").and("created", System.currentTimeMillis() - days * X.AYEAR, W.OP.lt));

		if (n > 0) {
			GLog.applog.info("dao", "cleanup", dao.tableName() + " cleanup=" + n, null, null);
		}

	}

	public static List<Stat> merge(W q, String groupby, MergeFunc func) {
		// load from stat, and group
		List<Stat> l1 = new ArrayList<Stat>();
		List<?> l2 = dao.distinct(groupby, q.copy().and(groupby, null, W.OP.neq).and(groupby, X.EMPTY, W.OP.neq));
		if (l2 != null) {
			for (Object o : l2) {
				Beans<Stat> bs = Stat.dao.load(q.copy().and(groupby, o), 0, 10000);
				if (!bs.isEmpty()) {
					Stat s = bs.get(0);

					for (String name : s.keySet()) {
						if (name.startsWith("n")) {
							Object o1 = s.get(name);
							if (o1 instanceof Long) {
								long v = func.call(name, bs);
								s.set(name, v);
							}
						}
					}

					l1.add(s);
				}
			}
		}
		return l1;

	}

	@FunctionalInterface
	public static interface MergeFunc extends Serializable {
		public long call(String name, List<Stat> l1);
	}

	public static void main(String[] args) {
		long t = System.currentTimeMillis();

		SIZE s1 = SIZE.day;
		String s2 = Stat.format(t, s1);
		long[] ss = Stat.time(s1, s2);

		System.out.println(s2 + ", " + Stat.format(ss[0], s1) + ", " + Stat.format(ss[1], s1));

	}

}
