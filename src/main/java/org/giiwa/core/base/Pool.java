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
package org.giiwa.core.base;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.TimeStamp;
import org.giiwa.core.task.Task;

/**
 * A general pool class, that can be for database , or something else
 * 
 * @author joe
 *
 */
public class Pool<E> {
  static Log              log     = LogFactory.getLog(Pool.class);

  private ReentrantLock   lock    = new ReentrantLock();
  private Condition       door    = lock.newCondition();

  private List<E>         list    = new ArrayList<E>();
  private int             initial = 10;
  private int             max     = 10;
  private int             created = 0;

  private IPoolFactory<E> factory = null;

  /**
   * create a pool by initial, max and factory.
   *
   * @param <E>
   *          the element type
   * @param initial
   *          the initial
   * @param max
   *          the max
   * @param factory
   *          the factory
   * @return the pool
   */
  public static <E> Pool<E> create(int initial, int max, IPoolFactory<E> factory) {
    Pool<E> p = new Pool<E>();
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

  private void init() {
    for (int i = 0; i < initial; i++) {
      E t = factory.create();
      if (t != null) {
        try {
          lock.lock();
          list.add(t);
          door.signal();
        } finally {
          lock.unlock();
        }
      }
    }
    created = list.size();
  }

  /**
   * release a object to the pool.
   *
   * @param t
   *          the t
   */
  public void release(E t) {
    if (t == null) {
      created--;
    } else {
      factory.cleanup(t);
      try {
        lock.lock();
        list.add(t);
        door.signal();
      } finally {
        lock.unlock();
      }
    }
  }

  /**
   * destroy the pool, and destroy all the object in the pool.
   */
  public void destroy() {
    synchronized (list) {
      for (E e : list) {
        factory.destroy(e);
      }
      list.clear();
    }
  }

  /**
   * get a object from the pool, if meet the max, then wait till timeout.
   *
   * @param timeout
   *          the timeout
   * @return the e
   */
  public E get(long timeout) {
    try {
      TimeStamp t = TimeStamp.create();

      long t1 = timeout;

      try {
        lock.lock();

        while (t1 > 0) {
          if (list.size() > 0) {
            return list.remove(0);
          } else {
            t1 = timeout - t.past();
            if (t1 > 0) {

              // log.debug("t1=" + t1);
              //
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

              door.awaitNanos(TimeUnit.MILLISECONDS.toNanos(t1));
            }
          }
        }
      } finally {
        lock.unlock();
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
   * @param <E>
   *          the Object
   */
  public interface IPoolFactory<E> {

    /**
     * create a object.
     *
     * @return the e
     */
    public E create();

    /**
     * clean up a object after used.
     *
     * @param t
     *          the t
     */
    public void cleanup(E t);

    /**
     * destroy a object.
     *
     * @param t
     *          the t
     */
    public void destroy(E t);
  }
}
