package org.giiwa.task;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Stat;
import org.giiwa.conf.Global;
import org.giiwa.dao.X;

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

	@Override
	public final void onExecute() {

		String name = this.getName();
		long last = Global.getLong("last.stat." + name, 0);
		if (System.currentTimeMillis() - last >= X.AMINUTE) {
			Global.setConfig("last.stat." + name, System.currentTimeMillis());

			List<?> l1 = getCategories();
			if (l1 != null) {
				for (Object o : l1) {

					for (Stat.SIZE s : ss) {

						long time2 = Stat.tomin();
						long time1 = time2 - X.AMINUTE;

						if (log.isDebugEnabled())
							log.debug("stat - " + this.getName() + ", size=" + s + ", time=" + (time1 + "-" + time2)
									+ ", cat=" + o);
						onStat(time1, time2, o);
					}
				}
			}

		}

	}

	protected abstract void onStat(long start, long end, Object cat);

	protected abstract List<?> getCategories();

	private Stat.SIZE[] ss = new Stat.SIZE[] { Stat.SIZE.m10, Stat.SIZE.m15, Stat.SIZE.m30, Stat.SIZE.hour,
			Stat.SIZE.day, Stat.SIZE.week, Stat.SIZE.month, Stat.SIZE.season, Stat.SIZE.year };

	@Override
	public void onFinish() {
		this.schedule(X.AMINUTE);
	}

}
