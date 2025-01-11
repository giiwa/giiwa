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

import org.giiwa.app.task.AutodeployTask;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.web.Path;

public class autodeploy extends org.giiwa.app.web.admin.setting {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void set() {

		Global.setConfig("autodeploy.url", this.getString("url"));
		Local.setConfig("autodeploy.enabled", X.isSame("on", this.getString("enabled")) ? 1 : 0);
		Local.setConfig("autodeploy.modules", this.getString("modules"));
		Local.setConfig("autodeploy.timerange", this.getString("autodeploy.timerange"));

		int interval = this.getInt("autodeploy.interval");
		if (interval < 1) {
			interval = 60;
		}

		Local.setConfig("autodeploy.interval", interval);

		AutodeployTask.inst.schedule(0);

		this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE, lang.get("save.success")));
	}

	@Override
	public void get() {
		this.settingPage("/admin/autodeploy.setting.html");
	}

	@Path(login = true, access = "access.config.admin")
	public void run() {
		AutodeployTask.inst.schedule(0);
		this.set(X.MESSAGE, lang.get("autodeploy.task.start")).send(200);
	}

}
