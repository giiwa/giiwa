/*
 *   giiwa, a java web foramewrok.
 *   Copyright (C) <2014>  <giiwa.org>
 *
 */
package org.giiwa.app.web.admin;

import java.util.LinkedHashMap;
import java.util.Map;

import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.framework.bean.OpLog;
import org.giiwa.framework.bean.Role;
import org.giiwa.framework.web.*;

/**
 * web api: /admin/setting <br>
 * use to custom setting, all module configuration MUST inherit from this class,
 * and override the "set" and "get" method
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
        OpLog.error(setting.class, "reset", e.getMessage(), e);
        
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
        OpLog.error(setting.class, "get", e.getMessage(), e);

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
        OpLog.error(setting.class, "set", e.getMessage(), e);

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

      String level = this.getString("level");
      Global.setConfig("run.level", level);
      Helper.DEBUG = X.isSame(level, "debug");

      Global.setConfig("user.system", this.getString("user_system"));
      Global.setConfig("user.role", this.getString("user_role"));

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

      this.set("node", Global.s("node", null));
      this.set("system_code", Global.l("system.code", 0));
      this.set("language", Global.s("language", "zh_cn"));
      this.set("level", Global.s("run.level", "debug"));
      this.set("user_system", Global.s("user.system", "close"));
      this.set("user_role", Global.s("user.role", "N/A"));

      this.set("cache_url", Global.s("cache.url", null));
      this.set("cache_group", Global.s("cache.group", "demo"));
      this.set("mongo_url", Global.s("mongo[prod].url", null));
      this.set("mongo_db", Global.s("mongo[prod].db", null));
      this.set("mongo_user", Global.s("mongo[prod].user", null));
      this.set("db_url", Global.s("db.url", null));

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
      this.set("protocol", Global.s("mail.protocol", "smtp"));
      this.set("host", Global.s("mail.host", X.EMPTY));
      this.set("email", Global.s("mail.email", X.EMPTY));
      this.set("title", Global.s("mail.title", X.EMPTY));
      this.set("user", Global.s("mail.user", X.EMPTY));
      this.set("passwd", Global.s("mail.passwd", X.EMPTY));

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
