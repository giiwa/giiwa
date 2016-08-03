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
package org.giiwa.app.web.admin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.giiwa.core.base.Shell;
import org.giiwa.core.bean.MongoHelper;
import org.giiwa.core.bean.RDSHelper;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.core.task.Task;
import org.giiwa.framework.web.Language;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Module;
import org.giiwa.framework.web.Path;

import net.sf.json.JSONObject;

/**
 * backup management
 * 
 * @author joe
 *
 */
public class backup extends Model {

  /*
   * (non-Javadoc)
   * 
   * @see org.giiwa.framework.web.Model#onGet()
   */
  @Path(login = true, access = "access.config.admin")
  @Override
  public void onGet() {
    String path = BackupTask.path();
    File root = new File(path);
    File[] fs = root.listFiles();
    List<File> list = new ArrayList<File>();
    if (fs != null) {
      for (File f : fs) {
        if (f.isDirectory()) {
          list.add(f);
        }
      }
    }

    this.set("list", list);
    this.show("/admin/backup.index.html");

  }

  /**
   * Delete.
   */
  @Path(path = "delete", login = true, access = "access.config.admin")
  public void delete() {

    try {
      String name = this.getString("name");
      String root = BackupTask.path();
      File f = new File(root + "/" + name);

      if (f.getCanonicalPath().startsWith(root)) {
        log.debug("delete: " + f.getCanonicalPath());
        this.delete(f);
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

  }

  /**
   * Now.
   */
  @Path(path = "now", login = true, access = "access.config.admin")
  public void now() {

    new BackupTask().schedule(10);

    this.set(X.MESSAGE, "开始备份，请稍后查询");
    onGet();

  }

  /**
   * Restore.
   */
  @Path(path = "restore", login = true, access = "access.config.admin")
  public void restore() {

    JSONObject jo = new JSONObject();

    String name = this.getString("name");
    task = new RecoverTask();
    task.name = name;
    task.schedule(10);

    this.response(jo);
  }

  /**
   * Restoring.
   */
  @Path(path = "restoring", login = true, access = "access.config.admin")
  public void restoring() {

    JSONObject jo = new JSONObject();

    if (task == null) {
      jo.put(X.STATE, 202);
      jo.put(X.MESSAGE, "没有启动!");
    } else if (task.done) {
      jo.put(X.STATE, 200);
      jo.put(X.MESSAGE, "已经恢复：" + task.name);
    } else {
      jo.put(X.STATE, 201);
      jo.put(X.MESSAGE, "正在恢复：" + task.name);
    }

    this.response(jo);
  }

  /**
   * backup.
   * 
   * @author joe
   *
   */
  public static class BackupTask extends Task {

    /**
     * Path.
     *
     * @return the string
     */
    public static String path() {
      return Global.s("backup.path", "/opt/backup");
    }

    @Override
    public String getName() {
      return "backup.task";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.giiwa.core.task.Task#onExecute()
     */
    @Override
    public void onExecute() {
      Module m = Module.home;
      try {

        String out = path() + "/" + Language.getLanguage().format(System.currentTimeMillis(), "yyyyMMddHHmm");
        new File(out).mkdirs();

        /**
         * 1, backup db
         */
        if (MongoHelper.isConfigured()) {
          MongoHelper.backup(out + "/mongo" + ".dmp");
        }
        if (RDSHelper.isConfigured()) {
          RDSHelper.backup(out + "/rds" + ".dmp");
        }

        /**
         * 2, backup repo
         */
        File f = m.getFile("/admin/clone/backup_tar.sh");
        String url = conf.getString("repo.path", null);
        if (!X.isEmpty(url)) {
          Shell.run("chmod ugo+x " + f.getCanonicalPath());
          Shell.run(f.getCanonicalPath() + " " + out + "/repo.tar.gz " + url);
        }

      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
    }

  };

  private static RecoverTask task = null;

  static class RecoverTask extends Task {

    String  name;
    boolean done;
    String  message;

    @Override
    public String getName() {
      return "recover.task";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.giiwa.core.task.Task#onExecute()
     */
    @Override
    public void onExecute() {
      String root = BackupTask.path();

      Module m = Module.home;
      try {

        String source = root + "/" + name;

        /**
         * 1, recover if configured
         */
        File[] fs = new File(source).listFiles();
        if (fs != null) {
          for (File f1 : fs) {
            if (f1.isFile()) {
              if (RDSHelper.isConfigured() && X.isSame("rds.dmp", f1.getName())) {
                RDSHelper.recover(f1.getCanonicalFile());
              }
              if (MongoHelper.isConfigured() && X.isSame("mongo.dmp", f1.getName())) {
                MongoHelper.recover(f1.getCanonicalFile());
              }
            }
          }
        }

        /**
         * 2, recover repo
         */
        File f = m.getFile("/admin/clone/recover_tar.sh");
        String url = conf.getString("repo.path", null);
        if (!X.isEmpty(url)) {
          Shell.run("chmod ugo+x " + f.getCanonicalPath());
          Shell.run(f.getCanonicalPath() + " " + source + "/repo.tar.gz " + url);
        }

      } catch (Exception e) {
        log.error(e.getMessage(), e);
        message = e.getMessage();
      }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.giiwa.core.task.Task#onFinish()
     */
    @Override
    public void onFinish() {
      done = true;
    }

  }
}
