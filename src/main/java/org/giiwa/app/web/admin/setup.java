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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.commons.configuration2.Configuration;
import org.bson.Document;
import org.giiwa.app.web.DefaultListener;
import org.giiwa.bean.GLog;
import org.giiwa.cache.Cache;
import org.giiwa.conf.Config;
import org.giiwa.dao.Helper;
import org.giiwa.dao.RDSHelper;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.task.Task;
import org.giiwa.web.Controller;
import org.giiwa.web.Module;
import org.giiwa.web.Path;

import com.mongodb.BasicDBObject;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * web api: /setup <br>
 * used to the initial configure, once configured, it will not be accessed
 * 
 * @author joe
 *
 */
public class setup extends Controller {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

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

			this.set("home", Controller.GIIWA_HOME);
			this.show("/admin/setup.html");

		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
			this.error(e1);
		}

	}

	/**
	 * the web api. <br>
	 * /setup/save
	 */
	@Path(path = "save")
	public void save() {

		if (Helper.isConfigured()) {
			this.set(X.MESSAGE, "already configured!").send(201);
			return;
		}

		JSON jo = new JSON();
		Configuration conf = Config.getConf();

		String driver = this.getHtml("driver");
		if (X.isSame(driver, "mongo")) {
			String url = this.getHtml("url");
			String db = this.getString("db");

			if (!X.isEmpty(url)) {
				conf.setProperty("db.url", url + "/" + db);
			}

			if (!X.isEmpty(db)) {
				conf.setProperty("site.group", db);
			}

		} else {

			String url = this.getHtml("url");
//			if (!X.isEmpty(dbdriver)) {
//				conf.setProperty("db[default].driver", dbdriver);
//			}

			if (!X.isEmpty(url)) {
				conf.setProperty("db.url", url);
			}
		}
		conf.setProperty("cache.url", this.getString("cache.url"));

		conf.setProperty("cluster.code", this.getLong("cluster.code"));

		Config.save();

		Helper.init2(conf);

//		if (!Helper.init2(conf)) {
//			RDB.init();
//			Helper.init(conf);
//		}

		DefaultListener.owner.upgrade(conf, Module.load("default"));

		jo.put(X.STATE, 200);
		this.send(jo);

		log.warn("save config, restart now!");

		Task.schedule(t -> {
			System.exit(0);
		}, 1000);
	}

	/**
	 * web api. <br>
	 * /setup/check
	 */
	@Path(path = "check")
	public void check() {

		if (Helper.isConfigured()) {
			this.set(X.MESSAGE, "already configured!").send(201);
			return;
		}

		JSON jo = new JSON();

		String url = this.getHtml(X.URL).trim();
		String driver = this.getString("driver");

		if ("mongo".equals(driver)) {

			String dbname = this.getString("db").trim();

			if (!X.isEmpty(url) && !X.isEmpty(dbname)) {
				if (log.isDebugEnabled())
					log.debug("url=" + url + ", db=" + dbname);

				try {

					ConnectionString connString = new ConnectionString(url);
					MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(connString)
							.retryWrites(true).build();

					MongoClient client = MongoClients.create(settings);

					MongoDatabase g = client.getDatabase(dbname);
					String name = "test_" + UID.digital(5);
					g.createCollection(name);
					MongoCollection<Document> c1 = g.getCollection(name);
					if (c1 != null) {
						c1.drop();

						c1 = g.getCollection("gi_user");
						FindIterable<Document> f1 = c1.find(new BasicDBObject().append(X.ID, 0));
						if (f1.iterator().hasNext()) {
							jo.put("admin", 1);
						} else {
							jo.put("admin", 0);
						}

						jo.put(X.STATE, 200);
					} else {
						jo.put(X.STATE, 201);
						jo.put(X.MESSAGE, "can not access");
					}
					client.close();
				} catch (Exception e1) {
					log.error(e1.getMessage(), e1);
					GLog.oplog.error(setup.class, "check", e1.getMessage(), e1, login, this.ip());

					jo.put(X.STATE, 201);
					jo.put(X.MESSAGE, e1.getMessage());
				}
			} else {
				jo.append(X.MESSAGE, lang.get("error.db.url.empty")).put(X.STATE, 201);
			}

		} else if ("cache".equals(driver)) {

			try {
				if (!X.isEmpty(url)) {
					Configuration conf = Config.getConf();
					conf.setProperty("cache.url", url);
					conf.setProperty("site.group", "demo");

					Cache.init(url, null, null);

					String s1 = "1";
					Cache.set("test", s1, X.AMINUTE);
					String s2 = Cache.get("test");
					if (X.isSame(s1, s2)) {
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

			String username = this.getString("username");
			if (!X.isEmpty(username)) {
				username = username.trim();
			}
			String passwd = this.getString("passwd");
			if (!X.isEmpty(passwd)) {
				passwd = passwd.trim();
			}

			try {
				if (!X.isEmpty(url)) {
					Connection c1 = RDSHelper.getConnection(url, username, passwd, null);
					Statement stat = c1.createStatement();
					stat.execute("create table test_ppp(X char(1))");
					stat.execute("drop table test_ppp");
					ResultSet r = null;
					try {
						r = stat.executeQuery("select * from gi_user where id=0");
						if (r.next()) {
							jo.put("admin", 1);
						} else {
							jo.put("admin", 0);
						}
					} catch (Exception e) {
						jo.put("admin", 0);
					} finally {
						RDSHelper.inst.close(r, stat, c1);
					}
					jo.put(X.STATE, 200);
				} else {
					jo.append(X.MESSAGE, lang.get("error.db.url.empty")).put(X.STATE, 201);
				}
			} catch (Exception e1) {
				log.error(e1.getMessage(), e1);
				GLog.oplog.error(setup.class, "check", e1.getMessage(), e1, login, this.ip());

				jo.put(X.STATE, 201);
				jo.put(X.MESSAGE, e1.getMessage());
			}

		}
		this.send(jo);
	}

}
