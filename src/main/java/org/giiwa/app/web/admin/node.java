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

import org.giiwa.bean.*;
import org.giiwa.conf.Global;
import org.giiwa.dao.X;
import org.giiwa.dao.Beans;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.dao.UID;
import org.giiwa.json.JSON;
import org.giiwa.task.Monitor;
import org.giiwa.task.Task;
import org.giiwa.web.*;

import jakarta.servlet.http.HttpServletResponse;

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
	@Path(path = "delete", login = true, access = "access.config.admin", oplog = true, loglevel = "warn")
	public void delete() {

		String id = this.getString(X.ID);
		if (X.isEmpty(id)) {
			Node.dao.load(W.create().and(X.ID, null));
			this.set(X.ERROR, "缺少参数, [id]").send(201);
			return;
		}

		Node n = Node.dao.load(id);
		if (n == null) {
			this.set(X.ERROR, "参数错误, [id]").send(201);
			return;
		}

		log.warn("delete node, id=" + id);

		Node.dao.delete(id);
		GLog.oplog.warn(this, "delete", n.label + "/" + n.id);

		this.send(200);

	}

	@Path(path = "power", login = true, access = "access.config.admin", oplog = true)
	public void power() {

		JSON jo = new JSON();

		String id = this.getString(X.ID);
		int power = this.getInt("power");
		Node n = Node.dao.load(id);

		if (n != null) {
			try {

				n.power(power);

				jo.put(X.STATE, 200);
				jo.put(X.MESSAGE, lang.get("sent.node.power." + power));
				GLog.oplog.info(this, "power", n.label + ", power=" + power);
			} catch (Exception e) {
				GLog.oplog.error(this, "power", e.getMessage(), e);
				jo.put(X.STATE, 201);
				jo.put(X.MESSAGE, e.getMessage());
			}
		}

		this.send(jo);

	}

	@Path(path = "update", login = true, access = "access.config.admin", oplog = true)
	public void update() {

		String label = this.getString("label");
		String id = this.getString(X.ID);
		Node.dao.update(id, V.create("label", label));

		GLog.oplog.error(this, "update", label + "/" + id, null);

		this.send(JSON.create().append(X.STATE, 200));

	}

	@Path(path = "stat", login = true, access = "access.config.admin")
	public void stat() {

		String id = this.getString(X.ID);

		Beans<Stat> bs = Stat.load("node.load", Stat.TYPE.snapshot, Stat.SIZE.min,
				W.create().and("dataid", id).and("time", Global.now() - X.AWEEK, W.OP.gte).sort("time", 1), 0,
				24 * 60 * 7);

		this.set(X.LIST, bs);
		this.show("/admin/node.stat.html");

	}

	@Path(path = "clean", login = true, access = "access.config.admin", oplog = true, loglevel = "warn")
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

		W q = W.create().sort("label", 1).sort(X.IP, 1);

		int s = this.getInt(X.S);
		int n = this.getInt(X.N, X.ITEMS_PER_PAGE);

		String name = this.getString(X.NAME);
		if (!X.isEmpty(name)) {
			W q1 = W.create();
			q1.or("label", name, W.OP.like);
			q1.or(X.IP, name, W.OP.like);
//			q1.or(X.ID, name, W.OP.like);
			q.and(q1);
			this.set(X.NAME, name);
		}

		Beans<Node> bs = Node.dao.load(q, s, n);
		bs.count();

		this.pages(bs, s, n);

		this.show("/admin/node.index.html");
	}

	@Path(path = "add", login = true, access = "access.config.admin", oplog = true)
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

			try {
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
							GLog.oplog.warn(node.this, "add", message);
						} else {
							GLog.oplog.error(node.this, "add", message, null);
						}
					}

				}, access);

				this.send(
						JSON.create().append(X.STATE, 200).append("url", "/f/t/state?id=" + tid + "&access=" + access));

			} catch (Exception e) {
				log.error(e.getMessage(), e);
				this.error(e);
			}
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

	/**
	 * 远程节点注册和heartbeat
	 */
	@Path(path = "hello")
	public void hello() {
		// Token授权验证
		String token = this.get("token");
		App a = App.load("appmon");
		if (a == null) {
			this.set(X.ERROR, "没有找到[appmon]应用，请联系管理员！").send(201);
			return;
		}
		if (!X.isSame(token, a.secret)) {
			this.set(X.ERROR, "错误[token]，请联系管理员！").send(201);
			return;
		}

		if (!a.isAllow(this)) {
			this.set(X.ERROR, "禁止[" + this.ip() + "]！").send(201);
			return;
		}

		a.touch(this.ip());

		String label = this.get(X.NAME);
		V v = V.create();

		v.append(X.IP, this.ip());
		v.append(X.OS, this.get(X.OS));
		v.append("modules", X.asList(X.split(this.getHtml("modules"), "[,; ]"), s -> s));
		v.append("uptime", this.getLong("uptime"));
		v.append("cores", this.getInt("cores"));
		v.append("mem", this.getLong("mem_total"));
		v.append("_usage", X.toDouble(this.get("cpu_usage")));
		v.append("mem_usage", X.toDouble(this.get("mem_usage")));
		v.append("timestamp", this.getHtml("time"));
		v.append("tag", this.get("tag"));
		v.append("lastcheck", Global.now());
		v.append("label", label);
		v.append("disk", this.getHtml("disk"));
		v.append("ghz", X.toDouble(this.get("ghz")));

		if (Node.dao.exists2(label)) {
			Node.dao.update(label, v);
		} else {
			v.append(X.ID, label);
			Node.dao.insert(v);
		}
		this.send(200);

	}

}
