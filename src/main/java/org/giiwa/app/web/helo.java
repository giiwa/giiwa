package org.giiwa.app.web;

import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;

public class helo extends Model {

	@Path(path = "test")
	public void test() {

		String name = this.getString("name");
		this.set("name", name);

		this.show("/helo.html");

	}

}
