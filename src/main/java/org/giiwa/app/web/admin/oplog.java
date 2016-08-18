/*
 *   giiwa, a java web foramewrok.
 *   Copyright (C) <2014>  <giiwa.org>
 *
 */
package org.giiwa.app.web.admin;

import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.X;
import org.giiwa.core.json.JSON;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.framework.bean.OpLog;
import org.giiwa.framework.bean.User;
import org.giiwa.framework.web.*;

/**
 * web api: /admin/log <br>
 * used to manage oplog
 * 
 * @author joe
 *
 */
public class oplog extends Model {

  /**
   * Deleteall.
   */
  @Path(path = "deleteall", login = true, access = "access.logs.admin")
  public void deleteall() {
    JSON jo = new JSON();
    int i = OpLog.cleanup();
    OpLog.warn(oplog.class, "deleteall", "deleted=" + i, login, this.getRemoteHost());
    jo.put(X.STATE, 200);
    this.response(jo);
  }

  private W getW(JSON jo) {

    W q = W.create();

    if (!X.isEmpty(jo.get("op"))) {
      q.and("op", jo.get("op"));
    }
    if (!X.isEmpty(jo.get("ip"))) {
      q.and("ip", jo.getString("ip"), W.OP_LIKE);
    }
    if (!X.isEmpty(jo.get("user"))) {

      String name = this.getString("user");
      W q1 = W.create().and(W.create().and("nickname", name, W.OP_LIKE).or("name", name, W.OP_LIKE)).sort("name", 1);
      Beans<User> bs = User.load(q1, 0, 100);
      if (bs != null && bs.getList() != null && bs.getList().size() > 0) {
        W q2 = W.create();
        for (User u : bs.getList()) {
          q2.or("uid", u.getId());
        }
        q.and(q2);
      } else {
        // user not found
        q.and("uid", -2);
      }
    }
    if (!X.isEmpty(jo.get("type"))) {
      q.and("type", X.toInt(jo.get("type")));
    }

    if (!X.isEmpty(jo.getString("model"))) {
      q.and("model", jo.getString("model"));
    }

    if (!X.isEmpty(jo.getString("node"))) {
      q.and("node", jo.getString("node"));
    }

    if (!X.isEmpty(jo.getString("starttime"))) {
      q.and("created", lang.parse(jo.getString("starttime"), "yyyy-MM-dd"), W.OP_GTE);

    } else {
      long today_2 = System.currentTimeMillis() - X.ADAY * 2;
      jo.put("starttime", lang.format(today_2, "yyyy-MM-dd"));
      q.and("created", today_2, W.OP_GTE);
    }

    if (!X.isEmpty(jo.getString("endtime"))) {
      q.and("created", lang.parse(jo.getString("endtime"), "yyyy-MM-dd"), W.OP_LTE);
    }

    String sortby = this.getString("sortby");
    if (!X.isEmpty(sortby)) {
      int sortby_type = this.getInt("sortby_type");
      q.sort(sortby, sortby_type);
    } else {
      q.sort("created", -1);
    }
    this.set(jo);

    return q;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.giiwa.framework.web.Model#onGet()
   */
  @Path(login = true, access = "access.logs.admin")
  public void onGet() {

    int s = this.getInt("s");
    int n = this.getInt("n", 20, "number.per.page");

    this.set("currentpage", s);

    JSON jo = this.getJSON();
    W w = getW(jo);

    Beans<OpLog> bs = OpLog.load(w, s, n);
    this.set(bs, s, n);

    this.query.path("/admin/oplog");
    this.show("/admin/oplog.index.html");
  }

}
