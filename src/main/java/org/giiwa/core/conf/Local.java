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
package org.giiwa.core.conf;

import java.util.HashMap;
import java.util.Map;

import org.giiwa.core.bean.*;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.cache.Cache;
import org.giiwa.core.task.SysTask;
import org.giiwa.framework.bean.Node;

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

	public static final BeanDAO<String, Local> dao = BeanDAO.create(Local.class);

	@Column(name = X.ID)
	String id;

	@Column(name = "s")
	String s;

	@Column(name = "i")
	int i;

	@Column(name = "l")
	long l;

	private static Local owner = new Local();

	public static Local getInstance() {
		return owner;
	}

	/**
	 * get the int value.
	 *
	 * @param name
	 *            the name
	 * @param defaultValue
	 *            the default value
	 * @return the int
	 */
	public static int getInt(String name, int defaultValue) {

		String s = name + "." + Local.id();

		if (!Helper.isConfigured()) {
			return X.toInt(cache.get(s), defaultValue);
		}

		Local c = Cache.get("local/" + s);
		if (c == null || c.expired()) {
			c = dao.load(s);
			if (c != null) {
				/**
				 * avoid restarted, can not load new config
				 */
				c.expired(System.currentTimeMillis() + X.AMINUTE);
				Cache.set("local/" + s, c);
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
	 * @param name
	 *            the name
	 * @param defaultValue
	 *            the default value
	 * @return the string
	 */
	public static String getString(String name, String defaultValue) {

		// log.debug("loading local." + name);

		String s = name + "." + Local.id();

		if (!Helper.isConfigured()) {
			return (String) cache.get(s);
		}

		Local c = Cache.get("local/" + s);
		if (c == null || c.expired()) {
			c = dao.load(s);
			if (c != null) {
				/**
				 * avoid restarted, can not load new config
				 */
				c.expired(System.currentTimeMillis() + X.AMINUTE);
				Cache.set("local/" + s, c);
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
	 * @param name
	 *            the name
	 * @param defaultValue
	 *            the default value
	 * @return the long
	 */
	public static long getLong(String name, long defaultValue) {

		String s = name + "." + Local.id();

		if (!Helper.isConfigured()) {
			return X.toLong(cache.get(s), defaultValue);
		}

		Local c = Cache.get("local/" + s);
		if (c == null || c.expired()) {
			c = dao.load(s);
			if (c != null) {
				/**
				 * avoid restarted, can not load new config
				 */
				c.expired(System.currentTimeMillis() + X.AMINUTE);
				Cache.set("local/" + s, c);

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
	 * @param name
	 *            the name
	 * @param o
	 *            the value
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
	 * @param name
	 *            the name
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
		new SysTask() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			long t = 0;

			@Override
			public void onExecute() {
				if (System.currentTimeMillis() - t > 10 * X.AMINUTE) {
					Node.touch(true);
					t = System.currentTimeMillis();
				} else {
					Node.touch(false);
				}
			}

			@Override
			public String getName() {
				return "local.node.hb";
			}

			@Override
			public void onFinish() {
				this.schedule(1000 * 6);
			}

		}.schedule(1000);
	}

}
