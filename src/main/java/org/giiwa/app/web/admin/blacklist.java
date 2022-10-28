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

import org.giiwa.bean.*;
import org.giiwa.dao.Beans;
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
public class blacklist extends Controller {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Adds the.
	 */
	@Path(path = "create", login = true, access = "access.config.admin")
	public void create() {
		if (method.isPost()) {

			String ip = this.getString("ip");
			if (X.isEmpty(ip)) {
				this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE, lang.get("bad.param.ip")));
				return;
			}

			try {

				if (Blacklist.dao.exists(W.create().and("id", ip))) {
					this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE, lang.get("bad.param.ip")));
					return;
				}

				V v = V.create();
				v.append("ip", ip);
				v.append("url", this.getHtml("url"));
				v.append("memo", this.get("memo"));
				v.append("times", 0);

				Blacklist.create(v);

				this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("save.success")));
				return;

			} catch (Exception e) {
				log.error(e.getMessage(), e);

				this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE, e.getMessage()));
				return;
			}

		}

		this.show("/admin/blacklist.create.html");
	}

	@Path(path = "edit", login = true, access = "access.config.admin")
	public void edit() {

		long id = this.getLong("id");

		if (method.isPost()) {

			V v = V.create();

			v.append("memo", this.getString("memo"));
			v.append("url", this.getHtml("url"));

			Blacklist.dao.update(id, v);

			this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("save.success")));
			return;

		}

		Blacklist a = Blacklist.dao.load(id);
		this.set("a", a);

		this.show("/admin/blacklist.edit.html");
	}

	/**
	 * Delete.
	 */
	@Path(path = "delete", login = true, access = "access.config.admin")
	public void delete() {

		JSON jo = new JSON();

		long id = this.getLong("id");
		Blacklist.dao.delete(id);
		jo.put(X.STATE, 200);

		this.send(jo);

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
		if (!X.isEmpty(name)) {
			q.or("ip", name, W.OP.like).or("url", name, W.OP.like);
			this.put("name", name);
		}

		int s = this.getInt("s");
		int n = this.getInt("n", X.ITEMS_PER_PAGE);

		Beans<Blacklist> bs = Blacklist.dao.load(q, s, n);
		bs.count();
		this.pages(bs, s, n);

		this.set("ip", this.ip());

		this.show("/admin/blacklist.index.html");
	}

}
