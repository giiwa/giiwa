/*
 *   giiwa, a java web foramewrok.
 *   Copyright (C) <2014>  <giiwa.org>
 *
 */
package org.giiwa.app.web.admin;

import java.util.LinkedHashMap;
import java.util.Map;

import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.ConfigGlobal;
import org.giiwa.framework.bean.OpLog;
import org.giiwa.framework.sync.SyncTask;
import org.giiwa.framework.utils.Shell;
import org.giiwa.framework.web.*;

// TODO: Auto-generated Javadoc
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

  public static class sync extends setting {

    /*
     * (non-Javadoc)
     * 
     * @see org.giiwa.app.web.admin.setting#reset()
     */
    @Override
    public void reset() {
      for (String c : SyncTask.getCollections().keySet()) {
        ConfigGlobal.setConfig("sync." + c + ".lasttime", 0L);
      }
      super.reset();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.giiwa.app.web.admin.setting#set()
     */
    @Override
    public void set() {
      String url = this.getString("sync_url");
      boolean changed = false;
      if (url != null && !url.equals(ConfigGlobal.s("sync.url", X.EMPTY))) {
        // was changed, reset all synced flag
        changed = true;
      }
      ConfigGlobal.setConfig("sync.url", this.getString("sync_url"));
      ConfigGlobal.setConfig("sync.appkey", this.getString("sync_appkey"));
      ConfigGlobal.setConfig("sync.secret", this.getString("sync_secret"));

      for (String group : SyncTask.getGroups()) {
        String s = this.getString("sync_" + group);
        ConfigGlobal.setConfig("sync." + group, s);
        for (String c : SyncTask.instance.collections(group)) {
          ConfigGlobal.setConfig("sync." + c, s);
        }
      }

      if (changed) {
        for (String c : SyncTask.getCollections().keySet()) {
          ConfigGlobal.setConfig("sync." + c + ".lasttime", 0L);
        }
      }

      SyncTask.instance.schedule(1000);

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
      this.set("sync_url", ConfigGlobal.s("sync.url", null));
      this.set("sync_appkey", ConfigGlobal.s("sync.appkey", null));
      this.set("sync_secret", ConfigGlobal.s("sync.secret", null));

      // this.set("collections", SyncTask.getCollections().keySet());
      this.set("t", SyncTask.instance);
      this.set("groups", SyncTask.getGroups());

      this.set("page", "/admin/setting.sync.html");
    }

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
      ConfigGlobal.setConfig("language", lang1);

      String level = this.getString("level");
      ConfigGlobal.setConfig("level", level);
      Bean.DEBUG = X.isSame(level, "debug");

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

      this.set("node", ConfigGlobal.s("node", null));
      this.set("system_code", ConfigGlobal.l("system.code", 1));
      this.set("language", ConfigGlobal.s("language", "zh_cn"));
      this.set("level", ConfigGlobal.s("level", "debug"));

      this.set("cache_url", ConfigGlobal.s("cache.url", null));
      this.set("cache_group", ConfigGlobal.s("cache.group", "demo"));
      this.set("mongo_url", ConfigGlobal.s("mongo[prod].url", null));
      this.set("mongo_db", ConfigGlobal.s("mongo[prod].db", null));
      this.set("mongo_user", ConfigGlobal.s("mongo[prod].user", null));
      this.set("db_url", ConfigGlobal.s("db.url", null));

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
      ConfigGlobal.setConfig("mail.protocol", this.getString("protocol"));
      ConfigGlobal.setConfig("mail.host", this.getString("host"));
      ConfigGlobal.setConfig("mail.email", this.getString("email"));
      ConfigGlobal.setConfig("mail.title", this.getString("title"));
      ConfigGlobal.setConfig("mail.user", this.getString("user"));
      ConfigGlobal.setConfig("mail.passwd", this.getString("passwd"));

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
      this.set("protocol", ConfigGlobal.s("mail.protocol", "smtp"));
      this.set("host", ConfigGlobal.s("mail.host", X.EMPTY));
      this.set("email", ConfigGlobal.s("mail.email", X.EMPTY));
      this.set("title", ConfigGlobal.s("mail.title", X.EMPTY));
      this.set("user", ConfigGlobal.s("mail.user", X.EMPTY));
      this.set("passwd", ConfigGlobal.s("mail.passwd", X.EMPTY));

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
      ConfigGlobal.setConfig("site.counter", this.getHtml("counter"));

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
//      this.set("counter", ConfigGlobal.s("site.counter", X.EMPTY));
      this.set("page", "/admin/setting.counter.html");
    }

  }
}
