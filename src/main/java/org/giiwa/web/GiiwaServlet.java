package org.giiwa.web;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.License;
import org.giiwa.conf.Global;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.web.view.View;

public class GiiwaServlet extends HttpServlet {

	public static ServletContext s️ervletContext;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static Log log = LogFactory.getLog(GiiwaServlet.class);

	@Override
	public synchronized void init() {

		try {
			s️ervletContext = getServletContext();

			if (log.isDebugEnabled())
				log.debug("init view ...");

			View.init();

			License.init();

		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}

		log.info("giiwa is ready for service, modules=" + Module.getAll(true) + ", top=" + Module.getHome());
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		if (!X.INITED)
			throw new IOException("not inited");

		TimeStamp t = TimeStamp.create();

		HttpServletRequest r1 = (HttpServletRequest) req;
		HttpServletResponse r2 = (HttpServletResponse) resp;

		String uri = r1.getRequestURI();
//		while (uri.indexOf("//") > -1) {
//			uri = uri.replaceAll("//", "/");
//		}

		if (log.isDebugEnabled())
			log.debug(req.getMethod() + " - " + uri);

		if (_domain == null) {
			_domain = Global.getString("cross.domain", "");
		}
		if (!X.isEmpty(_domain)) {
			r2.addHeader("Access-Control-Allow-Origin", _domain);
		}

		try {

			Controller.process(uri, r1, r2, req.getMethod(), t);
		} finally {
			if (log.isInfoEnabled())
				log.info(r1.getMethod() + " - " + uri + ", cost=" + t.past());
		}

	}

	private static String _domain = null;

}
