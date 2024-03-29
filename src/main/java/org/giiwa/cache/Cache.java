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

import org.apache.commons.logging.*;
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

	final static String MEMCACHED = "memcached://";
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

		if (url != null && url.startsWith(MEMCACHED)) {
			cacheSystem = MemCache.create(url, user, pwd);
		} else if (url != null && url.startsWith(REDIS)) {
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
			read.add(t.pastms());
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
			write.add(t.pastms());
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
		return set(id, data, (int) X.ADAY);
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
			write.add(t.pastms());
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
			write.add(t.pastms());
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
			read.add(t.pastms());
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
		TimeStamp t = TimeStamp.create();
		try {
			if (cacheSystem != null) {
				name = GROUP + name;
				return cacheSystem.trylock(name);
			}
			return false;
		} finally {
			read.add(t.pastms());
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
			write.add(t.pastms());
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
		TimeStamp t = TimeStamp.create();
		try {
			if (cacheSystem != null) {
				name = GROUP + name;
				return cacheSystem.unlock(name, value);
			}

			log.warn("no cache system!");
			return false;
		} finally {
			write.add(t.pastms());
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

}
