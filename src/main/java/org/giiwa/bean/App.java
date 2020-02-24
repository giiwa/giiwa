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
import org.giiwa.json.JSON;
import org.giiwa.misc.Digest;

/**
 * The App bean, used to store appid and secret table="gi_app"
 * 
 * @author wujun
 *
 */
@Table(name = "gi_app")
public class App extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(App.class);

	public static final BeanDAO<Long, App> dao = BeanDAO.create(App.class);

	@Column(name = X.ID, index = true, unique = true)
	private long id;

	@Column(name = "appid", index = true, unique = true)
	private String appid;

	@Column(name = "memo")
	private String memo;

	@Column(name = "secret")
	private String secret;

	@Column(name = "ip")
	private String ip;

	@Column(name = "lastime")
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

			return new String(Digest.aes_decrypt(bb, secret));
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
			byte[] bb = Digest.aes_encrypt(data.getBytes(), secret);
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
		App a = dao.load(W.create("appid", appid));
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
		return dao.update(W.create("appid", appid), v);
	}

	public static void main(String[] args) {
		App a = new App();
		a.appid = "1";
		a.secret = "123123";

		JSON j1 = JSON.create();
		j1.put("name", "1");
		j1.put("key", "122");

		try {
			String data = App.encode(j1.toPrettyString(), a.secret);
			System.out.println("data=" + data);
			String jo = App.decode(data, a.secret);
			System.out.println("jo=" + jo);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
