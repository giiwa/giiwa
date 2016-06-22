/*
 * Copyright 2015 Giiwa, Inc. and/or its affiliates.
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

import java.awt.image.BufferedImage;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpSession;

import org.giiwa.core.base.Base64;
import org.giiwa.core.base.DES;
import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;
import org.giiwa.core.bean.Bean.V;
import org.giiwa.core.conf.Global;
import org.giiwa.framework.bean.Appkey;
import org.giiwa.framework.bean.AuthToken;
import org.giiwa.framework.bean.OpLog;
import org.giiwa.framework.bean.Session;
import org.giiwa.framework.bean.User;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;
import org.giiwa.utils.image.Captcha;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.mongodb.BasicDBObject;

// TODO: Auto-generated Javadoc
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
    } else if (login.hasAccess("access.admin")) {
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
    if (!"true".equals(Global.s("user.register", "true"))) {
      this.set(X.MESSAGE, lang.get("register.deny"));
      this.redirect("/user/login");
      return;
    }

    if (method.isPost()) {

      String name = this.getString("name");
      // String email = this.getString("email");
      // String pwd = this.getString("pwd");
      // String nickname = this.getString("nickname");

      // Map<String, String> attr = new HashMap<String, String>();
      // attr.put("ip", this.getRemoteHost());
      // attr.put("browser", this.browser());

      JSONObject jo = this.getJSON();
      try {
        long id = User.create(V.create().copy(jo));

        this.setUser(User.loadById(id));
        OpLog.log(User.class, "register", lang.get("create.success") + ":" + name + ", uid=" + id);

        Session s = this.getSession();
        if (s.has("uri")) {
          this.redirect((String) s.get("uri"));
        } else {
          this.redirect("/");
        }

        return;
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        this.put(X.MESSAGE, lang.get("create_user_error_1"));
        OpLog.log(User.class, "register", lang.get("create.failed") + ":" + name);
      }

    }

    this.set("me", this.getUser());

    show("/user/user.register.html");

  }

  /**
   * Go.
   * 
   * @return true, if successful
   */
  @Path(path = "go", login = true)
  public boolean go() {

    Session s = this.getSession();
    if (s.has("oauth.callback")) {
      // it's come from oauth
      String url = (String) s.get("oauth.callback");
      String key = (String) s.get("oauth.appkey");

      try {

        JSONObject jo = new JSONObject();
        jo.put("uid", login.getId());
        jo.put("time", System.currentTimeMillis());
        jo.put("method", "login");
        JSONObject j1 = new JSONObject();
        login.toJSON(j1);
        jo.put("user", j1);

        String data = URLEncoder.encode(Base64.encode(DES.encode(jo.toString().getBytes(), key.getBytes())), "UTF-8");

        if (url.indexOf("?") > 0) {
          this.redirect(url + "&data=" + data);
        } else {
          this.redirect(url + "?data=" + data);
        }
        s.remove("oauth.callback").remove("oauth.appkey").store();

        return true;
      } catch (Exception e) {
        log.error("url=" + url + ", key=" + key, e);
      }
    } else if (s.has("uri")) {
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

  /**
   * Login_popup.
   */
  @Path(path = "login/popup")
  public void login_popup() {
    login();
  }

  /**
   * Login.
   */
  @Path(path = "login", log = Model.METHOD_POST)
  public void login() {
    if (!Bean.isConfigured()) {
      this.redirect("/configure");
      return;
    }

    if (method.isPost()) {

      String name = this.getString("name");
      String pwd = this.getString("pwd");
      String code = this.getString("code").toLowerCase();
      JSONObject jo = new JSONObject();

      Captcha.Result r = Captcha.verify(this.sid(), code);

      if (Captcha.Result.badcode == r) {
        jo.put(X.MESSAGE, lang.get("captcha.bad"));
      } else if (Captcha.Result.expired == r) {
        jo.put(X.MESSAGE, lang.get("captcha.expired"));
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

            jo.put("name", name);
            jo.put("pwd", pwd);
          } else {
            list = User.Lock.loadBySid(uid, time, sid());
            if (list != null && list.size() >= 3) {
              me.failed(this.getRemoteHost(), sid(), this.browser());
              jo.put(X.MESSAGE, lang.get("account.locked.error"));
              jo.put("name", name);
              jo.put("pwd", pwd);
            } else {

              this.setUser(me);

              /**
               * logined, to update the stat data
               */
              me.logined(sid(), this.getRemoteHost());

              if ("json".equals(this.getString("type"))) {
                jo.put("sid", sid());
                AuthToken t = AuthToken.update(me.getId(), sid(), this.getRemoteHost());
                if (t != null) {
                  jo.put("token", t.getToken());
                  jo.put("expired", t.getExpired());
                } else {
                  jo.put(X.MESSAGE, "create authtoken error");
                }
                this.response(jo);
              } else {
                this.redirect("/user/go");
              }
              return;
            }
          }

        } else {

          OpLog.warn(User.class, "user.login", lang.get("login.failed") + ":" + name + ", ip:" + this.getRemoteHost(),
              null);

          User u = User.load(name);
          if (u == null) {
            jo.put("message", lang.get("login.name_password.error"));
          } else {
            u.failed(this.getRemoteHost(), sid(), this.browser());

            List<User.Lock> list = User.Lock.loadByHost(u.getId(), System.currentTimeMillis() - X.AHOUR,
                this.getRemoteHost());

            if (list != null && list.size() >= 6) {
              jo.put("message", lang.get("login.locked.error"));
            } else {
              list = User.Lock.loadBySid(u.getId(), System.currentTimeMillis() - X.AHOUR, sid());
              if (list != null && list.size() >= 3) {
                jo.put("message", lang.get("login.locked.error"));
              } else {
                jo.put("message",
                    String.format(lang.get("login.name_password.error.times"), list == null ? 0 : list.size()));
              }
            }
          }

          jo.put("name", name);
          jo.put("pwd", pwd);
        }
      }
      if ("json".equals(this.getString("type"))) {
        this.response(jo);
      } else {
        this.set(jo);
      }
    }

    String refer = this.getString("refer");
    if (!X.isEmpty(refer)) {
      try {
        this.getSession().set("uri", URLDecoder.decode(refer, "UTF-8")).store();
      } catch (Exception e) {
        log.error(refer, e);
      }
    }

    show("/user/user.login.html");
  }

  /**
   * Logout.
   */
  @Path(path = "logout", method = Model.METHOD_GET | Model.METHOD_POST)
  public void logout() {
    if (this.getUser() != null) {
      this.getUser().logout();
    }

    if (this.getUser() != null) {
      /**
       * clear the user in session, but still keep the session
       */
      setUser(null);

      /**
       * test the oauth authenticated is true
       */
      if ("true".equals(Global.s("oauth.enabled", null))) {
        String oauth = Global.s("oauth.url", null);
        String appkey = Global.s("oauth.appkey", null);
        String key = Global.s("oauth.key", null);
        if (oauth != null && appkey != null && key != null) {
          String callback = Global.s("oauth.callback", "");
          try {
            StringBuilder url = new StringBuilder(oauth);
            if (!oauth.endsWith("/")) {
              url.append("/");
            }

            JSONObject jo = new JSONObject();
            jo.put("callback", callback);
            jo.put("force", false);
            jo.put("time", System.currentTimeMillis());

            String data = Base64.encode(DES.encode(jo.toString().getBytes(), key.getBytes()));
            data = URLEncoder.encode(data, "UTF-8");
            log.debug("data=" + data);

            url.append(appkey).append("/logout").append("?data=").append(data);

            this.redirect(url.toString());

            return;
          } catch (Exception e) {
            log.error("oauth=" + oauth + ", appkey=" + appkey + ", key=" + key, e);
          }
        }
      }
    }

    int s = 0;
    BasicDBObject q = new BasicDBObject();
    BasicDBObject order = new BasicDBObject(X._ID, 1);
    Beans<Appkey> bs = Appkey.load(q, order, s, 10);
    while (bs != null && bs.getList() != null && bs.getList().size() > 0) {
      for (Appkey a : bs.getList()) {
        String key = "sso.oauth." + a.getAppkey();
        if ("1".equals(this.getSession().get(key))) {
          this.getSession().remove(key).store();
          if (!X.isEmpty(a.getLogout())) {
            this.redirect(a.getLogout());
            return;
          }
        }
      }
      s += bs.getList().size();
      bs = Appkey.load(q, order, s, 10);

    }

    /**
     * redirect to home
     */
    this.redirect("/");

  }

  /**
   * Verify.
   */
  @Path(path = "verify", login = true, access = "access.user.query")
  public void verify() {
    String name = this.getString("name").trim();
    String value = this.getString("value").trim();

    JSONObject jo = new JSONObject();
    if ("name".equals(name)) {
      if (User.exists(new BasicDBObject("name", value))) {

        jo.put(X.STATE, 201);
        jo.put(X.MESSAGE, lang.get("user.name.exists"));

      } else if (User.exists(new BasicDBObject("name", value))) {
        jo.put(X.STATE, 202);
        jo.put(X.MESSAGE, lang.get("user.override.exists"));
      } else {
        String allow = Global.s("user.name", "^[a-zA-Z0-9]{4,16}$");

        if (X.isEmpty(value) || !value.matches(allow)) {
          jo.put(X.STATE, 201);
          jo.put(X.MESSAGE, lang.get("user.name.format.error"));
        } else {
          jo.put(X.STATE, 200);
        }
      }
    } else if ("password".equals(name)) {
      if (X.isEmpty(value)) {
        jo.put(X.STATE, 201);
        jo.put(X.MESSAGE, lang.get("user.password.format.error"));
      } else {
        String allow = Global.s("user.password", "^[a-zA-Z0-9]{6,16}$");
        if (!value.matches(allow)) {
          jo.put(X.STATE, 201);
          jo.put(X.MESSAGE, lang.get("user.password.format.error"));
        } else {
          jo.put(X.STATE, 200);
        }
      }
    } else {
      if (X.isEmpty(value)) {
        jo.put(X.STATE, 201);
        jo.put(X.MESSAGE, lang.get("user.not.empty"));
      } else {
        jo.put(X.STATE, 200);
      }
    }

    this.response(jo);
  }

  /**
   * Popup2.
   */
  @Path(path = "popup2", login = true, access = "access.user.query")
  public void popup2() {
    String access = this.getString("access");
    List<User> list = null;
    if (!X.isEmpty(access)) {
      list = User.loadByAccess(access);
    } else {
      Beans<User> bs = User.load(new BasicDBObject(X._ID, new BasicDBObject("$gt", 0)), 0, 1000);
      if (bs != null) {
        list = bs.getList();
      }
    }

    JSONObject jo = new JSONObject();
    if (list != null && list.size() > 0) {
      JSONArray arr = new JSONArray();
      for (User e : list) {
        JSONObject j = new JSONObject();
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

  // /**
  // * Message_count.
  // */
  // @Path(path = "message/count", login = true)
  // public void message_count() {
  // JSONObject jo = new JSONObject();
  // Beans<Message> bs = Message.load(login.getId(), W.create("flag",
  // Message.FLAG_NEW), 0, 1);
  // if (bs != null && bs.getTotal() > 0) {
  // jo.put("count", bs.getTotal());
  // } else {
  // jo.put("count", 0);
  // }
  // jo.put(X.STATE, 200);
  // this.response(jo);
  // }

  // /**
  // * Message_delete.
  // */
  // @Path(path = "message/delete", login = true)
  // public void message_delete() {
  // String ids = this.getString("id");
  // int updated = 0;
  // if (ids != null) {
  // String[] ss = ids.split(",");
  // for (String s : ss) {
  // updated += Message.delete(login.getId(), s);
  // }
  // }
  //
  // if (updated > 0) {
  // this.set(X.MESSAGE, lang.get("delete_success"));
  // } else {
  // this.set(X.MESSAGE, lang.get("select.required"));
  // }
  //
  // message();
  // }

  // /**
  // * Message_detail.
  // */
  // @Path(path = "message/detail", login = true)
  // public void message_detail() {
  // String id = this.getString("id");
  // if (id == null) {
  // message();
  // return;
  // }
  //
  // Message m = Message.load(login.getId(), id);
  // if (m == null) {
  // message();
  // return;
  // }
  //
  // this.set("m", m);
  //
  // this.show("/user/message.detail.html");
  // }

  // /**
  // * Message_mark.
  // */
  // @Path(path = "message/mark", login = true)
  // public void message_mark() {
  // String ids = this.getString("id");
  // int updated = 0;
  // if (ids != null) {
  // String[] ss = ids.split(",");
  // V v = V.create("flag", Message.FLAG_MARK);
  // for (String s : ss) {
  // updated += Message.update(login.getId(), s, v);
  // }
  // }
  //
  // if (updated > 0) {
  // this.set(X.MESSAGE, lang.get("save.success"));
  // } else {
  // this.set(X.MESSAGE, lang.get("select.required"));
  // }
  //
  // message();
  // }

  // /**
  // * Message_done.
  // */
  // @Path(path = "message/done", login = true)
  // public void message_done() {
  // String ids = this.getString("id");
  // int updated = 0;
  // if (ids != null) {
  // String[] ss = ids.split(",");
  // V v = V.create("flag", Message.FLAG_DONE);
  // for (String s : ss) {
  // updated += Message.update(login.getId(), s, v);
  // }
  // }
  //
  // if (updated > 0) {
  // this.set(X.MESSAGE, lang.get("save.success"));
  // } else {
  // this.set(X.MESSAGE, lang.get("select.required"));
  // }
  //
  // message();
  //
  // }

  // /**
  // * Message.
  // */
  // @Path(path = "message", login = true)
  // public void message() {
  //
  // JSONObject jo = this.getJSON();
  // if (!"message".equals(this.path)) {
  // Object o = this.getSession().get("query");
  // if (o != null && o instanceof JSONObject) {
  // jo.clear();
  // jo.putAll((JSONObject) o);
  // }
  // } else {
  // this.getSession().set("query", jo).store();
  // }
  // W w = W.create();
  // w.copy(jo, W.OP_LIKE, "subject").copy(jo, W.OP_EQ, "flag");
  // this.set(jo);
  //
  // int s = this.getInt(jo, "s");
  // int n = this.getInt(jo, "n", 10, "default.list.number");
  //
  // Beans<Message> bs = Message.load(login.getId(), w, s, n);
  // this.set(bs, s, n);
  // if (bs != null && bs.getList() != null && bs.getList().size() > 0) {
  // for (Message m : bs.getList()) {
  // if (Message.FLAG_NEW.equals(m.getFlag())) {
  // m.update(V.create("flag", Message.FLAG_READ));
  // }
  // }
  // }
  //
  // this.show("/user/user.message.html");
  // }

  /**
   * Dashboard.
   */
  @Path(path = "dashboard", login = true)
  public void dashboard() {

    /**
     * get the total of user messages, new messages
     */
    // Beans<Message> bs = Message.load(login.getId(), W.create("flag",
    // Message.FLAG_NEW), 0, 1);
    // if (bs != null && bs.getTotal() > 0) {
    // this.set("message_new", bs.getTotal());
    // }
    //
    // bs = Message.load(login.getId(), W.create(), 0, 1);
    // if (bs != null && bs.getTotal() > 0) {
    // this.set("message_total", bs.getTotal());
    // }

    this.show("/user/user.dashboard.html");

  }

  /**
   * Edits the.
   */
  @Path(path = "edit", login = true, log = Model.METHOD_POST)
  public void edit() {
    if (method.isPost()) {
      long id = login.getId();
      JSONObject j = this.getJSON();
      User u = User.loadById(id);
      if (u != null) {
        String password = this.getString("password");
        if (!X.isEmpty(password)) {
          u.update(V.create("password", password));
          JSONObject jo = new JSONObject();
          jo.put(X.STATE, 200);

          this.response(jo);
          return;
        } else {
          u.update(V.create().copy(j, "nickname", "title", "email", "phone"));

          this.set(X.MESSAGE, lang.get("save.success"));

          u = User.loadById(id);
          AuthToken.remove(id);
          this.setUser(u);
        }
      } else {
        this.set(X.ERROR, lang.get("save.failed"));
      }
      this.set(j);
    } else {
      User u = User.loadById(login.getId());
      this.set("u", u);
      JSONObject jo = new JSONObject();
      u.toJSON(jo);
      this.set(jo);
    }

    this.show("/user/user.edit.html");
  }

  /**
   * Callback.
   */
  @Path(path = "callback")
  public void callback() {
    String data = this.getString("data");
    String key = Global.s("oauth.key", null);
    if (data != null && key != null) {
      try {
        // data = URLDecoder.decode(data, "UTF-8");
        byte[] bb = Base64.decode(data);
        bb = DES.decode(bb, key.getBytes());
        data = new String(bb);

        JSONObject jo = JSONObject.fromObject(data);

        String method = jo.getString("method");
        if ("login".equals(method)) {
          if (jo.has("uid") && jo.has("time") && System.currentTimeMillis() - jo.getLong("time") < X.AMINUTE) {
            int uid = jo.getInt("uid");

            User me = User.loadById(uid);
            log.debug("uid=" + uid + ", user=" + me);

            if (me != null) {
              this.setUser(me);

              if ("true".equals(Global.s("cross.context", X.EMPTY))) {
                String sessionkey = Global.s("session.key", "user");
                JSONObject j1 = new JSONObject();
                me.toJSON(j1);
                HttpSession s = this.getHttpSession(true);
                s.getServletContext().setAttribute(sessionkey, j1);

                // log.debug("set session: " + s + ", "
                // + sessionkey + "=" + j1);
              }

              this.redirect("/user/go");

              return;
            } else {
              log.warn("can not found uid=" + uid);

              /**
               * force login again
               */
              String oauth = Global.s("oauth.url", null);
              String appkey = Global.s("oauth.appkey", null);
              key = Global.s("oauth.key", null);
              if (oauth != null && appkey != null && key != null) {
                String callback = Global.s("oauth.callback", "");
                try {
                  StringBuilder url = new StringBuilder(oauth);
                  if (!oauth.endsWith("/")) {
                    url.append("/");
                  }
                  jo = new JSONObject();
                  jo.put("callback", callback);
                  jo.put("force", true);

                  bb = jo.toString().getBytes();
                  bb = DES.encode(bb, key.getBytes());

                  url.append(appkey).append("/login").append("?data=")
                      .append(URLEncoder.encode(Base64.encode(bb), "UTF-8"));

                  this.redirect(url.toString());

                  return;
                } catch (Exception e) {
                  log.error("oauth=" + oauth + ", appkey=" + appkey + ", key=" + key, e);
                }
              }

            }
          }
        } else if ("logout".equals(method)) {

          if (jo.has("time") && System.currentTimeMillis() - jo.getLong("time") < X.AMINUTE) {

            this.setUser(null);

            this.redirect("/user/go");

            return;
          }
        }

      } catch (Exception e) {
        log.error("data=" + data + ", key=" + key, e);
      }
    }

    this.println(lang.get("callback.failed"));

  }

  /**
   * Forget.
   */
  @Path(path = "forget")
  public void forget() {

    if (method.isPost()) {
      String email = this.getString("email");
      int s = 0;
      BasicDBObject q = new BasicDBObject("email", email);
      Beans<User> bs = User.load(q, s, 10);
      List<String> list = new ArrayList<String>();
      while (bs != null && bs.getList() != null && bs.getList().size() > 0) {
        for (User u : bs.getList()) {
          if (!u.isDeleted()) {
            String token = UID.id(u.getId(), System.currentTimeMillis());
            u.update(V.create("reset_token", token).set("token_expired", System.currentTimeMillis() + X.ADAY));
            list.add("/user/reset?email=" + email + "&token=" + token);
          }
        }
        s += bs.getList().size();
        bs = User.load(q, s, 10);
      }

      if (list.size() > 0) {
        this.set("sent", 1);
      } else {
        this.set(X.MESSAGE, lang.get("invalid.email"));
      }
      this.set("email", email);
    }

    show("/user/user.forget.html");

  }

}
