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

import java.util.ArrayList;
import java.util.List;

import org.giiwa.bean.m._DiskIO;
import org.giiwa.bean.m._Net;
import org.giiwa.conf.Local;
import org.giiwa.dao.X;
import org.giiwa.misc.Shell;
import org.giiwa.dao.Helper.W;
import org.giiwa.web.*;

/**
 * web api: /admin/dashboard <br>
 * used to show dashboard
 * 
 * @author yjiang
 * 
 */
public class dashboard extends Controller {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model.onGet()
	 */
	@Override
	@Path(login = true)
	public void onGet() {

		String _node = this.getString("__node");
		if (X.isEmpty(_node)) {
			_node = Local.id();
		}
		this.set("__node", _node);

		login = this.user();
		if (login != null && !X.isEmpty(login.getString("desktop"))) {
			if (!X.isSame("/admin/dashboard", login.getString("desktop"))) {
				this.redirect(login.getString("desktop"));
				return;
			}
		}

		this.set("me", login);

		this.set("pid", Shell.pid());
		this.set("uptime", Controller.UPTIME);

		if (login != null && login.hasAccess("access.config.admin")) {

			this.set("nets",
					_Net.dao.load(W.create().and("node", Local.id())
							.and("updated", System.currentTimeMillis() - X.AMINUTE * 10, W.OP.gte).sort("inet", 1), 0,
							100));

			this.set("disks",
					_DiskIO.dao.load(W.create().and("node", Local.id())
							.and("updated", System.currentTimeMillis() - X.AMINUTE * 10, W.OP.gte).sort("path", 1), 0,
							100));

			this.set("portlets", portlets);

			this.show("/admin/dashboard.html");

		} else if (X.isSame("/admin/dashboard", HOME)) {

			this.show("/admin/dashboard.html");
		} else {
			this.redirect(HOME);
		}

	}

	@Path(login = true, path = "info")
	public void info() {

		this.set("pid", Shell.pid());
		
		this.set("uptime", Controller.UPTIME);

		if (login != null && login.hasAccess("access.config.admin")) {

			this.set("nets", _Net.dao.load(W.create().and("node", Local.id()).sort("inet", 1), 0, 100));

			this.show("/admin/dashboard.html");

		} else if (X.isSame("/admin/dashboard", HOME)) {
			this.show("/admin/dashboard.html");
		} else {
			this.redirect(HOME);
		}
	}

	public static void desk(String url) {
		if (!desks.contains(url)) {
			desks.add(url);
		}
	}

	public static void portlet(String url) {
		if (X.isSame(url, "br")) {
			portlets.add(url);
		} else {
			if (url.indexOf("?") < 0) {
				url = url + "?";
			}
			if (!portlets.contains(url)) {
				portlets.add(url);
			}
		}
	}

	private static String HOME = "/admin/dashboard";
	public static List<String> desks = new ArrayList<String>();
	private static List<String> portlets = new ArrayList<String>();

}
