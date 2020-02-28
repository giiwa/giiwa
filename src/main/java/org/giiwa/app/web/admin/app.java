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

import java.util.Arrays;

import org.giiwa.bean.*;
import org.giiwa.dao.Beans;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.web.*;

/**
 * web api: /admin/app <br>
 * used to manage appid<br>
 * 
 * @author joe
 *
 */
public class app extends Controller {

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
					V v = V.create();
					v.append("appid", appid);
					v.append("secret", secret);
					String expired = this.getString("expired");
					if (!X.isEmpty(expired)) {
						v.append("expired", lang.parse(expired, "yyyy-MM-dd HH:mm"));
					}
					v.append("memo", this.getString("memo"));
					v.append("access", Arrays.asList(X.split(this.getHtml("access"), "[,;\r\n]")));

					App.create(v);

					this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("save.success")));
					return;
				}
				this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE, lang.get("appid.exists")));
				return;

			} catch (Exception e) {
				log.error(e.getMessage(), e);

				this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE, e.getMessage()));
				return;
			}

		}

		this.show("/admin/app.create.html");
	}

	@Path(path = "edit", login = true, access = "access.config.admin")
	public void edit() {

		long id = this.getLong("id");

		if (method.isPost()) {

			V v = V.create();
			String expired = this.getString("expired");
			if (!X.isEmpty(expired)) {
				v.append("expired", lang.parse(expired, "yyyy-MM-dd HH:mm"));
			}
			v.append("memo", this.getString("memo"));
			v.append("access", Arrays.asList(X.split(this.getHtml("access"), "[,;\r\n]")));

			App.dao.update(id, v);

			this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("save.success")));
			return;

		}

		App a = App.dao.load(id);
		this.set("a", a);

		this.show("/admin/app.edit.html");
	}

	/**
	 * Delete.
	 */
	@Path(path = "delete", login = true, access = "access.config.admin")
	public void delete() {

		JSON jo = new JSON();

		long id = this.getLong("id");
		App.dao.delete(id);
		jo.put(X.STATE, 200);

		this.send(jo);

	}

	@Path(path = "reset", login = true, access = "access.config.admin")
	public void reset() {

		JSON jo = new JSON();

		String id = this.getString("id");
		App.update(id, V.create("secret", UID.random(32)));
		jo.put(X.STATE, 200);

		this.send(jo);

	}

	@Path(path = "detail", login = true, access = "access.config.admin")
	public void detail() {
		long id = this.getLong("id");
		App d = App.dao.load(id);
		this.set("b", d);
		this.set("id", id);
		this.show("/admin/bean.detail.html");
	}

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
			q.and("appid", name, W.OP.like);
			this.put("name", name);
		}

		int s = this.getInt("s");
		int n = this.getInt("n", X.ITEMS_PER_PAGE);

		Beans<App> bs = App.dao.load(q, s, n);
		bs.count();
		this.pages(bs, s, n);

		this.show("/admin/app.index.html");
	}

	@Path(path = "help")
	public void help() {
		this.show("/admin/app.help.html");
	}

}
