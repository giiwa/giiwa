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
package org.giiwa.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.logging.*;
import org.giiwa.bean.GLog;
import org.giiwa.conf.Global;
import org.giiwa.dao.Counter;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;

/**
 * The {@code Cache} Class Cache used for cache object, the cache was grouped by
 * cluster <br>
 * configuration in giiwa.properties
 * 
 * <pre>
 * cache.url=memcached://host:port
 * cache.group=demo
 * </pre>
 * 
 * @author joe
 *
 */
public final class Cache {

	/** The log. */
	private static Log log = LogFactory.getLog(Cache.class);

	final static String REDIS = "redis://";
	private static String GROUP = "g://";

	private static ICacheSystem cacheSystem;

	/**
	 * initialize the cache with configuration.
	 *
	 * @param url the configuration that includes cache configure ("cache.url")
	 */
	public static synchronized void init(String url, String user, String pwd) {
		/**
		 * comment it, let's re-conf in running-time
		 */
		log.warn("Cache init ..., url=" + url);

		// if (_conf != null)
		// return;
		String group = Global.getString("site.group", "demo");

		if (url != null && url.startsWith(REDIS)) {
			cacheSystem = RedisCache.create(url, user, pwd);
		}

		if (cacheSystem == null) {
			log.warn("no avaliable cache system, using file cache!");
			cacheSystem = FileCache.create();
		}

		GROUP = group + "://";

		log.warn("Cache inited. group=" + GROUP + ", system=" + cacheSystem);

	}

	/**
	 * Gets the object by id, if the object was expired, null return.
	 * 
	 * @param <T> the class
	 * @param id  the id of object in cache system
	 * @return Object if the object not presented or expired, will return null
	 */
	@SuppressWarnings({ "unchecked" })
	public static <T> T get(String id) {

		TimeStamp t = TimeStamp.create();
		try {

			id = GROUP + id;

			// log.debug("Cache.get, id=" + id + ", cache=" + cacheSystem);

			Object r = null;
			if (cacheSystem != null) {
				r = cacheSystem.get(id);
			}

//			log.debug("get cache name=" + id + ", T=" + r);

			return (T) r;

		} catch (Throwable e) {
			if (cacheSystem != null) {
				cacheSystem.delete(id);
			}

			log.error("nothing get from memcache by " + id + ", remove it!", e);
		} finally {
			read.add(t.pastms(), "id=%s", id);
		}
		return null;
	}

	/**
	 * Removes the cached object by id.
	 *
	 * @param id the object id in cache
	 * @return true, if successful
	 */
	public static boolean remove(String id) {
		TimeStamp t = TimeStamp.create();
		try {
			id = GROUP + id;
			if (cacheSystem != null) {
				return cacheSystem.delete(id);
			}
			return false;
		} finally {
			write.add(t.pastms(), "id=%s", id);
		}
	}

	/**
	 * cache the object with the id, if exists, then update it, otherwise create new
	 * in cache.
	 * 
	 * @Deprecated
	 * @param id   the id of the object
	 * @param data the object
	 * @return true, if successful
	 */
	public static boolean set(String id, Object data) {
		return set(id, data, X.ADAY);
	}

	/**
	 * cache the object with the id, if exists, then update it, otherwise create new
	 * in cache.
	 * 
	 * @param id       the id of the object
	 * @param data     the object
	 * @param expired, the expired time
	 * @return true, if successful
	 */
	public static boolean set(String id, Object data, long expired) {

		TimeStamp t = TimeStamp.create();
		try {
			id = GROUP + id;

			if (cacheSystem != null) {
				if (data == null) {
					return cacheSystem.delete(id);
				} else {
					return cacheSystem.set(id, data, (int) expired);
				}
			}
			return false;
		} finally {
			write.add(t.pastms(), "id=%s", id);
		}
	}

	/**
	 * set the data which exceed 1M
	 * 
	 * @Deprecated
	 * @param id
	 * @param data
	 * @return
	 */
	public static boolean setBigdata(String id, Object data) {

		TimeStamp t = TimeStamp.create();
		ObjectOutputStream out = null;
		ByteArrayInputStream in = null;

		try {
			ByteArrayOutputStream bo = new ByteArrayOutputStream();
			out = new ObjectOutputStream(bo);
			out.writeObject(data);
			out.flush();

			in = new ByteArrayInputStream(bo.toByteArray());

			List<String> keys = new ArrayList<String>();
			byte[] bb = new byte[1000 * 1024];
			int len = in.read(bb);
			int i = 1;
			while (len > 0) {
				String k = id + ":" + (i++);
				if (!set(k, new Object[] { bb, len })) {
					return false;
				}
				keys.add(k);

				len = in.read(bb);
			}
			return set(id, keys);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			X.close(in, out);
			write.add(t.pastms(), "id=%s", id);
		}
		return false;
	}

	/**
	 * get the data which exceed 1M
	 * 
	 * @param id
	 * @return the object
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getBigdata(String id) {
		TimeStamp t = TimeStamp.create();
		try {
			List<String> keys = get(id);
			if (keys != null && keys.size() > 0) {

				ByteArrayOutputStream bo = null;
				ObjectInputStream in = null;

				try {
					bo = new ByteArrayOutputStream();
					for (String k : keys) {
						Object[] oo = get(k);
						if (oo == null) {
							return null;
						}

						byte[] bb = (byte[]) oo[0];
						int len = (int) oo[1];
						bo.write(bb, 0, len);
					}
					bo.flush();

					in = new ObjectInputStream(new ByteArrayInputStream(bo.toByteArray()));
					Object o = in.readObject();
					return (T) o;
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				} finally {
					X.close(in, bo);
				}
			}
			return null;
		} finally {
			read.add(t.pastms(), "id=%s", id);
		}
	}

	/**
	 * trying lock the name with the value, will expired by expire
	 * 
	 * @param name
	 * @param value
	 * @param expire
	 * @return
	 */
	public static boolean trylock(String name) {
		return trylock(name, false);
	}

	public static boolean trylock(String name, boolean debug) {
		TimeStamp t = TimeStamp.create();
		try {
			if (cacheSystem != null) {
				name = GROUP + name;
				return cacheSystem.trylock(name, debug);
			}
			return false;
		} finally {
			read.add(t.pastms(), "name=%s", name);
		}
	}

	/**
	 * set the expire time which locked
	 * 
	 * @param name
	 * @param value
	 * @param ms
	 */
	public static void expire(String name, int ms) {
		TimeStamp t = TimeStamp.create();
		try {
			if (cacheSystem != null) {
				name = GROUP + name;
				cacheSystem.expire(name, ms);
			}
		} finally {
			write.add(t.pastms(), "name=%s", name);
		}
	}

	/**
	 * unlock the name
	 * 
	 * @param name
	 * @param value
	 * @return
	 */
	public static boolean unlock(String name, String value) {
		return unlock(name, value, false);
	}

	public static boolean unlock(String name, String value, boolean debug) {
		TimeStamp t = TimeStamp.create();
		try {
			if (cacheSystem != null) {
				name = GROUP + name;
				return cacheSystem.unlock(name, value, debug);
			}

			log.warn("no cache system!");
			GLog.applog.warn("sys", "unlock", "no cache system!, lock=" + name);
			return false;
		} finally {
			write.add(t.pastms(), "name=%s", name);
		}
	}

	private static Counter read = new Counter("read");
	private static Counter write = new Counter("write");

	public static Counter.Stat statRead() {
		return read.get();
	}

	public static Counter.Stat statWrite() {
		return write.get();
	}

	public static void close() {
		if (cacheSystem != null) {
			cacheSystem.close();
		}
	}

	public static long currentTimeMillis() {
		return now();
	}

	public static long now() {
		if (cacheSystem != null) {
			cacheSystem.now();
		}
		return System.currentTimeMillis();
	}

	public static void touch(String name, long expired) {
		if (cacheSystem != null) {
			cacheSystem.touch(name, expired);
		}
	}

	public static void init(Configuration conf) {
		log.info("Cache init ...");

		try {
			String url = Global.getString("cache.url", null);
			String user = Global.getString("cache.user", null);
			String passwd = Global.getString("cache.passwd", null);

			log.info("Cache init ... url=" + url);
			init(url, user, passwd);
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}

	}

}
