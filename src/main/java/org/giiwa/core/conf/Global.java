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
import java.util.concurrent.locks.Lock;

import org.giiwa.core.bean.*;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.task.GlobalLock;

/**
 * The Class Global is extended of Config, it can be "overrided" by module or
 * configured, it stored in database
 * 
 * @author yjiang
 */
@Table(name = "gi_config")
public final class Global extends Bean {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	public static final BeanDAO<String, Global> dao = BeanDAO.create(Global.class);

	@Column(name = X.ID)
	String id;

	@Column(name = "s")
	String s;

	@Column(name = "i")
	int i;

	@Column(name = "l")
	long l;

	@Column(name = "memo")
	String memo;

	private static Global owner = new Global();

	public static Global getInstance() {
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

		Global c = cached.get("global/" + name);
		if (c == null || c.expired()) {
			c = dao.load(name);
			if (c != null) {
				/**
				 * avoid restarted, can not load new config
				 */
				c.expired(System.currentTimeMillis() + X.AMINUTE);
				cached.put("global/" + name, c);
				return X.toInt(c.i, defaultValue);
			}
		}

		return c != null ? X.toInt(c.i, defaultValue) : Config.getConf().getInt(name, defaultValue);

	}

	private static Map<String, Global> cached = new HashMap<String, Global>();

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

		Global c = cached.get("global/" + name);
		if (c == null || c.expired()) {
			c = dao.load(name);
			if (c != null) {
				/**
				 * avoid restarted, can not load new config
				 */
				c.expired(System.currentTimeMillis() + X.AMINUTE);
				cached.put("global/" + name, c);

				// if (!X.isEmpty(c.memo))
				// return c.memo;
				return c.s != null ? c.s : defaultValue;
			}
		}

		// if (!X.isEmpty(c.memo))
		// return c.memo;

		return c != null && c.s != null ? c.s : Config.getConf().getString(name, defaultValue);

	}

	public static String getMemo(String name, String defaultValue) {

		Global c = cached.get("global/" + name);
		if (c == null || c.expired()) {
			c = dao.load(name);
			if (c != null) {
				/**
				 * avoid restarted, can not load new config
				 */
				c.expired(System.currentTimeMillis() + X.AMINUTE);
				cached.put("global/" + name, c);

				// if (!X.isEmpty(c.memo))
				// return c.memo;
				return c.memo != null ? c.memo : defaultValue;
			}
		}

		// if (!X.isEmpty(c.memo))
		// return c.memo;

		return c != null && c.memo != null ? c.memo : Config.getConf().getString(name, defaultValue);

	}

	// private static Map<String, Object> cache = new HashMap<String, Object>();

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

		try {
			Global c = cached.get("global/" + name);
			if (c == null || c.expired()) {
				c = dao.load(name);
				if (c != null) {
					/**
					 * avoid restarted, can not load new config
					 */
					c.expired(System.currentTimeMillis() + X.AMINUTE);
					cached.put("global/" + name, c);

					return X.toLong(c.l, defaultValue);
				}
			}
			return c != null ? X.toLong(c.l, defaultValue) : Config.getConf().getLong(name, defaultValue);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return defaultValue;
	}

	/**
	 * get the current time.
	 *
	 * @return long of current time
	 */
	public static long now() {
		return System.currentTimeMillis();
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

		if (o == null) {
			dao.delete(W.create(X.ID, name));
			return;
		}

		try {
			Global g = new Global();
			V v = V.create();
			if (o instanceof Integer) {
				v.set("i", o);
				g.i = X.toInt(o);
			} else if (o instanceof Long) {
				v.set("l", o);
				g.l = X.toLong(o);
			} else {
				String s = o.toString();
				// if (s.length() > 1000) {
				// v.append("memo", s).append("s", X.EMPTY);
				// g.memo = s;
				// g.s = s;
				// } else {
				v.append("s", s);
				g.s = s;
				// }
			}

			cached.put("global/" + name, g);

			if (Helper.isConfigured()) {
				if (dao.exists(name)) {
					dao.update(name, v);
				} else {
					dao.insert(v.force(X.ID, name));
				}
			}
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
		}
	}

	public synchronized static void setMemo(String name, String s) {
		if (X.isEmpty(name)) {
			return;
		}

		try {
			Global g = new Global();
			V v = V.create();
			v.append("memo", s);
			g.memo = s;

			cached.put("global/" + name, g);

			if (Helper.isConfigured()) {
				if (dao.exists(name)) {
					dao.update(name, v);
				} else {
					dao.insert(v.force(X.ID, name));
				}
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
	 * get a global lock in a cluster environment <br>
	 * the lock will be coordinated in multiple-servers <br>
	 * 
	 * @param name
	 *            the name of lock
	 * @return
	 */
	public static Lock getLock(String name) {
		return GlobalLock.create(name);
	}

	private static String _id = null;

	/**
	 * get the unique id of this cluster in the world
	 * 
	 * @return
	 */
	public static String id() {
		if (X.isEmpty(_id)) {
			_id = Global.getString("global.id", null);
			if (X.isEmpty(_id)) {
				_id = UID.uuid();
				Global.setConfig("global.id", _id);
			}
		}
		return _id;
	}

}
