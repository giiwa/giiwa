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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.giiwa.core.base.DES;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.X;
import org.giiwa.framework.bean.Appkey;
import org.giiwa.framework.bean.Data;
import org.giiwa.framework.sync.IFilter;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;
import org.postgresql.util.Base64;

import com.mongodb.BasicDBObject;

// TODO: Auto-generated Javadoc
/**
 * web api. <br>
 * /data
 * <br>
 * used the sync data
 * 
 * @author joe
 *
 */
public class data extends Model {

  /* (non-Javadoc)
   * @see org.giiwa.framework.web.Model#onPost()
   */
  @Path(log = Model.METHOD_POST)
  public void onPost() {
    String m = this.getHeader("m");
    if ("get".equals(m)) {
      get();
    } else if ("set".equals(m)) {
      set();
    }
  }

  /**
   * using appid to upload the data.
   */
  @Path(path = "set", method = Model.METHOD_POST, log = Model.METHOD_POST)
  public void set() {
    String appkey = this.getString("appkey");

    Appkey a = Appkey.load(appkey);
    JSONObject jo = new JSONObject();
    if (a == null || a.isLocked()) {
      jo.put(X.STATE, 201);
      jo.put(X.MESSAGE, "invalid appkey");
    } else {

      Appkey.update(appkey, V.create("lastlogin", System.currentTimeMillis()).set("ip", this.getRemoteHost()));

      try {

        String d1 = this.getString("data");
        String key = a.getSecret();
        byte[] d2 = DES.decode(Base64.decode(d1), key.getBytes());

        JSONObject j1 = JSONObject.fromObject(new String(d2));

        long t1 = j1.getLong("_time");
        if (System.currentTimeMillis() - t1 > X.AMINUTE) {
          jo.put(X.STATE, 201);
          jo.put(X.MESSAGE, "invalid data");
        } else {
          JSONArray list = j1.getJSONArray("list");

          String rule = a.getString("setrule");
          JSONArray rules = X.isEmpty(rule) ? null : JSONArray.fromObject(rule);
          int total = 0;
          log.debug("setting : total = " + list.size());
          for (int i = 0; i < list.size(); i++) {
            JSONObject j2 = list.getJSONObject(i);
            if (validation(j2, rules)) {
              total += Data.update(j2.getString("collection"), j2);
            } else {
              // forget this data
              log.warn("data is not match the setrule !!, setrule=" + rule + ", data=" + j2.toString());
              jo.put(X.MESSAGE, "data is not match the set rule!");
            }
          }

          jo.put(X.STATE, 200);
          jo.put("updated", total);
        }
      } catch (Exception e) {
        jo.put(X.STATE, 201);
        jo.put(X.MESSAGE, e.getMessage());

        log.error(e.getMessage(), e);
      }
    }

    this.response(jo);
  }

  /**
   * Validation.
   *
   * @param data
   *          the data
   * @param rules
   *          the rules
   * @return true, if successful
   */
  boolean validation(JSONObject data, JSONArray rules) {

    if (rules == null) {
      return true;
    }
    for (int i = 0; i < rules.size(); i++) {
      JSONObject r = rules.getJSONObject(i);
      // if match any one
      boolean b = true;
      for (Object name : r.keySet()) {
        Object r1 = r.get(name);
        if (!data.containsKey(name)) {
          b = false;
          continue;
        }
        Object d = data.get(name);
        if (!d.toString().matches(r1.toString())) {
          b = false;
          continue;
        }
      }

      if (b) {
        return true;
      }
    }

    return false;
  }

  /**
   * using appid to get data.
   */
  @Path(path = "get", method = Model.METHOD_POST, log = Model.METHOD_POST)
  public void get() {
    String appkey = this.getString("appkey");

    if (!X.isEmpty(appkey)) {
      Appkey a = Appkey.load(appkey);
      JSONObject jo = new JSONObject();
      if (a == null || a.isLocked()) {
        jo.put(X.STATE, 201);
        jo.put(X.MESSAGE, "invalid appkey");
      } else {

        Appkey.update(appkey, V.create("lastlogin", System.currentTimeMillis()).set("ip", this.getRemoteHost()));

        try {
          String d1 = this.getString("data");
          String key = a.getSecret();
          byte[] d2 = DES.decode(Base64.decode(d1), key.getBytes());

          JSONObject j1 = JSONObject.fromObject(new String(d2));
          long t1 = j1.getLong("_time");
          if (System.currentTimeMillis() - t1 > X.AMINUTE) {
            jo.put(X.STATE, 202);
            jo.put(X.MESSAGE, "invalid data");
          } else {

            String collection = j1.getString("collection");

            if (!X.isEmpty(collection)) {
              String rule = a.getString("getrule");
              JSONArray rules = X.isEmpty(rule) ? null : JSONArray.fromObject(rule);

              JSONObject query = j1.getJSONObject("query");
              JSONObject order = j1.getJSONObject("order");

              int s = j1.containsKey("s") ? j1.getInt("s") : 0;
              int n = j1.containsKey("n") ? j1.getInt("n") : 1;
              if (n < 1) {
                n = 1;
              }

              BasicDBObject q = new BasicDBObject();

              buildQuery(q, collection, rules);

              for (Object name : query.keySet()) {
                Object o = query.get(name);

                if (o instanceof JSONObject) {
                  BasicDBObject q1 = new BasicDBObject();
                  JSONObject j2 = (JSONObject) o;
                  for (Object n1 : j2.keySet()) {
                    q1.append(n1.toString(), j2.get(n1));
                  }
                  q.append(name.toString(), q1);
                } else {
                  q.append(name.toString(), o);
                }
              }

              BasicDBObject o = new BasicDBObject();
              for (Object name : order.keySet()) {
                o.append(name.toString(), order.get(name));
              }

              Beans<Data> bs = Data.load(collection, q, o, s, n);
              JSONArray arr = new JSONArray();
              if (bs != null && bs.getList() != null) {
                for (Data d : bs.getList()) {
                  JSONObject j2 = new JSONObject();

                  // special handler
                  List<IFilter> filter = filters.get(collection);
                  if (filter != null && filter.size() > 0) {
                    for (IFilter f : filter) {
                      if (!f.process("get", d)) {
                        break;
                      }
                    }
                  }
                  d.toJSON(j2);
                  j2.put("collection", collection);

                  arr.add(j2);
                }
              }

              jo.put(X.STATE, 200);
              jo.put("n", n);
              jo.put("s", s);
              jo.put("list", arr);

            } else {
              jo.put(X.STATE, 203);
              jo.put(X.MESSAGE, "collecction not presented");
            }
          }
        } catch (Exception e) {
          jo.put(X.STATE, 204);
          jo.put(X.MESSAGE, e.getMessage());
          log.error(e.getMessage(), e);
        }
      }

      this.response(jo);
    } else {
      login = this.getUser();
      if (login != null) {

      }
    }
  }

  /**
   * Builds the query.
   *
   * @param q
   *          the q
   * @param collection
   *          the collection
   * @param rules
   *          the rules
   */
  void buildQuery(BasicDBObject q, String collection, JSONArray rules) {
    if (rules == null) {
      return;
    }

    for (int i = 0; i < rules.size(); i++) {
      JSONObject rule = rules.getJSONObject(i);

      if (rule.containsKey("collection")) {
        String c1 = rule.getString("collection");

        if (collection.matches(c1)) {
          for (Object name : rule.keySet()) {
            if (!"collection".equals(name)) {
              String val = rule.getString(name.toString());
              q.append(name.toString(), Pattern.compile(val, Pattern.CASE_INSENSITIVE));
            }
          }
        }
      }
    }
  }

  private static Map<String, List<IFilter>> filters = new HashMap<String, List<IFilter>>();

  /**
   * Register.
   *
   * @param collection
   *          the collection
   * @param filter
   *          the filter
   */
  public static synchronized void register(String collection, IFilter filter) {
    List<IFilter> list = filters.get(collection);
    if (list == null) {
      list = new ArrayList<IFilter>();
      filters.put(collection, list);
    } else {
      list.remove(filter);
    }
    list.add(filter);
  }

}
