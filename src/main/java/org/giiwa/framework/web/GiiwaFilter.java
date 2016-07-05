package org.giiwa.framework.web;

import java.io.IOException;

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
import org.giiwa.framework.web.view.View;

public class GiiwaFilter implements Filter {

  static Log log = LogFactory.getLog(GiiwaFilter.class);

  @Override
  public void destroy() {

  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest r1 = (HttpServletRequest) req;
    String uri = r1.getRequestURI();

    String method = r1.getMethod();

    if ("GET".equalsIgnoreCase(method)) {

      Controller.dispatch(uri, r1, (HttpServletResponse) resp, new HTTPMethod(Model.METHOD_GET));

    } else if ("POST".equalsIgnoreCase(method)) {

      Controller.dispatch(uri, r1, (HttpServletResponse) resp, new HTTPMethod(Model.METHOD_POST));

    }

    chain.doFilter(req, resp);

  }

  @Override
  public void init(FilterConfig config) throws ServletException {

    Model.s️ervletContext = config.getServletContext();

    View.init(config);
  }

}
