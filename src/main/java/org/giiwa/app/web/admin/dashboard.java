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

import org.giiwa.core.base.Host;
import org.giiwa.core.conf.Local;
import org.giiwa.framework.bean.Repo;
import org.giiwa.framework.web.*;

/**
 * web api: /admin/dashboard <br>
 * used to show dashboard
 * 
 * @author yjiang
 * 
 */
public class dashboard extends Model {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model#onGet()
	 */
	@Override
	@Path(login = true)
	public void onGet() {

		this.set("me", this.getUser());
		this.set("uptime", lang.format(Model.UPTIME, "yy-MM-dd HH:mm:ss"));
		this.set("now", lang.format(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss"));
		this.set("past", lang.past(Model.UPTIME));
		this.set("node", Local.id());
		this.set("release", Module.load("default").getVersion());
		this.set("build", Module.load("default").getBuild());
		this.set("free", lang.size(Runtime.getRuntime().freeMemory()));
		this.set("total", lang.size(Runtime.getRuntime().totalMemory()));
		this.set("diskspeed", Repo.getSpeed() / 1024 / 1024);
		this.set("cpus", Runtime.getRuntime().availableProcessors());
		try {
			this.set("totalmemory", lang.size(Host.getMem().getTotal()));
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}

		if (login.hasAccess("access.config.admin")) {
			try {
				this.set("mem", Host.getMem());
				this.set("disk", Host.getDisks());
				this.set("net", Host.getIfstats());
			} catch (Throwable e) {
				log.error(e.getMessage(), e);
			}
		}
		show("admin/dashboard.html");
	}

	public String eclipse(String path) {
		return path.replaceAll("\\\\", "/");
	}
}
