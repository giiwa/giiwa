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

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.commons.configuration.Configuration;
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Config;
import org.giiwa.core.db.DB;
import org.giiwa.core.json.JSON;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.OpLog;
import org.giiwa.framework.bean.User;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Module;
import org.giiwa.framework.web.Path;

import com.mongodb.Mongo;
import com.mongodb.MongoOptions;
import com.mongodb.ServerAddress;

/**
 * web api: /setup <br>
 * used to the initial configure, once configured, it will not be accessed
 * 
 * @author joe
 *
 */
public class setup extends Model {

  /**
   * the default GET handler. <br>
   * /setup
   */
  @Path()
  public void onGet() {

    try {
      if (Helper.isConfigured()) {
        this.redirect("/");
        return;
      }

      this.show("/admin/setup.html");

    } catch (Exception e1) {
      log.debug(e1.getMessage(), e1);
      this.error(e1);
    }

  }

  /**
   * the web api. <br>
   * /setup/save
   */
  @Path(path = "save")
  public void save() {
    JSON jo = new JSON();
    if (Helper.isConfigured()) {
      jo.put(X.STATE, 201);
      jo.put(X.MESSAGE, "already configured, forbidden override, must edit the giiwa.properties by manual");
      this.response(jo);
      return;
    }

    Configuration conf = Config.getConfig();

    String dbdriver = this.getHtml("db.driver");
    String dburl = this.getHtml("db.url");
    if (!X.isEmpty(dbdriver)) {
      conf.setProperty("db.driver", dbdriver);
    }
    if (!X.isEmpty(dburl)) {
      conf.setProperty("db.url", dburl);
    }

    String mongourl = this.getHtml("mongo.url");
    String mongodb = this.getString("mongo.db");
    String user = this.getString("mongo.user");
    String pwd = this.getString("mongo.pwd");

    if (!X.isEmpty(mongourl)) {
      conf.setProperty("mongo[prod].url", mongourl);
    }

    if (!X.isEmpty(mongodb)) {
      conf.setProperty("mongo[prod].db", mongodb);
    }
    if (!X.isEmpty(user)) {
      conf.setProperty("mongo[prod].user", user);
    }
    if (!X.isEmpty(pwd)) {
      conf.setProperty("mongo[prod].password", pwd);
    }

    String node = this.getString("node");
    String systemcode = this.getString("systemcode");

    conf.setProperty("node", node);
    conf.setProperty("system.code", systemcode);
    conf.setProperty("home", null);

    Config.save();
    DB.init();
    Helper.init(conf);

    DefaultListener.owner.upgrade(conf, Module.load("default"));

    String admin = this.getString("admin").trim().toLowerCase();
    String password = this.getString("password").trim();

    User u1 = User.loadById(0);
    V v = V.create("name", admin).set("password", password).set("email", this.getString("email"))
        .set("phone", this.getString("phone")).set("nickname", admin);
    if (u1 != null) {
      User.update(0, v);
    } else {
      User.create(v.set("id", 0L));
    }

    jo.put(X.STATE, 200);
    this.response(jo);
    new Task() {

      @Override
      public void onExecute() {
        System.exit(0);
      }

    }.schedule(10);
  }

  /**
   * web api. <br>
   * /setup/check
   */
  @Path(path = "check")
  public void check() {
    JSON jo = new JSON();

    // Configuration conf = Config.getConfig();

    String op = this.getString("op");
    if ("db".equals(op)) {

      String url = this.getHtml("url").trim();
      // String driver = this.getHtml("driver");
      // conf.setProperty("db.url", url);
      // conf.setProperty("db.driver", driver);
      //
      try {
        if (!X.isEmpty(url)) {
          Connection c1 = DB.getConnectionByUrl(url);
          Statement stat = c1.createStatement();
          stat.execute("create table test_ppp(X char(1))");
          stat.execute("drop table test_ppp");
        }
        jo.put(X.STATE, 200);
      } catch (Exception e1) {
        log.error(e1.getMessage(), e1);
        OpLog.error(setup.class, "check", e1.getMessage(), e1, login, this.getRemoteHost());

        jo.put(X.STATE, 201);
        jo.put(X.MESSAGE, e1.getMessage());
      }

    } else if ("mongo".equals(op)) {
      String url = this.getHtml("url").trim();
      String dbname = this.getString("db").trim();
      String user = this.getString("user").trim();
      String pwd = this.getString("pwd").trim();

      if (!X.isEmpty(url) && !X.isEmpty(dbname)) {
        // jo.put(X.STATE, 201);
        // if (X.isEmpty(url)) {
        // jo.put(X.MESSAGE, "没有设置URL");
        // } else {
        // jo.put(X.MESSAGE, "没有设置DB");
        // }
        // } else {
        log.debug("url=" + url + ", db=" + dbname);
        String[] hosts = url.split(";");

        ArrayList<ServerAddress> list = new ArrayList<ServerAddress>();
        for (String s : hosts) {
          try {
            String[] s2 = s.split(":");
            String host;
            int port = 27017;
            if (s2.length > 1) {
              host = s2[0];
              port = Integer.parseInt(s2[1]);
            } else {
              host = s2[0];
            }

            list.add(new ServerAddress(host, port));
          } catch (Exception e1) {
            log.error(e1.getMessage(), e1);
            OpLog.error(setup.class, "check", e1.getMessage(), e1, login, this.getRemoteHost());
          }
        }

        try {
          MongoOptions mo = new MongoOptions();
          mo.connectionsPerHost = 10;
          Mongo mongodb = new Mongo(list, mo);
          com.mongodb.DB d1 = mongodb.getDB(dbname);
          if (X.isEmpty(user) || d1.authenticate(user, pwd.toCharArray())) {
            jo.put(X.STATE, 200);
          } else {
            jo.put(X.STATE, 201);
            jo.put(X.MESSAGE, "authentication fail");
          }

        } catch (Exception e1) {
          log.error(e1.getMessage(), e1);
          OpLog.error(setup.class, "check", e1.getMessage(), e1, login, this.getRemoteHost());

          jo.put(X.STATE, 201);
          jo.put(X.MESSAGE, e1.getMessage());
        }
      } else {
        jo.put(X.STATE, 200);
      }
    } else if ("mq".equals(op)) {

      jo.put(X.STATE, 200);

    } else if ("cache".equals(op)) {

      jo.put(X.STATE, 200);

    } else {

      jo.put(X.STATE, 201);

    }
    this.response(jo);
  }

}
