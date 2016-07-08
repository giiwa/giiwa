package org.giiwa.framework.web.view;

import java.io.File;

import javax.servlet.RequestDispatcher;

import org.giiwa.framework.web.Model;

public class JspView extends View {

  @Override
  protected boolean parse(File file, Model m) throws Exception {
    String name = file.getCanonicalPath().substring(Model.HOME.length());
    log.debug("viewname=" + name);

    name = name.replaceAll("\\\\", "/");
    if (m.context != null) {
      for (String s : m.context.keySet()) {
        m.req.setAttribute(s, m.context.get(s));
      }
    }
    m.req.setAttribute(System.getProperty("org.apache.jasper.Constants.JSP_FILE", "org.apache.catalina.jsp_file"),
        name);

    RequestDispatcher rd = m.req.getRequestDispatcher(name);
    if (rd == null) {
      log.warn("Not a valid resource path:" + name);
      return false;
    } else {
      log.debug("including jsp page, name=" + name);
    }

    rd.include(m.req, m.resp);

    return true;
  }

}
