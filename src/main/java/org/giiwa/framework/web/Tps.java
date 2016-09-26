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
package org.giiwa.framework.web;

import java.util.concurrent.atomic.AtomicInteger;

public class Tps {

  static Tps    owner = new Tps();

  long          time  = System.currentTimeMillis();
  int           last;
  AtomicInteger current;
  int           max;

  public static void add(int delta) {
    if (System.currentTimeMillis() - owner.time > 1000) {
      owner.time = System.currentTimeMillis();
      owner.last = owner.current.get();
      if (owner.last > owner.max) {
        owner.max = owner.last;
      }
      owner.current.set(0);
    }
    owner.current.addAndGet(delta);
  }

  public static int get() {
    if (System.currentTimeMillis() - owner.time > 1000) {
      owner.time = System.currentTimeMillis();
      owner.last = owner.current.get();
      if (owner.last > owner.max) {
        owner.max = owner.last;
      }
      owner.current.set(0);
    }
    return owner.last;
  }

  public static int max() {
    return owner.max;
  }

}
