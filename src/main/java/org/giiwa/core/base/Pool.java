package org.giiwa.core.base;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.TimeStamp;
import org.giiwa.core.task.Task;

/**
 * A general pool class, that can be for database , or something else
 * 
 * @author joe
 *
 * @param <E>
 */
public class Pool<E> {
  static Log              log     = LogFactory.getLog(Pool.class);

  private List<E>         list    = new ArrayList<E>();
  private int             initial = 10;
  private int             max     = 10;
  private int             created = 0;

  private IPoolFactory<E> factory = null;

  /**
   * create a pool by initial, max and factory
   * 
   * @param initial
   * @param max
   * @param factory
   * @return
   */
  public static <T> Pool<T> create(int initial, int max, IPoolFactory<T> factory) {
    Pool<T> p = new Pool<T>();
    p.initial = initial;
    p.max = max;
    p.factory = factory;
    new Task() {
      @Override
      public void onExecute() {
        p.init();
      }
    }.schedule(0);
    return p;
  }

  private synchronized void init() {
    for (int i = 0; i < initial; i++) {
      E t = factory.create();
      if (t != null) {
        list.add(t);
      }
    }
    created = list.size();
  }

  /**
   * release a object to the pool
   * 
   * @param t
   */
  public synchronized void release(E t) {
    if (t == null) {
      created--;
    } else {
      factory.cleanup(t);
      list.add(t);
    }
  }

  /**
   * destroy the pool, and destroy all the object in the pool
   */
  public synchronized void destroy() {
    for (E e : list) {
      factory.destroy(e);
    }
    list.clear();

  }

  /**
   * get a object from the pool, if meet the max, then wait till timeout
   * 
   * @param timeout
   * @return
   */
  public synchronized E get(long timeout) {
    try {
      TimeStamp t = TimeStamp.create();

      long t1 = timeout;

      while (t1 > 0) {
        if (list.size() > 0) {
          return list.remove(0);
        } else {
          t1 = timeout - t.past();
          if (t1 > 0) {

            log.debug("t1=" + t1);

            if (created < max) {
              new Task() {

                @Override
                public void onExecute() {
                  E e = factory.create();
                  if (e != null) {
                    created++;
                    release(e);
                  }

                }
              }.schedule(0);
            }

            this.wait(t1);

          }
        }

      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    return null;
  }

  /**
   * the pool factory interface using to create E object in pool
   * 
   * @author wujun
   *
   * @param <T>
   */
  public interface IPoolFactory<T> {

    /**
     * create a object
     * 
     * @return
     */
    public T create();

    /**
     * clean up a object after used
     * 
     * @param t
     */
    public void cleanup(T t);

    /**
     * destroy a object
     * 
     * @param t
     */
    public void destroy(T t);
  }
}
