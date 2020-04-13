package org.giiwa.dao;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.GLog;
import org.giiwa.conf.Global;
import org.giiwa.dao.Helper.W;
import org.giiwa.task.Task;
import org.giiwa.web.Language;

class Optimizer implements Helper.IOptimizer {

	private static Log log = LogFactory.getLog(Optimizer.class);

	private static HashSet<String> exists = new HashSet<String>();

	private static Queue<Object[]> queue = new ArrayBlockingQueue<Object[]>(100);

	private Task checker = null;

	@Override
	public void query(final String db, final String table, final W w) {

		// check the db.optimizer=1 ?
		if (Global.getInt("db.optimizer", 1) != 1)
			return;

		if (w != null && !w.isEmpty()) {

			try {
				List<LinkedHashMap<String, Integer>> l1 = w.sortkeys();
				if (l1 != null) {
					l1.forEach(e -> {
						StringBuilder sb = new StringBuilder();
						for (String s : e.keySet()) {

							if (X.isSame(s, "_id"))
								return;

							if (sb.length() > 0)
								sb.append(",");
							sb.append(s).append(":").append(e.get(s));
						}
						String id = UID.id(db, table, sb.toString());
						if (!exists.contains(id)) {
							_init(db, table);

							if (!exists.contains(id)) {
								if (queue.size() < 20) {
									queue.add(new Object[] { db, table, e });
								} else {
									log.warn("optimizer drop the [" + db + ", " + table + ", " + e + "]");
								}
								exists.add(id);
							}
						}
					});
				}
			} catch (Throwable e) {
				// ignore
			}

			if (queue.isEmpty())
				return;

//			log.debug("open", new Exception());

			if (checker == null) {

				checker = new Task() {

					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public String getName() {
						return "gi.db.optimizer";
					}

					@SuppressWarnings("unchecked")
					@Override
					public void onExecute() {

						// check the time, db.optimizer.time
						String time = Global.getString("db.optimizer.time", null);
						if (!X.isEmpty(time)) {
							String t1 = Language.getLanguage().format(System.currentTimeMillis(), "HH:mm");
							String[] ss = X.split(time, "-");
							if (ss.length == 2) {
								if (ss[0].compareTo(ss[1]) < 0) {
									if (t1.compareTo(ss[0]) < 0 || t1.compareTo(ss[1]) > 0) {
										// not in time
										return;
									}
								} else if (ss[0].compareTo(ss[1]) > 0) {
									if (t1.compareTo(ss[0]) > 0 || t1.compareTo(ss[1]) < 0) {
										// not in time
										return;
									}
								}
							}
						}

						try {
							Object[] o = queue.remove();
							while (o != null) {
								String db = (String) o[0];
								String table = (String) o[1];
								LinkedHashMap<String, Integer> keys = (LinkedHashMap<String, Integer>) o[2];

								if (!keys.isEmpty()) {
									GLog.applog.warn("db", "optimize", "table=" + table + ", key=" + keys.toString(),
											null, null);

									if (log.isDebugEnabled())
										log.debug("db.index, table=" + table + ", create.index=" + keys.toString());

									Helper.createIndex(db, table, keys);

									queue.add(o);

								}

								o = queue.remove();
							}
						} catch (Throwable e) {
							// ignore
						}
					}

					@Override
					public void onFinish() {
						if (!queue.isEmpty()) {
							this.schedule(X.AMINUTE);
						}
					}

				};
			}

			checker.schedule(0);

		}

	}

	@SuppressWarnings("unchecked")
	private static void _init(String db, String table) {

		List<Map<String, Object>> l1 = Helper.getIndexes(table, db);

//		GLog.applog.info("db", "get", "db.index, table=" + table + ", get.index.1=" + l1, null, null);

		if (l1 != null && !l1.isEmpty()) {
			for (Map<String, Object> d : l1) {
				Map<String, Object> keys = (Map<String, Object>) d.get("key");
				if (keys != null && !keys.isEmpty()) {
					StringBuilder sb = new StringBuilder();
					for (String s : keys.keySet()) {
						if (sb.length() > 0)
							sb.append(",");
						sb.append(s).append(":").append(X.toInt(keys.get(s)));
					}

					// GLog.applog.info("db", "get", "db.index, table=" + table + ", get.index=" +
					// sb.toString(), null,
					// null);

					if (log.isDebugEnabled())
						log.debug("db.index, table=" + table + ", get.index=" + sb.toString());

					String id = UID.id(db, table, sb.toString());
					exists.add(id);

				}
			}
		}
	}

	public static void main(String[] args) {
		String s1 = "02:00";
		String s2 = "06:00";

		System.out.println(s1.compareTo(s2));

	}

}
