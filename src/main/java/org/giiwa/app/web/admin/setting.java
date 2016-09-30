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
package org.giiwa.app.web.admin;

import java.util.LinkedHashMap;
import java.util.Map;

import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.framework.bean.OpLog;
import org.giiwa.framework.bean.Role;
import org.giiwa.framework.web.*;

/**
 * web api: /admin/setting <br>
 * use to custom setting, all module configuration MUST inherit from this class,
 * and override the "set" and "get" method,<br>
 * required "access.config.admin"
 * 
 * @author joe
 *
 */
public class setting extends Model {

  private static Map<String, Class<? extends setting>> settings = new LinkedHashMap<String, Class<? extends setting>>();

  /**
   * Register.
   *
   * @param name
   *          the name
   * @param m
   *          the m
   */
  final public static void register(String name, Class<? extends setting> m) {
    settings.put(name, m);
  }

  /**
   * Reset.
   *
   * @param name
   *          the name
   * @return the object
   */
  @Path(path = "reset/(.*)", login = true, access = "access.config.admin")
  final public Object reset(String name) {
    Class<? extends setting> c = settings.get(name);
    log.debug("/reset/" + c);
    if (c != null) {
      try {
        setting s = c.newInstance();
        s.req = this.req;
        s.resp = this.resp;
        s.login = this.login;
        s.lang = this.lang;
        s.module = this.module;
        s.reset();

        s.set("lang", lang);
        s.set("module", module);
        s.set("name", name);
        s.set("settings", settings.keySet());
        s.show("/admin/setting.html");

      } catch (Exception e) {
        log.error(name, e);
        OpLog.error(setting.class, "reset", e.getMessage(), e, login, this.getRemoteHost());

        this.show("/admin/setting.html");
      }
    }

    return null;
  }

  /**
   * Gets the.
   *
   * @param name
   *          the name
   * @return the object
   */
  @Path(path = "get/(.*)", login = true, access = "access.config.admin")
  final public Object get(String name) {
    Class<? extends setting> c = settings.get(name);
    log.debug("/get/" + c);
    if (c != null) {
      try {
        setting s = c.newInstance();
        s.copy(this);
        s.get();

        s.set("lang", lang);
        s.set("module", module);
        s.set("name", name);
        s.set("settings", settings.keySet());
        s.show("/admin/setting.html");

      } catch (Exception e) {
        log.error(name, e);
        OpLog.error(setting.class, "get", e.getMessage(), e, login, this.getRemoteHost());

        this.show("/admin/setting.html");
      }
    }

    return null;
  }

  /**
   * Sets the.
   *
   * @param name
   *          the name
   */
  @Path(path = "set/(.*)", login = true, access = "access.config.admin", log = Model.METHOD_POST)
  final public void set(String name) {
    Class<? extends setting> c = settings.get(name);
    log.debug("/set/" + c);
    if (c != null) {
      try {
        setting s = c.newInstance();
        s.copy(this);
        s.set();

        s.set("lang", lang);
        s.set("module", module);
        s.set("name", name);
        s.set("settings", settings.keySet());
        s.show("/admin/setting.html");
      } catch (Exception e) {
        log.error(name, e);
        OpLog.error(setting.class, "set", e.getMessage(), e, login, this.getRemoteHost());

        this.show("/admin/setting.html");
      }
    }
  }

  /**
   * invoked when post setting form.
   */
  public void set() {

  }

  /**
   * invoked when reset called.
   */
  public void reset() {
    get();
  }

  /**
   * invoked when get the setting form.
   */
  public void get() {

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.giiwa.framework.web.Model#onGet()
   */
  @Path(login = true, access = "access.config.admin")
  public final void onGet() {

    if (settings.size() > 0) {
      String name = settings.keySet().iterator().next();
      this.set("name", name);
      get(name);
      return;
    }

    this.println("not find page");

  }

  public static class system extends setting {

    /*
     * (non-Javadoc)
     * 
     * @see org.giiwa.app.web.admin.setting#set()
     */
    @Override
    public void set() {
      String lang1 = this.getString("language");
      Global.setConfig("language", lang1);

      Global.setConfig("home.uri", this.getHtml("home_uri"));
      Global.setConfig("user.name.rule", this.getHtml("user_name"));
      Global.setConfig("user.passwd.rule", this.getHtml("user_passwd"));
      Global.setConfig("user.captcha", X.isSame(this.getString("user_captcha"), "on") ? 1 : 0);
      Global.setConfig("user.token", X.isSame(this.getString("user_token"), "on") ? 1 : 0);
      Global.setConfig("user.system", this.getString("user_system"));
      Global.setConfig("user.role", this.getString("user_role"));
      Global.setConfig("cross.domain", this.getString("cross_domain"));
      Global.setConfig("cross.header", this.getString("cross_header"));
      String url = this.getString("site_url").trim();
      while (url.endsWith("/")) {
        url = url.substring(0, url.length() - 1);
      }
      Global.setConfig("site.url", url);

      this.set(X.MESSAGE, lang.get("save.success"));

      get();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.giiwa.app.web.admin.setting#get()
     */
    @Override
    public void get() {

      this.set("node", Global.getString("node", null));
      this.set("system_code", Global.getLong("system.code", 0));
      this.set("language", Global.getString("language", "zh_cn"));
      this.set("level", Global.getString("run.level", "debug"));
      this.set("user_system", Global.getString("user.system", "close"));
      this.set("user_role", Global.getString("user.role", "N/A"));
      this.set("cross_domain", Global.getString("cross.domain", "no"));
      this.set("cross_header", Global.getString("cross.header", "Content-Type, accept, Origin"));

      this.set("cache_url", Global.getString("cache.url", null));
      this.set("cache_group", Global.getString("cache.group", "demo"));
      this.set("mongo_url", Global.getString("mongo[prod].url", null));
      this.set("mongo_db", Global.getString("mongo[prod].db", null));
      this.set("mongo_user", Global.getString("mongo[prod].user", null));
      this.set("db_url", Global.getString("db.url", null));

      this.set("roles", Role.load(0, 100).getList());

      this.set("page", "/admin/setting.system.html");
    }

  }

  public static class mail extends setting {

    /*
     * (non-Javadoc)
     * 
     * @see org.giiwa.app.web.admin.setting#set()
     */
    @Override
    public void set() {
      Global.setConfig("mail.protocol", this.getString("protocol"));
      Global.setConfig("mail.host", this.getString("host"));
      Global.setConfig("mail.email", this.getString("email"));
      Global.setConfig("mail.title", this.getString("title"));
      Global.setConfig("mail.user", this.getString("user"));
      Global.setConfig("mail.passwd", this.getString("passwd"));

      this.set(X.MESSAGE, lang.get("save.success"));

      get();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.giiwa.app.web.admin.setting#get()
     */
    @Override
    public void get() {
      this.set("protocol", Global.getString("mail.protocol", "smtp"));
      this.set("host", Global.getString("mail.host", X.EMPTY));
      this.set("email", Global.getString("mail.email", X.EMPTY));
      this.set("title", Global.getString("mail.title", X.EMPTY));
      this.set("user", Global.getString("mail.user", X.EMPTY));
      this.set("passwd", Global.getString("mail.passwd", X.EMPTY));

      this.set("page", "/admin/setting.mail.html");
    }

  }

  public static class counter extends setting {

    /*
     * (non-Javadoc)
     * 
     * @see org.giiwa.app.web.admin.setting#set()
     */
    @Override
    public void set() {
      Global.setConfig("site.counter", this.getHtml("counter"));

      this.set(X.MESSAGE, lang.get("save.success"));

      get();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.giiwa.app.web.admin.setting#get()
     */
    @Override
    public void get() {
      // this.set("counter", ConfigGlobal.s("site.counter", X.EMPTY));
      this.set("page", "/admin/setting.counter.html");
    }

  }
}
