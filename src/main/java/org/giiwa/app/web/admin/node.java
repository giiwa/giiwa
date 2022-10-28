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

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.giiwa.bean.*;
import org.giiwa.dao.Beans;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.dao.UID;
import org.giiwa.json.JSON;
import org.giiwa.net.mq.MQ;
import org.giiwa.task.Monitor;
import org.giiwa.task.Task;
import org.giiwa.web.*;

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
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Delete.
	 */
	@Path(path = "delete", login = true, access = "access.config.admin")
	public void delete() {

		JSON jo = new JSON();

		String id = this.getString("id");
		Node n = Node.dao.load(id);
		if (n != null) {
			Node.dao.delete(id);
			GLog.oplog.warn("node", "delete", n.label + "/" + n.id, login, node.this.ip());
		}

		jo.put(X.STATE, 200);

		this.send(jo);

	}

	@Path(path = "power", login = true, access = "access.config.admin")
	public void power() {

		JSON jo = new JSON();

		String id = this.getString("id");
		int power = this.getInt("power");
		Node n = Node.dao.load(id);

		if (n != null) {
			try {

				MQ.topic("giiwa.state", org.giiwa.net.mq.MQ.Request.create()
						.put(JSON.create().append("node", id).append("power", power)));
				jo.put(X.STATE, 200);
				jo.put(X.MESSAGE, lang.get("sent.node.power." + power));
				GLog.oplog.info("node", "power", n.label + ", power=" + power, login, node.this.ip());
			} catch (Exception e) {
				GLog.oplog.error("node", "power", e.getMessage(), login, node.this.ip());
				jo.put(X.STATE, 201);
				jo.put(X.MESSAGE, e.getMessage());
			}
		}

		this.send(jo);

	}

	@Path(path = "update", login = true, access = "access.config.admin")
	public void update() {

		String label = this.getString("label");
		String id = this.getString("id");
		Node.dao.update(id, V.create("label", label));

		GLog.oplog.error(node.class, "update", label + "/" + id, login, node.this.ip());

		this.send(JSON.create().append(X.STATE, 200));

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

		W q = W.create().sort("label", 1).sort("ip", 1);

		int s = this.getInt("s");
		int n = this.getInt("n", 10);

		String name = this.getString("name");
		if (!X.isEmpty(name)) {
			W q1 = W.create();
			q1.or("label", name, W.OP.like);
			q1.or("ip", name, W.OP.like);
//			q1.or("id", name, W.OP.like);
			q.and(q1);
			this.set("name", name);
		}

		Beans<Node> bs = Node.dao.load(q, s, n);
		bs.count();

		this.pages(bs, s, n);

		this.show("/admin/node.index.html");
	}

	@SuppressWarnings("serial")
	@Path(path = "add", login = true, access = "access.config.admin")
	public void add() {

		if (method.isPost()) {

			String host = this.getString("host");
			if (X.isEmpty(host)) {
				this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE, "ssh missed!"));
				return;
			}

			String user = this.getString("user");
			if (X.isEmpty(user)) {
				this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE, "user missed!"));
				return;
			}

			String passwd = this.getHtml("passwd");
			if (X.isEmpty(passwd)) {
				this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE, "passwd missed!"));
				return;
			}

			String l2 = this.getString("l2");

			String alias = this.getString("alias");

			String access = UID.random(10);

			long tid = Monitor.start(new Task() {

				String message;
				int state = 0;

				@Override
				public void onExecute() {
					Node.add(host, user, passwd, l2, alias, (state, s) -> {
						this.message = s;
						this.state = state;
						Monitor.flush(this);
					});
					Monitor.flush(this);
					if (state == 200) {
						GLog.oplog.warn(node.class, "add", message, login, node.this.ip());
					} else {
						GLog.oplog.error(node.class, "add", message, login, node.this.ip());
					}
				}

			}, access);

			this.send(JSON.create().append(X.STATE, 200).append("url", "/f/t/state?id=" + tid + "&access=" + access));

			return;
		}

		List<org.giiwa.web.Module> l1 = org.giiwa.web.Module.getAll(true);
		List<String> l2 = X.asList(l1, e -> {
			String name = ((org.giiwa.web.Module) e).getName();
			if (X.isSame(name, "default")) {
				return null;
			}
			return name;
		});

		this.set("l2", X.join(l2, ","));

		this.show("/admin/node.add.html");

	}

}
