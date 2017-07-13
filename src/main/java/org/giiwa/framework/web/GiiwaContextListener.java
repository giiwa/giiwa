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
package org.giiwa.framework.web;

import java.io.File;

import javax.servlet.*;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.X;
import org.giiwa.core.bean.helper.RDB;
import org.giiwa.core.cache.Cache;
import org.giiwa.core.conf.Config;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.Repo;
import org.giiwa.framework.bean.Temp;

// TODO: Auto-generated Javadoc
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
   * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.
   * ServletContextEvent)
   */
  public void contextDestroyed(ServletContextEvent arg) {

  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet
   * .ServletContextEvent)
   */
  public void contextInitialized(ServletContextEvent event) {

    String home = event.getServletContext().getRealPath("/");

    init(home, event.getServletContext().getContextPath());

  }

  /**
   * Inits the.
   * 
   * @param home
   *          the home
   * @param contextPath
   *          the context path
   */
  private static void init(String home, String contextPath) {
    try {

      Model.GIIWA_HOME = System.getenv("GIIWA_HOME");

      if (X.isEmpty(Model.GIIWA_HOME)) {
        System.out.println("ERROR, did not set GIIWA_HOME, please set GIIWA_HOME=[path of web container]");
        System.exit(-1);
      }

      System.out.println("giiwa is starting ...");
      System.out.println("giiwa.home=" + Model.GIIWA_HOME);

      System.setProperty("home", Model.GIIWA_HOME);

      // TODO, remove it later, the old driver will cause can not startup
      File f = new File(Model.GIIWA_HOME + "/giiwa/WEB-INF/lib/mongo-java-driver-2.10.0.jar");
      if (f.exists()) {
        f.delete();
        System.out.println("Deleteing mongo-java-driver-2.10.0.jar, it will cause startup failed.");
        System.out.println("Restart the giiwa.");
        System.exit(0);
      }

      /**
       * initialize the configuration
       */
      Config.init(new File(Model.GIIWA_HOME + "/giiwa.properties"));

      Configuration conf = Config.getConf();

      // TO fix a bug, giiwa.properties may store the "home"
      conf.setProperty("home", Model.GIIWA_HOME);

      /**
       * initialize the cache
       */
      Cache.init(conf.getString("cache.url", X.EMPTY), conf.getString("site.group", "demo"));

      /**
       * initialize the helper, including RDB and Mongo
       */
      Helper.init(conf);

      Task.init(conf.getInt("thread.number", 20));

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
