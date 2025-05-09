/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.dao;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
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

	private HashMap<String, HashMap<String, Object>> exists = new HashMap<String, HashMap<String, Object>>();

	private Queue<Object[]> queue = new ArrayBlockingQueue<Object[]>(20);

	private DBHelper helper = null;

//	public static Optimizer inst = new Optimizer(Helper.primary);

	public Optimizer(DBHelper helper) {
		this.helper = helper;
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

				if (!_exists(table, id, sb.toString())) {

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
						query(table, q);
					}
				}
			}
		}

		return true;

	}

	public void query(final String table, final W q) {

		if (!table.startsWith("gi_") && Global.getInt("db.optimizer", 1) == 0) {
//			log.warn("optimize rquried for, table=" + table + ", q=" + q);
			return;
		}

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

						String id = UID.id(table, sb.toString());
						if (!_exists(table, id, sb.toString())) {

							if (queue.size() < 20) {

								HashMap<String, Object> m = exists.get(table);
								if (m == null) {
									return;
								}

								synchronized (m) {
									m.put(id, sb.toString());
								}

								queue.add(new Object[] { table, e });

								if (log.isWarnEnabled()) {
									log.warn("optimizer.query, table=" + table + ", query.size=" + queue.size());
								}

								checker.schedule(0);
							} else if (log.isWarnEnabled()) {
								log.warn("optimizer, too many pending, table=" + table + ", drop the [" + q
										+ "], pending=" + queue.size());
							}
						}
					});
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

	private static Set<String> _ignore = new HashSet<String>();

	public void optimize(String table, W q) {

		synchronized (_ignore) {
			String id = UID.id(table, q);
			if (_ignore.contains(id)) {
				return;
			}
			_ignore.add(id);
		}

		Task.schedule("optimize." + table, t -> {
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

					if (!_exists(table, id, sb.toString())) {

						HashMap<String, Object> m = exists.get(table);
						if (m == null) {
							// should be bug
							return;
						}
						if (m.size() >= 64) {
							String err = "the [" + table + "] index[" + m.size()
									+ "] exceed the max[64], required for [" + sb.toString() + "]";
							GLog.applog.warn(Optimizer.class, "index", err);
//							throw new RuntimeException(err);
						}

						synchronized (m) {
							m.put(id, sb.toString());
						}

						log.warn("create index [" + table + "], e=" + e);
						helper.createIndex(table, e, false);

					} else if (log.isDebugEnabled()) {
						log.debug("optimizer, table=" + table + ", sortkey exists, q=" + q);
					}

				});
			}

		}, false);

	}

	private boolean _exists(String table, String id, String keys) {

		HashMap<String, Object> m = null;

		m = exists.get(table);
		if (m == null) {
			_init(table);
			m = exists.get(table);
		}

		if (m == null) {
			log.warn("got index failed, table=" + table);
			return false;
		}

		if (m.containsKey(id)) {
			return true;
		}

		synchronized (m) {
			// load again, possible optimized by other node
			if (Global.now() - (long) m.get("_inited") > X.AMINUTE) {
				_init(table);
			}

			log.warn("table=" + table + ", m=" + m + ", id=" + id + ", keys=" + keys);

			for (Object o : m.values().toArray()) {

				if (o instanceof String) {
					String s = (String) o;
					if (s.startsWith(keys)) {
						m.put(id, keys);
						return true;
					}
				}
			}
		}

		return false;

	}

	@SuppressWarnings({ "unchecked" })
	private void _init(String table) {

		if (helper == null) {
			log.error("table=" + table, new Exception("helper is null"));
			return;
		}

		List<Map<String, Object>> l1 = helper.getIndexes(table);

		HashMap<String, Object> m = null;
		synchronized (exists) {
			m = exists.get(table);
			if (m == null) {
				m = new HashMap<String, Object>();
				m.put("_inited", Global.now());
				exists.put(table, m);
			}
		}

//		log.info("table=" + table + ", indexes=" + l1);
		if (l1 != null && !l1.isEmpty()) {
			synchronized (m) {
				for (Map<String, Object> d : l1) {
					Map<String, Object> keys = (Map<String, Object>) d.get("key");
					if (keys != null && !keys.isEmpty()) {
						StringBuilder sb = new StringBuilder();
						for (String s : keys.keySet()) {
							if (sb.length() > 0)
								sb.append(",");
							sb.append(s).append(":").append(X.toInt(keys.get(s)));
						}

						if (log.isDebugEnabled()) {
							log.debug("db.index, table=" + table + ", get.index=" + sb.toString());
						}

						String id = UID.id(table, sb.toString());
						m.put(id, sb.toString());
						m.put("_inited", Global.now());
					}
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

			Object[] o = queue.isEmpty() ? null : queue.remove();
			while (o != null) {

				String table = (String) o[0];
				LinkedHashMap<String, Object> keys = (LinkedHashMap<String, Object>) o[1];
				try {

					if (!keys.isEmpty()) {

						if (Global.getInt("db.optimizer", 1) == 1) {

							log.warn("optimizer, table=" + table + ", create.index=" + keys.toString() + ", queue.size="
									+ queue.size());
							helper.createIndex(table, keys, false);
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
