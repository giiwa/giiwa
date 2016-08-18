package org.giiwa.app.web.admin;

import org.giiwa.framework.bean.OpLog;
import org.giiwa.framework.bean.Temp;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;
import org.giiwa.utils.image.GImage;

/**
 * web api: /admin/qcode <br>
 * used to generate a qcode image by url
 * 
 * @author wujun
 *
 */
public class qcode extends Model {

  @Path()
  public void onGet() {
    String url = this.getString("url");
    Temp t = Temp.create("qcode.jpg");

    try {
      GImage.QRCode(t.getFile(), url, 120, 120);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      OpLog.error(qcode.class, "", e.getMessage(), e, login, this.getRemoteHost());

    }
    this.redirect(t.getUri());
  }
}
