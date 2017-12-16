package org.giiwa.app.web.portlet;

import org.giiwa.core.base.Host;

public class mem extends portlet {

	@Override
	public void get() {
		try {
			this.set("mem", Host.getMem());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		this.show("/portlet/mem.html");
	}

	@Override
	public void setup() {
	}

}
