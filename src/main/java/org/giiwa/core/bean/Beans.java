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
package org.giiwa.core.bean;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.cache.DefaultCachable;

/**
 * The {@code Beans} Class used to contains the Bean in query. <br>
 * it's includes the total count for the query
 * 
 * @param <T>
 *          the generic type
 */
public class Beans<T extends Object> extends DefaultCachable {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 2L;

  /** The log. */
  protected static Log      log              = LogFactory.getLog(Beans.class);

  /** The total. */
  int                       total            = -1;

  /** The list. */
  List<T>                   list;

  /**
   * Gets the total. please set the total first, than...
   *
   * @return the total
   */
  public int getTotal() {
    return total;
  }

  /**
   * Sets the total.
   *
   * @param total
   *          the new total
   */
  public void setTotal(int total) {
    this.total = total;
  }

  /**
   * Gets the list.
   *
   * @return the list
   */
  public List<T> getList() {
    return list;
  }

  /**
   * Sets the list.
   *
   * @param list
   *          the new list
   */
  public void setList(List<T> list) {
    this.list = list;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "Beans[total=" + total + ", list.size=" + (list == null ? null : list.size()) + "]";
  }

}
