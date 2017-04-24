package org.giiwa.core.bean;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.task.Task;

public class Optimizer implements Helper.IOptimizer {

  private static Log             log    = LogFactory.getLog(Optimizer.class);

  private static HashSet<String> exists = new HashSet<String>();

  @Override
  public void query(final String db, final String table, final W w) {
    if (w != null && !w.isEmpty()) {
      new Task() {

        @Override
        public void onExecute() {
          LinkedHashMap<String, Integer> keys = w.keys();

          StringBuilder sb = new StringBuilder();
          for (String s : keys.keySet()) {
            if (sb.length() > 0)
              sb.append(",");
            sb.append(s).append(":").append(keys.get(s));
          }

          String id = UID.id(db, table, sb.toString());
          try {
            if (!exists.contains(id)) {
              _init(db, table);
            }

            if (!exists.contains(id)) {
              exists.add(id);
              if (!keys.isEmpty()) {
                Helper.createIndex(db, table, keys);
              }
            }

          } catch (Exception e) {
            log.error("table=" + table + ", keys=" + keys, e);
          }
        }

      }.schedule(0);
    }
  }

  private static void _init(String db, String table) {

    List<Map<String, Object>> l1 = Helper.getIndexes(table, db);

    if (l1 != null && !l1.isEmpty()) {
      for (Map<String, Object> d : l1) {
        Map<String, Object> keys = (Map<String, Object>) d.get("key");
        if (keys != null && !keys.isEmpty()) {
          StringBuilder sb = new StringBuilder();
          for (String s : keys.keySet()) {
            if (sb.length() > 0)
              sb.append(",");
            sb.append(s).append(":").append(keys.get(s));
          }

          String id = UID.id(db, table, sb.toString());
          exists.add(id);
        }
      }
    }
  }

}
