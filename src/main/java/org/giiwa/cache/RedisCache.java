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
import java.util.List;

import org.apache.commons.logging.*;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.giiwa.bean.GLog;
import org.giiwa.dao.X;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

/**
 * The Class RedisCache is used to redis cache <br>
 * url: redis://host:port
 */
class RedisCache implements ICacheSystem {

	/** The log. */
	static Log log = LogFactory.getLog(RedisCache.class);

	private JedisPool pool;
	public String url;

	/**
	 * Inits the.
	 *
	 * @param conf the conf
	 * @return the i cache system
	 */
	@SuppressWarnings("deprecation")
	public static ICacheSystem create(String server, String user, String pwd) {

		try {
			RedisCache r = new RedisCache();
			r.url = server.substring(Cache.REDIS.length());

			String[] ss = r.url.split(":");
			String host = ss[0];
			int port = 6379;
			if (ss.length > 1) {
				port = X.toInt(ss[1], 0);
			}

			GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<Jedis>();
			config.setMaxTotal(20);
			config.setMaxIdle(5);
			config.setMaxWaitMillis(1000l);
			config.setTestOnBorrow(false);

			user = X.isEmpty(user) ? null : user;
			pwd = X.isEmpty(pwd) ? null : pwd;

			r.pool = new JedisPool(config, host, port, user, pwd);

			if (r.pool.isClosed()) {
				return null;
			}

			r.set("demo", 1, X.AMINUTE);
			int i = X.toInt(r.get("demo"));
			if (i != 1) {
				return null;
			}

			return r;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * get object.
	 *
	 * @param id the id
	 * @return the object
	 */
	public synchronized Object get(String name) {

		Jedis e = pool.getResource();
		try {
			byte[] key = name.getBytes();
			byte[] bb = e.get(key);
			if (bb != null) {
				return unserialize(bb);
			}
		} finally {
			e.close();
		}
		return null;
	}

	/**
	 * Sets the.
	 *
	 * @param id the id
	 * @param o  the o
	 * @return true, if successful
	 */
	public synchronized boolean set(String name, Object o, long expired) {
		if (o == null) {
			return delete(name);
		} else {
			Jedis e = pool.getResource();
			try {
				return e.psetex(name.getBytes(), expired, serialize(o)) != null;
			} catch (Exception e1) {
				log.error(e1.getMessage(), e1);
			} finally {
				e.close();
			}
		}
		return false;
	}

	/**
	 * Delete.
	 *
	 * @param id the id
	 * @return true, if successful
	 */
	public synchronized boolean delete(String name) {

		Jedis e = pool.getResource();
		try {
			return e.del(name) > 0;
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
		} finally {
			e.close();
		}
		return false;
	}

	private static byte[] serialize(Object object) {
		ObjectOutputStream oos = null;
		ByteArrayOutputStream baos = null;
		try {
			baos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(baos);
			oos.writeObject(object);
			byte[] bytes = baos.toByteArray();
			return bytes;
		} catch (Exception e) {

		}
		return null;
	}

	private static Object unserialize(byte[] bytes) {
		ByteArrayInputStream bais = null;
		try {
			bais = new ByteArrayInputStream(bytes);
			ObjectInputStream ois = new ObjectInputStream(bais);
			return ois.readObject();
		} catch (Exception e) {

		}
		return null;
	}

	@Override
	public String toString() {
		return "RedisCache [url=" + url + "]";
	}

	public synchronized boolean trylock(String name, boolean debug) {

		Jedis e = pool.getResource();
		try {
			SetParams p = new SetParams();
			p.ex(12L);
			p.nx();

			String n = e.set(name, "1", p);
			if (debug || log.isDebugEnabled()) {
				log.info("lock status=" + n + ", lock=" + name);
			}
			return X.isSame("OK", n);
		} finally {
			e.close();
		}
	}

	public synchronized void expire(String name, long ms) {
		Jedis e = pool.getResource();
		try {
			e.pexpire(name, ms);
		} finally {
			e.close();
		}
	}

	@Override
	public synchronized boolean unlock(String name, String value, boolean debug) {

		Jedis e = pool.getResource();
		try {
			long n = e.del(name);
			if (debug || log.isDebugEnabled()) {
				log.info("unlock status=" + n + ", lock=" + name);
			}
			return true;
		} catch (Throwable e1) {
			log.error("unlock failed, lock=" + name, e1);
			GLog.applog.error("sys", "unlock", "failed, lock=" + name, e1);
		} finally {
			e.close();
		}
		return true;
	}

	@Override
	public void close() {

	}

	@Override
	public long now() {
		Jedis e = pool.getResource();
		try {
			List<String> l1 = e.time();
			return X.toLong(l1.get(0)) + X.toLong(l1.get(1)) / 1000;
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
		} finally {
			e.close();
		}

		return System.currentTimeMillis();
	}

	@Override
	public void touch(String name, long expired) {
		Jedis e = pool.getResource();
		try {
			e.touch(name.getBytes());
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
		} finally {
			e.close();
		}
	}

}
