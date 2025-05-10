package org.giiwa.demo.web;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.app.web.admin.setting;
import org.giiwa.bean.License;
import org.giiwa.demo.web.admin.demosetting;
import org.giiwa.web.IListener;
import org.giiwa.web.Module;

public class DemoListener implements IListener {

	static Log log = LogFactory.getLog(DemoListener.class);

	public void onInit(Configuration conf, Module m) {
		log.warn("demo is initing...");

		m.setLicense(License.LICENSE.free, "modulecode");

		setting.register("demosetting", demosetting.class);

	}

	@Override
	public void onStart(Configuration conf, Module m) {
		// TODO Auto-generated method stub
		log.warn("demo is starting ...");

	}

	@Override
	public void onStop() {
		// TODO Auto-generated method stub
		log.warn("demo is stopping ...");

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
