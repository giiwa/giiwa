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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.*;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.logging.*;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.giiwa.core.base.Url;
import org.giiwa.core.bean.TimeStamp;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.conf.Config;
import org.giiwa.core.conf.Global;
import org.giiwa.core.conf.Local;
import org.giiwa.core.dfile.DFile;
import org.giiwa.framework.bean.AccessLog;
import org.giiwa.framework.bean.Disk;
import org.giiwa.framework.bean.Node;
import org.giiwa.framework.bean.User;
import org.giiwa.framework.web.view.View;

/**
 * load module, default module
 * 
 * @author yjiang
 * 
 */
public class GiiwaController {

	static Log log = LogFactory.getLog(GiiwaController.class);

	/**
	 * the configured context path
	 */
	public static String PATH;

	/**
	 * os info
	 */
	public static String OS;

	protected static List<String> welcomes = new ArrayList<String>();

	/**
	 * uptime of the app
	 */
	public final static long UPTIME = System.currentTimeMillis();

	/**
	 * Inits the.
	 * 
	 * @param conf the conf
	 * @param path the path
	 */
	public static void init(Configuration conf, String path) {

		GiiwaController.PATH = path;

		OS = System.getProperty("os.name").toLowerCase() + "_" + System.getProperty("os.version") + "_"
				+ System.getProperty("os.arch");

		Controller.HOME = Controller.GIIWA_HOME + "/modules";

		/**
		 * initialize the module
		 */
//		System.out.println("init modules");
		Module.init(conf);

//		System.out.println("init welcome page");
		// get welcome list
		init_welcome();

		log.info("controller has been initialized.");
	}

	@SuppressWarnings("unchecked")
	private static void init_welcome() {
		try {
			SAXReader reader = new SAXReader();
			Document document = reader.read(Controller.HOME + "/WEB-INF/web.xml");
			Element root = document.getRootElement();
			Element e1 = root.element("welcome-file-list");
			List<Element> l1 = e1.elements("welcome-file");

			welcomes.add("index");

			for (Element e2 : l1) {
				welcomes.add(e2.getText().trim());
			}

			if (log.isInfoEnabled())
				log.info("welcome=" + welcomes);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Gets the model.
	 *
	 * @param method the method
	 * @param uri    the uri
	 * @return the model
	 */
	public static Controller getModel(String method, String uri) {
		return Module.home.getModel(method, uri);
	}

	/**
	 * Dispatch.
	 * 
	 * @param uri    the uri
	 * @param req    the req
	 * @param resp   the resp
	 * @param method the method
	 */
	@SuppressWarnings("deprecation")
	public static void dispatch(String uri, HttpServletRequest req, HttpServletResponse resp, String method) {

		// log.debug("uri=" + uri);

		TimeStamp t = TimeStamp.create();

		String node = req.getParameter("__node");
		if (!X.isEmpty(node) && !X.isSame(node, Local.id())) {
			Node n = Node.dao
					.load(W.create(X.ID, node).and("updated", System.currentTimeMillis() - Node.LOST, W.OP.gte));
			if (n != null) {
				n.forward(uri, req, resp, method);
				return;
			}
		}

		if (!uri.endsWith(".js") && !uri.endsWith(".css")) {
			uri = Url.decode(uri);
		}

		/**
		 * test and load from cache first
		 */
		Controller mo = Module.home.loadModelFromCache(method, uri);
		if (mo != null) {
			mo.set("__node", node);

			Path p = mo.dispatch(uri, req, resp, method);

			if (p == null) {
				if (log.isInfoEnabled())
					log.info(method + " " + uri + " - " + mo.getStatus() + " - " + t.past() + " -" + mo.getRemoteHost()
							+ " " + mo);

				V v = V.create("method", method.toString()).set("cost", t.past()).set("sid", mo.sid());
				User u1 = mo.getUser();
				if (u1 != null) {
					v.set("uid", u1.getId()).set("username", u1.get("name"));
				}
				if (AccessLog.isOn())
					AccessLog.create(mo.getRemoteHost(), uri,
							v.set("status", mo.getStatus()).set("header", Arrays.toString(mo.getHeaders()))
									.set("client", mo.browser())
									.set("module", mo.module == null ? X.EMPTY : mo.module.getName())
									.set("model", mo.getClass().getName()));
			}

			// Counter.max("web.request.max", t.past(), uri);
			return;
		}

		try {
			// directly file
			File f = Module.home.getFile(uri);
			if (f != null && f.exists() && f.isFile()) {

				if (log.isDebugEnabled())
					log.debug("handled by module, uri=" + uri);

				Controller m = new DefaultController();
				m.req = req;
				m.resp = resp;
				m.set(m);

				m.set("me", m.getUser());
				m.put("lang", m.lang);
				m.put(X.URI, uri);
				m.put("module", Module.home);
				m.put("request", req);
				m.put("this", m);
				m.put("response", resp);
				m.set("session", m.getSession());
				m.set("global", Global.getInstance());
				m.set("conf", Config.getConf());
				m.set("local", Local.getInstance());
				m.set("requestid", UID.random(20));
				View.merge(f, m, uri);

				return;
			}

			// file in file.repo
			DFile f1 = Disk.seek(uri);
			if (f1 != null && f1.exists() && f1.isFile()) {

				if (log.isDebugEnabled())
					log.debug("handled by dfile, uri=" + uri);

				Controller m = new DefaultController();
				m.req = req;
				m.resp = resp;
				m.set(m);

				m.set("me", m.getUser());
				m.put("lang", m.lang);
				m.put(X.URI, uri);
				m.put("module", Module.home);
				m.put("request", req);
				m.put("this", m);
				m.put("response", resp);
				m.set("session", m.getSession());
				m.set("global", Global.getInstance());
				m.set("conf", Config.getConf());
				m.set("local", Local.getInstance());
				m.set("requestid", UID.random(20));

				View.merge(f1, m, uri);

				return;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		// dispatch to model
		if (X.isSame("/", uri) || !_dispatch(uri, req, resp, method, t)) {

			for (String suffix : welcomes) {
				if (_dispatch(uri + "/" + suffix, req, resp, method, t)) {
					return;
				}
			}

			/**
			 * get back of the uri, and set the path to the model if found, and the path
			 * instead
			 */
			int i = uri.lastIndexOf("/");
			while (i > 0) {
				String path = uri.substring(i + 1);
				String u = uri.substring(0, i);
				mo = getModel(method, u);
				if (mo != null) {

					mo.set("__node", node);

					mo.setPath(path);
					Path p = mo.dispatch(u, req, resp, method);

					if (p == null) {
						if (log.isInfoEnabled())
							log.info(method + " " + uri + " - " + mo.getStatus() + " - " + t.past() + " -"
									+ mo.getRemoteHost() + " " + mo);

						V v = V.create("method", method.toString()).set("cost", t.past()).set("sid", mo.sid());
						User u1 = mo.getUser();
						if (u1 != null) {
							v.set("uid", u1.getId()).set("username", u1.get("name"));
						}
						if (AccessLog.isOn())
							AccessLog.create(mo.getRemoteHost(), uri,
									v.set("status", mo.getStatus()).set("client", mo.browser())
											.set("header", Arrays.toString(mo.getHeaders()))
											.set("module", mo.module == null ? X.EMPTY : mo.module.getName())
											.set("model", mo.getClass().getName()));
					}

					// Counter.max("web.request.max", t.past(), uri);
					return;
				}
				i = uri.lastIndexOf("/", i - 1);
			}

			/**
			 * not found, then using dummymodel instead, and cache it
			 */
			mo = new DefaultController();
			mo.module = Module.load(0);
			mo.set("__node", node);

			/**
			 * do not put in model cache, <br>
			 * let's front-end (CDN, http server) do the the "static" resource cache
			 */
			// Module.home.modelMap.put(uri, (Class<Model>)
			// mo.getClass());
			mo.dispatch(uri, req, resp, method);

			if (log.isInfoEnabled())
				log.info(method + " " + uri + " - " + mo.getStatus() + " - " + t.past() + " -" + mo.getRemoteHost()
						+ " " + mo);
			V v = V.create("method", method.toString()).set("cost", t.past()).set("sid", mo.sid());
			User u1 = mo.getUser();
			if (u1 != null) {
				v.set("uid", u1.getId()).set("username", u1.get("name"));
			}
			if (AccessLog.isOn())
				AccessLog.create(mo.getRemoteHost(), uri,
						v.set("status", mo.getStatus()).set("client", mo.browser())
								.set("header", Arrays.toString(mo.getHeaders()))
								.set("module", mo.module == null ? X.EMPTY : mo.module.getName())
								.set("model", mo.getClass().getName()));

			// Counter.max("web.request.max", t.past(), uri);
		}

	}

	private static boolean _dispatch(String uri, HttpServletRequest req, HttpServletResponse resp, String method,
			TimeStamp t) {
		/**
		 * load model from the modules
		 */

		while (uri.indexOf("//") > -1) {
			uri = uri.replaceAll("//", "/");
		}
//		log.debug("_dispatch, uri=" + uri);

		Controller mo = getModel(method, uri);
		if (mo != null) {

			mo.set("__node", req.getParameter("__node"));

			Path p = mo.dispatch(uri, req, resp, method);

			if (p == null) {
				if (log.isInfoEnabled())
					log.info(method + " " + uri + " - " + mo.getStatus() + " - " + t.pastms() + "ms -"
							+ mo.getRemoteHost() + " " + mo);

				V v = V.create("method", method.toString()).set("cost", t.pastms()).set("sid", mo.sid());
				User u1 = mo.getUser();
				if (u1 != null) {
					v.set("uid", u1.getId()).set("username", u1.get("name"));
				}
				if (AccessLog.isOn())
					AccessLog.create(mo.getRemoteHost(), uri,
							v.set("status", mo.getStatus()).set("header", Arrays.toString(mo.getHeaders()))
									.set("client", mo.browser())
									.set("module", mo.module == null ? X.EMPTY : mo.module.getName())
									.set("model", mo.getClass().getName()));
			}

			// Counter.max("web.request.max", t.past(), uri);
			return true;
		} else {
			return false;
		}
	}
}