package org.giiwa.web;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.conf.Global;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;

public class GiiwaServlet extends HttpServlet {

	public static ServletContext s️ervletContext;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static Log log = LogFactory.getLog(GiiwaServlet.class);

	public static boolean INITED = false;

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		if (!INITED) {
			throw new IOException("not inited");
		}

		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

		TimeStamp t = TimeStamp.create();

		RequestHelper r1 = RequestHelper.create((HttpServletRequest) req);
		HttpServletResponse r2 = (HttpServletResponse) resp;

		String uri = r1.getRequestURI();
//		while (uri.indexOf("//") > -1) {
//			uri = uri.replaceAll("//", "/");
//		}

		if (log.isDebugEnabled()) {
			log.debug(req.getMethod() + " - " + uri + " - " + _ip(req));
		}

		String _domain = Global.getString("cross.domain", "");

		if (!X.isEmpty(_domain)) {
			r2.addHeader("Access-Control-Allow-Origin", _domain);
		}

		Controller mo = null;
		try {
			mo = Controller.process(uri, r1, r2, req.getMethod(), t);
		} catch (Exception e) {
			throw new IOException(e);
		} finally {
			if (t.pastms() > 3000) {
				log.warn(r1.getMethod() + " - " + uri + ", cost=" + t.past() + " - " + _ip(req) + ", body="
						+ (mo == null ? null : mo.json()));
			} else if (log.isInfoEnabled()) {
				log.info(r1.getMethod() + " - " + uri + ", cost=" + t.past() + " - " + _ip(req));
			}
		}

	}

	private String _ip(HttpServletRequest req) {

		StringBuilder sb = new StringBuilder();

		String remote = req.getHeader("X-Real-IP");
		if (!X.isEmpty(remote)) {
			sb.append(remote);
		}

		remote = req.getHeader("X-Forwarded-For");
		if (!X.isEmpty(remote)) {
			if (sb.length() > 0)
				sb.append("->");
			sb.append(remote);
		}

		remote = req.getRemoteAddr();
		if (!X.isEmpty(remote)) {
			if (sb.length() > 0)
				sb.append("->");
			sb.append(remote);
		}

		return sb.toString();
	}

}
