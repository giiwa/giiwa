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

import javax.servlet.http.*;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.*;
import org.giiwa.core.bean.TimeStamp;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.ConfigGlobal;
import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.Bean.V;
import org.giiwa.framework.bean.AccessLog;
import org.giiwa.framework.bean.User;
import org.giiwa.framework.web.Model.HTTPMethod;

// TODO: Auto-generated Javadoc
/**
 * load module, default module
 * 
 * @author yjiang
 * 
 */
public class Controller {

  static Log                log    = LogFactory.getLog(Controller.class);

  static private Controller owner  = new Controller();

  /**
   * the configured context path
   */
  public static String      PATH;

  /**
   * os info
   */
  public static String      OS;

  /**
   * uptime of the app
   */
  public final static long  UPTIME = System.currentTimeMillis();

  /**
   * the length of context path, to make the cut faster :-)
   */
  private static int        prelen = 0;

  /**
   * Inits the.
   * 
   * @param conf
   *          the conf
   * @param path
   *          the path
   */
  public static void init(Configuration conf, String path) {
    Controller.PATH = path;
    Controller.prelen = path == null ? 0 : path.length();

    OS = System.getProperty("os.name").toLowerCase() + "_" + System.getProperty("os.version") + "_"
        + System.getProperty("os.arch");

    Model.HOME = conf.getString("home") + "/giiwa";
    Bean.DEBUG = X.isSame(ConfigGlobal.s("type", "debug"), "debug");

    /**
     * initialize the module
     */
    Module.init(conf);

  }

  /**
   * Gets the model.
   *
   * @param method
   *          the method
   * @param uri
   *          the uri
   * @return the model
   */
  public static Model getModel(int method, String uri) {
    return Module.home.loadModel(method, uri);
  }

  /**
   * Dispatch with context path.
   * 
   * @param uri
   *          the uri
   * @param req
   *          the req
   * @param resp
   *          the resp
   * @param method
   *          the method
   */
  public static void dispatchWithContextPath(String uri, HttpServletRequest req, HttpServletResponse resp,
      HTTPMethod method) {

    /**
     * check the model in the cache, if exists then do it first to repad
     * response
     */
    // log.debug(uri);

    if (prelen > 0) {
      uri = uri.substring(prelen);
    }

    dispatch(uri, req, resp, method);
  }

  static private String getRemoteHost(HttpServletRequest req) {
    String remote = req.getHeader("X-Forwarded-For");
    if (remote == null) {
      remote = req.getHeader("X-Real-IP");

      if (remote == null) {
        remote = req.getRemoteAddr();
      }
    }

    return remote;
  }

  /**
   * Dispatch.
   * 
   * @param uri
   *          the uri
   * @param req
   *          the req
   * @param resp
   *          the resp
   * @param method
   *          the method
   */
  public static void dispatch(String uri, HttpServletRequest req, HttpServletResponse resp, Model.HTTPMethod method) {

    TimeStamp t = TimeStamp.create();

    uri = uri.replaceAll("//", "/");

    /**
     * test and load from cache first
     */
    Model mo = Module.home.loadModelFromCache(method.method, uri);
    if (mo != null) {

      Path p = mo.dispatch(uri, req, resp, method);

      if (p == null || p.accesslog()) {
        if (log.isInfoEnabled())
          log.info(
              method + " " + uri + " - " + mo.getStatus() + " - " + t.past() + "ms -" + mo.getRemoteHost() + " " + mo);

        V v = V.create("method", method.toString()).set("cost", t.past()).set("sid", mo.sid());
        User u1 = mo.login;// getUser();
        if (u1 != null) {
          v.set("uid", u1.getId()).set("username", u1.get("name"));
        }
        if (Bean.DEBUG)
          AccessLog.create(mo.getRemoteHost(), uri, v.set("status", mo.getStatus()).set("client", mo.browser()));
      }

      // Counter.max("web.request.max", t.past(), uri);
      return;
    }

    if ("/".equals(uri) || uri.endsWith("/")) {
      uri += "index";
    }

    /**
     * looking for the model
     */
    try {
      /**
       * load model from the modules
       */
      mo = getModel(method.method, uri);
      if (mo != null) {
        Path p = mo.dispatch(uri, req, resp, method);

        if (p == null || p.accesslog()) {
          if (log.isInfoEnabled())
            log.info(method + " " + uri + " - " + mo.getStatus() + " - " + t.past() + "ms -" + mo.getRemoteHost() + " "
                + mo);

          V v = V.create("method", method.toString()).set("cost", t.past()).set("sid", mo.sid());
          User u1 = mo.login;// getUser();
          if (u1 != null) {
            v.set("uid", u1.getId()).set("username", u1.get("name"));
          }
          // if (method.isMdc()) {
          // v.set("request",
          // mo.getJSONNonPassword().toString()).set("response",
          // mo.getOutput());
          // }
          if (Bean.DEBUG)
            AccessLog.create(mo.getRemoteHost(), uri, v.set("status", mo.getStatus()).set("client", mo.browser()));
        }

        // Counter.max("web.request.max", t.past(), uri);
        return;
      } else {
        /**
         * looking for sub path
         */
        mo = getModel(method.method, uri + "/index");
        if (mo != null) {
          Path p = mo.dispatch(uri, req, resp, method);

          if (p == null || p.accesslog()) {
            if (log.isInfoEnabled())
              log.info(method + " " + uri + " - " + mo.getStatus() + " - " + t.past() + "ms -" + mo.getRemoteHost()
                  + " " + mo);
            V v = V.create("method", method.toString()).set("cost", t.past()).set("sid", mo.sid());
            User u1 = mo.login;// getUser();
            if (u1 != null) {
              v.set("uid", u1.getId()).set("username", u1.get("name"));
            }
            // if (method.isMdc()) {
            // v.set("request",
            // mo.getJSONNonPassword().toString()).set("response",
            // mo.getOutput());
            // }
            if (Bean.DEBUG)
              AccessLog.create(mo.getRemoteHost(), uri, v.set("status", mo.getStatus()).set("client", mo.browser()));
          }

          // Counter.max("web.request.max", t.past(), uri);
          return;
        } else {
          /**
           * get back of the uri, and set the path to the model if found, and
           * the path instead
           */
          int i = uri.lastIndexOf("/");
          while (i > 0) {
            String path = uri.substring(i + 1);
            String u = uri.substring(0, i);
            mo = getModel(method.method, u);
            if (mo != null) {
              mo.setPath(path);
              Path p = mo.dispatch(u, req, resp, method);

              if (p == null || p.accesslog()) {
                if (log.isInfoEnabled())
                  log.info(method + " " + uri + " - " + mo.getStatus() + " - " + t.past() + "ms -" + mo.getRemoteHost()
                      + " " + mo);

                V v = V.create("method", method.toString()).set("cost", t.past()).set("sid", mo.sid());
                User u1 = mo.login;// getUser();
                if (u1 != null) {
                  v.set("uid", u1.getId()).set("username", u1.get("name"));
                }
                // if (method.isMdc()) {
                // v.set("request",
                // mo.getJSONNonPassword().toString()).set("response",
                // mo.getOutput());
                // }
                if (Bean.DEBUG)
                  AccessLog.create(mo.getRemoteHost(), uri,
                      v.set("status", mo.getStatus()).set("client", mo.browser()));
              }

              // Counter.max("web.request.max", t.past(), uri);
              return;
            }
            i = uri.lastIndexOf("/", i - 1);
          }

          /**
           * not found, then using dummymodel instead, and cache it
           */
          mo = new DummyModel();
          mo.module = Module.home;

          /**
           * do not put in model cache, <br>
           * let's front-end (CDN, http server) do the the "static" resource
           * cache
           */
          // Module.home.modelMap.put(uri, (Class<Model>)
          // mo.getClass());
          mo.dispatch(uri, req, resp, method);

          if (log.isInfoEnabled())
            log.info(method + " " + uri + " - " + mo.getStatus() + " - " + t.past() + "ms -" + mo.getRemoteHost() + " "
                + mo);
          V v = V.create("method", method.toString()).set("cost", t.past()).set("sid", mo.sid());
          User u1 = mo.login;// getUser();
          if (u1 != null) {
            v.set("uid", u1.getId()).set("username", u1.get("name"));
          }
          if (Bean.DEBUG)
            AccessLog.create(mo.getRemoteHost(), uri, v.set("status", mo.getStatus()).set("client", mo.browser()));

          // Counter.max("web.request.max", t.past(), uri);
        }
      }

    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(uri, e);
    }
  }
}
