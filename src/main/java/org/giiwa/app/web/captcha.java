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

      Captcha.create(this.sid(), System.currentTimeMillis() + 5 * X.AMINUTE, 200, 60, t.getFile(), 4);

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
    Captcha.Result r = Captcha.verify(this.sid(), code);

    JSONObject jo = new JSONObject();
    if (Captcha.Result.badcode == r) {
      jo.put(X.STATE, 202);
      jo.put(X.MESSAGE, "bad code");
    } else if (Captcha.Result.expired == r) {
      jo.put(X.STATE, 201);
      jo.put(X.MESSAGE, "expired");
    } else {
      jo.put(X.STATE, 200);
      jo.put(X.MESSAGE, "ok");
    }

    this.response(jo);

  }

}
