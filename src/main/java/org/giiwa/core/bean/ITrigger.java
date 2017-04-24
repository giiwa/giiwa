package org.giiwa.core.bean;

import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;

public interface ITrigger {

  public void beforeInsert(String db, String table, V v);

  public void beforeUpdate(String db, String table, W q, V v);

  public void beforeDelete(String db, String table, W q);

}
