/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
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
import org.giiwa.core.bean.Column;
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.bean.Table;

/**
 * The code bean, used to store special code linked with s1 and s2 fields
 * table="gi_code"
 * 
 * @author wujun
 *
 */
@Table(name = "gi_code")
public class Code extends Bean {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  @Column(name = "s1")
  private String            s1;

  @Column(name = "s2")
  private String            s2;

  @Column(name = "expired")
  private long              expired;

  @Column(name = "created")
  private long              created;

  public long getExpired() {
    return expired;
  }

  public static int create(String s1, String s2, V v) {
    W q = W.create("s1", s1).and("s2", s2);
    try {
      if (Helper.exists(q, Code.class)) {
        Helper.delete(q, Code.class);
      }
      return Helper.insert(v.set("s1", s1).set("s2", s2), Code.class);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return -1;
  }

  public static Code load(String s1, String s2) {
    return Helper.load(W.create("s1", s1).and("s2", s2), Code.class);
  }

  public static void delete(String s1, String s2) {
    Helper.delete(W.create("s1", s1).and("s2", s2), Code.class);
  }

  public static Code load(W q) {
    return Helper.load(q, Code.class);
  }

  public static int update(W q, V v) {
    return Helper.update(q, v, Code.class);
  }
}
