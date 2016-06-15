package org.giiwa.demo.web;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.demo.bean.Demo;
import org.giiwa.framework.web.LifeListener;
import org.giiwa.framework.web.Module;

public class DemoListener implements LifeListener {

	static Log log = LogFactory.getLog(DemoListener.class);

	@Override
	public void onStart(Configuration conf, Module m) {
		// TODO Auto-generated method stub
		log.info("webdemo is starting ...");

		Demo.cleanup();

	}

	@Override
	public void onStop() {
		// TODO Auto-generated method stub

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
