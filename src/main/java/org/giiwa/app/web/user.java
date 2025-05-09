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
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.giiwa.bean.App;
import org.giiwa.bean.AuthToken;
import org.giiwa.bean.Code;
import org.giiwa.bean.GLog;
import org.giiwa.bean.Role;
import org.giiwa.bean.Roles;
import org.giiwa.bean.Session;
import org.giiwa.bean.Temp;
import org.giiwa.bean.User;
import org.giiwa.bean.UserConfig;
import org.giiwa.cache.Cache;
import org.giiwa.conf.Global;
import org.giiwa.dao.Beans;
import org.giiwa.dao.Helper;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.misc.Base32;
import org.giiwa.misc.Captcha;
import org.giiwa.misc.Url;
import org.giiwa.misc.noti.Email;
import org.giiwa.web.Controller;
import org.giiwa.web.Path;
import org.giiwa.web.view.View;

import jakarta.servlet.http.HttpServletResponse;

/**
 * web api： /user <br>
 * used to login or logout, etc.
 * 
 * @author joe
 * 
 */
public class user extends Controller {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8249276425428015555L;

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
		App a = App.dao.load(W.create().and("appid", appid));
		if (a == null) {
			this.print("bad appid");
			return;
		}

		JSON jo = JSON.fromObject(App.decode(data, a.getSecret()));
		if (jo != null) {
			long time = jo.getLong("time");
			String name = jo.getString("name");
			if (Global.now() - time < X.AMINUTE) {
				User u = User.load(name);
				if (u != null) {
					this.user(u);

					Roles rs = u.getRole();
					for (Role r1 : rs.getList()) {
						if (!X.isEmpty(r1.url)) {
							this.redirect(r1.url);
							return;
						}
					}

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
				r1 = Captcha.verify(this.sid(true), code);
				Captcha.remove(this.sid(true));
			}

			if (Captcha.Result.badcode == r1 || Captcha.Result.expired == r1) {
				this.set(X.MESSAGE, lang.get("captcha.bad"));
			} else {

				String name = this.getString("name").trim().toLowerCase();

				try {
					V v = V.create("name", name).copy(this, "nickname", "email", "phone");

					String pwd = this.get("password");
					if (X.isEmpty(pwd)) {
						pwd = this.get("pwd1");
						if (!X.isEmpty(pwd)) {
							// decode
							pwd = new String(Base64.getDecoder().decode(pwd));
						}
					}
					v.append("password", pwd);

					long id = User.create(name, v);

					String role = Global.getString("user.role", "N/A");
					Role r = Role.loadByName(role);
					User u = User.dao.load(id);
					if (r != null) {
						u.setRole(r.getId());
					}
					this.user(u, LoginType.web);
					GLog.securitylog.info("user", "register", lang.get("create.success") + ":" + name + ", uid=" + id,
							login, this.ip());

					if (this.isAjax()) {
						this.send(JSON.create().append(X.STATE, 200).append("id", u.getId()).append(X.MESSAGE,
								lang.get("create.success")));
						return;
					} else {
						Session s = this.session(true);
						if (s.has("uri")) {
							this.redirect((String) s.get("uri"));
							return;
						} else {
							this.redirect("/user/go");
						}
					}
				} catch (Exception e) {
					log.error(e.getMessage(), e);
					GLog.securitylog.error("user", "register", e.getMessage(), e, login, this.ip());

					if (this.isAjax()) {
						this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE, e.getMessage()));
						return;
					} else {
						this.set(X.MESSAGE, e.getMessage());
						this.copy(this.json());
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

		Session s = this.session(false);
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

			List<String> ss = this.names();
			if (ss != null && !ss.isEmpty()) {

				String sid = this.sid();
				long uid = login.id;

				this.set("ss", ss);

				for (String s1 : ss) {
					String content = this.getHtml(s1);
					if (content == null || content.length() < 4096) {
						UserConfig.set(uid, sid, s1, content);
						this.set(s1, content);
					}
				}
			}

			this.set("MAXSIZE", 4096).send(200);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			this.set(X.ERROR, e.getMessage()).set("MAXSIZE", 4096).send(201);
		}
	}

	@Path(path = "info")
	public void info() {
		if (this.user() != null) {
			User u = User.dao.load(login.getId());
			if (u != null) {
				this.send(JSON.create().append(X.STATE, 200).append("data", u.json()));
				return;
			}
		}

		this.send(JSON.create().append(X.STATE, 401).append(X.MESSAGE, "login required"));
	}

	@Path(login = true, path = "get")
	public void get() {

		JSON jo = JSON.create();
		String names = this.getString("names");
		if (names != null) {

			String sid = this.sid();
			long uid = login.id;

			String[] ss = X.split(names, "[,;]");
			if (ss != null && ss.length > 0) {
				for (String s1 : ss) {
					String v = UserConfig.get(uid, sid, s1);
					jo.put(s1, v);
				}
			}
		}

		this.send(jo.append(X.STATE, 200));

	}

	@Path(path = "sso")
	public void sso() {

//		Global.setConfig("user.login.sso", X.isSame("on", this.getString("user.login.sso")) ? 1 : 0);
//		Global.setConfig("user.login.sso.mode", this.getString("user.login.sso.mode"));
//		Global.setConfig("user.login.sso.role", this.getString("user.login.sso.role"));
//		Global.setConfig("user.login.sso.expired", this.getLong("user.login.sso.expired"));

		if (Global.getInt("user.login.sso", 0) == 0) {

			GLog.securitylog.warn("user", "sso", "failed as disabled", null, this.ip());

			this.print("disabled");
			return;
		}

		String s = this.get("_s");
		if (X.isEmpty(s)) {
			this.print("[_s] missed");
			return;
		}
		s = new String(Base32.decode(s));
		JSON j0 = JSON.fromObject(s);

		String mode = Global.getString("user.login.sso.mode", X.EMPTY);
		if (X.isIn(mode, "appid")) {
			String appid = j0.getString("appid");
			if (X.isEmpty(appid)) {

				GLog.securitylog.warn("user", "sso", "failed as appid missed", null, this.ip());

				this.print("[appid] missed!");
				return;
			}

			App a = App.load(appid);
			if (a == null) {

				GLog.securitylog.warn("user", "sso", "failed as appid error", null, this.ip());

				this.print("[appid] error!");
				return;
			}

			String token = j0.getString("token");
			if (X.isEmpty(token)) {

				GLog.securitylog.warn("user", "sso", "failed as token missed", null, this.ip());

				this.print("[token] missed!");
				return;
			}

			token = token.replaceAll(" ", "+");

			String token1 = App.decode(token, a.secret);
			if (X.isEmpty(token1)) {

				GLog.securitylog.warn("user", "sso", "failed as token error", null, this.ip());

				this.print("[token] error! ");
				return;
			}

			JSON j1 = JSON.fromObject(token1);
			if (j1 == null || j1.isEmpty()) {

				GLog.securitylog.warn("user", "sso", "failed as token no json", null, this.ip());

				this.print("[token] is not json!");
				return;
			}

			if (!j1.containsKey("time")) {

				GLog.securitylog.warn("user", "sso", "failed as time missed in token", null, this.ip());

				this.print("[time] missed in token");
			}

			long time = j1.getLong("time");
			long expired = Global.getLong("user.login.sso.expired", 60);
			if (Global.now() - time > expired * 60 * X.AMINUTE) {

				GLog.securitylog.warn("user", "sso", "failed as exired", null, this.ip());

				this.print("expired");
				return;
			}

			String name = j1.getString("name");
			if (X.isEmpty(name)) {

				GLog.securitylog.warn("user", "sso", "failed as name missed in token", null, this.ip());

				this.print("[name] missed in token");
				return;
			}

			String url0 = j0.getString("url");
			String url = Url.decode(url0);

			User u = User.load(name);
			if (u != null) {
				this.user(u);

				if (!X.isEmpty(url)) {

					GLog.securitylog.warn("user", "sso", "redirect [" + url + "], url0=" + url0, u, this.ip());

					this.redirect(url);
					return;
				}

				Roles rs = u.getRole();
				if (rs != null && rs.getList() != null) {
					for (Role r1 : rs.getList()) {
						if (!X.isEmpty(r1.url)) {

							GLog.securitylog.warn("user", "sso", "redirect [" + r1.url + "]", u, this.ip());

							this.redirect(r1.url);
							return;
						}
					}
				}

				GLog.securitylog.warn("user", "sso", "redirect [/]", u, this.ip());

				this.redirect("/");
				return;
			}

			V v = V.create();
			v.append("name", name);
			v.append("nickname", j1.getString("nickname", name));
			v.append("createdua", "sso");
			v.append("password", UID.random(12));

			try {
				long id = User.create(v);

				GLog.securitylog.warn("sso", "createuser", "name=" + name, null, this.ip());

				u = User.dao.load(id);
				this.user(u);

				String role = Global.getString("user.login.sso.role", X.EMPTY);
				if (!X.isEmpty(role)) {
					Role r1 = Role.loadByName(role);
					if (r1 != null) {
						u.setRole(r1.id);

						if (!X.isEmpty(url)) {

							GLog.securitylog.warn("user", "sso", "redirect [" + url + "]", u, this.ip());

							this.redirect(url);
							return;
						}

						if (!X.isEmpty(r1.url)) {

							GLog.securitylog.warn("user", "sso", "redirect [" + r1.url + "]", u, this.ip());

							this.redirect(r1.url);
							return;
						}
					}
				}

				if (!X.isEmpty(url)) {

					GLog.securitylog.warn("user", "sso", "redirect [" + url + "]", u, this.ip());

					this.redirect(url);
					return;
				}

				Roles rs = u.getRole();
				if (rs != null && rs.getList() != null) {
					for (Role r1 : rs.getList()) {
						if (!X.isEmpty(r1.url)) {

							GLog.securitylog.warn("user", "sso", "redirect [" + r1.url + "]", u, this.ip());

							this.redirect(r1.url);
							return;
						}
					}
				}

				GLog.securitylog.warn("user", "sso", "redirect [/]", u, this.ip());

				this.redirect("/");
				return;

			} catch (Exception e) {
				log.error(e.getMessage(), e);

				GLog.securitylog.error("user", "sso", e.getMessage(), e, u, this.ip());

				this.print(X.toString(e));
			}

			return;
		}

		// single sign - on

		GLog.securitylog.warn("user", "sso", "redirect [/]", this.user(), this.ip());

		this.redirect("/");

	}

	@Path(path = "ssolink")
	public void ssolink() {

		String appid = this.get("appid");
		String secret = this.get("secret");
		String name = this.get("name");

		if (X.isEmpty(appid)) {
			this.set(X.ERROR, "[appid] missed").send(201);
			GLog.securitylog.warn("user", "ssolink", "[appid] missed", null, this.ip());
			return;
		}

		App a = App.load(appid);
		if (a == null) {
			this.set(X.ERROR, "bad appid [" + appid + "]").send(201);
			GLog.securitylog.warn("user", "ssolink", "bad appid [" + appid + "]", null, this.ip());
			return;
		}

		if (!X.isSame(secret, a.secret)) {
			this.set(X.ERROR, "bad secret for [" + appid + "]").send(201);
			GLog.securitylog.warn("user", "ssolink", "bad secret[" + secret + "] for [" + appid + "]", null, this.ip());
			return;
		}

		if (X.isEmpty(name)) {
			this.set(X.ERROR, "[name] missed, for username").send(201);
			GLog.securitylog.warn("user", "ssolink", "[name] missed, for username", null, this.ip());
			return;
		}

		JSON j1 = JSON.create();
		j1.append("name", name);
		j1.append("time", Global.now());

		String url = this.getHtml("url");

		String token = App.encode(j1.toString(), a.getSecret());
		JSON j2 = JSON.create();
		j2.append("appid", appid);
		j2.append("token", token);
		j2.append("url", Url.encode(url));
		String s = j2.toString();

		GLog.securitylog.info("user", "ssolink", "s=" + j2, null, this.ip());

		s = Base32.encode(s.getBytes());
		this.set("url", Global.getString("site.url", "") + "/user/sso?_s=" + s).send(200);

	}

	/**
	 * Login.
	 */
	@Path(path = "login")
	public void login() {

		if (!Helper.isConfigured()) {
			this.redirect("/admin/setup");
			return;
		}

		if (new File(Temp.ROOT + "/root.pwd").exists()) {
			this.set(X.MESSAGE, lang.get("root.pwd"));
		}

		if (method.isPost()) {

			String callback = this.get("callback");
			if (X.isEmpty(callback)) {
				callback = this.session(true).get("callback");
				log.info("get session, callback=" + callback);
			}

			if (!X.isEmpty(callback)) {
				try {
					String s = new String(Base32.decode(callback));
					if (!X.isEmpty(s)) {
						callback = s;
					}
				} catch (Exception e) {
					log.warn(e.getMessage(), e);
				}
			}

			JSON jo = JSON.create();
			AuthToken a = null;
			if (Global.getInt("user.token", 0) == 1) {
				String token = this.getString("token");
				a = AuthToken.load(token);
			}

			if (a != null) {

				// ok, logined
				jo.put(X.STATE, 200);
				jo.put(X.MESSAGE, "ok");
				jo.put("uid", a.getUid());
				jo.put("expired", a.getExpired());
				User u = a.getUser_obj();
				this.user(u, LoginType.ajax);

				if (u.expired()) {
					this.set(X.MESSAGE, lang.get("passwd.expired"));
					log.info("redirect to passwd as expired!");
					this.redirect("/user/passwd");
					GLog.securitylog.info("user", "login", "success, " + lang.get("passwd.expired"), u, this.ip());

					return;
				}

				if (!X.isEmpty(callback) && !callback.startsWith("/user/")) {
					// 重定向， 禁止重定向到 /user/...
					// SSO
					log.info("redirect to callback=" + callback);
					this.redirect(callback);
					GLog.securitylog.info("user", "login", "success, redirect: " + callback, u, this.ip());
					return;
				}

			} else {

				try {
					String name = this.getString("name");
					if (name != null) {
						name = name.toLowerCase().replaceAll("'", X.EMPTY).replaceAll("\"", X.EMPTY);
					}
					String pwd = this.getString("pwd");
					if (X.isEmpty(pwd)) {
						pwd = this.get("pwd1");
						if (!X.isEmpty(pwd)) {
							// decode
							pwd = new String(Base64.getDecoder().decode(pwd));
						}
					}
					if (!X.isEmpty(pwd)) {
						pwd = pwd.replaceAll("'", X.EMPTY).replaceAll("\"", X.EMPTY);
					}

					User u = User.load(name);
					Captcha.Result r = Captcha.Result.ok;

					if (Global.getInt("user.captcha", 0) == 1) {
						String code = this.getString("code");
						if (code != null) {
							code = code.toLowerCase();
						}
						r = Captcha.verify(this.sid(true), code);
						Captcha.remove(this.sid(true));

					}

					if (Captcha.Result.badcode == r) {

						jo.put(X.MESSAGE, lang.get("captcha.bad"));
						jo.put(X.STATE, 202);

						GLog.securitylog.error("user", "login", lang.get("captcha.bad"), u, this.ip());

					} else if (Captcha.Result.expired == r) {

						jo.put(X.MESSAGE, lang.get("captcha.expired"));
						jo.put(X.STATE, 203);

						GLog.securitylog.error("user", "login", lang.get("captcha.expired"), u, this.ip());

					} else {

						User me = User.load(name, pwd, this.ip());

						log.info("login: name=" + name + ", sid=" + sid(true) + ", me=" + me);

						if (me != null) {

							boolean locked = me.isLocked(this.ip());
							if (locked) {

								// locked by the host
								me.failed(this.ip(), sid(true), this.browser());
								jo.put(X.MESSAGE, lang.get("account.locked.error"));

								jo.put(X.STATE, 204);
								jo.put("name", name);
								jo.put("pwd", pwd);

								GLog.securitylog.error("user", "login", lang.get("account.locked.error"), u, this.ip());

							} else {

								GLog.securitylog.info("user", "login", me.name + " login success", u, this.ip());

								if (X.isSame("json", this.getString("type")) || this.isAjax()) {

									this.user(me, LoginType.ajax);

									if (log.isDebugEnabled()) {
										log.debug("isAjax login");
									}

									me.logined(sid(true), this.ip(),
											V.create("ajaxlogined", Global.now()));

									if (me.expired()) {
										this.set(X.MESSAGE, lang.get("passwd.expired"));

										log.info("redirect to passwd as expired");

										// this.redirect("/user/passwd");

										GLog.securitylog.info("user", "login", "success, " + lang.get("passwd.expired"),
												u, this.ip());

										return;
									}

//									if (!X.isEmpty(callback) && !callback.startsWith("/user/")) {
//										// 重定向， 禁止重定向到 /user/...
//										// SSO
//										log.info("redirect to callback=" + callback);
//										this.redirect(callback);
//										GLog.securitylog.info("user", "login", "success, redirect: " + callback, u,
//												this.ip());
//										return;
//									}

									jo.put("sid", sid(true));
									jo.put("uid", me.getId());

									/**
									 * test the configuration is enabled user token and this request is ajax
									 */
									if (Global.getInt("user.token", 0) == 1) {
										AuthToken t = AuthToken.create(me.getId(), this.ip());
										if (t != null) {
											jo.put("token", t.getToken());
											jo.put("expired", t.getExpired());
											jo.put(X.STATE, HttpServletResponse.SC_OK);
										} else {
											jo.put(X.MESSAGE, "create authtoken error");
											jo.put(X.STATE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
										}
									} else {
										jo.put(X.STATE, HttpServletResponse.SC_OK);
									}
									this.send(jo);
								} else {

									this.user(me, LoginType.web);

									/**
									 * logined, to update the stat data
									 */
									me.logined(sid(true), this.ip(),
											V.create("weblogined", Global.now()));

									if (me.expired()) {
										this.set(X.MESSAGE, lang.get("passwd.expired"));

										log.info("redirect to passwd as expired");

										this.redirect("/user/passwd");

										GLog.securitylog.info("user", "login", "success, " + lang.get("passwd.expired"),
												u, this.ip());

										return;
									}

									if (!X.isEmpty(callback) && !callback.startsWith("/user/")) {
										// 重定向， 禁止重定向到 /user/...
										// SSO
										log.info("redirect to callback=" + callback);
										this.redirect(callback);
										GLog.securitylog.info("user", "login", "success, redirect: " + callback, u,
												this.ip());
										return;

									}

									Roles rs = me.getRole();
									if (rs != null && rs.getList() != null) {
										for (Role r1 : rs.getList()) {
											if (!X.isEmpty(r1.url)) {

												log.info("redirect to role home=" + r1.url);

												this.redirect(r1.url);

												GLog.securitylog.info("user", "login", "success, home=" + r1.url, u,
														this.ip());

												return;
											}
										}
									}

									log.info("redirect to /");

									this.redirect("/");
//									this.redirect("/user/go");

									GLog.securitylog.info("user", "login", "success, goto /", u, this.ip());

								}
								return;
							}

						} else {

							jo.append(X.STATE, 200).append(X.MESSAGE, "ok");

							if (u == null) {
								jo.put("message", lang.get("login.name_password.error"));
								jo.put(X.STATE, 201);

								GLog.securitylog.error("user", "login",
										lang.get("login.name_password.error") + ":" + name, u, this.ip());
							} else {

								u.failed(this.ip(), sid(true), this.browser());

								boolean locked = u.isLocked(this.ip());
								if (locked) {
									jo.put("message", lang.get("login.locked.error"));
									jo.put(X.STATE, 204);

									GLog.securitylog.error("user", "login", lang.get("login.failed") + ":" + name, u,
											this.ip());
								} else {

									long[] n = u.failed(this.ip());
									if (n != null) {
										jo.put(X.MESSAGE,
												String.format(lang.get("login.name_password.error.times"), n[0], n[1]));
										jo.put(X.STATE, 204);
									}

									GLog.securitylog.warn("user", "login", jo.getString(X.MESSAGE) + ":" + name, u,
											this.ip());
								}
							}

							jo.put("name", name);
							jo.put("pwd", pwd);
						}
					}
				} catch (Exception e) {
					log.error(e.getMessage(), e);
					jo.put(X.MESSAGE, e.getMessage());
					jo.put(X.STATE, 201);
				}
			}

			if (X.isSame("json", this.getString("type")) || this.isAjax()) {

				this.send(jo);
				return;

			} else if (login != null) {

				Roles rs = login.getRole();
				if (rs != null && rs.getList() != null) {
					for (Role r1 : rs.getList()) {
						if (!X.isEmpty(r1.url)) {
							log.info("redirect to role home=" + r1.url);
							this.redirect(r1.url);
							return;
						}
					}
				}

				if (login.expired()) {
					this.set(X.MESSAGE, lang.get("passwd.expired"));
					log.info("redirect to passwd as expired");
					this.redirect("/user/passwd");
					return;
				}

				log.info("redirect to /");
				this.redirect("/");
			} else {
				this.copy(this.json());
				this.set(X.STATE, jo.getInt(X.STATE));
				this.set(X.MESSAGE, jo.getString(X.MESSAGE));
			}
		} else {
			String sid = this.getString("sid");
			if (!X.isEmpty(sid)) {
				AuthToken.dao.delete(W.create().and("sid", sid));
			}
		}

		String refer = this.getString("refer");
		if (!X.isEmpty(refer) && !isAjax()) {
			try {
				this.session(true).set("uri", URLDecoder.decode(refer, "UTF-8")).store();
			} catch (Exception e) {
				log.error(refer, e);
				GLog.securitylog.error("user", "login", e.getMessage(), e, login, this.ip());
			}
		}

		this.user(null);

		String callback = this.get("callback");
		try {
			if (callback != null) {
				// TO FIX XSS issue
				callback = Base32.encode(callback.getBytes());
				log.info("store in session, callback=" + callback);
				this.set("callback", callback);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		String tips = this.get("tips");
		byte[] bb = Base32.decode(tips);
		if (bb != null) {
			this.set("tips", tips);
			this.set("tips2", new String(bb));
		}

		show("/user/user.login.html");
	}

	/**
	 * Logout.
	 */
	@Path(path = "logout")
	public void logout() {

		User u = this.user();

		if (u != null) {

			/**
			 * clear auth-token
			 */
			AuthToken.delete(u.getId(), sid(true));

			GLog.securitylog.info("user", "logout", null, u, this.ip());

			login.logout();

			/**
			 * clear the user in session, but still keep the session
			 */
			user(null, (User) null);

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
			this.head("Location", "/");
			this.send(jo);
			return;
		} else {
			this.redirect("/");
			return;
		}

	}

	/**
	 * Verify.
	 */
	@Path(path = "verify")
	public void verify() {
		String name = this.getString("name");
		String value = this.getString("value");

		if (X.isEmpty(value)) {
			this.set(X.MESSAGE, lang.get("user.name.format.error")).send(201);
			return;
		}

//		JSON jo = new JSON();
		if (X.isSame(name, "name")) {
			try {
//				if (User.dao.exists(W.create().and("name", value))) {
//
//					jo.put(X.STATE, 201);
//					jo.put(X.MESSAGE, lang.get("user.name.exists"));
//
//					GLog.securitylog.info("user", "verify", "name=" + name + ",value=" + value + ",exists", login,
//							this.ip());
//
//				} else {
				String rule = Global.getString("user.name.rule", "^[a-zA-Z0-9]{4,16}$");

				if ((!X.isEmpty(rule) && !value.matches(rule))) {

					GLog.securitylog.info("user", "verify", "name=" + name + ",value=" + value + ",rule=" + rule, login,
							this.ip());

					this.set(X.MESSAGE, lang.get("user.name.format.error")).send(201);
					return;
				} else {
					this.send(200);
					return;
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				this.set(X.MESSAGE, "internal error").send(500);
				return;
			}
		} else if (X.isSame(name, "password")) {
			String rule = Global.getString("user.passwd.rule", "^[a-zA-Z0-9]{6,16}$");
			if (!X.isEmpty(rule) && !value.matches(rule)) {

				GLog.securitylog.info("user", "verify", "name=" + name + ",value=" + value + ",rule=" + rule, login,
						this.ip());

				this.set(X.MESSAGE, lang.get("user.name.format.error")).send(201);
				return;

			} else {
				this.send(200);
				return;
			}
		}

		this.set(X.MESSAGE, "bad [" + name + "]").send(201);
	}

	/**
	 * get user list by "access" token
	 */
	@Path(path = "popup2", login = true)
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

		this.send(jo);

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
					Code c = Code.dao.load(W.create().and("s2", email).sort(X.CREATED, -1));
					if (c != null && c.getUpdated() < X.AMINUTE) {

						jo.put(X.MESSAGE, lang.get("user.forget.email.sent"));
						jo.put(X.STATE, HttpServletResponse.SC_OK);

					} else {
						int s = 0;

						StringBuilder sb = new StringBuilder();
						W q = W.create().and("email", email);
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

							if (c == null || c.getExpired() < Global.now()) {
								code = UID.random(10);
								Code.create(code, email, V.create("expired", Global.now() + X.ADAY));
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
											Code.dao.update(W.create().and("s1", code).and("s2", email),
													V.create(X.UPDATED, Global.now()));

										} else {
											jo.put(X.MESSAGE, lang.get("user.forget.email.sent.failed"));
											jo.put(X.STATE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
										}
									} catch (Exception e) {
										log.error(e.getMessage(), e);
										GLog.applog.error("user", "forget", e.getMessage(), e, login, this.ip());
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
					} else if (c.getExpired() < Global.now()) {
						jo.put(X.STATE, HttpServletResponse.SC_BAD_REQUEST);
						jo.put(X.MESSAGE, lang.get("email.code.expired"));
					} else {
						Code.delete(code, email);

						String passwd = this.getString("passwd");
						if (X.isEmpty(passwd)) {
							passwd = this.get("pwd1");
							if (!X.isEmpty(passwd)) {
								// decode
								passwd = new String(Base64.getDecoder().decode(passwd));
							}
						}

						String rule = Global.getString("user.passwd.rule", "^[a-zA-Z0-9]{6,16}$");
						if (!X.isEmpty(rule) && !passwd.matches(rule)) {
							jo.put(X.STATE, HttpServletResponse.SC_BAD_REQUEST);
							jo.put(X.MESSAGE, "user.passwd.format.error");
						} else {
							try {
								User.update(W.create().and("email", email), V.create("password", passwd));
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
					Code c = Code.dao.load(W.create().and("s2", phone).sort(X.CREATED, -1));
					if (c != null && c.getUpdated() < X.AMINUTE) {

						jo.put(X.MESSAGE, lang.get("user.forget.phone.sent"));
						jo.put(X.STATE, HttpServletResponse.SC_OK);

					} else {
						int s = 0;

						StringBuilder sb = new StringBuilder();
						W q = W.create().and("phone", phone);
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

							if (c == null || c.getExpired() < Global.now()) {
								code = UID.digital(4);
								Code.create(code, phone,
										V.create("expired", Global.now() + X.AMINUTE * 6));
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
							// V.create(X.UPDATED, Global.now()));
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
					} else if (c.getExpired() < Global.now()) {
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
							User.update(W.create().and("phone", phone), V.create("password", passwd));
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

			this.send(jo);
			return;
		}

		this.show("/user/user.forget.html");

	}

	@Path(path = "access", login = true)
	public void access() {
		this.send(JSON.create().append(X.STATE, 200).append("list", login.getRole().getAccesses()));
	}

	/**
	 * 获取用户列表，参数：access 具有该权限的用户列表
	 */
	@Path(login = true, path = "list")
	public void list() {

		List<User> l1 = User.loadByAccess(this.get("access"));

		this.send(JSON.create().append(X.STATE, 200).append("data", X.asList(l1, e -> {
			return ((User) e).json().remove("_.*", "updated", "created", "password", "md4passwd", "locked");
		})));

	}

	/**
	 * 获取登录用户信息
	 */
	@Path(path = "myinfo")
	public void myinfo() {

		User u = this.user();
		if (u == null) {
//			this.set(X.ERROR, "login required").send(401);
			this.send(200);
			return;
		}

		u = User.dao.load(login.getId());
		JSON j1 = u.json();

		j1.append("ip", this.ip());
		j1.append("now", lang.format(Global.now(), "yyyy-MM-dd"));

		Map<String, JSON> home = new TreeMap<String, JSON>();
		JSON jo = JSON.create().append(X.STATE, 200).append("data", j1.append("accesses", u.getAccesses())
				.append("roles", u.getRole() == null ? null : X.asList(u.getRole().getList(), e -> {
					Role r = (Role) e;
					if (!X.isEmpty(r.url)) {
						home.put(r.url, JSON.create().append("url", r.url).append("name", r.getName()));
					}
					return r.getName();
//
//					JSON j2 = r.json();
//
//					return j2;
				})));
		if (!home.isEmpty()) {
			jo.append("home", home.values());
		}
		jo.put("home_title", lang.get("home.title"));
		jo.put("bg.watermark", Global.getInt("web.bg.watermark", 0));
		jo.put("name", u.name);

		this.send(jo);

	}

	/**
	 * 修改用户个人信息
	 */
	@Path(path = "edit", login = true)
	public void edit() {

		V v = V.create();
		for (String name : new String[] { "title", "phone", "email", "nickname", "photo" }) {
			String s = this.get(name);
			if (s != null) {
				v.append(name, s);
			}
		}
		if (!v.isEmpty()) {
			User.dao.update(login.id, v);
		}
		this.set(X.MESSAGE, lang.get("save.success")).send(200);

	}

	/**
	 * 修改个人密码
	 */
	@Path(login = true, path = "passwd")
	public void passwd() {

		if (this.method.isPost()) {
			try {
				String pwd1 = this.getString("pwd1");
				if (X.isEmpty(pwd1)) {
					pwd1 = this.getString("pwd");
				} else {
					pwd1 = new String(Base64.getDecoder().decode(pwd1));
				}

				User.update(login.getId(),
						V.create().append("password", pwd1).append("passwordtime", Global.now()));

				GLog.securitylog.warn("user", "passwd", lang.get("user.passwd.change"), login, this.ip());

				if (this.isAjax()) {

					this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, "密码修改成功！"));

				} else {

					Roles rs = login.getRole();
					if (rs != null && rs.getList() != null) {
						for (Role r1 : rs.getList()) {
							if (!X.isEmpty(r1.url)) {
								this.redirect(r1.url);
								return;
							}
						}
					}
					this.redirect("/");

				}
				return;
			} catch (Exception e) {

				log.error(e.getMessage(), e);
				this.set(X.MESSAGE, e.getMessage());

			}
		}

		// 修改密码
		this.show("/user/user.passwd.html");

	}

	@Path(path = "timestamp")
	public void timestamp() {
		this.print(Long.toString(Global.now()));
	}

	@Path(path = "sso/login", method = "POST")
	public void sso_login() {

		String appid = this.get("appid");
		String secret = this.get("secret");
		String cookiename = this.get("cookiename");
		String cookievalue = this.get("cookievalue");
		String username = this.get("username");
		String nickname = this.get("nickname");

		App a = App.load(appid);
		if (a == null) {
			this.set(X.ERROR, "bad appid [" + appid + "]").send(201);
			return;
		}

		if (!X.isSame(secret, a.secret)) {
			this.set(X.ERROR, "bad secret [" + secret + "]").send(201);
			return;
		}

		if (X.isEmpty(cookiename)) {
			this.set(X.ERROR, "bad cookiename [" + cookiename + "]").send(201);
			return;
		}

		if (X.isEmpty(cookievalue)) {
			this.set(X.ERROR, "bad cookievalue [" + cookievalue + "]").send(201);
			return;
		}

		if (X.isEmpty(username) || username.length() < 4) {
			this.set(X.ERROR, "bad username [" + username + "]").send(201);
			return;
		}

		if (!X.isSame(Controller.COOKIE_NAME, cookiename)) {
			log.warn("set cookiename=" + cookiename);
			GLog.applog.warn(user.class, "sso/login", "set cookiename=" + cookiename);
			Controller.COOKIE_NAME = cookiename;
		}

		User u = User.load(username);
		if (u == null) {
			// create a new
			V v = V.create();
			v.append("name", username);
			v.append("nickname", nickname);
			v.append("createdua", "sso");
			v.append("password", UID.random(12));

			try {
				long id = User.create(v);
				GLog.securitylog.warn("sso", "createuser", "name=" + username, null, this.ip());

				u = User.dao.load(id);

				String role = Global.getString("user.login.sso.role", X.EMPTY);
				if (!X.isEmpty(role)) {
					Role r1 = Role.loadByName(role);
					if (r1 != null) {
						u.setRole(r1.id);
					}
				}

			} catch (Exception e) {
				log.error(e.getMessage(), e);
				GLog.securitylog.error("sso", "login", "name=" + username, e, null, this.ip());
				this.set(X.ERROR, e.getMessage()).send(201);
				return;
			}

		}

		this.user(cookievalue, u);
		GLog.securitylog.info("sso", "login", "name=" + username, u, this.ip());

		this.set(X.MESSAGE, "ok").send(200);

	}

	@Path(path = "sso/logout", method = "POST")
	public void sso_logout() {
		String appid = this.get("appid");
		String secret = this.get("secret");

		App a = App.load(appid);
		if (a == null) {
			this.set(X.ERROR, "bad appid [" + appid + "]").send(201);
			return;
		}

		if (!X.isSame(secret, a.secret)) {
			this.set(X.ERROR, "bad secret [" + secret + "]").send(201);
			return;
		}

		String cookievalue = this.get("cookievalue");
		Session s = (Session) Cache.get("session/" + cookievalue);
		User u = s.get("user");
		this.user(cookievalue, null);
		if (u != null) {
			GLog.securitylog.info("sso", "logout", "cookievalue=" + cookievalue, u, this.ip());
		}

		this.set(X.MESSAGE, "ok").send(200);
	}

}
