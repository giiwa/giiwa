package org.giiwa.app.web;

import org.giiwa.core.bean.X;
import org.giiwa.framework.bean.OpLog;
import org.giiwa.framework.bean.Temp;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;
import org.giiwa.utils.image.Captcha;

import net.sf.json.JSONObject;

/**
 * web api: /captcha<br>
 * provides web api to get the captcha image and verify
 * 
 * @author wujun
 *
 */
public class captcha extends Model {

  /**
   * response the json with uri=[code.jpg]
   */
  @Path()
  public void onGet() {

    JSONObject jo = new JSONObject();
    Temp t = Temp.create("code.jpg");
    try {

      Captcha.create(this.sid(), System.currentTimeMillis() + 5 * X.AMINUTE, 200, 60, t.getFile(), 4);

      jo.put(X.STATE, 200);
      jo.put("sid", sid());
      jo.put("uri", t.getUri() + "?" + System.currentTimeMillis());
    } catch (Exception e1) {
      log.error(e1.getMessage(), e1);
      OpLog.error(captcha.class, "/", e1.getMessage(), e1);

      jo.put(X.STATE, 201);
      jo.put(X.MESSAGE, e1.getMessage());
    }

    this.response(jo);
  }

  /**
   * verify the code
   */
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
