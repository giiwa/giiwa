/*
 *   giiwa, a java web foramewrok.
 *   Copyright (C) <2014>  <giiwa.org>
 *
 */
package org.giiwa.app.web.admin;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import org.giiwa.core.bean.Bean;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.User;
import org.giiwa.framework.web.*;

import net.sf.json.JSONObject;

import com.mongodb.DB;

// TODO: Auto-generated Javadoc
/**
 * web api: /admin/system <br>
 * used to control the "system"
 * 
 * @author joe
 *
 */
public class system extends Model {

  /**
   * Restart.
   */
  @Path(path = "init", login = true, access = "access.config.admin", log = Model.METHOD_POST)
  public void init() {
    JSONObject jo = new JSONObject();
    User me = this.getUser();
    String pwd = this.getString("pwd");

    if (me.validate(pwd)) {
      jo.put("state", "ok");

      new Task() {

        @Override
        public String getName() {
          return "init";
        }

        @Override
        public void onExecute() {

          // drop all tables
          java.sql.Connection c = null;
          Statement stat = null;
          ResultSet r = null;

          try {
            c = Bean.getConnection();
            DatabaseMetaData d = c.getMetaData();
            r = d.getTables(null, null, null, new String[] { "TABLE" });
            while (r.next()) {
              String name = r.getString("table_name");

              ResultSetMetaData rm = r.getMetaData();
              StringBuilder sb = new StringBuilder();
              for (int i = 0; i < rm.getColumnCount(); i++) {
                sb.append(rm.getColumnName(i + 1) + "=" + r.getString(i + 1)).append(",");
              }
              log.warn("table=" + sb.toString());
              stat = c.createStatement();
              stat.execute("drop table " + name);
              stat.close();
              stat = null;
            }

            // drop all collections
            DB d1 = Bean.getDB();
            if (d1 != null) {
              d1.dropDatabase();
            }

          } catch (Exception e) {
            log.error(e.getMessage(), e);
          } finally {
            Bean.close(r, stat, c);
          }

          // drop all collection

          System.exit(0);
        }

        @Override
        public void onFinish() {

        }

      }.schedule(1000);
    } else {
      jo.put("state", "fail");
      jo.put("message", lang.get("invalid.password"));
    }

    this.response(jo);
  }

  /**
   * Restart.
   */
  @Path(path = "restart", login = true, access = "access.config.admin", log = Model.METHOD_POST)
  public void restart() {

    JSONObject jo = new JSONObject();
    User me = this.getUser();
    String pwd = this.getString("pwd");

    if (me.validate(pwd)) {
      jo.put("state", "ok");

      new Task() {

        @Override
        public String getName() {
          return "restart";
        }

        @Override
        public void onExecute() {
          System.exit(0);
        }

        @Override
        public void onFinish() {

        }

      }.schedule(1000);
    } else {
      jo.put("state", "fail");
      jo.put("message", lang.get("invalid.password"));
    }

    this.response(jo);
  }

}
