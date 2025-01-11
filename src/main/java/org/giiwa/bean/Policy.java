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
package org.giiwa.bean;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.cache.TimingCache;
import org.giiwa.dao.Bean;
import org.giiwa.dao.BeanDAO;
import org.giiwa.dao.Beans;
import org.giiwa.dao.Column;
import org.giiwa.dao.Helper;
import org.giiwa.dao.Table;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.web.Controller;
import org.giiwa.web.Controller.NameValue;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;

/**
 * The App bean, used to store appid and secret table="gi_app"
 * 
 * @author wujun
 *
 */
@Table(name = "gi_policy", memo = "GI-安全策略")
public final class Policy extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(Policy.class);

	public static final BeanDAO<Long, Policy> dao = BeanDAO.create(Policy.class);

	@Column(memo = "主键", unique = true)
	public long id;

	@Column(memo = "客户端标识", value = "正则表达式, ip:/10.*, head:/...")
	public String client;

	private transient String ip;
	private transient String head;

	@Column(memo = "访问链接", value = "正则表达式, /images/.*")
	public String url;

	@Column(memo = "序号")
	public int seq;

	@Column(memo = "开启", value = "1:yes")
	public int enabled;

	@Column(memo = "行动", value = "0:允许, 1:禁止，2:延时输出, 3:混淆输出,")
	public int action;

	@Column(name = "_percent", memo = "混淆比例")
	public int percent;

	@Column(memo = "延时区间(秒)")
	public int min_delay;

	@Column(memo = "延时区间(秒)")
	public int max_delay;

	@Column(memo = "备注", size = 100)
	public String memo;

	@Column(memo = "拦截次数")
	public long times;

	public static Policy matches(Controller m, String url) {
		try {
			if (Helper.isConfigured()) {
				String id = "gi/policy";
				Beans<Policy> l1 = TimingCache.get(Beans.class, id);
				if (l1 == null) {
					l1 = dao.load(W.create().and("enabled", 1).sort("seq"), 0, 128);
					if (l1 != null) {
						TimingCache.set(Beans.class, id, l1);
					}
				}

				if (l1 != null && !l1.isEmpty()) {
					String ip = null;
					String head = null;
					for (Policy e : l1) {
						if (e.client.startsWith("ip:/")) {
							if (e.ip == null) {
								e.ip = e.client.substring(4);
							}
							if (ip == null) {
								ip = m.ip();
							}
							if (ip != null && ip.matches(e.ip) && url.matches(e.url)) {
								dao.inc(W.create().and(X.ID, e.id), "times", 1, null);
								log.info("matches, ip=" + ip + ", url=" + url + ", e.ip=" + e.ip + ", e.url=" + e.url);
								return e.copy(ip, url, ip);
							}
						} else if (e.client.startsWith("head:/")) {
							if (e.head == null) {
								e.head = e.client.substring(6);
							}
							if (head == null) {
								head = X.join(X.asList(m.heads(), o -> {
									NameValue e1 = (NameValue) o;
									return e1.name + "=" + e1.value;
								}), ",");
							}
							if (head != null && head.matches(e.head) && url.matches(e.url)) {
								dao.inc(W.create().and(X.ID, e.id), "times", 1, null);
								log.info("matches, head=" + head + ", url=" + url + ", e.head=[" + e.head + "], e.url="
										+ e.url);
								return e.copy(head, url, ip);
							}
						}
					}
				}
			}
		} catch (Throwable e) {
			// avoid config error cause can not enter system
			log.error(e.getMessage(), e);
		}

		return null;
	}

	private Policy copy(String client, String url, String ip) {
		Policy e = new Policy();
		e.id = id;
		e.action = action;
		e.percent = percent;
		e.min_delay = min_delay;
		e.max_delay = max_delay;
		e.client = client;
		e.url = url;
		e.ip = ip;
		return e;
	}

	/**
	 * Creates the.
	 *
	 * @param v the v
	 * @return the int
	 */
	public static long create(V v) {
		try {
			long id = dao.next();
			while (dao.exists(id)) {
				id = dao.next();
			}
			dao.insert(v.append(X.ID, id));
			return id;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return -1;
	}

	public boolean allow(boolean firstime) {
		if (action == 0) {
			// allow
			return true;
		} else if (action == 2) {
			// delay output
			if (firstime) {
				try {
					Integer delay = X.toInt(min_delay + Math.random() * (max_delay - min_delay));
					GLog.securitylog.warn(Policy.class, "delay",
							"delay=" + delay + ", client=" + client + ", url=" + url, null, ip);
					log.warn("blocked, delay output=" + delay + "s");
					synchronized (delay) {
						delay.wait(delay * 1000);
					}
				} catch (Exception err) {
					log.error(err.getMessage(), err);
				}
			}
			return true;
		} else if (action == 3) {
			// mix output
			return true;
		} else {
			// action = 1;
			// forbidden
			GLog.securitylog.warn(Policy.class, "block", "client=" + client + ", url=" + url, null, ip);
			log.warn("block, deny, [" + client + ", " + url + "]");
			return false;
		}
	}

	public JSON mix(JSON data) {
		if (action != 3 || data == null || data.isEmpty()) {
			return data;
		}

		log.warn("mix, rule=[" + client + ", " + url + "]");
		GLog.securitylog.warn(Policy.class, "mix", "client=" + client + ", url=" + url, null, ip);

		data.scan((j1, name) -> {
			Object v = j1.get(name);
			if (v == null) {
				return;
			}

			boolean d = (Math.random() * 100) > percent ? true : false;

			if (d) {
				return;
			}

			if (v instanceof String) {
				StringBuilder s = new StringBuilder((String) v);
				int len = s.length();
				if (len > 2) {
					int n = len > 10 ? Math.min(10, len / 10) : 1;
					for (int x = 0; x < n; x++) {
						int i = (int) (Math.random() * len);
						int j = (int) (Math.random() * len);
						if (i != j) {
							char c = s.charAt(i);
							s.setCharAt(i, s.charAt(j));
							s.setCharAt(j, c);
						}
					}
					v = s.toString();
					j1.put(name, v);
				}
			} else if (v instanceof Double) {
				v = Math.random() * X.toDouble(v);
				j1.put(name, v);
			} else if (v instanceof Float) {
				v = X.toFloat(Math.random() * X.toFloat(v));
				j1.put(name, v);
			} else if (v instanceof Integer) {
				v = X.toInt(Math.random() * X.toInt(v));
				j1.put(name, v);
			} else if (v instanceof Long) {
				v = X.toLong(Math.random() * X.toLong(v));
				j1.put(name, v);
			}
		});

		return data;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Map<String, Object> mix(Map<String, Object> data) {

		if (action != 3 || data == null || data.isEmpty()) {
			return data;
		}

		GLog.securitylog.warn(Policy.class, "mix", "client=" + client + ", url=" + url, null, ip);
		log.warn("mix, rule=[" + client + ", " + url + "]");

		for (String name : data.keySet()) {

			boolean d = (Math.random() * 100) > percent ? true : false;
			Object v = data.get(name);
			if (v instanceof String) {
				if (d) {
					continue;
				}
				StringBuilder s = new StringBuilder((String) v);
				int len = s.length();
				if (len > 2) {
					int n = len > 10 ? Math.min(10, len / 10) : 1;
					for (int x = 0; x < n; x++) {
						int i = (int) (Math.random() * len);
						int j = (int) (Math.random() * len);
						if (i != j) {
							char c = s.charAt(i);
							s.setCharAt(i, s.charAt(j));
							s.setCharAt(j, c);
						}
					}
					v = s.toString();
					data.put(name, v);
				}
			} else if (v instanceof Double) {
				if (d) {
					continue;
				}
				StringBuilder s = new StringBuilder(v.toString());
				int len = s.length();
				if (len > 2) {
					int i = (int) (Math.random() * len);
					int j = (int) (Math.random() * len);
					if (i != j) {
						char c = s.charAt(i);
						s.setCharAt(i, s.charAt(j));
						s.setCharAt(j, c);
					}
					v = X.toDouble(s.toString());
					data.put(name, v);
				}
			} else if (v instanceof Float) {
				if (d) {
					continue;
				}
				StringBuilder s = new StringBuilder(v.toString());
				int len = s.length();
				if (len > 2) {
					int i = (int) (Math.random() * len);
					int j = (int) (Math.random() * len);
					if (i != j) {
						char c = s.charAt(i);
						s.setCharAt(i, s.charAt(j));
						s.setCharAt(j, c);
					}
					v = X.toFloat(s.toString());
					data.put(name, v);
				}
			} else if (v instanceof Integer) {
				if (d) {
					continue;
				}
				StringBuilder s = new StringBuilder(v.toString());
				int len = s.length();
				if (len > 2) {
					int i = (int) (Math.random() * len);
					int j = (int) (Math.random() * len);
					if (i != j) {
						char c = s.charAt(i);
						s.setCharAt(i, s.charAt(j));
						s.setCharAt(j, c);
					}
					v = X.toInt(s.toString());
					data.put(name, v);
				}
			} else if (v instanceof Long) {
				if (d) {
					continue;
				}
				StringBuilder s = new StringBuilder(v.toString());
				int len = s.length();
				if (len > 2) {
					int i = (int) (Math.random() * len);
					int j = (int) (Math.random() * len);
					if (i != j) {
						char c = s.charAt(i);
						s.setCharAt(i, s.charAt(j));
						s.setCharAt(j, c);
					}
					v = X.toLong(s.toString());
					data.put(name, v);
				}
			} else if (v instanceof Collection) {
				if (d) {
					continue;
				}
				Collection c = (Collection) v;
				Iterator it = c.iterator();
				for (int i = 0; i < (int) (Math.random() * c.size()); i++) {
					it.next();
				}
				Object o = it.next();
				c.remove(o);
				v = c;
				data.put(name, v);
			} else if (v instanceof Bean) {
				v = mix((Bean) v);
				data.put(name, v);
			} else if (v instanceof Map) {
				v = mix((Map) v);
				data.put(name, v);
			}
		}
		return data;
	}

}
