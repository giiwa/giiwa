package org.giiwa.framework.web.view;

import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterConfig;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.framework.web.Model;

public abstract class View {

  static Log log = LogFactory.getLog(View.class);

  protected abstract boolean parse(File file, Model m) throws Exception;

  public static void init(FilterConfig config) {

    Enumeration e = config.getInitParameterNames();
    while (e.hasMoreElements()) {
      String name = e.nextElement().toString();
      String value = config.getInitParameter(name);
      try {
        View v = (View) Class.forName(value).newInstance();
        views.put(name, v);
      } catch (Exception e1) {
        log.error(value, e1);
      }
    }

    log.debug("config=" + views);

  }

  public static void merge(File file, Model m) throws Exception {

    String name = file.getName();
    for (String suffix : views.keySet()) {
      if (name.endsWith(suffix)) {
        View v = views.get(suffix);
        v.parse(file, m);
        return;
      }
    }

    fileview.parse(file, m);
  }

  private static Map<String, View> views        = new HashMap<String, View>();
  private static View              fileview = new FileView();
}
