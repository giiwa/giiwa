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

import org.giiwa.bean.User;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.misc.Shell;
import org.giiwa.task.Task;
import org.giiwa.web.*;

/**
 * web api: /admin/system <br>
 * used to control the "system"<br>
 * required "access.config.admin"
 * 
 * @author joe
 *
 */
public class system extends Controller {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Path(path = "info")
	public void info() {

		this.send(JSON.create().append(X.STATE, 200).append("uptime", Controller.UPTIME).append("local", Local.id())
				.append("global", Global.id()).append("pid", Shell.pid()));
	}

	/**
	 * Restart.
	 */
	@Path(path = "restart", login = true, access = "access.config.admin", oplog = true)
	public void restart() {

		JSON jo = new JSON();
		User me = User.dao.load(login.getId());
		String pwd = this.getString("pwd");

		if (me.validate(pwd)) {
			jo.put("state", "ok");

			log.warn("restarted by [" + this.ipPath() + "]");

			Task.schedule(t -> {
				System.exit(0);
			}, 1000);

		} else {
			jo.put("state", "fail");
			jo.put("message", lang.get("invalid.password"));
		}

		this.send(jo);
	}

}
