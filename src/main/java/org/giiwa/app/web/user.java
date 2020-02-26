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
package org.giiwa.app.web;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.giiwa.bean.App;
import org.giiwa.bean.AuthToken;
import org.giiwa.bean.Code;
import org.giiwa.bean.GLog;
import org.giiwa.bean.Role;
import org.giiwa.bean.Session;
import org.giiwa.bean.User;
import org.giiwa.bean.UserConfig;
import org.giiwa.conf.Global;
import org.giiwa.dao.Beans;
import org.giiwa.dao.Helper;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.misc.Captcha;
import org.giiwa.misc.noti.Email;
import org.giiwa.web.Controller;
import org.giiwa.web.Path;
import org.giiwa.web.view.View;

/**
 * web api： /user <br>
 * used to login or logout, etc.
 * 
 * @author joe
 * 
 */
public class user extends Controller {

	/**
	 * Index.
	 */
	@Path()
	public void onGet() {
		if (!Helper.isConfigured()) {
			this.redirect("/admin/setup");
		} else if (login == null) {
			this.redirect("/user/login");
		} else if (login.hasAccess("access.config.admin")) {
			this.redirect("/admin");
		} else {
			this.redirect("/");
		}
	}

	/**
	 * 3rd redirect user/app
	 */
	@Path(path = "app")
	public void app() {
		String appid = this.getString("appid");
		String data = this.getString("data");
		if (X.isEmpty(appid) || X.isEmpty(data)) {
			this.print("bad appid or data");
			return;
		}
		App a = App.dao.load(W.create("appid", appid));
		if (a == null) {
			this.print("bad appid");
			return;
		}

		JSON jo = JSON.fromObject(App.decode(data, a.getSecret()));
		if (jo != null) {
			long time = jo.getLong("time");
			String name = jo.getString("name");
			if (System.currentTimeMillis() - time < X.AMINUTE) {
				User u = User.load(name);
				if (u != null) {
					this.setUser(u);
					this.redirect("/");
					return;
				} else {
					this.print("bad name in data");
				}
			} else {
				this.print("bad time in data, please check time/clock");
			}
		} else {
			this.print("bad data");
		}
	}

	/**
	 * Register.
	 */
	@Path(path = "register")
	public void register() {
		if ("close".equals(Global.getString("user.system", "close"))) {
			this.redirect("/user/login");
			return;
		}

		if (method.isPost()) {

			Captcha.Result r1 = Captcha.Result.ok;

			if (Global.getInt("user.captcha", 0) == 1) {
				String code = this.getString("code");
				if (code != null) {
					code = code.toLowerCase();
				}
				r1 = Captcha.verify(this.sid(), code);
				Captcha.remove(this.sid());
			}

			if (Captcha.Result.badcode == r1 || Captcha.Result.expired == r1) {
				this.set(X.MESSAGE, lang.get("captcha.bad"));
			} else {

				String name = this.getString("name").trim().toLowerCase();

				try {
					V v = V.create("name", name).copy(this, "password", "nickname", "email", "phone");
					long id = User.create(name, v);

					String role = Global.getString("user.role", "N/A");
					Role r = Role.loadByName(role);
					User u = User.dao.load(id);
					if (r != null) {
						u.setRole(r.getId());
					}
					this.setUser(u, LoginType.web);
					GLog.securitylog.info(user.class, "register",
							lang.get("create.success") + ":" + name + ", uid=" + id, login, this.getRemoteHost());

					if (this.isAjax()) {
						this.response(JSON.create().append(X.STATE, 200).append("id", u.getId()).append(X.MESSAGE,
								lang.get("create.success")));
						return;
					} else {
						Session s = this.getSession(true);
						if (s.has("uri")) {
							this.redirect((String) s.get("uri"));
							return;
						} else {
							this.redirect("/user/go");
						}
					}
				} catch (Exception e) {
					log.error(e.getMessage(), e);
					GLog.securitylog.error(user.class, "register", e.getMessage(), e, login, this.getRemoteHost());

					if (this.isAjax()) {
						this.response(JSON.create().append(X.STATE, 201).append(X.MESSAGE, e.getMessage()));
						return;
					} else {
						this.set(X.MESSAGE, e.getMessage());
						this.set(this);
					}
				}
			}
		}

		this.show("/user/user.register.html");

	}

	/**
	 * redirect the web to right site
	 * 
	 * @return true, if successful
	 */
	@Path(path = "go", login = true)
	public boolean go() {

		Session s = this.getSession(false);
		if (s.has("uri")) {
			String uri = (String) s.get("uri");

			if (log.isDebugEnabled())
				log.debug("redirecting:" + uri);

			if (uri.endsWith("/index")) {
				uri = uri.substring(0, uri.length() - 6);
			}

			if (X.isEmpty(uri)) {
				this.redirect("/");
			} else {
				this.redirect(uri);
			}
			s.remove("uri").store();

			return true;
		}

		this.redirect("/");

		/**
		 * default, return false for "inherit" module to re-write it;
		 */
		return false;

	}

	@Path(login = true, path = "set")
	public void set() {

		try {
			List<String> ss = this.getNames();
			if (ss != null && !ss.isEmpty()) {
				for (String s1 : ss) {
					String content = this.getHtml(s1);
					if (content.length() < 4096) {
						UserConfig.set(login.getId(), s1, content);
					}
				}
			}

			this.response(JSON.create().append(X.STATE, 200).append(X.MESSAGE, "ok").append("MAXSIZE", 4096));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			this.response(JSON.create().append(X.STATE, 201).append(X.MESSAGE, e.getMessage()).append("MAXSIZE", 4096));
		}
	}

	@Path(path = "info")
	public void info() {
		if (this.getUser() != null) {
			User u = User.dao.load(login.getId());
			if (u != null) {
				this.response(JSON.create().append(X.STATE, 200).append("data", u.json()));
				return;
			}
		}

		this.response(JSON.create().append(X.STATE, 401).append(X.MESSAGE, "login required"));
	}

	@Path(login = true, path = "get")
	public void get() {

		JSON jo = JSON.create();
		String names = this.getString("names");
		if (names != null) {
			String[] ss = X.split(names, "[,;]");
			if (ss != null && ss.length > 0) {
				for (String s1 : ss) {
					String v = UserConfig.get(login.getId(), s1);
					jo.put(s1, v);
				}
			}
		}

		this.response(jo.append(X.STATE, 200).append(X.MESSAGE, "ok"));

	}

	/**
	 * Login.
	 */
	@Path(path = "login")
	public void login() {

		if (new File(Controller.GIIWA_HOME + "/root.pwd").exists()) {
			this.set(X.MESSAGE, lang.get("root.pwd"));
		}

		if (method.isPost()) {

			JSON jo = new JSON();
			AuthToken a = null;
			if (Global.getInt("user.token", 0) == 1) {
				String token = this.getString("token");
				String sid = this.getString("sid");
				a = AuthToken.load(sid, token);
			}

			if (a != null) {
				// ok, logined
				jo.put(X.STATE, 200);
				jo.put(X.MESSAGE, "ok");
				jo.put("uid", a.getUid());
				jo.put("expired", a.getExpired());
				User u = a.getUser_obj();
				this.setUser(u, LoginType.ajax);

				GLog.securitylog.info(user.class, "login", null, u, this.getRemoteHost());
			} else {
				String name = this.getString("name");
				if (name != null) {
					name = name.toLowerCase();
				}
				String pwd = this.getString("pwd");

				Captcha.Result r = Captcha.Result.ok;

				if (Global.getInt("user.captcha", 0) == 1) {
					String code = this.getString("code");
					if (code != null) {
						code = code.toLowerCase();
					}
					r = Captcha.verify(this.sid(), code);
					Captcha.remove(this.sid());
				}

				if (Captcha.Result.badcode == r) {
					jo.put(X.MESSAGE, lang.get("captcha.bad"));
					jo.put(X.STATE, 202);

					GLog.securitylog.error(user.class, "login", lang.get("captcha.bad"), null, this.getRemoteHost());

				} else if (Captcha.Result.expired == r) {
					jo.put(X.MESSAGE, lang.get("captcha.expired"));
					jo.put(X.STATE, 203);

					GLog.securitylog.error(user.class, "login", lang.get("captcha.expired"), null,
							this.getRemoteHost());

				} else {

					User me = User.load(name, pwd);

					log.info("login: " + sid() + "-" + me);

					if (me != null) {

						long uid = me.getId();
						long time = System.currentTimeMillis() - X.AHOUR;
						List<User.Lock> list = User.Lock.loadByHost(uid, time, this.getRemoteHost());

						if (me.isLocked() || (list != null && list.size() >= 6)) {
							// locked by the host
							me.failed(this.getRemoteHost(), sid(), this.browser());
							jo.put(X.MESSAGE, lang.get("account.locked.error"));

							jo.put(X.STATE, 204);
							jo.put("name", name);
							jo.put("pwd", pwd);

							GLog.securitylog.error(user.class, "login", lang.get("account.locked.error"), me,
									this.getRemoteHost());

						} else {
							list = User.Lock.loadBySid(uid, time, sid());
							if (list != null && list.size() >= 3) {
								me.failed(this.getRemoteHost(), sid(), this.browser());

								jo.put(X.MESSAGE, lang.get("account.locked.error"));
								jo.put("name", name);
								jo.put("pwd", pwd);
								jo.put(X.STATE, 204);

								GLog.securitylog.error(user.class, "login", lang.get("account.locked.error"), me,
										this.getRemoteHost());
							} else {

								GLog.securitylog.info(user.class, "login", null, me, this.getRemoteHost());

								if (X.isSame("json", this.getString("type")) || this.isAjax()) {

									this.setUser(me, LoginType.ajax);

									if (log.isDebugEnabled())
										log.debug("isAjax login");

									login.logined(sid(), this.getRemoteHost(),
											V.create("ajaxlogined", System.currentTimeMillis()));

									jo.put("sid", sid());
									jo.put("uid", me.getId());

									/**
									 * test the configuration is enabled user token and this request is ajax
									 */
									if (Global.getInt("user.token", 1) == 1) {
										AuthToken t = AuthToken.update(me.getId(), sid(), this.getRemoteHost());
										if (t != null) {
											jo.put("token", t.getToken());
											jo.put("expired", t.getExpired());
											jo.put(X.STATE, HttpServletResponse.SC_OK);
										} else {
											jo.put(X.MESSAGE, "create authtoken error");
											jo.put(X.STATE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
										}
									}
									this.response(jo);
								} else {

									this.setUser(me, LoginType.web);

									/**
									 * logined, to update the stat data
									 */
									me.logined(sid(), this.getRemoteHost(),
											V.create("weblogined", System.currentTimeMillis()));

									this.redirect("/");
//									this.redirect("/user/go");
								}
								return;
							}
						}

					} else {

						jo.append(X.STATE, 200).append(X.MESSAGE, "ok");

						User u = User.load(name);
						if (u == null) {
							jo.put("message", lang.get("login.name_password.error"));
							jo.put(X.STATE, 201);

							GLog.securitylog.error(user.class, "login",
									lang.get("login.name_password.error") + ":" + name, u, this.getRemoteHost());
						} else {

							u.failed(this.getRemoteHost(), sid(), this.browser());

							List<User.Lock> list = User.Lock.loadByHost(u.getId(), System.currentTimeMillis() - X.AHOUR,
									this.getRemoteHost());

							if (list != null && list.size() >= 6) {
								jo.put("message", lang.get("login.locked.error"));
								jo.put(X.STATE, 204);

								GLog.securitylog.error(user.class, "login", lang.get("login.failed") + ":" + name, u,
										this.getRemoteHost());

							} else {
								list = User.Lock.loadBySid(u.getId(), System.currentTimeMillis() - X.AHOUR, sid());
								if (list != null && list.size() >= 3) {
									jo.put("message", lang.get("login.locked.error"));
									jo.put(X.STATE, 204);

									GLog.securitylog.error(user.class, "login",
											lang.get("login.locked.error") + ":" + name, u, this.getRemoteHost());

								} else {
									jo.put(X.MESSAGE, String.format(lang.get("login.name_password.error.times"),
											list == null ? 0 : list.size()));
									jo.put(X.STATE, 204);

									GLog.securitylog.warn(user.class, "login", jo.getString(X.MESSAGE) + ":" + name, u,
											this.getRemoteHost());

								}
							}
						}

						jo.put("name", name);
						jo.put("pwd", pwd);
					}
				}
			}

			if (X.isSame("json", this.getString("type")) || this.isAjax()) {
				this.response(jo);
				return;
			} else if (login != null) {
				this.redirect("/");
//				this.redirect("/user/go");
			} else {
				this.set(this);
				this.set(X.STATE, jo.getInt(X.STATE));
				this.set(X.MESSAGE, jo.getString(X.MESSAGE));
			}
		} else {
			String sid = this.getString("sid");
			if (!X.isEmpty(sid)) {
				AuthToken.dao.delete(W.create("sid", sid));
			}
		}

		String refer = this.getString("refer");
		if (!X.isEmpty(refer) && !isAjax()) {
			try {
				this.getSession(true).set("uri", URLDecoder.decode(refer, "UTF-8")).store();
			} catch (Exception e) {
				log.error(refer, e);
				GLog.securitylog.error(user.class, "login", e.getMessage(), e, login, this.getRemoteHost());
			}
		}

		show("/user/user.login.html");
	}

	/**
	 * Logout.
	 */
	@Path(path = "logout")
	public void logout() {

		User u = this.getUser();

		if (u != null) {

			/**
			 * clear auth-token
			 */
			AuthToken.delete(u.getId(), sid());

			GLog.securitylog.info(user.class, "logout", null, u, this.getRemoteHost());

			login.logout();

			/**
			 * clear the user in session, but still keep the session
			 */
			setUser(null, null);

		}

		/**
		 * redirect to home
		 */
		if (isAjax() || method.isPost()) {
			JSON jo = JSON.create();
			jo.put(X.STATE, 200);
			jo.put(X.MESSAGE, "ok");
			jo.put("path", "/");
			// this.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
			this.setHeader("Location", "/");
			this.response(jo);
			return;
		} else {
			this.redirect("/");
			return;
		}

	}

	/**
	 * Verify.
	 */
	@Path(path = "verify", access = "access.config.user.query")
	public void verify() {
		String name = this.getString("name").trim().toLowerCase();
		String value = this.getString("value").trim().toLowerCase();

		JSON jo = new JSON();
		if ("name".equals(name)) {
			try {
				if (User.dao.exists(W.create("name", value))) {

					jo.put(X.STATE, 201);
					jo.put(X.MESSAGE, lang.get("user.name.exists"));

					GLog.securitylog.info(user.class, "verify", "name=" + name + ",value=" + value + ",exists", login,
							this.getRemoteHost());

				} else {
					String rule = Global.getString("user.name.rule", "^[a-zA-Z0-9]{4,16}$");

					if (X.isEmpty(value) || !value.matches(rule)) {
						jo.put(X.STATE, 201);
						jo.put(X.MESSAGE, lang.get("user.name.format.error"));

						GLog.securitylog.info(user.class, "verify",
								"name=" + name + ",value=" + value + ",rule=" + rule, login, this.getRemoteHost());
					} else {
						jo.put(X.STATE, 200);
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				jo.append(X.STATE, 500).append(X.MESSAGE, "internal error");
			}
		} else if ("password".equals(name)) {
			String rule = Global.getString("user.passwd.rule", "^[a-zA-Z0-9]{6,16}$");
			if (X.isEmpty(value) || !value.matches(rule)) {
				jo.put(X.STATE, 201);
				jo.put(X.MESSAGE, lang.get("user.passwd.format.error"));

				GLog.securitylog.info(user.class, "verify", "name=" + name + ",value=" + value + ",rule=" + rule, login,
						this.getRemoteHost());
			} else {
				jo.put(X.STATE, 200);
			}
		}

		this.response(jo);
	}

	/**
	 * get user list by "access" token
	 */
	@Path(path = "popup2", login = true, access = "access.config.user.query")
	public void popup2() {
		String access = this.getString("access");
		List<User> list = null;
		if (!X.isEmpty(access)) {
			list = User.loadByAccess(access);
		} else {
			Beans<User> bs = User.load(W.create().and(X.ID, 0, W.OP.gt), 0, 1000);
			if (bs != null) {
				list = bs;
			}
		}

		JSON jo = new JSON();
		if (list != null && list.size() > 0) {
			List<JSON> arr = new ArrayList<JSON>();
			for (User e : list) {
				JSON j = new JSON();
				j.put("value", e.getId());
				j.put("name", e.get("nickname") + "(" + e.get("name") + ")");
				arr.add(j);
			}
			jo.put("list", arr);
			jo.put(X.STATE, 200);

		} else {
			jo.put(X.STATE, 201);
		}

		this.response(jo);

	}

	/**
	 * Dashboard.
	 */
	@Path(path = "dashboard", login = true)
	public void dashboard() {

		this.show("/user/user.dashboard.html");

	}

	/**
	 * Forget password
	 */
	@Path(path = "forget")
	public void forget() {

		if (method.isPost()) {
			JSON jo = JSON.create();

			String email = this.getString("email");
			String phone = this.getString("phone");
			int phase = this.getInt("phase");

			if (!X.isEmpty(email)) {
				if (phase == 0) {
					// verify email and send a code
					Code c = Code.dao.load(W.create("s2", email).sort(X.CREATED, -1));
					if (c != null && c.getUpdated() < X.AMINUTE) {

						jo.put(X.MESSAGE, lang.get("user.forget.email.sent"));
						jo.put(X.STATE, HttpServletResponse.SC_OK);

					} else {
						int s = 0;

						StringBuilder sb = new StringBuilder();
						W q = W.create("email", email);
						Beans<User> bs = User.load(q, s, 10);
						while (bs != null && !bs.isEmpty()) {
							for (User u : bs) {
								if (!u.isDeleted()) {
									if (sb.length() > 0) {
										sb.append(",");
									}
									sb.append(u.getName());
								}
							}
							s += bs.size();
							bs = User.load(q, s, 10);
						}

						if (sb.length() > 0) {

							String code = null;

							if (c == null || c.getExpired() < System.currentTimeMillis()) {
								code = UID.random(10);
								Code.create(code, email, V.create("expired", System.currentTimeMillis() + X.ADAY));
							} else {
								code = c.getString("s1");
							}

							File f = module.getFile("/user/email.validation." + lang.getLocale() + ".template");
							if (f != null) {
								JSON j1 = JSON.create();
								j1.put("email", email);
								j1.put("account", sb.toString());
								j1.put("code", code);

								View v1 = View.getVelocity();
								String body = v1.parse(f, j1);
								if (body != null) {
									try {
										if (Email.send(lang.get("mail.validation.code"), body, email)) {
											jo.put(X.MESSAGE, lang.get("user.forget.email.sent"));
											jo.put(X.STATE, HttpServletResponse.SC_OK);
											Code.dao.update(W.create("s1", code).and("s2", email),
													V.create(X.UPDATED, System.currentTimeMillis()));

										} else {
											jo.put(X.MESSAGE, lang.get("user.forget.email.sent.failed"));
											jo.put(X.STATE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
										}
									} catch (Exception e) {
										log.error(e.getMessage(), e);
										GLog.applog.error(user.class, "forget", e.getMessage(), e, login,
												this.getRemoteHost());
										jo.put(X.MESSAGE,
												lang.get("user.forget.email.sent.failed") + ": " + e.getMessage());
										jo.put(X.STATE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
									}
								} else {
									jo.put(X.STATE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
									jo.put(X.MESSAGE, lang.get("user.forget.template.error"));
								}
							} else {
								jo.put(X.STATE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
								jo.put(X.MESSAGE, lang.get("user.forget.template.notfound"));
							}
						} else {
							jo.put(X.STATE, HttpServletResponse.SC_BAD_REQUEST);
							jo.put(X.MESSAGE, lang.get("user.forget.noaccount"));
						}
					}
				} else if (phase == 1) {
					// verify code
					String code = this.getString("code");
					Code c = Code.load(code, email);
					if (c == null) {
						jo.put(X.STATE, HttpServletResponse.SC_BAD_REQUEST);
						jo.put(X.MESSAGE, lang.get("email.code.bad"));
					} else if (c.getExpired() < System.currentTimeMillis()) {
						jo.put(X.STATE, HttpServletResponse.SC_BAD_REQUEST);
						jo.put(X.MESSAGE, lang.get("email.code.expired"));
					} else {
						Code.delete(code, email);

						String passwd = this.getString("passwd");
						String rule = Global.getString("user.passwd.rule", "^[a-zA-Z0-9]{6,16}$");
						if (!X.isEmpty(rule) && !passwd.matches(rule)) {
							jo.put(X.STATE, HttpServletResponse.SC_BAD_REQUEST);
							jo.put(X.MESSAGE, "user.passwd.format.error");
						} else {
							try {
								User.update(W.create("email", email), V.create("password", passwd));
								jo.put(X.STATE, HttpServletResponse.SC_OK);
								jo.put(X.MESSAGE, lang.get("user.passwd.updated"));
							} catch (Exception e) {
								jo.put(X.STATE, HttpServletResponse.SC_BAD_REQUEST);
								jo.put(X.MESSAGE, lang.get("save.failed") + ":" + e.getMessage());
							}
						}

					}
				}
			} else if (!X.isEmpty(phone)) {

				if (phase == 0) {
					// verify email and send a code
					Code c = Code.dao.load(W.create("s2", phone).sort(X.CREATED, -1));
					if (c != null && c.getUpdated() < X.AMINUTE) {

						jo.put(X.MESSAGE, lang.get("user.forget.phone.sent"));
						jo.put(X.STATE, HttpServletResponse.SC_OK);

					} else {
						int s = 0;

						StringBuilder sb = new StringBuilder();
						W q = W.create("phone", phone);
						Beans<User> bs = User.load(q, s, 10);
						while (bs != null && !bs.isEmpty()) {
							for (User u : bs) {
								if (!u.isDeleted()) {
									if (sb.length() > 0) {
										sb.append(",");
									}
									sb.append(u.getName());
								}
							}
							s += bs.size();
							bs = User.load(q, s, 10);
						}

						if (sb.length() > 0) {

							String code = null;

							if (c == null || c.getExpired() < System.currentTimeMillis()) {
								code = UID.digital(4);
								Code.create(code, phone,
										V.create("expired", System.currentTimeMillis() + X.AMINUTE * 6));
							} else {
								code = c.getString("s1");
							}

							JSON j1 = JSON.create();
							j1.put("phone", phone);
							j1.put("account", sb.toString());
							j1.put("code", code);

							// if (Sms.send(phone, "user.forget.password", j1)) {
							// jo.put(X.MESSAGE, lang.get("user.forget.phone.sent"));
							// jo.put(X.STATE, HttpServletResponse.SC_OK);
							// Code.dao.update(W.create("s1", code).and("s2", phone),
							// V.create(X.UPDATED, System.currentTimeMillis()));
							//
							// } else {
							// jo.put(X.MESSAGE, lang.get("user.forget.phone.sent.failed"));
							// jo.put(X.STATE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
							//
							// }

						} else {
							jo.put(X.STATE, HttpServletResponse.SC_BAD_REQUEST);
							jo.put(X.MESSAGE, lang.get("user.forget.noaccount"));
						}
					}
				} else if (phase == 1) {
					// verify code
					String code = this.getString("code");
					Code c = Code.load(code, phone);
					if (c == null) {
						jo.put(X.STATE, HttpServletResponse.SC_BAD_REQUEST);
						jo.put(X.MESSAGE, lang.get("phone.code.bad"));
					} else if (c.getExpired() < System.currentTimeMillis()) {
						jo.put(X.STATE, HttpServletResponse.SC_BAD_REQUEST);
						jo.put(X.MESSAGE, lang.get("phone.code.expired"));
					} else {
						Code.delete(code, phone);
						jo.put(X.STATE, HttpServletResponse.SC_OK);
						jo.put(X.MESSAGE, lang.get("phone.code.ok"));
					}
				} else if (phase == 2) {
					// change the password
					String passwd = this.getString("passwd");
					String rule = Global.getString("user.passwd.rule", "^[a-zA-Z0-9]{6,16}$");
					if (!X.isEmpty(rule) && !passwd.matches(rule)) {
						jo.put(X.STATE, HttpServletResponse.SC_BAD_REQUEST);
						jo.put(X.MESSAGE, "user.passwd.format.error");
					} else {
						try {
							User.update(W.create("phone", phone), V.create("password", passwd));
							jo.put(X.STATE, HttpServletResponse.SC_OK);
							jo.put(X.MESSAGE, lang.get("user.passwd.updated"));
						} catch (Exception e) {
							jo.put(X.STATE, HttpServletResponse.SC_BAD_REQUEST);
							jo.put(X.MESSAGE, lang.get("save.failed") + ":" + e.getMessage());
						}
					}
				}

			} else {
				jo.put(X.STATE, HttpServletResponse.SC_BAD_REQUEST);
				jo.put(X.MESSAGE, lang.get("param.error"));
			}

			this.response(jo);
			return;
		}

		this.show("/user/user.forget.html");

	}

	// public static void main(String[] args) {
	// // String rule = "^[a-zA-Z0-9]{4,16}$";
	// // String name = "joej";
	// // System.out.println(name.matches(rule));
	//
	// try {
	// StringBuilder sb = new StringBuilder();
	// sb.append("var a = 1 + 1;");
	// sb.append("a");
	// Object o = JS.run(sb.toString());
	//
	// System.out.println(o);
	// } catch (Exception e) {
	// log.error(e.getMessage(), e);
	// }
	// }

	@Path(path = "access", login = true)
	public void access() {
		this.response(JSON.create().append(X.STATE, 200).append(X.MESSAGE, "ok").append("list",
				login.getRole().getAccesses()));
	}

}
