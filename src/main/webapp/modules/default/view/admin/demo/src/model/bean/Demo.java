package org.giiwa.demo.bean;

import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.DBMapping;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;

import com.mongodb.BasicDBObject;

/**
 * Demo bean
 * 
 * @author joe
 * 
 */
@DBMapping(collection = "tbldemo")
public class Demo extends Bean {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public String getId() {
    return this.getString(X._ID);
  }

  public String getName() {
    return this.getString("name");
  }

  public String getContent() {
    return this.getString("content");
  }

  // ------------

  public static String create(V v) {
    /**
     * generate a unique id in distribute system
     */
    String id = "d" + UID.next("demo.id");
    try {
      while (exists(id)) {
        id = "d" + UID.next("demo.id");
      }
      Bean.insert(v.set(X._ID, id), Demo.class);
      return id;
    } catch (Exception e1) {
      log.error(e1.getMessage(), e1);
    }
    return null;
  }

  public static boolean exists(String id) {
    try {
      return Bean.exists(new BasicDBObject(X._ID, id), Demo.class);
    } catch (Exception e1) {
      log.error(e1.getMessage(), e1);
    }
    return false;
  }

  public static int update(String id, V v) {
    return Bean.updateCollection(id, v, Demo.class);
  }

  public static Beans<Demo> load(BasicDBObject q, int s, int n) {
    return Bean.load(q, new BasicDBObject(X._ID, 1), s, n, Demo.class);
  }

  public static Demo load(String id) {
    return Bean.load(new BasicDBObject(X._ID, id), Demo.class);
  }

  public static void delete(String id) {
    Bean.delete(new BasicDBObject(X._ID, id), Demo.class);
  }

  public static void cleanup() {
    // TODO Auto-generated method stub
    // just for demo
  }

}
