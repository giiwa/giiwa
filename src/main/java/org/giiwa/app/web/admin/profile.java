package org.giiwa.app.web.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.giiwa.core.bean.X;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.UID;
import org.giiwa.core.json.JSON;
import org.giiwa.core.noti.Email;
import org.giiwa.framework.bean.AuthToken;
import org.giiwa.framework.bean.Code;
import org.giiwa.framework.bean.GLog;
import org.giiwa.framework.bean.User;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;

public class profile extends Model {

	private static List<String> names = new ArrayList<String>();
	private static Map<String, Class<? extends profile>> settings = new HashMap<String, Class<? extends profile>>();

	final public static void register(int seq, String name, Class<? extends profile> m) {
		if (names.contains(name)) {
			log.error("duplicated name, name=" + name, new Exception("duplicated name=" + name));
			return;
		}

		if (seq < 0 || seq >= names.size()) {
			names.add(name);
		} else {
			names.add(seq, name);
		}
		settings.put(name, m);
	}

	final public static void register(String name, Class<? extends profile> m) {
		register(-1, name, m);
	}

	@Path(path = "get/(.*)", login = true)
	final public Object get(String name) {

		this.query.path("/admin/profile/get/" + name);

		Class<? extends profile> c = settings.get(name);
		log.debug("/get/" + c);
		if (c != null) {
			try {
				profile s = c.newInstance();
				s.copy(this);
				s.get();

				s.set("lang", lang);
				s.set("module", module);
				s.set("name", name);
				s.set("settings", names);
				s.set("me", this.getUser());
				s.show("/admin/profile.html");

			} catch (Exception e) {
				log.error(name, e);
				GLog.oplog.error(setting.class, "get", e.getMessage(), e, login, this.getRemoteHost());

				this.show("/admin/profile.html");
			}
		}

		return null;
	}

	/**
	 * Sets the.
	 *
	 * @param name
	 *            the name
	 */
	@Path(path = "set/(.*)", login = true, log = Model.METHOD_POST)
	final public void set(String name) {

		// this.query.path("/admin/profile/get/" + name);
		// this.set("query", this.query);

		Class<? extends profile> c = settings.get(name);
		log.debug("/set/" + c);
		if (c != null) {
			try {
				profile s = c.newInstance();
				s.copy(this);
				s.set();

				// s.set("lang", lang);
				// s.set("module", module);
				// s.set("name", name);
				// s.set("settings", names);
				// s.show("/admin/profile.html");
			} catch (Exception e) {
				log.error(name, e);
				GLog.oplog.error(setting.class, "set", e.getMessage(), e, login, this.getRemoteHost());

				this.response(JSON.create().append(X.STATE, 201).append(X.MESSAGE, e.getMessage()));
			}
		}
	}

	/**
	 * invoked when post setting form.
	 */
	public void set() {

	}

	public void settingPage(String view) {
		this.set("page", view);
	}

	/**
	 * invoked when get the setting form.
	 */
	public void get() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model#onGet()
	 */
	@Path(login = true)
	public final void onGet() {

		if (!names.isEmpty()) {
			String name = names.get(0);
			this.set("name", name);
			get(name);
			return;
		}

		this.println("not find page");

	}

	public static class my extends profile {

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.giiwa.app.web.admin.setting#set()
		 */
		@Override
		public void set() {

			try {
				String password = this.getString("password");
				if (!X.isEmpty(password)) {

					login.update(V.create("password", password));

					GLog.securitylog.info(profile.class, "passwd", lang.get("user.passwd.change"), login,
							this.getRemoteHost());

					this.response(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("save.success")));
					return;
				} else {
					V v = V.create();

					v.append("nickname", this.getString("nickname"));
					v.append("title", this.getString("title"));
					v.append("desktop", this.getString("desktop"));

					String email = this.getString("email1");
					if (!X.isEmpty(email)) {
						v.append("email", email);
					} else {
						v.append("email", this.getString("email"));
					}
					String phone = this.getString("phone1");
					if (!X.isEmpty(phone)) {
						v.append("phone", phone);
					} else {
						v.append("phone", this.getString("phone"));
					}

					login.update(v);

					login = User.dao.load(login.getId());
					AuthToken.delete(login.getId());
					this.setUser(login, LoginType.web);

					this.response(JSON.create().append(X.STATE, 201).append(X.MESSAGE, lang.get("save.success")));
					return;
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				this.response(JSON.create().append(X.STATE, 201).append(X.MESSAGE,
						lang.get("save.failed") + ":" + e.getMessage()));
				return;
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.giiwa.app.web.admin.setting#get()
		 */
		@Override
		public void get() {
			this.set("desks", dashboard.desks);
			this.settingPage("/admin/profile.my.html");
		}

	}

	@Path(path = "verify1", login = true)
	public void verify1() {
		String email = this.getString("email");
		if (!X.isEmpty(email)) {
			String code = UID.random(10);
			Code.create(email, code, V.create("expired", System.currentTimeMillis() + X.AMINUTE * 10));
			if (Email.send(lang.get("email.verify.subject"), code, email)) {
				this.response(JSON.create().append(X.STATE, 200).append(X.MESSAGE, "sent"));
				return;
			}
		}
		this.response(JSON.create().append(X.STATE, 201).append(X.MESSAGE, lang.get("validation.sent.error")));
	}

	@Path(path = "verify2", login = true)
	public void verify2() {
		String email = this.getString("email");
		String phone = this.getString("phone");
		String code = this.getString("code");
		if (!X.isEmpty(email)) {
			Code e = Code.load(email, code);
			if (e != null && e.getExpired() > System.currentTimeMillis()) {
				this.response(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("validation.ok")));
				return;
			}
		} else if (!X.isEmpty(phone)) {
			Code e = Code.load(phone, code);
			if (e != null && e.getExpired() > System.currentTimeMillis()) {
				this.response(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("validation.ok")));
				return;
			}
		}
		this.response(JSON.create().append(X.STATE, 201).append(X.MESSAGE, lang.get("validation.error")));
	}

}
