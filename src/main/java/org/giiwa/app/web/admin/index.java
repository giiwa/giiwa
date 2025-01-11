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

import org.giiwa.bean.*;
import org.giiwa.conf.Global;
import org.giiwa.dao.X;
import org.giiwa.web.*;

/**
 * web api: <a href='/admin' target='_blank'>/admin</a> <br>
 * used to show home of admin
 * 
 * @author joe
 *
 */
public class index extends Controller {

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
	@Path(login = true, method = "GET")
	public void onGet() {
		/**
		 * let's post method to handle it
		 */
		onPost();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model.onPost()
	 */
	@Path(login = true, method = "POST")
	public void onPost() {

		String ip = Global.getString("admin.ip", ".*");
		if (!X.isEmpty(ip) && !X.isIn(ip, "\\*", ".*") && !this.ipPath().matches(ip)) {
			this.deny(null, "[" + this.ipPath() + "] is denied!");
			return;
		}

		// ok
		if (login.hasAccess("access.config.admin")) {
			User me = this.user();
			/**
			 * put the user in mode
			 */
			this.put("me", me);

			/**
			 * show view ...
			 */
			this.show("/admin/index.html");
		} else {
			this.deny();
		}

	}

}
