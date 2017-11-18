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
import org.giiwa.framework.bean.GLog;
import org.giiwa.framework.bean.User;
import org.giiwa.framework.web.*;

/**
 * web api: /admin/log <br>
 * used to manage oplog,<br>
 * required "access.logs.admin"
 * 
 * @author joe
 *
 */
public class oplog extends Model {

	/**
	 * Deleteall.
	 */
	@Path(path = "deleteall", login = true, access = "access.config.admin|access.config.logs.admin")
	public void deleteall() {
		JSON jo = new JSON();
		int i = GLog.cleanup();
		GLog.oplog.warn(oplog.class, "deleteall", "deleted=" + i, login, this.getRemoteHost());
		jo.put(X.STATE, 200);
		this.response(jo);
	}

	private W getW(JSON jo) {

		W q = W.create();

		if (!X.isEmpty(jo.get("op"))) {
			q.and("op", jo.get("op"));
		}
		if (!X.isEmpty(jo.get("ip"))) {
			q.and("ip", jo.getString("ip"), W.OP.like);
		}
		if (!X.isEmpty(jo.get("user"))) {

			String name = this.getString("user");
			W q1 = W.create().and(W.create().and("nickname", name, W.OP.like).or("name", name, W.OP.like)).sort("name",
					1);
			Beans<User> bs = User.load(q1, 0, 100);
			if (bs != null && !bs.isEmpty()) {
				W q2 = W.create();
				for (User u : bs) {
					q2.or("uid", u.getId());
				}
				q.and(q2);
			} else {
				// user not found
				q.and("uid", -2);
			}
		}
		if (!X.isEmpty(jo.get("type"))) {
			q.and("type", X.toInt(jo.get("type")));
		}

		if (!X.isEmpty(jo.get("level"))) {
			q.and("level", X.toInt(jo.get("level")));
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
			long today_2 = System.currentTimeMillis() - X.ADAY * 2;
			jo.put("starttime", lang.format(today_2, "yyyy-MM-dd"));
			q.and(X.CREATED, today_2, W.OP.gte);
		}

		if (!X.isEmpty(jo.getString("endtime"))) {
			q.and(X.CREATED, lang.parse(jo.getString("endtime"), "yyyy-MM-dd"), W.OP.lte);
		}

		int sortby_type = this.getInt("sortby_type", -1);
		q.sort(X.CREATED, sortby_type);
		this.set(jo);

		return q;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model#onGet()
	 */
	@Path(login = true, access = "access.config.admin|access.config.logs.admin")
	public void onGet() {

		int s = this.getInt("s");
		int n = this.getInt("n", X.ITEMS_PER_PAGE, "items.per.page");

		this.set("currentpage", s);

		JSON jo = this.getJSON();
		W w = getW(jo);

		Beans<GLog> bs = GLog.dao.load(w, s, n);
		this.set(bs, s, n);

		this.query.path("/admin/oplog");
		this.show("/admin/oplog.index.html");
	}

	@Path(path = "detail", login = true, access = "access.config.admin|access.config.logs.admin")
	public void detail() {
		String id = this.getString("id");
		GLog d = GLog.dao.load(id);
		this.set("b", d);
		this.set("id", id);
		this.show("/admin/oplog.detail.html");
	}

}
