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

import org.giiwa.bean.GLog;
import org.giiwa.dao.Beans;
import org.giiwa.dao.Helper;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.task.Task;
import org.giiwa.web.*;

/**
 * web api: /admin/syslog <br>
 * used to manage oplog,<br>
 * required "access.logs.admin"
 * 
 * @author joe
 *
 */
public class glog extends Controller {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Deleteall.
	 */
	@Path(path = "deleteall", login = true, access = "access.config.admin", oplog = true)
	public void deleteall() {

		JSON jo = new JSON();

		jo.put(X.STATE, 200);
		jo.put(X.MESSAGE, lang.get("glog.deletealling"));

		Task.schedule(t -> {
			try {
				int i = GLog.dao.delete(W.create().and("created", System.currentTimeMillis() - X.ADAY, W.OP.lte));
				GLog.oplog.warn(this, "deleteall", lang.get("glog.deleteall", i));

				Helper.primary.repair(GLog.dao.tableName());
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		});

		this.send(jo);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model.onGet()
	 */
	@Path(login = true, access = "access.config.admin")
	public void onGet() {

		int s = this.getInt("s");
		int n = this.getInt("n", X.ITEMS_PER_PAGE);

		this.set("s", s);

		W q = W.create();

		String op = this.get("op");
		if (!X.isEmpty(op)) {
			q.and("op", op);
			this.set("op", op);
		}
		String ip = this.get("ip");
		if (!X.isEmpty(ip)) {
			q.and("ip", ip);
			this.set("ip", ip);
		}

		String type = this.get("type");
		if (!X.isEmpty(type)) {
			q.and("type", X.toInt(type));
			this.set("type", type);
		}

		String level = this.get("level");
		if (!X.isEmpty(level)) {
			q.and("level", X.toInt(level));
			this.set("level", level);
		}

		String node = this.get("node");
		if (!X.isEmpty(node)) {
			org.giiwa.bean.Node e = org.giiwa.bean.Node.dao.load(W.create().and("label", node));
			q.and("node", e.id);
			this.set("node", node);
		}

		String model = this.get("model");
		if (!X.isEmpty(model)) {
			q.and("model", model);
			this.set("model", model);
		}

		String startime = this.getString("startime");
		if (!X.isEmpty(startime)) {
			q.and(X.CREATED, lang.parse(startime, "yyyy-MM-dd"), W.OP.gte);
			this.set("startime", startime);

		} else {
			long today_2 = System.currentTimeMillis() - X.ADAY * 2;
			q.and(X.CREATED, today_2, W.OP.gte);
			this.set("startime", lang.format(today_2, "yyyy-MM-dd"));
		}

		String endtime = this.get("endtime");
		if (!X.isEmpty(endtime)) {
			q.and(X.CREATED, lang.parse(endtime, "yyyy-MM-dd"), W.OP.lte);
			this.set("endtime", endtime);
		}

		String thread = this.getString("thread");
		if (!X.isEmpty(thread)) {
			q.and("thread", thread);
			this.set("thread", thread);
		}

		q.sort("created", -1);

		GLog.dao.optimize(q);

		Beans<GLog> bs = GLog.dao.load(q, s, n);

		bs.setTotal(GLog.dao.count(W.create()));

		this.pages(bs, s, n);

		this.show("/admin/glog.index.html");
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

		this.show("/admin/glog.detail.html");
	}

}
