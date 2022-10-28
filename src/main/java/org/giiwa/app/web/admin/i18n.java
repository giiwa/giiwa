package org.giiwa.app.web.admin;

import java.util.Map;

import org.giiwa.web.Controller;
import org.giiwa.web.Path;

/**
 * some setting of the module
 * 
 * @author joe
 *
 */
public class i18n extends Controller {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Path(login = true, access = "access.config.admin|access.config.module.admin")
	public void onGet() {
		Map<String, String> missed = lang.getMissed();
		StringBuilder sb = new StringBuilder();
		for (String n : missed.keySet()) {
			sb.append(n).append("=").append("<br>");
		}
		if (sb.length() > 0) {
			this.set("missed", sb.toString());
		}

		Map<String, String[]> d = lang.getData();
		this.set("d", d);
		this.show("/admin/i18n.index.html");

	}

}
