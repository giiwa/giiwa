/*
 * Copyright 2015 Giiwa, Inc. and/or its affiliates.
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
package org.giiwa.framework.web;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.commons.configuration.Configuration;
import org.giiwa.core.bean.*;
import org.giiwa.core.cache.Cache;
import org.giiwa.core.conf.*;
import org.giiwa.core.db.DB;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.Repo;
import org.giiwa.framework.bean.Temp;
import org.giiwa.framework.web.Model.HTTPMethod;

// TODO: Auto-generated Javadoc
/**
 * the {@code giiwaServlet} Class handles all the request, and dispatch to
 * appropriate {@code Model}
 * 
 * @author yjiang
 * 
 */
public class GiiwaServlet extends HttpServlet {

	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	protected static ServletConfig config;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.
	 * HttpServletRequest , javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Controller.dispatchWithContextPath(req.getRequestURI(), req, resp, new HTTPMethod(Model.METHOD_GET));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.
	 * HttpServletRequest , javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Controller.dispatchWithContextPath(req.getRequestURI(), req, resp, new HTTPMethod(Model.METHOD_POST));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {
		GiiwaServlet.config = config;

		super.init(config);

	}

	/**
	 * Shutdown.
	 */
	public static void shutdown() {
		System.out.println("shutdown the app");
		System.exit(0);
	}

	/**
	 * Restart.
	 */
	public static void restart() {
		System.out.println("restart the app");

		/**
		 * clean cache
		 */
		Module.clean();
		TemplateLoader.clean();
		// Model.clean();

		/**
		 * re-initialize
		 */
		init(home, Controller.PATH);
	}

	private static String home;

	/**
	 * Inits the.
	 * 
	 * @param home
	 *            the home
	 * @param contextPath
	 *            the context path
	 */
	public static void init(String home, String contextPath) {
		try {
			GiiwaServlet.home = home;

			Model.GIIWA_HOME = System.getenv("GIIWA_HOME");

			if (X.isEmpty(Model.GIIWA_HOME)) {
				System.out.println("ERROR, did not set GIIWA_HOME, please set GIIWA_HOME=[path of web container]");
				System.exit(-1);
			}

			System.out.println("giiwa is starting ...");
			System.out.println("giiwa.home=" + Model.GIIWA_HOME);

			System.setProperty("home", Model.GIIWA_HOME);

			/**
			 * initialize the configuration
			 */
			Local.init("home", "giiwa");

			Configuration conf = Local.getConfig();

			// TO fix a bug, giiwa.properties may store the "home"
			conf.setProperty("home", Model.GIIWA_HOME);

			/**
			 * initialize the DB connections pool
			 */
			DB.init();

			/**
			 * initialize the cache
			 */
			Cache.init(conf);

			Bean.init(conf);

			Task.init(conf.getInt("thread.number", 20), conf);

			/**
			 * initialize the controller, this MUST place in the end !:-)
			 */
			Controller.init(conf, contextPath);

			/**
			 * initialize the repo
			 */
			Repo.init(conf);

			/**
			 * initialize the temp
			 */
			Temp.init(conf);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
