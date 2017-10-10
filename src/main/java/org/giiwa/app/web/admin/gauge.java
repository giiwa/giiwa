/*/*
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

import org.giiwa.core.base.Host;
import org.giiwa.core.json.JSON;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;
import org.giiwa.framework.web.Tps;
import org.hyperic.sigar.SigarException;

/**
 * web api: /admin/gauge <br>
 * used to get "computer" status
 * 
 * @author joe
 *
 */
public class gauge extends Model {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model#onGet()
	 */
	public void onGet() {
		this.redirect("/user");
	}

	/**
	 * Cpu.
	 */
	@Path(path = "tps", login = true, accesslog = false)
	public void tps() {
		this.set("max", Tps.max());
		this.show("/admin/gauge.tps.html");
	}

	/**
	 * Cpu_status.
	 */
	@Path(path = "tps/status", login = true, accesslog = false)
	public void tps_status() {
		// todo
		JSON jo = new JSON();
		jo.put("total", Tps.get());

		this.response(jo);

	}

	/**
	 * Mem_status.
	 */
	@Path(path = "mem/status", login = true, accesslog = false)
	public void mem_status() {
		// todo
		JSON jo = new JSON();
		try {
			jo.put("used", lang.format(Host.getMem().getUsed() / 1024 / 1024 / 1024F, "%.1f"));
		} catch (SigarException e) {
			log.error(e.getMessage(), e);
		}

		this.response(jo);

	}

	/**
	 * Mem.
	 */
	@Path(path = "mem", login = true, accesslog = false)
	public void mem() {
		try {
			this.set("total", lang.format(Host.getMem().getTotal() / 1024 / 1024 / 1024F, "%.1f"));
		} catch (SigarException e) {
			log.error(e.getMessage(), e);
		}

		this.show("/admin/gauge.mem.html");
	}

	/**
	 * Disk.
	 */
	@Path(path = "disk", login = true, accesslog = false)
	public void disk() {

		try {
			List<JSON> l1 = Host.getDisks();
			for (JSON j : l1) {
				long free = j.getLong("free");
				long used = j.getLong("used");
				j.put("free", lang.format(free / 1024F / 1024 / 1024, "%.1f"));
				j.put("used", lang.format(used / 1024F / 1024 / 1024, "%.1f"));
			}
			this.set("list", l1);
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}

		this.show("/admin/gauge.disk.html");
	}

}
