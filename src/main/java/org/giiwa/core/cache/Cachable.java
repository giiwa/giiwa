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
package org.giiwa.core.cache;

import java.io.Serializable;

// TODO: Auto-generated Javadoc
/**
 * The Interface Cachable.
 */
public interface Cachable extends Serializable {

  /**
   * Age.
   *
   * @return the long
   * @deprecated
   */
  public long age();

  /**
   * set the expired time by second, -1 never expired
   * 
   * @param t
   *          the time of seconds
   */
  public void setExpired(int t);

  /**
   * check whether expired.
   *
   * @return boolean
   */
  public boolean expired();

}
