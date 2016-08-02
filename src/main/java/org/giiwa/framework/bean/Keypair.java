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

import org.giiwa.core.base.RSA;
import org.giiwa.core.base.RSA.Key;
import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.bean.Table;
import org.giiwa.core.bean.X;

// TODO: Auto-generated Javadoc
/**
 * RSA key pair. <br>
 * collection="gi_keypair"
 * 
 * @author joe
 *
 */
@Table(name = "gi_keypair")
public class Keypair extends Bean {

  /**
  * 
  */
  private static final long serialVersionUID = 1L;

  // long created;
  // String memo;
  // int length;
  //
  // String pubkey;
  // String prikey;

  /**
   * Creates the.
   * 
   * @param length
   *          the length
   * @param memo
   *          the memo
   * @return the long
   */
  public static long create(int length, String memo) {

    Key k = RSA.generate(length);
    if (k != null) {
      long created = System.currentTimeMillis();
      if (Helper.insert(V.create(X.ID, created).set("created", created).set("length", length).set("memo", memo)
          .set("pubkey", k.pub_key).set("prikey", k.pri_key), Keypair.class) > 0) {
        return created;
      }
    }

    return 0;
  }

  /**
   * Load.
   * 
   * @param s
   *          the s
   * @param n
   *          the n
   * @return the beans
   */
  public static Beans<Keypair> load(int s, int n) {
    return Helper.load(W.create(), s, n, Keypair.class);
  }

  /**
   * Update.
   * 
   * @param created
   *          the created
   * @param v
   *          the v
   * @return the int
   */
  public static int update(long created, V v) {
    return Helper.update(created, v, Keypair.class);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.giiwa.bean.Bean#load(java.sql.ResultSet)
   */
  // @Override
  // protected void load(ResultSet r) throws SQLException {
  // created = r.getLong("created");
  // memo = r.getString("memo");
  // length = r.getInt("length");
  //
  // pubkey = r.getString("pubkey");
  // prikey = r.getString("prikey");
  // }

  public long getCreated() {
    return getLong("created");
  }

  public String getMemo() {
    return getString("memo");
  }

  public int getLength() {
    return getInt("length");
  }

  public String getPubkey() {
    return getString("pubkey");
  }

  public String getPrikey() {
    return getString("prikey");
  }

  /**
   * Load.
   * 
   * @param created
   *          the created
   * @return the keypair
   */
  public static Keypair load(long created) {
    return Helper.load(W.create(X.ID, created), Keypair.class);
  }

  /**
   * Delete.
   *
   * @param created
   *          the created
   */
  public static void delete(long created) {
    Helper.delete(W.create(X.ID, created), Keypair.class);
  }

}
