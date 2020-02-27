//begin of the Model
package org.giiwa.demo.web.admin;

import org.giiwa.dao.Beans;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.dao.X;
import org.giiwa.demo.bean.Demo;
import org.giiwa.json.JSON;
import org.giiwa.web.Controller;
import org.giiwa.web.Path;

public class demo extends Controller {

	@Path(login = true, access = "access.demo.admin")
	public void onGet() {

		// get the parameters from the request
		int s = this.getInt("s");
		int n = this.getInt("n", 10);

		// create the query condition
		W q = Demo.dao.query();
		String name = this.getString("name");

		if (!X.isEmpty(name)) {
			q.and("name", name, W.OP.like);
			this.put("name", name);
		}

		// load data from database
		try {
			Beans<Demo> bs = q.load(s, n);

			// set the data in the model
			this.set(bs, s, n);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		// show (print) the list HTML page
		this.show("/admin/demo.index.html");
	}

	@Path(path = "detail", login = true, access = "access.demo.admin")
	public void detail() {
		// get the parameter from the request
		long id = this.getLong("id");

		// load data from the database
		Demo d = Demo.dao.load(id);

		// set the data in model
		this.put("b", d);
		this.put("id", id);

		// show (print) the detail HTML page
		this.show("/admin/demo.detail.html");
	}

	@Path(path = "delete", login = true, access = "access.demo.admin")
	public void delete() {
		// get the parameter from the request
		long id = this.getLong("id");

		// delete the data from the database
		Demo.dao.delete(id);

		// response (print) the json to response
		this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("delete.success")));
	}

	@Path(path = "create", login = true, access = "access.demo.admin")
	public void create() {

		// is POST method?
		if (method.isPost()) {

			// create the value object
			V v = V.create();
			v.set("name", this.getString("name"));

			// insert the data into database
			Demo.create(v);

			// response(print) the json to response
			this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("save.success")));
			return;
		}

		// show (print) the create HTML page
		this.show("/admin/demo.create.html");
	}

	// web api: /admin/demo/edit?id=
	@Path(path = "edit", login = true, access = "access.demo.admin")
	public void edit() {
		// get the parameter from the request
		long id = this.getLong("id");

		// is POST method?
		if (method.isPost()) {

			// create the value object
			V v = V.create();
			v.set("name", this.getString("name"));

			// update the data in database
			Demo.dao.update(id, v);

			// response (print) the json to response
			this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("save.success")));
			return;
		}

		// load the data from the database
		Demo d = Demo.dao.load(id);

		// set the data in Model
		this.copy(d.json());
		this.put("id", id);

		// show (print) the edit HTML page
		this.show("/admin/demo.edit.html");
	}

}
// end of the Model
