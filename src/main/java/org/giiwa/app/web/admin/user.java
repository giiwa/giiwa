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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.giiwa.bean.*;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.Beans;
import org.giiwa.dao.Helper;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.dao.UID;
import org.giiwa.json.JSON;
import org.giiwa.misc.noti.Email;
import org.giiwa.task.Task;
import org.giiwa.web.*;
import org.giiwa.web.view.View;

/**
 * web api: /admin/user <br>
 * used to manage user<br>
 * required "access.user.admin"
 * 
 * @author joe
 *
 */
public class user extends Controller {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Adds the.
	 */
	@Path(path = "create", login = true, access = "access.config.admin", oplog = true)
	public void create() {
		if (method.isPost()) {

			JSON jo = this.json();
			final String name = this.getString("name").trim().toLowerCase();
			try {

				int number = this.getInt("number", 1);
				if (number > 1) {

					Task.schedule(t -> {
						int n = 0;
						try {
							for (int i = 0; n < number; i++) {

								String name1 = name + "_" + i;

								/**
								 * create the user
								 */
								if (!User.dao.exists(W.create("name", name1))) {

									V v = V.create("name", name1).copy(jo).append("locked", 0);
									v.remove("role");
									v.append("createdip", this.ip()).append("createdua", this.browser())
											.force("createdby", login.getId());
									v.append("title", this.get("title"));
									v.append("company", this.get("company"));
									v.append("phone", this.get("phone"));
									v.append("email", this.get("email"));
									v.force("unitid", this.getLong("unitid"));
									v.append("password", this.getHtml("password"));
									v.force("limitip", this.getInt("limitip"));
									v.force("disklimitsize", this.getLong("disklimitsize"));

									long id = User.create(name, v);

									n++;

									/**
									 * set the role
									 */
									String[] roles = this.getStrings("role");
									if (log.isDebugEnabled())
										log.debug("roles=" + Helper.toString(roles));

									if (roles != null) {
										User u = User.dao.load(id);
										List<Long> list = new ArrayList<Long>();
										for (String s : roles) {
											list.add(X.toLong(s));
										}
										u.setRoles(list);
									}
								}
							}
						} catch (Exception e) {
							log.error(e.getMessage(), e);
							GLog.securitylog.error(user.class, "create", e.getMessage(), e, login, this.ip());
						}
					});

					GLog.securitylog.warn(user.class, "create", this.json().toString(), login, this.ip());
					this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("save.success")));
					return;

				} else {
					/**
					 * create the user
					 */
					if (User.dao.exists(W.create("name", name))) {
						/**
						 * exists, create failded
						 */
						this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE, lang.get("user.name.exists")));
						return;

					} else {

						V v = V.create("name", name).copy(jo).append("locked", 0);
						v.remove("role");
						v.append("createdip", this.ip()).append("createdua", this.browser()).append("createdby",
								login.getId());
						v.append("title", this.get("title"));
						v.append("company", this.get("company"));
						v.append("phone", this.get("phone"));
						v.append("email", this.get("email"));
						v.force("unitid", this.getLong("unitid"));
						v.append("password", this.getHtml("password"));
						v.force("limitip", this.getInt("limitip"));
						v.force("disklimitsize", this.getLong("disklimitsize"));

						long id = User.create(name, v);

						/**
						 * set the role
						 */
						String[] roles = this.getStrings("role");
						if (log.isDebugEnabled())
							log.debug("roles=" + Helper.toString(roles));

						if (roles != null) {
							User u = User.dao.load(id);
							List<Long> list = new ArrayList<Long>();
							for (String s : roles) {
								list.add(X.toLong(s));
							}
							u.setRoles(list);
						}

						/**
						 * log
						 */
						GLog.securitylog.warn(user.class, "create", this.json().toString(), login, this.ip());

						if (Global.getInt("user.updated.noti", 1) == 1) {
							final String email = this.getString("email");
							final String passwd = this.getString("password");

							if (!X.isEmpty(email)) {

								Task.schedule(t -> {

									if (!X.isEmpty(email)) {

										File f = module
												.getFile("/admin/email.creation." + lang.getLocale() + ".template");
										if (f != null) {
											JSON j1 = JSON.create();
											j1.put("email", email);
											j1.put("account", name);
											j1.put("passwd", passwd);
											j1.put("lang", lang);
											j1.put("global", Global.getInstance());
											j1.put("local", Local.getInstance());

											View v1 = View.getVelocity();
											String body = v1.parse(f, j1);
											if (body != null) {
												try {
													Email.send(lang.get("mail.creation.noti"), body, email);
												} catch (Exception e) {
													log.error(e.getMessage(), e);
													GLog.applog.error(user.class, "create", e.getMessage(), e, login,
															this.ip());
												}
											}
										}

									}

								}, 10);
							}
						}

						this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("save.success")));
						return;
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				GLog.securitylog.error(user.class, "create", e.getMessage(), e, login, this.ip());

				this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE,
						lang.get("save.failed") + ":" + e.getMessage()));
				return;
			}

		}

		this.set("password", UID.random(12).toLowerCase());
		Beans<Role> bs = Role.load(0, 1000);
		this.set("roles", bs);

		Beans<Unit> l1 = Unit.dao.load(W.create().sort("no"), 0, 1024);
		this.set("units", l1);

		this.show("/admin/user.create.html");
	}

	/**
	 * Delete.
	 */
	@Path(path = "delete", login = true, access = "access.config.admin", oplog = true)
	public void delete() {

		JSON jo = new JSON();

		long id = this.getLong("id");
		if (id > 0) {
			User.delete(id);
			AuthToken.delete(id);

			GLog.securitylog.warn(user.class, "delete", this.json().toString(), login, this.ip());
			jo.put(X.STATE, 200);
		} else {
			jo.put(X.MESSAGE, lang.get("delete.failed"));
		}

		this.send(jo);

	}

	@Path(path = "deleteall", login = true, access = "access.config.admin", oplog = true)
	public void deleteall() {

		String name = this.getString("name");
		W q = W.create();
		W list = W.create();

		list.or("name", name, W.OP.like);
		list.or("nickname", name, W.OP.like);
		if (X.isNumber(name)) {
			list.or("id", X.toLong(name));
		}
		q.and(list);
		User.dao.delete(q);

		GLog.securitylog.warn(user.class, "deleteall", this.json().toString(), login, this.ip());
		this.set(X.MESSAGE, lang.get("delete.success")).send(200);

	}

	/**
	 * Edits the user.
	 */
	@Path(path = "edit", login = true, access = "access.config.admin", oplog = true)
	public void edit() {
		long id = this.getLong("id");

		if (method.isPost()) {

			try {
				String password = this.getString("password");
				if (!X.isEmpty(password)) {

					User.update(id, V.create("password", password));

					Session.expired(id);

					GLog.securitylog.warn(user.class, "passwd", lang.get("user.passwd.change"), login, this.ip());

					this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("save.success")));
					return;
				}
				JSON j = this.json();
				V v = V.create().copy(j);
				v.remove("role", X.ID);
				v.force("limitip", this.getInt("limitip"));
				v.force("unitid", this.getLong("unitid"));
				v.force("failtimes", this.getInt("failtimes"));
				if (!"on".equals(this.getString("locked"))) {
					/**
					 * clean all the locked info
					 */
					User.Lock.removed(id);
					v.force("locked", 0);
				} else {
					v.force("locked", 1);
				}

				User.update(id, v);
				User u = User.dao.load(id);

				String[] roles = this.getStrings("role");
				if (roles != null) {
					List<Long> list = new ArrayList<Long>();
					for (String s : roles) {
						list.add(X.toLong(s));
					}

					u.setRoles(list);
					v.append("roles", list);
				}

				Session.expired(id);

				GLog.securitylog.info(user.class, "edit", this.json().toString(), login, this.ip());

				this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("save.success")));
				return;
			} catch (Exception e) {
				this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE,
						lang.get("save.failed") + ":" + e.getMessage()));
				return;
			}

		} else {

			User u = User.dao.load(id);
			if (u != null) {
				this.copy(u.json());
				this.set("u", u);

				Beans<Role> bs = Role.load(0, 1000);
				this.set("roles", bs);

				Beans<Unit> l1 = Unit.dao.load(W.create().sort("no"), 0, 1024);
				this.set("units", l1);

				this.set("id", id);
				this.show("/admin/user.edit.html");
				return;
			}

			this.set(X.ERROR, lang.get("select.required"));
			onGet();

		}
	}

	@Path(path = "unlock", login = true, access = "access.config.admin", oplog = true)
	public void unlock() {
		long id = this.getLong("id");

		try {

			V v = V.create();

			User.Lock.removed(id);
			v.force("locked", 0);

			User.update(id, v);
			User u = User.dao.load(id);

			GLog.securitylog.warn(user.class, "edit", id + "/" + u.name + "/" + u.nickname, login, this.ip());

			this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("save.success")));
			return;
		} catch (Exception e) {
			this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE,
					lang.get("save.failed") + ":" + e.getMessage()));
			return;
		}

	}

	/**
	 * Detail.
	 */
	@Path(path = "detail", login = true, access = "access.config.admin", oplog = true)
	public void detail() {
		String id = this.getString("id");
		if (id != null) {
			long i = X.toLong(id, -1);
			User u = User.dao.load(i);
			this.set("u", u);

			Beans<Role> bs = Role.load(0, 100);
			if (bs != null) {
				this.set("roles", bs);
			}

			this.show("/admin/user.detail.html");
		} else {
			onGet();
		}
	}

//	@Path(path = "oplog", login = true, access = "access.config.admin")
//	public void oplog() {
//
//		int s = this.getInt("s");
//		int n = this.getInt("n", X.ITEMS_PER_PAGE);
//
//		W q = getW(this.json());
//		Beans<GLog> bs = GLog.dao.load(q, s, n);
//		this.pages(bs, s, n);
//
//		this.show("/admin/user.oplog.html");
//	}

//	@Path(path = "accesslog", login = true, access = "access.config.admin|access.config.logs.admin")
//	public void accesslog() {
//		long uid = this.getLong("uid");
//		this.set("uid", uid);
//
//		W q = W.create("uid", uid).sort("created", -1);
//		int s = this.getInt("s");
//		int n = this.getInt("n", 10);
//
//		Beans<AccessLog> bs = AccessLog.dao.load(q, s, n);
//
//		this.pages(bs, s, n);
//
//		this.show("/admin/user.accesslog.html");
//	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model.onGet()
	 */
	@Override
	@Path(login = true, access = "access.config.admin")
	public void onGet() {

		String name = this.getString("name");
		W q = W.create();
		if (X.isEmpty(this.path) && !X.isEmpty(name)) {
			W list = W.create();

			list.or("name", name, W.OP.like);
			list.or("nickname", name, W.OP.like);
			if (X.isNumber(name)) {
				list.or("id", X.toLong(name));
			}
			q.and(list);

			this.set("name", name);
		}

		int s = this.getInt("s");
		int n = this.getInt("n", X.ITEMS_PER_PAGE);

		q.and(X.ID, 0, W.OP.gt).sort("name", 1);

		User.dao.optimize(q);

		Beans<User> bs = User.load(q, s, n);
		if (bs != null) {
			bs.count();
		}
		this.pages(bs, s, n);

		this.show("/admin/user.index.html");
	}

//	private W getW(JSON jo) {
//
//		W q = W.create();
//
//		if (!X.isEmpty(jo.get("op"))) {
//			q.and("op", jo.get("op"));
//		}
//		if (!X.isEmpty(jo.get("ip"))) {
//			q.and("ip", jo.getString("ip"), W.OP.like);
//		}
//		q.and("uid", jo.getLong("uid"));
//		if (!X.isEmpty(jo.get("type"))) {
//			q.and("type", X.toInt(jo.get("type")));
//		}
//
//		if (!X.isEmpty(jo.getString("model"))) {
//			q.and("model", jo.getString("model"));
//		}
//
//		if (!X.isEmpty(jo.getString("node"))) {
//			q.and("node", jo.getString("node"));
//		}
//
//		if (!X.isEmpty(jo.getString("starttime"))) {
//			q.and(X.CREATED, lang.parse(jo.getString("starttime"), "yyyy-MM-dd"), W.OP.gte);
//
//		} else {
//			long today_2 = System.currentTimeMillis() - X.AYEAR * 1;
//			jo.put("starttime", lang.format(today_2, "yyyy-MM-dd"));
//			q.and(X.CREATED, today_2, W.OP.gte);
//		}
//
//		if (!X.isEmpty(jo.getString("endtime"))) {
//			q.and(X.CREATED, lang.parse(jo.getString("endtime"), "yyyy-MM-dd"), W.OP.like);
//		}
//
//		String sortby = this.getString("sortby");
//		if (!X.isEmpty(sortby)) {
//			int sortby_type = this.getInt("sortby_type");
//			q.sort(sortby, sortby_type);
//		} else {
//			q.sort(X.CREATED, -1);
//		}
//		this.copy(jo);
//
//		return q;
//	}

	@Path(path = "random", login = true, access = "access.config.admin")
	public void random() {
		this.set("code", UID.random(12).toLowerCase()).send(200);
	}

}
