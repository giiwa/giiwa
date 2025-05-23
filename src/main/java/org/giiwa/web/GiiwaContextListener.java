/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.web;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Temp;
import org.giiwa.cache.Cache;
import org.giiwa.conf.Config;
import org.giiwa.dao.Helper;
import org.giiwa.dao.X;
import org.giiwa.misc.Shell;
import org.giiwa.server.Server;
import org.giiwa.task.Task;
import org.giiwa.web.view.View;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

/**
 * the {@code giiwaContextListener} Class listen the life listener of tomcat
 * 
 * @author yjiang
 * 
 */
public class GiiwaContextListener implements ServletContextListener {

	static Log log = LogFactory.getLog(GiiwaContextListener.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletContextListener.contextDestroyed(javax.servlet.
	 * ServletContextEvent)
	 */
	public void contextDestroyed(ServletContextEvent arg) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletContextListener.contextInitialized(javax.servlet
	 * .ServletContextEvent)
	 */
	public void contextInitialized(ServletContextEvent event) {

//		String home = event.getServletContext().getRealPath("/");

		log.warn("context init, event=" + event + ", this=" + this);
		init(event.getServletContext());

		// log.info("GiiwaContextListener inited.");

	}

	public static boolean INITED = false;

	public final synchronized static void init(ServletContext servletContext) {

		log.warn("context init, event=" + servletContext);

		if (INITED)
			return;

		INITED = true;

		try {

			Thread.currentThread().setName("main");

			Controller.GIIWA_HOME = System.getenv("GIIWA_HOME");

			if (X.isEmpty(Controller.GIIWA_HOME)) {
				System.out.println("ERROR, did not set GIIWA_HOME, please set GIIWA_HOME=[path of web container]");
				System.exit(-1);
				return;
			}

			System.out.println("initing, giiwa.home=" + Controller.GIIWA_HOME);

			File f1 = new File(Controller.GIIWA_HOME + "/modules/default/view/init/upgrade/upgrade.sh");
			if (f1.exists()) {
				try {
					Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
					perms.add(PosixFilePermission.OWNER_READ);
					perms.add(PosixFilePermission.OWNER_WRITE);
					perms.add(PosixFilePermission.OWNER_EXECUTE);
					Files.setPosixFilePermissions(Paths.get(f1.getAbsolutePath()), perms);

					log.warn("upgrade shell ...");
					Shell.run(f1.getAbsolutePath(), X.AMINUTE);

				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}

			System.setProperty("home", Controller.GIIWA_HOME);

			/**
			 * initialize the configuration
			 */
			{
				File config = new File("/data/etc/giiwa.properties");
				if (config.exists()) {
					Config.init(config);
				} else {
					Config.init(new File(Controller.GIIWA_HOME + "/giiwa.properties"));
				}
			}

			GiiwaServlet.s️ervletContext = servletContext;
			GiiwaServlet.INITED = true;

			System.out.println("giiwa is initing ...");
			log.info("giiwa is initing ...");

			Configuration conf = Config.getConf();

			// TO fix a bug, giiwa.properties may store the "home"
			conf.setProperty("home", Controller.GIIWA_HOME);

			/**
			 * init task first
			 */
			Task.init(conf.getInt("thread.number", 20));

			/**
			 * initialize the helper, including RDB and Mongo
			 */
			Helper.init2(conf);

			/**
			 * initialize the cache
			 */
			Cache.init(conf);

			/**
			 * initialize the temp
			 */
			Temp.init(conf);

			/**
			 * initialize the controller, this MUST place in the end !:-)
			 */
			Controller.init(conf);

			View.init();

			log.warn("giiwa is ready for service, modules=" + Module.getAll(true) + ", top=" + Module.getHome());

			Server.startup();

			log.warn("started!");

		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			e.printStackTrace();
		}

	}

}
