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
package org.giiwa.framework.web;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.zip.*;

import org.apache.commons.configuration.*;
import org.apache.commons.logging.*;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.giiwa.app.web.DefaultListener;
import org.giiwa.core.base.FileUtil;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.framework.bean.Access;
import org.giiwa.framework.bean.Jar;
import org.giiwa.framework.bean.Menu;
import org.giiwa.framework.bean.Repo;
import org.giiwa.framework.bean.Repo.Entity;
import org.giiwa.framework.bean.User;
import org.giiwa.framework.web.Model.PathMapping;

/**
 * module includes: a module.xml, a group of model/view/images/css/js/language,
 * etc. and it will handle the request, if not found the handler in the module,
 * then will let the parent to handle it
 * 
 * the module organized as a chain, the home module will handler first, if found
 * then stop, else parent or parent's parent
 * 
 * this purpose of module is reuse the module with only changing of
 * configuration file
 * 
 * @author yjiang
 * 
 */
public class Module {

  static Log          log      = LogFactory.getLog(Module.class);

  /**
   * the absolute path of the module
   */
  String              path;

  String              viewroot;
  /**
   * the id of the module, MUST unique, and also is a sequence of the loading:
   * biggest first
   */
  public int          id;

  /**
   * the name of the module, the name of module should be unique in whole
   * context
   */
  String              name;

  /**
   * IListener which will be invoke in each life cycle of the module
   */
  String              listener;

  boolean             enabled  = false;

  String              version;
  String              build;

  String              screenshot;

  /**
   * the root package name of the module, which will use to mapping the handler
   */
  String              pack;

  Map<String, String> settings = new TreeMap<String, String>();
  Map<String, Object> filters  = new TreeMap<String, Object>();

  /**
   * readme file maybe html
   */
  String              readme;

  /**
   * handle by filter ad invoke the before
   * 
   * @param m
   *          the model
   * @return boolean
   */
  public boolean before(Model m) {
    String uri = m.getURI();

    for (String name : filters.keySet()) {
      if (uri.matches(name)) {
        Object o = filters.get(name);
        try {
          IFilter f = null;

          if (o instanceof IFilter) {
            f = (IFilter) o;
          } else {
            f = (IFilter) (Class.forName((String) o).newInstance());
            filters.put(name, f);
          }
          if (!f.before(m)) {
            return false;
          }
        } catch (Exception e) {
          log.error(e.getMessage(), e);
          filters.remove(name);
        }
      }
    }
    Module m1 = floor();
    if (m1 != null) {
      return m1.before(m);
    }
    return true;
  }

  /**
   * install a module by Entity
   * 
   * @param e
   *          the entity of module file
   * @return true required restart, false not need
   * @throws Exception
   *           throw Exception if failed
   */
  public static boolean install(Entity e) throws Exception {
    if (e == null) {
      throw new Exception("invalid repo entity");
    }
    String url = e.getUrl();

    String temp = Language.getLanguage().format(System.currentTimeMillis(), "yyyyMMdd");
    String root = Model.HOME + "/modules/" + temp + "/";

    try {
      ZipInputStream in = new ZipInputStream(e.getInputStream());

      /**
       * store all entry in temp file
       */

      ZipEntry z = in.getNextEntry();
      byte[] bb = new byte[4 * 1024];
      while (z != null) {
        File f = new File(root + z.getName());

        // log.info("name:" + z.getName() + ", " +
        // f.getAbsolutePath());
        if (z.isDirectory()) {
          f.mkdirs();
        } else {
          if (!f.exists()) {
            f.getParentFile().mkdirs();
          }

          FileOutputStream out = new FileOutputStream(f);
          int len = in.read(bb);
          while (len > 0) {
            out.write(bb, 0, len);
            len = in.read(bb);
          }

          out.close();
        }

        z = in.getNextEntry();
      }

      Module m = Module.load(temp);
      File f = new File(root);
      File dest = new File(Model.HOME + File.separator + "modules" + File.separator + m.getName());
      if (dest.exists()) {
        delete(dest);
      }

      Module m1 = Module.load(m.getName());
      if (m1 != null) {
        String repo = m1.getRepo();
        if (!X.isEmpty(repo)) {
          log.debug("old.repo=" + repo + ", new.repo=" + url);
          Entity e1 = Repo.load(repo);
          if (e1 != null && !X.isSame(e1.getId(), e.getId())) {
            // not the same file
            e1.delete();
          }
        }
      }

      /**
       * merge WEB-INF and depends lib
       * 
       */
      boolean restart = m.merge();

      /**
       * move the temp to target dest
       */
      f.renameTo(dest);

      Module.init(m);
      m.set(m.getName() + "_repo", url);
      m.store();
      return restart;

    } finally {

      // remove root
      delete(new File(root));

    }

  }

  /**
   * handle by filter and invoke the after
   * 
   * @param m
   *          the model
   * @return boolean
   */
  public boolean after(Model m) {
    for (String name : filters.keySet()) {
      if (m.getURI().matches(name)) {

        Object o = filters.get(name);
        try {
          IFilter f = null;

          if (o instanceof IFilter) {
            f = (IFilter) o;
          } else {
            f = (IFilter) (Class.forName((String) o).newInstance());
            filters.put(name, f);
          }
          if (!f.after(m)) {
            return false;
          }
        } catch (Exception e) {
          log.error(e.getMessage(), e);
          filters.remove(name);
        }

      }
    }

    Module m1 = floor();
    if (m1 != null) {
      return m1.after(m);
    }

    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return new StringBuilder("Module [").append(id).append(",").append(name).append(",enabled=").append(enabled)
        .append("]").toString();
  }

  private String locale = null;

  /**
   * parent module defined in module.xml.
   * 
   * @return String of the language
   */
  public String getLanguage() {
    if (locale == null) {
      locale = Global.getString("language", "en_us");
    }
    return locale;
  }

  /**
   * module class loader
   */
  public static GClassLoader              classLoader;

  /**
   * the default home module
   */
  public static Module                    home;

  private String                          status;
  private String                          error;

  /**
   * cache all modules by name
   */
  private static TreeMap<Integer, Module> modules  = new TreeMap<Integer, Module>();

  /**
   * cache the model in the module, the modelMap structure: {"method|uri",
   * "class"}
   */
  private static Map<String, CachedModel> modelMap = new HashMap<String, CachedModel>();

  /**
   * configuration
   */
  public static Configuration             _conf;

  /**
   * Reset.
   */
  public static void reset() {
    modelMap.clear();
  }

  /**
   * get the module by name.
   *
   * @param module
   *          the module
   * @return Module
   */
  public Module module(String module) {
    for (Module m : modules.values()) {
      if (m.name.equals(module)) {
        return m;
      }
    }
    return null;
  }

  /**
   * Clean.
   */
  public static void clean() {
    home = null;
    modules.clear();
  }

  /**
   * Store.
   */
  public void store() {
    File f = new File(Model.HOME + File.separator + "modules" + File.separator + name + File.separator + "module.xml");
    try {

      Document doc = DocumentHelper.createDocument();
      Element root = doc.addElement("module");
      Element e = root.addElement("id");
      e.setText(Integer.toString(this.id));

      e = root.addElement("name");
      e.setText(this.name);

      if (!X.isEmpty(this.pack)) {
        e = root.addElement("package");
        e.setText(this.pack);
      }

      if (!X.isEmpty(this.screenshot)) {
        e = root.addElement("screenshot");
        e.setText(this.screenshot);
      }
      e = root.addElement("version");
      e.setText(this.version);
      e = root.addElement("build");
      e.setText(this.build);
      e = root.addElement("enabled");
      e.setText(Boolean.toString(this.enabled).toLowerCase());

      if (!X.isEmpty(this.readme)) {
        e = root.addElement("readme");
        e.setText(this.readme);
      }

      if (!X.isEmpty(this.listener)) {
        e = root.addElement("listener");
        e = e.addElement("class");
        e.setText(this.listener);
      }

      e = root.addElement("setting");
      for (String name : settings.keySet()) {
        Element e1 = e.addElement("param");
        e1.addAttribute("name", name);
        e1.addAttribute("value", settings.get(name));
      }
      for (String name : filters.keySet()) {
        e = root.addElement("filter");
        Element e1 = e.addElement("pattern");
        e1.setText(name);

        e1 = e.addElement("class");
        Object o = filters.get(name);
        if (o instanceof IFilter) {
          IFilter f1 = (IFilter) o;
          e1.setText(f1.getClass().getName());
        } else {
          e1.setText((String) o);
        }
      }

      OutputFormat format = OutputFormat.createPrettyPrint();
      format.setEncoding("UTF-8");
      XMLWriter writer = new XMLWriter(new FileOutputStream(f), format);
      writer.write(doc);
      writer.close();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Gets the.
   * 
   * @param name
   *          the name
   * @return the string
   */
  public String get(String name) {
    return get(name, null);
  }

  public String getRepo() {
    return get(this.name + "_repo");
  }

  /**
   * Gets the int.
   * 
   * @param setting
   *          the setting
   * @return the int
   */
  public int getInt(String setting) {
    return X.toInt(get(setting), 0);
  }

  /**
   * Gets the long.
   * 
   * @param setting
   *          the setting
   * @return the long
   */
  public long getLong(String setting) {
    return X.toLong(get(setting), 0);
  }

  /**
   * Checks if is enabled.
   * 
   * @param setting
   *          the setting
   * @return true, if is enabled
   */
  public boolean isEnabled(String setting) {
    return "true".equalsIgnoreCase(get(setting, "false"));
  }

  /**
   * Gets the.
   * 
   * @param name
   *          the name
   * @param defaultValue
   *          the default value
   * @return the string
   */
  public String get(String name, String defaultValue) {
    String s = name;
    if (!s.startsWith("setting.")) {
      s = "setting." + name;
    }

    if (settings.containsKey(s)) {
      return settings.get(s);
    } else {
      Module m = this.floor();
      if (m != null) {
        return m.get(s, defaultValue);
      }

      // set(s, defaultValue);
      //
      return Global.getString(name, defaultValue);
    }
  }

  /**
   * Sets the.
   * 
   * @param name
   *          the name
   * @param value
   *          the value
   */
  public void set(String name, String value) {
    if (name.startsWith("setting.")) {
      settings.put(name, value);
    } else {
      settings.put("setting." + name, value);
    }

  }

  /**
   * loading all jar files in /model
   */
  private void initModels() {
    File root = new File(path + "/model");
    // log.debug("looking for: " + root.getAbsolutePath());

    File[] list = root.listFiles();
    if (list != null) {
      for (File f : list) {
        if (f.getName().endsWith(".jar")) {
          classLoader.addJar(f.getAbsolutePath());
          log.debug("loading: " + f.getAbsolutePath());
        }
      }
    }

    // if (parent != null) {
    // parent.initModels();
    // }
  }

  /**
   * Gets the file.
   * 
   * @param resource
   *          the resource
   * @return the file
   */
  public File getFile(String resource) {
    return getFile(resource, true);
  }

  /**
   * Gets the file.
   * 
   * @param resource
   *          the resource
   * @param inFloor
   *          the in floor
   * @return the file
   */
  public File getFile(String resource, boolean inFloor) {
    return getFile(resource, inFloor, true);
  }

  /**
   * get the file in the module box
   * 
   * @param resource
   *          the resource name
   * @param inFloor
   *          search the resource in floor
   * @param inbox
   *          get the resource in sanbox
   * @return the File if exists, otherwise null
   */
  public File getFile(String resource, boolean inFloor, boolean inbox) {
    try {
      File f = new File(viewroot + File.separator + resource);
      if (f.exists()) {
        /**
         * test the file is still in the path? if not, then do not allow to
         * access, avoid user using "../../" to access system file
         */
        if (inbox && f.getCanonicalPath().startsWith(viewroot)) {
          return f;
        } else if (f.exists()) {
          return f;
        }
      }

      if (inFloor) {
        Module e = floor();
        if (e != null) {
          return e.getFile(resource);
        }
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    return null;
  }

  public String[] getSupportLocale() {
    File f = new File(path + "/i18n/");
    if (f.exists()) {
      return f.list();
    }

    return null;
  }

  /**
   * Gets the all langs.
   * 
   * @param locale
   *          the locale
   * @return the all langs
   */
  public File[] getAllLangs(String locale) {
    File f = new File(path + "/i18n/" + locale);
    if (f.exists()) {
      return f.listFiles();
    }

    return null;
  }

  /**
   * Gets the all langs.
   * 
   * @param locale
   *          the locale
   * @param lang
   *          the lang
   * @return the all langs
   */
  public String getAllLangs(String locale, String lang) {
    File f = new File(path + "/i18n/" + locale + "/" + lang);
    if (f.exists()) {
      BufferedReader reader = null;
      try {
        reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "utf-8"));
        StringBuilder sb = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
          sb.append(line).append("\r\n");
          line = reader.readLine();
        }
        return sb.toString();
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      } finally {
        if (reader != null) {
          try {
            reader.close();
          } catch (IOException e) {
            log.error(e);
          }
        }
      }
    }

    return null;
  }

  public String getPath() {
    return path;
  }

  public int getId() {
    return id;
  }

  public String getListener() {
    return listener;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public String getScreenshot() {
    return screenshot;
  }

  public String getVersion() {
    return version;
  }

  public String getError() {
    return error;
  }

  public String getBuild() {
    return build;
  }

  public String getPack() {
    return pack;
  }

  public String getReadme() {
    return readme;
  }

  /**
   * Inits the.
   * 
   * @param conf
   *          the conf
   */
  public static void init(Configuration conf) {
    try {
      _conf = conf;
      classLoader = new GClassLoader(Module.class.getClassLoader());

      Thread thread = Thread.currentThread();
      thread.setContextClassLoader(classLoader);

      // String t = SystemConfig.s("home.module", "default");
      File f = new File(Model.HOME + "/modules/");

      if (f.exists()) {
        File[] list = f.listFiles();
        if (list != null) {
          for (File f1 : list) {
            if (f1.isDirectory()) {
              Module m = load(f1.getName());

              // TODO
              if (m == null) {
                /**
                 * the module is invalid
                 */
                log.info("[" + f1.getName() + "] is not a valid module");

              } else if (!m.enabled) {
                /**
                 * the module was disabled
                 */
                log.info("[" + f1.getName() + "] is disabled");

              } else if (modules.containsKey(m.id)) {
                /**
                 * the module was duplicated, ignore this
                 */
                log.error("the [id] duplicated, [" + m.name + ", " + modules.get(m.id).name + "], ignore the [" + m.name
                    + "]");
                m.error = Language.getLanguage().get("module.error.duplicatedid");

              } else if (!X.isSame(m.name, f1.getName())) {
                /**
                 * the module name was invalid
                 */
                log.error("the [name] is invlaid, folder=" + f1.getName() + ", module=" + m.name);
                m.error = Language.getLanguage().get("module.error.name");

              } else {
                /**
                 * cache the module
                 */
                modules.put(m.id, m);
              }
            }
          }
        }
      } else {
        log.error("giiwa modules missed, please re-install it, modules=" + f.getCanonicalPath());
      }

      if (modules.size() > 0) {
        home = modules.lastEntry().getValue();
      }

      log.debug("modules=" + modules);

      Menu.reset();

      for (Module m : modules.values()) {
        /**
         * loading the models
         */
        m.initModels();

        /**
         * initialize the life listener
         */
        m._init(_conf);

      }
      Menu.cleanup();

      // the the default locale
      String locale = null;
      Module f1 = home;
      while (locale == null && f1 != null) {
        locale = f1.get("default.locale");
        f1 = f1.floor();
      }
      if (locale != null) {
        Locale.setDefault(new Locale(locale));
      }

      /**
       * check is there any jar file not used by any module
       */
      boolean changed = false;

      List<Object> names = Jar.loadAll(W.create());
      if (names != null && names.size() > 0) {
        for (Object name : names) {
          List<Object> modules = Jar.load(name.toString());
          boolean used = false;
          if (modules != null && modules.size() > 0) {
            for (Object m1 : modules) {
              Module m2 = Module.load(m1.toString());
              if (m2 != null) {
                used = true;
              }
            }
          }
          if (!used) {
            File f2 = new File(
                Model.HOME + File.separator + "WEB-INF" + File.separator + "lib" + File.separator + name);
            if (f2.exists()) {
              log.info("as not module used the jar, delete it, [" + name + "]");
              f2.delete();

              changed = true;
            }
            Jar.remove(name.toString());
          }
        }
      }

      if (changed) {
        log.warn("jar files changed, restarting again...");
        System.exit(0);
      }

    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

  }

  /**
   * load the module by id
   * 
   * @param id
   * @return the Module
   */
  public static Module load(int id) {
    return modules.get(id);
  }

  /**
   * Load.
   * 
   * @param name
   *          the name
   * @return the module
   */
  public static Module load(String name) {
    try {
      for (Module m : modules.values()) {
        if (name.equals(m.name)) {
          return m;
        }
      }

      Module t = new Module();
      File f = new File(Model.HOME + "/modules/" + name + "/module.xml");
      if (f.exists()) {
        /**
         * initialize the module
         */
        SAXReader reader = new SAXReader();
        Document document = reader.read(f);
        Element root = document.getRootElement();
        t._load(root);

        t.path = f.getParent();
        t.viewroot = new File(t.path + File.separator + "view").getCanonicalPath();

        return t;
      } else {
        log.warn("the module.xml doesnt exists, file=" + f.getCanonicalPath());
      }

    } catch (Exception e) {
      log.error("loadModule:" + name, e);
    }

    return null;
  }

  @SuppressWarnings("unchecked")
  private boolean _load(Element root) {
    try {
      // TODO Auto-generated method stub
      List<Element> list = root.elements();
      for (Element e1 : list) {
        String tag = e1.getName();
        if (X.isSame(tag, "id")) {
          id = X.toInt(e1.getText(), Integer.MAX_VALUE);
        } else if (X.isSame(tag, "name")) {
          name = e1.getText();
        } else if (X.isSame(tag, "package")) {
          pack = e1.getText();
        } else if (X.isSame(tag, "screenshot")) {
          screenshot = e1.getText();
        } else if (X.isSame(tag, "version")) {
          version = e1.getText();
        } else if (X.isSame(tag, "build")) {
          build = e1.getText();
        } else if (X.isSame(tag, "enabled")) {
          enabled = X.isSame("true", e1.getText().toLowerCase());
        } else if (X.isSame(tag, "readme")) {
          readme = e1.getText();
        } else if (X.isSame(tag, "listener")) {
          List<Element> l2 = e1.elements();
          for (Element e2 : l2) {
            String tag2 = e2.getName();
            if (X.isSame(tag2, "class")) {
              listener = e2.getText();
            }
          }
        } else if (X.isSame(tag, "setting")) {
          List<Element> l2 = e1.elements();
          for (Element e2 : l2) {
            String tag2 = e2.getName();
            if (X.isSame(tag2, "param")) {
              String name = e2.attributeValue("name");
              String value = e2.attributeValue("value");
              settings.put(name, value);
            }
          }
        } else if (X.isSame(tag, "filter")) {
          List<Element> l2 = e1.elements();
          String pattern = null;
          String clazz = null;
          for (Element e2 : l2) {
            String tag2 = e2.getName();
            if (X.isSame(tag2, "pattern")) {
              pattern = e2.getText();
            } else if (X.isSame(tag2, "class")) {
              clazz = e2.getText();
            }
          }
          if (!X.isEmpty(pattern) && !X.isEmpty(clazz)) {
            filters.put(pattern, clazz);
          }
        }
      }

      return true;
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    return false;
  }

  /**
   * invoke the life listener of the module
   * 
   * @param conf
   */
  private void _init(Configuration conf) {
    // find the lifelistener, and init
    try {
      if (this.id > 0) {
        /**
         * force: using default lifelistener to initialize the install script
         */
        DefaultListener d = new DefaultListener();
        d.upgrade(conf, this);
      }

      /**
       * loading the module's lifelistener, to initialize the install script
       */
      if (!X.isEmpty(listener)) {
        String name = listener;
        if (name != null) {

          try {
            Class<?> c = Class.forName(name, true, classLoader);
            Object o = c.newInstance();

            if (o instanceof IListener) {

              log.info("initializing: " + name);
              IListener l = (IListener) o;

              l.upgrade(conf, this);

              l.onStart(conf, this);
            }
          } catch (Throwable e) {
            log.error(this.name + ", listener=" + name, e);
          }
        }
      }
    } catch (Throwable e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * get all modules
   * 
   * @return List
   */
  public static List<Module> getAll() {
    String home = Model.HOME;
    File troot = new File(home + "/modules/");
    File[] files = troot.listFiles();

    List<Module> list = new ArrayList<Module>();

    if (files != null) {
      for (File f : files) {
        if (f.isDirectory()) {
          Module m = load(f.getName());
          if (m != null && !m.enabled) {
            list.add(m);
          }
        }
      }
    }

    return list;
  }

  /**
   * Load model from cache.
   *
   * @param method
   *          the method
   * @param uri
   *          the uri
   * @return Model
   */
  public Model loadModelFromCache(int method, String uri) {
    try {
      // log.debug("looking for model for <" + method + "|" + uri + ">,
      // mapping=" + modelMap);

      CachedModel c = modelMap.get(method + "|" + uri);

      if (c != null) {
        Model m = c.create(uri);

        return m;
      }

      c = modelMap.get(method + "|" + uri + "/" + X.NONE);
      if (c != null) {
        Model m = c.create(uri);

        return m;
      }

    } catch (Exception e) {
      // e.printStackTrace();
    }

    Module e = floor();
    if (e != null && e.getId() != this.id) {
      return e.loadModelFromCache(method, uri);
    }

    return null;
  }

  /**
   * Load model.
   *
   * @param method
   *          the method
   * @param uri
   *          the uri
   * @return the model
   */
  @SuppressWarnings("unchecked")
  public Model getModel(int method, String uri) {

    try {

      // log.debug("looking for model for <" + method + "|" + uri + ">");

      CachedModel c = null;
      synchronized (modelMap) {
        c = modelMap.get(method + "|" + uri);
        // log.debug("uri=" + (method + "|" + uri));
        if (c == null) {
          /**
           * looking for the model class
           */
          String name = (pack + "." + uri).replace("/", ".").replace("..", ".");

          Class<Model> c1 = (Class<Model>) Class.forName(name, false, classLoader);

          /**
           * cache it and cache all the path
           */
          Map<Integer, Map<String, Model.PathMapping>> path = _loadPath(c1);
          if (path != null && path.size() > 0) {
            String u = uri;
            // if (!u.endsWith("/")) {
            // u += "/";
            // }
            for (int m1 : path.keySet()) {
              Map<String, Model.PathMapping> p = path.get(m1);
              for (String s : p.keySet()) {
                c = CachedModel.create(c1, path, this);
                _cache(m1 + "|" + u + "/" + s, c);
                // log.debug("uri=" + (m1 + "|" + u + "/" + s));
              }
              // c = CachedModel.create(c1, path, this);
              // _cache(m1 + "|" + u, c);
              // log.debug("uri=" + (m1 + "|" + u));

            }
          } else {
            c = CachedModel.create(c1, path, this);
            _cache(method + "|" + uri, c);

            // log.debug("uri=" + (method + "|" + uri));
          }
        }
      }

      if (c != null) {
        Model m = c.create(uri);

        return m;
      }
      // System.out.println("loading [" + name + "], c=" + c);

    } catch (Throwable e) {
      /**
       * not found, or is not a model, ignore the exception
       */

    }

    Module e = floor();
    if (e != null && e.getId() != this.id) {
      return e.getModel(method, uri);
    }

    return null;
  }

  private void _cache(String uri, CachedModel c) {
    CachedModel c1 = modelMap.get(uri);
    if (c1 != null) {
      if (c1.module.getId() > c.module.getId()) {
        // the cached uri is bigger module's, forget current
        return;
      }
    }

    modelMap.put(uri, c);
  }

  private Map<Integer, Map<String, Model.PathMapping>> _loadPath(Class<Model> c) {
    Method[] list = c.getMethods();
    if (list != null && list.length > 0) {

      Map<Integer, Map<String, Model.PathMapping>> map = new HashMap<Integer, Map<String, Model.PathMapping>>();
      for (Method m : list) {
        Path p = m.getAnnotation(Path.class);
        if (p != null) {
          /**
           * check the access and insert
           */
          String access = p.access();
          if (!X.isEmpty(access) && !X.NONE.equals(access)) {
            if (access.startsWith("access.")) {
              log.debug("access[" + access + "] at " + c.getCanonicalName() + "." + m.getName());

              Access.set(access);
            } else if (!X.isEmpty(access)) {
              log.error("access error! [" + access + "] at " + c.getCanonicalName() + "." + m.getName());
            }
          }

          int method = p.method();
          String path = p.path();

          Model.PathMapping oo = Model.PathMapping.create(Pattern.compile(path), p, m);

          /**
           * set the method mapping info
           */
          if ((method & Model.METHOD_GET) > 0) {
            Map<String, Model.PathMapping> mm = map.get(Model.METHOD_GET);
            if (mm == null) {
              mm = new HashMap<String, Model.PathMapping>();
              map.put(Model.METHOD_GET, mm);
            }
            mm.put(path, oo);
          }

          if ((method & Model.METHOD_POST) > 0) {
            Map<String, Model.PathMapping> mm = map.get(Model.METHOD_POST);
            if (mm == null) {
              mm = new HashMap<String, Model.PathMapping>();
              map.put(Model.METHOD_POST, mm);
            }
            mm.put(path, oo);
          }
        }
      }

      // if (map.size() > 0) {
      // pathmapping.put(c, map);
      // }
      return map;
    }

    return null;
  }

  /**
   * Load lang.
   * 
   * @param data
   *          the data
   * @param locale
   *          the locale
   */
  public void loadLang(Map<String, String> data, String locale) {

    Module e = floor();
    if (e != null) {
      e.loadLang(data, locale);
    }

    /**
     * read the file
     */
    File f = new File(path + "/i18n/" + locale + ".lang");

    // log.debug("loading:" + f.getAbsolutePath() + ", path=" + path +
    // ", locale:" + locale + ", lang:" + lang);

    if (f.exists()) {
      try {
        /**
         * read the language file using utf-8 encoding?
         */
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
        try {
          String line = reader.readLine();
          while (line != null) {
            if (!line.startsWith("#")) {
              int i = line.indexOf("=");
              if (i > 0) {
                String name = line.substring(0, i).trim();
                String value = line.substring(i + 1).trim();
                if ("@include".equals(name)) {
                  /**
                   * load the include value as lang file
                   */
                  loadLang(data, name);
                } else {
                  data.put(name, value);
                }
              }
            }

            line = reader.readLine();
          }
        } finally {
          reader.close();
        }
      } catch (Exception e1) {
        log.error("loadLang:" + locale, e1);
      }

    }
  }

  public String getName() {
    return name;
  }

  public static Module getHome() {
    return home;
  }

  /**
   * Put lang.
   * 
   * @param locale
   *          the locale
   * @param name
   *          the name
   */
  public void putLang(String locale, String name) {

    // log.error("not found", new Exception("not found [" + name + "] "));

    /**
     * read the file
     */
    File f = new File(path + "/i18n/" + locale + ".lang");

    Map<String, String> tmp = new TreeMap<String, String>();
    tmp.put(name, name);

    if (f.exists()) {
      BufferedReader reader = null;
      try {
        reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
        String line = reader.readLine();
        while (line != null) {
          line = line.trim();
          if (!line.startsWith("#")) {
            int i = line.indexOf("=");
            if (i > 0) {
              String s1 = line.substring(0, i);
              String s2 = line.substring(i + 1);
              tmp.put(s1, s2);
            }
          }

          line = reader.readLine();
        }

      } catch (Exception e) {
        log.error(f.getAbsolutePath(), e);
      } finally {
        if (reader != null) {
          try {
            reader.close();
          } catch (IOException e) {
            log.error(e);
          }
        }
      }
    } else {
      f.getParentFile().mkdirs();
    }

    PrintStream out = null;
    try {
      out = new PrintStream(f, "UTF-8");

      for (String key : tmp.keySet()) {
        out.println(key + "=" + tmp.get(key));
      }
      out.flush();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      try {
        out.close();
      } catch (Exception e) {
      }
    }
  }

  /**
   * Floor.
   * 
   * @return the module
   */
  public Module floor() {
    Entry<Integer, Module> e = modules.floorEntry(id - 1);
    if (e != null) {
      return e.getValue();
    }
    return null;
  }

  public void setEnabled(boolean b) {
    if (id == 0 && !b) {
      return;
    }

    enabled = b;

    if (b) {
      // install it
      init(this);

    } else {
      // uninstall it
      try {
        if (this.id > 0) {
          DefaultListener d = new DefaultListener();
          d.uninstall(_conf, this);
        }

        if (this.listener != null) {
          String name = listener;
          if (name != null) {
            Class<?> c = Class.forName(name, true, classLoader);
            Object o = c.newInstance();

            if (o instanceof IListener) {

              log.info("uninstall: " + name);
              IListener l = (IListener) o;

              l.onStop();
              l.uninstall(_conf, this);
            }
          }
        }
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      } finally {
        modules.remove(id);
      }
    }

    if (modules.size() > 0) {
      home = modules.lastEntry().getValue();
    }

    store();

  }

  public String getMenu() {
    /**
     * check the menus
     * 
     */
    File f = getFile("/install/menu.json", false);
    if (f != null && f.exists()) {
      BufferedReader reader = null;
      try {
        reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
          sb.append(line).append("\r\n");
          line = reader.readLine();
        }

        return sb.toString();

      } catch (Exception e) {
        log.error(e.getMessage(), e);
      } finally {
        if (reader != null) {
          try {
            reader.close();
          } catch (IOException e) {
            log.error(e);
          }
        }
      }
    } else {
      return "[{'name':'admin', 'childs':[]},\r\n{'name':'home', childs:[]},\r\n{'name':'user', childs:[]}]\r\n";
    }

    return null;
  }

  /**
   * Zip to.
   * 
   * @param file
   *          the file
   * @return the file
   */
  public File zipTo(String file) {
    /**
     * model, view, i18n, module.xml
     */
    try {
      File f = new File(file);
      f.getParentFile().mkdirs();
      ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f));

      for (String s : source) {
        addFile(out, s);
      }

      out.close();

      return f;
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    return null;
  }

  private int addFile(ZipOutputStream out, String filename) {

    File f = new File(path + filename);
    if (f.isFile()) {
      FileInputStream in = null;
      try {
        in = new FileInputStream(f);

        // Add ZIP entry to output stream.
        out.putNextEntry(new ZipEntry(filename));

        // Transfer bytes from the file to the ZIP file
        int len;
        byte[] buf = new byte[1024];
        while ((len = in.read(buf)) > 0) {
          out.write(buf, 0, len);
        }

        // Complete the entry
        out.closeEntry();

        return 1;
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      } finally {
        if (in != null) {
          try {
            in.close();
          } catch (IOException e) {
            log.error(e);
          }
        }
      }
    } else if (f.isDirectory()) {
      try {
        ZipEntry z = new ZipEntry(filename + "/");
        out.putNextEntry(z);

        String[] list = f.list();
        int i = 0;
        if (list != null) {
          for (String s : list) {
            i += addFile(out, filename + "/" + s);
          }
        }
        return i;
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
    }

    return 0;
  }

  public Set<String> getSettings() {
    return settings.keySet();
  }

  private static String[] source = new String[] { "/model", "/view", "/i18n", "/module.xml" };

  /**
   * Update lang.
   * 
   * @param locale
   *          the locale
   * @param langname
   *          the langname
   * @param text
   *          the text
   */
  public void updateLang(String locale, String langname, String text) {
    File f = new File(path + "/i18n/" + locale + "/" + langname);
    if (f.exists()) {
      PrintStream out = null;
      try {
        out = new PrintStream(new FileOutputStream(f));
        out.println(text);

        Language.clean();

      } catch (Exception e) {
        log.error(e.getMessage(), e);
      } finally {
        if (out != null) {
          out.close();
        }
      }
    }
  }

  /**
   * Support locale.
   * 
   * @param locale
   *          the locale
   * @return true, if successful
   */
  public boolean supportLocale(String locale) {
    return new File(path + "/i18n/" + locale + ".lang").exists();
  }

  /**
   * Inits the.
   * 
   * @param m
   *          the m
   * @return true, if successful
   */
  public static boolean init(Module m) {
    if (!m.enabled) {
      log.info("[" + m.getName() + "] is disabled");
    } else if (modules.containsKey(m.id)) {
      log.error("the [id] duplicated, [" + m.name + ", " + modules.get(m.id).name + "], ignore the [" + m.name + "]");
    } else {
      try {
        /**
         * possible the original has been moved to ..., always using the package
         * to initialize
         */
        m.path = new File(Model.HOME + "/modules/" + m.name).getCanonicalPath();
        m.viewroot = new File(m.path + File.separator + "view").getCanonicalPath();

        /**
         * loading the models
         */
        m.initModels();

        /**
         * initialize the life listener
         */
        m._init(_conf);

        /**
         * cache the module
         */
        modules.put(m.id, m);

        if (modules.size() > 0) {
          home = modules.lastEntry().getValue();
        }

        return true;
      } catch (Throwable e) {
        log.error(m.name, e);
      }
    }

    return false;

  }

  /**
   * Delete.
   */
  public void delete() {
    File f = new File(path);
    delete(f);

    modules.remove(id);
  }

  private static void delete(File f) {

    if (f.isFile()) {
      /**
       * delete the file
       */
      f.delete();
    } else if (f.isDirectory()) {
      File[] list = f.listFiles();
      if (list != null && list.length > 0) {
        /**
         * delete all file's or dir
         */
        for (File f1 : list) {
          delete(f1);
        }
      }

      /**
       * delete the empty dir
       */
      f.delete();
    }
  }

  /**
   * Merge.
   *
   * @return true, if successful
   */
  public boolean merge() {

    Jar.reset(this.getName());

    boolean changed = false;
    String webinf = this.path + File.separator + "WEB-INF";
    File f = new File(webinf);

    if (f.exists() && f.isDirectory()) {
      /**
       * copy all files to application, and remove original one
       */
      changed = move(f, Model.HOME);
    } else {
      log.debug("no files need merge!");
    }

    return changed;
  }

  private boolean move(File f, String dest) {

    // log.debug("moving ..." + f.getAbsolutePath());

    boolean r1 = false;
    if (f.isDirectory()) {
      File[] list = f.listFiles();
      if (list != null && list.length > 0) {
        for (File f1 : list) {
          if (move(f1, dest + File.separator + f.getName())) {
            r1 = true;
          }
        }
      }
    } else {
      /**
       * check the file version, and remove all the related version (may newer
       * or elder); <br>
       * looking for all the "f.getName()" in "classpath", and remove the same
       * package but different "version"<br>
       */
      {
        FileUtil f1 = new FileUtil(f);

        if (f.getName().endsWith(".jar")) {
          log.debug("checking [" + f1.getName() + "]");

          // check the version
          File m = new File(Model.HOME + File.separator + "WEB-INF" + File.separator + "lib");
          File[] list = m.listFiles();
          if (list != null) {
            for (File f2 : list) {
              if (f2.getName().endsWith(".jar")) {
                FileUtil.R r = f1.compareTo(f2);
                if (r != FileUtil.R.DIFF) {
                  /**
                   * load all the related modules for f2
                   */
                  List<Object> modules = Jar.load(f2.getName());
                  if (modules == null || modules.size() == 0) {
                    Jar.update("default", f1.getName());
                  } else {
                    for (Object m1 : modules) {
                      Jar.update(m1.toString(), f1.getName());
                      Jar.remove(m1.toString(), f2.getName());
                    }
                  }

                  log.warn("same jar file, but different varsion, remove [" + f2.getAbsolutePath() + "]");
                  f2.delete();
                  r1 = true;

                }
              }
            }
          } else {
            log.debug("no file in [" + m.getAbsolutePath() + "]");
          }

          // ------

          File d = new File(dest + File.separator + f.getName());
          if (d.exists()) {
            d.delete();

          } else {
            d.getParentFile().mkdirs();
          }

          Jar.update(this.getName(), d.getName());

          f.renameTo(d);
        } else {
          // files
          File d = new File(dest + File.separator + f.getName());
          if (d.exists()) {
            d.delete();

          } else {
            d.getParentFile().mkdirs();
          }

          f.renameTo(d);
        }
      }
    }

    f.delete();

    return r1;
  }

  /**
   * Load menu.
   * 
   * @param me
   *          the me
   * @param name
   *          the name
   * @return the list
   */
  public List<Menu> loadMenu(User me, String name) {
    return loadMenu(me, 0, name);
  }

  /**
   * Load menu.
   * 
   * @param me
   *          the me
   * @param id
   *          the id
   * @param name
   *          the name
   * @return the list
   */
  public List<Menu> loadMenu(User me, int id, String name) {
    Beans<Menu> bs = null;
    Menu m = null;
    if (name != null) {
      /**
       * load the menu by id and name
       */
      m = Menu.load(id, name);

      if (m != null) {

        /**
         * load the submenu of the menu
         */
        bs = m.submenu();
      }
    } else {
      /**
       * load the submenu by id
       */
      bs = Menu.submenu(id);

    }
    List<Menu> list = bs == null ? null : bs.getList();

    /**
     * filter out the item which no access
     */
    Menu.filterAccess(list, me);

    return list;
  }

  static class CachedModel {
    Class<Model>                                 model;
    Map<Integer, Map<String, Model.PathMapping>> pathmapping;
    Module                                       module;

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
      return "{" + module.getName() + "//" + model.getName() + "}";
    }

    /**
     * Creates the.
     *
     * @param model
     *          the model
     * @param pathmapping
     *          the pathmapping
     * @param module
     *          the module
     * @return the cached model
     */
    static CachedModel create(Class<Model> model, Map<Integer, Map<String, PathMapping>> pathmapping, Module module) {
      CachedModel m = new CachedModel();
      m.model = model;
      m.pathmapping = pathmapping;
      m.module = module;
      return m;
    }

    /**
     * Creates the.
     *
     * @param uri
     *          the uri
     * @return the model
     * @throws Exception
     *           the exception
     */
    public Model create(String uri) throws Exception {
      Model m = model.newInstance();
      m.module = module;
      m.pathmapping = pathmapping;
      m.path = getPath(uri);
      return m;
    }

    private String getPath(String uri) {
      String name = model.getName().substring(module.pack.length() + 1).replaceAll("\\.", "/");
      String path = uri.length() > name.length() ? uri.replaceAll(name, "") : X.EMPTY;
      while (path.startsWith("/")) {
        path = path.substring(1);
      }
      return path;
    }

  }

  public void setError(String error) {
    if (this.error == null) {
      this.error = error;
    } else {
      this.error += "<br>" + error;
    }
  }

  public void setStatus(String status) {
    if (this.status == null) {
      this.status = status;
    } else {
      this.status += "<br>" + status;
    }
  }

  public String getStatus() {
    return status;
  }

  /**
   * return the shortname of the class, cut the prefix by module package
   * 
   * @param model
   *          the subclass of model
   * @return the shortname of the subclass
   */
  public String shortName(Class<? extends Model> model) {
    if (model == null) {
      return X.EMPTY;
    }
    String name = model.getName();
    if (name.startsWith(this.pack)) {
      return name.substring(this.pack.length() + 1);
    }

    Module m1 = floor();
    if (m1 != null) {
      return m1.shortName(model);
    }
    return name;
  }
}
