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
package org.giiwa.conf;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Node;
import org.giiwa.cache.TimingCache;
import org.giiwa.dao.*;
import org.giiwa.dao.Helper.V;
import org.giiwa.json.JSON;
import org.giiwa.misc.Shell;
import org.giiwa.net.mq.IStub;
import org.giiwa.net.mq.MQ;
import org.giiwa.net.mq.MQ.Request;
import org.giiwa.task.SysTask;
import org.giiwa.task.Task;

/**
 * The Class Global is extended of Config, it can be "overrided" by module or
 * configured, it stored in database
 * 
 * @author yjiang
 */
@Table(name = "gi_config")
public final class Local extends Bean {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(Local.class);

	public static final BeanDAO<String, Local> dao = BeanDAO.create(Local.class);

	@Column(memo = "主键", size = 100)
	String id;

	@Column(memo = "字符串值", size = 1000)
	String s;

	@Column(memo = "整数值")
	int i;

	@Column(memo = "长整数值")
	long l;

	@Column(memo = "字符串值", size = 512)
	String link;

	private static Local inst = new Local();

	public static Local getInstance() {
		return inst;
	}

	/**
	 * get the int value.
	 *
	 * @param name         the name
	 * @param defaultValue the default value
	 * @return the int
	 */
	public static int getInt(String name, int defaultValue) {

		Local c = TimingCache.get(Local.class, name);
		if (c == null) {

			String s = name + "." + Local.id();
			try {
				c = dao.load(s);
				if (c != null) {
					/**
					 * avoid restarted, can not load new config
					 */
					TimingCache.set(Local.class, name, c);
					return X.toInt(c.i, defaultValue);
				} else {
					c = new Local();
					c.i = Config.getConf().getInt(name, defaultValue);
					TimingCache.set(Local.class, name, c);
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

		return c != null ? X.toInt(c.i, defaultValue) : defaultValue;

	}

	private static int getInt(String node, String name, int defaultValue) {

		String s = name + "." + node;
		Local c = dao.load(s);
		try {
			if (c != null) {
				return X.toInt(c.i, defaultValue);
			} else {
				return Config.getConf().getInt(name, defaultValue);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return c != null ? X.toInt(c.i, defaultValue) : defaultValue;

	}

	public static int getInt(Node e, String name, int defaultValue) {

		String s = name + "." + e.id;
		try {
			Local c = dao.load(s);
			if (c != null) {
				return X.toInt(c.i, defaultValue);
			} else {
				return Config.getConf().getInt(name, defaultValue);
			}
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
		}
		return defaultValue;

	}

	public static long getLong(Node e, String name, long defaultValue) {

		String s = name + "." + e.id;
		try {
			Local c = dao.load(s);
			if (c != null) {
				return X.toLong(c.l, defaultValue);
			} else {
				return Config.getConf().getLong(name, defaultValue);
			}
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
		}
		return defaultValue;

	}

	private static Map<String, Lock> _all = new HashMap<String, Lock>();

	public static Lock getLock(String name) {
		synchronized (_all) {
			Lock e = _all.get(name);
			if (e == null) {
				e = new ReentrantLock();
				_all.put(name, e);
			}
			return e;
		}
	}

	/**
	 * get the string value.
	 *
	 * @param name         the name
	 * @param defaultValue the default value
	 * @return the string
	 */
	public static String getString(String name, String defaultValue) {

		Local c = TimingCache.get(Local.class, name);
		if (c == null) {

			String s = name + "." + Local.id();

			try {
				c = dao.load(s);
				if (c != null) {
					/**
					 * avoid restarted, can not load new config
					 */
					TimingCache.set(Local.class, name, c);
					return c.s != null ? c.s : defaultValue;
				} else {
					c = new Local();
					c.s = Config.getConf().getString(name, defaultValue);
					TimingCache.set(Local.class, name, c);
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

		return c != null ? c.s : defaultValue;

	}

	private static String getString(String node, String name, String defaultValue) {

		String s = name + "." + node;

		Local c = dao.load(s);
		try {
			if (c != null) {
				return c.s != null ? c.s : defaultValue;
			} else {
				return Config.getConf().getString(name, defaultValue);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return c != null ? c.s : defaultValue;

	}

	/**
	 * get the long value.
	 *
	 * @param name         the name
	 * @param defaultValue the default value
	 * @return the long
	 */
	public static long getLong(String name, long defaultValue) {

		Local c = TimingCache.get(Local.class, name);
		if (c == null) {

			String s = name + "." + Local.id();
			try {
				c = dao.load(s);
				if (c != null) {
					/**
					 * avoid restarted, can not load new config
					 */
					TimingCache.set(Local.class, name, c);

					return X.toLong(c.l, defaultValue);
				} else {
					c = new Local();
					c.l = Config.getConf().getInt(name, (int) defaultValue);
					TimingCache.set(Local.class, name, c);
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

		return c != null ? X.toLong(c.l, defaultValue) : defaultValue;

	}

	private static long getLong(String node, String name, long defaultValue) {

		String s = name + "." + node;
		Local c = dao.load(s);
		try {
			if (c != null) {
				return X.toLong(c.l, defaultValue);
			} else {
				return Config.getConf().getLong(name, defaultValue);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return c != null ? X.toLong(c.l, defaultValue) : defaultValue;

	}

	/**
	 * Sets the value of the name in database, it will remove the configuration
	 * value if value is null.
	 *
	 * @param name the name
	 * @param o    the value
	 */
	public synchronized static void setConfig(String name, Object o) {

		if (X.isEmpty(name)) {
			return;
		}

		TimingCache.remove(Local.class, name);

		String s = name + "." + Local.id();

		if (o == null) {
			try {
				dao.delete(s);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			return;
		}

		try {
			Local e = new Local();
			V v = V.create();
			if (o instanceof Integer) {
				e.i = (Integer) o;
				v.append("i", e.i);
			} else if (o instanceof Long) {
				e.l = (Long) o;
				v.append("l", e.l);
			} else {
				e.s = o.toString();
				v.append("s", e.s);
			}

			if (Helper.isConfigured()) {
				if (dao.exists(s)) {
					dao.update(s, v);
				} else {
					dao.insert(v.force(X.ID, s));
				}
			} else {
				TimingCache.set(Local.class, name, e, X.AYEAR);
			}
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
		}
	}

	/**
	 * Gets the string value
	 *
	 * @param name the name
	 * @return the string
	 */
	public String get(String name) {
		return getString(name, null);
	}

	public static String _id;

	/**
	 * get the unique id of this node in the cluster
	 * 
	 * @return
	 */
	public static String id() {
		if (X.isEmpty(_id)) {
			Configuration conf = Config.getConf();
			_id = (conf != null ? conf.getString("node.id", null) : UID.uuid());
			if (X.isEmpty(_id)) {
				// create id
				log.warn("restarting as node.id=null");

				Config.save2();
				Task.schedule(t -> {
					System.exit(0);
				}, 1000);
			}
		}
		return _id;
	}

	public static void init() {
		// start listen
		try {
			new IStub("giiwa.state") {

				@Override
				public void onRequest(long seq, Request req) {

					try {
						JSON j = req.get();

						if (log.isDebugEnabled())
							log.debug("got message, j=" + j + ", local=" + Local.id());

						if (j != null && X.isSame(Local.id(), j.getString("node"))) {
							int power = j.getInt("power");
							synchronized (Task.class) {
								if (power == 1) {
									// restart service
									log.warn("restart by admin [" + req.from + "]");
									Task.schedule(t -> {
										System.exit(0);
									}, 1000);
								} else if (power == 2) {
									// restart
									log.warn("poweroff by admin [" + req.from + "]");
									Task.schedule(t -> {
										try {
											Shell.run("halt", X.AMINUTE);
										} catch (Exception e) {
											log.error(e.getMessage(), e);
										}
									}, 1000);
								}
							}
						}
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}

				}

			}.bindAs(MQ.Mode.TOPIC);

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		// start heartbeat
		new SysTask() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			long t = 0;

			@Override
			public void onExecute() {
				// checking node load
				if ((Global.now() - t) > 10 * X.AMINUTE) {
					Node.touch(true);
					t = Global.now();
				} else {
					Node.touch(false);
				}

			}

			@Override
			public String getName() {
				return "gi.node.hb";
			}

			@Override
			public void onFinish() {
				this.schedule(1000 * 6);
			}

		}.schedule(6000);
	}

	public static String label() {
		Node e = Node.dao.load(Local.id());
		if (e != null && !X.isEmpty(e.label)) {
			return e.label;
		}
		return Local.id();
	}

	public static Node node() {
		return Node.dao.load(Local.id());
	}

	/**
	 * 
	 * load("enabled,0", 1, "host,string,abc","port,int,0", "days,long,0")
	 * 
	 * @param key
	 * @param value
	 * @param name
	 * @return
	 */
	public List<JSON> load(String key, Object value, String... name) {

		Beans<Node> bs = Node.alive();
		List<JSON> l1 = JSON.createList();

		bs.forEach(e -> {
			boolean ok = false;
			if (X.isEmpty(key) || value == null) {
				// load all
				ok = true;
			} else if (value instanceof Integer) {
				String[] kk = X.split(key, ",");
				if (kk.length == 2) {
					int val = X.toInt(value);
					if (Local.getInt(e.id, kk[0], X.toInt(kk[1])) == val) {
						ok = true;
					}
				}
			} else if (value instanceof Long) {
				String[] kk = X.split(key, ",");
				if (kk.length == 2) {
					long val = X.toLong(value);
					if (Local.getLong(e.id, kk[0], X.toLong(kk[1])) == val) {
						ok = true;
					}
				}
			} else {
				String[] kk = X.split(key, ",");
				String val = value.toString();
				if (Local.getString(e.id, kk[0], kk.length > 1 ? kk[1] : null) == val) {
					ok = true;
				}
			}

			if (ok) {
				JSON j1 = JSON.create();
				for (String s : name) {
					String[] ss = X.split(s, ",");
					if (ss.length >= 2) {
						if (X.isIn(ss[1], "int")) {
							j1.put(ss[0], Local.getInt(e.id, ss[0], ss.length > 2 ? X.toInt(ss[2]) : 0));
						} else if (X.isIn(ss[1], "long")) {
							j1.put(ss[0], Local.getLong(e.id, ss[0], ss.length > 2 ? X.toLong(ss[2]) : 0));
						} else {
							j1.put(ss[0], Local.getString(e.id, ss[0], ss.length > 2 ? ss[2].toString() : ""));
						}
					}
				}
				if (!j1.isEmpty()) {
					if (X.isEmpty(e.label)) {
						j1.append("label", e.id);
					} else {
						j1.append("label", e.label);
					}
					j1.append("id", e.id);
					l1.add(j1);
				}
			}
		});

		Collections.sort(l1, new Comparator<JSON>() {

			@Override
			public int compare(JSON o1, JSON o2) {
				String s1 = o1.getString("label");
				String s2 = o2.getString("label");
				return X.compareTo(s1, s2);
			}

		});

		return l1;

	}

}
