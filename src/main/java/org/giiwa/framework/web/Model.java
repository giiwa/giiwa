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

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.*;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.*;
import org.giiwa.core.base.H64;
import org.giiwa.core.base.Html;
import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.core.json.JSON;
import org.giiwa.framework.bean.*;
import org.giiwa.framework.web.view.View;

/**
 * the {@code Model} Class is base model, all class that provides web api should
 * inherit it <br>
 * it provides api mapping, path/method mapping, contains all the parameters
 * from the request, and contains all key/value will be pass to view. <br>
 * it package all the request in getHeader, or getString method.
 * 
 * <br>
 * the most important method:
 * 
 * <pre>
 * getString(String name), get the value of parameter in request, it will convert HTML tag to "special" tag, to avoid destroy;
 * getHtml(String name), get the value of the parameter in request as original HTML format;
 * getHeader(String name),get the value from header;
 * getFile(String name),get the File value from the request;
 * set(String, Object), set the key/value back to view;
 * </pre>
 * 
 * @author yjiang
 * 
 */
public class Model {
  public static Log                        log            = LogFactory.getLog(Model.class);

  public static String                     ENCODING       = "UTF-8";

  /**
   * the original request (http, mdc)
   */
  public HttpServletRequest                req;

  /**
   * the original response
   */
  public HttpServletResponse               resp;

  /**
   * language map
   */
  public Language                          lang;

  /**
   * the request method(POST, GET)
   */
  public HTTPMethod                        method;

  /**
   * the response context, includes all the response key-value, used by
   * view(html)
   */
  public Map<String, Object>               context;

  /**
   * the home of the webgiiwa
   */
  public static String                     HOME;

  /**
   * the home of the giiwa
   */
  public static String                     GIIWA_HOME;

  /**
   * session id
   */
  private String                           sid;

  /**
   * locale of user
   */
  public String                            locale;

  /**
   * the uri of request
   */
  public String                            uri;

  /**
   * the module of this model
   */
  public Module                            module;

  /**
   * the query string of the request
   */
  public QueryString                       query;

  public static ServletContext             s️ervletContext;

  /**
   * the uptime of the system
   */
  public final static long                 UPTIME         = System.currentTimeMillis();

  /**
   * contentType of response
   */
  private String                           contentType;

  /**
   * associated login user
   */
  protected User                           login          = null;

  private static final ThreadLocal<Module> _currentmodule = new ThreadLocal<Module>();

  /**
   * get the request as inputstream.
   * 
   * @return InputStream
   * @throws IOException
   *           occur error when get the inputstream from request
   */
  final public InputStream getInputStream() throws IOException {
    return req.getInputStream();
  }

  /**
   * get the response as outputstream.
   * 
   * @return OutputStream
   * @throws IOException
   *           occur error when get the outputstream from the reqponse.
   */
  final public OutputStream getOutputStream() throws IOException {
    return resp.getOutputStream();
  }

  /**
   * the response status
   */
  public int status = HttpServletResponse.SC_OK;

  /**
   * get the response state code
   * 
   * @return int of state code
   */
  final public int getStatus() {
    return status;
  }

  /**
   * get the locale
   * 
   * @return String
   */
  final public String getLocale() {
    if (locale == null) {
      locale = this.getString("lang");
      if (locale == null) {
        /**
         * get the language from the cookie or default setting;
         */
        locale = getCookie("lang");
        if (locale == null) {
          // locale = Module._conf.getString("default.locale",
          // "en_us");
          //
          // /**
          // * get from the default, then set back to the cookie
          // */
          // this.addCookie("lang", locale, (int) (X.AYEAR / 1000));
          locale = Module.home.getLanguage();
        }
      } else {
        /**
         * get the language from the query, then set back in the cookie;
         */
        this.addCookie("lang", locale, (int) (X.AYEAR / 1000));
      }
    }

    // System.out.println("lang:" + locale);
    return locale;
  }

  /**
   * Current module.
   * 
   * @return the module
   */
  public static Module currentModule() {
    return _currentmodule.get();
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
   * @return Path
   */
  final public Path dispatch(String uri, HttpServletRequest req, HttpServletResponse resp, HTTPMethod method) {

    // created = System.currentTimeMillis();

    // construct var
    // init
    try {

      _currentmodule.set(module);

      this.req = req;
      this.resp = resp;
      this.method = method;
      this.uri = uri;

      this._multipart = ServletFileUpload.isMultipartContent(req);

      req.setCharacterEncoding(ENCODING);

      /**
       * set default data in model
       */
      this.lang = Language.getLanguage(getLocale());

      // if path exists, then using pathmapping instead
      // log.debug("pathmapping=" + pathmapping);

      if (!Module.home.before(this)) {
        log.debug("handled by filter, and stop to dispatch");
        return null;
      }

      if (pathmapping != null) {

        String path = this.path;
        if (X.isEmpty(this.path)) {
          path = X.NONE;
        }

        Map<String, PathMapping> methods = pathmapping.get(this.method.method);

        // log.debug(this.method + "=>" + methods);

        if (methods != null) {
          for (String s : methods.keySet()) {
            if (X.isEmpty(s)) {
              continue;
            }

            /**
             * catch the exception avoid break the whole block
             */
            try {
              /**
               * match test in outside first
               */
              // log.debug(s + "=>" + this.path);

              if (path != null && path.matches(s)) {

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
                      this.redirect("/setup");
                      return null;
                    }

                    login = this.getUser();
                    if (login == null) {
                      /**
                       * login require
                       */
                      gotoLogin();
                      return pp;
                    }

                    if (!X.NONE.equals(pp.access()) && !login.hasAccess(pp.access().split("\\|"))) {
                      /**
                       * no access
                       */
                      this.put("lang", lang);
                      this.deny();

                      OpLog.warn(this.getClass(), pp.path(), "deny the access, requred: " + lang.get(pp.access()),
                          getUser(), this.getRemoteHost());
                      return pp;
                    }
                  }

                  /**
                   * set the "global" attribute for the model
                   */
                  switch (this.method.method) {
                    case METHOD_POST:
                    case METHOD_GET:
                      this.put("lang", lang);
                      this.put(X.URI, uri);
                      this.put("module", Module.home);
                      this.put("path", this.path); // set
                      // original
                      // path
                      this.put("request", req);
                      this.put("response", resp);
                      this.put("this", this);
                      this.set("me", login);
                      this.set("session", this.getSession());
                      this.set("global", Global.getInstance());

                      createQuery();

                      break;
                  }

                  /**
                   * invoke the method
                   */
                  Method m = oo.method;
                  // log.debug("invoking: " + m.getName());

                  try {
                    m.invoke(this, params);

                    if ((pp.log() & method.method) > 0) {

                      /**
                       * clone a new one
                       */
                      JSON jo = JSON.fromObject(this.getJSON());
                      if (jo.has("password")) {
                        jo.put("password", "******");
                      }
                      if (jo.has("pwd")) {
                        jo.put("pwd", "******");
                      }
                      if (jo.has("passwd")) {
                        jo.put("passwd", "******");
                      }

                      OpLog.info(this.getClass(), pp.path(), jo.toString(), getUser(), this.getRemoteHost());

                    }
                  } catch (Exception e) {
                    if (log.isErrorEnabled())
                      log.error(e.getMessage(), e);

                    OpLog.error(this.getClass(), pp.path(), e.getMessage(), e, getUser(), this.getRemoteHost());

                    error(e);
                  }

                  return pp;
                }
              }
            } catch (Exception e) {
              if (log.isErrorEnabled())
                log.error(s, e);

              OpLog.error(this.getClass(), path, e.getMessage(), e, getUser(), this.getRemoteHost());

              error(e);
            }
          }
        }
      } // end of "pathmapping is not null

      /**
       * default handler
       */
      this.put("lang", lang);
      this.put("uri", uri);
      this.put("module", Module.home);
      this.put("path", path);
      this.put("request", req);
      this.put("this", this);
      this.put("response", resp);
      this.set("session", this.getSession());
      this.set("global", Global.getInstance());

      this.createQuery();

      switch (method.method) {
        case METHOD_GET: {

          Method m = this.getClass().getMethod("onGet");
          log.debug("m=" + m);
          if (m != null) {
            Path p = m.getAnnotation(Path.class);
            if (p != null) {
              // check ogin
              if (p.login()) {
                if (this.getUser() == null) {
                  gotoLogin();
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

          onGet();

          break;
        }
        case METHOD_POST: {

          Method m = this.getClass().getMethod("onPost");
          if (m != null) {
            Path p = m.getAnnotation(Path.class);
            if (p != null) {
              // check ogin
              if (p.login()) {
                if (this.getUser() == null) {
                  gotoLogin();
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

          break;
        }
      } // end default handler

    } catch (Exception e) {
      error(e);
    } finally {
      _currentmodule.remove();

      Module.home.after(this);
    }
    return null;
  }

  private void createQuery() {
    String url = uri;
    if (this.path != null && !"index".equals(this.path)) {
      url += "/" + path;
    }

    if (url.endsWith("/")) {
      url = url.substring(0, url.length() - 1);
    }
    query = new QueryString(url).copy(this);
    this.set("query", query);

  }

  /**
   * Goto login.
   */
  final public void gotoLogin() {
    if (this.uri != null && this.uri.indexOf("/user/") < 0) {
      if (query == null) {
        createQuery();
      }
      if (!isAjax()) {
        Session.load(sid()).set("uri", this.query == null ? this.uri : this.query.path(this.uri).toString()).store();
      }
    }

    if (isAjax()) {
      JSON jo = JSON.create();

      jo.put(X.STATE, HttpServletResponse.SC_UNAUTHORIZED);
      this.setHeader("status", Integer.toString(HttpServletResponse.SC_UNAUTHORIZED));

      jo.put(X.MESSAGE, lang.get("login.required"));
      jo.put(X.ERROR, lang.get("not.login"));
      this.response(jo);

    } else {
      this.redirect("/user/login");
    }
  }

  /**
   * get the current session sid, if not present, create a new one, please refer
   * {@code sid(boolean)}.
   *
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
   * @param newSession
   *          if true and not presented, create a new one, otherwise return null
   * @return the string
   */
  final public String sid(boolean newSession) {
    if (X.isEmpty(sid)) {
      sid = this.getCookie("sid");
      if (X.isEmpty(sid)) {
        sid = this.getHeader("sid");
        if (X.isEmpty(sid)) {
          sid = this.getString("sid");
          if (X.isEmpty(sid) && newSession) {
            do {
              sid = H64.toString((int) (Math.random() * Integer.MAX_VALUE)) + H64.toString(System.currentTimeMillis());
            } while (Session.exists(sid));

          }
        }

        /**
         * get session.expired in seconds
         */
        addCookie("sid", sid, Global.getInt("session.expired", -1));
      }
    }

    return sid;
  }

  /**
   * get the token
   * 
   * @return String
   */
  final public String getToken() {
    String token = this.getCookie("token");
    if (token == null) {
      token = this.getHeader("token");
      if (token == null) {
        token = this.getString("token");
      }
    }
    return token;
  }

  /**
   * response and redirect to the url
   * 
   * @param url
   *          the url
   */
  final public void redirect(String url) {
    resp.setHeader("Location", url);
    setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
  }

  /**
   * Forward to the model(url), do not response yet
   * 
   * @param model
   *          the model
   */
  final public void forward(String model) {
    Controller.dispatch(model, req, resp, method);
  }

  /**
   * Put the name=object to the model.
   *
   * @param name
   *          the name
   * @param o
   *          the object
   */
  final public void put(String name, Object o) {
    // System.out.println("put:" + name + "=>" + o);

    if (context == null) {
      context = new HashMap<String, Object>();
    }
    if (name == null) {
      return;
    }

    if (o == null) {
      /**
       * clear
       */
      context.remove(name);
    } else {
      context.put(name, o);
    }

    return;
  }

  /**
   * remove the name from the model
   * 
   * @param name
   *          the name of data setted in model
   */
  final public void remove(String name) {
    if (context != null) {
      context.remove(name);
    }
  }

  /**
   * Sets name=object back to model which accessed by view.
   * 
   * @param name
   *          the name of data in model
   * @param o
   *          the value object
   */
  final public void set(String name, Object o) {
    put(name, o);
  }

  /**
   * get the value from the context
   * 
   * @param name
   *          the name in model which using in view
   * @param defaultValue
   *          the default value if the name not presented in model
   * @return Object
   */
  final public Object get(String name, Object defaultValue) {
    if (context != null) {
      return context.get(name);
    }
    return defaultValue;
  }

  /**
   * Sets response the header name=value.
   * 
   * @param name
   *          the name of header to response
   * @param value
   *          the String value
   */
  final public void setHeader(String name, String value) {
    if (resp.containsHeader(name)) {
      resp.setHeader(name, value);
    } else {
      addHeader(name, value);
    }
  }

  /**
   * set response the date header name=value
   * 
   * @param name
   *          the name of header to response
   * @param date
   *          the data value in header to response
   */
  final public void setDateHeader(String name, long date) {
    resp.setDateHeader(name, date);
  }

  /**
   * Sets Beans back to Model which accessed by view, it will auto paging
   * according the start and number per page.
   *
   * @param bs
   *          the Beans
   * @param s
   *          the start position
   * @param n
   *          the number per page
   */
  final public void set(Beans<? extends Bean> bs, int s, int n) {
    if (bs != null) {
      this.set("list", bs.getList());
      int total = bs.getTotal();
      if (total > 0) {
        this.set("total", total);
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
   * Sets Map back to the Model which accessed by view.
   * 
   * @param jo
   *          the map of data
   * @param names
   *          the names that will be set back to model, if null, will set all
   */
  final public void set(JSON jo, String... names) {
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
   * Checks if has the name in the model for response
   * 
   * @param name
   *          the name
   * @return true, if has
   */
  final public boolean has(String name) {
    return context != null && context.containsKey(name);
  }

  /**
   * Gets the request header.
   * 
   * @param tag
   *          the header tag
   * @return String of the header
   */
  final public String getHeader(String tag) {
    try {
      return req.getHeader(tag);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Adds the header.
   * 
   * @param tag
   *          the tag
   * @param v
   *          the v
   */
  final public void addHeader(String tag, String v) {
    try {
      resp.addHeader(tag, v);
    } catch (Exception e) {
    }
  }

  /**
   * Gets the int parameter from request
   * 
   * @param tag
   *          the tag
   * @return the int
   */
  final public int getInt(String tag) {
    return getInt(tag, 0);
  }

  /**
   * Gets the int from the request parameter, if the tag is not presented, then
   * get the value from the session, if the value less than minvalue, then get
   * the minvalue, and store the value in session
   * 
   * @param tag
   *          the tag
   * @param minValue
   *          the min value
   * @param tagInSession
   *          the tag in session
   * @return the int
   */
  final public int getInt(String tag, int minValue, String tagInSession) {
    int r = getInt(tag);
    if (r < minValue) {
      Session s = this.getSession();
      r = s.getInt(tagInSession);
      if (r < minValue) {
        r = Global.getInt(tagInSession, minValue);
      }
    } else {
      Session s = this.getSession();
      s.set(tagInSession, r).store();
    }

    // if (r > 500) {
    // r = 500;
    // log.error("the page number exceed max[500]: " + r);
    // }
    return r;
  }

  /**
   * get the parameter from the request, if not presented, then get from session
   * 
   * @param tag
   *          the name of the parameter in request.
   * @param tagInSession
   *          associated with the name in session.
   * @param defaultValue
   *          the default value if not presented in both.
   * @return String of the value
   */
  final public String getString(String tag, String tagInSession, String defaultValue) {
    String r = getString(tag);
    if (X.isEmpty(r)) {
      Session s = this.getSession();
      r = (String) s.get(tagInSession);
      if (X.isEmpty(r)) {
        r = defaultValue;
        s.set(tagInSession, r).store();
      }
    } else {
      Session s = this.getSession();
      s.set(tagInSession, r).store();
    }

    return r;
  }

  /**
   * Gets the int in request parameter, if not presented, return the
   * defaultvalue
   * 
   * @param tag
   *          the tag
   * @param defaultValue
   *          the default value
   * @return the int
   */
  final public int getInt(String tag, int defaultValue) {
    String v = this.getString(tag);
    return X.toInt(v, defaultValue);
  }

  /**
   * Gets the long request parameter, if not presented, return the defaultvalue
   * 
   * @param tag
   *          the name of parameter
   * @param defaultvalue
   *          the default value when the name not presented.
   * @return long
   */
  final public long getLong(String tag, long defaultvalue) {
    String v = this.getString(tag);
    return X.toLong(v, defaultvalue);
  }

  /**
   * get the long request value, if not presented, return 0;
   * 
   * @param tag
   *          the name of parameter in request.
   * @return long
   */
  final public long getLong(String tag) {
    return getLong(tag, 0);
  }

  /**
   * get all cookies
   * 
   * @return Cookie[]
   */
  final public Cookie[] getCookie() {
    return req.getCookies();
  }

  /**
   * Gets the cookie.
   * 
   * @param name
   *          the name
   * @return the cookie
   */
  final public String getCookie(String name) {
    Cookie[] cc = getCookie();
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
   * @return HttpServletRequest
   */
  final public HttpServletRequest getRequest() {
    return req;
  }

  /**
   * get the response
   * 
   * @return HttpServletResponse
   */
  final public HttpServletResponse getResponse() {
    return resp;
  }

  /**
   * get the user-agent of browser
   * 
   * @return the string
   */
  final public String browser() {
    return this.getHeader("user-agent");
  }

  /**
   * set the cookie back to the response.
   * 
   * @param key
   *          the name of cookie
   * @param value
   *          the value of the cookie
   * @param expireseconds
   *          the expire time of seconds.
   */
  final public void addCookie(String key, String value, int expireseconds) {
    if (key == null) {
      return;
    }

    Cookie c = new Cookie(key, value);
    if (value == null) {
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

    addCookie(c);
  }

  /**
   * Adds the cookie.
   * 
   * @param c
   *          the c
   */
  final public void addCookie(Cookie c) {
    if (c != null) {
      resp.addCookie(c);
    }
  }

  /**
   * the the request uri
   * 
   * @return String
   */
  final public String getURI() {
    String uri = req.getRequestURI();
    while (uri.indexOf("//") > -1) {
      uri = uri.replaceAll("//", "/");
    }
    return uri;
  }

  /**
   * get the sub.path of the uri
   * 
   * @return String
   */
  final public String getPath() {
    return path;
  }

  /**
   * trying to get the client ip from request header
   * 
   * @return String
   */
  final public String getRemoteHost() {
    String remote = this.getHeader("X-Forwarded-For");
    if (remote == null) {
      remote = getHeader("X-Real-IP");

      if (remote == null) {
        remote = req.getRemoteAddr();
      }
    }

    return remote;
  }

  /**
   * get local port
   * 
   * @return int of local port
   */
  final public int getPort() {
    String port = this.getHeader("Port");
    if (X.isEmpty(port)) {
      return req.getLocalPort();
    }

    return X.toInt(port, -1);
  }

  /**
   * get the local host name
   * 
   * @return String of local host
   */
  final public String getHost() {
    String host = this.getHeader("Host");
    if (X.isEmpty(host)) {
      host = req.getLocalAddr();
    }

    return host;
  }

  /**
   * get all parameter names
   * 
   * @return Enumeration
   */
  @SuppressWarnings("unchecked")
  final public Enumeration<String> getParameterNames() {
    try {
      return req.getParameterNames();
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(e.getMessage(), e);
      return null;
    }
  }

  private JSON _json;

  /**
   * get the request as JSON
   * 
   * @return JSONObject
   */
  final public JSON getJSON() {
    if (_json == null) {
      _json = JSON.create();
      for (String name : this.getNames()) {
        String s = this.getString(name);
        _json.put(name, s);
      }

      // mix the password
      // for (Object name : _json.keySet()) {
      // if ("password".equals(name) || "pwd".equals(name) ||
      // "passwd".equals(name)) {
      // _json.put(name, "*******");
      // }
      // }
    }
    return _json;
  }

  /**
   * get the request as JSON, it will mix the password key
   * 
   * @deprecated
   * @return JSONObject
   */
  final public JSON getJSONNonPassword() {
    if (_json == null) {
      getJSON();
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
   * Gets the value of request string parameter. it auto handle multiple-part,
   * and convert "&lt;" or "&gt;" to html char and normal request
   * 
   * @param name
   *          the name of parameter
   * @return string of requested value
   */
  final public String getString(String name) {
    try {
      String c1 = req.getContentType();
      if (c1 != null && c1.indexOf("application/json") > -1) {
        if (uploads == null) {
          BufferedReader in = req.getReader();

          StringBuilder sb = new StringBuilder();
          char[] buff = new char[1024];
          int len;
          while ((len = in.read(buff)) != -1) {
            sb.append(buff, 0, len);
          }

          log.debug("params=" + sb.toString());

          JSON jo = JSON.fromObject(sb.toString());
          uploads = new HashMap<String, Object>();
          uploads.putAll(jo);
        }

        Object v1 = uploads.get(name);
        if (v1 != null) {
          return v1.toString().trim();
        }
        return null;
      } else if (this._multipart) {
        getFiles();

        FileItem i = this.getFile(name);

        if (i != null && i.isFormField()) {
          InputStream in = i.getInputStream();
          byte[] bb = new byte[in.available()];
          in.read(bb);
          in.close();
          return new String(bb, "UTF8").replaceAll("<", "&lt;").replaceAll(">", "&gt;").trim();
        }

      } else {
        String[] ss = req.getParameterValues(name);
        if (ss != null && ss.length > 0) {
          String s = ss[ss.length - 1];
          return s.replaceAll("<", "&lt;").replaceAll(">", "&gt;").trim();
        }
      }

      return null;
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error("get request parameter " + name + " get exception.", e);
      return null;
    }
  }

  /**
   * Gets the request value by name, and limited length.
   * 
   * @param name
   *          the parameter name
   * @param maxlength
   *          the maxlength
   * @return string of value
   */
  final public String getString(String name, int maxlength) {
    String s = getString(name);
    if (!X.isEmpty(s)) {
      if (s.getBytes().length > maxlength) {
        s = Html.create(s).text(maxlength);
      }
    }

    return s;
  }

  /**
   * Gets the request value by name, and limited length, if not presented, using
   * the default value.
   * 
   * @param name
   *          the parameter name
   * @param maxlength
   *          the maxlength
   * @param defaultvalue
   *          the defaultvalue
   * @return string of value
   */
  final public String getString(String name, int maxlength, String defaultvalue) {
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
   * get the request value as html (original string), it maybe includes html
   * format
   * 
   * @param name
   *          the parameter name
   * @return String of value
   */
  final public String getHtml(String name) {
    return getHtml(name, false);
  }

  /**
   * Gets the html.
   * 
   * @param name
   *          the name of the parameter
   * @param all
   *          whether get all data
   * @return String
   */
  final public String getHtml(String name, boolean all) {
    try {
      String c1 = req.getContentType();
      if (c1 != null && c1.indexOf("application/json") > -1) {
        if (uploads == null) {
          BufferedReader in = req.getReader();

          StringBuilder sb = new StringBuilder();
          char[] buff = new char[1024];
          int len;
          while ((len = in.read(buff)) != -1) {
            sb.append(buff, 0, len);
          }

          log.debug("params=" + sb.toString());

          JSON jo = JSON.fromObject(sb.toString());
          uploads = new HashMap<String, Object>();
          uploads.putAll(jo);
        }

        Object v1 = uploads.get(name);
        if (v1 != null) {
          return v1.toString();
        }
        return null;
      } else if (this._multipart) {
        getFiles();

        FileItem i = this.getFile(name);

        if (i != null && i.isFormField()) {
          InputStream in = i.getInputStream();
          byte[] bb = new byte[in.available()];
          in.read(bb);
          in.close();
          return new String(bb, "UTF8");
        }
        return null;
      } else {

        String[] ss = req.getParameterValues(name);
        if (ss != null && ss.length > 0) {
          String s = ss[ss.length - 1];
          return s;
        }

        return null;
      }
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error("get request parameter " + name + " get exception.", e);
      return null;
    }
  }

  /**
   * Gets the html from the request, and cut by the maxlength
   * 
   * @param name
   *          the name
   * @param maxlength
   *          the maxlength
   * @return the html
   */
  final public String getHtml(String name, int maxlength) {
    String html = getHtml(name);
    if (!X.isEmpty(html)) {
      if (html.getBytes().length >= maxlength) {
        html = Html.create(html).text(maxlength);
      }
    }
    return html;
  }

  /**
   * Gets the html from the request, it will not convert anything for the html
   * 
   * @param name
   *          the name of request parameter
   * @param maxlength
   *          the maxlength
   * @param defaultvalue
   *          the defaultvalue
   * @return String of the html
   */
  final public String getHtml(String name, int maxlength, String defaultvalue) {
    String html = getHtml(name);
    if (!X.isEmpty(html)) {
      if (html.getBytes().length >= maxlength) {
        html = Html.create(html).text(maxlength);
      }
    } else {
      html = defaultvalue;
    }
    return html;
  }

  /**
   * Gets the strings from the request, <br>
   * and will convert the "&lt;" to "&amp;lt;", "&gt;" to "&amp;gt;"
   * 
   * @param name
   *          the name of the request parameter
   * @return String[] of request
   */
  @SuppressWarnings("unchecked")
  final public String[] getStrings(String name) {
    try {
      if (this._multipart) {
        getFiles();

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
            ss[i] = ss[i].replaceAll("<", "&lt").replaceAll(">", "&gt");
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
   * get the parameters names
   * 
   * @return List of the request names
   */
  final public List<String> getNames() {
    if (this._multipart) {
      getFiles();
      return new ArrayList<String>(uploads.keySet());
    } else {
      Enumeration<?> e = req.getParameterNames();
      List<String> list = new ArrayList<String>();

      while (e.hasMoreElements()) {
        list.add(e.nextElement().toString());
      }
      return list;
    }

  }

  /**
   * get the session, if not presented, then create a new, "user" should store
   * the session invoking session.store()
   * 
   * @return Session
   */
  final public Session getSession() {
    return Session.load(sid());
  }

  /**
   * Gets the http session.
   * 
   * @param bfCreate
   *          the bf create
   * @return the http session
   */
  final public HttpSession getHttpSession(boolean bfCreate) {

    return req.getSession(bfCreate);
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
  final public boolean isMultipart() {
    return _multipart;
  }

  /**
   * get the user associated with the session, <br>
   * this method will cause set user in session if not setting, <br>
   * to avoid this, you need invoke the "login" variable
   * 
   * @return User
   */
  final public User getUser() {
    if (login == null) {
      Session s = getSession();
      login = (User) s.get("user");

      if (login == null) {

        if (Global.getInt("user.token", 1) == 1) {
          String sid = sid();
          String token = getToken();
          if (!X.isEmpty(sid) && !X.isEmpty(token)) {
            AuthToken t = AuthToken.load(sid, token);
            if (t != null) {
              login = t.getUser_obj();
              this.setUser(login);
            }
          }
        }
      }

      // log.debug("getUser, user=" + login + " session=" + s);

    }

    return login;
  }

  /**
   * set the user associated with the session
   * 
   * @param u
   *          the user object associated with the session
   */
  final public void setUser(User u) {
    Session s = getSession();
    User u1 = (User) s.get("user");
    if (u != null && u1 != null && u1.getId() != u.getId()) {
      log.debug("clear the data in session");
      s.clear();
    }

    if (u != null) {
      s.set("user", u);
    } else {
      log.debug("clear the data in session");
      s.clear();
    }
    s.store();

    log.debug("store session: session=" + s + ", getSession=" + getSession());

    login = u;
  }

  /**
   * get files or multiple-part request
   * 
   * @return
   */
  @SuppressWarnings("unchecked")
  final private Map<String, Object> getFiles() {
    if (uploads == null) {
      uploads = new HashMap<String, Object>();
      DiskFileItemFactory factory = new DiskFileItemFactory();

      // Configure a repository (to ensure a secure temp location is used)
      File repository = (File) s️ervletContext.getAttribute("javax.servlet.context.tempdir");
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
   * Gets the request file by name.
   * 
   * @param name
   *          the parameter name
   * @return file of value, null if not presented
   */
  @SuppressWarnings("unchecked")
  final public FileItem getFile(String name) {

    getFiles();

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
  public String               path;

  /**
   * set the response contenttype
   * 
   * @param contentType
   *          the content type in response
   */
  final public void setContentType(String contentType) {
    this.contentType = contentType;
    resp.setContentType(contentType);
  }

  /**
   * return the contentType
   * 
   * @return String of content-type
   */
  final public String getResponseContentType() {
    if (contentType == null) {
      return MIME_HTML;
    } else {
      return contentType;
    }
  }

  /**
   * output the json as "application/json" to end-user
   * 
   * @param jo
   *          the json that will be output
   */
  final public void response(JSON jo) {
    if (jo == null) {
      responseJson("{}");
    } else {
      responseJson(jo.toString());
    }
  }

  /**
   * response "json" to end-user directly
   * 
   * @param state
   *          the status to response
   * @param message
   *          the message to response
   */
  final public void response(int state, String message) {
    JSON jo = JSON.create();
    jo.put(X.STATE, HttpServletResponse.SC_OK);
    jo.put(X.MESSAGE, message);
    this.response(jo);
  }

  /**
   * output the jsonarr as "application/json" to end-user
   * 
   * @param arr
   *          the array of json
   */
  final public void response(List<JSON> arr) {
    if (arr == null) {
      responseJson("[]");
    } else {
      responseJson(arr.toString());
    }
  }

  /**
   * output the string as "application/json" to end-userr.
   * 
   * @param jsonstr
   *          the jsonstr string
   */
  private void responseJson(String jsonstr) {
    this.setContentType(Model.MIME_JSON);
    this.print(jsonstr);
  }

  /**
   * using current model to render the template, and show the result html page
   * to end-user.
   * 
   * @param viewname
   *          the viewname template
   * @return boolean
   */
  final public boolean show(String viewname) {

    try {
      this.set("path", this.path);
      this.set("query", this.query);

      // TimeStamp t1 = TimeStamp.create();
      File file = Module.home.getFile(viewname);
      if (file != null && file.exists()) {
        View.merge(file, this, viewname);

        // if (log.isDebugEnabled())
        // log.debug("showing viewname = " + viewname + ", cost: " + t1.past() +
        // "ms");
      } else {
        notfound();
      }
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(viewname, e);

      // error(e);
    }

    return false;
  }

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
    if (module != null) {
      Module t = module.floor();
      if (t != null) {
        Model m = t.getModel(Model.METHOD_POST, uri);
        if (m != null) {
          m.dispatch(uri, req, resp, method);
          return;
        }
      }
    }

    String uri = req.getRequestURI();
    while (uri.indexOf("//") > -1) {
      uri = uri.replaceAll("//", "/");
    }
    Model mo = new DefaultModel();
    mo.module = Module.load(0);
    mo.copy(this);
    mo.dispatch(uri, req, resp, method);

  }

  /**
   * set current path.
   * 
   * @deprecated
   * @param path
   *          rewrite the path
   */
  final public void setPath(String path) {
    this.path = path;
  }

  /**
   * Get the mime type.
   * 
   * @param uri
   *          the type of uri
   * @return String of mime type
   */
  public static String getMimeType(String uri) {
    return s️ervletContext.getMimeType(uri);
  }

  /**
   * Show error page to end user.<br>
   * if the request is AJAX, then response a json with error back to front
   * 
   * @param e
   *          the throwable
   */
  final public void error(Throwable e) {
    if (log.isErrorEnabled())
      log.error(e.getMessage(), e);

    StringWriter sw = new StringWriter();
    PrintWriter out = new PrintWriter(sw);
    e.printStackTrace(out);
    String s = sw.toString();

    if (isAjax()) {
      JSON jo = JSON.create();
      jo.put(X.STATE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      jo.put(X.MESSAGE, s);
      this.response(jo);
    } else {
      this.set("me", this.getUser());

      String lineSeparator = System.lineSeparator();
      s = s.replaceAll(lineSeparator, "<br/>");
      s = s.replaceAll(" ", "&nbsp;");
      s = s.replaceAll("\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
      this.set("error", s);

      this.show("/error.html");
      setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

  }

  /**
   * default notfound handler <br>
   * 1) look for "/notfound" model, if found, dispatch to it. <br>
   * 2) else response notfound page or json to front-end according the request
   * type <br>
   */
  final public void notfound() {
    if (log.isWarnEnabled())
      log.warn(this.getClass().getName() + "[" + this.getURI() + "]");

    Model m = Module.home.getModel(method.method, "/notfound");
    // log.debug("m=" + m);
    if (m != null && !m.getClass().equals(this.getClass())) {
      try {
        m.copy(this);

        if (method.isGet()) {
          m.onGet();
        } else {
          m.onPost();
        }

        return;
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
    }

    if (isAjax()) {
      JSON jo = new JSON();
      jo.put(X.STATE, HttpServletResponse.SC_NOT_FOUND);
      jo.put(X.MESSAGE, "not found");
      this.response(jo);
    } else {
      this.set("me", this.getUser());
      this.show("/notfound.html");
      this.setStatus(HttpServletResponse.SC_NOT_FOUND);
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
  protected boolean isAjax() {
    String request = this.getHeader("X-Requested-With");
    if (request != null && request.equals("XMLHttpRequest")) {
      return true;
    }

    String type = this.getHeader("Content-Type");
    if (type != null && type.contains("application/json")) {
      return true;
    }

    String output = this.getString("output");
    if (X.isSame("json", output)) {
      return true;
    }

    output = this.getHeader("output");
    if (X.isSame("json", output)) {
      return true;
    }

    return false;
  }

  /**
   * set the sponse status code.
   * 
   * @param statuscode
   *          the status code to response
   */
  final public void setStatus(int statuscode) {
    resp.setStatus(statuscode);
    status = statuscode;
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
   * @param url
   *          the url will be responsed
   * @param error
   *          the error that will be displaied
   */
  final public void deny(String url, String error) {
    if (log.isDebugEnabled())
      log.debug(this.getClass().getName() + "[" + this.getURI() + "]", new Exception("deny " + error));

    if (isAjax()) {

      JSON jo = new JSON();
      jo.put(X.STATE, HttpServletResponse.SC_UNAUTHORIZED);
      jo.put(X.MESSAGE, lang.get("access.deny"));
      jo.put(X.ERROR, error);
      jo.put(X.URL, url);
      this.response(jo);

    } else {

      setStatus(HttpServletResponse.SC_FORBIDDEN);
      this.set("me", this.getUser());
      this.set(X.ERROR, error);
      this.set(X.URL, url);
      this.show("/deny.html");

    }

  }

  /**
   * get the request method, GET/POST
   * 
   * @return int
   */
  final public int getMethod() {
    return method.method;
  }

  /**
   * HTTP GET
   */
  final public static int    METHOD_GET  = 1;

  /**
   * HTTP POST
   */
  final public static int    METHOD_POST = 2;

  /**
   * MIME TYPE of JSON
   */
  final public static String MIME_JSON   = "application/json;charset=" + ENCODING;

  /**
   * MIME TYPE of HTML
   */
  final public static String MIME_HTML   = "text/html;charset=" + ENCODING;

  /**
   * Copy all request params from the model.
   *
   * @param m
   *          the model of source
   */
  final public void copy(Model m) {

    this.req = m.req;
    this.resp = m.resp;
    this.contentType = m.contentType;
    this.context = m.context;
    this.lang = m.lang;
    this.locale = m.locale;
    this.login = m.login;
    this.method = m.method;
    this.path = m.path;
    this.sid = m.sid;
    this.uri = m.uri;
    this._multipart = ServletFileUpload.isMultipartContent(req);

    if (this._multipart) {
      this.uploads = m.getFiles();
    }

  }

  /**
   * pathmapping structure: {"method", {"path", Path|Method}}
   */
  public Map<Integer, Map<String, PathMapping>> pathmapping;

  /**
   * println the object to end-user
   * 
   * @param o
   *          the object of printing
   */
  final public void println(Object o) {
    print(o);
  }

  /**
   * Print the object to end-user
   * 
   * @param o
   *          the object of printing
   */
  final public void print(Object o) {
    try {
      BufferedWriter writer = new BufferedWriter(resp.getWriter());

      writer.write(o.toString());
      writer.flush();
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(o, e);
    }
  }

  /**
   * Rebound the request parameters to response
   * 
   * @param names
   *          the names that request parameters will copied
   * @return int of copied
   */
  final public int rebound(String... names) {
    int count = 0;
    if (names != null && names.length > 0) {
      for (String name : names) {
        set(name, this.getString(name));
        count++;
      }
    } else {
      for (String name : this.getNames()) {
        set(name, this.getString(name));
        count++;
      }
    }
    return count;
  }

  /**
   * PathMapping inner class
   * 
   * @author joe
   *
   */
  protected static class PathMapping {
    Pattern pattern;
    Method  method;
    Path    path;

    /**
     * Creates the Pathmapping
     * 
     * @param pattern
     *          the pattern
     * @param path
     *          the path
     * @param method
     *          the method
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

  /**
   * the {@code HTTPMethod} is request method class, GET, POST
   * 
   * @author joe
   *
   */
  public static class HTTPMethod {
    int method = Model.METHOD_GET;

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      switch (method) {
        case Model.METHOD_GET: {
          return "GET";
        }
        case Model.METHOD_POST: {
          return "POST";
        }
      }
      return "Unknown";
    }

    /**
     * Instantiates a new HTTP method.
     * 
     * @param m
     *          the m
     */
    public HTTPMethod(int m) {
      this.method = m;
    }

    public boolean isGet() {
      return method == Model.METHOD_GET;
    }

    public boolean isPost() {
      return method == Model.METHOD_POST;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
      return method;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      HTTPMethod other = (HTTPMethod) obj;
      if (method != other.method)
        return false;
      return true;
    }

  }

  transient String tostring;

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  final public String toString() {
    if (tostring == null) {
      tostring = new StringBuilder(this.getClass().getName()).append("[").append(this.uri).append(", path=")
          .append(this.path).append("]").toString();
    }
    return tostring;
  }

  /**
   * get the name pair from the request query
   * 
   * @return NameValue[]
   */
  @SuppressWarnings("unchecked")
  final public NameValue[] getQueries() {
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

  private static String node = null;

  /**
   * get the name of this node in cluster.
   *
   * @return string of name
   */
  public static String node() {
    if (node == null) {
      node = Module._conf.getString("node", null);
    }
    return node;
  }

  /**
   * get the name pair from the request header
   * 
   * @return NameValue[]
   */
  @SuppressWarnings("unchecked")
  final public NameValue[] getHeaders() {
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
    String name;
    String value;

    /**
     * Creates the.
     * 
     * @param name
     *          the name
     * @param value
     *          the value
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
     * @see java.lang.Object#toString()
     */
    public String toString() {
      return new StringBuilder(name).append("=").append(value).toString();
    }
  }

  /**
   * set the current module
   * 
   * @param e
   *          the module
   */
  public static void setCurrentModule(Module e) {
    _currentmodule.set(e);
  }

}
