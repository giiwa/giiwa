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

import java.io.Serializable;
import java.util.*;

import org.apache.commons.logging.*;
import org.giiwa.cache.*;
import org.giiwa.conf.Global;
import org.giiwa.dao.Bean;
import org.giiwa.dao.BeanDAO;
import org.giiwa.dao.Column;
import org.giiwa.dao.Table;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;

/**
 * Session of http request
 * 
 * @author yjiang
 * 
 */
public final class Session implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static Log log = LogFactory.getLog(Session.class);

	private static int MAX = 128;

	String sid;

	Map<String, Object> a = new TreeMap<String, Object>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object.toString()
	 */
	public String toString() {
		return new StringBuilder("Session@{sid=").append(sid).append(",data=").append(a).append("}").toString();
	}

	/**
	 * Exists.
	 * 
	 * @param sid the sid
	 * @return true, if successful
	 */
	public static boolean exists(String sid) {
		Session o = Cache.get("session/" + sid);
		return o != null;
	}

	/**
	 * Delete.
	 * 
	 * @param sid the sid
	 */
	public static void delete(String sid) {
		Cache.remove("session/" + sid);
		SID.dao.delete(sid);
//		log.warn("remove session, sid=" + sid);
	}

	/**
	 * Load session by sid and ip
	 * 
	 * @param sid the sid
	 * @return the session
	 */
	public static Session load(String sid, String ip) {

		if (X.isEmpty(sid)) {
			return null;
		}

//		log.debug("new session", new Exception());

		Session o = (Session) Cache.get("session/" + sid);

		if (o == null || (Global.getInt("session.baseip", 0) == 1 && !X.isSame(ip, o.get("ip")))) {
			o = new Session();

			/**
			 * set the session expired time
			 */
			o.sid = sid;
			try {
				o.set("ip", ip);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}

			if (log.isDebugEnabled())
				log.debug("new session, sid=" + sid);

		}

		return o;
	}

	/**
	 * Checks for.
	 * 
	 * @param key the key
	 * @return true, if successful
	 */
	public boolean has(String key) {
		return a.containsKey(key);
	}

	/**
	 * Removes the.
	 * 
	 * @param key the key
	 * @return the session
	 */
	public Session remove(String key) {
		a.remove(key);
		return this;
	}

	/**
	 * Store the session with configured expired
	 * 
	 * @return the session
	 */
	public Session store() {
		long expired = Global.getLong("session.alive", X.AWEEK / X.AHOUR) * X.AHOUR;
		if (expired < 0) {
			expired = 7 * 24 * X.AHOUR;
		}

		return store(expired);
	}

	/**
	 * store the session with the expired
	 * 
	 * @param expired the expired timestamp, ms in future
	 * @return Session
	 */
	public Session store(long expired) {

		if (log.isDebugEnabled())
			log.debug("store session, sid=" + sid + ", expired=" + expired);

		if (!Cache.set("session/" + sid, this, expired)) {
			log.error("set session failed !", new Exception("store session failed"));
		}

		return this;
	}

	/**
	 * Sets the.
	 * 
	 * @param key the key
	 * @param o   the o
	 * @return the session
	 */
	public Session set(String key, Object o) throws Exception {
		if (a.size() < MAX) {
			a.put(key, o);
		} else {
			throw new Exception("exceed the MAX=" + MAX);
		}

		return this;
	}

	/**
	 * Sid.
	 * 
	 * @return the string
	 */
	public String sid() {
		return sid;
	}

	/**
	 * Gets the.
	 * 
	 * @param <T> the object
	 * @param key the key
	 * @return the object
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(String key) {
		return (T) a.get(key);
	}

	/**
	 * Gets the int.
	 * 
	 * @param key the key
	 * @return the int
	 */
	public int getInt(String key) {
		Integer i = (Integer) a.get(key);
		if (i != null) {
			return i;
		}
		return 0;
	}

	/**
	 * Clear.
	 */
	public void clear() {
		a.clear();
	}

	public static void expired(long uid) {
		try {
			W q = W.create().and("uid", uid).sort("sid", 1);
			SID.dao.stream(q, e -> {
				Session.delete(e.sid);
				return true;
			});
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Table(name = "gi_sid")
	public static class SID extends Bean {

		private static final long serialVersionUID = 1L;

		public static final BeanDAO<String, SID> dao = BeanDAO.create(SID.class);

		String id;

		@Column(memo = "会话ID")
		String sid;

		@Column(memo = "用户ID")
		long uid;

		@Column(memo = "IP")
		String ip;

		@Column(memo = "browser")
		String browser;

		public static void update(String sid, long uid, String ip, String browser) {
			try {
				V v = V.create("uid", uid).append("sid", sid).append("ip", ip).append("browser", browser);
				if (dao.exists(sid)) {
					dao.update(sid, v);
				} else {
					dao.insert(v.append(X.ID, sid));
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

	}

}
