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

import java.util.Base64;

import org.giiwa.core.base.Digest;
import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.BeanDAO;
import org.giiwa.core.bean.Column;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.bean.Table;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;
import org.giiwa.core.cache.Cache;
import org.giiwa.core.json.JSON;

// TODO: Auto-generated Javadoc
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

	public static final BeanDAO<App> dao = BeanDAO.create(App.class);

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

	@Column(name = "role")
	private long role;

	public void touch(String ip) {
		update(appid, V.create("lastime", System.currentTimeMillis()).set("ip", ip));
	}

	public long getId() {
		return id;
	}

	public String getMemo() {
		return memo;
	}

	public boolean hasAccess(String... name) {
		Role r = getRole_obj();
		if (r != null) {
			for (String s : name) {
				if (r.has(s))
					return true;
			}
		}
		return false;
	}

	transient Role role_obj;

	public Role getRole_obj() {
		if (role_obj == null) {
			role_obj = Role.dao.load(role);
		}
		return role_obj;
	}

	public long getRole() {
		return role;
	}

	public String getAppid() {
		return appid;
	}

	public String getSecret() {
		return secret;
	}

	public String getIp() {
		return ip;
	}

	public long getLastime() {
		return lastime;
	}

	public long getExpired() {
		return expired;
	}

	/**
	 * data = Base64(AES(params)) <br>
	 * decode, params=AES(Base64(data));
	 * 
	 * @param data
	 *            the data
	 * @return JSON
	 */
	public static JSON parseParameters(String data, String secret) {
		try {
			byte[] bb = Base64.getDecoder().decode(data);

			data = new String(Digest.aes_decrypt(bb, secret));
			JSON jo = JSON.fromObject(data);
			return jo;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * data = Base64(AES(params)) <br>
	 * decode, params=AES(Base64(data));
	 * 
	 * @param jo
	 *            the json data
	 * @param secret
	 *            the secret
	 * @return the string
	 */
	public static String generateParameter(JSON jo, String secret) {
		try {
			byte[] bb = Digest.aes_encrypt(jo.toString().getBytes(), secret);
			String data = Base64.getEncoder().encodeToString(bb);
			return data;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Creates the.
	 *
	 * @param v
	 *            the v
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
	 * Delete.
	 *
	 * @param appid
	 *            the appid
	 */
	public static void delete(String appid) {
		Cache.remove("app/" + appid);
		dao.delete(W.create("appid", appid));
	}

	/**
	 * Load.
	 *
	 * @param appid
	 *            the appid
	 * @return the app
	 */
	public static App load(String appid) {
		App a = Cache.get("app/" + appid);
		if (a == null || a.expired()) {
			a = dao.load(W.create("appid", appid));
			a.setExpired(System.currentTimeMillis() + X.AMINUTE);
			Cache.set("app/" + appid, a);
		}
		return a;
	}

	/**
	 * Update.
	 *
	 * @param appid
	 *            the appid
	 * @param v
	 *            the values
	 * @return the int
	 */
	public static int update(String appid, V v) {
		Cache.remove("node/" + appid);
		return dao.update(W.create("appid", appid), v);
	}

	public static class Param {
		V v = V.create();

		/**
		 * Creates the.
		 *
		 * @return the param
		 */
		public static Param create() {
			return new Param();
		}

		/**
		 * Builds the.
		 *
		 * @return the v
		 */
		public V build() {
			return v;
		}

		/**
		 * Appid.
		 *
		 * @param appid
		 *            the appid
		 * @return the param
		 */
		public Param appid(String appid) {
			v.set("appid", appid);
			return this;
		}

		/**
		 * Secret.
		 *
		 * @param secret
		 *            the secret
		 * @return the param
		 */
		public Param secret(String secret) {
			v.set("secret", secret);
			return this;
		}

		/**
		 * Expired.
		 *
		 * @param expired
		 *            the expired
		 * @return the param
		 */
		public Param expired(long expired) {
			v.set("expired", expired);
			return this;
		}

		/**
		 * Lastime.
		 *
		 * @param lastime
		 *            the lastime
		 * @return the param
		 */
		public Param lastime(long lastime) {
			v.set("lastime", lastime);
			return this;
		}

		/**
		 * Ip.
		 *
		 * @param ip
		 *            the ip
		 * @return the param
		 */
		public Param ip(String ip) {
			v.set("ip", ip);
			return this;
		}

		/**
		 * Memo.
		 *
		 * @param memo
		 *            the memo
		 * @return the param
		 */
		public Param memo(String memo) {
			v.set("memo", memo);
			return this;
		}

		public Param role(long role) {
			v.set("role", role);
			return this;
		}

	}

	public static void main(String[] args) {
		App a = new App();
		a.appid = "1";
		a.secret = "123123";

		JSON j1 = JSON.create();
		j1.put("name", "1");
		j1.put("key", "122");

		try {
			String data = App.generateParameter(j1, a.secret);
			System.out.println("data=" + data);
			JSON jo = App.parseParameters(data, a.secret);
			System.out.println("jo=" + jo);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int inc(String name, int n) {
		return dao.inc(W.create(X.ID, id), name, n, null);
	}

}
