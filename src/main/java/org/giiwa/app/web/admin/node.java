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
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.core.json.JSON;
import org.giiwa.framework.bean.*;
import org.giiwa.framework.web.*;
import org.giiwa.mq.MQ;

/**
 * web api: /admin/node <br>
 * used to manage user<br>
 * required "access.user.admin"
 * 
 * @author joe
 *
 */
public class node extends Controller {

	/**
	 * Delete.
	 */
	@Path(path = "delete", login = true, access = "access.config.admin")
	public void delete() {

		JSON jo = new JSON();

		String id = this.getString("id");
		Node.dao.delete(id);
		jo.put(X.STATE, 200);

		this.response(jo);

	}

	@Path(path = "power", login = true, access = "access.config.admin")
	public void power() {

		JSON jo = new JSON();

		String id = this.getString("id");
		int power = this.getInt("power");

		try {

			Global.setConfig("node." + id, power);

			MQ.topic("giiwa.state",
					org.giiwa.mq.MQ.Request.create().put(JSON.create().append("node", id).append("power", power)));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		jo.put(X.STATE, 200);

		this.response(jo);

	}

	@Path(path = "update", login = true, access = "access.config.admin")
	public void update() {

		String label = this.getString("label");
		String id = this.getString("id");
		Node.dao.update(id, V.create("label", label));

		this.response(JSON.create().append(X.STATE, 200));

	}

	@Path(path = "stat", login = true, access = "access.config.admin")
	public void stat() {

		String id = this.getString("id");

		Beans<Stat> bs = Stat.load("node.load", Stat.TYPE.snapshot, Stat.SIZE.min, W.create().and("dataid", id)
				.and("time", System.currentTimeMillis() - X.AWEEK, W.OP.gte).sort("time", 1), 0, 24 * 60 * 7);

		this.set("list", bs);
		this.show("/admin/node.stat.html");

	}

	@Path(path = "clean", login = true, access = "access.config.admin")
	public void clean() {
		JSON jo = JSON.create();

		Node.dao.delete(W.create());
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
	@Path(login = true, access = "access.config.admin")
	public void onGet() {

		W q = W.create().sort("label", 1).sort("ip", 1);

		int s = this.getInt("s");
		int n = this.getInt("n", 10);

		String name = this.getString("name");
		if (!X.isEmpty(name)) {
			W q1 = W.create();
			q1.or("label", name, W.OP.like);
			q1.or("ip", name, W.OP.like);
			q1.or("id", name, W.OP.like);
			q.and(q1);
			this.set("name", name);
		}

		Beans<Node> bs = Node.dao.load(q, s, n);
		bs.count();

		this.set(bs, s, n);

		this.show("/admin/node.index.html");
	}

	@Path(login = true, path = "usage", access = "access.config.admin")
	public void usage() {

		W q = W.create().sort("label", 1).sort("ip", 1);

		int s = this.getInt("s");
		int n = this.getInt("n", 50);

		Beans<Node> bs = Node.dao.load(q, s, n);
		bs.count();

		this.set(bs, s, n);

		this.show("/admin/node.usage.html");
	}

	@Path(login = true, path = "threads", access = "access.config.admin")
	public void threads() {

		W q = W.create().sort("label", 1).sort("ip", 1);

		int s = this.getInt("s");
		int n = this.getInt("n", 50);

		Beans<Node> bs = Node.dao.load(q, s, n);
		bs.count();

		this.set(bs, s, n);

		this.show("/admin/node.threads.html");
	}

	@Path(login = true, path = "tcpestablished", access = "access.config.admin")
	public void tcpestablished() {

		W q = W.create().sort("label", 1).sort("ip", 1);

		int s = this.getInt("s");
		int n = this.getInt("n", 50);

		Beans<Node> bs = Node.dao.load(q, s, n);
		bs.count();

		this.set(bs, s, n);

		this.show("/admin/node.tcpestablished.html");
	}

	@Path(login = true, path = "tcpclosewait", access = "access.config.admin")
	public void tcpclosewait() {

		W q = W.create().sort("label", 1).sort("ip", 1);

		int s = this.getInt("s");
		int n = this.getInt("n", 50);

		Beans<Node> bs = Node.dao.load(q, s, n);
		bs.count();

		this.set(bs, s, n);

		this.show("/admin/node.tcpclosewait.html");
	}

	@Path(login = true, path = "running", access = "access.config.admin")
	public void running() {

		W q = W.create().sort("label", 1).sort("ip", 1);

		int s = this.getInt("s");
		int n = this.getInt("n", 50);

		Beans<Node> bs = Node.dao.load(q, s, n);
		bs.count();

		this.set(bs, s, n);

		this.show("/admin/node.running.html");
	}

	@Path(login = true, path = "pending", access = "access.config.admin")
	public void pending() {

		W q = W.create().sort("label", 1).sort("ip", 1);

		int s = this.getInt("s");
		int n = this.getInt("n", 50);

		Beans<Node> bs = Node.dao.load(q, s, n);
		bs.count();

		this.set(bs, s, n);

		this.show("/admin/node.pending.html");
	}

}
