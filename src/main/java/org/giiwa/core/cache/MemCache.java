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

import org.apache.commons.logging.*;

import com.whalin.MemCached.MemCachedClient;
import com.whalin.MemCached.SockIOPool;

/**
 * The Class MemCache is used to memcached cache <br>
 * url: memcached://host:port
 */
class MemCache implements ICacheSystem {

  /** The log. */
  static Log              log = LogFactory.getLog(MemCache.class);

  private MemCachedClient memCachedClient;

  private String          url;

  /**
   * Inits the.
   *
   * @param conf
   *          the conf
   * @return the i cache system
   */
  public static ICacheSystem create(String server) {

    MemCache f = new MemCache();

    f.url = server.substring(Cache.MEMCACHED.length());

    SockIOPool pool = SockIOPool.getInstance();
    pool.setServers(new String[] { f.url });
    pool.setFailover(true);
    pool.setInitConn(10);
    pool.setMinConn(5);
    pool.setMaxConn(1000);
    pool.setMaintSleep(30);
    pool.setNagle(false);
    pool.setSocketTO(3000);
    pool.setAliveCheck(true);
    pool.initialize();

    f.memCachedClient = new MemCachedClient();

    return f;
  }

  /**
   * get object.
   *
   * @param id
   *          the id
   * @return the object
   */
  public synchronized Object get(String id) {
    return memCachedClient.get(id);
  }

  /**
   * Sets the data linked the id, the maximum size of data 1M
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
        return memCachedClient.set(id, o);
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
    return memCachedClient.delete(id);
  }

  @Override
  public String toString() {
    return "MemCache [url=" + url + "]";
  }

}
