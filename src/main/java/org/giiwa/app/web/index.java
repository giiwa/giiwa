package org.giiwa.app.web;

import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;

public class index extends Model {

  @Path()
  public void onGet() {
    String h1 = Global.getString("home.uri", X.EMPTY);
    if (!X.isEmpty(h1)) {
      this.redirect(h1);
    } else {
      this.show("/index.html");
    }
  }

}
