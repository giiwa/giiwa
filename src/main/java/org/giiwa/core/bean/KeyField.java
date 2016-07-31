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
package org.giiwa.core.bean;

import java.util.List;

import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.task.Task;

import net.sf.json.JSONObject;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

// TODO: Auto-generated Javadoc
/**
 * the key fields for the collection, used to create index
 * 
 * @author joe
 *
 */

@Table(name = "gi_key")
public class KeyField extends Bean {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public String getId() {
    return this.getString(X._ID);
  }

  public String getCollection() {
    return this.getString("collection");
  }

  public String getQ() {
    return this.getString("q");
  }

  /**
   * Load.
   *
   * @param id
   *          the id
   * @return the key field
   */
  // -------------------
  public static KeyField load(String id) {
    return MongoHelper.load(new BasicDBObject(X._ID, id), KeyField.class);
  }

  /**
   * Load.
   *
   * @param q
   *          the q
   * @param order
   *          the order
   * @param s
   *          the s
   * @param n
   *          the n
   * @return the beans
   */
  public static Beans<KeyField> load(BasicDBObject q, BasicDBObject order, int s, int n) {
    return MongoHelper.load(q, order, s, n, KeyField.class);
  }

  /**
   * Delete all.
   */
  public static void deleteAll() {
    MongoHelper.delete(new BasicDBObject(), KeyField.class);
  }

  /**
   * Delete.
   *
   * @param id
   *          the id
   */
  public static void delete(String id) {
    MongoHelper.delete(new BasicDBObject(X._ID, id), KeyField.class);
  }

  /**
   * Update.
   *
   * @param id
   *          the id
   * @param v
   *          the v
   */
  public static void update(String id, V v) {
    MongoHelper.updateCollection(id, v, KeyField.class);
  }

  /**
   * Creates the.
   *
   * @param collection
   *          the collection
   * @param q
   *          the q
   * @param order
   *          the order
   */
  public static void create(final String collection, final DBObject q, final DBObject order) {
    if (!MongoHelper.isConfigured()) {
      return;
    }

    // log.debug("bean.debug=" + Bean.DEBUG + ", q=" + q);

    new Task() {
      @Override
      public void onExecute() {
        BasicDBObject r = new BasicDBObject();
        if (q != null) {
          for (String s : q.keySet()) {
            Object v = q.get(s);

            if (!X._ID.equals(s) && !s.startsWith("$") && !r.containsField(s)) {
              r.append(s, 1);
            }

            if (v instanceof DBObject) {
              general((DBObject) v, r);
            } else if (v instanceof List) {
              general((List) v, r);
            }

          }
        }

        if (order != null) {
          for (String s : order.keySet()) {
            Object v = order.get(s);

            if (!X._ID.equals(s) && !s.startsWith("$") && !r.containsField(s)) {
              r.append(s, 1);
            }

            if (v instanceof DBObject) {
              general((DBObject) v, r);
            } else if (v instanceof List) {
              general((List) v, r);
            }

          }
        }

        if (r.size() > 0) {
          try {
            String id = UID.id(collection, r);
            if (!exists(id)) {
              log.debug("r=" + r);

              MongoHelper.insertCollection(V.create(X._ID, id).set("collection", collection).set("q", r.toString())
                  .set("created", System.currentTimeMillis()), KeyField.class);
            }
          } catch (Exception e1) {
            log.error(e1.getMessage(), e1);
          }
        }
      }

    }.schedule(0);

  }

  private static boolean exists(String id) throws Exception {
    return MongoHelper.exists(new BasicDBObject(X._ID, id), KeyField.class);
  }

  private static void general(List l, BasicDBObject r) {
    for (int i = 0; i < l.size(); i++) {
      Object v = l.get(i);
      if (v instanceof DBObject) {
        general((DBObject) v, r);
      } else if (v instanceof List) {
        general((List) v, r);
      }
    }
  }

  private static void general(DBObject q, BasicDBObject r) {
    if (q instanceof BasicDBList) {
      BasicDBList l = (BasicDBList) q;
      for (int i = 0; i < l.size(); i++) {
        Object v = l.get(i);
        if (v instanceof DBObject) {
          general((DBObject) v, r);
        } else if (v instanceof List) {
          general((List) v, r);
        }
      }
    } else {
      for (String s : q.keySet()) {
        Object v = q.get(s);
        if (v instanceof DBObject) {
          general((DBObject) v, r);
        } else if (!X._ID.equals(s) && !s.startsWith("$") && !r.containsField(s)) {
          r.append(s, 1);
        }
      }
    }
  }

  /**
   * Run.
   */
  public void run() {
    new Task() {

      @Override
      public void onExecute() {
        try {
          String q = getQ();
          String collection = getCollection();
          JSONObject jo = JSONObject.fromObject(q);
          BasicDBObject keys = new BasicDBObject();
          for (Object name : jo.keySet()) {
            keys.append((String) name, 1);
          }
          DBCollection c = MongoHelper.getCollection(collection);
          if (c != null) {
            c.ensureIndex(keys);
          }
        } catch (Exception e) {
          log.error(e.getMessage(), e);
        }
      }

    }.schedule(0);

    update(this.getId(), V.create("status", "done"));
  }

}
