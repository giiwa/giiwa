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
import org.giiwa.core.bean.UID;
import org.giiwa.framework.web.*;

/**
 * web api: /admin/task <br>
 * used to manage task,<br>
 * required "access.config.admin"
 * 
 * @author joe
 *
 */
public class sysstat extends Model {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model.onGet()
	 */
	@Path(login = true, access = "access.config.admin")
	public void onGet() {
		this.redirect("/admin/sysstat/cpu");
	}

	@Path(path = "cpu", login = true, access = "access.config.admin")
	public void cpu() {

		try {
			this.set("cpuperc", Host.getCpuPerc());
			this.set("id", "cpustat" + UID.random());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		this.show("/admin/sysstat.cpu.html");
	}

	@Path(path = "cpu/list", login = true, access = "access.config.admin")
	public void cpu_list() {

		try {
			this.set("cpuperc", Host.getCpuPerc());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		this.show("/admin/sysstat.cpu.list.html");
	}

	@Path(path = "net", login = true, access = "access.config.admin")
	public void net() {

		try {
			this.set("list", Host.getIfstats());
			this.set("id", "netstat" + UID.random());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		this.show("/admin/sysstat.net.html");
	}

	@Path(path = "net/list", login = true, access = "access.config.admin")
	public void net_list() {

		try {
			this.set("list", Host.getIfstats());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		this.show("/admin/sysstat.net.list.html");
	}

	@Path(path = "disk", login = true, access = "access.config.admin")
	public void disk() {

		try {
			this.set("list", Host.getDisks());
			this.set("id", "diskstat" + UID.random());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		this.show("/admin/sysstat.disk.html");
	}

	@Path(path = "disk/list", login = true, access = "access.config.admin")
	public void disk_list() {

		try {
			this.set("list", Host.getDisks());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		this.show("/admin/sysstat.disk.list.html");
	}

	@Path(path = "netstat", login = true, access = "access.config.admin")
	public void netstat() {

		try {
			this.set("stat", Host.getNetStat());
			this.set("list", Host.getNetStats());
			this.set("id", "netstat" + UID.random());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		this.show("/admin/sysstat.netstat.html");
	}

}
