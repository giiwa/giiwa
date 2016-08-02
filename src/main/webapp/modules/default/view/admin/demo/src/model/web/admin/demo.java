package org.giiwa.demo.web.admin;

import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.X;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.demo.bean.Demo;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;

import net.sf.json.JSONObject;

public class demo extends Model {

	@Path(login = true, access = "access.demo.admin")
	public void onGet() {
		int s = this.getInt("s");
		int n = this.getInt("n", 20, "number.per.page");

		W q = W.create();
		String name = this.getString("name");

		if (!X.isEmpty(name) && path == null) {
			q.and("name", name, W.OP_LIKE);
			this.set("name", name);
		}
		Beans<Demo> bs = Demo.load(q, s, n);
		this.set(bs, s, n);

		this.show("/admin/demo.index.html");
	}

	@Path(path = "detail", login = true, access = "access.demo.admin")
	public void detail() {
		String id = this.getString("id");
		Demo d = Demo.load(id);
		this.set("b", d);
		this.set("id", id);
		this.show("/admin/demo.detail.html");
	}

	@Path(path = "delete", login = true, access = "access.demo.admin")
	public void delete() {
		String id = this.getString("id");
		Demo.delete(id);
		JSONObject jo = new JSONObject();
		jo.put(X.STATE, 200);
		this.response(jo);
	}

	@Path(path = "create", login = true, access = "access.demo.admin")
	public void create() {
		if (method.isPost()) {
			JSONObject jo = this.getJSON();
			V v = V.create().copy(jo, "name");
			v.set("content", this.getHtml("content"));
			String id = Demo.create(v);

			this.set(X.MESSAGE, lang.get("create.success"));
			onGet();
			return;
		}

		this.show("/admin/demo.create.html");
	}

	@Path(path = "edit", login = true, access = "access.demo.admin")
	public void edit() {
		String id = this.getString("id");
		if (method.isPost()) {
			JSONObject jo = this.getJSON();
			V v = V.create().copy(jo, "name");
			v.set("content", this.getHtml("content"));
			Demo.update(id, v);

			this.set(X.MESSAGE, lang.get("save.success"));
			onGet();
			return;
		}

		Demo d = Demo.load(id);
		this.set(d.getJSON());
		this.set("id", id);
		this.show("/admin/demo.edit.html");
	}

}
