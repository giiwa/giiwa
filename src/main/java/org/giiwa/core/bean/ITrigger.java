package org.giiwa.core.bean;

import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;

public interface ITrigger {

  default void beforeInsert(String db, String table, V v) {
    // do nothing
  };

  default void beforeUpdate(String db, String table, W q, V v) {
    // do nothing
  };

  default void beforeDelete(String db, String table, W q) {
    // do nothing
  };

}
