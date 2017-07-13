package org.giiwa.app.web.admin;

import java.util.Map;

import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;

/**
 * some setting of the module
 * 
 * @author joe
 *
 */
public class i18n extends Model {

  @Path(login = true, access = "access.config.admin")
  public void onGet() {
    Map<String, String> missed = lang.getMissed();
    StringBuilder sb = new StringBuilder();
    for (String n : missed.keySet()) {
      sb.append(n).append("=").append("<br>");
    }
    this.set("missed", sb.toString());

    Map<String, String[]> d = lang.getData();
    this.set("d", d);
    this.show("/admin/i18n.index.html");

  }

}
