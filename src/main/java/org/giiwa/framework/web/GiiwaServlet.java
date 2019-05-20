package org.giiwa.framework.web;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.servlets.WebdavServlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.framework.bean.License;
import org.giiwa.framework.web.view.View;

public class GiiwaServlet extends WebdavServlet {
	private Map<String, String> config = new ConcurrentHashMap<String, String>();

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static Log log = LogFactory.getLog(GiiwaServlet.class);

	@Override
	protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		log.info("method=" + req.getMethod());
		super.doHead(req, resp);
	}

	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		log.info("method=" + req.getMethod());
		super.doOptions(req, resp);
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) {
		try {
			HttpServletRequest r1 = (HttpServletRequest) req;
			HttpServletResponse r2 = (HttpServletResponse) resp;

			String uri = r1.getRequestURI();
			while (uri.indexOf("//") > -1) {
				uri = uri.replaceAll("//", "/");
			}
			/**
			 * rewrite uri
			 */
			uri = URL.rewrite(uri);
			String domain = Global.getString("cross.domain", "");

			if (!X.isEmpty(domain)) {
				r2.addHeader("Access-Control-Allow-Origin", domain);
			}

			// log.info("method=" + req.getMethod());

			req.getMethod();
			Controller.dispatch(uri, r1, r2, "GET");
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) {
		try {
			HttpServletRequest r1 = (HttpServletRequest) req;
			HttpServletResponse r2 = (HttpServletResponse) resp;

			String uri = r1.getRequestURI();
			while (uri.indexOf("//") > -1) {
				uri = uri.replaceAll("//", "/");
			}
			/**
			 * rewrite uri
			 */
			uri = URL.rewrite(uri);
			String domain = Global.getString("cross.domain", "");

			if (!X.isEmpty(domain)) {
				r2.addHeader("Access-Control-Allow-Origin", domain);
			}

			Controller.dispatch(uri, r1, r2, "POST");
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public synchronized void init() {
		try {
			Model.s️ervletContext = getServletContext();

			log.debug("init view ...");
			Enumeration<?> e = getInitParameterNames();
			while (e.hasMoreElements()) {
				String name = e.nextElement().toString();
				String value = getInitParameter(name);
				config.put(name, value);
			}

			View.init(config);

			License.init();

		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}

		log.info("giiwa is ready for service, modules=" + Module.getAll(true) + ", top=" + Module.getHome());
	}
}
