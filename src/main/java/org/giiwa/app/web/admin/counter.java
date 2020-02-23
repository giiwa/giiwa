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
import org.giiwa.core.bean.X;
import org.giiwa.core.json.JSON;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.framework.bean.Counter;
import org.giiwa.framework.web.*;

/**
 * web api: /admin/syslog <br>
 * used to manage oplog,<br>
 * required "access.logs.admin"
 * 
 * @author joe
 *
 */
public class counter extends Controller {

	/**
	 * Deleteall.
	 */
	@Path(path = "deleteall", login = true, access = "access.config.admin")
	public void deleteall() {

		W q = W.create();
		String name = this.getString("name");
		if (!X.isEmpty(name)) {
			q.and(W.create().or("node", name, W.OP.like).or("name", name));
		}

		Counter.dao.delete(q);

		this.response(JSON.create().append(X.STATE, 200));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model.onGet()
	 */
	@Path(login = true, access = "access.config.admin")
	public void onGet() {

		int s = this.getInt("s");
		int n = this.getInt("n", 10);

		W q = W.create();
		String name = this.getString("name");
		if (!X.isEmpty(name)) {
			q.and(W.create().or("node", name, W.OP.like).or("name", name));
			this.set("name", name);
		}
		Beans<Counter> bs = Counter.dao.load(q.sort("node", 1).sort("name", 1), s, n);
		bs.count();

		this.set(bs, s, n);

		this.query().path("/admin/counter");
		this.show("/admin/counter.index.html");
	}

}
