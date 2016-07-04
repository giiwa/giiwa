package org.giiwa.framework.web;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.framework.web.Model.HTTPMethod;

public class GiiwaFilter implements Filter {

  static Log          log         = LogFactory.getLog(GiiwaFilter.class);

  static final String transparent = "/modules/.*.jsp";

  @Override
  public void destroy() {

  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest r1 = (HttpServletRequest) req;
    String uri = r1.getRequestURI();

    if (uri.matches(transparent)) {
      log.debug("uri=" + uri + ", transparent=" + transparent);

      chain.doFilter(req, resp);

    } else {

      String method = r1.getMethod();
      // log.debug("method=" + method);

      if ("GET".equalsIgnoreCase(method)) {

        Controller.dispatch(uri, r1, (HttpServletResponse) resp, new HTTPMethod(Model.METHOD_GET));

      } else if ("POST".equalsIgnoreCase(method)) {

        Controller.dispatch(uri, r1, (HttpServletResponse) resp, new HTTPMethod(Model.METHOD_POST));

      }

    }

  }

  @Override
  public void init(FilterConfig config) throws ServletException {

    Model.s️ervletContext = config.getServletContext();

    Enumeration e = config.getInitParameterNames();
    while (e.hasMoreElements()) {
      String name = e.nextElement().toString();
      String value = config.getInitParameter(name);
      m.put(name, value);
    }

    log.debug("config=" + m);
  }

  /**
   * test is jsp page
   * 
   * @param url
   * @return boolean
   */
  public static boolean isJsp(String url) {
    String v = m.get("jsp");
    return v != null && url.matches(v);
  }

  /**
   * test is velocity template
   * 
   * @param url
   * @return boolean
   */
  public static boolean isVelocity(String url) {
    String v = m.get("velocity");
    return v != null && url.matches(v);
  }

  /**
   * test is freemaker template
   * 
   * @param url
   * @return boolean
   */
  public static boolean isFreemaker(String url) {
    String v = m.get("freemaker");
    return v != null && url.matches(v);
  }

  private static Map<String, String> m = new HashMap<String, String>();

}
