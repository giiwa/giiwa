package org.giiwa.app.web;

import java.util.List;

import org.giiwa.bean.Role;
import org.giiwa.bean.Roles;
import org.giiwa.bean.User;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.X;
import org.giiwa.web.Controller;
import org.giiwa.web.Path;

public class index extends Controller {

	@Path()
	public void onGet() {

		User u = this.user();
		if (u != null) {
			Roles rs = u.getRole();
			if (rs != null) {
				List<Role> l1 = rs.getList();
				if (l1 != null && !l1.isEmpty()) {
					for (Role r : l1) {
						if (!X.isEmpty(r.url)) {
							this.head("due", "role//" + r.id);
							this.redirect(r.url);
							return;
						}
					}
				}
			}
			if (u.hasAccess("access.config.admin")) {
				this.redirect("/admin/");
				return;
			}
		}

		String h1 = Local.getString("home.uri.1", null);
		if (X.isEmpty(h1)) {
			h1 = Global.getString("home.uri", X.EMPTY);
			if (!X.isEmpty(h1)) {
				this.head("due", "global");
			}
		} else {
			this.head("due", "node");
		}

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
