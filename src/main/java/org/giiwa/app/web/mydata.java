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

import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.X;
import org.giiwa.core.bean.Bean.V;
import org.giiwa.framework.bean.MyData;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;

import net.sf.json.JSONObject;

import com.mongodb.BasicDBObject;

// TODO: Auto-generated Javadoc
/**
 * web api: /mydata
 * <br>
 * used to store or get user data
 * 
 * @author joe
 *
 */
public class mydata extends Model {

    /**
     * Gets the.
     */
    @Path(path = "get", login = true)
    public void get() {
        String table = this.getString("table");
        String order = this.getString("order");

        BasicDBObject o = new BasicDBObject();
        if (!X.isEmpty(order)) {
            String[] ss = order.split(",");
            for (String s : ss) {
                String[] s1 = s.split(" ");
                if (s1.length > 1) {
                    o.append(s1[0], Bean.toInt(s1[1]));
                } else {
                    o.append(s1[0], 1);
                }
            }
        }

        JSONObject j = this.getJSON();
        for (String s : KEYWORDS) {
            j.remove(s);
        }
        BasicDBObject q = new BasicDBObject();
        for (Object name : j.keySet()) {
            String v = this.getHtml(name.toString());
            if (!X.isEmpty(v)) {
                q.append(name.toString().trim(), v);
            }
        }

        int s = this.getInt("s");
        int n = this.getInt("n", 10);

        Beans<MyData> bs = MyData.load(login.getId(), table, q, o, s, n);

        JSONObject jo = new JSONObject();
        if (bs != null) {
            jo.put("list", bs.getList());
            jo.put("total", bs.getTotal());
            jo.put("s", s);
            jo.put("n", n);
        }
        jo.put(X.STATE, 200);
        this.response(jo);
    }

    /**
     * Delete.
     */
    @Path(path = "delete", login = true)
    public void delete() {
        long id = this.getLong("id");
        JSONObject jo = new JSONObject();
        MyData.remove(id, login.getId());
        jo.put(X.STATE, 200);
        this.response(jo);
    }

    /**
     * Sets the.
     */
    @Path(path = "set", login = true)
    public void set() {
        long id = this.getLong("id", -1);
        final JSONObject jo = new JSONObject();
        JSONObject j = this.getJSON();
        for (String s : KEYWORDS) {
            j.remove(s);
        }
        j.remove("id");
        V v = V.create();
        for (Object name : j.keySet()) {
            v.set(name.toString(), this.getHtml(name.toString()));
        }

        if (id > 0) {
            MyData.update(id, login.getId(), v.set("updated", System.currentTimeMillis()));
            jo.put("op", "update");
        } else {
            String table = this.getString("table");
            id = MyData.create(login.getId(), table, v.set("created", System.currentTimeMillis()).set("updated", System.currentTimeMillis()));
        }

        MyData d = MyData.load(id, login.getId());
        jo.put(X.STATE, 200);
        jo.put("data", d.getJSON());

        /**
         * broadcast to all except myself
         */
        final String clientid = this.getString("clientid");

        this.response(jo);
    }

    static String[] KEYWORDS = { "table", "order", "s", "n", "uid" };
}
