package org.giiwa.core.bean;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.GLog;

class Optimizer implements Helper.IOptimizer {

	private static Log log = LogFactory.getLog(Optimizer.class);

	private static HashSet<String> exists = new HashSet<String>();

	@Override
	public void query(final String db, final String table, final W w) {
		if (w != null && !w.isEmpty()) {

			Task.schedule(() -> {

				try {
					// all keys
					List<LinkedHashMap<String, Integer>> l1 = w.sortkeys();

					if (l1 != null) {
						for (LinkedHashMap<String, Integer> keys : l1) {
							StringBuilder sb = new StringBuilder();
							for (String s : keys.keySet()) {

								if (X.isSame(s, "_id"))
									return;

								if (sb.length() > 0)
									sb.append(",");
								sb.append(s).append(":").append(keys.get(s));
							}

							String id = UID.id(db, table, sb.toString());
							if (!exists.contains(id)) {
								_init(db, table);
							}

							if (!exists.contains(id)) {
								exists.add(id);
								if (!keys.isEmpty()) {
									GLog.applog.warn("db", "optimize", "table=" + table + ", key=" + sb.toString(),
											null, null);

									log.debug("db.index, table=" + table + ", create.index=" + sb.toString());

									Helper.createIndex(db, table, keys);
								}
							}

						}
					}
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}

			}, 0);
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

					log.debug("db.index, table=" + table + ", get.index=" + sb.toString());

					String id = UID.id(db, table, sb.toString());
					exists.add(id);

				}
			}
		}
	}

}
