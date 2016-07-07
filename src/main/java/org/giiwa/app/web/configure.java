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
import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.X;
import org.giiwa.core.bean.Bean.V;
import org.giiwa.core.conf.Local;
import org.giiwa.core.db.DB;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.User;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Module;
import org.giiwa.framework.web.Path;

import net.sf.json.JSONObject;

import com.mongodb.Mongo;
import com.mongodb.MongoOptions;
import com.mongodb.ServerAddress;

// TODO: Auto-generated Javadoc
/**
 * web api .<br>
 * /configure <br>
 * used to the initial configure, once configured, it will not be accessed
 * 
 * @author joe
 *
 */
public class configure extends Model {

  /**
   * the default GET handler. <br>
   * /configure
   */
  @Path()
  public void onGet() {

    try {
      if (Bean.isConfigured() ) {
        this.redirect("/");
        return;
      }

      this.show("/admin/configure.html");

    } catch (Exception e1) {
      log.debug(e1.getMessage(), e1);
      this.error(e1);
    }

  }

  /**
   * the web api. <br>
   * /configure/save
   */
  @Path(path = "save")
  public void save() {
    JSONObject jo = new JSONObject();
    if (Bean.isConfigured()) {
      jo.put(X.STATE, 201);
      jo.put(X.MESSAGE, "already configured, forbidden override, must edit the giiwa.properties by manual");
      this.response(jo);
      return;
    }

    Configuration conf = Local.getConfig();

    String dbdriver = this.getHtml("db.driver");
    String dburl = this.getHtml("db.url");
    conf.setProperty("db.driver", dbdriver);
    conf.setProperty("db.url", dburl);

    String mongourl = this.getHtml("mongo.url");
    String mmongodb = this.getString("mongo.db");
    String user = this.getString("mongo.user");
    String pwd = this.getString("mongo.pwd");
    conf.setProperty("mongo[prod].url", mongourl);
    conf.setProperty("mongo[prod].db", mmongodb);
    conf.setProperty("mongo[prod].user", user);
    conf.setProperty("mongo[prod].password", pwd);

    String node = this.getString("node");
    String systemcode = this.getString("systemcode");
    conf.setProperty("node", node);
    conf.setProperty("system.code", systemcode);

    Local.save();
    DB.init();
    Bean.init(conf);

    DefaultListener.owner.upgrade(conf, Module.load("default"));

    String admin = this.getString("admin").trim();
    String password = this.getHtml("password").trim();

    User u1 = User.loadById(0);
    if (u1 != null) {
      User.update(0, V.create("name", admin).set("password", password));
    } else {
      User.create(V.create("name", admin).set("password", password).set("id", 0L));
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
   * /configure/check
   */
  @Path(path = "check")
  public void check() {
    JSONObject jo = new JSONObject();

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
