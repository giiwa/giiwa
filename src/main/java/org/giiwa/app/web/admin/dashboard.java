/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.app.web.admin;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import org.giiwa.core.bean.X;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.conf.Local;
import org.giiwa.framework.bean.m._Net;
import org.giiwa.framework.web.*;

/**
 * web api: /admin/dashboard <br>
 * used to show dashboard
 * 
 * @author yjiang
 * 
 */
public class dashboard extends Controller {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model.onGet()
	 */
	@Override
	@Path(login = true)
	public void onGet() {
		login = this.getUser();
		if (login != null && !X.isEmpty(login.getString("desktop"))) {
			if (!X.isSame("/admin/dashboard", login.getString("desktop"))) {
				this.redirect(login.getString("desktop"));
				return;
			}
		}

		this.set("me", login);

		String name = ManagementFactory.getRuntimeMXBean().getName();
		this.set("pid", X.split(name, "[@]")[0]);
		this.set("uptime", GiiwaController.UPTIME);

		if (login != null && login.hasAccess("access.config.admin")) {

			this.set("nets", _Net.dao.load(W.create("node", Local.id()).sort("inet", 1), 0, 100));

			this.show("/admin/dashboard.html");

		} else if (X.isSame("/admin/dashboard", HOME)) {
			this.show("/admin/dashboard.html");
		} else {
			this.redirect(HOME);
		}

	}

	@Path(login = true, path = "info")
	public void info() {

		String name = ManagementFactory.getRuntimeMXBean().getName();
		this.set("pid", X.split(name, "[@]")[0]);
		this.set("uptime", GiiwaController.UPTIME);

		if (login != null && login.hasAccess("access.config.admin")) {

			this.set("nets", _Net.dao.load(W.create("node", Local.id()).sort("inet", 1), 0, 100));

			this.show("/admin/dashboard.html");

		} else if (X.isSame("/admin/dashboard", HOME)) {
			this.show("/admin/dashboard.html");
		} else {
			this.redirect(HOME);
		}
	}

	public static void add(String url) {
		if (!desks.contains(url)) {
			desks.add(url);
		}
	}

	public static String HOME = "/admin/dashboard";
	public static List<String> desks = new ArrayList<String>();

}
