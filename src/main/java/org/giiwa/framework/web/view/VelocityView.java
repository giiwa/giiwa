package org.giiwa.framework.web.view;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.giiwa.framework.web.Model;

public class VelocityView extends View {

  @Override
  public boolean parse(File file, Model m) throws IOException {
    // TODO Auto-generated method stub
    Template template = getTemplate(file);

    // System.out.println(viewname + "=>" + template);
    if (template != null) {
      m.resp.setContentType(m.getContentType());

      BufferedWriter writer = new BufferedWriter(m.resp.getWriter());

      template.merge(new VelocityContext(m.context), writer);
      writer.flush();

      return true;
    }
    return false;
  }

  /**
   * Gets the template by viewname, the viewname is relative path
   * 
   * @param viewname
   *          the relative view path name
   * @param allowEmpty
   *          if not presented, allow using a empty
   * @return Template
   * @throws IOException
   */
  private Template getTemplate(File f) throws IOException {

    String fullname = f.getCanonicalPath();
    T t = cache.get(fullname);

    if (t == null || t.last != f.lastModified()) {

      if (cache.size() == 0) {
        /**
         * initialize template loader for velocity
         */
        Properties p = new Properties();
        p.setProperty("input.encoding", "utf-8");
        p.setProperty("output.encoding", "utf-8");
        p.setProperty("log4j.logger.org.apache.velocity", "ERROR");
        p.setProperty("directive.set.null.allowed", "true");
        p.setProperty("file.resource.loader.class", "org.giiwa.framework.web.view.VelocityTemplateLoader");
        Velocity.init(p);
      }

      /**
       * get the template from the top
       */
      Template t1 = Velocity.getTemplate(fullname, "UTF-8");
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

  /**
   * cache the template by viewname, the template will be override in child
   * module
   */
  private static Map<String, T> cache = new HashMap<String, T>();

}
