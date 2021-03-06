package org.giiwa.app.web.portlet;

import java.util.Properties;

import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.misc.Host;
import org.giiwa.web.Controller;
import org.giiwa.web.Module;

public class sysinfo extends portlet {

	@Override
	public void get() {

		this.set("uptime", lang.format(Controller.UPTIME, "yy-MM-dd HH:mm:ss"));
		this.set("now", lang.format(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss"));
		this.set("past", lang.past(Controller.UPTIME));
		this.set("node", Local.id());
		this.set("gnode", Global.id());
		this.set("release", Module.load("default").getVersion());
		this.set("build", Module.load("default").getBuild());
		this.set("free", lang.size(Runtime.getRuntime().freeMemory()));
		this.set("total", lang.size(Runtime.getRuntime().totalMemory()));
		this.set("cpus", Runtime.getRuntime().availableProcessors());

		this.set("ip", this.ipPath());

		Properties props = System.getProperties();
		this.set("jdkversion", props.getProperty("java.version"));
		this.set("jdkvendor", props.getProperty("java.vendor"));

		try {
			this.set("totalmemory", lang.size(Host.getMem().getTotal()));
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}

		this.show("/portlet/sysinfo.html");
	}

}
