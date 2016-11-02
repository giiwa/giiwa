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

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.*;
import org.giiwa.core.bean.X;

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
  private static Log          log       = LogFactory.getLog(Cache.class);

  public final static String  MEMCACHED = "memcached://";
  public final static String  REDIS     = "redis://";
  private static String       GROUP     = "g://";

  private static ICacheSystem cacheSystem;

  /**
   * initialize the cache with configuration.
   *
   * @param conf
   *          the configuration that includes cache configure ("cache.url")
   */
  public static synchronized void init(Configuration conf) {
    /**
     * comment it, let's re-conf in running-time
     */
    // if (_conf != null)
    // return;

    String server = conf.getString("cache.url", X.EMPTY);
    if (server.startsWith(MEMCACHED)) {
      cacheSystem = MemCache.create(conf);
    } else if (server.startsWith(REDIS)) {
      cacheSystem = RedisCache.create(conf);
    } else {
      log.debug("not configured cache system, using file cache!");
      cacheSystem = FileCache.create(conf);
    }

    GROUP = conf.getString("cache.group", "demo") + "://";
  }

  /**
   * Gets the object by id, if the object was expired, null return.
   *
   * @param id
   *          the id of object in cache system
   * @return cachable if the object not presented or expired, will return null
   */
  @SuppressWarnings("deprecation")
  public static <T> T get(String id) {

    try {

      id = GROUP + id;

      Cachable r = null;
      if (cacheSystem != null) {
        r = cacheSystem.get(id);
      }
      if (r != null) {
        if (r.expired()) {
          if (cacheSystem != null) {
            cacheSystem.delete(id);
          }

          log.debug("the object was expired, id=" + id + ", age=" + r.age() + "ms");

          return null;
        }
      }

      return (T) r;
    } catch (Throwable e) {
      if (cacheSystem != null) {
        cacheSystem.delete(id);
      }
      log.warn("nothing get from memcache by " + id + ", remove it!");
    }
    return null;
  }

  /**
   * Removes the cached object by id.
   *
   * @param id
   *          the object id in cache
   * @return true, if successful
   */
  public static boolean remove(String id) {
    id = GROUP + id;
    if (cacheSystem != null) {
      return cacheSystem.delete(id);
    }
    return false;
  }

  /**
   * cache the object with the id, if exists, then update it, otherwise create
   * new in cache.
   *
   * @param id
   *          the id of the object
   * @param data
   *          the object
   * @return true, if successful
   */
  public static boolean set(String id, Cachable data) {

    id = GROUP + id;

    if (cacheSystem != null) {
      if (data == null) {
        return cacheSystem.delete(id);
      } else {
        return cacheSystem.set(id, data);
      }
    }
    return false;
  }

}
