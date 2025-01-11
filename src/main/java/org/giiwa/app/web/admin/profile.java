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
package org.giiwa.app.web.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.giiwa.bean.AuthToken;
import org.giiwa.bean.Code;
import org.giiwa.bean.GLog;
import org.giiwa.bean.User;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.json.JSON;
import org.giiwa.misc.noti.Email;
import org.giiwa.web.Controller;
import org.giiwa.web.Path;

public class profile extends Controller {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

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

	@Path(path = "(.*)", login = true, oplog = true)
	public void get1(String name) {

		Class<? extends profile> c = settings.get(name);
		if (log.isDebugEnabled())
			log.debug("/get/" + c);

		if (c != null) {
			try {
				profile s = c.getDeclaredConstructor().newInstance();
				s.copy(this);
				s.get();

				s.set("lang", lang);
				s.set("module", module);
				s.set("__node", this.getString("__node"));
				s.set("name", name);
				s.set("settings", names);
				s.set("me", this.user());
				s.show("/admin/profile.html");

			} catch (Exception e) {
				log.error(name, e);
				GLog.oplog.error(this, path, e.getMessage(), e);

				this.error(e);
//				this.show("/admin/profile.html");
			}
		}
	}

	/**
	 * Sets the.
	 *
	 * @param name the name
	 */
	@Path(path = "set/(.*)", login = true, oplog = true)
	final public void set(String name) {

		// this.query.path("/admin/profile/get/" + name);
		// this.set("query", this.query);

		Class<? extends profile> c = settings.get(name);
		if (log.isDebugEnabled())
			log.debug("/set/" + c);

		if (c != null) {
			try {
				profile s = c.getDeclaredConstructor().newInstance();
				s.copy(this);
				s.set();

				// s.set("lang", lang);
				// s.set("module", module);
				// s.set("name", name);
				// s.set("settings", names);
				// s.show("/admin/profile.html");
			} catch (Exception e) {
				log.error(name, e);
				GLog.oplog.error(this, "set", e.getMessage(), e);

				this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE, e.getMessage()));
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
	 * @see org.giiwa.framework.web.Model.onGet()
	 */
	@Path(login = true)
	public final void onGet() {

		if (!names.isEmpty()) {
			String name = names.get(0);
			this.set("name", name);
			get(name);
			return;
		}

		this.print("not find page");

	}

	public static class my extends profile {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.giiwa.app.web.admin.setting.set()
		 */
		@Override
		public void set() {

			try {
				String password = this.getString("password");
				if (!X.isEmpty(password)) {

					login.update(V.create("password", password));

					GLog.securitylog.info(profile.class, "passwd", lang.get("user.passwd.change"), login, this.ip());

					this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("save.success")));
					return;

				} else {
					V v = V.create();

					v.append("nickname", this.getString("nickname"));
					v.append("title", this.getString("title"));
					v.append("company", this.getString("company"));
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
					this.user(login, LoginType.web);

					this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE, lang.get("save.success")));
					return;
				}

			} catch (Exception e) {

				log.error(e.getMessage(), e);

				this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE,
						lang.get("save.failed") + ", " + e.getMessage()));
				return;
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.giiwa.app.web.admin.setting.get()
		 */
		@Override
		public void get() {

//			log.warn("profile/my");

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
			try {
				if (Email.send(lang.get("email.verify.subject"), code, email)) {
					this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, "sent"));
					return;
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				GLog.applog.error(profile.class, "verify1", e.getMessage(), e, login, this.ip());
			}
		}
		this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE, lang.get("validation.sent.error")));
	}

	@Path(path = "verify2", login = true)
	public void verify2() {
		String email = this.getString("email");
		String phone = this.getString("phone");
		String code = this.getString("code");
		if (!X.isEmpty(email)) {
			Code e = Code.load(email, code);
			if (e != null && e.getExpired() > System.currentTimeMillis()) {
				this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("validation.ok")));
				return;
			}
		} else if (!X.isEmpty(phone)) {
			Code e = Code.load(phone, code);
			if (e != null && e.getExpired() > System.currentTimeMillis()) {
				this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("validation.ok")));
				return;
			}
		}
		this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE, lang.get("validation.error")));
	}

}
