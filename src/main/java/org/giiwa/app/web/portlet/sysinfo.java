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
package org.giiwa.app.web.portlet;

import java.util.Properties;

import org.giiwa.bean.Node;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.Helper;
import org.giiwa.dao.Helper.W;
import org.giiwa.misc.Host;
import org.giiwa.web.Controller;
import org.giiwa.web.Module;

public class sysinfo extends portlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void get() {

		this.set("uptime", lang.format(Controller.UPTIME, "yy-MM-dd HH:mm:ss"));
		this.set("now", lang.format(Global.now(), "yyyy-MM-dd HH:mm:ss"));
		this.set("past", lang.past(Controller.UPTIME));
		this.set("node", Local.id());
		this.set("gnode", Global.id());
		this.set("release", Module.load("default").getVersion());
		this.set("build", Module.load("default").getBuild());
		this.set("free", lang.size(Runtime.getRuntime().freeMemory()));
		this.set("total", lang.size(Runtime.getRuntime().totalMemory()));
		this.set("cpus", Runtime.getRuntime().availableProcessors());

		this.set("dbstats", Helper.primary.stats(null));
		this.set("cores",
				Node.dao.sum("cores", W.create().and("updated", Global.now() - Node.LOST, W.OP.gte)));
//		this.set("dfile", JSON.create().append("free", Disk.getFreeSpace()).append("used",
//				Disk.getTotalSpace() - Disk.getFreeSpace()));

		this.set("ip", this.ipPath());

		Properties props = System.getProperties();
		this.set("jdkversion", props.getProperty("java.version"));
		this.set("jdkvendor", props.getProperty("java.vendor"));

		try {
			this.set("totalmemory", lang.size(Host.getMem().getTotal()));
		} catch (Throwable e) {
			// TODO, ignore
//			log.error(e.getMessage(), e);
		}

		this.show("/portlet/sysinfo.html");
	}

}
