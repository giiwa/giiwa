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
import org.giiwa.framework.bean.Node;
import org.giiwa.framework.web.*;

/**
 * web api: /admin/syslog <br>
 * used to manage oplog,<br>
 * required "access.logs.admin"
 * 
 * @author joe
 *
 */
public class syslog extends Model {

	/**
	 * Deleteall.
	 */
	@Path(path = "deleteall", login = true, access = "access.config.admin|access.config.logs.admin")
	public void deleteall() {
		JSON jo = new JSON();
		int i = GLog.dao.delete(W.create());
		GLog.oplog.warn(syslog.class, "deleteall", "deleted=" + i, login, this.getRemoteHost());
		jo.put(X.STATE, 200);
		this.response(jo);
	}

	private W getW(JSON jo) {

		W q = W.create();

		if (!X.isEmpty(jo.get("op"))) {
			q.and("op", jo.get("op"));
		}
		if (!X.isEmpty(jo.get("ip"))) {
			q.and("ip", jo.getString("ip"));
		}

		if (!X.isEmpty(jo.get("type"))) {
			q.and("type1", X.toInt(jo.get("type")));
		}

		if (!X.isEmpty(jo.get("level"))) {
			q.and("level", X.toInt(jo.get("level")));
		}

		if (!X.isEmpty(jo.getString("node"))) {
			Node n = Node.dao.load(W.create("label", jo.getString("node")));
			if (n != null) {
				q.and("node", n.getId());
			} else {
				q.and("node", "-1");
			}
		}

		if (!X.isEmpty(jo.getString("model"))) {
			q.and("model", jo.getString("model"));
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

		Beans<GLog> bs = GLog.dao.load(w.sort("created", -1), s, n);
		this.set(bs, s, n);

		this.query.path("/admin/syslog");
		this.show("/admin/syslog.index.html");
	}

	@Path(path = "detail", login = true, access = "access.config.admin|access.config.logs.admin")
	public void detail() {
		String id = this.getString("id");

		if (!X.isEmpty(id)) {
			GLog d = GLog.dao.load(id);
			this.set("b", d);
			this.set("id", id);
		} else {
			long prev = this.getLong("prev");
			if (prev > 0) {
				GLog d = GLog.dao.load(W.create().and("created", prev, W.OP.lt).sort("created", -1));
				if (d != null) {
					this.set("b", d);
					this.set("id", d.get(X.ID));
				}
			} else {
				long next = this.getLong("next");
				GLog d = GLog.dao.load(W.create().and("created", next, W.OP.gt).sort("created", 1));
				if (d != null) {
					this.set("b", d);
					this.set("id", d.get(X.ID));
				}
			}
		}

		this.show("/admin/syslog.detail.html");
	}

}
