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

import java.util.concurrent.locks.Lock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.cache.Cache;
import org.giiwa.cache.GlobalLock;
import org.giiwa.cache.TimingCache;
import org.giiwa.dao.Bean;
import org.giiwa.dao.BeanDAO;
import org.giiwa.dao.Column;
import org.giiwa.dao.Comment;
import org.giiwa.dao.Helper;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.dao.Table;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;

/**
 * The Class Global is extended of Config, it can be "overrided" by module or
 * configured, it stored in database
 * 
 * @author yjiang
 */
@Comment(text = "全局配置")
@Table(name = "gi_config", memo = "GI-全局配置")
public final class Global extends Bean {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(Global.class);

	public static final BeanDAO<String, Global> dao = BeanDAO.create(Global.class);

	@Column(memo = "主键", size = 100)
	public String id;

	@Column(memo = "字符串值", size = 1000)
	String s;

	@Column(memo = "整数值")
	int i;

	@Column(memo = "长整数值")
	long l;

	@Column(memo = "字符串值", size = 512)
	String link;

	private static Global inst = new Global();

	public static Global getInstance() {
		return inst;
	}

	/**
	 * get the int value.
	 *
	 * @param name         the name
	 * @param defaultValue the default value
	 * @return the int
	 */
	@Comment()
	public static int getInt(@Comment(text = "name") String name, @Comment(text = "defaultvalue") int defaultValue) {

		Global c = TimingCache.get(Global.class, name);
		if (c == null) {
			try {
				c = dao.load(name);
				if (c != null) {
					/**
					 * avoid restarted, can not load new config
					 */
					TimingCache.set(Global.class, name, c);
					return X.toInt(c.i, defaultValue);
				} else {
					c = new Global();
					c.i = Config.getConf().getInt(name, defaultValue);
					TimingCache.set(Global.class, name, c);
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

		return c != null ? X.toInt(c.i, defaultValue) : Config.getConf().getInt(name, defaultValue);

	}

	/**
	 * get the string value.
	 *
	 * @param name         the name
	 * @param defaultValue the default value
	 * @return the string
	 */
	public static String getString(String name, String defaultValue) {

		Global c = TimingCache.get(Global.class, name);

		if (c == null && Helper.isConfigured()) {
			try {
				c = dao.load(name);
				if (c != null) {
					/**
					 * avoid restarted, can not load new config
					 */
					TimingCache.set(Global.class, name, c);

					return c.s != null ? c.s : defaultValue;
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

		if (c != null && c.s != null) {
			return c.s;
		}
		if (Config.getConf() != null) {
			return Config.getConf().getString(name, defaultValue);
		}

		return defaultValue;

	}

//	public static String getMemo(String name, String defaultValue) {
//
//		Global c = TimingCache.get(Global.class, name);
//		if (c == null) {
//
//			try {
//				c = dao.load(name);
//				if (c != null) {
//					/**
//					 * avoid restarted, can not load new config
//					 */
//					TimingCache.set(Global.class, name, c);
//
//					return c.memo != null ? c.memo : defaultValue;
//				}
//			} catch (Exception e) {
//				log.error(e.getMessage(), e);
//			}
//		}
//
//		return c != null && c.memo != null ? c.memo : Config.getConf().getString(name, defaultValue);
//
//	}

	/**
	 * get the long value.
	 *
	 * @param name         the name
	 * @param defaultValue the default value
	 * @return the long
	 */
	@Comment()
	public static long getLong(@Comment(text = "name") String name, @Comment(text = "defaultvalue") long defaultValue) {

		Global c = TimingCache.get(Global.class, name);
		if (c == null) {
			try {
				c = dao.load(name);
				if (c != null) {
					/**
					 * avoid restarted, can not load new config
					 */
					TimingCache.set(Global.class, name, c);

					return X.toLong(c.l, defaultValue);
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return c != null ? X.toLong(c.l, defaultValue) : Config.getConf().getLong(name, defaultValue);
	}

	/**
	 * get the current time.
	 *
	 * @return long of current time
	 */
	public static long now() {
		return Cache.currentTimeMillis();
//		long t = System.currentTimeMillis();
//		Long c = Cache.get("now");
//		if (c == null || c < t) {
//			if (NtpTask.inst.ok) {
//				Cache.set("now", t, X.AHOUR);
//			}
//		} else {
//			t = c;
//		}
//		return t;
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

		TimingCache.remove(Global.class, name);

		if (o == null) {
			try {
				dao.delete(W.create().and(X.ID, name));
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			return;
		}

		try {
			Global g = new Global();
			V v = V.create();
			if (o instanceof Integer) {
				g.i = (Integer) o;
				v.append("i", g.i);
			} else if (o instanceof Long) {
				g.l = (Long) o;
				v.append("l", g.l);
			} else {
				String s = o.toString();
				v.append("s", s);
				g.s = s;
			}

			if (Helper.isConfigured()) {
				if (dao.exists(name)) {
					dao.update(name, v);
				} else {
					dao.insert(v.force(X.ID, name));
				}
			} else {
				TimingCache.set(Global.class, name, g, X.AYEAR);
			}
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
		}
	}

//	public synchronized static void setMemo(String name, String s) {
//		if (X.isEmpty(name)) {
//			return;
//		}
//
//		try {
//			Global g = new Global();
//			V v = V.create();
//			v.append("memo", s);
//			g.memo = s;
//
//			TimingCache.set(Global.class, name, g);
//
//			if (Helper.isConfigured()) {
//				if (dao.exists(name)) {
//					dao.update(name, v);
//				} else {
//					dao.insert(v.force(X.ID, name));
//				}
//			}
//		} catch (Exception e1) {
//			log.error(e1.getMessage(), e1);
//		}
//	}

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
	 * get a global lock in a cluster environment <br>
	 * the lock will be coordinated in multiple-servers <br>
	 * 
	 * @param name the name of lock
	 * @return
	 */
	public static Lock getLock(String name) {
		return getLock(name, false);
	}

	public static Lock getLock(String name, boolean debug) {
//		if (ZkLock.isOk()) {
//			return ZkLock.create(name, debug);
//		}
		return GlobalLock.create(name, debug);

	}

	private static String _id = null;

	/**
	 * get the unique id of this cluster in the world
	 * 
	 * @return
	 */
	public static String id() {
		if (X.isEmpty(_id) && Helper.isConfigured()) {
			_id = Global.getString("global.id", null);
			if (X.isEmpty(_id)) {
				_id = UID.uuid();
				Global.setConfig("global.id", _id);
			}
		}
		return _id;
	}

}
