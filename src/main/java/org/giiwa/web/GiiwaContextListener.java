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

import javax.servlet.*;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Temp;
import org.giiwa.cache.Cache;
import org.giiwa.conf.Config;
import org.giiwa.dao.Helper;
import org.giiwa.dao.X;
import org.giiwa.task.Task;
import org.giiwa.web.view.View;

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

		init(event.getServletContext());

		// log.info("GiiwaContextListener inited.");

	}

	public static boolean INITED = false;

	public final synchronized static void init(ServletContext servletContext) {

		if (INITED)
			return;

		INITED = true;

		try {

			Thread.currentThread().setName("main");

			Controller.GIIWA_HOME = System.getenv("GIIWA_HOME");

			if (X.isEmpty(Controller.GIIWA_HOME)) {
				System.out.println("ERROR, did not set GIIWA_HOME, please set GIIWA_HOME=[path of web container]");
				System.exit(-1);
			}

			System.out.println("giiwa.home=" + Controller.GIIWA_HOME);

			System.setProperty("home", Controller.GIIWA_HOME);

			/**
			 * initialize the configuration
			 */
			Config.init(new File(Controller.GIIWA_HOME + "/giiwa.properties"));

			GiiwaServlet.s️ervletContext = servletContext;
			GiiwaServlet.INITED = true;

			System.out.println("giiwa is initing ...");
			log.info("giiwa is initing ...");

			Configuration conf = Config.getConf();

			// TO fix a bug, giiwa.properties may store the "home"
			conf.setProperty("home", Controller.GIIWA_HOME);

			/**
			 * initialize the helper, including RDB and Mongo
			 */
			Helper.init2(conf);
//			 {
//				Helper.init(conf);
//			}

			/**
			 * initialize the cache
			 */
			Cache.init(conf.getString("cache.url", X.EMPTY), conf.getString("cache.user", X.EMPTY),
					conf.getString("cache.passwd", X.EMPTY));

			Task.init(conf.getInt("thread.number", 20));

			/**
			 * initialize the repo
			 */
//			Repo.init(conf);

			/**
			 * initialize the temp
			 */
			Temp.init(conf);

			/**
			 * initialize the controller, this MUST place in the end !:-)
			 */
			Controller.init(conf, servletContext.getContextPath());

			View.init();

			if (log.isWarnEnabled())
				log.warn("giiwa is ready for service, modules=" + Module.getAll(true) + ", top=" + Module.getHome());

		} catch (Throwable e) {
			java.util.logging.Logger.getLogger("giiwa").severe(e.getMessage());
			e.printStackTrace();
		}

	}

}
