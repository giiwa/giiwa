package org.giiwa.app.web;

import org.giiwa.framework.web.Controller;
import org.giiwa.framework.web.Path;

public class helo extends Controller {

	@Path(path = "test")
	public void test() {

		String name = this.getString("name");
		this.set("name", name);

		this.show("/helo.html");

	}

}
