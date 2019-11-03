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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.base.Digest;
import org.giiwa.core.base.H32;
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

	@Column(name = "role")
	private long role;

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

	/**
	 * check has the access name
	 * 
	 * @param name the name string...
	 * @return the boolean, true if has this access
	 */
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

	/**
	 * get the role object attach with this appid
	 * 
	 * @return the Role
	 */
	public Role getRole_obj() {
		if (role_obj == null) {
			role_obj = Role.dao.load(role);
		}
		return role_obj;
	}

	/**
	 * get tht role id
	 * 
	 * @return the role if
	 */
	public long getRole() {
		return role;
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
	 * Delete.
	 *
	 * @param appid the appid
	 */
	public static void delete(String appid) {
		Cache.remove("app/" + appid);
		dao.delete(W.create("appid", appid));
	}

	/**
	 * Load.
	 *
	 * @param appid the appid
	 * @return the app
	 */
	public static App load(String appid) {
		App a = Cache.get("app/" + appid);
		if (a == null) {
			a = dao.load(W.create("appid", appid));
			if (a != null) {
				Cache.set("app/" + appid, a, X.AMINUTE);
			}
		}
		return a;
	}

	/**
	 * Update.
	 *
	 * @param appid the appid
	 * @param v     the values
	 * @return the int
	 */
	public static int update(String appid, V v) {
		Cache.remove("app/" + appid);
		return dao.update(W.create("appid", appid), v);
	}

	/**
	 * the parameter class of the App
	 * 
	 * @author joe
	 *
	 */
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
		 * Builds the Value object
		 *
		 * @return the v
		 */
		public V build() {
			return v;
		}

		/**
		 * set the appid
		 *
		 * @param appid the appid
		 * @return the param
		 */
		public Param appid(String appid) {
			v.set("appid", appid);
			return this;
		}

		/**
		 * set the secret
		 *
		 * @param secret the secret
		 * @return the param
		 */
		public Param secret(String secret) {
			v.set("secret", secret);
			return this;
		}

		/**
		 * set the Expired.
		 *
		 * @param expired the expired
		 * @return the param
		 */
		public Param expired(long expired) {
			v.set("expired", expired);
			return this;
		}

		/**
		 * set the Lastime.
		 *
		 * @param lastime the lastime
		 * @return the param
		 */
		public Param lastime(long lastime) {
			v.set("lastime", lastime);
			return this;
		}

		/**
		 * set the Ip.
		 *
		 * @param ip the ip
		 * @return the param
		 */
		public Param ip(String ip) {
			v.set("ip", ip);
			return this;
		}

		/**
		 * set the Memo.
		 *
		 * @param memo the memo
		 * @return the param
		 */
		public Param memo(String memo) {
			v.set("memo", memo);
			return this;
		}

		/**
		 * set the role
		 * 
		 * @param role
		 * @return the Param object
		 */
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
			String data = App.encode(j1.toPrettyString(), a.secret);
			System.out.println("data=" + data);
			String jo = App.decode(data, a.secret);
			System.out.println("jo=" + jo);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
