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

import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;
import org.giiwa.core.json.JSON;
import org.giiwa.framework.bean.*;
import org.giiwa.framework.web.*;

/**
 * web api: /admin/app <br>
 * used to manage appid<br>
 * 
 * @author joe
 *
 */
public class app extends Model {

	/**
	 * Adds the.
	 */
	@Path(path = "create", login = true, access = "access.config.admin")
	public void create() {
		if (method.isPost()) {
			String appid = this.getString("appid");
			try {
				if (!App.dao.exists(W.create("appid", appid))) {
					String secret = UID.random(32);
					App.create(App.Param.create().appid(appid).role(this.getLong("role")).secret(secret)
							.memo(this.getString("memo")).build());

					this.response(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("save.success")));
					return;
				}
				this.response(JSON.create().append(X.STATE, 201).append(X.MESSAGE, lang.get("appid.exists")));
				return;
			} catch (Exception e) {
				log.error(e.getMessage(), e);

				this.response(JSON.create().append(X.STATE, 201).append(X.MESSAGE, e.getMessage()));
				return;
			}

		}

		Beans<Role> rs = Role.load(0, 1000);
		this.set("roles", rs);

		this.show("/admin/app.create.html");
	}

	/**
	 * Delete.
	 */
	@Path(path = "delete", login = true, access = "access.config.admin")
	public void delete() {

		JSON jo = new JSON();

		String appid = this.getString("id");
		if (!X.isEmpty(appid)) {
			App.delete(appid);
			jo.put(X.STATE, 200);
		} else {
			jo.put(X.MESSAGE, lang.get("delete.failed"));
		}

		this.response(jo);

	}

	@Path(path = "reset", login = true, access = "access.config.admin")
	public void reset() {

		JSON jo = new JSON();

		String id = this.getString("id");
		App.update(id, App.Param.create().secret(UID.random(32)).build());
		jo.put(X.STATE, 200);

		this.response(jo);

	}

	@Path(path = "detail", login = true, access = "access.config.admin")
	public void detail() {
		long id = this.getLong("id");
		App d = App.dao.load(id);
		this.set("b", d);
		this.set("id", id);
		this.show("/admin/app.detail.html");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model#onGet()
	 */
	@Override
	@Path(login = true, access = "access.config.admin")
	public void onGet() {

		String name = this.getString("name");
		W q = W.create();
		if (X.isEmpty(this.path) && !X.isEmpty(name)) {
			q.and("appid", name, W.OP.like);
			this.set("name", name);
		}

		int s = this.getInt("s");
		int n = this.getInt("n", X.ITEMS_PER_PAGE, "items.per.page");

		Beans<App> bs = App.dao.load(q, s, n);
		this.set(bs, s, n);

		this.query.path("/admin/app");

		this.show("/admin/app.index.html");
	}

	@Path(path = "help")
	public void help() {
		this.show("/admin/app.help.html");
	}

}
