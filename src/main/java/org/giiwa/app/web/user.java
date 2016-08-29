/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.app.web;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.giiwa.core.base.Captcha;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.core.json.JSON;
import org.giiwa.core.noti.Email;
import org.giiwa.core.noti.Sms;
import org.giiwa.framework.bean.AuthToken;
import org.giiwa.framework.bean.Code;
import org.giiwa.framework.bean.OpLog;
import org.giiwa.framework.bean.Role;
import org.giiwa.framework.bean.Session;
import org.giiwa.framework.bean.User;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;
import org.giiwa.framework.web.view.VelocityView;

/**
 * web api： /user <br>
 * used to login or logout, etc.
 * 
 * @author joe
 * 
 */
public class user extends Model {

  /**
   * Index.
   */
  @Path()
  public void onGet() {
    if (login == null) {
      this.redirect("/user/login");
    } else if (login.hasAccess("access.config.admin")) {
      this.redirect("/admin");
    } else {
      this.redirect("/");
    }
  }

  /**
   * Register.
   */
  @Path(path = "register")
  public void register() {
    if ("close".equals(Global.getString("user.system", "close"))) {
      this.redirect("/user/login");
      return;
    }

    if (method.isPost()) {

      Captcha.Result r1 = Captcha.Result.ok;

      if (Global.getInt("user.captcha", 1) == 1) {
        String code = this.getString("code");
        if (code != null) {
          code = code.toLowerCase();
        }
        r1 = Captcha.verify(this.sid(), code);
        Captcha.remove(this.sid());
      }

      JSON jo = this.getJSON();

      if (Captcha.Result.badcode == r1) {
        jo.put(X.MESSAGE, lang.get("captcha.bad"));
        jo.put(X.STATE, 202);
      } else if (Captcha.Result.expired == r1) {
        jo.put(X.MESSAGE, lang.get("captcha.expired"));
        jo.put(X.STATE, 203);
      } else {

        String name = this.getString("name").trim().toLowerCase();
        String passwd = this.getString("password").trim().toLowerCase();

        String namerule = Global.getString("user.name.rule", "^[a-zA-Z0-9]{4,16}$");
        String passwdrule = Global.getString("user.passwd.rule", "^[a-zA-Z0-9]{6,16}$");
        if (!X.isEmpty(namerule) && !name.matches(namerule)) {
          jo.put(X.MESSAGE, lang.get("user.name.format.error"));
        } else if (!X.isEmpty(passwdrule) && !passwd.matches(passwdrule)) {
          jo.put(X.MESSAGE, lang.get("user.passwd.format.error"));
        } else {
          try {
            V v = V.create("name", name).copy(jo);
            long id = User.create(v);

            String role = Global.getString("user.role", "N/A");
            Role r = Role.loadByName(role);
            User u = User.loadById(id);
            if (r != null) {
              u.setRole(r.getId());
            }
            this.setUser(u);
            OpLog.info(user.class, "register", lang.get("create.success") + ":" + name + ", uid=" + id, login,
                this.getRemoteHost());

            Session s = this.getSession();
            if (s.has("uri")) {
              this.redirect((String) s.get("uri"));
              return;
            } else {
              this.set(X.MESSAGE, lang.get("user.register.success"));
              this.set("success", 1);
            }

          } catch (Exception e) {
            log.error(e.getMessage(), e);
            OpLog.error(user.class, "register", e.getMessage(), e, login, this.getRemoteHost());

            this.put(X.MESSAGE, lang.get("create_user_error_1"));
            OpLog.info(user.class, "register", lang.get("create.failed") + ":" + name, login, this.getRemoteHost());
          }
        }
      }
      this.set(jo);
    }

    show("/user/user.register.html");

  }

  /**
   * redirect the web to right site
   * 
   * @return true, if successful
   */
  @Path(path = "go", login = true)
  public boolean go() {

    Session s = this.getSession();
    if (s.has("uri")) {
      String uri = (String) s.get("uri");

      log.debug("redirecting:" + uri);

      if (uri.endsWith("/index")) {
        uri = uri.substring(0, uri.length() - 6);
      }

      if (X.isEmpty(uri)) {
        this.redirect("/");
      } else {
        this.redirect(uri);
      }
      s.remove("uri").store();

      return true;
    }

    this.redirect("/");

    /**
     * default, return false for "inherit" module to re-write it;
     */
    return false;

  }

  @Path(login = true, path = "get", log = Model.METHOD_POST)
  public void get() {
    long uid = this.getLong("uid", -1);
    if (uid <= 0) {
      uid = login.getId();
    }

    User u = User.loadById(uid);
    JSON jo = new JSON();
    if (u != null) {
      jo.put("data", u.getJSON());
      jo.put(X.STATE, 200);
      jo.put(X.MESSAGE, "ok");
    } else {
      jo.put(X.STATE, 201);
      jo.put(X.MESSAGE, "not exists");
    }
    this.response(jo);

  }

  /**
   * Login.
   */
  @Path(path = "login", log = Model.METHOD_POST)
  public void login() {

    if (method.isPost()) {

      String type = this.getString("type");

      JSON jo = new JSON();
      AuthToken a = null;
      if (Global.getInt("user.token", 1) == 1) {
        String token = this.getString("token");
        String sid = this.getString("sid");
        a = AuthToken.load(sid, token);
      }

      if (a != null) {
        // ok, logined
        jo.put(X.STATE, 200);
        jo.put(X.MESSAGE, "ok");
        jo.put("uid", a.getUid());
        jo.put("expired", a.getExpired());
      } else {
        String name = this.getString("name");
        if (name != null) {
          name = name.toLowerCase();
        }
        String pwd = this.getString("pwd");

        Captcha.Result r = Captcha.Result.ok;

        if (Global.getInt("user.captcha", 1) == 1) {
          String code = this.getString("code");
          if (code != null) {
            code = code.toLowerCase();
          }
          r = Captcha.verify(this.sid(), code);
          Captcha.remove(this.sid());
        }

        if (Captcha.Result.badcode == r) {
          jo.put(X.MESSAGE, lang.get("captcha.bad"));
          jo.put(X.STATE, 202);
        } else if (Captcha.Result.expired == r) {
          jo.put(X.MESSAGE, lang.get("captcha.expired"));
          jo.put(X.STATE, 203);
        } else {

          User me = User.load(name, pwd);
          log.debug("login: " + sid() + "-" + me);
          if (me != null) {

            long uid = me.getId();
            long time = System.currentTimeMillis() - X.AHOUR;
            List<User.Lock> list = User.Lock.loadByHost(uid, time, this.getRemoteHost());

            if (me.isLocked() || (list != null && list.size() >= 6)) {
              // locked by the host
              me.failed(this.getRemoteHost(), sid(), this.browser());
              jo.put(X.MESSAGE, lang.get("account.locked.error"));

              jo.put(X.STATE, 204);
              jo.put("name", name);
              jo.put("pwd", pwd);
            } else {
              list = User.Lock.loadBySid(uid, time, sid());
              if (list != null && list.size() >= 3) {
                me.failed(this.getRemoteHost(), sid(), this.browser());
                jo.put(X.MESSAGE, lang.get("account.locked.error"));
                jo.put("name", name);
                jo.put("pwd", pwd);
                jo.put(X.STATE, 204);
              } else {

                this.setUser(me);

                /**
                 * logined, to update the stat data
                 */
                me.logined(sid(), this.getRemoteHost());

                if ("json".equals(this.getString("type")) || isAjax()) {
                  jo.put("sid", sid());
                  jo.put("uid", me.getId());

                  /**
                   * test the configuration is enabled user token and this
                   * request is ajax
                   */
                  if (Global.getInt("user.token", 1) == 1) {
                    AuthToken t = AuthToken.update(me.getId(), sid(), this.getRemoteHost());
                    if (t != null) {
                      jo.put("token", t.getToken());
                      jo.put("expired", t.getExpired());
                      jo.put(X.STATE, HttpServletResponse.SC_OK);
                    } else {
                      jo.put(X.MESSAGE, "create authtoken error");
                      jo.put(X.STATE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    }
                  }
                  this.response(jo);
                } else {
                  this.redirect("/user/go");
                }
                return;
              }
            }

          } else {

            OpLog.warn(user.class, "login", lang.get("login.failed") + ":" + name, login, this.getRemoteHost());

            User u = User.load(name);
            if (u == null) {
              jo.put("message", lang.get("login.name_password.error"));
              jo.put(X.STATE, 201);
            } else {
              u.failed(this.getRemoteHost(), sid(), this.browser());

              List<User.Lock> list = User.Lock.loadByHost(u.getId(), System.currentTimeMillis() - X.AHOUR,
                  this.getRemoteHost());

              if (list != null && list.size() >= 6) {
                jo.put("message", lang.get("login.locked.error"));
                jo.put(X.STATE, 204);
              } else {
                list = User.Lock.loadBySid(u.getId(), System.currentTimeMillis() - X.AHOUR, sid());
                if (list != null && list.size() >= 3) {
                  jo.put("message", lang.get("login.locked.error"));
                  jo.put(X.STATE, 204);
                } else {
                  jo.put("message",
                      String.format(lang.get("login.name_password.error.times"), list == null ? 0 : list.size()));
                  jo.put(X.STATE, 204);
                }
              }
            }

            jo.put("name", name);
            jo.put("pwd", pwd);
          }
        }
      }

      if (X.isSame(type, "json")) {
        this.response(jo);
        return;
      } else {
        this.set(this.getJSON());
        this.set(jo);
      }
    }

    String refer = this.getString("refer");
    if (!X.isEmpty(refer) && !isAjax()) {
      try {
        this.getSession().set("uri", URLDecoder.decode(refer, "UTF-8")).store();
      } catch (Exception e) {
        log.error(refer, e);
        OpLog.error(user.class, "login", e.getMessage(), e, login, this.getRemoteHost());
      }
    }

    show("/user/user.login.html");
  }

  /**
   * Logout.
   */
  @Path(path = "logout", method = Model.METHOD_GET | Model.METHOD_POST)
  public void logout() {

    User u = this.getUser();

    if (u != null) {

      /**
       * clear auth-token
       */
      AuthToken.delete(u.getId(), sid());

      login.logout();

      /**
       * clear the user in session, but still keep the session
       */
      setUser(null);

    }

    /**
     * redirect to home
     */
    if (isAjax()) {
      JSON jo = JSON.create();
      jo.put(X.STATE, HttpServletResponse.SC_OK);
      jo.put(X.MESSAGE, "ok");
      this.response(jo);
    } else {
      this.redirect("/");
    }

  }

  /**
   * Verify.
   */
  @Path(path = "verify", access = "access.user.query")
  public void verify() {
    String name = this.getString("name").trim().toLowerCase();
    String value = this.getString("value").trim().toLowerCase();

    JSON jo = new JSON();
    if ("name".equals(name)) {
      if (User.exists(W.create("name", value))) {

        jo.put(X.STATE, 201);
        jo.put(X.MESSAGE, lang.get("user.name.exists"));

        OpLog.info(user.class, "verify", "name=" + name + ",value=" + value + ",exists", login, this.getRemoteHost());

      } else {
        String rule = Global.getString("user.name.rule", "^[a-zA-Z0-9]{4,16}$");

        if (X.isEmpty(value) || !value.matches(rule)) {
          jo.put(X.STATE, 201);
          jo.put(X.MESSAGE, lang.get("user.name.format.error"));

          OpLog.info(user.class, "verify", "name=" + name + ",value=" + value + ",rule=" + rule, login,
              this.getRemoteHost());
        } else {
          jo.put(X.STATE, 200);
        }
      }
    } else if ("password".equals(name)) {
      String rule = Global.getString("user.passwd.rule", "^[a-zA-Z0-9]{6,16}$");
      if (X.isEmpty(value) || !value.matches(rule)) {
        jo.put(X.STATE, 201);
        jo.put(X.MESSAGE, lang.get("user.passwd.format.error"));

        OpLog.info(user.class, "verify", "name=" + name + ",value=" + value + ",rule=" + rule, login,
            this.getRemoteHost());
      } else {
        jo.put(X.STATE, 200);
      }
    }

    this.response(jo);
  }

  /**
   * get user list by "access" token
   */
  @Path(path = "popup2", login = true, access = "access.user.query")
  public void popup2() {
    String access = this.getString("access");
    List<User> list = null;
    if (!X.isEmpty(access)) {
      list = User.loadByAccess(access);
    } else {
      Beans<User> bs = User.load(W.create().and(X.ID, 0, W.OP_GT), 0, 1000);
      if (bs != null) {
        list = bs.getList();
      }
    }

    JSON jo = new JSON();
    if (list != null && list.size() > 0) {
      List<JSON> arr = new ArrayList<JSON>();
      for (User e : list) {
        JSON j = new JSON();
        j.put("value", e.getId());
        j.put("name", e.get("nickname") + "(" + e.get("name") + ")");
        arr.add(j);
      }
      jo.put("list", arr);
      jo.put(X.STATE, 200);

    } else {
      jo.put(X.STATE, 201);
    }

    this.response(jo);

  }

  /**
   * Dashboard.
   */
  @Path(path = "dashboard", login = true)
  public void dashboard() {

    this.show("/user/user.dashboard.html");

  }

  @Path(path = "profile", login = true, log = Model.METHOD_POST)
  public void profile() {
    if (method.isPost()) {
      long id = login.getId();
      JSON j = this.getJSON();
      User u = User.loadById(id);
      if (u != null) {
        String password = this.getString("password");
        if (!X.isEmpty(password)) {
          u.update(V.create("password", password));
          JSON jo = new JSON();
          jo.put(X.STATE, 200);

          this.response(jo);
          return;
        } else {
          u.update(V.create().copy(j, "nickname", "title", "email", "phone"));

          this.set(X.MESSAGE, lang.get("save.success"));

          u = User.loadById(id);
          AuthToken.delete(id);
          this.setUser(u);
        }
        this.set(u.getJSON());
      } else {
        this.set(X.ERROR, lang.get("save.failed"));
      }
      this.set(j);
    } else {
      User u = User.loadById(login.getId());
      this.set("u", u);
      JSON jo = new JSON();
      u.toJSON(jo);
      this.set(jo);
    }

    this.show("/user/user.profile.html");
  }

  /**
   * Edits the.
   */
  @Path(path = "edit", login = true, log = Model.METHOD_POST)
  public void edit() {
    if (method.isPost()) {
      long id = login.getId();
      JSON j = this.getJSON();
      User u = User.loadById(id);
      if (u != null) {
        String password = this.getString("password");
        if (!X.isEmpty(password)) {
          u.update(V.create("password", password));
          JSON jo = new JSON();
          jo.put(X.STATE, 200);

          this.response(jo);
          return;
        } else {
          u.update(V.create().copy(j, "nickname", "title", "email", "phone"));

          this.set(X.MESSAGE, lang.get("save.success"));

          u = User.loadById(id);
          AuthToken.delete(id);
          this.setUser(u);
        }
        this.set(u.getJSON());
      } else {
        this.set(X.ERROR, lang.get("save.failed"));
      }
      this.set(j);
    } else {
      User u = User.loadById(login.getId());
      this.set("u", u);
      JSON jo = new JSON();
      u.toJSON(jo);
      this.set(jo);
    }

    this.show("/user/user.edit.html");
  }

  /**
   * Forget password
   */
  @Path(path = "forget")
  public void forget() {

    if (method.isPost()) {
      JSON jo = JSON.create();

      String email = this.getString("email");
      String phone = this.getString("phone");
      int phase = this.getInt("phase");

      if (!X.isEmpty(email)) {
        if (phase == 0) {
          // verify email and send a code
          Code c = Code.load(W.create("s2", email).sort("created", -1));
          if (c != null && c.getUpdated() < X.AMINUTE) {

            jo.put(X.MESSAGE, lang.get("user.forget.email.sent"));
            jo.put(X.STATE, HttpServletResponse.SC_OK);

          } else {
            int s = 0;

            StringBuilder sb = new StringBuilder();
            W q = W.create("email", email);
            Beans<User> bs = User.load(q, s, 10);
            while (bs != null && bs.getList() != null && bs.getList().size() > 0) {
              for (User u : bs.getList()) {
                if (!u.isDeleted()) {
                  if (sb.length() > 0) {
                    sb.append(",");
                  }
                  sb.append(u.getName());
                }
              }
              s += bs.getList().size();
              bs = User.load(q, s, 10);
            }

            if (sb.length() > 0) {

              String code = null;

              if (c == null || c.getExpired() < System.currentTimeMillis()) {
                code = UID.random(10);
                Code.create(code, email, V.create("expired", System.currentTimeMillis() + X.ADAY));
              } else {
                code = c.getString("s1");
              }

              File f = module.getFile("/user/email.validation." + lang.getLocale() + ".template");
              if (f != null) {
                JSON j1 = JSON.create();
                j1.put("email", email);
                j1.put("account", sb.toString());
                j1.put("code", code);

                VelocityView v1 = new VelocityView();
                String body = v1.parse(f, j1);
                if (body != null) {
                  if (Email.send(lang.get("mail.validation.code"), body, email)) {
                    jo.put(X.MESSAGE, lang.get("user.forget.email.sent"));
                    jo.put(X.STATE, HttpServletResponse.SC_OK);
                    Code.update(W.create("s1", code).and("s2", email), V.create("updated", System.currentTimeMillis()));

                  } else {
                    jo.put(X.MESSAGE, lang.get("user.forget.email.sent.failed"));
                    jo.put(X.STATE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                  }
                } else {
                  jo.put(X.STATE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                  jo.put(X.MESSAGE, lang.get("user.forget.template.error"));
                }
              } else {
                jo.put(X.STATE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                jo.put(X.MESSAGE, lang.get("user.forget.template.notfound"));
              }
            } else {
              jo.put(X.STATE, HttpServletResponse.SC_BAD_REQUEST);
              jo.put(X.MESSAGE, lang.get("user.forget.noaccount"));
            }
          }
        } else if (phase == 1) {
          // verify code
          String code = this.getString("code");
          Code c = Code.load(code, email);
          if (c == null) {
            jo.put(X.STATE, HttpServletResponse.SC_BAD_REQUEST);
            jo.put(X.MESSAGE, lang.get("email.code.bad"));
          } else if (c.getExpired() < System.currentTimeMillis()) {
            jo.put(X.STATE, HttpServletResponse.SC_BAD_REQUEST);
            jo.put(X.MESSAGE, lang.get("email.code.expired"));
          } else {
            Code.delete(code, email);
            jo.put(X.STATE, HttpServletResponse.SC_OK);
            jo.put(X.MESSAGE, lang.get("email.code.ok"));
          }
        } else if (phase == 2) {
          // change the password
          String passwd = this.getString("passwd");
          String rule = Global.getString("user.passwd.rule", "^[a-zA-Z0-9]{6,16}$");
          if (!X.isEmpty(rule) && !passwd.matches(rule)) {
            jo.put(X.STATE, HttpServletResponse.SC_BAD_REQUEST);
            jo.put(X.MESSAGE, "user.passwd.format.error");
          } else {
            User.update(W.create("email", email), V.create("password", passwd));
            jo.put(X.STATE, HttpServletResponse.SC_OK);
            jo.put(X.MESSAGE, lang.get("user.passwd.updated"));
          }
        }
      } else if (!X.isEmpty(phone)) {

        if (phase == 0) {
          // verify email and send a code
          Code c = Code.load(W.create("s2", phone).sort("created", -1));
          if (c != null && c.getUpdated() < X.AMINUTE) {

            jo.put(X.MESSAGE, lang.get("user.forget.phone.sent"));
            jo.put(X.STATE, HttpServletResponse.SC_OK);

          } else {
            int s = 0;

            StringBuilder sb = new StringBuilder();
            W q = W.create("phone", phone);
            Beans<User> bs = User.load(q, s, 10);
            while (bs != null && bs.getList() != null && bs.getList().size() > 0) {
              for (User u : bs.getList()) {
                if (!u.isDeleted()) {
                  if (sb.length() > 0) {
                    sb.append(",");
                  }
                  sb.append(u.getName());
                }
              }
              s += bs.getList().size();
              bs = User.load(q, s, 10);
            }

            if (sb.length() > 0) {

              String code = null;

              if (c == null || c.getExpired() < System.currentTimeMillis()) {
                code = UID.digital(4);
                Code.create(code, phone, V.create("expired", System.currentTimeMillis() + X.AMINUTE * 6));
              } else {
                code = c.getString("s1");
              }

              JSON j1 = JSON.create();
              j1.put("phone", phone);
              j1.put("account", sb.toString());
              j1.put("code", code);

              if (Sms.send(phone, "user.forget.password", j1)) {
                jo.put(X.MESSAGE, lang.get("user.forget.phone.sent"));
                jo.put(X.STATE, HttpServletResponse.SC_OK);
                Code.update(W.create("s1", code).and("s2", phone), V.create("updated", System.currentTimeMillis()));

              } else {
                jo.put(X.MESSAGE, lang.get("user.forget.phone.sent.failed"));
                jo.put(X.STATE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

              }

            } else {
              jo.put(X.STATE, HttpServletResponse.SC_BAD_REQUEST);
              jo.put(X.MESSAGE, lang.get("user.forget.noaccount"));
            }
          }
        } else if (phase == 1) {
          // verify code
          String code = this.getString("code");
          Code c = Code.load(code, phone);
          if (c == null) {
            jo.put(X.STATE, HttpServletResponse.SC_BAD_REQUEST);
            jo.put(X.MESSAGE, lang.get("phone.code.bad"));
          } else if (c.getExpired() < System.currentTimeMillis()) {
            jo.put(X.STATE, HttpServletResponse.SC_BAD_REQUEST);
            jo.put(X.MESSAGE, lang.get("phone.code.expired"));
          } else {
            Code.delete(code, phone);
            jo.put(X.STATE, HttpServletResponse.SC_OK);
            jo.put(X.MESSAGE, lang.get("phone.code.ok"));
          }
        } else if (phase == 2) {
          // change the password
          String passwd = this.getString("passwd");
          String rule = Global.getString("user.passwd.rule", "^[a-zA-Z0-9]{6,16}$");
          if (!X.isEmpty(rule) && !passwd.matches(rule)) {
            jo.put(X.STATE, HttpServletResponse.SC_BAD_REQUEST);
            jo.put(X.MESSAGE, "user.passwd.format.error");
          } else {
            User.update(W.create("phone", phone), V.create("password", passwd));
            jo.put(X.STATE, HttpServletResponse.SC_OK);
            jo.put(X.MESSAGE, lang.get("user.passwd.updated"));
          }
        }

      } else {
        jo.put(X.STATE, HttpServletResponse.SC_BAD_REQUEST);
        jo.put(X.MESSAGE, lang.get("param.error"));
      }

      if (this.isAjax()) {
        this.response(jo);
        return;
      } else {
        this.set(jo);
      }
      this.set("email", email);
      this.set("phone", phone);
    }

    show("/user/user.forget.html");

  }

  public static void main(String[] args) {
    String rule = "^[a-zA-Z0-9]{4,16}$";
    String name = "joej";
    System.out.println(name.matches(rule));

  }
}
