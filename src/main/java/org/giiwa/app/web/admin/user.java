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

import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.conf.Global;
import org.giiwa.core.conf.Local;
import org.giiwa.core.bean.X;
import org.giiwa.core.json.JSON;
import org.giiwa.core.noti.Email;
import org.giiwa.core.noti.Sms;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.*;
import org.giiwa.framework.web.*;
import org.giiwa.framework.web.view.VelocityView;

/**
 * web api: /admin/user <br>
 * used to manage user<br>
 * required "access.user.admin"
 * 
 * @author joe
 *
 */
public class user extends Model {

	/**
	 * Adds the.
	 */
	@SuppressWarnings("deprecation")
	@Path(path = "create", login = true, access = "access.config.admin|access.config.user.admin")
	public void create() {
		if (method.isPost()) {

			JSON jo = this.getJSON();
			final String name = this.getString("name").trim().toLowerCase();
			String rule = Global.getString("user.name.rule", "^[a-zA-Z0-9]{4,16}$");
			if (!X.isEmpty(rule) && !name.matches(rule)) {
				this.set(jo);
				this.set(X.MESSAGE, lang.get("user.name.format.error"));
			} else {
				try {

					/**
					 * create the user
					 */
					if (User.dao.exists(W.create("name", name))) {
						/**
						 * exists, create failded
						 */
						this.set(X.ERROR, lang.get("user.name.exists"));
					} else {

						V v = V.create("name", name).copy(jo).set("locked", 0);
						v.remove("role");
						long id = User.create(v);

						/**
						 * set the role
						 */
						String[] roles = this.getStrings("role");
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
						GLog.securitylog.info(user.class, "create", this.getJSONNonPassword().toString(), login,
								this.getRemoteHost());

						this.set(X.MESSAGE, lang.get("save.success"));

						if (Global.getInt("user.updated.noti", 1) == 1) {
							final String email = this.getString("email");
							final String phone = this.getString("phone");
							final String passwd = this.getString("password");

							if (!X.isEmpty(email) || !X.isEmpty(phone)) {
								new Task() {

									@Override
									public void onExecute() {
										if (!X.isEmpty(phone)) {
											JSON jo = JSON.create();
											jo.put("account", name);
											jo.put("passwd", passwd);
											Sms.send(phone, "add.account", jo);
										}

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

												VelocityView v1 = new VelocityView();
												String body = v1.parse(f, j1);
												if (body != null) {
													Email.send(lang.get("mail.creation.noti"), body, email);
												}
											}

										}
									}
								}.schedule(10);
							}
						}

						onGet();
						return;
					}
				} catch (Exception e) {
					log.error(e.getMessage(), e);
					GLog.securitylog.error(user.class, "create", e.getMessage(), e, login, this.getRemoteHost());

					this.set(X.ERROR, lang.get("save.failed"));

					this.set(jo);
				}
			}

		}

		Beans<Role> bs = Role.load(0, 1000);
		if (bs != null) {
			this.set("roles", bs);
		}

		this.show("/admin/user.create.html");
	}

	/**
	 * Delete.
	 */
	@SuppressWarnings("deprecation")
	@Path(path = "delete", login = true, access = "access.config.admin|access.config.user.admin")
	public void delete() {

		JSON jo = new JSON();

		long id = this.getLong("id");
		if (id > 0) {
			User.delete(id);
			List<String> list = AuthToken.delete(id);
			if (list != null) {
				for (String sid : list) {
					Session.delete(sid);
				}
			}

			GLog.securitylog.warn(user.class, "delete", this.getJSONNonPassword().toString(), login,
					this.getRemoteHost());
			jo.put(X.STATE, 200);
		} else {
			jo.put(X.MESSAGE, lang.get("delete.failed"));
		}

		this.response(jo);

	}

	/**
	 * Edits the user.
	 */
	@SuppressWarnings("deprecation")
	@Path(path = "edit", login = true, access = "access.config.admin|access.config.user.admin")
	public void edit() {
		long id = this.getLong("id");

		if (method.isPost()) {

			String password = this.getString("password");
			if (!X.isEmpty(password)) {
				JSON jo = new JSON();
				User.update(id, V.create("password", password));
				jo.put(X.STATE, 200);
				this.response(jo);
				return;
			}
			JSON j = this.getJSON();
			V v = V.create().copy(j);
			v.remove("role", X.ID);

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
				v.set("roles", list);
			}

			List<String> list = AuthToken.delete(id);
			if (list != null && list.size() > 0) {
				for (String s : list) {
					Session.delete(s);
				}
			}

			GLog.securitylog.info(user.class, "edit", this.getJSONNonPassword().toString(), login,
					this.getRemoteHost());

			this.set(X.MESSAGE, lang.get("save.success"));

			onGet();

		} else {

			User u = User.dao.load(id);
			if (u != null) {
				this.set(u.getJSON());
				this.set("u", u);

				Beans<Role> bs = Role.load(0, 1000);
				if (bs != null) {
					this.set("roles", bs);
				}

				this.set("id", id);
				this.show("/admin/user.edit.html");
				return;
			}

			this.set(X.ERROR, lang.get("select.required"));
			onGet();

		}
	}

	/**
	 * Detail.
	 */
	@Path(path = "detail", login = true, access = "access.config.admin|access.config.user.admin")
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

	@Path(path = "oplog", login = true, access = "access.config.admin|access.config.user.admin")
	public void oplog() {

		int s = this.getInt("s");
		int n = this.getInt("n", X.ITEMS_PER_PAGE, "items.per.page");

		W q = getW(this.getJSON());
		Beans<GLog> bs = GLog.dao.load(q, s, n);
		this.set(bs, s, n);

		this.show("/admin/user.oplog.html");
	}

	@Path(path = "accesslog", login = true, access = "access.config.admin|access.config.logs.admin")
	public void accesslog() {
		long uid = this.getLong("uid");
		this.set("uid", uid);

		W q = W.create("uid", uid);
		int s = this.getInt("s");
		int n = this.getInt("n", X.ITEMS_PER_PAGE, "items.per.page");

		Beans<AccessLog> bs = AccessLog.dao.load(q, s, n);

		this.set(bs, s, n);

		this.show("/admin/user.accesslog.html");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model#onGet()
	 */
	@Override
	@Path(login = true, access = "access.config.admin|access.config.user.admin")
	public void onGet() {

		String name = this.getString("name");
		W q = W.create();
		if (X.isEmpty(this.path) && !X.isEmpty(name)) {
			W list = W.create();

			list.or("name", name, W.OP.like);
			list.or("nickname", name, W.OP.like);
			q.and(list);

			this.set("name", name);
		}

		int s = this.getInt("s");
		int n = this.getInt("n", X.ITEMS_PER_PAGE, "items.per.page");

		Beans<User> bs = User.load(q.and(X.ID, 0, W.OP.gt), s, n);
		this.set(bs, s, n);

		this.query.path("/admin/user");

		this.show("/admin/user.index.html");
	}

	private W getW(JSON jo) {

		W q = W.create();

		if (!X.isEmpty(jo.get("op"))) {
			q.and("op", jo.get("op"));
		}
		if (!X.isEmpty(jo.get("ip"))) {
			q.and("ip", jo.getString("ip"), W.OP.like);
		}
		q.and("uid", jo.getLong("uid"));
		if (!X.isEmpty(jo.get("type"))) {
			q.and("type", X.toInt(jo.get("type")));
		}

		if (!X.isEmpty(jo.getString("model"))) {
			q.and("model", jo.getString("model"));
		}

		if (!X.isEmpty(jo.getString("node"))) {
			q.and("node", jo.getString("node"));
		}

		if (!X.isEmpty(jo.getString("starttime"))) {
			q.and(X.CREATED, lang.parse(jo.getString("starttime"), "yyyy-MM-dd"), W.OP.gte);

		} else {
			long today_2 = System.currentTimeMillis() - X.AYEAR * 1;
			jo.put("starttime", lang.format(today_2, "yyyy-MM-dd"));
			q.and(X.CREATED, today_2, W.OP.gte);
		}

		if (!X.isEmpty(jo.getString("endtime"))) {
			q.and(X.CREATED, lang.parse(jo.getString("endtime"), "yyyy-MM-dd"), W.OP.like);
		}

		String sortby = this.getString("sortby");
		if (!X.isEmpty(sortby)) {
			int sortby_type = this.getInt("sortby_type");
			q.sort(sortby, sortby_type);
		} else {
			q.sort(X.CREATED, -1);
		}
		this.set(jo);

		return q;
	}

}
