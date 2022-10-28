package org.giiwa.dao;

import java.sql.SQLException;
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
import org.giiwa.dao.Helper.DBHelper;
import org.giiwa.dao.Helper.W;
import org.giiwa.task.Task;

public final class Optimizer {

	public static final long MIN = 1000;

	private static Log log = LogFactory.getLog(Optimizer.class);

	private HashSet<String> exists = new HashSet<String>();

	private Queue<Object[]> queue = new ArrayBlockingQueue<Object[]>(20);

	private DBHelper def = null;

	public static Optimizer inst = new Optimizer(Helper.primary);

	public Optimizer(DBHelper db) {
		def = db;
	}

	public boolean check(String table, W q) throws SQLException {

		List<LinkedHashMap<String, Object>> l1 = q.sortkeys();
		if (l1 != null) {
			out: for (LinkedHashMap<String, Object> e : l1) {
				StringBuilder sb = new StringBuilder();
				for (String s : e.keySet()) {

					if (X.isIn(s, "_id")) {
						continue out;
					}
					if (X.isIn(s, "id") && e.size() > 1) {
						continue out;
					}

					if (sb.length() > 0)
						sb.append(",");
					sb.append(s).append(":").append(e.get(s));
				}
				String id = UID.id(table, sb.toString());

				if (!exists.contains(id)) {
					_init(table);

					if (!exists.contains(id)) {
						StringBuilder sb1 = new StringBuilder();
						for (String s1 : e.keySet()) {
							if (sb1.length() > 0) {
								sb1.append(" and ");
							}
							sb1.append(s1).append("=").append(e.get(s1));
						}
						if (Global.getInt("db.optimizer", 1) == 0) {
							throw new SQLException("optimizing required: " + sb1.toString());
						} else {
							optimize(table, q);
						}
					}
				}
			}
		}

		return true;

	}

	public void query(final String table, final W q) {

		if (q != null && !q.isEmpty()) {

			try {
				List<LinkedHashMap<String, Object>> l1 = q.sortkeys();
				if (l1 != null) {
					l1.forEach(e -> {
						StringBuilder sb = new StringBuilder();
						for (String s : e.keySet()) {

							if (X.isIn(s, "_id")) {
								return;
							}

							if (X.isIn(s, "id") && e.size() > 1) {
								return;
							}

							if (sb.length() > 0)
								sb.append(",");
							sb.append(s).append(":").append(e.get(s));
						}

//						if (log.isDebugEnabled()) {
//							log.debug("optimizer.query, table=" + table + ", sb=" + sb);
//						}

						String id = UID.id(table, sb.toString());
						if (!exists.contains(id)) {
							_init(table);

							if (!exists.contains(id)) {
								if (queue.size() < 20) {
									exists.add(id);
									queue.add(new Object[] { table, e });

									if (log.isWarnEnabled()) {
										log.warn("optimizer.query, table=" + table + ", query.size=" + queue.size());
									}

									checker.schedule(0);
								} else if (log.isDebugEnabled()) {
									log.debug("optimizer, table=" + table + ", drop the [" + q + "], size="
											+ queue.size());
								}
							} else if (log.isDebugEnabled()) {
								log.debug("optimizer, table=" + table + ", exists, q=" + q);
							}
						} else if (log.isDebugEnabled()) {
							log.debug("optimizer, table=" + table + ", exists, q=" + q);
						}
					});
				} else if (log.isDebugEnabled()) {
					log.debug("optimizer, table=" + table + ", sortkey is null, q=" + q);
				}
			} catch (Throwable e) {
				// ignore
				log.error(e.getMessage(), e);
				GLog.applog.error("db", "optimize", "table=" + table + ", query=" + q, e);
			}

			if (queue.isEmpty()) {
				return;
			}

//			log.debug("open", new Exception());

			checker.schedule(0);

		}

	}

	public void optimize(String table, W q) {

		List<LinkedHashMap<String, Object>> l1 = q.sortkeys();
		if (l1 != null) {
			l1.forEach(e -> {
				StringBuilder sb = new StringBuilder();
				for (String s : e.keySet()) {

					if (X.isIn(s, "_id")) {
						return;
					}

					if (X.isIn(s, "id") && e.size() > 1) {
						return;
					}

					if (sb.length() > 0)
						sb.append(",");
					sb.append(s).append(":").append(e.get(s));
				}
				String id = UID.id(table, sb.toString());

				if (!exists.contains(id)) {
					_init(table);

					if (!exists.contains(id)) {
						if (queue.size() < 20) {
							exists.add(id);

							log.warn("create index [" + table + "], e=" + e);
							Helper.createIndex(table, e, false);

						} else {
							log.warn("optimizer, table=" + table + ", drop the [" + q + "]");
						}
					} else if (log.isDebugEnabled()) {
						log.debug("optimizer, table=" + table + ", sortkey exists, q=" + q);
					}
				}
			});
		}

	}

	@SuppressWarnings({ "unchecked" })
	private void _init(String table) {

		List<Map<String, Object>> l1 = def.getIndexes(table);

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

					if (log.isDebugEnabled()) {
						log.debug("db.index, table=" + table + ", get.index=" + sb.toString());
					}

					String id = UID.id(table, sb.toString());
					exists.add(id);

				}
			}
		}
	}

	private Task checker = new Task() {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public String getName() {
			return "gi.db.optimizer";
		}

		@Override
		public int getPriority() {
			return Thread.MIN_PRIORITY;
		}

		@SuppressWarnings({ "unchecked" })
		@Override
		public void onExecute() {

			// check the db.optimizer=1 ?

//			log.warn("optimizer.check starting");

			Object[] o = queue.isEmpty() ? null : queue.remove();
			while (o != null) {

				String table = (String) o[0];
				LinkedHashMap<String, Object> keys = (LinkedHashMap<String, Object>) o[1];
				try {

					if (!keys.isEmpty()) {

						if (Global.getInt("db.optimizer", 1) == 1) {

							log.warn("optimizer, table=" + table + ", create.index=" + keys.toString()
									+ ", queue.size=" + queue.size());
							Helper.createIndex(table, keys, false);
						} else {

							if (log.isWarnEnabled()) {
								log.warn("optimizer, table=" + table + ", disabled");
							}

							GLog.applog.warn("db", "optimize", "required, table=" + table + ", keys=" + keys);
						}

					}
				} catch (Throwable e) {
					// ignore
					log.error(e.getMessage(), e);
					GLog.applog.error("db", "optimize", "table=" + table + ", key=" + keys, e);
				}

				o = queue.isEmpty() ? null : queue.remove();
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
