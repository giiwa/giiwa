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
  static Log           log     = LogFactory.getLog(Pool.class);

  private List<E>      list    = new ArrayList<E>();
  private int          initial = 10;
  private int          max     = 10;
  private int          created = 0;
  @SuppressWarnings("rawtypes")
  private IPoolFactory creator;

  public static <T> Pool<T> create(int initial, int max, IPoolFactory<T> creator) {
    Pool<T> p = new Pool<T>();
    p.initial = initial;
    p.max = max;
    p.creator = creator;
    p.init();
    return p;
  }

  @SuppressWarnings("unchecked")
  private synchronized void init() {
    for (int i = 0; i < initial; i++) {
      E t = (E) creator.create();
      if (t != null) {
        list.add(t);
      }
    }
    created = list.size();
  }

  @SuppressWarnings("unchecked")
  public synchronized void release(E t) {
    if (t == null) {
      created--;
    } else {
      creator.cleanup(t);
      list.add(t);
    }
  }

  @SuppressWarnings("unchecked")
  public synchronized E get(long timeout) {
    try {
      TimeStamp t = TimeStamp.create();

      long t1 = timeout;

      while (timeout > 0) {
        if (list.size() > 0) {
          return list.remove(0);
        } else {
          t1 = timeout - t.past();
          if (t1 > 0) {
            this.wait(t1);
          }

          if (created < max) {
            new Task() {

              @Override
              public void onExecute() {
                E e = (E) creator.create();
                if (e != null) {
                  created++;
                  release(e);
                }

              }
            }.schedule(0);
          }
        }

      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    return null;
  }

  public interface IPoolFactory<T> {
    public T create();

    public void cleanup(T t);

  }
}
