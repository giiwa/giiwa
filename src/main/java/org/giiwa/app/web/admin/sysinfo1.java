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

import java.util.List;

import org.giiwa.dao.Helper;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.misc.Host;
import org.giiwa.web.*;

/**
 * web api: /admin/task <br>
 * used to manage task,<br>
 * required "access.config.admin"
 * 
 * @author joe
 *
 */
public class sysinfo1 extends Controller {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model.onGet()
	 */
	@Path(login = true, access = "access.config.admin")
	public void onGet() {
		this.redirect("/admin/sysinfo1/cpu");
	}

	@Path(path = "cpu", login = true, access = "access.config.admin")
	public void cpu() {

		try {
			this.set("cpuinfo", Host.getCpuInfo()[0]);
			this.set("cpuperc", Host.getCpuPerc());
			this.set("os", Host.getOS());

		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}

		log.warn("show sysinfo.cput.html");

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

	@Path(path = "db", login = true, access = "access.config.admin")
	public void db() {

		try {

			JSON stat = Helper.primary.stats(null);
			this.set("stat", stat);

			this.set("list", Helper.primary.listOp());

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		this.show("/admin/sysinfo.db.html");

	}

	@Path(path = "db/kill", login = true, access = "access.config.admin")
	public void db_kill() {

		String id = this.get("id");
		Helper.killOp(id);
		this.set(X.MESSAGE, "killed").send(200);

	}

	@Path(path = "db/killall", login = true, access = "access.config.admin")
	public void db_killall() {

		List<JSON> l1 = Helper.primary.listOp();
		for (JSON j1 : l1) {
			String id = j1.getString("opid");
			Helper.killOp(id);
		}
		this.set(X.MESSAGE, "killed").send(200);

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
