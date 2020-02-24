package org.giiwa.demo.web;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.License;
import org.giiwa.web.IListener;
import org.giiwa.web.Module;

public class DemoListener implements IListener {

	static Log log = LogFactory.getLog(DemoListener.class);

	public void onInit(Configuration conf, Module m) {
		log.warn("webdemo is initing...");

		m.setLicense(License.LICENSE.free,
				"ApATTsoSvCA8q8cg9TGS5JHVKwrxIO91l7i3ExAp7qHi83RsGR8njf4cdFjr981SLg0IL0EWM8f/9g4+CqcTAp1Ui4DWxA/ssEzDIt2hOw2vCLVwDv+N6yKPycwfYB0gjweemYbjhJIeeYjNK+KRC/badN2+/TQxxxxgqjMrlAU=");

	}

	@Override
	public void onStart(Configuration conf, Module m) {
		// TODO Auto-generated method stub
		log.warn("webdemo is starting ...");

	}

	@Override
	public void onStop() {
		// TODO Auto-generated method stub
		log.warn("webdemo is stopping ...");

	}

	@Override
	public void uninstall(Configuration conf, Module m) {
		// TODO Auto-generated method stub

	}

	@Override
	public void upgrade(Configuration conf, Module m) {
		// TODO Auto-generated method stub

	}

}
