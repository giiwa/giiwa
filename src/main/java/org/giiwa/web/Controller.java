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
package org.giiwa.web;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.*;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.*;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.giiwa.bean.*;
import org.giiwa.conf.Config;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.Bean;
import org.giiwa.dao.Beans;
import org.giiwa.dao.Helper;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dfile.DFile;
import org.giiwa.json.JSON;
import org.giiwa.misc.Html;
import org.giiwa.misc.IOUtil;
import org.giiwa.misc.Url;
import org.giiwa.task.Task;
import org.giiwa.web.view.View;

/**
 * the {@code Controller} Class is base controller, all class that provides web
 * api should inherit it <br>
 * it provides api mapping, path/method mapping, contains all the parameters
 * from the request, and contains all key/value will be pass to view. <br>
 * it wrap all parameter in head, or get method.
 * 
 * <br>
 * the most important method:
 * 
 * <pre>
 * get(String name), get the value of parameter in request, it will convert HTML tag to "special" tag, to avoid destroy;
 * getHtml(String name), get the value of the parameter in request as original HTML format;
 * head(String name),get the value from header;
 * file(String name),get the File value from the request;
 * set(String, Object), set the key/value back to view;
 * </pre>
 * 
 * @author yjiang
 * 
 */
public class Controller {

	protected static Log log = LogFactory.getLog(Controller.class);

	protected static List<String> welcomes = new ArrayList<String>();

	/**
	 * uptime of the app
	 */
	public final static long UPTIME = System.currentTimeMillis();

	private static String ENCODING = "UTF-8";

	/**
	 * the request
	 */
	public HttpServletRequest req;

	/**
	 * the response
	 */
	public HttpServletResponse resp;

	/**
	 * language utility
	 */
	public Language lang = Language.getLanguage();

	/**
	 * the request method(POST, GET, ...)
	 */
	public HttpMethod method = HttpMethod.GET;

	/**
	 * the data which put by "put or set" api, used for HTML view
	 */
	public Map<String, Object> data;

	/**
	 * the home of the modules
	 */
	public static String MODULE_HOME;

	/**
	 * the home of the giiwa
	 */
	public static String GIIWA_HOME;

	/**
	 * session id
	 */
	private String sid;

	/**
	 * locale of user
	 */
	private static String locale;

	/**
	 * the uri of request
	 */
	protected String uri;

	/**
	 * the module of this model
	 */
	protected Module module;

	/**
	 * contentType of response
	 */
	private String contentType;

	/**
	 * associated login user
	 */
	protected User login = null;

//	private static final ThreadLocal<Module> _currentmodule = new ThreadLocal<Module>();

	protected static enum LoginType {
		web, ajax
	};

	/**
	 * get the ServletContext
	 * 
	 * @return
	 */
	public ServletContext context() {
		return GiiwaServlet.s️ervletContext;
	}

	/**
	 * get the response outputstream.
	 * 
	 * @return OutputStream
	 * @throws IOException occur error when get the outputstream from the reqponse.
	 */
	final public OutputStream getOutputStream() throws IOException {
		return resp.getOutputStream();
	}

	/**
	 * the response status
	 */
	protected int status = HttpServletResponse.SC_OK;

	/**
	 * get the response state code
	 * 
	 * @return int of state code
	 */
	final public int status() {
		return status;
	}

	/**
	 * get the locale
	 * 
	 * @return String
	 */
	final public String locale() {
		if (locale == null) {
			locale = Global.getString("language", "en_us");
		}

		// System.out.println("lang:" + locale);
		return locale;
	}

	/**
	 * Current module.
	 * 
	 * @return the module
	 */
//	public static Module currentModule() {
//		return _currentmodule.get();
//	}

	private Path _process() throws Exception {

//		if (method.isGet()) {
//			try {
//				String non = Global.getString("site.browser.nonredirect", null);
//				String ignore = Global.getString("site.browser.ignoreurl", null);
//				if (!X.isEmpty(non) && !X.isSame(non, uri) && !X.isSame(uri, "/admin/browserinfo")
//						&& (X.isEmpty(ignore) || !uri.matches(ignore))) {
//
//					String ua = Global.getString("site.browser", "*");
//
//					if (!X.isEmpty(ua) && !X.isSame(ua, "*")) {
//						String b = this.browser();
//						if (!b.matches(ua)) {
//							this.redirect(non);
//							return null;
//						}
//					}
//				}
//			} catch (Exception e) {
//				log.error(e.getMessage(), e);
//			}
//		}

		if (pathmapping != null) {

			String path = this.path;
			if (X.isEmpty(this.path)) {
				path = X.NONE;
			}

//			X.getCanonicalPath(path);

			while (path.startsWith("/") && path.length() > 1) {
				path = path.substring(1);
			}
			while (path.endsWith("/") && path.length() > 1) {
				path = path.substring(0, path.length() - 1);
			}

			Map<String, PathMapping> methods = pathmapping.get(this.method.name);
			if (methods == null) {
				methods = pathmapping.get("*");
			}

//			log.debug(this.method + "|" + path + "=>" + methods);

			if (methods != null) {
				for (String s : methods.keySet()) {
					if (X.isEmpty(s)) {
						continue;
					}

					if (path == null || !path.matches(s)) {
						continue;
					}

					/**
					 * catch the exception avoid break the whole block
					 */
					try {
						/**
						 * match test in outside first
						 */
//						log.debug(s + "=>" + this.path);

						/**
						 * create the pattern
						 */
						PathMapping oo = methods.get(s);
						if (oo != null) {
							Pattern p = oo.pattern;
							Matcher m1 = p.matcher(path);

							/**
							 * find
							 */
							Object[] params = null;
							if (m1.find()) {
								/**
								 * get all the params
								 */
								params = new Object[m1.groupCount()];
								for (int i = 0; i < params.length; i++) {
									params[i] = m1.group(i + 1);
								}
							}

							Path pp = oo.path;
							/**
							 * check the access and login status
							 */
							if (pp.login()) {

								// check the system has been initialized
								// ?
								if (!Helper.isConfigured()) {
									this.redirect("/admin/setup");
									return null;
								}

								login = this.user();
								if (login == null) {
									/**
									 * login require
									 */
									_gotoLogin();
									return pp;
								}

								if (!X.NONE.equals(pp.access()) && !login.hasAccess(pp.access().split("\\|"))) {
									/**
									 * no access
									 */
									this.put("lang", lang);
									this.deny();

									GLog.securitylog.warn(this.getClass(), pp.path(),
											"deny the access, requred: " + lang.get(pp.access()), user(), this.ip());
									return pp;
								}
							}

							/**
							 * invoke the method
							 */
							Method m = oo.method;
							// log.debug("invoking: " + m.getName());

							try {

								m.invoke(this, params);

							} catch (Exception e) {
								if (log.isErrorEnabled())
									log.error(e.getMessage(), e);

								GLog.oplog.error(this.getClass(), pp.path(), this.json().toString(), e, user(),
										this.ip());

								error(e);
							}

							return pp;
						}

					} catch (Exception e) {
						if (log.isErrorEnabled())
							log.error(s, e);

						GLog.oplog.error(this.getClass(), path, e.getMessage(), e, user(), this.ip());

						error(e);
						return null;
					}

					break;

				}
			}
		} // end of "pathmapping is not null

		// forward the request the file
//		if (staticfile()) {
//			return null;
//		}

		if (method.isGet()) {

			Method m = this.getClass().getMethod("onGet");
			// log.debug("m=" + m);
			if (m != null) {
				Path p = m.getAnnotation(Path.class);
				if (p != null) {
					// check ogin
					if (p.login()) {
						if (this.user() == null) {
							_gotoLogin();
							return null;
						}

						// check access
						if (!X.isEmpty(p.access()) && !X.NONE.equals(p.access())) {
							if (!login.hasAccess(p.access())) {
								deny();
								return null;
							}
						}
					}
				}
			}

			onGet();
		} else if (method.isPost()) {

			Method m = this.getClass().getMethod("onPost");
			if (m != null) {
				Path p = m.getAnnotation(Path.class);
				if (p != null) {
					// check ogin
					if (p.login()) {
						if (this.user() == null) {
							_gotoLogin();
							return null;
						}

						// check access
						if (!X.isEmpty(p.access())) {
							if (!login.hasAccess(p.access())) {
								deny();
								return null;
							}
						}
					}
				}
			}
			onPost();
		} else if (method.isPut()) {

			Method m = this.getClass().getMethod("onPut");
			if (m != null) {
				Path p = m.getAnnotation(Path.class);
				if (p != null) {
					// check ogin
					if (p.login()) {
						if (this.user() == null) {
							_gotoLogin();
							return null;
						}

						// check access
						if (!X.isEmpty(p.access())) {
							if (!login.hasAccess(p.access())) {
								deny();
								return null;
							}
						}
					}
				}
			}
			onPut();
		} else if (method.isOptions()) {

			Method m = this.getClass().getMethod("onOptions");
			if (m != null) {
				Path p = m.getAnnotation(Path.class);
				if (p != null) {
					// check ogin
					if (p.login()) {
						if (this.user() == null) {
							_gotoLogin();
							return null;
						}

						// check access
						if (!X.isEmpty(p.access())) {
							if (!login.hasAccess(p.access())) {
								deny();
								return null;
							}
						}
					}
				}
			}
			onOptions();

		}

		return null;

	}

//	private boolean staticfile() {
//		String uri = this.uri;
//		if (!X.isEmpty(path) && !X.isSame(path, X.NONE)) {
//			uri += "/" + path;
//		}
//		uri = uri.replaceAll("//", "/");
//		// log.debug("staticfile=" + uri);
//
//		File f = Module.home.getFile(uri);
//		if (f != null && f.exists() && f.isFile()) {
//			this.set(this);
//
//			this.set("me", this.getUser());
//			this.put("lang", lang);
//			this.put(X.URI, uri);
//			this.put("module", Module.home);
//			this.put("path", path);
//			this.put("request", req);
//			this.put("this", this);
//			this.put("response", resp);
//			this.put("session", this.getSession(false));
//			this.put("global", Global.getInstance());
//			this.put("conf", Config.getConf());
//			this.put("local", Local.getInstance());
//
//			show(uri);
//			return true;
//		}
//
//		return false;
//	}

	final public void init(String uri, HttpServletRequest req, HttpServletResponse resp, String method) {
		try {
			this.uri = uri;
			this.req = req;
			this.resp = resp;
			this.method = HttpMethod.create(method);

			this._multipart = ServletFileUpload.isMultipartContent(req);

			req.setCharacterEncoding(ENCODING);

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Dispatch.
	 * 
	 * @param uri    the uri
	 * @param req    the req
	 * @param resp   the resp
	 * @param method the method
	 * @return Path
	 */
	final Path dispatch(String uri, HttpServletRequest req, HttpServletResponse resp, String method) {

		// created = System.currentTimeMillis();

		// construct var
		// init
		try {

//			_currentmodule.set(module);

			init(uri, req, resp, method);

			if (!Module.home.before(this)) {
				if (log.isDebugEnabled())
					log.debug("handled by filter, and stop to dispatch");
				return null;
			}

			return _process();

		} catch (Exception e) {
			error(e);
		} finally {
//			_currentmodule.remove();

			Module.home.after(this);
		}
		return null;
	}

	private void _gotoLogin() {
		if (this.uri != null && this.uri.indexOf("/user/") < 0) {
			if (!isAjax()) {
				try {
					Session.load(sid()).set(X.URI, this.uri).store();
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		}

		if (isAjax()) {
			JSON jo = JSON.create();

			jo.put(X.STATE, HttpServletResponse.SC_UNAUTHORIZED);

//			this.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
//			this.setHeader("Location", "/");

			jo.put(X.MESSAGE, lang.get("login.required"));
			jo.put(X.ERROR, lang.get("not.login"));
			this.send(jo);

		} else {
			String node = this.getString("__node");
			if (!X.isEmpty(node)) {
				this.redirect("/user/login?__node=" + node);
			} else {
				this.redirect("/user/login");
			}
		}
	}

	/**
	 * get the current session sid, if not present, create a new one, please refer
	 * {@code sid(boolean)}.
	 * 
	 * @deprecated
	 * @return the string
	 */
	final public String sid() {
		return sid(true);
	}

	/**
	 * get the current session, the sequence of session from request:
	 * 
	 * <pre>
	 * sid=? in cookie
	 * sid=? in header
	 * token=? in query
	 * sid=? in query
	 * </pre>
	 * 
	 * .
	 *
	 * @param newSession if true and not presented, create a new one, otherwise
	 *                   return null
	 * @return the string
	 */
	final public String sid(boolean newSession) {
		if (X.isEmpty(sid)) {
			sid = this.cookie("sid");
			if (X.isEmpty(sid)) {
				sid = this.head("sid");
				if (X.isEmpty(sid)) {
					sid = this.getString("sid");
					if (X.isEmpty(sid)) {
						sid = (String) req.getAttribute("sid");
						if (X.isEmpty(sid) && newSession) {
							do {
								sid = UID.random();
							} while (Session.exists(sid));

							log.debug("creeate new sid=" + sid);
						}
					}
				}

				/**
				 * get session.expired in seconds
				 */
				if (!X.isEmpty(sid)) {
					long expired = Global.getLong("session.alive", X.AWEEK / X.AHOUR) * X.AHOUR;
					if (expired <= 0) {
						cookie("sid", sid, (int) expired);
					} else {
						cookie("sid", sid, (int) (expired / 1000));
					}
				}
			}

			if (!X.isEmpty(sid))
				sid += "/" + this.ip();

//			log.debug("sid=" + sid);
		}

		return sid;
	}

	/**
	 * get the token
	 * 
	 * @return String
	 */
	final public String token() {
		String token = this.cookie("token");
		if (token == null) {
			token = this.head("token");
			if (token == null) {
				token = this.getString("token");
			}
		}
		return token;
	}

	/**
	 * response and redirect to the url
	 * 
	 * @param url the url
	 */
	final public void redirect(String url) {
		resp.setHeader("Location", url);
		status(HttpServletResponse.SC_MOVED_TEMPORARILY);
	}

	/**
	 * forward or lets the clazz to handle the request
	 * 
	 * @param clazz the class name
	 * @return the Path
	 */
	final public Path forward(Class<? extends Controller> clazz) {
		try {
			Controller m = module.getModel(method.name, clazz);

			m.copy(this);

			return m._process();
		} catch (Exception e) {
			error(e);
		}
		return null;
	}

	/**
	 * Forward to the model(url), do not response yet
	 * 
	 * @param url the url
	 * @throws IOException
	 */
	public void forward(String url) throws IOException {
		req.setAttribute("sid", sid(false));
		Controller.process(url, req, resp, method.name, TimeStamp.create());
	}

	/**
	 * Put the name=object to the model.
	 *
	 * @param name the name
	 * @param o    the object
	 */
	final public Controller put(String name, Object o) {
		// System.out.println("put:" + name + "=>" + o);

		if (data == null) {
			data = new HashMap<String, Object>();
		}
		if (name == null) {
			return this;
		}

		if (o == null) {
			/**
			 * clear
			 */
			data.remove(name);
		} else {
			data.put(name, o);
		}

		return this;
	}

	/**
	 * remove the name from the model
	 * 
	 * @param name the name of data setted in model
	 */
	final public void remove(String name) {
		if (data != null) {
			data.remove(name);
		}
	}

	/**
	 * Sets name=object back to model which accessed by view.
	 * 
	 * @param name the name of data in model
	 * @param o    the value object
	 */
	final public Controller set(String name, Object o) {
		return put(name, o);
	}

	/**
	 * get parameter from request
	 * 
	 * @param name
	 * @return
	 */
	public String get(String name) {
		return this.getHtml(name);
	}

	/**
	 * Sets Beans back to Model which accessed by view, it will auto paging
	 * according the start and number per page.
	 * 
	 * @deprecated
	 * @param bs the Beans
	 * @param s  the start position
	 * @param n  the number per page
	 */
	final public void set(Beans<? extends Bean> bs, int s, int n) {
		pages(bs, s, n);
	}

	/**
	 * 
	 * Sets Beans back to Model which accessed by view, it will auto paging
	 * according the start and number per page.
	 * 
	 * @param bs
	 * @param s
	 * @param n
	 */
	final public void pages(Beans<? extends Bean> bs, int s, int n) {
		if (bs != null) {
			this.set("list", bs);
			int total = (int) bs.getTotal();
			if (total > 0) {
				this.set("total", total);
			}
			if (bs.getCost() > 0) {
				this.set("cost", bs.getCost());
			}
			if (n > 0) {
				if (total > 0) {
					int t = total / n;
					if (total % n > 0)
						t++;
					this.set("totalpage", t);
				}
			}
			this.set("pages", Paging.create(total, s, n));
		}
		return;
	}

	/**
	 * copy the data from the JSON.
	 * 
	 * @param jo    the map of data
	 * @param names the names that will be set back to model, if null, will set all
	 */
	final public void copy(JSON jo, String... names) {
		if (jo == null) {
			return;
		}

		if (names == null || names.length == 0) {
			for (Object name : jo.keySet()) {
				put(name.toString(), jo.get(name));
			}
		} else {
			for (String name : names) {
				if (jo.containsKey(name)) {
					put(name, jo.get(name));
				}
			}
		}

		return;
	}

	/**
	 * @deprecated
	 * @param name
	 * @return
	 */
	public String getHeader(String name) {
		return head(name);
	}

	/**
	 * Gets the request header.
	 * 
	 * @param name the header name
	 * @return String of the header
	 */
	final public String head(String name) {
		try {
			return req.getHeader(name);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Adds the header.
	 * 
	 * @param name  the name
	 * @param value the value
	 */
	final public void head(String name, String value) {
		try {
			if (resp.containsHeader(name)) {
				resp.setHeader(name, value);
			} else {
				resp.addHeader(name, value);
			}
		} catch (Exception e) {
		}
	}

	/**
	 * Gets the int parameter by name
	 * 
	 * @param name the name
	 * @return the int
	 */
	public int getInt(String name) {
		return getInt(name, 0);
	}

	/**
	 * Gets the int from the request parameter, if the tag is not presented, then
	 * get the value from the session, if the value less than minvalue, then get the
	 * minvalue, and store the value in session
	 * 
	 * @deprecated
	 * @param name         the name
	 * @param defaultValue the default value
	 * @param tagInSession the tag in session
	 * @return the int
	 */
	public int getInt(String name, int defaultValue, String tagInSession) {
		int r = getInt(name);
		try {
			if (r < 1) {
				Session s = this.session(false);
				r = s == null ? -1 : s.getInt(tagInSession);
				if (r < 1) {
					r = defaultValue;
				}
			} else {
				Session s = this.session(false);
				if (s != null) {
					s.set(tagInSession, r).store();
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		// if (r > 500) {
		// r = 500;
		// log.error("the page number exceed max[500]: " + r);
		// }
		return r;
	}

	/**
	 * @deprecated get the parameter from the request, if not presented, then get
	 *             from session
	 * 
	 * @param tag          the name of the parameter in request.
	 * @param tagInSession associated with the name in session.
	 * @param defaultValue the default value if not presented in both.
	 * @return String of the value
	 */
	final public String getString(String name, String tagInSession, String defaultValue) {
		String r = getString(name);
		try {
			if (X.isEmpty(r)) {
				Session s = this.session(false);
				r = s == null ? null : (String) s.get(tagInSession);
				if (X.isEmpty(r)) {
					r = defaultValue;
					s.set(tagInSession, r).store();
				}
			} else {
				Session s = this.session(false);
				if (s != null)
					s.set(tagInSession, r).store();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return r;
	}

	/**
	 * @deprecated
	 * @param tag
	 * @param tagInSession
	 * @param queryfirst
	 * @return
	 */
	final public String getString(String name, String tagInSession, boolean queryfirst) {
		String r = null;
		try {
			if (queryfirst) {
				r = getString(name);
				Session s = this.session(false);
				if (s != null)
					s.set(tagInSession, r).store();
			} else {
				Session s = this.session(false);
				r = s == null ? null : (String) s.get(tagInSession);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return r;
	}

	/**
	 * Gets the int parameter by name, if not presented, return the defaultvalue
	 * 
	 * @param name         the name
	 * @param defaultValue the default value
	 * @return the int
	 */
	public int getInt(String name, int defaultValue) {
		String v = this.getString(name);
		return X.toInt(v, defaultValue);
	}

	/**
	 * Gets the long parameter by name, if not presented, return the defaultvalue
	 * 
	 * @param tag          the name of parameter
	 * @param defaultvalue the default value when the name not presented.
	 * @return long
	 */
	final public long getLong(String tag, long defaultvalue) {
		String v = this.getString(tag);
		return X.toLong(v, defaultvalue);
	}

	/**
	 * get the long parameter by name, if not presented, return 0;
	 * 
	 * @param tag the name of parameter in request.
	 * @return long
	 */
	public long getLong(String tag) {
		return getLong(tag, 0);
	}

	/**
	 * @deprecated
	 * @return
	 */
	public Cookie[] getCookie() {
		return cookies();
	}

	/**
	 * get all cookies from the request
	 * 
	 * @return Cookie[]
	 */
	final public Cookie[] cookies() {
		return req.getCookies();
	}

	/**
	 * @deprecated
	 * @param name
	 * @return
	 */
	public String getCookie(String name) {
		return cookie(name);
	}

	/**
	 * Gets the cookie from the request
	 * 
	 * @param name the name
	 * @return the cookie
	 */
	final public String cookie(String name) {
		Cookie[] cc = cookies();
		if (cc != null) {
			for (int i = cc.length - 1; i >= 0; i--) {
				Cookie c = cc[i];
				if (c.getName().equals(name)) {
					return c.getValue();
				}
			}
		}

		return null;
	}

	/**
	 * get the request
	 * 
	 * @deprecated
	 * @return HttpServletRequest
	 */
	public HttpServletRequest getRequest() {
		return req;
	}

	/**
	 * get the response
	 * 
	 * @deprecated
	 * @return HttpServletResponse
	 */
	public HttpServletResponse getResponse() {
		return resp;
	}

	/**
	 * get the browser user-agent
	 * 
	 * @return the string
	 */
	final public String browser() {
		return this.head("user-agent");
	}

	/**
	 * set the cookie back to the response.
	 * 
	 * @param key           the name of cookie
	 * @param value         the value of the cookie
	 * @param expireseconds the expire time of seconds.
	 */
	final public void cookie(String key, String value, int expireseconds) {
		if (key == null) {
			return;
		}

		Cookie c = new Cookie(key, value);
		if (expireseconds == 0) {
			c.setMaxAge(0);
		} else if (expireseconds > 0) {
			c.setMaxAge(expireseconds);
		}
		c.setPath("/");

		/**
		 * set back to the domain
		 */
		// c.setDomain(SystemConfig.s("domain", this.getHeader("Host")));
		String domain = Module._conf.getString("domain", null);
		if (!X.isEmpty(domain)) {
			c.setDomain(domain);
		}

		resp.addCookie(c);
	}

	/**
	 * the request uri
	 * 
	 * @return String
	 */
	final public String uri() {
		if (X.isEmpty(uri)) {
			uri = req.getRequestURI();
			while (uri.indexOf("//") > -1) {
				uri = uri.replaceAll("//", "/");
			}
			return uri;
		}
		return uri;
	}

	/**
	 * get the sub.path of the uri
	 * 
	 * @deprecated
	 * @return String
	 */
	final public String getPath() {
		return path;
	}

	/**
	 * @deprecated
	 * @return
	 */
	public String getRemoteHost() {
		return this.ip();
	}

	/**
	 * trying to get the client ip from request header
	 * 
	 * @return String
	 */
	final public String ip() {
		String remote = this.head("X-Forwarded-For");
		if (remote == null) {
			remote = head("X-Real-IP");

			if (remote == null) {
				remote = req.getRemoteAddr();
			}
		}

		return remote;
	}

	private JSON _json;

	/**
	 * @deprecated
	 * @return
	 */
	public JSON getJSON() {
		return json();
	}

	/**
	 * get the request as JSON
	 * 
	 * @return JSONObject
	 */
	final public JSON json() {
		if (_json == null) {
			_json = JSON.create();
			for (String name : this.names()) {

				String s = this.getHtml(name);
				_json.put(name, s);
			}

		}
		return _json;
	}

	/**
	 * get the request as JSON, it will mix the password key
	 * 
	 * @deprecated
	 * @return JSONObject
	 */
	public JSON getJSONNonPassword() {
		if (_json == null) {
			json();
		}

		JSON jo = JSON.fromObject(_json);
		// mix the password
		for (Object name : jo.keySet()) {
			if ("password".equals(name) || "pwd".equals(name) || "passwd".equals(name)) {
				jo.put(name.toString(), "*******");
			}
		}
		return jo;
	}

	/**
	 * Gets the value of request string parameter. it auto handle multiple-part, and
	 * convert "&lt;" or "&gt;" to html char and normal request
	 * 
	 * @param name the name of parameter
	 * @return string of requested value
	 */
	final public String getString(String name) {
		String s = this.getHtml(name);
		if (s != null) {
			return s.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
		}
		return null;
	}

	/**
	 * get the paramter by value and truncate by length
	 * 
	 * @param name      the parameter name
	 * @param maxlength the maxlength
	 * @return string of value
	 */
	final public String get(String name, int maxlength) {
		String s = getString(name);
		if (!X.isEmpty(s)) {
			if (s.getBytes().length > maxlength) {
				s = Html.create(s).text(maxlength);
			}
		}

		return s;
	}

	/**
	 * Get the parameter by name and truncate by length, if not presented, using the
	 * default value.
	 * 
	 * @deprecated
	 * @param name         the parameter name
	 * @param maxlength    the maxlength
	 * @param defaultvalue the defaultvalue
	 * @return string of value
	 */
	public String get(String name, int maxlength, String defaultvalue) {
		String s = getString(name);
		if (!X.isEmpty(s)) {
			if (s.getBytes().length > maxlength) {
				s = Html.create(s).text(maxlength);
			}
		} else {
			s = defaultvalue;
		}

		return s;
	}

	/**
	 * get the parameter by name as keep original string
	 * 
	 * @param name the parameter name
	 * @return String of value
	 */
	final public String getHtml(String name) {

		try {
			String c1 = req.getContentType();

//			log.debug("get s, name=" + name + ", c1=" + c1);

			if (c1 != null && c1.indexOf("application/json") > -1) {

				if (uploads == null) {

					BufferedReader in = req.getReader();

					StringBuilder sb = new StringBuilder();
					char[] buff = new char[1024];
					int len;
					while ((len = in.read(buff)) != -1) {
						sb.append(buff, 0, len);
					}

					JSON jo = JSON.fromObject(sb.toString());
					if (jo != null) {
						uploads = new HashMap<String, Object>();
						uploads.putAll(jo);
					}
				}

				if (uploads != null) {
					Object v1 = uploads.get(name);

//					log.debug("get s=" + v1);

					if (v1 != null) {
						return v1.toString().trim();
					}
				}
			}

			if (this._multipart) {

				_get_files();

				FileItem i = this.file(name);

				if (i != null && i.isFormField()) {
					InputStream in = i.getInputStream();
					byte[] bb = new byte[in.available()];
					in.read(bb);
					in.close();
					return new String(bb, ENCODING);
				}
				return null;

			}

			String[] ss = req.getParameterValues(name);
			if (ss == null || ss.length == 0) {
				return null;
			}

			String s = ss[ss.length - 1];
			s = _decode(s);

//			log.debug("get s = " + s);
			return s;

		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error("get request parameter " + name + " get exception.", e);
		}

		return null;

	}

	private String _decode(String s) {
		try {
			String t = this.head("Content-Type");
			if (t == null) {
				// do nothing
				// log.debug("get s=" + s);

			} else if (t.indexOf("UTF-8") > -1) {
				// log.debug("get s=" + s);

			} else if (t.indexOf("urlencoded") > -1) {
				// do nothing
//				s = new String(s.getBytes("ISO-8859-1"), ENCODING);

			} else if (t.indexOf("application/json") > -1) {
//				log.debug("get s=" + s);
				if (method.isPost()) {
					s = new String(s.getBytes("ISO-8859-1"), ENCODING);
				}
			} else {
//				log.debug("get s=" + s);
			}
		} catch (Exception e) {
			log.error(s, e);
		}
		return s;
	}

	/**
	 * get the values by name from the request, and convert the HTML tag
	 * 
	 * @param name
	 * @return
	 */
	final public String[] getStrings(String name) {
		String[] ss = getHtmls(name);
		if (ss != null) {
			for (int i = 0; i < ss.length; i++) {
				ss[i] = ss[i].replaceAll("<", "&lt;").replaceAll(">", "&gt;");
			}
		}
		return ss;
	}

	/**
	 * Gets the strings from the request, <br>
	 * 
	 * @param name the name of the request parameter
	 * @return String[] of request
	 */
	@SuppressWarnings("unchecked")
	final public String[] getHtmls(String name) {
		try {
			if (this._multipart) {
				_get_files();

				Object o = uploads.get(name);
				if (o instanceof FileItem) {
					return new String[] { getString(name) };
				} else if (o instanceof List) {
					List<FileItem> list = (List<FileItem>) o;
					String[] ss = new String[list.size()];
					for (int i = 0; i < ss.length; i++) {
						FileItem ii = list.get(i);
						if (ii.isFormField()) {
							InputStream in = ii.getInputStream();
							byte[] bb = new byte[in.available()];
							in.read(bb);
							in.close();
							ss[i] = new String(bb, "UTF8").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
						}
					}
					return ss;
				}
			} else {
				String[] ss = req.getParameterValues(name);
				if (ss != null && ss.length > 0) {
					for (int i = 0; i < ss.length; i++) {
						if (ss[i] == null)
							continue;

						ss[i] = _decode(ss[i]);

						ss[i] = ss[i].replaceAll("<", "&lt").replaceAll(">", "&gt").trim();
					}
				}
				return ss;
			}
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(name, e);
		}
		return null;
	}

	/**
	 * @deprecated
	 * @return
	 */
	public List<String> getNames() {
		return names();
	}

	/**
	 * get the parameters names
	 * 
	 * @return List of the request names
	 */
	final public List<String> names() {
		String c1 = req.getContentType();
		if (c1 != null && c1.indexOf("application/json") > -1) {
			this.getString(null);// initialize uploads
			if (uploads != null) {
				return new ArrayList<String>(uploads.keySet());
			}
		} else if (this._multipart) {
			_get_files();
			return new ArrayList<String>(uploads.keySet());
		}

		Enumeration<?> e = req.getParameterNames();
		if (e == null) {
			return Arrays.asList();
		}

		List<String> list = new ArrayList<String>();

		while (e.hasMoreElements()) {
			list.add(e.nextElement().toString());
		}
		return list;

	}

	final public List<String> keySet() {
		return this.names();
	}

	/**
	 * @deprecated
	 * @param newsession
	 * @return
	 */
	public Session getSession(boolean newsession) {
		return session(newsession);
	}

	/**
	 * get the session, if not presented, then create a new, "user" should store the
	 * session invoking session.store()
	 * 
	 * @return Session
	 */
	final public Session session(boolean newsession) {
		return Session.load(sid(newsession));
	}

	/**
	 * indicator of multipart request
	 */
	transient boolean _multipart = false;

	/**
	 * is multipart request
	 * 
	 * @return boolean
	 */
//	final public boolean isMultipart() {
//		return _multipart;
//	}

	/**
	 * @deprecated
	 * @return
	 */
	public Roles getRoles() {
		User u = this.user();
		if (u != null) {
			return u.getRole();
		}

		Session s = this.session(false);
		if (s != null && s.has("roles")) {
			return s.get("roles");
		}

		return null;
	}

	/**
	 * @deprecated
	 * @return
	 */

	public User getUser() {
		return user();
	}

	/**
	 * get the user associated with the session, <br>
	 * this method will cause set user in session if not setting, <br>
	 * to avoid this, you need invoke the "login" variable
	 * 
	 * @return User
	 */
	final public User user() {
		if (login == null) {
			Session s = session(false);
			login = s == null ? null : s.get("user");

			if (login == null) {
				if (Global.getInt("user.token", 1) == 1) {
					String sid = sid();
					String token = token();
					if (!X.isEmpty(sid) && !X.isEmpty(token)) {
						AuthToken t = AuthToken.load(sid, token);
						if (t != null) {
							login = t.getUser_obj();
							this.user(login, LoginType.ajax);
						}
					}
				}
			}

			// log.debug("getUser, user=" + login + " session=" + s);
		}

		if (login != null) {
			if (System.currentTimeMillis() - login.getLong("lastlogined") > X.AMINUTE) {

				User.dao.update(login.id, V.create("lastlogined", System.currentTimeMillis()));

				login.set("lastlogined", System.currentTimeMillis());
				Session s = session(true);
				try {
					s.set("user", login);
					s.store();
				} catch (Exception e) {
				}

				V v = V.create("lastlogined", System.currentTimeMillis());

				String type = (String) login.get("logintype");
				if (X.isSame(type, "web")) {
					v.append("weblogined", System.currentTimeMillis());
				} else if (X.isSame(type, "ajax")) {
					v.append("ajaxlogined", System.currentTimeMillis());
				}

				Task.schedule(() -> {
					try {
						User.dao.update(login.id, v);
						login = User.dao.load(login.id);
						if (login == null || login.isLocked() || login.isDeleted()) {
							s.remove("user");
						} else {
							s.set("user", login);
						}
						s.store();
					} catch (Exception e) {

					}

				});
			}
		}

		return login;
	}

	/**
	 * set user in session
	 * 
	 * @param u
	 */
	final public void user(User u) {
		this.user(u, LoginType.web);
	}

	/**
	 * @deprecated
	 * @param u
	 */
	public void setUser(User u) {
		this.user(u, LoginType.web);
	}

	/**
	 * @deprecated
	 * @param u
	 * @param logintype
	 */
	public void setUser(User u, LoginType logintype) {
		this.user(u, logintype);
	}

	/**
	 * set the user associated with the session
	 * 
	 * @param u the user object associated with the session
	 */
	final public void user(User u, LoginType logintype) {

		Session s = session(true);
		User u1 = s.get("user");
		if (u != null && u1 != null && u1.getId() != u.getId()) {
			log.warn("clear the data in session");
			s.clear();
		}

		if (u != null) {
			if (!X.isEmpty(logintype)) {
				u.set("logintype", logintype.toString());
			}
			if (System.currentTimeMillis() - u.getLong("lastlogined") > X.AMINUTE) {
				u.set("lastlogined", System.currentTimeMillis());
				u.set("ip", this.ip());

				V v = V.create();
				String type = (String) u.get("logintype");
				if (X.isSame(type, "web")) {
					v.append("weblogined", System.currentTimeMillis());
					v.append("ip", this.ip());
				} else if (X.isSame(type, "ajax")) {
					v.append("ajaxlogined", System.currentTimeMillis());
					v.append("ip", this.ip());
				}
				if (!v.isEmpty()) {
					User.dao.update(u.getId(), v);
				}
			}

			try {
				s.set("user", u);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		} else {
			log.warn("clear the data in session");
			s.clear();
		}
		s.store();

		if (log.isDebugEnabled())
			log.debug("store session: session=" + s + ", getSession=" + session(false));

		login = u;
	}

	/**
	 * get files or multiple-part request
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	final private Map<String, Object> _get_files() {

		if (uploads == null) {

			uploads = new HashMap<String, Object>();
			DiskFileItemFactory factory = new DiskFileItemFactory();

			// Configure a repository (to ensure a secure temp location is used)
			File repository = (File) context().getAttribute("javax.servlet.context.tempdir");
			factory.setRepository(repository);

			// Create a new file upload handler
			ServletFileUpload upload = new ServletFileUpload(factory);

			// Parse the request
			try {
				List<FileItem> items = upload.parseRequest(req);
				if (items != null && items.size() > 0) {
					for (FileItem f : items) {
						if (uploads.containsKey(f.getFieldName())) {
							Object o = uploads.get(f.getFieldName());
							if (o instanceof FileItem) {
								List<FileItem> list = new ArrayList<FileItem>();
								list.add((FileItem) o);
								list.add(f);
								uploads.put(f.getFieldName(), list);
							} else if (o instanceof List) {
								((List<FileItem>) o).add(f);
							}
						} else {
							uploads.put(f.getFieldName(), f);
						}
					}
				} else {
					if (log.isWarnEnabled())
						log.warn("nothing got!!!");
				}
			} catch (FileUploadException e) {
				if (log.isErrorEnabled())
					log.error(e.getMessage(), e);
			}
		}

		return uploads;
	}

	/**
	 * @deprecated
	 * @param name
	 * @return
	 */
	public FileItem getFile(String name) {
		return file(name);
	}

	/**
	 * Gets the file by name from the request.
	 * 
	 * @param name the parameter name
	 * @return file of value, null if not presented
	 */
	@SuppressWarnings("unchecked")
	final public FileItem file(String name) {

		_get_files();

		Object o = uploads.get(name);
		if (o instanceof FileItem) {
			return (FileItem) o;
		} else if (o instanceof List) {
			List<FileItem> list = (List<FileItem>) o;
			return list.get(list.size() - 1);
		}
		return null;
	}

	/**
	 * uploaded file
	 */
	private Map<String, Object> uploads = null;

	/**
	 * the path of the uri, http://[host:port]/[class]/[path], usually the
	 * path=method name
	 */
	protected String path;

	/**
	 * set the response contenttype
	 * 
	 * @param contentType the content type in response
	 */
	final public void setContentType(String contentType) {
		this.contentType = contentType;
		resp.setContentType(contentType);
	}

	/**
	 * return the contentType
	 * 
	 * @deprecated
	 * @return String of content-type
	 */
	public String getResponseContentType() {
		if (contentType == null) {
			return MIME_HTML;
		} else {
			return contentType;
		}
	}

	/**
	 * output the json as "application/json" to end-user
	 * 
	 * @param jo the json that will be output
	 */
	final public void send(JSON jo) {

		if (outputed > 0) {
			Exception e = new Exception("response twice!");
			GLog.applog.error(this.getClass(), "response", e.getMessage(), e);
			log.error(jo.toString(), e);

			error(e);

			return;
		}

		outputed++;

		if (jo == null) {
			_send_json("{}");
		} else {
			_send_json(jo.toString());
		}
	}

	/**
	 * output the jsonarr as "application/json" to end-user
	 * 
	 * @param arr the array of json
	 */
	final public void send(List<JSON> arr) {
		if (arr == null) {
			_send_json("[]");
		} else {
			_send_json(arr.toString());
		}
	}

	/**
	 * output the string as "application/json" to end-userr.
	 * 
	 * @param jsonstr the jsonstr string
	 */
	private void _send_json(String jsonstr) {

		this.setContentType(Controller.MIME_JSON);

		try {
			PrintWriter writer = resp.getWriter();
			writer.write(jsonstr);
			writer.flush();
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(jsonstr, e);
		}

	}

	transient QueryString _query;

	/**
	 * get the parameter as querystring object
	 * 
	 * @return
	 */
	public QueryString query() {
		if (_query == null) {
			String url = req.getRequestURI();
			_query = new QueryString(url).copy(this);
		}
		return _query;

	}

	private int outputed = 0;

	/**
	 * using current model to render the template, and show the result html page to
	 * end-user.
	 * 
	 * @param viewname the viewname template
	 * @return boolean
	 */
	final public boolean show(String viewname) {

		try {
			if (outputed > 0) {
				throw new Exception("show twice!");
			}

			/**
			 * set default data in model
			 */
			this.lang = Language.getLanguage(locale());

			this.put("lang", lang);
			this.put(X.URI, uri);
			this.put("module", Module.home);
			this.put("path", this.path);
			this.put("req", this);
			this.put("global", Global.getInstance());
			this.put("conf", Config.getConf());
			this.put("local", Local.getInstance());
			this.put("requestid", UID.random(20));
			this.put("me", login);

			// TimeStamp t1 = TimeStamp.create();
			File file = Module.home.getFile(viewname);
			if (file != null && file.exists()) {
				View.merge(file, this, viewname);

				return true;
			} else {
				DFile d = Disk.seek(viewname);
				if (d.exists()) {

					View.merge(d, this, viewname);

					return true;
				}
				notfound("page not found, page=" + viewname);
			}
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(viewname, e);

			GLog.applog.error(this.getClass(), "show", e.getMessage(), e, login, this.ip());

			error(e);
		} finally {
			outputed++;
		}

		return false;
	}

//	final public boolean passup(String viewname) {
//
//		try {
//
//			File file = module.floor() != null ? module.floor().getFile(viewname) : null;
//			if (file != null && file.exists()) {
//				View.merge(file, this, viewname);
//				return true;
//			} else {
//				notfound("page not found, page=" + viewname);
//			}
//		} catch (Exception e) {
//			if (log.isErrorEnabled())
//				log.error(viewname, e);
//		}
//
//		return false;
//	}

	/**
	 * On get requested from HTTP GET method.
	 */
	public void onGet() {
		onPost();
	}

	/**
	 * On post requested from HTTP POST method.
	 */
	public void onPost() {

	}

	/**
	 * On put requested from HTTP PUT method.
	 */
	public void onPut() {
		if (this.isAjax()) {
			send(JSON.create().append(X.STATE, HttpServletResponse.SC_FORBIDDEN));
		} else {
			this.print("forbidden");
		}
	}

	public void onOptions() {
		if (this.isAjax()) {
			send(JSON.create().append(X.STATE, HttpServletResponse.SC_FORBIDDEN));
		} else {
			this.print("forbidden");
		}
	}

	/**
	 * Get the mime type.
	 * 
	 * @param uri the type of uri
	 * @return String of mime type
	 */
	public static String getMimeType(String uri) {
		String mime = GiiwaServlet.s️ervletContext.getMimeType(uri);
		if (mime == null) {
			if (uri.endsWith(".tgz")) {
				mime = "application/x-gzip";
			}
		}
		if (log.isDebugEnabled())
			log.debug("mimetype=" + mime + ", uri=" + uri);
		return mime;
	}

	/**
	 * Show error page to end user.<br>
	 * if the request is AJAX, then response a json with error back to front
	 * 
	 * @param e the throwable
	 */
	final public void error(Throwable e) {
		if (log.isErrorEnabled())
			log.error(e.getMessage(), e);

		outputed = 0;

		StringWriter sw = new StringWriter();
		PrintWriter out = new PrintWriter(sw);
		e.printStackTrace(out);
		String s = sw.toString();

		if (isAjax()) {
			JSON jo = JSON.create();
			jo.append(X.STATE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			String s1 = e.getMessage();
			if (X.isEmpty(s1)) {
				jo.append(X.MESSAGE, lang.get("request.error"));
			} else {
				jo.append(X.MESSAGE, s1);
			}

			jo.append(X.TRACE, s);
			this.send(jo);
		} else {
			this.set("me", this.user());

			String lineSeparator = System.lineSeparator();
			s = s.replaceAll(lineSeparator, "<br/>");
			s = s.replaceAll(" ", "&nbsp;");
			s = s.replaceAll("\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
			this.set("error", s);

			this.show("/error.html");
			status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}

	}

	/**
	 * default notfound handler <br>
	 * 1) look for "/notfound" model, if found, dispatch to it. <br>
	 * 2) else response notfound page or json to front-end according the request
	 * type <br>
	 * 
	 * @deprecated
	 */
	public void notfound() {
		notfound(null);
	}

	/**
	 * show notfound with message
	 * 
	 * @param message
	 */
	final public void notfound(String message) {
		if (log.isWarnEnabled())
			log.warn(this.getClass().getName() + "[" + this.uri() + "]");

		Controller m = Module.home.getModel(method.name, "/notfound", null);

		if (m != null) {
			if (log.isDebugEnabled())
				log.debug("m.class=" + m.getClass() + ", this.class=" + this.getClass());
		}

		if (m != null && !m.getClass().equals(this.getClass())) {
			try {
				log.info("m.class=" + m.getClass() + ", this.class=" + this.getClass());

				m.copy(this);

				if (X.isSame("GET", method)) {
					m.onGet();
				} else {
					m.onPost();
				}

				status = m.status();
				return;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

		if (isAjax()) {
			JSON jo = new JSON();
			jo.put(X.STATE, HttpServletResponse.SC_NOT_FOUND);
			jo.put(X.MESSAGE, "not found, " + message);
			this.send(jo);
		} else {
			this.set("me", this.user());
			this.print("not found, " + message);
			this.status(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	/**
	 * test the request is Ajax ? <br>
	 * X-Request-With:XMLHttpRequest<br>
	 * Content-Type: application/json <br>
	 * output: json <br>
	 * 
	 * @return true: yes
	 */
	public boolean isAjax() {

		String request = this.head("X-Requested-With");
		if (X.isSame(request, "XMLHttpRequest")) {
			return true;
		}

		String type = this.head("Content-Type");
		if (X.isSame(type, "application/json")) {
			return true;
		}

		String output = this.getString("output");
		if (X.isSame("json", output)) {
			return true;
		}

		output = this.head("output");
		if (X.isSame("json", output)) {
			return true;
		}

		String accept = this.head("Accept");
		if (!X.isEmpty(accept) && accept.indexOf("application/json") > -1 && accept.indexOf("text/html") == -1) {
			return true;
		}

		return false;
	}

	/**
	 * set the sponse status code.
	 * 
	 * @param statuscode the status code to response
	 */
	final public void status(int statuscode) {
		status = statuscode;
		resp.setStatus(statuscode);

		// GLog.applog.info("test", "test", "status=" + statuscode, null, null);
	}

	/**
	 * send error code and empty message
	 * 
	 * @param code
	 */
	final public void send(int code) {

		status = code;

		JSON j1 = JSON.create();
		if (data != null && !data.isEmpty()) {
			j1.putAll(data);
		}
		send(j1.append(X.STATE, code));
	}

	final public void error(int code) {
		try {
			status = code;

			resp.sendError(code, (String) data.get(X.MESSAGE));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}

	/**
	 * show deny page to end-user <br>
	 * if the request is AJAX, then response json back to front
	 */
	final public void deny() {
		deny(null, null);
	}

	/**
	 * show deny page with error info to end-user
	 * 
	 * @param url   the url will be responsed
	 * @param error the error that will be displaied
	 */
	final public void deny(String url, String error) {
		if (log.isDebugEnabled())
			log.debug(this.getClass().getName() + "[" + this.uri() + "]", new Exception("deny " + error));

		if (isAjax()) {

			JSON jo = new JSON();
			jo.put(X.STATE, HttpServletResponse.SC_UNAUTHORIZED);
			jo.put(X.MESSAGE, lang.get("access.deny"));
			jo.put(X.ERROR, error);
			jo.put(X.URL, url);
			this.send(jo);

		} else {

			status(HttpServletResponse.SC_FORBIDDEN);
			this.set("me", this.user());
			this.set(X.ERROR, error);
			this.set(X.URL, url);
			this.show("/deny.html");

		}

	}

	/**
	 * @deprecated
	 * @return
	 */
	public String getMethod() {
		return method();
	}

	/**
	 * get the request method, GET/POST
	 * 
	 * @return int
	 */
	final public String method() {
		return method.name;
	}

	/**
	 * MIME TYPE of JSON
	 */
	private static String MIME_JSON = "application/json;charset=" + ENCODING;

	/**
	 * MIME TYPE of HTML
	 */
	private static String MIME_HTML = "text/html;charset=" + ENCODING;

	/**
	 * Copy all request params from the model.
	 *
	 * @param m the model of source
	 */
	final public void copy(Controller m) {

		this.init(m.uri, m.req, m.resp, m.method.name);
		this.login = m.login;

		if (this._multipart) {
			this.uploads = m._get_files();
		}

	}

	/**
	 * pathmapping structure: {"method", {"path", Path|Method}}
	 */
	Map<String, Map<String, PathMapping>> pathmapping;

	/**
	 * println the object to end-user
	 * 
	 * @param o the object of printing
	 */
//	final public void println(Object o) {
//		print(o + "<br>");
//	}

	/**
	 * Print the object to end-user
	 * 
	 * @param o the object of printing
	 */
	final public void print(Object o) {
		try {
			PrintWriter writer = resp.getWriter();
			writer.write("<pre>" + X.toString(o) + "</pre>");
			writer.flush();
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(o, e);
		}
	}

	/**
	 * PathMapping inner class
	 * 
	 * @author joe
	 *
	 */
	protected static class PathMapping {
		Pattern pattern;
		Method method;
		Path path;

		/**
		 * Creates the Pathmapping
		 * 
		 * @param pattern the pattern
		 * @param path    the path
		 * @param method  the method
		 * @return the path mapping
		 */
		public static PathMapping create(Pattern pattern, Path path, Method method) {
			PathMapping e = new PathMapping();
			e.pattern = pattern;
			e.path = path;
			e.method = method;
			return e;
		}

	}

//	transient String tostring;
//
//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see java.lang.Object.toString()
//	 */
//	@Override
//	public String toString() {
//		if (tostring == null) {
//			tostring = new StringBuilder(this.getClass().getName()).append("[").append(this.uri).append(", path=")
//					.append(this.path).append("]").toString();
//		}
//		return tostring;
//	}

	/**
	 * get the name pair from the request query
	 * 
	 * @deprecated
	 * @return NameValue[]
	 */
	public NameValue[] getQueries() {
		Enumeration<String> e = req.getParameterNames();
		if (e != null) {
			List<NameValue> list = new ArrayList<NameValue>();
			while (e.hasMoreElements()) {
				String n = e.nextElement();
				list.add(NameValue.create(n, Helper.toString(req.getParameterValues(n))));
			}

			return list.toArray(new NameValue[list.size()]);
		}
		return null;
	}

	/**
	 * get the name pair from the request header
	 * 
	 * @return NameValue[]
	 */
	public NameValue[] heads() {
		Enumeration<String> e = req.getHeaderNames();
		if (e != null) {
			List<NameValue> list = new ArrayList<NameValue>();
			while (e.hasMoreElements()) {
				String n = e.nextElement();
				list.add(NameValue.create(n, req.getHeader(n)));
			}

			return list.toArray(new NameValue[list.size()]);
		}

		return null;
	}

	/**
	 * the {@code NameValue} Class used to contain the name and value
	 * 
	 * @author joe
	 *
	 */
	public static class NameValue {
		public String name;
		public String value;

		/**
		 * Creates the.
		 * 
		 * @param name  the name
		 * @param value the value
		 * @return the name value
		 */
		public static NameValue create(String name, String value) {
			NameValue h = new NameValue();
			h.name = name;
			h.value = value;
			return h;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object.toString()
		 */
		public String toString() {
			return new StringBuilder(name).append("=").append(value).toString();
		}
	}

	/**
	 * set the current module
	 * 
	 * @param e the module
	 */
//	public static void setCurrentModule(Module e) {
//		_currentmodule.set(e);
//	}

	public static void main(String[] args) {
		String s = "aaa.tgz";
		System.out.println(Controller.getMimeType(s));
	}

	public static class HttpMethod {

		public static final HttpMethod GET = HttpMethod.create("GET");
		public static final HttpMethod POST = HttpMethod.create("POST");

		public String name;

		public static HttpMethod create(String s) {
			return new HttpMethod(s);
		}

		private HttpMethod(String s) {
			this.name = s.toUpperCase();
		}

		public boolean is(String name) {
			return X.isSame(name, this.name);
		}

		public boolean isGet() {
			return is("GET");
		}

		public boolean isPost() {
			return is("POST");
		}

		public boolean isPut() {
			return is("PUT");
		}

		public boolean isOptions() {
			return is("OPTIONS");
		}

		@Override
		public String toString() {
			return name;
		}

	}

	public void send(String name, InputStream in, long total) {

		try {

			this.setContentType("application/octet-stream");
			name = Url.encode(name);
			this.head("Content-Disposition", "attachment; filename*=UTF-8''" + name);

			String range = this.head("range");

			long start = 0;
			long end = total;
			if (!X.isEmpty(range)) {
				String[] ss = range.split("(=|-)");
				if (ss.length > 1) {
					start = X.toLong(ss[1]);
				}

				if (ss.length > 2) {
					end = Math.min(total, X.toLong(ss[2]));
				}
			}

			if (end <= start) {
				end = start + 1024 * 1024;
			}

			if (end > total) {
				end = total;
			}

			long length = end - start;

			if (end < total) {
				this.status(206);
			}

			if (start == 0) {
				this.head("Accept-Ranges", "bytes");
			}
			this.head("Content-Length", Long.toString(length));
			this.head("Content-Range", "bytes " + start + "-" + (end - 1) + "/" + total);

			log.info("response.stream, bytes " + start + "-" + (end - 1) + "/" + total);
			if (length > 0) {
				OutputStream out = this.getOutputStream();

				IOUtil.copy(in, out, start, end, false);
				out.flush();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			X.close(in);
		}

	}

	/**
	 * Inits the.
	 * 
	 * @param conf the conf
	 * @param path the path
	 */
	public static void init(Configuration conf, String path) {

		log.info("controller init ... ");

//		OS = System.getProperty("os.name").toLowerCase() + "_" + System.getProperty("os.version") + "_"
//				+ System.getProperty("os.arch");

		Controller.MODULE_HOME = Controller.GIIWA_HOME + "/modules";

		/**
		 * initialize the module
		 */
//		System.out.println("init modules");
		Module.init(conf);

//		System.out.println("init welcome page");
		// get welcome list
		_init_welcome();

		log.info("controller has been initialized.");
	}

	@SuppressWarnings("unchecked")
	private static void _init_welcome() {
		try {
			SAXReader reader = new SAXReader();
			Document document = reader.read(Controller.MODULE_HOME + "/WEB-INF/web.xml");
			Element root = document.getRootElement();
			Element e1 = root.element("welcome-file-list");
			List<Element> l1 = e1.elements("welcome-file");

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
	public static Controller getModel(String method, String uri, String original) {
		return Module.home.getModel(method, uri, original);
	}

	/**
	 * process.
	 * 
	 * @param uri    the uri
	 * @param req    the req
	 * @param resp   the resp
	 * @param method the method
	 * @throws IOException
	 */
	public static void process(String uri, HttpServletRequest req, HttpServletResponse resp, String method, TimeStamp t)
			throws IOException {

		// log.debug("uri=" + uri);

//		String node = null;

//		String node = req.getParameter("__node");
//		if (!X.isEmpty(node) && !X.isSame(node, Local.id())) {
//			Node n = Node.dao
//					.load(W.create(X.ID, node).and("updated", System.currentTimeMillis() - Node.LOST, W.OP.gte));
//			if (n != null) {
//				n.forward(uri, req, resp, method);
//				return;
//			}
//		}

//		if (!uri.endsWith(".js") && !uri.endsWith(".css")) {
//			uri = Url.decode(uri);
//		}

		/**
		 * test and load from cache first
		 */
		Controller mo = Module.home.loadModelFromCache(method, uri);
		if (mo != null) {
//			mo.put("__node", node);

			if (log.isDebugEnabled())
				log.debug("cost=" + t.past() + ", find model, uri=" + uri + ", model=" + mo);

			mo.dispatch(uri, req, resp, method);

			return;
		}

//		if (log.isDebugEnabled())
//			log.debug("cost=" + t.past() + ", no model for uri=" + uri);

		Controller m1 = getModel(method, uri, uri);
		if (m1 != null) {
			m1.dispatch(uri, req, resp, method);
			return;
		}

		// parallel
		try {
			// directly file
			String filename = uri;
			if (!uri.endsWith(".js") && !uri.endsWith(".css")) {
				filename = Url.decode(uri);
			}

			File f = Module.home.getFile(filename);
			if (f != null && f.exists() && f.isFile()) {

				if (log.isDebugEnabled())
					log.debug("cost " + t.past() + ", find file, uri=" + uri);

				Controller m = new DefaultController();
				m.req = req;
				m.resp = resp;
//				m.set(m);

				m.put("me", m.user());
				m.put("lang", m.lang);
				m.put(X.URI, uri);
				m.put("module", Module.home);
				m.put("req", m);
				m.put("global", Global.getInstance());
				m.put("conf", Config.getConf());
				m.put("local", Local.getInstance());
				m.put("requestid", UID.random(20));

				View.merge(f, m, uri);

				return;
			}

			// file in file.repo
//			DFile f1 = Disk.seek(uri);
//			if (f1 != null && f1.exists() && f1.isFile()) {
//
//				if (log.isDebugEnabled())
//					log.debug("cost " + t.past() + ", find dfile, uri=" + uri);
//
//				Controller m = new DefaultController();
//				m.req = req;
//				m.resp = resp;
//				m.set(m);
//
//				m.put("me", m.getUser());
//				m.put("lang", m.lang);
//				m.put(X.URI, uri);
//				m.put("module", Module.home);
//				m.put("request", req);
//				m.put("this", m);
//				m.put("response", resp);
//				m.put("session", m.getSession(false));
//				m.put("global", Global.getInstance());
//				m.put("conf", Config.getConf());
//				m.put("local", Local.getInstance());
//				m.put("requestid", UID.random(20));
//
//				// String name = Url.encode(f1.getName());
//				// m.addHeader("Content-Disposition", "attachment; filename*=UTF-8''" + name);
//
//				m.setContentType(Controller.getMimeType(f1.getName()));
//
//				View.merge(f1, m, uri);
//
//				return;
//			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		// dispatch to model
		if (X.isEmpty(uri) || uri.endsWith("/")) {

//			log.debug("uri=" + uri);

			Controller[] m = new Controller[1];

			for (String s : welcomes) {
				Controller m2 = getModel(method, uri + s, uri);
				if (m2 != null) {
					m[0] = m2;
					break;
				}
			}

//			for (String suffix : welcomes) {
//				if (_dispatch(uri + "/" + suffix, req, resp, method, t)) {
//					return;
//				}
//			}

			if (m[0] != null) {
				m[0].dispatch(uri, req, resp, method);
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
			mo = getModel(method, u, uri);
			if (mo != null) {

				if (log.isDebugEnabled())
					log.debug("cost " + t.past() + ", find the model, uri=" + uri + ", model=" + mo);

//				mo.put("__node", node);

				mo.path = path;

//					Path p = 
				mo.dispatch(u, req, resp, method);

//					if (p == null) {
				if (log.isInfoEnabled())
					log.info(method + " " + uri + " - " + mo.status() + " - " + t.past() + " -" + mo.ip() + " " + mo);

//					if (AccessLog.isOn()) {
//
//						V v = V.create("method", method.toString()).set("cost", t.past()).set("sid", mo.sid());
//						User u1 = mo.getUser();
//						if (u1 != null) {
//							v.set("uid", u1.getId()).set("username", u1.get("name"));
//						}
//
//						AccessLog.create(mo.getRemoteHost(), uri,
//								v.set("status", mo.getStatus()).set("client", mo.browser())
//										.set("header", Arrays.toString(mo.getHeaders()))
//										.set("module", mo.module == null ? X.EMPTY : mo.module.getName())
//										.set("model", mo.getClass().getName()));
//					}
//					}

				// Counter.max("web.request.max", t.past(), uri);
				return;
			}
			i = uri.lastIndexOf("/", i - 1);
		}

		/**
		 * not found, then using dummymodel instead, and cache it
		 */
		if (log.isDebugEnabled())
			log.debug("cost " + t.past() + ", no model, using default, uri=" + uri);

		mo = new DefaultController();
		mo.module = Module.load(0);
//		mo.put("__node", node);

		/**
		 * do not put in model cache, <br>
		 * let's front-end (CDN, http server) do the the "static" resource cache
		 */
		// Module.home.modelMap.put(uri, (Class<Model>)
		// mo.getClass());
		mo.dispatch(uri, req, resp, method);

		if (log.isInfoEnabled())
			log.info(method + " " + uri + " - " + mo.status() + " - " + t.past() + " -" + mo.ip() + " " + mo);
//			if (AccessLog.isOn()) {
//
//				V v = V.create("method", method.toString()).set("cost", t.past()).set("sid", mo.sid());
//				User u1 = mo.getUser();
//				if (u1 != null) {
//					v.set("uid", u1.getId()).set("username", u1.get("name"));
//				}
//
//				AccessLog.create(mo.getRemoteHost(), uri,
//						v.set("status", mo.getStatus()).set("client", mo.browser())
//								.set("header", Arrays.toString(mo.getHeaders()))
//								.set("module", mo.module == null ? X.EMPTY : mo.module.getName())
//								.set("model", mo.getClass().getName()));
//			}

		// Counter.max("web.request.max", t.past(), uri);

	}

}
