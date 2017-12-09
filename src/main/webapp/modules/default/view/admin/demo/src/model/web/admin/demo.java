package org.giiwa.demo.web.admin;

import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.X;
import org.giiwa.core.json.JSON;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.demo.bean.Demo;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;

public class demo extends Model {

	@Path(login = true, access = "access.demo.admin")
	public void onGet() {
		int s = this.getInt("s");
		int n = this.getInt("n", 10);

		W q = W.create();
		String name = this.getString("name");

		if (!X.isEmpty(name) && X.isEmpty(path)) {
			q.and("name", name, W.OP.like);
			this.set("name", name);
		}
		Beans<Demo> bs = Demo.dao.load(q, s, n);
		this.set(bs, s, n);

		this.show("/admin/demo.index.html");
	}

	@Path(path = "detail", login = true, access = "access.demo.admin")
	public void detail() {
		String id = this.getString("id");
		Demo d = Demo.dao.load(id);
		this.set("b", d);
		this.set("id", id);
		this.show("/admin/demo.detail.html");
	}

	@Path(path = "delete", login = true, access = "access.demo.admin")
	public void delete() {
		String id = this.getString("id");
		Demo.dao.delete(id);
		JSON jo = new JSON();
		jo.put(X.STATE, 200);
		this.response(jo);
	}

	@Path(path = "create", login = true, access = "access.demo.admin")
	public void create() {
		if (method.isPost()) {
			JSON jo = this.getJSON();
			V v = V.create().copy(jo, "name");
			v.set("content", this.getHtml("content"));
			String id = Demo.create(v);

			this.response(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("save.success")));
			return;
		}

		this.show("/admin/demo.create.html");
	}

	@Path(path = "edit", login = true, access = "access.demo.admin")
	public void edit() {
		String id = this.getString("id");
		if (method.isPost()) {
			JSON jo = this.getJSON();
			V v = V.create().copy(jo, "name");
			v.set("content", this.getHtml("content"));
			Demo.dao.update(id, v);

			this.response(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("save.success")));
			return;
		}

		Demo d = Demo.dao.load(id);
		this.set(d.getJSON());
		this.set("id", id);
		this.show("/admin/demo.edit.html");
	}

}
