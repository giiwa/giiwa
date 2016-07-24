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
package org.giiwa.framework.sync;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.base.Base64;
import org.giiwa.core.base.DES;
import org.giiwa.core.base.Http;
import org.giiwa.core.base.Http.Response;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.Data;
import org.giiwa.framework.bean.OpLog;

import com.mongodb.BasicDBObject;

// TODO: Auto-generated Javadoc
/**
 * the {@code SyncTask} Class lets sync the data from/to remote
 * 
 * the most important APIs:
 * 
 * <pre>
 * register(String name, String order), register the sync data
 * 
 * </pre>
 * 
 * @author joe
 *
 */
public class SyncTask extends Task {

  static Log             log      = LogFactory.getLog(SyncTask.class);

  public static SyncTask instance = new SyncTask();

  public static enum Type {
    set, get, mset;
  };

  private static Map<String, List<String>> groups      = new LinkedHashMap<String, List<String>>();
  private static Map<String, DataFilter>   collections = new LinkedHashMap<String, DataFilter>();

  /**
   * test the collection setting is support op "t".
   *
   * @param collection
   *          the collection
   * @param t
   *          the t
   * @return boolean
   */
  public boolean support(String collection, String t) {
    DataFilter df = collections.get(collection);
    if (df != null && df.type != null) {
      for (Type t1 : df.type) {
        if (t1.toString().equals(t)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * get the setting of the collection.
   *
   * @param collection
   *          the collection
   * @return String
   */
  public String setting(String collection) {
    return Global.s("sync." + collection, null);
  }

  /**
   * get the lasttime of data which synced.
   *
   * @param collection
   *          the collection
   * @return long
   */
  public long lasttime(String collection) {
    return Global.l("sync." + collection + ".lasttime", 0);
  }

  /**
   * get All Collections for syncing
   * 
   * @return Map
   */
  public static Map<String, DataFilter> getCollections() {
    return collections;
  }

  /**
   * register the collection that can be sync, with the order:
   * "{update_time:1}".
   *
   * @param collection
   *          the collection
   * @param order
   *          the order
   * @param number
   *          the number
   */
  public static void register(String collection, String order, int number) {
    register(collection, collection, order, number);
  }

  /**
   * register a collection under the "parent" setting, with the "order".
   *
   * @param parent
   *          the parent
   * @param collection
   *          the collection
   * @param order
   *          the order
   * @param number
   *          the number
   */
  public static void register(String parent, String collection, String order, int number) {
    register(parent, collection, order, null, number);
  }

  /**
   * set default type: set and get.
   *
   * @param parent
   *          the parent
   * @param collection
   *          the collection
   * @param order
   *          the order
   * @param filter
   *          the filter
   * @param number
   *          the number
   */
  public static void register(String parent, String collection, String order, IFilter filter, int number) {
    register(parent, collection, order, filter, new Type[] { Type.set, Type.get }, number);
  }

  /**
   * register a collection under parent, order by "order", and call filter when
   * syncing.
   *
   * @param parent
   *          the parent
   * @param collection
   *          the collection
   * @param order
   *          the order
   * @param filter
   *          the filter
   * @param t
   *          the t
   * @param number
   *          the number
   */
  public static void register(String parent, String collection, String order, IFilter filter, Type[] t, int number) {
    DataFilter df = new DataFilter();
    df.order = order;
    df.filter = filter;
    df.type = t;
    df.number = number;

    collections.put(collection, df);
    List<String> list = groups.get(parent);
    if (list == null) {
      list = new ArrayList<String>();
      groups.put(parent, list);
    }
    list.add(collection);
  }

  /**
   * reset the sync timestamp, and start to sync.
   *
   * @param group
   *          the group name of the data
   * @param collection
   *          the collection name, if it is null, then reset all the collections
   *          under the group
   */
  public static void reset(String group, String collection) {

    if (X.isEmpty(collection)) {
      Global.setConfig("sync." + collection + ".lasttime", 0L);
    } else {
      List<String> collections = groups.get(group);
      if (collections != null && collections.size() > 0) {
        for (String s : collections) {
          Global.setConfig("sync." + s + ".lasttime", 0L);
        }
      }
    }

    SyncTask.start();
  }

  /**
   * start the sync right now.
   */
  public static void start() {
    instance.schedule(0);
  }

  /**
   * get groups
   * 
   * @return Set
   */
  public static Set<String> getGroups() {
    return groups.keySet();
  }

  /**
   * get collection under a group.
   *
   * @param group
   *          the group
   * @return List
   */
  public List<String> collections(String group) {
    return groups.get(group);
  }

  private SyncTask() {
  }

  private void sync(final String collection, final String url, final String appkey, final String secret) {
    String type = Global.s("sync." + collection, X.EMPTY);

    log.debug("sync type, " + collection + "=" + type);

    if ("get".equals(type)) {
      new Task() {

        @Override
        public String getName() {
          return "synctask." + collection;
        }

        @Override
        public void onExecute() {
          JSONObject req = new JSONObject();
          JSONObject query = new JSONObject();
          JSONObject order = new JSONObject();

          try {
            DataFilter df = collections.get(collection);
            if (df != null && !X.isEmpty(df.order)) {
              order = JSONObject.fromObject(df.order);
            } else {
              order.put("updated", 1);
            }

            long updated = Global.l("sync." + collection + ".lasttime", 0);
            JSONObject q = new JSONObject();
            q.put("$gte", updated);
            query.put(order.keys().next(), q);

            req.put("query", query);
            req.put("order", order);
            req.put("collection", collection);

            int s = 0;
            int n = 10;

            boolean hasmore = true;
            while (hasmore) {
              hasmore = false;

              req.put("s", s);
              req.put("n", n);
              req.put("_time", System.currentTimeMillis());

              String data = Base64.encode(DES.encode(req.toString().getBytes(), secret.getBytes()));

              Response r = Http.post(url, null, new String[][] { { "m", "get" } },
                  new String[][] { { "appkey", appkey }, { "data", data } });

              // log.debug("resp=" + r.body);
              if (r.status == 200) {
                JSONObject jo = JSONObject.fromObject(r.body);
                log.debug("resp=" + jo);

                JSONArray arr = jo.getJSONArray("list");

                if (arr != null && arr.size() > 0) {

                  for (int i = 0; i < arr.size(); i++) {
                    JSONObject j1 = arr.getJSONObject(i);
                    if (df != null && df.filter != null) {
                      df.filter.process("get", j1);
                    }

                    Data.update(collection, j1);

                    long l1 = j1.getLong(order.keys().next().toString());
                    if (l1 > updated) {
                      updated = l1;
                      Global.setConfig("sync." + collection + ".lasttime", updated);
                    }
                  }

                  s += arr.size();
                  hasmore = arr.size() >= n;
                }
              } else {
                // end if "status == 200"
                Global.setConfig("sync.lasterror", r.body);
                Global.setConfig("sync.lasterrortime", System.currentTimeMillis());
              }

            }
          } catch (Exception e) {
            log.error("query=" + query + " order=" + order, e);
            OpLog.warn("sync", e.getMessage(), e.getMessage());
            Global.setConfig("sync.lasterror", e.getMessage());
            Global.setConfig("sync.lasterrortime", System.currentTimeMillis());
          }
        }

      }.schedule(0);
    } else if ("set".equals(type)) {
      new Task() {

        @Override
        public String getName() {
          return "synctask." + collection;
        }

        @Override
        public void onExecute() {

          // auto push
          BasicDBObject query = new BasicDBObject().append("synced", new BasicDBObject().append("$ne", 1));
          BasicDBObject order = new BasicDBObject().append(X._ID, 1);
          int s = 0;

          try {
            Beans<Data> bs = Data.load(collection, query, order, s, 100);
            JSONObject jo = new JSONObject();
            jo.put("synced", 1);
            DataFilter df = collections.get(collection);

            while (bs != null && bs.getList() != null && bs.getList().size() > 0) {
              for (Data p : bs.getList()) {
                if (df != null && df.filter != null) {
                  df.filter.process("set", p);
                }
                p.set("collection", collection);

                if (Publisher.publish(p) > 0) {
                  jo.put(X._ID, p.get(X._ID));
                  Data.update(collection, jo);
                }
              }

              // s += bs.getList().size();
              bs = Data.load(collection, query, order, s, 100);
            }
          } catch (Exception e) {
            log.error(e.getMessage(), e);

            Global.setConfig("sync.lasterror", e.getMessage());
            Global.setConfig("sync.lasterrortime", System.currentTimeMillis());
          }

        }

      }.schedule(0);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.giiwa.core.task.Task#onExecute()
   */
  @Override
  public void onExecute() {

    String url = Global.s("sync.url", null);
    String appkey = Global.s("sync.appkey", null);
    String secret = Global.s("sync.secret", null);
    if (!X.isEmpty(url) && !X.isEmpty(appkey) && !X.isEmpty(secret)) {

      log.debug("collections=" + collections);

      for (String collection : collections.keySet()) {
        sync(collection, url, appkey, secret);
      }

    } else {
      Global.setConfig("sync.lasterror", "miss configuration!");
      Global.setConfig("sync.lasterrortime", System.currentTimeMillis());
    }

  }

  @Override
  public String getName() {
    return "sync.task";
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.giiwa.core.task.Task#onFinish()
   */
  @Override
  public void onFinish() {

    log.info("sync.task done.......................");

    this.schedule(X.AMINUTE);
  }

  private static class DataFilter {
    IFilter filter;
    Type[]  type;
    String  order;
    int     number = 10;
  }
}
