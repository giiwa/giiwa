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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.internal.Base64;
import org.giiwa.bean.Node;
import org.giiwa.cache.Cache;
import org.giiwa.dao.*;
import org.giiwa.dao.Helper.V;
import org.giiwa.json.JSON;
import org.giiwa.misc.Digest;
import org.giiwa.misc.Host;
import org.giiwa.misc.MD5;
import org.giiwa.net.mq.IStub;
import org.giiwa.net.mq.MQ;
import org.giiwa.net.mq.MQ.Request;
import org.giiwa.task.SysTask;
import org.giiwa.task.Task;
import org.giiwa.web.Module;

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

	@Column(memo = "唯一序号")
	String id;

	@Column(memo = "字符串值")
	String s;

	@Column(memo = "整数值")
	int i;

	@Column(memo = "长整数值")
	long l;

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

		String s = name + "." + Local.id();

		if (!Helper.isConfigured()) {
			return X.toInt(cache.get(s), defaultValue);
		}

		Local c = Cache.get("local/" + s);
		if (c == null) {
			c = dao.load(s);
			if (c != null) {
				/**
				 * avoid restarted, can not load new config
				 */
				Cache.set("local/" + s, c, X.AMINUTE);
				return X.toInt(c.i, defaultValue);
			} else {
				return Config.getConf().getInt(name, defaultValue);
			}
		}

		return c != null ? X.toInt(c.i, defaultValue) : defaultValue;

	}

	/**
	 * get the string value.
	 *
	 * @param name         the name
	 * @param defaultValue the default value
	 * @return the string
	 */
	public static String getString(String name, String defaultValue) {

		// log.debug("loading local." + name);

		String s = name + "." + Local.id();

		if (!Helper.isConfigured()) {
			return (String) cache.get(s);
		}

		Local c = Cache.get("local/" + s);
		if (c == null) {
			c = dao.load(s);
			if (c != null) {
				/**
				 * avoid restarted, can not load new config
				 */
				Cache.set("local/" + s, c, X.AMINUTE);
				return c.s;
			} else {
				return Config.getConf().getString(name, defaultValue);
			}
		}

		return c != null ? c.s : defaultValue;

	}

	private static Map<String, Object> cache = new HashMap<String, Object>();

	/**
	 * get the long value.
	 *
	 * @param name         the name
	 * @param defaultValue the default value
	 * @return the long
	 */
	public static long getLong(String name, long defaultValue) {

		String s = name + "." + Local.id();

		if (!Helper.isConfigured()) {
			return X.toLong(cache.get(s), defaultValue);
		}

		Local c = Cache.get("local/" + s);
		if (c == null) {
			c = dao.load(s);
			if (c != null) {
				/**
				 * avoid restarted, can not load new config
				 */
				Cache.set("local/" + s, c, X.AMINUTE);

				return X.toLong(c.l, defaultValue);
			} else {
				return Config.getConf().getLong(name, defaultValue);
			}
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

		String s = name + "." + Local.id();
		Cache.remove("local/" + s);

		if (o == null) {
			dao.delete(s);
			return;
		}

		if (!Helper.isConfigured()) {
			cache.put(s, o);
			return;
		}

		try {
			V v = V.create();
			if (o instanceof Integer) {
				v.set("i", o);
			} else if (o instanceof Long) {
				v.set("l", o);
			} else {
				v.set("s", o.toString());
			}

			if (dao.exists(s)) {
				dao.update(s, v);
			} else {
				dao.insert(v.force(X.ID, s));
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

	/**
	 * get the unique id of this node in the cluster
	 * 
	 * @return
	 */
	public static String id() {
		return Config.id();
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
								if (Task.powerstate != power) {
									Task.powerstate = power;
									if (Task.powerstate == 1) {
										// start
										Module.startAll();
									} else {
										// stop
										Module.stopAll();
									}
								}
							}
						}
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}

				}

			}.bind(MQ.Mode.TOPIC);

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
				if (System.currentTimeMillis() - t > 10 * X.AMINUTE) {
					Node.touch(true);
					t = System.currentTimeMillis();
				} else {
					Node.touch(false);
				}

				// check node disk
//				W q = W.create().and("node", Local.id()).sort("created", 1);
//				Beans<Disk> l1 = Disk.dao.load(q, 0, 10);
//				if (l1 != null && !l1.isEmpty()) {
//					for (Disk d : l1) {
//
//						if (d.isLocal()) {
//							Disk.dao.update(d.getId(),
//									V.create("bad", 0).append("lasttime", System.currentTimeMillis()));
//
//							if (System.currentTimeMillis() - d.getLong("checktime") > X.AMINUTE) {
//								d.check();
//							}
//						}
//
//					}
//				}

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

	public static boolean unlimited(String name) {

		try {
			String s = getString(name, null);
			if (X.isEmpty(s)) {
				return false;
			}

			String machineid = Digest.md5(Host.getLocalip() + "/" + Local.id());

			String ip = machineid + "/" + name;
			String s1 = MD5.sha1(Base64.encode(Digest.aes_encrypt(ip.getBytes(), "giisoo")));
			if (X.isSame(s, s1))
				return true;

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return false;

	}

}
