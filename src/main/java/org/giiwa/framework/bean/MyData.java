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

import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.DBMapping;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;

import com.mongodb.BasicDBObject;

// TODO: Auto-generated Javadoc
/**
 * MyData, used to record user customer data, please refer model ("/mydata").
 * <br>
 * collection="gi_mydata"
 * 
 * @author joe
 *
 */
@DBMapping(collection = "gi_mydata")
public class MyData extends Bean {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public long getId() {
    return this.getLong(X._ID);
  }

  public long getUid() {
    return this.getLong("uid");
  }

  public String getTable() {
    return this.getString("table");
  }

  /**
   * Creates the.
   *
   * @param uid
   *          the uid
   * @param table
   *          the table
   * @param v
   *          the v
   * @return the long
   */
  public static long create(long uid, String table, V v) {
    long id = UID.next("userdata.id");

    while (Bean.exists(new BasicDBObject(X._ID, id), MyData.class)) {
      id = UID.next("userdata.id");
    }
    Bean.insertCollection(v.set(X._ID, id, true).set("id", id, true).set("uid", uid, true).set("table", table, true),
        MyData.class);
    return id;
  }

  /**
   * Update.
   *
   * @param id
   *          the id
   * @param uid
   *          the uid
   * @param v
   *          the v
   * @return the int
   */
  public static int update(long id, long uid, V v) {
    return Bean.updateCollection(new BasicDBObject(X._ID, id).append("uid", uid), v, MyData.class);
  }

  /**
   * Removes the.
   *
   * @param id
   *          the id
   * @param uid
   *          the uid
   * @return the int
   */
  public static int remove(long id, long uid) {
    return Bean.delete(new BasicDBObject(X._ID, id).append("uid", uid), MyData.class);
  }

  /**
   * Load.
   *
   * @param id
   *          the id
   * @param uid
   *          the uid
   * @return the my data
   */
  public static MyData load(long id, long uid) {
    return Bean.load(new BasicDBObject(X._ID, id).append("uid", uid), MyData.class);
  }

  /**
   * Load.
   *
   * @param uid
   *          the uid
   * @param table
   *          the table
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
  public static Beans<MyData> load(long uid, String table, BasicDBObject q, BasicDBObject order, int s, int n) {
    if (q == null) {
      q = new BasicDBObject();
    }
    return Bean.load(q.append("uid", uid).append("table", table), order, s, n, MyData.class);
  }

}
