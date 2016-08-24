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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.*;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.*;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.giiwa.core.bean.TimeStamp;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.framework.bean.AccessLog;
import org.giiwa.framework.bean.User;
import org.giiwa.framework.web.Model.HTTPMethod;

/**
 * load module, default module
 * 
 * @author yjiang
 * 
 */
public class Controller {

  static Log                    log      = LogFactory.getLog(Controller.class);

  /**
   * the configured context path
   */
  public static String          PATH;

  /**
   * os info
   */
  public static String          OS;

  protected static List<String> welcomes = new ArrayList<String>();

  /**
   * uptime of the app
   */
  public final static long      UPTIME   = System.currentTimeMillis();

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

    OS = System.getProperty("os.name").toLowerCase() + "_" + System.getProperty("os.version") + "_"
        + System.getProperty("os.arch");

    Model.HOME = conf.getString("home") + "/giiwa";

    /**
     * initialize the module
     */
    Module.init(conf);

    // get welcome list
    init_welcome();
  }

  @SuppressWarnings("unchecked")
  private static void init_welcome() {
    try {
      SAXReader reader = new SAXReader();
      Document document = reader.read(Model.HOME + "/WEB-INF/web.xml");
      Element root = document.getRootElement();
      Element e1 = root.element("welcome-file-list");
      List<Element> l1 = e1.elements("welcome-file");
      for (Element e2 : l1) {
        welcomes.add(e2.getText().trim());
      }

      log.debug("welcome=" + welcomes);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
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
    return Module.home.getModel(method, uri);
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
  @SuppressWarnings("deprecation")
  public static void dispatch(String uri, HttpServletRequest req, HttpServletResponse resp, Model.HTTPMethod method) {

    TimeStamp t = TimeStamp.create();
    Tps.add(1);

    /**
     * cut-off all the "//"
     */
    while (uri.indexOf("//") > -1) {
      uri = uri.replaceAll("//", "/");
    }

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
        User u1 = mo.getUser();
        if (u1 != null) {
          v.set("uid", u1.getId()).set("username", u1.get("name"));
        }
        if (AccessLog.isOn())
          AccessLog.create(mo.getRemoteHost(), uri, v.set("status", mo.getStatus()).set("client", mo.browser()));
      }

      // Counter.max("web.request.max", t.past(), uri);
      return;
    }

    if (!_dispatch(uri, req, resp, method, t)) {

      for (String suffix : welcomes) {
        if (_dispatch(uri + "/" + suffix, req, resp, method, t)) {
          return;
        }
      }

      /**
       * get back of the uri, and set the path to the model if found, and the
       * path instead
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
            User u1 = mo.getUser();
            if (u1 != null) {
              v.set("uid", u1.getId()).set("username", u1.get("name"));
            }
            if (AccessLog.isOn())
              AccessLog.create(mo.getRemoteHost(), uri, v.set("status", mo.getStatus()).set("client", mo.browser()));
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
       * let's front-end (CDN, http server) do the the "static" resource cache
       */
      // Module.home.modelMap.put(uri, (Class<Model>)
      // mo.getClass());
      mo.dispatch(uri, req, resp, method);

      if (log.isInfoEnabled())
        log.info(
            method + " " + uri + " - " + mo.getStatus() + " - " + t.past() + "ms -" + mo.getRemoteHost() + " " + mo);
      V v = V.create("method", method.toString()).set("cost", t.past()).set("sid", mo.sid());
      User u1 = mo.getUser();
      if (u1 != null) {
        v.set("uid", u1.getId()).set("username", u1.get("name"));
      }
      if (AccessLog.isOn())
        AccessLog.create(mo.getRemoteHost(), uri, v.set("status", mo.getStatus()).set("client", mo.browser()));

      // Counter.max("web.request.max", t.past(), uri);
    }

  }

  private static boolean _dispatch(String uri, HttpServletRequest req, HttpServletResponse resp, HTTPMethod method,
      TimeStamp t) {
    /**
     * load model from the modules
     */
    uri = uri.replaceAll("//", "/");

    // log.debug("dispatch, uri=" + uri);
    Model mo = getModel(method.method, uri);
    if (mo != null) {

      Path p = mo.dispatch(uri, req, resp, method);

      if (p == null || p.accesslog()) {
        if (log.isInfoEnabled())
          log.info(
              method + " " + uri + " - " + mo.getStatus() + " - " + t.past() + "ms -" + mo.getRemoteHost() + " " + mo);

        V v = V.create("method", method.toString()).set("cost", t.past()).set("sid", mo.sid());
        User u1 = mo.getUser();
        if (u1 != null) {
          v.set("uid", u1.getId()).set("username", u1.get("name"));
        }
        if (AccessLog.isOn())
          AccessLog.create(mo.getRemoteHost(), uri, v.set("status", mo.getStatus()).set("client", mo.browser()));
      }

      // Counter.max("web.request.max", t.past(), uri);
      return true;
    } else {
      return false;
    }
  }
}