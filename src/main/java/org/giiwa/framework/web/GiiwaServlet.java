package org.giiwa.framework.web;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.framework.bean.License;
import org.giiwa.framework.web.view.View;

public class GiiwaServlet extends HttpServlet {

	private Map<String, String> config = new ConcurrentHashMap<String, String>();

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static Log log = LogFactory.getLog(GiiwaServlet.class);

	@Override
	public synchronized void init() {
		try {
			Controller.s️ervletContext = getServletContext();

			if (log.isDebugEnabled())
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

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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
//			uri = URL.rewrite(uri);
			String domain = Global.getString("cross.domain", "");

			if (!X.isEmpty(domain)) {
				r2.addHeader("Access-Control-Allow-Origin", domain);
			}

			if (log.isDebugEnabled())
				log.debug(req.getMethod() + ", uri=" + uri);

			Controller.process(uri, r1, r2, req.getMethod());
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}

	}

}
