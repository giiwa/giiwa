package org.giiwa.app.web.portlet;

import org.giiwa.core.base.Host;

public class disk extends portlet {

	@Override
	public void get() {
		try {
			this.set("disk", Host.getDisks());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		this.show("/portlet/disk.html");
	}

	public String eclipse(String path) {
		return path.replaceAll("\\\\", "/");
	}

}
