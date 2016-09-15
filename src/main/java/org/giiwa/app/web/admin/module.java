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

import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.apache.commons.fileupload.FileItem;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;
import org.giiwa.core.base.IOUtil;
import org.giiwa.core.base.MD5;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.conf.Global;
import org.giiwa.core.json.JSON;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.*;
import org.giiwa.framework.bean.Repo.Entity;
import org.giiwa.framework.web.*;

/**
 * web api: /admin/module <br>
 * used to manage modules, <br>
 * required "access.config.admin" except "create a module"
 * 
 * @author joe
 *
 */
public class module extends Model {

  private static String ROOT = "/tmp/modules/";

  /**
   * create a new module.
   */
  @Path(path = "create", log = Model.METHOD_POST | Model.METHOD_GET)
  public void create() {
    if (method.isPost()) {
      /**
       * create
       */
      JSON jo = new JSON();

      try {
        String file = createmodule();
        jo.put(X.STATE, 200);
        jo.put("file", file);
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        OpLog.error(module.class, "create", e.getMessage(), e, login, this.getRemoteHost());

        jo.put(X.MESSAGE, e.getMessage());
        jo.put(X.STATE, 201);

      }
      this.response(jo);
    } else {
      this.set("id", Global.getInt("module.id.next", 100));
      this.show("/admin/module.create.html");
    }
  }

  /**
   * Createmodule.
   *
   * @return the string
   * @throws Exception
   *           the exception
   */
  @SuppressWarnings("deprecation")
  public String createmodule() throws Exception {
    int id = this.getInt("id");
    String name = this.getString("name");
    String package1 = this.getString("package");
    String lifelistener = package1 + "." + name.substring(0, 1).toUpperCase() + name.substring(1) + "Listener";
    String readme = this.getString("readme");
    boolean includes = X.isSame(this.getString("includes"), "on");

    String fid = UID.id(login == null ? UID.random() : login.getId(), System.currentTimeMillis());

    File f = Temp.get(fid, name + ".zip");
    if (f.exists()) {
      f.delete();
    } else {
      f.getParentFile().mkdirs();
    }

    ZipOutputStream out = null;

    try {
      out = new ZipOutputStream(new FileOutputStream(f));

      /**
       * create project info
       */
      create(out, name + "/").closeEntry();
      create(out, name + "/.project");
      File f1 = module.getFile("/admin/demo/.project");
      if (f1.exists()) {
        // copy to out
        copy(out, f1, new String[] { "webdemo", name });
      }

      create(out, name + "/depends/").closeEntry();
      f1 = new File(Model.HOME + "/WEB-INF/lib/");
      List<String> list = new ArrayList<String>();
      File[] ff1 = f1.listFiles();
      if (ff1 != null) {
        for (File f2 : ff1) {
          if (includes || Jar.exists(W.create("module", "default").and("name", f2.getName()))) {
            list.add(f2.getName());
            create(out, name + "/depends/" + f2.getName());
            copy(out, f2);
            out.closeEntry();
          }
        }
      }

      create(out, name + "/.classpath");
      f1 = module.getFile("/admin/demo/.classpath");
      copy(out, f1, list);
      out.closeEntry();

      create(out, name + "/build.xml");
      f1 = module.getFile("/admin/demo/build.xml");
      if (f1 != null) {
        copy(out, f1);
      }
      out.closeEntry();

      create(out, name + "/src/").closeEntry();
      create(out, name + "/src/module.xml");

      Document doc = DocumentHelper.createDocument();
      Element root = doc.addElement("module");
      root.addAttribute("version", "1.0");

      Element e = root.addElement("id");
      e.setText(Integer.toString(id));

      e = root.addElement("name");
      e.setText(name);

      e = root.addElement("package");
      e.setText(package1);

      e = root.addElement("screenshot");
      e.setText("/images/demo_screenshot.png");
      e = root.addElement("version");
      e.setText("1.0.1");
      e = root.addElement("build");
      e.setText("0");
      e = root.addElement("enabled");
      e.setText("true");
      e = root.addElement("readme");
      e.setText(readme);
      e = root.addElement("listener");
      e = e.addElement("class");
      e.setText(lifelistener);

      e = root.addElement("setting");
      e.addComment("TODO, remove it, please refer module.get(d1)");
      Element e1 = e.addElement("param");
      e1.setAttributeValue("name", "d1");
      e1.setAttributeValue("value", "1");

      e = root.addElement("filter");
      e.addComment("TODO, remove it, please refer web.IFilter");
      e1 = e.addElement("pattern");
      e1.setText("/user/login");
      e1 = e.addElement("class");
      e1.setText("org.giiwa.demo.web.UserFilter");

      e = root.addElement("depends");
      e.addComment("TODO, remove it");
      e1 = e.addElement("module");
      e1.addAttribute("name", "default");
      e1.addAttribute("version", Module.load("default").getVersion());
      e1 = e.addElement("jar");
      e1.addAttribute("url", "http://giiwa.org/aaa_1.1.jar");

      OutputFormat format = OutputFormat.createPrettyPrint();
      format.setEncoding("UTF-8");
      XMLWriter writer = new XMLWriter(out, format);
      writer.write(doc);

      out.closeEntry();
      // end of project info

      /**
       * create helpful folder
       */
      create(out, name + "/src/WEB-INF/").closeEntry();
      create(out, name + "/src/WEB-INF/lib/").closeEntry();

      /**
       * create i18n info
       */
      create(out, name + "/src/i18n/").closeEntry();
      f1 = module.getFile("/admin/demo/src/i18n/");
      if (f1.exists()) {
        ff1 = f1.listFiles();
        if (ff1 != null) {
          for (File f2 : ff1) {
            if (f2.isFile()) {
              create(out, name + "/src/i18n/" + f2.getName());
              copy(out, f2);
              out.closeEntry();
            }
          }
        }
      }
      // end of i18n

      /**
       * create resources info
       */
      create(out, name + "/src/init/").closeEntry();
      f1 = module.getFile("/admin/demo/src/init/");
      if (f1.exists()) {
        ff1 = f1.listFiles();
        if (ff1 != null) {
          for (File f2 : ff1) {
            if (f2.isFile()) {
              create(out, name + "/src/init/" + f2.getName());
              copy(out, f2);
              out.closeEntry();
            } else if (f2.isDirectory()) {
              create(out, name + "/src/init/" + f2.getName() + "/").closeEntry();
              // copy all files
              File[] ff2 = f2.listFiles();
              if (ff2 != null) {
                for (File f3 : ff2) {
                  if (f3.isDirectory()) {
                    create(out, name + "/src/init/" + f2.getName() + "/" + f3.getName() + "/").closeEntry();
                    File[] ff3 = f3.listFiles();
                    if (ff3 != null) {
                      for (File f4 : ff3) {
                        if (f4.isFile()) {
                          create(out, name + "/src/init/" + f2.getName() + "/" + f3.getName() + "/" + f4.getName());
                          copy(out, f4);
                          out.closeEntry();
                        }
                      }
                    }
                  } else {
                    create(out, name + "/src/init/" + f2.getName() + "/" + f3.getName());
                    copy(out, f3);
                    out.closeEntry();
                  }
                }
              }
            }
          }
        }
      }
      // end of resources

      /**
       * create model info
       */
      create(out, name + "/src/model/").closeEntry();
      create(out, name + "/src/model/java/").closeEntry();
      String[] ss = package1.split("\\.");
      String s1 = null;
      for (String s : ss) {
        if (s1 == null) {
          s1 = s;
        } else {
          s1 = s1 + "/" + s;
        }
        create(out, name + "/src/model/java/" + s1 + "/").closeEntry();
      }

      // create bean
      String p1 = package1.replaceAll("\\.", "/");
      create(out, name + "/src/model/java/" + p1.substring(0, p1.lastIndexOf("/")) + "/bean/").closeEntry();

      create(out, name + "/src/model/java/" + p1.substring(0, p1.lastIndexOf("/")) + "/bean/Demo.java");
      f1 = module.getFile("/admin/demo/src/model/bean/Demo.java");
      if (f1 != null) {
        copy(out, f1, new String[] { "org.giiwa.demo.bean",
            (p1.substring(0, p1.lastIndexOf("/")) + "/bean").replaceAll("/", ".") });
      }
      out.closeEntry();

      // copy demo model
      create(out, name + "/src/model/java/" + p1 + "/demo.java");
      f1 = module.getFile("/admin/demo/src/model/web/demo.java");
      if (f1 != null) {
        copy(out, f1, new String[] { "org.giiwa.demo.web", p1.replaceAll("/", ".") }, new String[] {
            "org.giiwa.demo.bean", (p1.substring(0, p1.lastIndexOf("/")) + "/bean").replaceAll("/", ".") });
      }
      out.closeEntry();

      if (!X.isEmpty(lifelistener)) {
        create(out, name + "/src/model/java/" + lifelistener.replaceAll("\\.", "/") + ".java");
        f1 = module.getFile("/admin/demo/src/model/web/DemoListener.java");
        if (f1 != null) {
          copy(out, f1, new String[] { "org.giiwa.demo.web", lifelistener.substring(0, lifelistener.lastIndexOf(".")) },
              new String[] { "DemoListener", lifelistener.substring(lifelistener.lastIndexOf(".") + 1) },
              new String[] { "webdemo", name });
        }
        out.closeEntry();
      }

      // copy admin/demo model
      create(out, name + "/src/model/java/" + p1 + "/admin/").closeEntry();
      create(out, name + "/src/model/java/" + p1 + "/admin/demo.java");
      f1 = module.getFile("/admin/demo/src/model/web/admin/demo.java");
      if (f1 != null) {
        copy(out, f1, new String[] { "org.giiwa.demo.web", p1.replaceAll("/", ".") }, new String[] {
            "org.giiwa.demo.bean", (p1.substring(0, p1.lastIndexOf("/")) + "/bean").replaceAll("/", ".") });
      }
      out.closeEntry();

      // end of model

      /**
       * create view info
       */
      create(out, name + "/src/view/").closeEntry();
      f1 = module.getFile("/admin/demo/src/view/");
      if (f1.exists()) {
        ff1 = f1.listFiles();
        if (ff1 != null) {
          for (File f2 : ff1) {
            if (f2.isFile()) {
              create(out, name + "/src/view/" + f2.getName());
              copy(out, f2);
              out.closeEntry();
            }
          }
        }
      }
      create(out, name + "/src/view/admin/").closeEntry();
      f1 = module.getFile("/admin/demo/src/view/admin");
      if (f1 != null) {
        ff1 = f1.listFiles();
        if (ff1 != null) {
          for (File f2 : ff1) {
            if (f2.isFile()) {
              create(out, name + "/src/view/admin/" + f2.getName());
              copy(out, f2);
              out.closeEntry();
            }
          }
        }
      }
      create(out, name + "/src/view/js/").closeEntry();
      f1 = module.getFile("/admin/demo/src/view/js");
      if (f1 != null) {
        ff1 = f1.listFiles();
        if (ff1 != null) {
          for (File f2 : ff1) {
            if (f2.isFile()) {
              create(out, name + "/src/view/js/" + f2.getName());
              copy(out, f2);
              out.closeEntry();
            }
          }
        }
      }
      create(out, name + "/src/view/css/").closeEntry();
      f1 = module.getFile("/admin/demo/src/view/css");
      if (f1 != null) {
        ff1 = f1.listFiles();
        if (ff1 != null) {
          for (File f2 : ff1) {
            if (f2.isFile()) {
              create(out, name + "/src/view/css/" + f2.getName());
              copy(out, f2);
              out.closeEntry();
            }
          }
        }
      }
      create(out, name + "/src/view/images/").closeEntry();
      f1 = module.getFile("/admin/demo/src/view/images");
      if (f1 != null) {
        ff1 = f1.listFiles();
        if (ff1 != null) {
          for (File f2 : ff1) {
            if (f2.isFile()) {
              create(out, name + "/src/view/images/" + f2.getName());
              copy(out, f2);
              out.closeEntry();
            }
          }
        }
      }

      f1 = module.getFile("/admin/demo/src/view/install");
      if (f1 != null) {
        create(out, name + "/src/view/install/").closeEntry();
        ff1 = f1.listFiles();
        if (ff1 != null) {
          for (File f2 : ff1) {
            if (f2.isFile()) {
              create(out, name + "/src/view/install/" + f2.getName());
              copy(out, f2);
              out.closeEntry();
            }
          }
        }
      }
      // end of view

      int id1 = Global.getInt("module.id.next", 100);
      if (id >= id1) {
        Global.setConfig("module.id.next", id + 1);
      }

    } finally {
      if (out != null) {
        out.close();
      }
    }

    return "/temp/" + fid + "/" + name + ".zip";
  }

  private ZipOutputStream copy(ZipOutputStream out, File f) throws Exception {
    InputStream in = null;
    try {
      in = new FileInputStream(f);
      IOUtil.copy(in, out, false);
    } finally {
      if (in != null) {
        in.close();
      }
    }

    return out;
  }

  private ZipOutputStream copy(ZipOutputStream out, File f, String[]... replacement) throws Exception {
    BufferedReader in = null;
    try {
      PrintStream o = new PrintStream(out);
      in = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
      String line = in.readLine();
      while (line != null) {
        if (replacement != null && replacement.length > 0) {
          for (String[] ss : replacement) {
            if (ss.length == 2) {
              if (X.isEmpty(ss[1])) {
                line = X.EMPTY;
              } else {
                line = line.replaceAll(ss[0], ss[1]);
              }
            }
          }
        }
        o.println(line);

        line = in.readLine();
      }
    } finally {
      if (in != null) {
        in.close();
      }
    }

    return out;
  }

  private ZipOutputStream copy(ZipOutputStream out, File f, List<String> classpath) throws Exception {
    BufferedReader in = null;
    try {
      PrintStream o = new PrintStream(out);
      in = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
      String line = in.readLine();
      while (line != null) {
        if (line.indexOf("webdemo.jar") > 0) {
          for (String s : classpath) {
            String s1 = line.replaceAll("webdemo.jar", s);
            o.println(s1);
          }
        } else {
          o.println(line);
        }
        line = in.readLine();
      }
    } finally {
      if (in != null) {
        in.close();
      }
    }

    return out;
  }

  private ZipOutputStream create(ZipOutputStream out, String filename) throws IOException {
    ZipEntry e = new ZipEntry(filename);
    out.putNextEntry(e);
    return out;
  }

  /**
   * Adds the.
   */
  @Path(path = "add", login = true, access = "access.config.admin", log = Model.METHOD_POST | Model.METHOD_GET)
  public void add() {

    String url = this.getString("url");
    Entity e = Repo.load(url);

    JSON jo = new JSON();

    try {
      boolean restart = Module.install(e);

      jo.put("result", "ok");

      if (restart) {
        jo.put(X.STATE, 201);
        jo.put("message", lang.get("restarting.giiwa"));
      } else {
        jo.put(X.STATE, 200);
        jo.put("message", lang.get("restart.required"));
      }

      if (restart) {
        new Task() {

          @Override
          public void onExecute() {

            log.info("WEB-INF has been merged, need to restart");
            System.exit(0);
          }

        }.schedule(2000);
      }

    } catch (Exception e1) {
      jo.put(X.STATE, 404);
      jo.put("result", "fail");
      jo.put("message", "entity not found in repo for [" + url + "]");

    }

    Module.reset();

    this.response(jo);

  }

  /**
   * Index.
   */
  @Path(login = true, access = "access.config.admin")
  public void onGet() {

    List<Module> actives = new ArrayList<Module>();
    Module m = Module.home;
    while (m != null) {
      actives.add(m);
      m = m.floor();
    }

    this.set("actives", actives);

    this.set("list", Module.getAll());

    this.query.path("/admin/module");

    this.show("/admin/module.index.html");

  }

  /**
   * Download.
   */
  @Path(path = "download", login = true, access = "access.config.admin")
  public void download() {
    String name = this.getString("name");

    /**
     * zip module
     */
    Module m = Module.load(name);
    String file = ROOT + name + ".zip";
    File f = m.zipTo(Model.HOME + file);
    if (f != null && f.exists()) {

      this.set("f", f);
      this.set("link", file);

      this.show("/admin/module.download.html");
      return;
    } else {
      this.set(X.MESSAGE, lang.get("message.fail"));
      onGet();
    }
  }

  /**
   * Disable.
   */
  @Path(path = "disable", login = true, access = "access.config.admin", log = Model.METHOD_POST | Model.METHOD_GET)
  public void disable() {
    String name = this.getString("name");

    Module m = Module.load(name);
    m.setEnabled(false);

    Module.reset();

    onGet();
  }

  /**
   * Update.
   */
  @Path(path = "update", login = true, access = "access.config.admin")
  public void update() {
    String name = this.getString("name");
    int id = this.getInt("id");

    JSON jo = new JSON();
    if (id > 0) {
      jo.put(X.STATE, 200);
      Module m = Module.load(name);
      m.id = id;
      m.store();
    } else {
      jo.put(X.STATE, 201);
      jo.put(X.MESSAGE, lang.get("module.id.gt0"));
    }

    this.response(jo);
  }

  @Path(path = "query")
  public void query() {
    String name = this.getString("name");

    JSON jo = new JSON();
    Module m = Module.load(name);
    if (m != null) {

      Entity e = Repo.load(m.getRepo());
      if (e != null) {
        try {
          InputStream in = e.getInputStream();
          String md5 = MD5.md5(in);
          if (md5 != null) {
            jo.put("md5", md5);
            jo.put(X.STATE, 200);
            jo.put("uri", m.getRepo());
            jo.put("name", name);
            jo.put("version", m.getVersion());
            jo.put("build", m.getBuild());
          } else {
            jo.put(X.STATE, 201);
            jo.put(X.MESSAGE, "get md5 failed for the repo=" + m.getRepo());
          }
        } catch (Exception e1) {
          jo.put(X.STATE, 201);
          jo.put(X.MESSAGE, e1.getMessage());

        }
      } else {
        jo.put(X.STATE, 201);
        jo.put(X.MESSAGE, "repo file was missed, id=" + m.getRepo());
      }
    } else {
      jo.put(X.STATE, 201);
      jo.put(X.MESSAGE, "not found, name=" + name);
    }
    this.response(jo);
  }

  /**
   * Enable.
   */
  @Path(path = "enable", login = true, access = "access.config.admin", log = Model.METHOD_POST | Model.METHOD_GET)
  public void enable() {
    String name = this.getString("name");

    Module m = Module.load(name);
    m.setEnabled(true);

    Module.reset();

    onGet();
  }

  /**
   * Delete.
   */
  @SuppressWarnings("deprecation")
  @Path(path = "delete", login = true, access = "access.config.admin", log = Model.METHOD_POST | Model.METHOD_GET)
  public void delete() {
    String name = this.getString("name");
    Module m = Module.load(name);
    String url = m.get("repo");
    if (!X.isEmpty(url)) {
      Entity e = Repo.loadByUri(url);
      if (e != null) {
        e.delete();
      }
    }
    m.delete();

    Module.reset();

    onGet();
  }

  @SuppressWarnings("unused")
  private boolean validate(FileItem file) {
    return false;
  }
}