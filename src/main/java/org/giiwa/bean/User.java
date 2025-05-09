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

import java.awt.Color;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.app.web.user;
import org.giiwa.bean.Session.SID;
import org.giiwa.cache.TimingCache;
import org.giiwa.conf.Config;
import org.giiwa.conf.Global;
import org.giiwa.dao.Bean;
import org.giiwa.dao.BeanDAO;
import org.giiwa.dao.Beans;
import org.giiwa.dao.Column;
import org.giiwa.dao.Helper;
import org.giiwa.dao.Table;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.dfile.DFile;
import org.giiwa.json.JSON;
import org.giiwa.misc.Base32;
import org.giiwa.misc.Digest;
import org.giiwa.misc.GImage;
import org.giiwa.web.Language;

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
@Table(name = "gi_user", memo = "GI-用户")
public final class User extends Bean {

	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(User.class);

	public static final BeanDAO<Long, User> dao = BeanDAO.create(User.class);

	@Column(memo = "主键", unique = true)
	public long id;

	@Column(memo = "登录名", size = 50)
	public String name;

	@Column(memo = "昵称", size = 50)
	public String nickname;

	@Column(memo = "称谓", size = 50)
	public String title;

	@Column(memo = "用户组", value = "unit.id")
	public long unitid;

	@Column(memo = "密码", size = 128)
	private String password;

	@Column(memo = "密码设置时间")
	public long passwordtime;

	@Column(memo = "删除", value = "1:yes")
	int deleted;

//	private String md5passwd;
//
//	private String createdip;

	@Column(memo = "头像", size = 128)
	public String photo;

	@Column(memo = "创建用户UA", size = 255)
	private String createdua;

	@Column(memo = "限制IP", value = "1:yes")
	private int limitip;

	@Column(memo = "文件仓库限制空间", value = "GB, <0:不限制")
	public long disklimitsize;

	@Column(memo = "创建用户ID")
	private long createdby;

	transient User createdby_obj;

	public User getCreatedby_obj() {
		if (createdby_obj == null) {
			createdby_obj = dao.load(createdby);
		}
		return createdby_obj;
	}

	public String getPhoto() {
		return X.isEmpty(photo) ? "/images/user.png" : photo;
	}

	/**
	 * get the unique ID of the user
	 * 
	 * @return long
	 */
	public long getId() {
		return id;
	}

	public String getPassword() {
		return password;
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
	 * @see org.giiwa.core.bean.Bean.toString()
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
	 * @param r the r
	 * @return true, if is role
	 */
	public boolean isRole(Role r) {
		try {
			return UserRole.dao.exists(W.create().and("uid", this.getId()).and("rid", r.getId()));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return false;
	}

	private static void _check(V v, String... ss) throws Exception {
		if (ss == null || ss.length == 0)
			return;

		for (String s : ss) {
			String o = (String) v.value(s);
			if (X.isSame("name", s)) {
				String rule = Global.getString("user.name.rule", "^[a-zA-Z0-9]{4,16}$");
				if (!X.isEmpty(rule) && !o.matches(rule)) {
					throw new Exception(Global.getString("user.name.rule.tips", "name MUST 4+ char or digital"));
				}
			} else if (X.isSame("password", s)) {
				String rule = Global.getString("user.passwd.rule", "^[a-zA-Z0-9]{6,16}$");
				if (X.isEmpty(o) || (!X.isEmpty(rule) && !o.matches(rule))) {
					throw new Exception(Global.getString("user.passwd.rule.tips", "password MUST 6+ char or digital"));
				}
			}
		}
	}

	/**
	 * @Deprecated
	 * @param v
	 * @return
	 * @throws Exception
	 */
	public synchronized static long create(V v) throws Exception {
		String name = v.value("name").toString();
		return create(name, v);
	}

	/**
	 * Creates a user with the values, <br>
	 * if the values contains "password" field, it will auto encrypt the password
	 * field.
	 *
	 * @param v the values
	 * @return long of the user id, if failed, return -1
	 * @throws Exception throw Exception if name or password not matches the setting
	 */
	public synchronized static long create(String name, V v) throws Exception {

		if (dao.exists(W.create().and("name", name))) {
			throw new Exception(Language.getLanguage().get("user.name.exists"));
		}

		v.append("name", name);
		// check name and password
		_check(v, "name", "password");

		String s = (String) v.value("password");
		if (s != null) {
			v.force("password", encrypt2(s));
			v.append("passwordtime", Global.now());
		}

		Long id = (Long) v.value("id");
		if (id == null) {
			id = UID.next("user.id");
			try {
				while (dao.exists(id)) {
					id = UID.next("user.id");
				}
			} catch (Exception e1) {
				log.error(e1.getMessage(), e1);
			}
		}
		if (log.isDebugEnabled()) {
			log.debug("v=" + v);
		}

		// check photo
		_checkphoto(-1, v);

		v.append("deleted", 0);

		dao.insert(v.append(X.ID, id).append(X.CREATED, Global.now()).append(X.UPDATED,
				Global.now()));

		GLog.securitylog.warn(user.class, "create", "name=" + name + ", nickname=" + v.value("nickname"), dao.load(id),
				(String) v.value("createdip"));

		return id;
	}

	private static void _checkphoto(long id, V v) {

		try {
			Object nickname = v.value("nickname");

			if (X.isEmpty(nickname)) {
				return;
			}

			User u = dao.load(id);
			if (u != null && !X.isEmpty(u.photo)) {
				if (X.isSame(nickname, u.nickname) && Disk.seek(u.photo).exists()) {
					return;
				}
			}

			char c = nickname.toString().charAt(0);

			Language lang = Language.getLanguage();
			DFile f = Disk.seek("/user/photo/auto/" + lang.format(Global.now(), "yyyy/MM/dd") + "/"
					+ Global.now() + ".png");

			if (f != null) {
				GImage.cover(145, Character.toString(c).toUpperCase(), new Color((int) (128 * Math.random()),
						(int) (156 * Math.random()), (int) (156 * Math.random())), f.getOutputStream());
				v.append("photo", "/f/g/" + f.getId() + "/" + f.getName());
			}

		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Load user by name and password.
	 *
	 * @param name     the name of the user
	 * @param password the password
	 * @return User, if not match anyoone, return null
	 * @throws Exception
	 */
	public static User load(String name, String password, String ip) throws Exception {

		String password2 = encrypt2(password);
		W q = W.create().and("name", name).and("password", password2).and("deleted", 1, W.OP.neq);
		User e = dao.load(q);
//		log.warn("q=" + q + ", e=" + e);

		if (e != null) {
			if (e.id == 0 && !X.isIn(Config.getConf().getString("root.login", "no"), "yes", "true")) {
				throw new Exception("root disallow");
			}
			return e;
		}

		String password1 = encrypt1(password);
		e = dao.load(W.create().and("name", name).and("password", password1).and("deleted", 1, W.OP.neq));
		if (e != null) {

			dao.update(e.id, V.create().append("password", password2));

			if (e.id == 0 && !X.isIn(Config.getConf().getString("root.login", "no"), "yes", "true")) {
				throw new Exception("root disallow");
			}

			return e;
		}

		// Compatible old giiwa, 1, manual clean the password in database, 2, using this
		// method to reset the password
		e = dao.load(W.create().and("name", name).and("password", "").and("deleted", 1, W.OP.neq));
		if (e != null) {
			dao.update(e.id, V.create().append("password", password2));

			if (e.id > 0 || X.isIn(Config.getConf().getString("root.login", "no"), "yes", "true")) {
				return e;
			}

			GLog.securitylog.warn(user.class, "passwd", "set password as original is empty!", null, ip);
		}

		return null;
	}

	public boolean isDeleted() {
		return getInt("deleted") == 1;
	}

	/**
	 * Load user by name.
	 *
	 * @param name the name of the name
	 * @return User
	 */
	public static User load(String name) {
		return dao.load(W.create().and("name", name).and("deleted", 1, W.OP.neq).sort(X.UPDATED, -1));
	}

	/**
	 * Load users by access token name.
	 *
	 * @param access the access token name
	 * @return list of user who has the access token
	 */
	public static Beans<User> loadByAccess(String access) {

		Beans<Role> bs = Role.loadByAccess(access, 0, 1000);

		if (bs == null || bs.isEmpty()) {
			return null;
		}

		List<Long> l1 = new ArrayList<Long>();
		for (Role a : bs) {
			l1.add(a.getId());
		}

		Beans<UserRole> b2 = UserRole.dao.load(W.create().and("rid", l1), 0, 1000);

		if (b2 == null || b2.isEmpty()) {
			return null;
		}

		l1.clear();
		for (UserRole a : b2) {
			l1.add(a.getLong("uid"));
		}

		Beans<User> us = dao.load(
				W.create().and("id", l1).and("deleted", 1, W.OP.neq).and("locked", 1, W.OP.neq).sort("name", 1), 0,
				Integer.MAX_VALUE);
		return us;

	}

	/**
	 * Validate the user with the password.
	 *
	 * @param password the password
	 * @return true, if the password was match
	 */
	public boolean validate(String password) {

		/**
		 * if the user has been locked, then not allow to login
		 */
		if (this.isLocked())
			return false;

		try {
			String password2 = encrypt2(password);
			if (get("password") != null && get("password").equals(password2)) {
				return true;
			}

			String password1 = encrypt1(password);
			return get("password") != null && get("password").equals(password1);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return false;
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
	 * @param name the name of the access token
	 * @return true, if has anyone
	 */
	public boolean hasAccess(String... name) {

		if (this.id == 0L) {
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
			log.error(e.getMessage(), e);
		}
		return false;
	}

	transient Roles role = null;

	/**
	 * get the roles for the user
	 * 
	 * @return Roles
	 */
	@SuppressWarnings("unchecked")
	public Roles getRole() {
		if (role == null) {
			Roles r = TimingCache.get(Roles.class, id);
			if (r == null) {
				List<?> l1 = UserRole.dao.distinct("rid", W.create().and("uid", this.getId()));
				if (l1 != null && !l1.isEmpty()) {
					r = new Roles((List<Long>) l1);
					TimingCache.set(Roles.class, id, r);
				}
			}
			role = r;
		}
		return role;
	}

	/**
	 * set a role to a user with role id
	 * 
	 * @param rid the role id
	 */
	public void setRole(long rid) {
		try {
			if (!UserRole.dao.exists(W.create().and("uid", this.getId()).and("rid", rid))) {
				UserRole.dao.insert(
						V.create("uid", this.getId()).append("rid", rid).append(X.ID, UID.id(this.getId(), rid)));
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Removes the role.
	 * 
	 * @param rid the rid
	 */
	public void removeRole(long rid) {

		UserRole.dao.delete(W.create().and("uid", this.getId()).and("rid", rid));

		GLog.securitylog.info(user.class, "grant", "remove role", this, null);
	}

	/**
	 * Removes the all roles.
	 */
	public void removeAllRoles() {
		UserRole.dao.delete(W.create().and("uid", this.getId()));
		GLog.securitylog.info(user.class, "grant", "remove all role", this, null);
	}

	/**
	 * encrypt the password
	 * 
	 * @param passwd the password
	 * @return the string
	 * @throws Exception
	 */
	public static String encrypt2(String passwd) throws Exception {
		if (X.isEmpty(passwd)) {
			return X.EMPTY;
		}

		return Base32.encode(Digest.encode(passwd.getBytes(), Key.get("giiwa", 24)));
	}

	/**
	 * @deprecated
	 * 
	 * @param passwd
	 * @return
	 * @throws Exception
	 */
	public static String encrypt1(String passwd) throws Exception {
		if (X.isEmpty(passwd)) {
			return X.EMPTY;
		}
		return UID.id(passwd);
	}

	public static String decrypt(String passwd) throws Exception {
		if (X.isEmpty(passwd)) {
			return X.EMPTY;
		}
		return new String(Digest.decode(Base32.decode(passwd), Key.get("giiwa", 24)));
	}

	/**
	 * Load the users by the query.
	 *
	 * @param q      the query of the condition
	 * @param offset the start number
	 * @param limit  the number
	 * @return Beans
	 */
	public static Beans<User> load(W q, int offset, int limit) {
		return dao.load(q.and(X.ID, 0, W.OP.gt), offset, limit);
	}

	/**
	 * Update the user with the V.
	 *
	 * @param v the values
	 * @return int
	 * @throws Exception
	 */
	public int update(V v) throws Exception {
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
	 * @param id the user id
	 * @param v  the values
	 * @return int, 0 no user updated
	 * @throws Exception
	 */
	public static int update(long id, V v) throws Exception {

		v.remove("name");

		String passwd = (String) v.value("password");
		if (!X.isEmpty(passwd)) {

			_check(v, "password");
			passwd = encrypt2(passwd);
			v.force("password", passwd);

		} else {
			v.remove("password");
		}

		_checkphoto(id, v);

		return dao.update(id, v);
	}

	/**
	 * update the user by query
	 * 
	 * @param q the query
	 * @param v the value
	 * @return the number of updated
	 * @throws Exception
	 */
	public static int update(W q, V v) throws Exception {

		v.remove("name");

		String passwd = (String) v.value("password");
		if (!X.isEmpty(passwd)) {

			_check(v, "password");

			passwd = encrypt2(passwd);
			v.force("password", passwd);
		} else {
			v.remove("password");
		}
		return dao.update(q, v);
	}

	/***
	 * replace all the roles for the user
	 * 
	 * @param roles the list of role id
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
	 * @param ip        the ip that login come from
	 * @param sid       the session id
	 * @param useragent the browser agent
	 * @return int of the locked times
	 */
	public int failed(String ip, String sid, String useragent) {
		set("failtimes", getInt("failtimes") + 1);

		Lock.locked(id, sid, ip, useragent);

//		if (isLocked(ip)) {
//			User.dao.update(getId(), V.create("locked", 1));
//		}
		return 1;
	}

	/**
	 * record the logout info in database for the user.
	 *
	 * @return the int
	 */
	public int logout() {
		return dao.update(getId(), V.create("sid", X.EMPTY));
	}

	/**
	 * record login info in database for the user.
	 *
	 * @param sid the session id
	 * @param ip  the ip that the user come from
	 * @param v   the V object
	 * 
	 * @return the int
	 */
	public int logined(String sid, String ip, V v) {

		if (v == null) {
			v = V.create();
		}

		// update
		set("logintimes", getInt("logintimes") + 1);

		Lock.removed(getId(), sid);

		/**
		 * cleanup the old sid for the old logined user
		 */
		dao.update(W.create().and("sid", sid), V.create("sid", X.EMPTY));

		return dao.inc(W.create().and(X.ID, getId()), "logintimes", 1,
				v.append("lastlogintime", Global.now()).append("ip", ip).append("locked", 0)
						.append("lockexpired", 0).append("sid", sid));

	}

	@Table(name = "gi_userrole", memo = "GI-用户角色")
	public static class UserRole extends Bean {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public static final BeanDAO<String, UserRole> dao = BeanDAO.create(UserRole.class);

		@Column(memo = "主键")
		String id;

		@Column(memo = "用户ID")
		long uid;

		@Column(memo = "角色ID")
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
	@Table(name = "gi_userlock", memo = "GI-用户登录失败")
	public static class Lock extends Bean {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public static final BeanDAO<String, Lock> dao = BeanDAO.create(Lock.class);

		@Column(memo = "主键")
		String id;

		@Column(memo = "用户ID")
		long uid;

		@Column(memo = "会话ID")
		String sid;

		@Column(memo = "主机")
		String host;

		@Column(memo = "浏览器")
		String useragent;

		/**
		 * Locked.
		 *
		 * @param uid       the uid
		 * @param sid       the sid
		 * @param host      the host
		 * @param useragent the useragent
		 * @return the int
		 */
		public static int locked(long uid, String sid, String host, String useragent) {

			return Lock.dao.insert(V.create("uid", uid).append(X.ID, UID.id(uid, sid, Global.now()))
					.append("sid", sid).append("host", host).append("useragent", useragent)
					.append(X.CREATED, Global.now()));
		}

		/**
		 * Removed.
		 *
		 * @param uid the uid
		 * @return the int
		 */
		public static int removed(long uid) {
			return Lock.dao.delete(W.create().and("uid", uid));
		}

		/**
		 * Removed.
		 *
		 * @param uid the uid
		 * @param sid the sid
		 * @return the int
		 */
		public static int removed(long uid, String sid) {
			return Lock.dao.delete(W.create().and("uid", uid).and("sid", sid));
		}

		/**
		 * Load.
		 *
		 * @param uid  the uid
		 * @param time the time
		 * @return the list
		 */
		public static List<Lock> load(long uid, long time) {
			Beans<Lock> bs = Lock.dao.load(W.create().and("uid", uid).and(X.CREATED, time, W.OP.gt).sort(X.CREATED, 1),
					0, Integer.MAX_VALUE);
			return bs;
		}

		/**
		 * Load by sid.
		 *
		 * @param uid  the uid
		 * @param time the time
		 * @param sid  the sid
		 * @return the list
		 */
		public static List<Lock> loadBySid(long uid, long time, String sid) {
			Beans<Lock> bs = Lock.dao.load(
					W.create().and("uid", uid).and(X.CREATED, time, W.OP.gt).and("sid", sid).sort(X.CREATED, 1), 0,
					Integer.MAX_VALUE);
			return bs;
		}

		/**
		 * delete all user lock info for the user id
		 * 
		 * @param uid the user id
		 * @return the number deleted
		 */
		public static int cleanup(long uid) {
			return Lock.dao.delete(W.create().and("uid", uid));
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
	 * @param id the id of the user
	 * @return int how many was deleted
	 */
	public static int delete(long id) {

		Lock.cleanup(id);

		return dao.delete(id);
	}

	public static User loadByToken(String token, long expired) {
		try {
			String s = new String(Digest.decode(Base32.decode(token), "giisoo"));
			String[] ss = X.split(s, "//");
			if (ss != null && ss.length == 2) {
				long id = X.toLong(ss[0]);
				long time = X.toLong(ss[1]);
				if (Global.now() - time < expired) {
					return dao.load(id);
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public String token() {
		try {
			String s = id + "//" + Global.now();
			return Base32.encode(Digest.encode(s.getBytes(), "giisoo"));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	private transient List<AuthToken> token_obj;

	public List<AuthToken> getTokens() {
		if (token_obj == null) {
			Beans<AuthToken> bs = AuthToken.load(this.getId());
			if (bs != null) {
				token_obj = bs;
			}
		}
		return token_obj;

	}

	private transient List<SID> sid_obj;

	public List<SID> getSid_obj() {
		if (sid_obj == null) {
			Beans<SID> bs = SID.dao.load(W.create().and("uid", id).sort("updated", -1), 0, 10);
			if (bs != null) {
				sid_obj = bs;
			}
		}
		return sid_obj;
	}

	private transient List<GLog> log_obj;

	public List<GLog> getLog_obj() {
		if (log_obj == null) {
			Beans<GLog> bs = GLog.dao.load(W.create().and("uid", id).sort("updated", -1), 0, 10);
			if (bs != null) {
				log_obj = bs;
			}
		}
		return log_obj;
	}

	/**
	 * check the database, if there is no "config.admin" user, then create the
	 * "admin" user, with "admin" as password
	 */
	public static void checkAndInit() {

		if (Helper.isConfigured()) {
			try {
				if (!dao.exists(0L)) {
					List<User> list = User.loadByAccess("access.config.admin");
					if (list == null || list.size() == 0) {
						try {
							String passwd = UID.random(16);
							User.create("root", V.create("id", 0L).append("name", "root").append("password", passwd)
									.append("nickname", "root"));

							File temp = new File(Temp.ROOT);
							if (!temp.exists()) {
								X.IO.mkdirs(temp);
							}
							PrintStream out = new PrintStream(Temp.ROOT + "/root.pwd");
							out.print(passwd);
							out.close();

							log.warn("checkAndInit, create [root], passwd in [" + Temp.ROOT + "/root.pwd" + "]");

						} catch (Exception e) {
							log.error(e.getMessage(), e);
						}
					} else {
						log.warn("checkAndInit, admin user [" + X.asList(list, e -> {
							User u1 = (User) e;
							return u1.name;
						}));
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	public static void to(JSON j) {
		int s = 0;
		W q = W.create().and(X.ID, 0, W.OP.gt).sort(X.ID, 1);

		List<JSON> l1 = new ArrayList<JSON>();
		Beans<User> bs = User.load(q, s, 100);
		while (bs != null && !bs.isEmpty()) {
			for (User e : bs) {
				l1.add(e.json());
			}
			s += bs.size();
			bs = User.load(q, s, 100);
		}

		j.append("users", l1);
	}

//	public static int from(JSON j) {
//		int total = 0;
//		Collection<JSON> l1 = j.getList("users");
//		if (l1 != null) {
//			for (JSON e : l1) {
//				long id = e.getLong(X.ID);
//				V v = V.fromJSON(e);
//				v.remove(X.ID, "_id");
//				User s = dao.load(W.create(X.ID, id));
//				if (s != null) {
//					dao.update(id, v);
//				} else {
//					dao.insert(v.append(X.ID, id));
//				}
//				total++;
//			}
//		}
//		return total;
//	}

	public static void repair() throws Exception {

		Beans<User> bs = User.dao.load(W.create().sort("created", 1), 0, 1000);
		if (bs != null) {
			for (User u : bs) {
				if (X.isEmpty(u.getPhoto())) {
					V v = V.create();
					v.append("name", u.getName());
					v.append("nickname", u.getNickname());

					_checkphoto(u.getId(), v);

					dao.update(u.getId(), v);
				}
			}
		}

	}

	public List<String> getAccesses() {
		if (role == null) {
			getRole();
		}

		if (role == null) {
			return new ArrayList<String>();
		}

		return role.getAccesses();

	}

	@Override
	public JSON json() {
		JSON j1 = super.json();
		j1.remove("_.*", "password", "md4passwd", "md5passwd", "sid", "passwd", "passwordtime", "createdua");

		if (this.expired()) {
			j1.append("passwordexpired", 1);
		} else {
			j1.append("passwordexpired", 0);
		}

		return j1;
	}

	public boolean isLocked(String host) {

		if (this.isLocked()) {
			return true;
		}

		if (Global.getInt("user.login.failed.lock", 1) == 0) {
			return false;
		}

		long times = Global.getLong("user.login.failed.times", 3);
		long time = Global.getLong("user.login.failed.lock.time", 1); // 小时
		if (time < 1) {
			time = 1;
		}
		String mode = Global.getString("user.login.failed.mode", "ip");

		W q = W.create().and("uid", id).and(X.CREATED, Global.now() - time * X.AHOUR, W.OP.gt);
		if (X.isSame(mode, "ip")) {
			q.and("host", host);
		}

		long n = Lock.dao.count(q);
		return (n >= times);
	}

	public long[] failed(String host) {

		if (Global.getInt("user.login.failed.lock", 1) == 0) {
			return null;
		}

		long times = Global.getLong("user.login.failed.times", 3);
		long time = Global.getLong("user.login.failed.lock.time", 1); // 小时
		if (time <= 0) {
			time = 1;
		}
		String mode = Global.getString("user.login.failed.mode", "ip");

		W q = W.create().and("uid", id).and("created", Global.now() - time * X.AHOUR, W.OP.gt);
		if (X.isSame(mode, "ip")) {
			q.and("host", host);
		}

		long n = Lock.dao.count(q);

//		log.warn("q=" + q + ", n=" + n + ", time=" + time);
		return new long[] { n, times };
	}

	public boolean expired() {
		int days = Global.getInt("user.passwd.expired", 90);
		if (days == -1) {
			return false;
		}

		return (Global.now() - passwordtime) > days * X.ADAY;
	}

	public boolean getLimitip() {
		return limitip == 1;
	}

	transient Unit unit_obj;

	public Unit getUnit_obj() {
		if (unit_obj == null && unitid > 0) {
			unit_obj = Unit.dao.load(unitid);
		}
		return unit_obj;
	}

}
