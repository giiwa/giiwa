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

import javax.servlet.http.HttpServletResponse;

import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.bean.X;
import org.giiwa.core.json.JSON;
import org.giiwa.framework.bean.*;
import org.giiwa.framework.web.*;

/**
 * web api: /admin/token <br>
 * used to manage user<br>
 * required "access.user.admin"
 * 
 * @author joe
 *
 */
public class token extends Controller {

	/**
	 * Delete.
	 */
	@Path(path = "delete", login = true, access = "access.config.admin|access.config.user.admin")
	public void delete() {

		JSON jo = new JSON();

		String id = this.getString("id");
		AuthToken.dao.delete(id);
		jo.put(X.STATE, 200);

		this.response(jo);

	}

	@Path(path = "clean", login = true, access = "access.config.admin|access.config.user.admin")
	public void clean() {
		JSON jo = JSON.create();

		AuthToken.dao.delete(W.create());
		jo.put(X.STATE, HttpServletResponse.SC_OK);
		jo.put(X.MESSAGE, "ok");

		this.response(jo);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model.onGet()
	 */
	@Override
	@Path(login = true, access = "access.config.admin|access.config.user.admin")
	public void onGet() {

		String name = this.getString("name");
		W q = W.create();
		if (X.isEmpty(this.path) && !X.isEmpty(name)) {

			q.or("sid", name, W.OP.like);
			q.or("token", name, W.OP.like);
			q.or("uid", X.toLong(name));

			this.set("name", name);
		}

		int s = this.getInt("s");
		int n = this.getInt("n", 10);

		Beans<AuthToken> bs = AuthToken.dao.load(q, s, n);
		this.set(bs, s, n);

		this.query.path("/admin/token");

		this.show("/admin/token.index.html");
	}

}
