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

import java.io.PrintStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bouncycastle.jce.provider.JDKMessageDigest.MD4;
import org.giiwa.core.bean.*;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.json.JSON;
import org.giiwa.framework.web.Model;

/**
 * 
 * The {@code User} Class is base user class, all the login/access controlled in
 * giiwa was depended on the user, it contains all the user-info, and is
 * expandable. <br>
 * table="gi_user" <br>
 * 
 * MOST important field
 * 
 * <pre>
 * id: long, global unique,
 * name: login name, global unique
 * password: string of hashed
 * nickname: string of nickname
 * title: title of the user
 * hasAccess: test whether has the access token for the user
 * </pre>
 * 
 * @author yjiang
 * 
 */
@Table(name = "gi_user")
public class User extends Bean {

	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	@Column(name = X.ID)
	private long id;

	@Column(name = "name")
	private String name;

	@Column(name = "nickname")
	private String nickname;

	@Column(name = "title")
	private String title;

	@Column(name = "password")
	private String password;

	@Column(name = "md4passwd")
	private String md4passwd;

	/**
	 * get the MD4 passwd
	 * 
	 * @return byte[]
	 */
	public byte[] getMD4passwd() {
		try {
			return md4decrypt(md4passwd);
		} catch (Exception e) {

		}
		return null;
	}

	/**
	 * get the unique ID of the user
	 * 
	 * @return long
	 */
	public long getId() {
		return id;
	}

	/**
	 * get the login name
	 * 
	 * @return String
	 */
	public String getName() {
		return name;
	}

	/**
	 * get the nick name
	 * 
	 * @return String
	 */
	public String getNickname() {
		return nickname;
	}

	/**
	 * get the phone number
	 * 
	 * @return String
	 */
	public String getPhone() {
		return this.getString("phone");
	}

	/**
	 * get the email address
	 * 
	 * @return String
	 */
	public String getEmail() {
		return this.getString("email");
	}

	/**
	 * get the title of the user
	 * 
	 * @return String
	 */
	public String getTitle() {
		return title;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.core.bean.Bean#toString()
	 */
	public String toString() {
		return "User@{id=" + this.getId() + ",name=" + this.getString("name") + "}";
	}

	/**
	 * Instantiates a new user.
	 */
	public User() {

	}

	/**
	 * Checks if is role.
	 * 
	 * @param r
	 *            the r
	 * @return true, if is role
	 */
	public boolean isRole(Role r) {
		try {
			return Helper.exists(W.create("uid", this.getId()).and("rid", r.getId()), UserRole.class);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return false;
	}

	/**
	 * Creates a user with the values, <br>
	 * if the values contains "password" field, it will auto encrypt the password
	 * field.
	 *
	 * @param v
	 *            the values
	 * @return long of the user id, if failed, return -1
	 */
	public static long create(V v) {

		String s = (String) v.value("password");
		if (s != null) {
			v.force("md4passwd", s);
			v.force("password", encrypt(s));
		}

		Long id = (Long) v.value("id");
		if (id == null) {
			id = UID.next("user.id");
			try {
				while (Helper.exists(id, User.class)) {
					id = UID.next("user.id");
				}
			} catch (Exception e1) {
				log.error(e1.getMessage(), e1);
			}
		}
		if (log.isDebugEnabled())
			log.debug("v=" + v);

		Helper.insert(
				v.set(X.ID, id).set(X.CREATED, System.currentTimeMillis()).set(X.UPDATED, System.currentTimeMillis()),
				User.class);

		return id;
	}

	/**
	 * Load user by name and password.
	 *
	 * @param name
	 *            the name of the user
	 * @param password
	 *            the password
	 * @return User, if not match anyoone, return null
	 */
	public static User load(String name, String password) {

		password = encrypt(password);

		log.debug("name=" + name + ", passwd=" + password);
		// System.out.println("name=" + name + ", passwd=" + password);

		return Helper.load(W.create("name", name).and("password", password).and("deleted", 1, W.OP.neq), User.class);

	}

	public boolean isDeleted() {
		return getInt("deleted") == 1;
	}

	/**
	 * Load user by name.
	 *
	 * @param name
	 *            the name of the name
	 * @return User
	 */
	public static User load(String name) {
		return Helper.load(W.create("name", name).and("deleted", 1, W.OP.neq).sort(X.ID, -1), User.class);
	}

	/**
	 * load user by query
	 * 
	 * @param q
	 * @return
	 */
	public static User load(W q) {
		return Helper.load(q.sort(X.ID, -1), User.class);
	}

	/**
	 * Load user by id.
	 * 
	 * @param id
	 *            the user id
	 * @return User
	 */
	public static User loadById(long id) {

		return Helper.load(id, User.class);
	}

	/**
	 * Load the user object by id
	 *
	 * @param id
	 *            the id
	 * @return the user
	 */
	public static User load(long id) {
		return Helper.load(id, User.class);
	}

	/**
	 * Load users by access token name.
	 *
	 * @param access
	 *            the access token name
	 * @return list of user who has the access token
	 */
	public static List<User> loadByAccess(String access) {

		Beans<Role> bs = Role.loadByAccess(access, 0, 1000);
		W q = W.create();

		if (bs != null) {
			if (bs.size() > 1) {
				W list = W.create();
				for (Role a : bs) {
					list.or("rid", a.getId());
				}
				q.and(list);
			} else if (bs.size() == 1) {
				q.and("rid", bs.get(0).getId());
			}

		}

		Beans<UserRole> b2 = Helper.load(q, 0, 1000, UserRole.class);
		q = W.create();
		if (b2 != null) {
			if (b2.size() > 1) {
				W list = W.create();
				for (UserRole a : b2) {
					list.or("id", a.getLong("uid"));
				}
				q.and(list);
			} else if (b2.size() == 1) {
				q.and("id", b2.get(0).getLong("uid"));
			}
		}

		q.and("deleted", 1, W.OP.neq);

		Beans<User> us = Helper.load(q.sort("name", 1), 0, Integer.MAX_VALUE, User.class);
		return us;

	}

	/**
	 * Validate the user with the password.
	 *
	 * @param password
	 *            the password
	 * @return true, if the password was match
	 */
	public boolean validate(String password) {

		/**
		 * if the user has been locked, then not allow to login
		 */
		if (this.isLocked())
			return false;

		password = encrypt(password);
		return get("password") != null && get("password").equals(password);
	}

	/**
	 * whether the user has been locked
	 * 
	 * @return boolean
	 */
	public boolean isLocked() {
		return getInt("locked") > 0;
	}

	/**
	 * Checks whether has the access token.
	 * 
	 * @param name
	 *            the name of the access token
	 * @return true, if has anyone
	 */
	public boolean hasAccess(String... name) {
		if (this.getId() == 0L) {
			for (String s : name) {
				if (X.isSame(s, "access.config.admin"))
					return true;
			}
			return false;
		}

		// log.debug("uid=" + this.getId() + ", access=" + Helper.toString(name));
		if (role == null) {
			getRole();
		}

		try {
			return role.hasAccess(id, name);
		} catch (Exception e) {
			// ignore
		}
		return false;
	}

	transient Roles role = null;

	/**
	 * get the roles for the user
	 * 
	 * @return Roles
	 */
	public Roles getRole() {
		if (role == null) {
			Beans<UserRole> bs = Helper.load(W.create("uid", this.getId()), 0, 100, UserRole.class);
			if (bs != null) {
				List<Long> roles = new ArrayList<Long>();
				for (UserRole r : bs) {
					roles.add(r.getLong("rid"));
				}
				role = new Roles(roles);
			}
		}
		return role;
	}

	/**
	 * set a role to a user with role id
	 * 
	 * @param rid
	 *            the role id
	 */
	public void setRole(long rid) {
		try {
			if (!Helper.exists(W.create("uid", this.getId()).and("rid", rid), UserRole.class)) {
				Helper.insert(V.create("uid", this.getId()).set("rid", rid), UserRole.class);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Removes the role.
	 * 
	 * @param rid
	 *            the rid
	 */
	public void removeRole(long rid) {
		Helper.delete(W.create("uid", this.getId()).and("rid", rid), UserRole.class);
	}

	/**
	 * Removes the all roles.
	 */
	public void removeAllRoles() {
		Helper.delete(W.create("uid", this.getId()), UserRole.class);

	}

	/**
	 * encrypt the password
	 * 
	 * @param passwd
	 *            the password
	 * @return the string
	 */
	public static String encrypt(String passwd) {
		if (X.isEmpty(passwd)) {
			return X.EMPTY;
		}
		return UID.id(passwd);
	}

	public static byte[] md4decrypt(String passwd) {
		String[] ss = X.split(passwd, ":");
		if (ss == null || ss.length == 0)
			return null;

		byte[] bb = new byte[ss.length];

		for (int i = 0; i < ss.length; i++) {
			char[] b1 = ss[i].toCharArray();
			if (b1.length > 1) {
				bb[i] = (byte) (X.hexToInt(b1[0]) * 16 + X.hexToInt(b1[1]));
			} else {
				bb[i] = (byte) (X.hexToInt(b1[0]));
			}
		}
		return bb;
	}

	public static String md4encrypt(String passwd) {
		if (X.isEmpty(passwd)) {
			return X.EMPTY;
		}
		try {
			MessageDigest md4 = MD4.getInstance("MD4");
			byte[] bb = md4.digest(passwd.getBytes("UnicodeLittleUnmarked"));
			StringBuilder sb = new StringBuilder();
			for (byte b : bb) {
				if (sb.length() > 0)
					sb.append(":");
				sb.append(X.toHex(b));
			}
			return sb.toString();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return X.EMPTY;
	}

	public static void main(String[] args) {
		String s = "123123";
		String s1 = md4encrypt(s);
		System.out.println(s1);
		System.out.println(Arrays.toString(md4decrypt(s1)));

		byte[] bb = null;
		try {
			MessageDigest md4 = MD4.getInstance("MD4");
			bb = md4.digest(s.getBytes("UnicodeLittleUnmarked"));
			System.out.println(Arrays.toString(bb));

			md4 = MD4.getInstance("MD4");
			bb = s.getBytes("UnicodeLittleUnmarked");
			md4.update(bb);
			byte[] p21 = new byte[21];
			bb = md4.digest();
			System.arraycopy(bb, 0, p21, 0, 16);

			System.out.println(Arrays.toString(bb));
			System.out.println(Arrays.toString(p21));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Load the users by the query.
	 *
	 * @param q
	 *            the query of the condition
	 * @param offset
	 *            the start number
	 * @param limit
	 *            the number
	 * @return Beans
	 */
	public static Beans<User> load(W q, int offset, int limit) {
		return Helper.load(q.and(X.ID, 0, W.OP.gt).sort("name", 1), offset, limit, User.class);
	}

	/**
	 * Update the user with the V.
	 *
	 * @param v
	 *            the values
	 * @return int
	 */
	public int update(V v) {
		for (String name : v.names()) {
			this.set(name, v.value(name));
		}
		return update(this.getId(), v);
	}

	/**
	 * update the user by the values, <br>
	 * if the values contains "password" field, it will auto encrypt the password
	 * field.
	 *
	 * @param id
	 *            the user id
	 * @param v
	 *            the values
	 * @return int, 0 no user updated
	 */
	public static int update(long id, V v) {

		String passwd = (String) v.value("password");
		if (!X.isEmpty(passwd)) {

			v.force("md4passwd", md4encrypt(passwd));
			passwd = encrypt(passwd);
			v.force("password", passwd);
		} else {
			v.remove("password");
		}
		return Helper.update(id, v.set(X.UPDATED, System.currentTimeMillis()), User.class);
	}

	/**
	 * update the user by query
	 * 
	 * @param q
	 *            the query
	 * @param v
	 *            the value
	 * @return the number of updated
	 */
	public static int update(W q, V v) {

		String passwd = (String) v.value("password");
		if (!X.isEmpty(passwd)) {

			v.force("md4passwd", md4encrypt(passwd));
			passwd = encrypt(passwd);
			v.force("password", passwd);
		} else {
			v.remove("password");
		}
		return Helper.update(q, v.set(X.UPDATED, System.currentTimeMillis()), User.class);
	}

	/***
	 * replace all the roles for the user
	 * 
	 * @param roles
	 *            the list of role id
	 */
	public void setRoles(List<Long> roles) {
		this.removeAllRoles();
		for (long rid : roles) {
			this.setRole(rid);
		}
	}

	/**
	 * record the login failure, and record the user lock info.
	 *
	 * @param ip
	 *            the ip that login come from
	 * @param sid
	 *            the session id
	 * @param useragent
	 *            the browser agent
	 * @return int of the locked times
	 */
	public int failed(String ip, String sid, String useragent) {
		set("failtimes", getInt("failtimes") + 1);

		return Lock.locked(getId(), sid, ip, useragent);
	}

	/**
	 * record the logout info in database for the user.
	 *
	 * @return the int
	 */
	public int logout() {
		return Helper.update(getId(), V.create("sid", X.EMPTY).set(X.UPDATED, System.currentTimeMillis()), User.class);
	}

	/**
	 * record login info in database for the user.
	 *
	 * @param sid
	 *            the session id
	 * @param ip
	 *            the ip that the user come fram
	 * @return the int
	 */
	public int logined(String sid, String ip) {

		// update
		set("logintimes", getInt("logintimes") + 1);

		Lock.removed(getId(), sid);

		/**
		 * cleanup the old sid for the old logined user
		 */
		Helper.update(W.create("sid", sid), V.create("sid", X.EMPTY), User.class);

		return Helper.update(getId(),
				V.create("lastlogintime", System.currentTimeMillis()).set("logintimes", getInt("logintimes"))
						.set("ip", ip).set("failtimes", 0).set("locked", 0).set("lockexpired", 0).set("sid", sid)
						.set(X.UPDATED, System.currentTimeMillis()),
				User.class);

	}

	@Table(name = "gi_userrole")
	public static class UserRole extends Bean {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Column(name = "uid")
		long uid;

		@Column(name = "rid")
		long rid;

	}

	/**
	 * The {@code Lock} Class used to record login failure log, was used by webgiiwa
	 * framework. <br>
	 * collection="gi_userlock"
	 * 
	 * @author joe
	 *
	 */
	@Table(name = "gi_userlock")
	public static class Lock extends Bean {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * Locked.
		 *
		 * @param uid
		 *            the uid
		 * @param sid
		 *            the sid
		 * @param host
		 *            the host
		 * @param useragent
		 *            the useragent
		 * @return the int
		 */
		public static int locked(long uid, String sid, String host, String useragent) {
			return Helper.insert(V.create("uid", uid).set("sid", sid).set("host", host).set("useragent", useragent)
					.set(X.CREATED, System.currentTimeMillis()), Lock.class);
		}

		/**
		 * Removed.
		 *
		 * @param uid
		 *            the uid
		 * @return the int
		 */
		public static int removed(long uid) {
			return Helper.delete(W.create("uid", uid), Lock.class);
		}

		/**
		 * Removed.
		 *
		 * @param uid
		 *            the uid
		 * @param sid
		 *            the sid
		 * @return the int
		 */
		public static int removed(long uid, String sid) {
			return Helper.delete(W.create("uid", uid).and("sid", sid), Lock.class);
		}

		/**
		 * Load.
		 *
		 * @param uid
		 *            the uid
		 * @param time
		 *            the time
		 * @return the list
		 */
		public static List<Lock> load(long uid, long time) {
			Beans<Lock> bs = Helper.load(W.create("uid", uid).and(X.CREATED, time, W.OP.gt).sort(X.CREATED, 1), 0,
					Integer.MAX_VALUE, Lock.class);
			return bs;
		}

		/**
		 * Load by sid.
		 *
		 * @param uid
		 *            the uid
		 * @param time
		 *            the time
		 * @param sid
		 *            the sid
		 * @return the list
		 */
		public static List<Lock> loadBySid(long uid, long time, String sid) {
			Beans<Lock> bs = Helper.load(
					W.create("uid", uid).and(X.CREATED, time, W.OP.gt).and("sid", sid).sort(X.CREATED, 1), 0,
					Integer.MAX_VALUE, Lock.class);
			return bs;
		}

		/**
		 * Load by host.
		 *
		 * @param uid
		 *            the uid
		 * @param time
		 *            the time
		 * @param host
		 *            the host
		 * @return the list
		 */
		public static List<Lock> loadByHost(long uid, long time, String host) {
			Beans<Lock> bs = Helper.load(
					W.create("uid", uid).and(X.CREATED, time, W.OP.gt).and("host", host).sort(X.CREATED, 1), 0,
					Integer.MAX_VALUE, Lock.class);
			return bs;
		}

		/**
		 * delete all user lock info for the user id
		 * 
		 * @param uid
		 *            the user id
		 * @return the number deleted
		 */
		public static int cleanup(long uid) {
			return Helper.delete(W.create("uid", uid), Lock.class);
		}

		public long getUid() {
			return getLong("uid");
		}

		public long getCreated() {
			return getLong(X.CREATED);
		}

		public String getSid() {
			return getString("sid");
		}

		public String getHost() {
			return getString("host");
		}

		public String getUseragent() {
			return getString("useragent");
		}

	}

	/**
	 * Delete the user by ID.
	 *
	 * @param id
	 *            the id of the user
	 * @return int how many was deleted
	 */
	public static int delete(long id) {

		Lock.cleanup(id);

		return Helper.delete(id, User.class);
	}

	private List<AuthToken> token_obj;

	public List<AuthToken> getTokens() {
		if (token_obj == null) {
			Beans<AuthToken> bs = AuthToken.load(this.getId());
			if (bs != null) {
				token_obj = bs;
			}
		}
		return token_obj;

	}

	/**
	 * check the database, if there is no "config.admin" user, then create the
	 * "admin" user, with "admin" as password
	 */
	public static void checkAndInit() {
		if (Helper.isConfigured()) {
			if (!User.exists(0)) {
				List<User> list = User.loadByAccess("access.config.admin");
				if (list == null || list.size() == 0) {
					String passwd = UID.random(20);
					try {
						PrintStream out = new PrintStream(Model.GIIWA_HOME + "/admin.pwd");
						out.print(passwd);
						out.close();
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
					User.create(V.create("id", 0L).set("name", "admin").set("password", passwd).set("title", "Admin"));
				}
			}
		}
	}

	/**
	 * test the user exists for the query.
	 *
	 * @param q
	 *            the query
	 * @return boolean
	 */
	public static boolean exists(W q) {
		try {
			return Helper.exists(q, User.class);
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
		}
		return false;
	}

	/**
	 * test the user exists for the id.
	 *
	 * @param id
	 *            the id
	 * @return boolean
	 */
	public static boolean exists(long id) {
		try {
			return Helper.exists(id, User.class);
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
		}
		return false;
	}

	public static class Param {
		V v = V.create();

		public static Param create() {
			return new Param();
		}

		public V build() {
			return v;
		}

		public Param name(String name) {
			v.force("name", name);
			return this;
		}

		public Param nickname(String nickname) {
			v.force("nickname", nickname);
			return this;
		}

		public Param title(String title) {
			v.force("title", title);
			return this;
		}

		public Param password(String password) {
			v.force("password", password);
			return this;
		}

		public Param photo(String photo) {
			v.force("photo", photo);
			return this;
		}

	}

	public static void to(JSON j) {
		int s = 0;
		W q = W.create().and(X.ID, 0, W.OP.gt).sort(X.ID, 1);

		List<JSON> l1 = new ArrayList<JSON>();
		Beans<User> bs = User.load(q, s, 100);
		while (bs != null && !bs.isEmpty()) {
			for (User e : bs) {
				l1.add(e.getJSON());
			}
			s += bs.size();
			bs = User.load(q, s, 100);
		}

		j.append("users", l1);
	}

	public static int from(JSON j) {
		int total = 0;
		List<JSON> l1 = j.getList("users");
		if (l1 != null) {
			for (JSON e : l1) {
				long id = e.getLong(X.ID);
				V v = V.fromJSON(e);
				v.remove(X.ID, "_id");
				User s = User.load(W.create(X.ID, id));
				if (s != null) {
					Helper.update(id, v, User.class);
				} else {
					Helper.insert(v.append(X.ID, id), User.class);
				}
				total++;
			}
		}
		return total;
	}

}
