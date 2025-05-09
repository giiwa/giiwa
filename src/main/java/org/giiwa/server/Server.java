package org.giiwa.server;

import java.io.File;

import org.apache.commons.configuration2.Configuration;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.ServerConnector;
import org.giiwa.conf.Config;
import org.giiwa.dao.X;
import org.giiwa.misc.IOUtil;
import org.giiwa.web.Controller;
import org.giiwa.web.GiiwaServlet;

public class Server {

	private static boolean STARTED = false;

	public static void main(String[] args) {

		startup();

	}

	public synchronized static void startup() {

		if (STARTED) {
			return;
		}

		STARTED = true;

		_upgrade();

		System.out.println("init ... " + Server.class.getClassLoader());
		_init();

		System.out.println("startup ...");
		_startup();

	}

	private static void _startup() {

		try {
			Configuration conf = Config.getConf();

			// Server
			org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server();

//			server.setServerInfo("giiwa(3.3)");

			// fix bug
			X.IO.delete(new File("/\""));
			//end of fix

			HttpConfiguration httpconf = new HttpConfiguration();
			httpconf.setSendServerVersion(false);

			// HTTP connector
			ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(httpconf));
			http.setHost("0.0.0.0");
			http.setPort(conf.getInt("http.port", 8080));
			http.setIdleTimeout(30000);

			// 设置 connector
			server.addConnector(http);

			ServletContextHandler context = new ServletContextHandler();
			context.setContextPath("/");
			context.setWelcomeFiles(new String[] { "index", "index.html", "index.htm" });

			// Add servlet to produce output
			context.addServlet(org.giiwa.web.GiiwaServlet.class, "/*");

			GiiwaServlet.s️ervletContext = context.getServletContext();

			server.setHandler(context);

			// 启动 Server
			server.start();

			server.join();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean INITED = false;

	private static void _init() {

		if (INITED)
			return;

		INITED = true;

		try {

			Thread.currentThread().setName("main");

			org.giiwa.web.Controller.GIIWA_HOME = System.getenv("GIIWA_HOME");

			if (org.giiwa.dao.X.isEmpty(org.giiwa.web.Controller.GIIWA_HOME)) {
				System.out.println("ERROR, did not set GIIWA_HOME, please set GIIWA_HOME=[path of web container]");
				System.exit(-1);
				return;
			}

			System.out.println("initing, giiwa.home=" + org.giiwa.web.Controller.GIIWA_HOME);

			System.setProperty("home", org.giiwa.web.Controller.GIIWA_HOME);

			/**
			 * initialize the configuration
			 */
			{
				org.giiwa.conf.Config.init(new File(org.giiwa.web.Controller.GIIWA_HOME + "/conf/giiwa.properties"));
			}

//			GiiwaServlet.s️ervletContext = servletContext;
			org.giiwa.web.GiiwaServlet.INITED = true;

			System.out.println("giiwa is initing ...");

			org.apache.commons.configuration2.Configuration conf = org.giiwa.conf.Config.getConf();

			// TO fix a bug, giiwa.properties may store the "home"
			conf.setProperty("home", org.giiwa.web.Controller.GIIWA_HOME);

			/**
			 * init task first
			 */
			org.giiwa.task.Task.init(conf.getInt("thread.number", 20));

			/**
			 * initialize the helper, including RDB and Mongo
			 */
			org.giiwa.dao.Helper.init2(conf);

			/**
			 * initialize the cache
			 */
			org.giiwa.cache.Cache.init(conf);

			/**
			 * initialize the temp
			 */
			org.giiwa.bean.Temp.init(conf);

			/**
			 * initialize the controller, this MUST place in the end !:-)
			 */
			org.giiwa.web.Controller.init(conf);

			org.giiwa.web.view.View.init();

		} catch (Throwable e) {
			e.printStackTrace();
		}

	}

	private static void _upgrade() {

		try {

			Controller.GIIWA_HOME = System.getenv("GIIWA_HOME");
			if (Controller.GIIWA_HOME == null || "".equals(Controller.GIIWA_HOME)) {
				System.err.println("GIIWA_HOME 没有设置!");
				System.exit(0);
			}

			// upgrade from <20250307
			File f1 = new File(Controller.GIIWA_HOME + "/giiwa.properties");
			if (f1.exists()) {
				System.err.println("moving giiwa.properties to conf/");
				f1.renameTo(new File(Controller.GIIWA_HOME + "/conf/giiwa.properties"));
			}
			f1 = new File("/data/etc/giiwa.properties");
			if (f1.exists()) {
				System.err.println("moving giiwa.properties to conf/");
				f1.renameTo(new File(Controller.GIIWA_HOME + "/conf/giiwa.properties"));
			}

			f1 = new File(Controller.GIIWA_HOME + "/log4j.properties");
			if (f1.exists()) {
				System.err.println("moving log4j.properties to conf/");
				f1.renameTo(new File(Controller.GIIWA_HOME + "/conf/log4j.properties"));
			}
			f1 = new File("/data/etc/log4j.properties");
			if (f1.exists()) {
				System.err.println("moving log4j.properties to conf/");
				f1.renameTo(new File(Controller.GIIWA_HOME + "/conf/log4j.properties"));
			}

			// upgrade from <20250307
			f1 = new File(Controller.GIIWA_HOME + "/modules/WEB-INF/lib/");
			if (f1.exists()) {
				// copy all WEB-INF/lib/* to lib
				File[] ff = f1.listFiles();
				for (File f : ff) {
					if (f.isFile() && f.getName().endsWith(".jar")) {
						IOUtil.copy(f, new File(Controller.GIIWA_HOME + "/lib/" + f.getName()));
					}
				}
				IOUtil.delete(f1.getParentFile());
			}

			// copy startup.sh

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
