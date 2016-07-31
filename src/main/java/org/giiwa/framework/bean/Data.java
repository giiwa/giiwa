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
package org.giiwa.framework.bean;

import net.sf.json.JSONObject;

import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Table;
import org.giiwa.core.bean.X;

import com.mongodb.BasicDBObject;

// TODO: Auto-generated Javadoc
/**
 * Freedom data storage, not specify.
 * <br>
 * collection=""
 * 
 * @author wujun
 *
 */
@Table(collection = "gi_data")
public class Data extends Bean {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public String getId() {
    return this.getString(X._ID);
  }

  /**
   * Load.
   *
   * @param collection
   *          the collection
   * @param query
   *          the query
   * @param order
   *          the order
   * @param s
   *          the s
   * @param n
   *          the n
   * @return the beans
   */
  public static Beans<Data> load(String collection, BasicDBObject query, BasicDBObject order, int s, int n) {
    return Bean.load(collection, query, order, s, n, Data.class);
  }

  /**
   * Load.
   *
   * @param collection
   *          the collection
   * @param id
   *          the id
   * @return the data
   */
  public static Data load(String collection, String id) {
    Data d = Bean.load(collection, new BasicDBObject(), Data.class);
    if (d != null) {
      Object _id = id;
      Object o = d.get(X._ID);
      if (o instanceof Long) {
        _id = Bean.toLong(id);
      }
      return Bean.load(collection, new BasicDBObject().append(X._ID, _id), Data.class);
    }
    return null;
  }

  /**
   * Update.
   *
   * @param collection
   *          the collection
   * @param jo
   *          the jo
   * @return the int
   */
  public static int update(String collection, JSONObject jo) {
    V v = V.create();

    Object id = jo.get(X._ID);
    jo.remove(X._ID);

    for (Object name : jo.keySet()) {
      v.set(name.toString(), jo.get(name));
    }

    Data d = Bean.load(collection, new BasicDBObject(), Data.class);

    if (d != null) {

      if (Bean.load(collection, new BasicDBObject(X._ID, id)) == null) {
        // new , insert
        log.debug("inserted: " + v);
        return Bean.insertCollection(collection, v.set(X._ID, id), null);
      } else {
        return Bean.updateCollection(collection, id, v);
      }
    } else {
      log.debug("inserted: " + v);
      return Bean.insertCollection(collection, v.set(X._ID, id), null);
    }
  }

}
