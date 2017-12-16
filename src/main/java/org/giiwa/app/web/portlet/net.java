package org.giiwa.app.web.portlet;

import org.giiwa.core.base.Host;

public class net extends portlet {

	@Override
	public void get() {
		try {
			this.set("net", Host.getIfstats());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		this.show("/porlet/net.html");
	}

	@Override
	public void setup() {
	}

}
