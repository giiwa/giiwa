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
package org.giiwa.core.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.*;
import org.giiwa.core.bean.X;

import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.params.SetParams;

/**
 * The Class RedisCache is used to redis cache <br>
 * url: redis://host:port
 */
class RedisCache implements ICacheSystem {

	/** The log. */
	static Log log = LogFactory.getLog(RedisCache.class);

	public ShardedJedis jedis;
	public ShardedJedisPool shardedJedisPool;
	public String url;

	/**
	 * Inits the.
	 *
	 * @param conf the conf
	 * @return the i cache system
	 */
	public static ICacheSystem create(String server) {

		RedisCache r = new RedisCache();
		r.url = server.substring(Cache.REDIS.length());

		String[] ss = r.url.split(":");
		String host = ss[0];
		int port = 6379;
		if (ss.length > 1) {
			port = X.toInt(ss[1], 0);
		}

		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxTotal(20);
		config.setMaxIdle(5);
		config.setMaxWaitMillis(1000l);
		config.setTestOnBorrow(false);

		List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
		shards.add(new JedisShardInfo(host, port, "master"));

		r.shardedJedisPool = new ShardedJedisPool(config, shards);
		r.jedis = r.shardedJedisPool.getResource();

		return r;
	}

	/**
	 * get object.
	 *
	 * @param id the id
	 * @return the object
	 */
	public synchronized Object get(String name) {
		byte[] bb = jedis.get(name.getBytes());
		if (bb != null) {
			return unserialize(bb);
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
	public synchronized boolean set(String name, Object o) {
		try {
			if (o == null) {
				return delete(name);
			} else {
				return jedis.set(name.getBytes(), serialize(o)) != null;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
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
		try {
			return jedis.del(name) > 0;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
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

	public synchronized boolean trylock(String name, String value, long ms) {

		SetParams p = new SetParams();
		p.ex(12);
		p.nx();

		String n = jedis.set(name, value, p);
		return X.isSame("OK", n);
		
	}

	public synchronized void expire(String name, String value, long ms) {
		int sec = (int) (ms / 1000);
		jedis.expire(name, sec);
	}

	@Override
	public synchronized boolean unlock(String name, String value) {
		jedis.del(name);
		return true;
	}

	// public static void main(String[] args) {
	// Task.init(10);
	//
	// try {
	// ICacheSystem r = RedisCache.create("redis://s01:6379");
	// System.out.println(r.trylock("a", 12000));
	// Thread.sleep(10000);
	// System.out.println(r.delete("a"));
	// System.out.println(r.trylock("a", 12000));
	// } catch (Exception e) {
	// log.error(e.getMessage(), e);
	// }
	//
	// }

}
