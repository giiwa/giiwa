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

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.dao.Bean;
import org.giiwa.dao.BeanDAO;
import org.giiwa.dao.Column;
import org.giiwa.dao.Table;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.misc.Digest;
import org.giiwa.web.Controller;

/**
 * The App bean, used to store appid and secret table="gi_app"
 * 
 * @author wujun
 *
 */
@Table(name = "gi_app", memo = "GI-应用接入")
public final class App extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(App.class);

	public static final BeanDAO<Long, App> dao = BeanDAO.create(App.class);

	@Column(memo = "唯一序号")
	public long id;

	@Column(memo = "应用标识")
	public String appid;

	@Column(memo = "应用名")
	public String name;

	@Column(memo = "备注")
	private String memo;

	@Column(memo = "密钥")
	public String secret;

	@Column(memo = "允许的IP")
	public String allowip;

	@Column(memo = "IP地址")
	private String ip;

	@Column(memo = "lastime")
	private long lastime;

	@Column(name = "expired")
	private long expired;

	@Column(name = "access")
	private List<String> access;

	public void touch(String ip) {
		update(appid, V.create("lastime", System.currentTimeMillis()).set("ip", ip));
	}

	/**
	 * get the id
	 * 
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * get the memo
	 * 
	 * @return the String
	 */
	public String getMemo() {
		return memo;
	}

	public List<String> getAccess() {
		return access;
	}

	/**
	 * check has the access name
	 * 
	 * @param name the name string...
	 * @return the boolean, true if has this access
	 */
	public boolean hasAccess(String... name) {

		if (X.isEmpty(access)) {
			// access all
			return true;
		}

		for (String s : name) {
			if (access.contains(s))
				return true;
		}
		return false;

	}

	/**
	 * get the appid
	 * 
	 * @return the String
	 */
	public String getAppid() {
		return appid;
	}

	/**
	 * get the secret
	 * 
	 * @return the string of secret
	 */
	public String getSecret() {
		return secret;
	}

	public boolean isAllow(Controller m) {
		if (X.isEmpty(allowip)) {
			return true;
		}

		String ip = m.ip();
		if (ip.matches(allowip)) {
			return true;
		}

		return false;

	}

	/**
	 * get the ip who used the appid
	 * 
	 * @return the IP String
	 */
	public String getIp() {
		return ip;
	}

	/**
	 * the the last access time
	 * 
	 * @return the last time
	 */
	public long getLastime() {
		return lastime;
	}

	/**
	 * get the expire time
	 * 
	 * @return the long of expired
	 */
	public long getExpired() {
		return expired;
	}

	public void addAccess(String... name) {
		if (access == null) {
			access = new ArrayList<String>();
		}
		for (String s : name) {
			if (!access.contains(s)) {
				access.add(s);
			}
		}
		dao.update(id, V.create("access", access));
	}

	/**
	 * data = Base64(AES(params)) <br>
	 * decode, params=AES(Base64(data));
	 * 
	 * @param data   the string of data
	 * @param secret the string of secret
	 * @return JSON
	 */
	public static String decode(String data, String secret) {
		try {
			byte[] bb = Base64.getDecoder().decode(data);
			return new String(Digest.decode(bb, secret));
		} catch (Exception e) {
			log.error("data=" + data + ", secret=" + secret, e);
		}
		return null;
	}

	/**
	 * data = Base64(AES(params)) <br>
	 * decode, params=AES(Base64(data));
	 * 
	 * @param jo     the json data
	 * @param secret the secret
	 * @return the string
	 */
	public static String encode(String data, String secret) {
		try {
			byte[] bb = Digest.encode(data.getBytes(), secret);
			return Base64.getEncoder().encodeToString(bb);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Creates the.
	 *
	 * @param v the v
	 * @return the int
	 */
	public static int create(V v) {
		try {
			long id = UID.next("app.id");
			if (dao.exists(id)) {
				id = UID.next("app.id");
			}
			return dao.insert(v.set(X.ID, id));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return -1;
	}

	/**
	 * Load.
	 *
	 * @param appid the appid
	 * @return the app
	 */
	public static App load(String appid) {
		App a = dao.load(W.create().and("appid", appid));
		if (a != null && (a.expired <= 0 || a.expired > System.currentTimeMillis())) {
			return a;
		}
		return null;
	}

	/**
	 * Update.
	 *
	 * @param appid the appid
	 * @param v     the values
	 * @return the int
	 */
	public static int update(String appid, V v) {
		return dao.update(W.create().and("appid", appid), v);
	}

}
