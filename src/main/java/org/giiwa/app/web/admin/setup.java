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

import org.giiwa.dao.Helper;
import org.giiwa.web.Controller;
import org.giiwa.web.Path;

/**
 * web api: /setup <br>
 * used to the initial configure, once configured, it will not be accessed
 * 
 * @author joe
 *
 */
public class setup extends Controller {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * the default GET handler. <br>
	 * /setup
	 */
	@Path()
	public void onGet() {

		try {
			if (Helper.isConfigured()) {
				this.redirect("/");
				return;
			}

			this.set("home", Controller.GIIWA_HOME);
			this.show("/admin/setup.html");

		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
			this.error(e1);
		}

	}

}
