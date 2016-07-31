/*
 *   giiwa, a java web foramewrok.
 *   Copyright (C) <2014>  <giiwa.org>
 *
 */
package org.giiwa.app.web.admin;

import java.util.regex.Pattern;

import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;
import org.giiwa.framework.bean.Appkey;
import org.giiwa.framework.bean.OpLog;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;

import net.sf.json.JSONObject;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

// TODO: Auto-generated Javadoc
/**
 * web api: /admin/app
 * <br>
 * used to manage the app token
 * 
 * @author joe
 *
 */
public class appkey extends Model {

    /**
     * History.
     */
    @Path(path = "history", login = true, access = "access.user.admin")
    public void history() {
        int s = this.getInt("s");
        int n = this.getInt("n", 10, "default.list.number");

        JSONObject jo = this.getJSON();
        // W w = W.create().copy(jo, W.OP_EQ, "op");
        // w.and("module", App.class.getName(), W.OP_EQ);
        BasicDBObject q = new BasicDBObject().append("module", Appkey.class.getName());
        if (!X.isEmpty(jo.get("op"))) {
            q.append("op", jo.get("op"));
        }
        if (!X.isEmpty(jo.get("uid"))) {
            q.append("uid", X.toInt(jo.get("uid")));
        }
        if (!X.isEmpty(jo.get("type"))) {
            q.append("type", X.toInt(jo.get("type")));
        }
        if (!X.isEmpty(jo.get("ip"))) {
            q.append("ip", Pattern.compile(jo.getString("ip"), Pattern.CASE_INSENSITIVE));
        }

        this.set(jo);

        if (s > 0) {
            this.set("currentpage", 1);
        }

        Beans<OpLog> bs = OpLog.load(q, s, n);
        this.set(bs, s, n);

        this.show("/admin/app.history.html");

    }

    /**
     * Verify.
     */
    @Path(path = "verify", login = true, access = "access.config.admin")
    public void verify() {
        String name = this.getString("name");
        String value = this.getString("value");
        JSONObject jo = new JSONObject();
        if (X.isEmpty(value)) {
            jo.put(X.STATE, 201);
            jo.put(X.MESSAGE, lang.get("id.empty.error"));
        } else {
            if (Appkey.load(value) != null) {
                jo.put(X.STATE, 201);
                jo.put(X.MESSAGE, lang.get("id.exists.error"));
            } else {
                jo.put(X.STATE, 200);
            }
        }

        this.response(jo);
    }

    /**
     * app/create.
     */
    @Path(path = "create", login = true, access = "access.config.admin", log = Model.METHOD_POST)
    public void create() {
        if (method.isPost()) {
            JSONObject jo = this.getJSON();
            String appkey = this.getString("appkey");
            if (Appkey.create(appkey, V.create("appkey", appkey).copy(jo, "memo", "contact", "phone", "logout", "email").set("setrule", this.getHtml("setrule")).set("getrule",
                    this.getHtml("getrule")).set("secret", UID.random(24))) > 0) {
                this.set(X.MESSAGE, lang.get("create.success"));

                onGet();

                return;
            } else {
                this.set(X.MESSAGE, lang.get("create.fail"));
                this.set(jo);
            }
        }

        this.show("/admin/appkey.create.html");
    }

    /**
     * Lock.
     */
    @Path(path = "lock", login = true, access = "access.config.admin")
    public void lock() {
        String appkey = this.getString("appkey");
        int updated = 0;
        if (appkey != null) {
            String[] ss = appkey.split(",");
            V v = V.create("locked", 1);
            for (String s : ss) {
                Appkey a = Appkey.load(s);
                if (a != null) {
                    int i = Appkey.update(s, v);
                    if (i > 0) {
                        OpLog.log(Appkey.class, "lock", a.getAppkey(), null, login.getId(), this.getRemoteHost());
                        updated += i;
                    }
                }
            }
        }

        if (updated > 0) {
            this.set(X.MESSAGE, lang.get("edit.seccuss"));
        } else {
            this.set(X.MESSAGE, lang.get("select.required"));
        }

        onGet();
    }

    /**
     * Delete.
     */
    @Path(path = "delete", login = true, access = "access.config.admin")
    public void delete() {
        String appkey = this.getString("appkey");
        Appkey.delete(appkey);
        this.response(200, null);
    }

    /**
     * Edits the.
     */
    @Path(path = "edit", login = true, access = "access.config.admin")
    public void edit() {
        String appkey = this.getString("appkey");
        if (method.isPost()) {
            JSONObject jo = this.getJSON();
            V v = V.create().copy(jo, "memo", "secret", "contact", "phone", "logout", "email").set("locked", "on".equals(this.getString("locked")) ? 1 : 0).set("setrule",
                    this.getHtml("setrule")).set("getrule", this.getHtml("getrule"));

            if (Appkey.update(appkey, v) > 0) {
                this.set(X.MESSAGE, lang.get("save.success"));

                onGet();

                return;
            } else {
                this.set(X.MESSAGE, lang.get("save.fail"));
                this.set(jo);
            }
        } else {
            if (appkey != null) {
                String[] ss = appkey.split(",");
                for (String s : ss) {
                    Appkey a = Appkey.load(s);
                    JSONObject jo = new JSONObject();
                    a.toJSON(jo);
                    this.set(jo);
                    break;
                }
            } else {
                this.set(X.MESSAGE, lang.get("select.required"));
                onGet();
                return;
            }
        }

        this.show("/admin/appkey.edit.html");

    }

    /**
     * Newkey.
     */
    @Path(path = "newkey", login = true, access = "access.config.admin")
    public void newkey() {
        this.println(UID.random(24));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.giiwa.framework.web.Model#onGet()
     */
    @Path(login = true, access = "access.config.admin")
    public void onGet() {
        int s = this.getInt("s");
        int n = this.getInt("n", 10, "number.per.page");

        BasicDBObject q = new BasicDBObject();
        String name = this.getString("name");
        if (X.isEmpty(this.path) && !X.isEmpty(name)) {
            BasicDBList list = new BasicDBList();
            Pattern pattern = Pattern.compile(name, Pattern.CASE_INSENSITIVE);

            list.add(new BasicDBObject().append("appkey", pattern));
            list.add(new BasicDBObject().append("secret", pattern));
            list.add(new BasicDBObject().append("company", pattern));
            list.add(new BasicDBObject().append("contact", pattern));
            list.add(new BasicDBObject().append("phone", pattern));
            q.append("$or", list);
        }
        BasicDBObject order = new BasicDBObject("appkey", 1);
        Beans<Appkey> bs = Appkey.load(W.create(), s, 10);
        this.set(bs, s, n);

        this.query.parse("/admin/app");
        show("/admin/appkey.index.html");
    }

}
