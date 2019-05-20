package org.giiwa.framework.web;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;

import org.apache.catalina.WebResource;
import org.apache.catalina.servlets.WebdavServlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.framework.bean.GLog;
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
	public void doGet(HttpServletRequest req, HttpServletResponse resp) {
		doPost(req, resp);
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

			Controller.dispatch(uri, r1, r2, req.getMethod());
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

	@Override
	protected boolean checkIfHeaders(HttpServletRequest request, HttpServletResponse response, WebResource resource)
			throws IOException {

		GLog.applog.info("default", "checkIfHeaders", "uri=" + request.getRequestURI(), null, null);
		return super.checkIfHeaders(request, response, resource);
	}

	@Override
	protected void doCopy(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// TODO Auto-generated method stub

		GLog.applog.info("default", "doCopy", "uri=" + req.getRequestURI(), null, null);
		super.doCopy(req, resp);
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		GLog.applog.info("default", "doDelete", "uri=" + req.getRequestURI(), null, null);
		super.doDelete(req, resp);
	}

	@Override
	protected void doLock(HttpServletRequest req, HttpServletResponse arg1) throws ServletException, IOException {
		// TODO Auto-generated method stub
		GLog.applog.info("default", "doLock", "uri=" + req.getRequestURI(), null, null);
		super.doLock(req, arg1);
	}

	@Override
	protected void doMkcol(HttpServletRequest req, HttpServletResponse arg1) throws ServletException, IOException {
		// TODO Auto-generated method stub
		GLog.applog.info("default", "doMkcol", "uri=" + req.getRequestURI(), null, null);
		super.doMkcol(req, arg1);
	}

	@Override
	protected void doMove(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// TODO Auto-generated method stub
		GLog.applog.info("default", "doMove", "uri=" + req.getRequestURI(), null, null);
		super.doMove(req, resp);
	}

	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		GLog.applog.info("default", "doOptions", "uri=" + req.getRequestURI(), null, null);
		super.doOptions(req, resp);
	}

	@Override
	protected void doPropfind(HttpServletRequest req, HttpServletResponse arg1) throws ServletException, IOException {
		// TODO Auto-generated method stub
		GLog.applog.info("default", "doPropfind", "uri=" + req.getRequestURI(), null, null);
		super.doPropfind(req, arg1);
	}

	@Override
	protected void doProppatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// TODO Auto-generated method stub
		GLog.applog.info("default", "doProppatch", "uri=" + req.getRequestURI(), null, null);
		super.doProppatch(req, resp);
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		GLog.applog.info("default", "doPut", "uri=" + req.getRequestURI(), null, null);
		super.doPut(req, resp);
	}

	@Override
	protected void doUnlock(HttpServletRequest req, HttpServletResponse arg1) throws IOException {
		// TODO Auto-generated method stub
		GLog.applog.info("default", "doUnlock", "uri=" + req.getRequestURI(), null, null);
		super.doUnlock(req, arg1);
	}

	@Override
	protected DocumentBuilder getDocumentBuilder() throws ServletException {
		// TODO Auto-generated method stub
		GLog.applog.info("default", "getDocumentBuilder", "uri=", null, null);
		return super.getDocumentBuilder();
	}

	@Override
	protected String getPathPrefix(HttpServletRequest req) {
		// TODO Auto-generated method stub
		GLog.applog.info("default", "getPathPrefix", "uri=" + req.getRequestURI(), null, null);
		return super.getPathPrefix(req);
	}

	@Override
	protected String getRelativePath(HttpServletRequest req, boolean arg1) {
		// TODO Auto-generated method stub
		GLog.applog.info("default", "getRelativePath", "uri=" + req.getRequestURI(), null, null);
		return super.getRelativePath(req, arg1);
	}

	@Override
	protected String getRelativePath(HttpServletRequest req) {
		// TODO Auto-generated method stub
		GLog.applog.info("default", "getRelativePath", "uri=" + req.getRequestURI(), null, null);
		return super.getRelativePath(req);
	}

	@Override
	protected void doHead(HttpServletRequest req, HttpServletResponse response) throws IOException, ServletException {
		GLog.applog.info("default", "doHead", "uri=" + req.getRequestURI(), null, null);
		super.doHead(req, response);
	}

}
