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

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.*;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.logging.*;
import org.apache.velocity.*;
import org.giiwa.core.base.H64;
import org.giiwa.core.base.Html;
import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.TimeStamp;
import org.giiwa.core.bean.X;
import org.giiwa.core.bean.Bean.V;
import org.giiwa.core.conf.Global;
import org.giiwa.framework.bean.*;

// TODO: Auto-generated Javadoc
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
  public VelocityContext                   context;

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

  /**
   * cache the template by viwename, the template will be override in child
   * module
   */
  private static Map<String, Template>     cache          = new HashMap<String, Template>();

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
  public User                              login          = null;

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
                    if (!Bean.isConfigured()) {
                      this.redirect("/configure");
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

                      OpLog.warn("deny", uri, "requred: " + lang.get(pp.access()), login.getId(), this.getRemoteHost());
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
                      this.set("me", login);
                      this.set("session", this.getSession());
                      this.set("system", Global.getInstance());

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

                      boolean error = false;

                      StringBuilder sb = new StringBuilder();
                      JSONObject jo = JSONObject.fromObject(this.getJSON());
                      if (jo.has("password")) {
                        jo.put("password", "******");
                      }
                      if (jo.has("pwd")) {
                        jo.put("pwd", "******");
                      }
                      if (jo.has("passwd")) {
                        jo.put("passwd", "******");
                      }

                      sb.append("<b>IN</b>=").append(jo.toString());
                      String message = null;
                      sb.append("; <b>OUT</b>=");
                      if (context != null) {
                        if (context.containsKey("jsonstr")) {
                          sb.append(context.get("jsonstr"));
                        } else {
                          jo = new JSONObject();
                          if (context.containsKey(X.MESSAGE)) {
                            jo.put(X.MESSAGE, context.get(X.MESSAGE));
                            message = jo.getString(X.MESSAGE);
                          }
                          if (context.containsKey(X.ERROR)) {
                            jo.put(X.ERROR, context.get(X.ERROR));
                            message = jo.getString(X.ERROR);
                            error = true;
                          }
                          sb.append(jo.toString());
                        }
                      }

                      if (error) {
                        OpLog.warn(this.getClass().getName(), path == null ? uri : uri + "/" + path, message,
                            sb.toString(), getUser() == null ? -1 : getUser().getId(), this.getRemoteHost());
                      } else {
                        OpLog.info(this.getClass().getName(), path == null ? uri : uri + "/" + path, message,
                            sb.toString(), getUser() == null ? -1 : getUser().getId(), this.getRemoteHost());
                      }
                    }
                  } catch (Exception e) {
                    if (log.isErrorEnabled())
                      log.error(e.getMessage(), e);

                    error(e);
                  }

                  return pp;
                }
              }
            } catch (Exception e) {
              if (log.isErrorEnabled())
                log.error(s, e);

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
      this.put("response", resp);
      this.set("session", this.getSession());
      this.set("system", Global.getInstance());
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

      Session.load(sid()).set("uri", this.query == null ? this.uri : this.query.path(this.uri).toString()).store();
    }

    String request = this.getHeader("X-Requested-With");
    if ("XMLHttpRequest".equals(request)) {
      JSONObject jo = new JSONObject();

      jo.put(X.STATE, 401);
      this.setHeader("status", "401");
      jo.put(X.STATE, 401);

      jo.put(X.MESSAGE, "请重现登录！");
      jo.put(X.ERROR, "没有登录信息！");
      // this.redirect("/user/login/popup");
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
    if (sid == null) {
      sid = this.getCookie("sid");
      if (sid == null) {
        sid = this.getHeader("sid");
        if (sid == null) {
          sid = this.getString("sid");
          if (sid == null && newSession) {
            do {
              sid = H64.toString((int) (Math.random() * Integer.MAX_VALUE)) + H64.toString(System.currentTimeMillis());
            } while (Session.exists(sid));

          }
        }

        /**
         * get session.expired in seconds
         */
        addCookie("sid", sid, Global.i("session.expired", -1));
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

  public JSONObject mockMdc = null;

  /**
   * Mock mdc.
   * 
   * @deprecated
   */
  final public void mockMdc() {
    mockMdc = new JSONObject();
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

    if (mockMdc != null) {
      if (o != null) {
        mockMdc.put(name, o);
      } else {
        mockMdc.remove(name);
      }
    }

    if (context == null) {
      context = new VelocityContext();
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
      this.set("total", bs.getTotal());
      if (n > 0) {
        int t = bs.getTotal() / n;
        if (bs.getTotal() % n > 0)
          t++;
        this.set("totalpage", t);
      }
      this.set("pages", Paging.create(bs.getTotal(), s, n));
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
  final public void set(Map<Object, Object> jo, String... names) {
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
   * Gets the int.
   * 
   * @deprecated
   * @param jo
   *          the jo
   * @param tag
   *          the tag
   * @return the int
   */
  final public int getInt(JSONObject jo, String tag) {
    if (jo.has(tag)) {
      return Bean.toInt(jo.get(tag));
    }

    return this.getInt(tag);
  }

  /**
   * Gets the int.
   * 
   * @deprecated
   * @param jo
   *          the jo
   * @param tag
   *          the tag
   * @param minValue
   *          the min value
   * @param tagInSession
   *          the tag in session
   * @return the int
   */
  final public int getInt(JSONObject jo, String tag, int minValue, String tagInSession) {
    if (jo.has(tag)) {
      int i = Bean.toInt(jo.get(tag));
      if (i >= minValue) {
        return i;
      }
    }

    return this.getInt(tag, minValue, tagInSession);
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
        r = Global.i(tagInSession, minValue);
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
    return Bean.toInt(v, defaultValue);
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
    return Bean.toLong(v, defaultvalue);
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

  transient JSONObject _json;

  /**
   * get the request as JSON
   * 
   * @return JSONObject
   */
  final public JSONObject getJSON() {
    if (_json == null) {
      _json = new JSONObject();
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
  final public JSONObject getJSONNonPassword() {
    if (_json == null) {
      getJSON();
    }

    JSONObject jo = JSONObject.fromObject(_json);
    // mix the password
    for (Object name : jo.keySet()) {
      if ("password".equals(name) || "pwd".equals(name) || "passwd".equals(name)) {
        jo.put(name, "*******");
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
      if (this._multipart) {
        getFiles();

        FileItem i = this.getFile(name);

        if (i != null && i.isFormField()) {
          InputStream in = i.getInputStream();
          byte[] bb = new byte[in.available()];
          in.read(bb);
          in.close();
          return new String(bb, "UTF8").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        }

      } else {
        String[] ss = req.getParameterValues(name);
        if (ss != null && ss.length > 0) {
          String s = ss[ss.length - 1];
          return s.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
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
      if (this._multipart) {
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

        String sid = sid();
        String token = getToken();
        if (!X.isEmpty(sid) && !X.isEmpty(token)) {
          AuthToken t = AuthToken.load(sid, token);
          if (t != null) {
            login = t.getUser();

            this.setUser(login);
          }
        }
      } else if (Bean.toInt(s.get("closed")) == 1) {
        s.remove("closed").store();
      }

      log.debug("getUser, user=" + login + " session=" + s);

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
  final private Map<String, Object> getFiles() {
    if (uploads == null) {
      uploads = new HashMap<String, Object>();
      DiskFileItemFactory factory = new DiskFileItemFactory();

      // Configure a repository (to ensure a secure temp location is used)
      ServletContext servletContext = GiiwaServlet.config.getServletContext();
      File repository = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
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
  final public String getContentType() {
    if (contentType == null) {
      return MIME_HTML;
    } else {
      return contentType;
    }
  }

  /**
   * Gets the template by viewname, the viewname is relative path
   * 
   * @param viewname
   *          the relative view path name
   * @param allowEmpty
   *          if not presented, allow using a empty
   * @return Template
   */
  final public Template getTemplate(String viewname, boolean allowEmpty) {
    Template template = cache.get(viewname);

    if (template == null || template.isSourceModified()) {
      /**
       * get the template from the top
       */
      template = Module.home.getTemplate(viewname, allowEmpty);

      cache.put(viewname, template);
    }

    return template;
  }

  /**
   * render and output the html page to end-user
   * 
   * @param viewname
   *          the path of the html view, <br>
   *          the path is the relative path refer the "/view/" path, <br>
   *          etc. the real view file is under: ../view/abc.html, <br>
   *          the "path" of the viewname is "/abc.html"<br>
   * @return boolean
   */
  final public boolean show(String viewname) {
    this.set("path", this.path);
    this.set("query", this.query);

    return show(viewname, false);
  }

  /**
   * output the json as "application/json" to end-user
   * 
   * @param jo
   *          the json that will be output
   */
  final public void response(JSONObject jo) {
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
    JSONObject jo = new JSONObject();
    jo.put(X.STATE, 200);
    jo.put(X.MESSAGE, message);
    this.response(jo);
  }

  /**
   * output the jsonarr as "application/json" to end-user
   * 
   * @param arr
   *          the array of json
   */
  final public void response(JSONArray arr) {
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
   * Render the template with current model and return
   * 
   * @param viewname
   *          the template name of view
   * @return string of rendered
   */
  final public String parse(String viewname) {
    StringBuilderWriter sb = null;
    try {

      Template template = getTemplate(viewname, true);

      // System.out.println(viewname + "=>" + template);
      if (template != null) {
        sb = new StringBuilderWriter();

        template.merge(context, sb);

        return sb.toString();
      }

    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(viewname, e);
    } finally {
      if (sb != null) {
        sb.close();
      }
    }

    return null;
  }

  /**
   * using current model to render the template, and show the result html page
   * to end-user.
   * 
   * @param viewname
   *          the viewname template
   * @param allowOverride
   *          if true and not presented, will trying load the template from
   *          parent module
   * @return boolean
   */
  final public boolean show(String viewname, boolean allowOverride) {

    Writer writer = null;
    try {

      TimeStamp t1 = TimeStamp.create();
      Template template = getTemplate(viewname, allowOverride);
      if (log.isDebugEnabled())
        log.debug("finding template = " + viewname + ", cost: " + t1.past() + "ms, result=" + template);

      // System.out.println(viewname + "=>" + template);
      if (template != null) {
        resp.setContentType(this.getContentType());

        writer = new BufferedWriter(resp.getWriter());

        TimeStamp t = TimeStamp.create();
        template.merge(context, writer);
        writer.flush();
        if (log.isDebugEnabled())
          log.debug("merge [" + viewname + "] cost: " + t.past() + "ms");

        return true;
      }

    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(viewname, e);
    } finally {
      if (writer != null) {
        try {
          writer.close();
        } catch (IOException e) {
          if (log.isErrorEnabled())
            log.error(e);
        }
      }
    }

    return false;
  }

  /**
   * render the template with parameters and return the result
   * 
   * @param viewname
   *          the relative path name of template
   * @param params
   *          the params array
   * @return String of the render result
   */
  final public String parse(String viewname, Object[]... params) {

    StringWriter writer = null;
    try {

      resp.setContentType(this.getContentType());

      TimeStamp t1 = TimeStamp.create();
      Template template = getTemplate(viewname, true);
      if (log.isDebugEnabled())
        log.debug("finding template = " + viewname + ", cost: " + t1.past() + "ms");

      // System.out.println(viewname + "=>" + template);
      if (template != null) {
        writer = new StringWriter();

        TimeStamp t = TimeStamp.create();

        VelocityContext context = new VelocityContext();
        if (params != null) {
          for (Object[] p : params) {
            if (p.length == 2) {
              context.put(p[0].toString(), p[1]);
            }
          }
        }
        template.merge(context, writer);
        writer.flush();
        if (log.isDebugEnabled())
          log.debug("merge [" + viewname + "] cost: " + t.past() + "ms");

        return writer.toString();
      }

    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(viewname, e);
    } finally {
      if (writer != null) {
        try {
          writer.close();
        } catch (IOException e) {
          if (log.isErrorEnabled())
            log.error(e);
        }
      }
    }

    return X.EMPTY;
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
        Model m = t.loadModel(Model.METHOD_POST, uri);
        if (m != null) {
          m.dispatch(uri, req, resp, method);
          return;
        }
      }
    }

    notfound();
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
   * render the template with current model, and return the result, please
   * refers to "render"
   * 
   * @deprecated
   * @param uri
   *          the uri
   * @return the string
   */
  final public String merge(String uri) {
    Template template = getTemplate(uri, true);

    // System.out.println(viewname + "=>" + template);
    StringWriter writer = null;
    try {
      if (template != null) {
        writer = new StringWriter();

        template.merge(context, writer);
        writer.flush();
        return writer.toString();
      }
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(e.getMessage(), e);
    } finally {
      try {
        if (writer != null) {
          writer.close();
        }
      } catch (IOException e) {
        if (log.isErrorEnabled())
          log.error(e);
      }
    }
    return null;
  }

  /**
   * Get the mime type.
   * 
   * @param uri
   *          the type of uri
   * @return String of mime type
   */
  public static String getMimeType(String uri) {
    return GiiwaServlet.config.getServletContext().getMimeType(uri);
  }

  /**
   * Show error page to end user.
   * 
   * @param e
   *          the throwable
   */
  final public void error(Throwable e) {
    if (log.isErrorEnabled())
      log.error(e.getMessage(), e);

    if (resp != null) {
      this.set("me", this.getUser());

      StringWriter sw = new StringWriter();
      PrintWriter out = new PrintWriter(sw);
      e.printStackTrace(out);
      String s = sw.toString();
      String lineSeparator = java.security.AccessController
          .doPrivileged(new sun.security.action.GetPropertyAction("line.separator"));
      s = s.replaceAll(lineSeparator, "<br/>");
      s = s.replaceAll(" ", "&nbsp;");
      s = s.replaceAll("\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
      this.set("error", s);

      this.show("/error.html", true);
    }
    setStatus(HttpServletResponse.SC_EXPECTATION_FAILED);

  }

  /**
   * show notfound page to end-user
   */
  final public void notfound() {
    if (log.isDebugEnabled())
      log.debug(this.getClass().getName() + "[" + this.getURI() + "]", new Exception("page notfound"));

    this.set("me", this.getUser());
    this.show("/notfound.html", true);
    this.setStatus(HttpServletResponse.SC_NOT_FOUND);
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
   * show deny page to end-user
   */
  final public void deny() {
    deny(null);
    setStatus(HttpServletResponse.SC_FORBIDDEN);
  }

  /**
   * show deny page with error info to end-user
   * 
   * @param error
   *          the error that will be displaied
   */
  final public void deny(String error) {
    if (log.isDebugEnabled())
      log.debug(this.getClass().getName() + "[" + this.getURI() + "]", new Exception("deny " + error));

    String request = this.getHeader("X-Requested-With");
    if ("XMLHttpRequest".equals(request)) {
      JSONObject jo = new JSONObject();
      jo.put(X.STATE, 202);
      jo.put(X.MESSAGE, "你没有权限访问！");
      jo.put(X.ERROR, error);
      // this.redirect("/user/login/popup");
      this.response(jo);
    } else {
      this.set("me", this.getUser());
      this.set(X.ERROR, error);
      this.show("/deny.html", true);
    }

  }

  /**
   * Delete the file and files under the path
   * 
   * @deprecated
   * @param f
   *          the file which deleted
   */
  final public void delete(File f) {
    if (!f.exists()) {
      return;
    }
    if (f.isFile()) {
      f.delete();
    }

    if (f.isDirectory()) {
      File[] list = f.listFiles();
      if (list != null && list.length > 0) {
        for (File f1 : list) {
          delete(f1);
        }
      }
      f.delete();
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
   * the utility api of copying all data in "inputstream" to "outputstream".
   *
   * @param in
   *          the inputstream
   * @param out
   *          the outputstream
   * @return int the size of copied
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   * @deprecated
   */
  public static int copy(InputStream in, OutputStream out) throws IOException {
    return copy(in, out, true);
  }

  /**
   * copy the data in "inputstream" to "outputstream", from start to end.
   *
   * @param in
   *          the inputstream
   * @param out
   *          the outputstream
   * @param start
   *          the start position of started
   * @param end
   *          the end position of ended
   * @param closeAfterDone
   *          close after done, true: close if done, false: not close
   * @return int the size of copied
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   * @deprecated
   */
  public static int copy(InputStream in, OutputStream out, long start, long end, boolean closeAfterDone)
      throws IOException {
    try {
      if (in == null || out == null)
        return 0;

      byte[] bb = new byte[1024 * 4];
      int total = 0;
      in.skip(start);
      int ii = (int) Math.min((end - start), bb.length);
      int len = in.read(bb, 0, ii);
      while (len > 0) {
        out.write(bb, 0, len);
        total += len;
        ii = (int) Math.min((end - start - total), bb.length);
        len = in.read(bb, 0, ii);
        out.flush();
      }
      return total;
    } finally {
      if (closeAfterDone) {
        if (in != null) {
          in.close();
        }
        if (out != null) {
          out.close();
        }
      }
    }
  }

  /**
   * Copy data in "inputstream" to "outputstream".
   *
   * @param in
   *          the inputstream
   * @param out
   *          the outputstream
   * @param closeAfterDone
   *          close after done, true: close if done, false: not close
   * @return int the size of copied
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   * @deprecated
   */
  public static int copy(InputStream in, OutputStream out, boolean closeAfterDone) throws IOException {
    try {
      if (in == null || out == null)
        return 0;

      byte[] bb = new byte[1024 * 4];
      int total = 0;
      int len = in.read(bb);
      while (len > 0) {
        out.write(bb, 0, len);
        total += len;
        len = in.read(bb);
        out.flush();
      }
      return total;
    } finally {
      if (closeAfterDone) {
        if (in != null) {
          in.close();
        }
        if (out != null) {
          out.close();
        }
      }
    }
  }

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
   * Copy the request params to V
   * 
   * @param v
   *          the destination of v
   * @param names
   *          the names that request parameters will copied
   * @return int of copied
   */
  final public int copy(V v, String... names) {
    if (v == null || names == null || names.length == 0)
      return 0;

    int count = 0;
    for (String name : names) {
      String s = this.getString(name);
      v.set(name, s);
      count++;
    }
    return count;
  }

  /**
   * Copy the request "int" params to V.
   * 
   * @param v
   *          the destination of v
   * @param names
   *          the names that request parameters will copied
   * @return int of copied
   */
  final public int copyInt(V v, String... names) {
    if (v == null || names == null || names.length == 0)
      return 0;

    int count = 0;
    for (String name : names) {
      int s = Bean.toInt(this.getString(name));
      v.set(name, s);
      count++;
    }
    return count;
  }

  /**
   * Copy the request long params to V.
   * 
   * @param v
   *          the destination of v
   * @param names
   *          the names that request parameters will copied
   * @return int of copied
   */
  final public int copyLong(V v, String... names) {
    if (v == null || names == null || names.length == 0)
      return 0;

    int count = 0;
    for (String name : names) {
      long s = Bean.toLong(this.getString(name));
      v.set(name, s);
      count++;
    }
    return count;
  }

  /**
   * Copy the request date params to V.
   * 
   * @param v
   *          the destination of v
   * @param format
   *          the format of datetime
   * @param names
   *          the names that request parameters will copied
   * @return int of copied
   */
  final public int copyDate(V v, String format, String... names) {
    if (v == null || names == null || names.length == 0)
      return 0;

    int count = 0;
    for (String name : names) {
      long s = lang.parse(this.getString(name), format);
      if (s > 0) {
        v.set(name, s);
        count++;
      }
    }
    return count;
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

  // /**
  // * Random the list
  // *
  // * @deprecated
  // * @param <T>
  // * the generic type
  // * @param list
  // * the source list
  // * @return list of random
  // */
  // public static <T> List<T> random(List<T> list) {
  // if (list == null || list.size() == 0)
  // return list;
  //
  // int len = list.size();
  // for (int i = 0; i < len; i++) {
  // int j = (int) Math.random() * len;
  // if (j == i)
  // continue;
  //
  // T o = list.get(i);
  // list.set(i, list.get(j));
  // list.set(j, o);
  // }
  //
  // return list;
  // }

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
        list.add(NameValue.create(n, Bean.toString(req.getParameterValues(n))));
      }

      return list.toArray(new NameValue[list.size()]);
    }
    return null;
  }

  /**
   * get the name of this node in cluster.
   *
   * @return string of name
   */
  public static String node() {
    return Module._conf.getString("node", null);
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
