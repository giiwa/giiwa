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
package org.giiwa.dao.bean;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.conf.Global;
import org.giiwa.dao.Bean;
import org.giiwa.dao.BeanDAO;
import org.giiwa.dao.Beans;
import org.giiwa.dao.Column;
import org.giiwa.dao.Table;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;

/**
 * The AuthToken bean. <br>
 * table="gi_authtoken"
 * 
 * @author wujun
 *
 */
@Table(name = "gi_authtoken")
public class AuthToken extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(AuthToken.class);

	public static final BeanDAO<String, AuthToken> dao = BeanDAO.create(AuthToken.class);

	@Column(name = X.ID, index = true, unique = true)
	private String id;

	@Column(name = "uid")
	private long uid;

	@Column(name = "token", index = true)
	private String token;

	@Column(name = "expired", index = true)
	private long expired;

	@Column(name = "sid")
	private String sid;

	/**
	 * get the id
	 * 
	 * @return String
	 */
	public String getId() {
		return id;
	}

	/**
	 * get the uid
	 * 
	 * @return long
	 */
	public long getUid() {
		return uid;
	}

	/**
	 * get the token
	 * 
	 * @return String
	 */
	public String getToken() {
		return token;
	}

	/**
	 * get the expired timestamp
	 * 
	 * @return long
	 */
	public long getExpired() {
		return expired;
	}

	private User user_obj;

	/**
	 * get the user object
	 * 
	 * @return User
	 */
	public User getUser_obj() {
		if (user_obj == null && this.getUid() >= 0) {
			user_obj = User.dao.load(this.getUid());
		}
		return user_obj;
	}

	/**
	 * get the session id
	 * 
	 * @return String
	 */
	public String getSid() {
		return sid;
	}

	/**
	 * update the session token.
	 *
	 * @param uid
	 *            the uid
	 * @param sid
	 *            the sid
	 * @param ip
	 *            the ip
	 * @return the auth token
	 */
	public static AuthToken update(long uid, String sid, String ip) {
		String token = UID.random(20);

		String id = UID.id(uid, sid, ip, token);

		V v = V.create("uid", uid).set("sid", sid).set("token", token).set("ip", ip);

		try {
			if (dao.exists(id)) {
				// update
				dao.update(id, v);
			} else {
				// insert
				long expired = System.currentTimeMillis()
						+ Global.getLong("session.alive", X.AWEEK / X.AHOUR) * X.AHOUR;
				dao.insert(v.set(X.ID, id).append("expired", expired));
			}
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
		}

		return dao.load(id);
	}

	/**
	 * load the AuthToken by the session and token.
	 *
	 * @param sid
	 *            the sid
	 * @param token
	 *            the token
	 * @return AuthToken
	 */
	public static AuthToken load(String sid, String token) {
		return dao.load(W.create("sid", sid).and("token", token).and("expired", System.currentTimeMillis(), W.OP.gt));
	}

	/**
	 * remove all the session and token for the uid, and return all the session id
	 * for the user.
	 *
	 * @param uid
	 *            the user id
	 * @return List of session
	 */
	public static List<String> delete(long uid) {
		List<String> list = new ArrayList<String>();
		W q = W.create("uid", uid);
		int s = 0;
		Beans<AuthToken> bs = dao.load(q, s, 10);
		while (bs != null && !bs.isEmpty()) {
			for (AuthToken t : bs) {
				String sid = t.getSid();
				list.add(sid);
			}
			s += bs.size();
			bs = dao.load(q, s, 10);
		}
		dao.delete(W.create("uid", uid));
		return list;
	}

	/**
	 * load Beans by uid, a uid may has more AuthToken.
	 *
	 * @param uid
	 *            the user id
	 * @return Beans of the Token
	 */
	public static Beans<AuthToken> load(long uid) {
		return dao.load(W.create("uid", uid).and("expired", System.currentTimeMillis(), W.OP.gt), 0, 100);
	}

	/**
	 * delete all the token by the uid and sid
	 * 
	 * @param uid
	 *            the user id
	 * @param sid
	 *            the session id
	 */
	public static void delete(long uid, String sid) {
		dao.delete(W.create("uid", uid).and("sid", sid));
	}

	/**
	 * cleanup the expired token
	 */
	public void cleanup() {
		dao.delete(W.create().and("expired", System.currentTimeMillis(), W.OP.lt));
		dao.cleanup();
	}

}
