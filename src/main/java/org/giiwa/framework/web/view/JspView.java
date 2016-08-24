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
