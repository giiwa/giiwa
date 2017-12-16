package org.giiwa.app.web.portlet;

import org.giiwa.core.base.Host;
import org.giiwa.core.conf.Local;
import org.giiwa.framework.bean.Repo;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Module;

public class sysinfo extends portlet {

	@Override
	public void get() {

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

		this.show("/portlet/sysinfo.html");
	}

}
