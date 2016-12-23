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

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.*;
import org.giiwa.core.bean.X;

import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

/**
 * The Class RedisCache is used to redis cache <br>
 * url: redis://host:port
 */
class RedisCache implements ICacheSystem {

  /** The log. */
  static Log               log = LogFactory.getLog(RedisCache.class);

  private ShardedJedis     jedis;
  private ShardedJedisPool shardedJedisPool;

  /**
   * Inits the.
   *
   * @param conf
   *          the conf
   * @return the i cache system
   */
  public static ICacheSystem create(Configuration conf) {

    String server = conf.getString("cache.url").substring(Cache.REDIS.length());
    String[] ss = server.split(":");
    String host = ss[0];
    int port = 6379;
    if (ss.length > 1) {
      port = X.toInt(ss[1], 0);
    }
    RedisCache r = new RedisCache();

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
   * @param id
   *          the id
   * @return the object
   */
  public synchronized Object get(String id) {
    byte[] bb = jedis.get(id.getBytes());
    if (bb != null) {
      return unserialize(bb);
    }
    return null;
  }

  /**
   * Sets the.
   *
   * @param id
   *          the id
   * @param o
   *          the o
   * @return true, if successful
   */
  public synchronized boolean set(String id, Object o) {
    try {
      if (o == null) {
        return delete(id);
      } else {
        return jedis.set(id.getBytes(), serialize(o)) != null;
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return false;
  }

  /**
   * Delete.
   *
   * @param id
   *          the id
   * @return true, if successful
   */
  public synchronized boolean delete(String id) {
    return jedis.del(id.getBytes()) > 0;
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
}
