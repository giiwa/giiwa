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
@Table(name = "gi_authtoken", memo = "GI-授权TOKEN")
public final class AuthToken extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(AuthToken.class);

	public static final BeanDAO<String, AuthToken> dao = BeanDAO.create(AuthToken.class, time -> {
		// return cleanup query
		return W.create().and("created", Global.now() - X.ADAY * 7, W.OP.lte);
	});

	@Column(memo = "主键", unique = true, size=50)
	private String id;

	@Column(memo = "用户ID")
	private long uid;

	@Column(memo = "TOKEN")
	private String token;

	@Column(memo = "过期时间")
	private long expired;

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
		if (user_obj == null) {
			user_obj = User.dao.load(this.getUid());
		}
		return user_obj;
	}

	public static AuthToken create(long uid, String ip) {
		long expired = Global.now() + Global.getLong("session.alive", X.AWEEK / X.AHOUR) * X.AHOUR;

		return create(uid, ip, V.create("expired", expired));
	}

	/**
	 * update the session token.
	 *
	 * @param uid the uid
	 * @param ip  the ip
	 * @return the auth token
	 */
	public static AuthToken create(long uid, String ip, V v) {

		try {
			v = v.append("uid", uid).append("ip", ip);
			String token = UID.random(20);
			while (dao.exists(token)) {
				// update
				token = UID.random(20);
			}

			// insert
			dao.insert(v.append(X.ID, token).force("token", token));
			return dao.load(token);
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
		}
		return null;
	}

	/**
	 * load the AuthToken by the session and token.
	 *
	 * @param sid   the sid
	 * @param token the token
	 * @return AuthToken
	 */
	public static AuthToken load(String token) {
		return dao.load(W.create().and("token", token).and("expired", Global.now(), W.OP.gt));
	}

	/**
	 * remove all the session and token for the uid, and return all the session id
	 * for the user.
	 *
	 * @param uid the user id
	 * @return List of session
	 */
	public static void delete(long uid) {
		dao.delete(W.create().and("uid", uid));
	}

	/**
	 * load Beans by uid, a uid may has more AuthToken.
	 *
	 * @param uid the user id
	 * @return Beans of the Token
	 */
	public static Beans<AuthToken> load(long uid) {
		return dao.load(W.create().and("uid", uid).and("expired", Global.now(), W.OP.gt), 0, 100);
	}

	/**
	 * delete all the token by the uid and sid
	 * 
	 * @param uid the user id
	 * @param sid the session id
	 */
	public static void delete(long uid, String sid) {
		dao.delete(W.create().and("uid", uid).and("sid", sid));
	}

	/**
	 * cleanup the expired token
	 */
	public void cleanup() {
		dao.delete(W.create().and("expired", Global.now(), W.OP.lt));
		dao.cleanup();
	}

	public static AuthToken update(long uid, String sid, String ip) {
		long expired = Global.now() + Global.getLong("session.alive", X.AWEEK / X.AHOUR) * X.AHOUR;

		return update(uid, sid, ip, V.create("expired", expired));
	}

	/**
	 * update the session token.
	 *
	 * @param uid the uid
	 * @param sid the sid
	 * @param ip  the ip
	 * @return the auth token
	 */
	public static AuthToken update(long uid, String sid, String ip, V v) {
		String token = UID.random(20);

		String id = UID.id(uid, sid, ip, token);

		v = v.append("uid", uid).append("sid", sid).append("token", token).append("ip", ip);

		try {
			if (dao.exists(id)) {
				// update
				dao.update(id, v);
			} else {
				// insert
				dao.insert(v.append(X.ID, id));
			}
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
		}

		return dao.load(id);
	}

}
