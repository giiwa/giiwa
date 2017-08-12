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
package org.giiwa.framework.bean;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Column;
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.bean.Table;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Config;
import org.giiwa.core.conf.Global;
import org.giiwa.core.task.Task;

import com.mongodb.BasicDBObject;

/**
 * The web access log bean. <br>
 * table="gi_accesslog"
 * 
 * @author joe
 * 
 */
@Table(name = "gi_accesslog")
public class AccessLog extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static AtomicLong seq = new AtomicLong(0);
	static String node = Config.getConf().getString("node.name");

	@Column(name = X.ID, index = true, unique = true)
	private String id;

	@Column(name = "url", index = true)
	private String url;

	@Column(name = "cost", index = true)
	private long cost;

	@Column(name = X.CREATED, index = true)
	private long created;

	public static boolean isOn() {
		return Global.getInt("accesslog.on", 1) == 1;
	}

	/**
	 * Count.
	 *
	 * @param q
	 *            the q
	 * @return the long
	 */
	public static long count(W q) {
		return Helper.count(q, AccessLog.class);
	}

	public String getUrl() {
		return this.getString(X.URL);
	}

	/**
	 * Creates the AccessLog.
	 * 
	 * @param ip
	 *            the ip address
	 * @param url
	 *            the url
	 * @param v
	 *            the values
	 */
	public static void create(final String ip, final String url, final V v) {
		new Task() {

			@Override
			public void onExecute() {
				long created = System.currentTimeMillis();
				String id = UID.id(ip, url, created, node, seq.incrementAndGet());
				Helper.insert(v.set(X.ID, id).set("ip", ip).set(X.URL, url).set(X.CREATED, created), AccessLog.class);
			}

		}.schedule(0);
	}

	/**
	 * Load.
	 *
	 * @param q
	 *            the query and order
	 * @param s
	 *            the start number
	 * @param n
	 *            the number of items
	 * @return the beans
	 */
	public static Beans<AccessLog> load(W q, int s, int n) {
		return Helper.load(q, s, n, AccessLog.class);
	}

	public static AccessLog load(String id) {
		return Helper.load(id, AccessLog.class);
	}

	/**
	 * Cleanup.
	 */
	public static void cleanup() {
		Helper.delete(new BasicDBObject().append(X.CREATED,
				new BasicDBObject().append("$lt", System.currentTimeMillis() - X.AMONTH)), AccessLog.class);
	}

	/**
	 * Delete all.
	 */
	public static void deleteAll() {
		Helper.delete(W.create(), AccessLog.class);
	}

	/**
	 * Distinct.
	 *
	 * @param name
	 *            the name
	 * @return Map
	 */
	public static Map<Object, Long> distinct(String name) {
		List<String> list = Helper.distinct(name, W.create("status", 200), AccessLog.class, String.class);
		Map<Object, Long> m = new TreeMap<Object, Long>();
		for (String v : list) {
			long d = Helper.count(W.create(name, v).and("status", 200), AccessLog.class);
			m.put(v, d);
		}

		return m;
	}

}
