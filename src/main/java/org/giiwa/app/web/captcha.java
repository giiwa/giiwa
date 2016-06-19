package org.giiwa.app.web;

import org.giiwa.core.bean.X;
import org.giiwa.framework.bean.Session;
import org.giiwa.framework.bean.Temp;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;
import org.giiwa.utils.image.Captcha;

import net.sf.json.JSONObject;

public class captcha extends Model {

  @Path()
  public void onGet() {

    JSONObject jo = new JSONObject();
    Temp t = Temp.create("code.jpg");
    try {

      String code = Captcha.create(200, 60, t.getFile(), 4);
      this.getSession().set("captcha", code).set("captcha.expired", System.currentTimeMillis() + 5 * X.AMINUTE).store();
      jo.put(X.STATE, 200);
      jo.put("uri", t.getUri());
    } catch (Exception e1) {
      log.error(e1.getMessage(), e1);
      jo.put(X.STATE, 201);
      jo.put(X.MESSAGE, e1.getMessage());
    }

    this.response(jo);
  }

  @Path(path = "verify")
  public void verify() {
    String code = this.getString("code").toLowerCase();
    Session s = this.getSession();

    JSONObject jo = new JSONObject();
    if (X.isSame(code, s.get("captcha"))) {
      if (X.toLong(s.get("captcha.expired"), 0) > System.currentTimeMillis()) {
        jo.put(X.STATE, 200);
        jo.put(X.MESSAGE, "ok");
      } else {
        jo.put(X.STATE, 201);
        jo.put(X.MESSAGE, "expired");
      }
    } else {
      jo.put(X.STATE, 202);
      jo.put(X.MESSAGE, "bad code");
    }

    this.response(jo);

  }

}
