package org.giiwa.app.web;

import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;

public class index extends Model {

	@Path()
	public void onGet() {
		String h1 = Global.getString("home.uri", X.EMPTY);
		if (X.isEmpty(h1)) {
			h1 = "/index.html";
		}

		String node = this.getString("__node");
		if (X.isEmpty(node)) {
			this.redirect(h1);
		} else {
			if (h1.indexOf("?") > 0) {
				this.redirect(h1 + "&__node=" + node);
			} else {
				this.redirect(h1 + "?__node=" + node);
			}
		}
	}

}
