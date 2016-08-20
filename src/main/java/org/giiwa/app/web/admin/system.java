/*
 *   giiwa, a java web foramewrok.
 *   Copyright (C) <2014>  <giiwa.org>
 *
 */
package org.giiwa.app.web.admin;

import org.giiwa.core.json.JSON;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.User;
import org.giiwa.framework.web.*;

/**
 * web api: /admin/system <br>
 * used to control the "system"<br>
 * required "access.config.admin"
 * 
 * @author joe
 *
 */
public class system extends Model {

  /**
   * Restart.
   */
  @Path(path = "restart", login = true, access = "access.config.admin", log = Model.METHOD_POST)
  public void restart() {

    JSON jo = new JSON();
    User me = User.loadById(login.getId());
    String pwd = this.getString("pwd");

    if (me.validate(pwd)) {
      jo.put("state", "ok");

      new Task() {

        @Override
        public String getName() {
          return "restart";
        }

        @Override
        public void onExecute() {
          System.exit(0);
        }

        @Override
        public void onFinish() {

        }

      }.schedule(1000);
    } else {
      jo.put("state", "fail");
      jo.put("message", lang.get("invalid.password"));
    }

    this.response(jo);
  }

}
