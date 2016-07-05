package org.giiwa.framework.web.view;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.giiwa.framework.web.Model;

import freemarker.template.Template;

public class FreemakerView extends View {

  @Override
  public boolean parse(File file, Model m) {
    // TODO Auto-generated method stub
    // load
    try {

      Template template = getTemplate(file);
      if (template != null) {

        Writer out = new OutputStreamWriter(m.getOutputStream());
        template.process(m.context, out);
        out.flush();

        return true;
      }
    } catch (Exception e) {
      log.error(file.getName(), e);
    }
    return false;
  }

  Template getTemplate(File f) throws IOException {

    String fullname = f.getCanonicalPath();
    T t = cache.get(fullname);
    if (t == null || t.last != f.lastModified()) {
      if (cache.size() == 0) {
        cfg.setDirectoryForTemplateLoading(new File(Model.HOME));
      }

      Template t1 = cfg.getTemplate(f.getCanonicalPath().substring(Model.HOME.length()), "UTF-8");
      t = T.create(t1, f.lastModified());
      cache.put(fullname, t);
    }
    return t == null ? null : t.template;
  }

  private static class T {
    Template template;
    long     last;

    static T create(Template t, long last) {
      T t1 = new T();
      t1.template = t;
      t1.last = last;
      return t1;
    }
  }

  private static Map<String, T>                    cache = new HashMap<String, T>();
  private static freemarker.template.Configuration cfg   = new freemarker.template.Configuration(
      freemarker.template.Configuration.VERSION_2_3_24);

}
