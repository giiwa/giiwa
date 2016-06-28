package org.giiwa.framework.web;

public class Tps {

  static Tps owner = new Tps();

  long       time  = System.currentTimeMillis();
  int        last;
  int        current;
  int        max;

  public static void add(int delta) {
    if (System.currentTimeMillis() - owner.time > 1000) {
      owner.time = System.currentTimeMillis();
      owner.last = owner.current;
      if (owner.last > owner.max) {
        owner.max = owner.last;
      }
      owner.current = 0;
    }
    owner.current += delta;
  }

  public static int get() {
    if (System.currentTimeMillis() - owner.time > 1000) {
      owner.time = System.currentTimeMillis();
      owner.last = owner.current;
      if (owner.last > owner.max) {
        owner.max = owner.last;
      }
      owner.current = 0;
    }
    return owner.last;
  }

  public static int max() {
    return owner.max;
  }

}
