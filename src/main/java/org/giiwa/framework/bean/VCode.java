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
import org.giiwa.core.bean.Table;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;

import com.mongodb.BasicDBObject;

// TODO: Auto-generated Javadoc
@Table(collection = "gi_vcode")
public class VCode extends Bean {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public String getCode() {
    return this.getString("code");
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.giiwa.core.cache.DefaultCachable#expired()
   */
  public boolean expired() {
    return this.getLong("expired") < System.currentTimeMillis();
  }

  // --------------

  /**
   * Creates the.
   *
   * @param name
   *          the name
   * @param ip
   *          the ip
   * @param sid
   *          the sid
   * @param len
   *          the len
   * @param expiredtime
   *          the expiredtime
   * @return the string
   */
  public static String create(String name, String ip, String sid, int len, long expiredtime) {
    VCode c = VCode.load(new BasicDBObject("name", name).append("ip", ip).append("sid", sid).append("len", len)
        .append("created", new BasicDBObject("$gt", System.currentTimeMillis() - X.AMINUTE)));

    if (c != null) {
      return c.getCode();
    }

    String code = UID.digital(len);
    String id = UID.id(code, name, ip, sid, len, System.currentTimeMillis());

    try {
      while (Bean.exists(new BasicDBObject(X._ID, id), VCode.class)) {
        id = UID.id(code, name, ip, sid, len, System.currentTimeMillis());
      }

      V v = V.create(X._ID, id).set("name", name).set("ip", ip).set("sid", sid).set("code", code).set("len", len)
          .set("expired", expiredtime);
      Bean.insertCollection(v, VCode.class);

      return code;
    } catch (Exception e1) {
      log.error(e1.getMessage(), e1);
    }
    return null;
  }

  /**
   * Load.
   *
   * @param q
   *          the q
   * @return the v code
   */
  public static VCode load(BasicDBObject q) {
    return Bean.load(q, VCode.class);
  }

  /**
   * verify the name and code pair.
   *
   * @param name
   *          the name
   * @param code
   *          the code
   * @return 1: ok <br>
   *         -1: invalid code <br>
   *         -2: expired
   */
  public static int verify(String name, String code) {
    VCode c = load(new BasicDBObject("name", name).append("code", code));
    if (c == null) {
      // invalid code
      return -1;
    }

    if (c.expired()) {
      // expired
      return -2;
    }

    // ok
    return 1;
  }
}
