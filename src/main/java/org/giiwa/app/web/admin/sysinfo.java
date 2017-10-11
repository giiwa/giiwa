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
import org.giiwa.framework.web.*;

/**
 * web api: /admin/task <br>
 * used to manage task,<br>
 * required "access.config.admin"
 * 
 * @author joe
 *
 */
public class sysinfo extends Model {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model#onGet()
	 */
	@Path(login = true, access = "access.config.admin")
	public void onGet() {
		this.redirect("/admin/sysinfo/cpu");
	}

	@Path(path = "cpu", login = true, access = "access.config.admin")
	public void cpu() {

		try {
			this.set("cpuinfo", Host.getCpuInfo()[0]);
			this.set("cpuperc", Host.getCpuPerc());
			this.set("os", Host.getOS());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		this.show("/admin/sysinfo.cpu.html");
	}

	@Path(path = "process", login = true, access = "access.config.admin")
	public void process() {

		try {
			this.set("list", Host.getProcess());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		this.show("/admin/sysinfo.process.html");
	}

	@Path(path = "net", login = true, access = "access.config.admin")
	public void net() {

		try {
			this.set("list", Host.getIfaces());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		this.show("/admin/sysinfo.net.html");
	}

	@Path(path = "route", login = true, access = "access.config.admin")
	public void route() {

		try {
			this.set("list", Host.getRoutes());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		this.show("/admin/sysinfo.route.html");
	}

	@Path(path = "disk", login = true, access = "access.config.admin")
	public void disk() {

		try {
			this.set("list", Host.getDisks());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		this.show("/admin/sysinfo.disk.html");
	}

}
