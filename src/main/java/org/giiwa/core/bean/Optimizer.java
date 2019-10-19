package org.giiwa.core.bean;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.task.Task;

class Optimizer implements Helper.IOptimizer {

	private static Log log = LogFactory.getLog(Optimizer.class);

	private static HashSet<String> exists = new HashSet<String>();

	private static Queue<Object[]> queue = new ArrayBlockingQueue<Object[]>(100);

	private Task checker = null;

	@Override
	public void query(final String db, final String table, final W w) {
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
								queue.add(new Object[] { db, table, e });
								exists.add(id);
							}
						}
					});
				}
			} catch (Throwable e) {
				// ignore
			}

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

						try {
							Object[] o = queue.remove();
							while (o != null) {
								String db = (String) o[0];
								String table = (String) o[1];
								LinkedHashMap<String, Integer> keys = (LinkedHashMap<String, Integer>) o[2];

								if (!keys.isEmpty()) {
//									GLog.applog.warn("db", "optimize", "table=" + table + ", key=" + keys.toString(),
//											null, null);

									if (log.isDebugEnabled())
										log.debug("db.index, table=" + table + ", create.index=" + keys.toString());

									Helper.createIndex(db, table, keys);
								}

								o = queue.remove();
							}
						} catch (Throwable e) {
							// ignore
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

}
