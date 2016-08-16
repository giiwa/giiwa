package org.giiwa.framework.bean;

import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.Column;
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.bean.Table;

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
}
