package org.giiwa.demo.web;

import org.giiwa.web.Controller;
import org.giiwa.web.Path;

/**
 * web api: /demo
 * 
 * @author joe
 * 
 */
public class demo extends Controller {

	@Path()
	public void onGet() {

		// TODO

		this.show("/demo.html");
	}

	@Path(path = "hello")
	public void hello() {

		// TODO

		this.put("time", lang.format(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss"));
		this.show("/demo.html");
	}

}
