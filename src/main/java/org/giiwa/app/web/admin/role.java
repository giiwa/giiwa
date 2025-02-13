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

import java.util.List;
import java.util.Map;

import org.giiwa.bean.*;
import org.giiwa.dao.Beans;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.web.*;

/**
 * web api: /admin/role <br>
 * used toe manage role,<br>
 * required "access.role.admin"
 * 
 * @author joe
 *
 */
public class role extends Controller {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Adds the.
	 */
	@Path(path = "create", login = true, access = "access.config.admin|access.config.role.admin", oplog = true)
	public void create() {
		if (method.isPost()) {

			String name = this.getString("name");
			String memo = this.getString("memo");
			String url = this.getHtml("url");
			String menu = this.getHtml("menu");
			long id = Role.create(name, memo,
					V.create("url", url).append("menu", menu).append("seq", this.getInt("seq")));
			if (id > 0) {

//				String[] ss = this.getStrings("access");
				String s1 = this.getHtml("access");
				String[] ss = X.split(s1, ";");
				if (ss != null) {
					Role r = Role.dao.load(id);
					r.setAccess(ss);
				}

				this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("save.success")));
				return;

			} else {
				this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE, lang.get("save.failed")));
				return;

			}

		}

		Map<String, List<Access>> bs = Access.load();
		this.set("accesses", bs);

		this.show("/admin/role.create.html");
	}

	/**
	 * Verify.
	 */
	@Path(path = "verify", login = true, access = "access.config.admin|access.config.role.admin")
	public void verify() {
		String value = this.getString("value");

		JSON jo = new JSON();
		if (X.isEmpty(value)) {
			jo.put(X.STATE, 201);
			jo.put(X.MESSAGE, lang.get("name.empty.error"));
		} else {
			if (Role.loadByName(value) != null) {
				jo.put(X.STATE, 201);
				jo.put(X.MESSAGE, lang.get("name.exists.error"));
			} else {
				jo.put(X.STATE, 200);
			}
		}

		this.send(jo);
	}

	@Path(path = "cleanup", login = true, access = "access.config.admin", oplog = true)
	public void cleanup() {

		Map<String, List<Access>> m1 = Access.load();
		for (String g : m1.keySet()) {
			List<Access> l1 = m1.get(g);
			if (!lang.has("group." + g)) {
				for (Access a : l1) {
					Access.dao.delete(a.getName());
				}
			} else {
				for (Access a : l1) {
					if (!lang.has(a.getName())) {
						Access.dao.delete(a.getName());
					}
				}
			}
		}

		this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("save.success")));
	}

	/**
	 * Edits the.
	 */
	@Path(path = "edit", login = true, access = "access.config.admin|access.config.role.admin", oplog = true)
	public void edit() {
		if (method.isPost()) {

			long id = this.getLong("id");
			String name = this.getString("name");
			Role r = Role.dao.load(id);
			if (r != null) {

				if (Role.dao.update(id,
						V.create("name", name).append("seq", this.getInt("seq")).append("url", this.getHtml("url"))
								.append("menu", this.getHtml("menu")).append("memo", this.getString("memo"))) > 0) {

					String s = this.getHtml("access");
					String[] accesses = X.split(s, ";");
					r.setAccess(accesses);

				} else {
					this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE, lang.get("save.failed")));
					return;
				}
			}

			this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("save.success")));
			return;

		} else {

			long id = this.getLong("id");
			Role r = Role.dao.load(id);
			this.set("r", r);

			JSON jo = r.json();
			this.copy(jo);

			List<?> l1 = r.getAccesses();
			this.set("access", X.join(l1, ";"));

			Map<String, List<Access>> bs = Access.load();
			this.set("accesses", bs);

			this.show("/admin/role.edit.html");
		}

	}

	/**
	 * Delete.
	 */
	@Path(path = "delete", login = true, access = "access.config.admin|access.config.role.admin", oplog = true)
	public void delete() {
		String ids = this.getString("id");
		int updated = 0;
		if (ids != null) {
			String[] ss = ids.split(",");
			for (String s : ss) {
				long id = X.toLong(s);
				Role r = Role.dao.load(id);
				int i = Role.dao.delete(id);
				if (i > 0) {
					updated += i;
					GLog.oplog.info(this, "delete", r.getName());
				}
			}
		}

		if (updated > 0) {
			this.set(X.MESSAGE, lang.get("delete.success"));
		} else {
			this.set(X.ERROR, lang.get("select.required"));
		}

		onGet();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model.onGet()
	 */
	@Path(login = true, access = "access.config.admin|access.config.role.admin")
	public void onGet() {

		int s = this.getInt("s");
		int n = this.getInt("n", X.ITEMS_PER_PAGE);

		Beans<Role> bs = Role.load(s, n);
		bs.count();

		this.pages(bs, s, n);

		this.show("/admin/role.index.html");
	}

	@Path(path = "access", login = true, access = "access.config.debug")
	public void access() {
		int s = this.getInt("s");
		int n = this.getInt("n", 10);

		Beans<Access> bs = Access.dao.load(W.create().sort(X.ID, 1), s, n);
		bs.count();

		this.pages(bs, s, n);

		this.show("/admin/role.access.html");
	}

	@Path(path = "accessdelete", login = true, access = "access.config.debug")
	public void accessdelete() {
		String id = this.getString("id");
		Access.dao.delete(id);

		this.send(JSON.create().append(X.STATE, 200));
	}

}
