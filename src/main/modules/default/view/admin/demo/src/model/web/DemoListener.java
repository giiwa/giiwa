package org.giiwa.demo.web;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.framework.bean.License;
import org.giiwa.framework.web.IListener;
import org.giiwa.framework.web.Module;

public class DemoListener implements IListener {

	static Log log = LogFactory.getLog(DemoListener.class);

	public void onInit(Configuration conf, Module m) {
		log.warn("webdemo is initing...");

		m.setLicense(License.LICENSE.free, "modulecode");

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
