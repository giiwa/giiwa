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

import org.apache.commons.logging.*;
import org.giiwa.misc.Url;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClient;

/**
 * The Class MemCache is used to memcached cache <br>
 * url: memcached://host:port
 */
class MemCache implements ICacheSystem {

	/** The log. */
	static Log log = LogFactory.getLog(MemCache.class);

	private MemcachedClient memCachedClient;

	private Url url;

	/**
	 * Inits the.
	 *
	 * @param conf the conf
	 * @return the i cache system
	 */
	public static ICacheSystem create(String server, String user, String pwd) {

		MemCache f = new MemCache();

		f.url = Url.create(server);
		try {
			f.memCachedClient = new XMemcachedClient(f.url.getIp(), f.url.getPort(11211));
			if (f.memCachedClient.getConnector() != null && f.memCachedClient.getConnector().isStarted()) {
				return f;
			}
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
		try {
			return memCachedClient.get(name);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Sets the data linked the id, the maximum size of data 1M
	 *
	 * @param id the id
	 * @param o  the o
	 * @return true, if successful
	 */
	public synchronized boolean set(String name, Object o, long expired) {
		try {
			if (o == null) {
				return delete(name);
			} else {
//				return memCachedClient.set(name, o, new Date(System.currentTimeMillis() + expired));
				return memCachedClient.set(name, (int) (expired / 1000), o);
//				return memCachedClient.set(name, o);
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
			return memCachedClient.delete(name);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return false;
	}

	@Override
	public String toString() {
		return "MemCache [url=" + url + "]";
	}

	public boolean trylock(String name) {
		try {
			return memCachedClient.add(name, 12, 1);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return false;
	}

	public void expire(String name, long ms) {
		try {
			memCachedClient.set(name, (int) (ms / 1000), 1);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
//		log.debug("memcached expire, name=" + name + ", value=" + memCachedClient.get(name));
	}

	@Override
	public boolean unlock(String name, String value) {
		try {
			memCachedClient.delete(name);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return true;
	}

}
