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

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

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

							if (Global.getInt("web.debug", 0) == 1) {
								this.head("due", "role//" + r.id);
							}

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
				if (Global.getInt("web.debug", 0) == 1) {
					this.head("due", "global");
				}
			}
		} else {
			this.head("due", "node");
		}

		if (X.isEmpty(h1)) {
			h1 = "/index.html";
		}

		this.redirect(h1);
	}

}
