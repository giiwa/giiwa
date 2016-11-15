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

import java.sql.Connection;
import java.sql.Statement;

import org.apache.commons.configuration.Configuration;
import org.bson.Document;
import org.giiwa.core.bean.RDB;
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;
import org.giiwa.core.cache.Cache;
import org.giiwa.core.cache.DefaultCachable;
import org.giiwa.core.conf.Config;
import org.giiwa.core.json.JSON;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.OpLog;
import org.giiwa.framework.bean.User;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Module;
import org.giiwa.framework.web.Path;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

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
    conf.setProperty("db.user", this.getString("db.user"));
    conf.setProperty("db.passwd", this.getString("db.passwd"));

    String mongourl = this.getHtml("mongo.url");
    String mongodb = this.getString("mongo.db");

    if (!X.isEmpty(mongourl)) {
      conf.setProperty("mongo[prod].url", mongourl);
    }

    if (!X.isEmpty(mongodb)) {
      conf.setProperty("mongo[prod].db", mongodb);
    }

    String node = this.getString("node");
    String systemcode = this.getString("systemcode");

    conf.setProperty("cache.url", this.getString("cache.url"));
    conf.setProperty("cache.group", this.getString("cache.group"));

    conf.setProperty("node", node);
    conf.setProperty("system.code", systemcode);

    Config.save();
    RDB.init();
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

      String url = this.getHtml(X.URL).trim();
      String username = this.getString("username").trim();
      String passwd = this.getString("passwd").trim();

      // String driver = this.getHtml("driver");
      // conf.setProperty("db.url", url);
      // conf.setProperty("db.driver", driver);
      //
      try {
        if (!X.isEmpty(url)) {
          Connection c1 = RDB.getConnectionByUrl(url, username, passwd);
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
      String url = this.getHtml(X.URL).trim();
      String dbname = this.getString("db").trim();

      if (!X.isEmpty(url) && !X.isEmpty(dbname)) {
        log.debug("url=" + url + ", db=" + dbname);

        try {
          MongoClientOptions.Builder opts = new MongoClientOptions.Builder().socketTimeout(5000)
              .serverSelectionTimeout(1000);
          MongoClient client = new MongoClient(new MongoClientURI(url, opts));

          MongoDatabase g = client.getDatabase(dbname);
          String name = "test_" + UID.digital(5);
          g.createCollection(name);
          MongoCollection<Document> c1 = g.getCollection(name);
          if (c1 != null) {
            c1.drop();
            jo.put(X.STATE, 200);
          } else {
            jo.put(X.STATE, 201);
            jo.put(X.MESSAGE, "can not access");
          }
          client.close();
        } catch (Exception e1) {
          log.error(e1.getMessage(), e1);
          OpLog.error(setup.class, "check", e1.getMessage(), e1, login, this.getRemoteHost());

          jo.put(X.STATE, 201);
          jo.put(X.MESSAGE, e1.getMessage());
        }
      } else {
        jo.put(X.STATE, 200);
      }

    } else if ("cache".equals(op)) {
      String url = this.getHtml(X.URL).trim();
      String group = this.getString("group").trim();

      try {
        if (!X.isEmpty(url)) {
          Configuration conf = Config.getConfig();
          conf.setProperty("cache.url", url);
          conf.setProperty("cache.group", group);

          Cache.init(conf);
          DefaultCachable v1 = new DefaultCachable();
          Cache.set("test", v1);
          DefaultCachable v2 = Cache.get("test");
          if (v2 != null && v1.age() == v2.age()) {
            jo.put(X.STATE, 200);
          } else {
            jo.put(X.STATE, 201);
            jo.put(X.MESSAGE, "cache system failed");
          }
        } else {
          jo.put(X.STATE, 200);
        }
      } catch (Exception e) {
        log.error("url=" + url, e);
        jo.put(X.STATE, 201);
        jo.put(X.MESSAGE, e.getMessage());
      }

    } else {

      jo.put(X.STATE, 201);

    }
    this.response(jo);
  }

}
