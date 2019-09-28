package org.giiwa.demo.web;

import org.giiwa.framework.web.Controller;
import org.giiwa.framework.web.Path;

/**
 * web api: /demo
 * 
 * @author joe
 * 
 */
public class demo extends Controller {

	@Path()
	public void onGet() {
		// TODO do something
		this.show("/demo.html");
	}

	@Path(path = "hello")
	public void hello() {
		// TODO do something
		this.set("time", lang.format(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss"));
		this.show("/demo.html");
	}

}
