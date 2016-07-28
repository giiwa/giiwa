package org.giiwa.framework.web.view;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.framework.web.Model;

public abstract class View {

  static Log log = LogFactory.getLog(View.class);

  protected abstract boolean parse(File file, Model m) throws Exception;

  public static void init(Map<String, String> config) {

    for (String name : config.keySet()) {
      if (name.startsWith(".")) {
        String value = config.get(name);
        try {
          View v = (View) Class.forName(value).newInstance();
          views.put(name, v);
        } catch (Exception e1) {
          log.error(value, e1);
        }
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

  private static Map<String, View> views    = new HashMap<String, View>();
  private static View              fileview = new FileView();
}
