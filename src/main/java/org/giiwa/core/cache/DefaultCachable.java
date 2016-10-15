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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The Class DefaultCachable.
 */
public class DefaultCachable implements Cachable {

  static Log                log              = LogFactory.getLog(DefaultCachable.class);

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1L;

  /** The _age. */
  private long              _age             = System.currentTimeMillis();

  private long              expired          = -1;

  /*
   * (non-Javadoc)
   * 
   * @see com.giiwa.cache.Cachable#age()
   */
  @Override
  public long age() {
    return System.currentTimeMillis() - _age;
  }

  @Override
  public void setExpired(long expired) {
    this.expired = expired;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.giiwa.core.cache.Cachable#expired()
   */
  public boolean expired() {
    // log.debug("check expired, expired=" + expired + ", now=" +
    // System.currentTimeMillis());

    return expired > 0 && expired < System.currentTimeMillis();
  }
}