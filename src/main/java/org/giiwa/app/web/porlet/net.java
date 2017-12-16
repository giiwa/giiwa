package org.giiwa.app.web.porlet;

import org.giiwa.core.base.Host;

public class net extends porlet {

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
