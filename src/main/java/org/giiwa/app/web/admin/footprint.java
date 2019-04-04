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
import org.giiwa.framework.bean.Footprint;
import org.giiwa.framework.bean.GLog;
import org.giiwa.framework.web.*;

/**
 * web api: /admin/footprint <br>
 * used to manage footprint,<br>
 * required "access.logs.admin"
 * 
 * @author joe
 *
 */
public class footprint extends Model {

	/**
	 * Deleteall.
	 */
	@Path(path = "deleteall", login = true, access = "access.config.admin")
	public void deleteall() {
		JSON jo = new JSON();
		int i = Footprint.dao.delete(W.create());
		GLog.oplog.warn(footprint.class, "deleteall", "deleted=" + i, login, this.getRemoteHost());
		jo.put(X.STATE, 200);
		this.response(jo);
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

		this.set("currentpage", s);

		W q = W.create();
		String table = this.getString("table");
		if (!X.isEmpty(table)) {
			q.and("table", table);
			this.set("table", table);
		}

		String dataid = this.getString("dataid");
		if (!X.isEmpty(dataid)) {
			q.and("dataid", dataid);
			this.set("dataid", dataid);
		}

		String field = this.getString("field");
		if (!X.isEmpty(field)) {
			q.and("field", field);
			this.set("field", field);
		}

		Beans<Footprint> bs = Footprint.dao.load(q.sort("created", -1), s, n);
		this.set(bs, s, n);

		this.query.path("/admin/footprint");
		this.show("/admin/footprint.index.html");
	}

	@Path(path = "detail", login = true, access = "access.config.admin")
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
