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

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

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
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.framework.web.Model.HTTPMethod;
import org.giiwa.framework.web.view.View;

public class GiiwaFilter implements Filter {

  static Log log          = LogFactory.getLog(GiiwaFilter.class);

  String     cross_domain = null;
  String     cross_header = null;

  @Override
  public void destroy() {

  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
      throws IOException, ServletException {

    try {
      HttpServletRequest r1 = (HttpServletRequest) req;
      HttpServletResponse r2 = (HttpServletResponse) resp;

      String uri = r1.getRequestURI();
      while (uri.indexOf("//") > -1) {
        uri = uri.replaceAll("//", "/");
      }

      String method = r1.getMethod();

      if ("GET".equalsIgnoreCase(method)) {
        if (!X.isEmpty(cross_domain)) {
          r2.addHeader("Access-Control-Allow-Origin", cross_domain);
        }

        Controller.dispatch(uri, r1, r2, new HTTPMethod(Model.METHOD_GET));

      } else if ("POST".equalsIgnoreCase(method)) {
        if (!X.isEmpty(cross_domain)) {
          r2.addHeader("Access-Control-Allow-Origin", cross_domain);
        }

        Controller.dispatch(uri, r1, r2, new HTTPMethod(Model.METHOD_POST));

      } else if ("OPTIONS".equals(method)) {
        r2.setStatus(200);
        r2.addHeader("Access-Control-Allow-Origin", cross_domain);
        r2.addHeader("Access-Control-Allow-Headers", cross_header);
        r2.getOutputStream().write(cross_domain.getBytes());
      } else if ("HEAD".equals(method)) {

      }

      // chain.doFilter(req, resp);
    } catch (Throwable e) {
      log.error(e.getMessage(), e);
    }
  }

  @SuppressWarnings("rawtypes")
  @Override
  public void init(FilterConfig c1) throws ServletException {
    try {
      Model.s️ervletContext = c1.getServletContext();

      cross_domain = Global.getString("cross.domain", "no");
      cross_header = Global.getString("cross.header", "forbidden");

      Enumeration e = c1.getInitParameterNames();
      while (e.hasMoreElements()) {
        String name = e.nextElement().toString();
        String value = c1.getInitParameter(name);
        config.put(name, value);
      }

      View.init(config);
    } catch (Throwable e) {
      log.error(e.getMessage(), e);
    }
  }

  private Map<String, String> config = new HashMap<String, String>();

}
