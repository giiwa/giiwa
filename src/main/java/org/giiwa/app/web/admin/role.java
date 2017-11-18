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

import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.bean.X;
import org.giiwa.core.json.JSON;
import org.giiwa.framework.bean.*;
import org.giiwa.framework.web.*;

/**
 * web api: /admin/role <br>
 * used toe manage role,<br>
 * required "access.role.admin"
 * 
 * @author joe
 *
 */
public class role extends Model {

	/**
	 * Adds the.
	 */
	@Path(path = "create", login = true, access = "access.config.admin|access.config.role.admin")
	public void create() {
		if (method.isPost()) {

			String name = this.getString("name");
			String memo = this.getString("memo");
			long id = Role.create(name, memo);
			if (id > 0) {
				String[] access = this.getStrings("access");
				if (access != null) {
					for (String s : access) {
						Role.setAccess(id, s);
					}
				}

				this.set(X.MESSAGE, lang.get("save.success"));
			} else {
				this.set("name", name);
				this.set(X.ERROR, lang.get("save.failed"));
			}

			onGet();
			return;
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

		this.response(jo);
	}

	/**
	 * Edits the.
	 */
	@Path(path = "edit", login = true, access = "access.config.admin|access.config.role.admin")
	public void edit() {
		if (method.isPost()) {

			long id = this.getLong("id");
			String name = this.getString("name");
			Role r = Role.dao.load(id);
			if (r != null) {
				if (Role.dao.update(id, V.create("name", name).set("memo", this.getString("memo"))) > 0) {
					this.path = null;
					this.set(X.MESSAGE, lang.get("save.success"));

					String[] accesses = this.getStrings("access");
					r.setAccess(accesses);

				} else {
					this.set("name", name);
					this.set(X.ERROR, lang.get("save.failed"));
				}
			} else {
				this.set("name", name);
				this.set(X.ERROR, lang.get("save.failed"));
			}

			this.set(X.MESSAGE, lang.get("save.success"));

			onGet();
			return;

		} else {

			long id = this.getLong("id");
			Role r = Role.dao.load(id);
			this.set("r", r);

			JSON jo = new JSON();
			r.toJSON(jo);
			this.set(jo);

			Map<String, List<Access>> bs = Access.load();
			this.set("accesses", bs);

			this.show("/admin/role.edit.html");
		}

	}

	/**
	 * Delete.
	 */
	@Path(path = "delete", login = true, access = "access.config.admin|access.config.role.admin")
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
					GLog.oplog.info(role.class, "delete", r.getName(), null, login, this.getRemoteHost());
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
	 * @see org.giiwa.framework.web.Model#onGet()
	 */
	@Path(login = true, access = "access.config.admin|access.config.role.admin")
	public void onGet() {

		int s = this.getInt("s");
		int n = this.getInt("n", X.ITEMS_PER_PAGE, "items.per.page");

		Beans<Role> bs = Role.load(s, n);
		bs.setTotal((int) Helper.count(W.create(), Role.class));
		this.set(bs, s, n);

		this.query.path("/admin/role");

		this.show("/admin/role.index.html");
	}

}
