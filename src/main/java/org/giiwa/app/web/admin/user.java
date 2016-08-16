/*
 *   giiwa, a java web foramewrok.
 *   Copyright (C) <2014>  <giiwa.org>
 *
 */
package org.giiwa.app.web.admin;

import java.util.ArrayList;
import java.util.List;

import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.conf.Global;
import org.giiwa.core.bean.X;
import org.giiwa.core.json.JSON;
import org.giiwa.framework.bean.*;
import org.giiwa.framework.web.*;

/**
 * web api: /admin/user <br>
 * used to manage user
 * 
 * @author joe
 *
 */
public class user extends Model {

  /**
   * Adds the.
   */
  @Path(path = "create", login = true, access = "access.user.admin")
  public void create() {
    if (method.isPost()) {

      JSON jo = this.getJSON();
      String name = this.getString("name").trim().toLowerCase();
      String rule = Global.getString("user.name.rule", "^[a-zA-Z0-9]{4,16}$");
      if (!X.isEmpty(rule) && !name.matches(rule)) {
        this.set(jo);
        this.set(X.MESSAGE, lang.get("user.name.format.error"));
      } else {
        try {

          /**
           * create the user
           */
          if (User.exists(W.create("name", name))) {
            /**
             * exists, create failded
             */
            this.set(X.ERROR, lang.get("user.name.exists"));
          } else {

            V v = V.create("name", name).copy(jo).set("locked", 0);
            v.remove("role");
            long id = User.create(v);

            /**
             * set the role
             */
            String[] roles = this.getStrings("role");
            log.debug("roles=" + Bean.toString(roles));

            if (roles != null) {
              User u = User.loadById(id);
              List<Long> list = new ArrayList<Long>();
              for (String s : roles) {
                list.add(X.toLong(s));
              }
              u.setRoles(list);
            }

            /**
             * log
             */
            OpLog.info(user.class, "user.create", this.getJSONNonPassword().toString(), null, login.getId(),
                this.getRemoteHost());

            this.set(X.MESSAGE, lang.get("save.success"));

            onGet();
            return;
          }
        } catch (Exception e) {
          log.error(e.getMessage(), e);
          OpLog.error(user.class, "create", e.getMessage(), e);

          this.set(X.ERROR, lang.get("save.failed"));

          this.set(jo);
        }
      }

    }

    Beans<Role> bs = Role.load(0, 1000);
    if (bs != null) {
      this.set("roles", bs.getList());
    }

    this.show("/admin/user.create.html");
  }

  /**
   * Delete.
   */
  @Path(path = "delete", login = true, access = "access.user.admin")
  public void delete() {

    JSON jo = new JSON();

    long id = this.getLong("id");
    if (id > 0) {
      User.delete(id);
      OpLog.warn(user.class, "user.delete", this.getJSONNonPassword().toString(), null, login.getId(),
          this.getRemoteHost());
      jo.put(X.STATE, 200);
    } else {
      jo.put(X.MESSAGE, "删除错误，请稍后重试！");
    }

    this.response(jo);

  }

  /**
   * s Edits the.
   */
  @Path(path = "edit", login = true, access = "access.user.admin")
  public void edit() {
    long id = this.getLong("id");

    if (method.isPost()) {

      String password = this.getString("password");
      if (!X.isEmpty(password)) {
        JSON jo = new JSON();
        User.update(id, V.create("password", password));
        jo.put(X.STATE, 200);
        this.response(jo);
        return;
      }
      JSON j = this.getJSON();
      V v = V.create().copy(j);
      v.remove("role");

      v.set("failtimes", this.getInt("failtimes"), true);
      if (!"on".equals(this.getString("locked"))) {
        /**
         * clean all the locked info
         */
        User.Lock.removed(id);
        v.set("locked", 0, true);
      } else {
        v.set("locked", 1, true);
      }

      User.update(id, v);
      User u = User.loadById(id);

      String[] roles = this.getStrings("role");
      if (roles != null) {
        List<Long> list = new ArrayList<Long>();
        for (String s : roles) {
          list.add(X.toLong(s));
        }

        u.setRoles(list);
        v.set("roles", list);
      }

      List<String> list = AuthToken.remove(id);
      if (list != null && list.size() > 0) {
        for (String s : list) {
          Session.delete(s);
        }
      }

      OpLog.info(user.class, "user.edit", this.getJSONNonPassword().toString(), null, login.getId(),
          this.getRemoteHost());

      this.set(X.MESSAGE, lang.get("save.success"));

      onGet();

    } else {

      User u = User.loadById(id);
      if (u != null) {
        this.set(u.getJSON());
        this.set("u", u);

        Beans<Role> bs = Role.load(0, 1000);
        if (bs != null) {
          this.set("roles", bs.getList());
        }

        this.set("id", id);
        this.show("/admin/user.edit.html");
        return;
      }

      this.set(X.ERROR, lang.get("select.required"));
      onGet();

    }
  }

  /**
   * Detail.
   */
  @Path(path = "detail", login = true, access = "access.user.query")
  public void detail() {
    String id = this.getString("id");
    if (id != null) {
      long i = X.toLong(id, -1);
      User u = User.loadById(i);
      this.set("u", u);

      Beans<Role> bs = Role.load(0, 100);
      if (bs != null) {
        this.set("roles", bs.getList());
      }

      this.show("/admin/user.detail.html");
    } else {
      onGet();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.giiwa.framework.web.Model#onGet()
   */
  @Override
  @Path(login = true, access = "access.user.admin")
  public void onGet() {

    String name = this.getString("name");
    W q = W.create();
    if (X.isEmpty(this.path) && !X.isEmpty(name)) {
      W list = W.create();

      list.or("name", name, W.OP_LIKE);
      list.or("nickname", name, W.OP_LIKE);
      q.and(list);

      this.set("name", name);
    }

    int s = this.getInt("s");
    int n = this.getInt("n", 10, "number.per.page");

    Beans<User> bs = User.load(q, s, n);
    this.set(bs, s, n);

    this.query.path("/admin/user");

    this.show("/admin/user.index.html");
  }

}
