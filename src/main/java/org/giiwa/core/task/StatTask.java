package org.giiwa.core.task;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.framework.bean.Stat;
import org.giiwa.framework.bean.Stat.SIZE;

/**
 * the task which using to stat
 * 
 * @author joe
 *
 */
public abstract class StatTask extends Task {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected static Log log = LogFactory.getLog(StatTask.class);

	public static StatTask inst = null;

	protected long[] time(Stat.SIZE s) {
		if (Stat.SIZE.day.equals(s)) {
			return new long[] { Stat.today() - X.ADAY, Stat.today() };
		} else if (Stat.SIZE.hour.equals(s)) {
			return new long[] { Stat.tohour() - X.AHOUR, Stat.tohour() };
		} else if (Stat.SIZE.min.equals(s)) {
			return new long[] { Stat.tomin() - X.AMINUTE, Stat.tomin() };
		} else if (Stat.SIZE.m10.equals(s)) {
			return new long[] { Stat.tom10() - 10 * X.AMINUTE, Stat.tom10() };
		} else if (Stat.SIZE.m15.equals(s)) {
			return new long[] { Stat.tom15() - 15 * X.AMINUTE, Stat.tom15() };
		} else if (Stat.SIZE.m30.equals(s)) {
			return new long[] { Stat.tom30() - 30 * X.AMINUTE, Stat.tom30() };
		} else if (Stat.SIZE.month.equals(s)) {
			return new long[] { Stat.tomonth() - X.AMONTH, Stat.tomonth() };
		} else if (Stat.SIZE.season.equals(s)) {
			return new long[] { Stat.toseason() - X.AMONTH * 3, Stat.toseason() };
		} else if (Stat.SIZE.week.equals(s)) {
			return new long[] { Stat.toweek() - X.AWEEK, Stat.toweek() };
		} else if (Stat.SIZE.year.equals(s)) {
			return new long[] { Stat.toyear() - X.AYEAR, Stat.toyear() };
		}
		return null;
	}

	protected long[] time1(Stat.SIZE s) {
		if (Stat.SIZE.day.equals(s)) {
			return new long[] { Stat.today(), System.currentTimeMillis() };
		} else if (Stat.SIZE.hour.equals(s)) {
			return new long[] { Stat.tohour(), System.currentTimeMillis() };
		} else if (Stat.SIZE.min.equals(s)) {
			return new long[] { Stat.tomin(), System.currentTimeMillis() };
		} else if (Stat.SIZE.m10.equals(s)) {
			return new long[] { Stat.tom10(), System.currentTimeMillis() };
		} else if (Stat.SIZE.m15.equals(s)) {
			return new long[] { Stat.tom15(), System.currentTimeMillis() };
		} else if (Stat.SIZE.m30.equals(s)) {
			return new long[] { Stat.tom30(), System.currentTimeMillis() };
		} else if (Stat.SIZE.month.equals(s)) {
			return new long[] { Stat.tomonth(), System.currentTimeMillis() };
		} else if (Stat.SIZE.season.equals(s)) {
			return new long[] { Stat.toseason(), System.currentTimeMillis() };
		} else if (Stat.SIZE.week.equals(s)) {
			return new long[] { Stat.toweek(), System.currentTimeMillis() };
		} else if (Stat.SIZE.year.equals(s)) {
			return new long[] { Stat.toyear(), System.currentTimeMillis() };
		}
		return null;
	}

	@Override
	public final void onExecute() {

		String name = this.getName();
		long last = Global.getLong("last.stat." + name, 0);
		if (System.currentTimeMillis() - last >= X.AMINUTE) {
			if (this.tryLock()) {
				try {
					Global.setConfig("last.stat." + name, System.currentTimeMillis());

					List<?> l1 = getCategories();
					if (l1 != null) {
						for (Object o : l1) {

							Stat.SIZE[] ss = this.getSizes();
							for (Stat.SIZE s : ss) {
								long[] time = time(s);
								if (log.isDebugEnabled())
									log.debug("stat - " + this.getName() + ", size=" + s + ", time="
											+ Arrays.toString(time) + ", cat=" + o);
								onStat(s, time[0], time[1], o);

								time = time1(s);
								if (log.isDebugEnabled())
									log.debug("stat - " + this.getName() + ", size=" + s + ", time="
											+ Arrays.toString(time) + ", cat=" + o);
								onStat(s, time[0], time[1], o);
							}
						}
					}

				} finally {
					this.unlock();
				}
			}
		}
	}

	protected abstract void onStat(SIZE size, long start, long end, Object cat);

	protected abstract List<?> getCategories();

	protected Stat.SIZE[] getSizes() {
		return new Stat.SIZE[] { Stat.SIZE.day, Stat.SIZE.week, Stat.SIZE.month, Stat.SIZE.season, Stat.SIZE.year };
	}

	@Override
	public void onFinish() {
		this.schedule(X.AMINUTE * 10);
	}

}
