package org.giiwa.framework.web;

import java.util.HashMap;
import java.util.Map;

public class URL {

  private static final Map<String, String> rewrites = new HashMap<String, String>();

  /**
   * force rewrite some url to new
   * 
   * @param originalurl
   *          the original url
   * @param newurl
   *          the new url
   */
  public static void rewrite(String originalurl, String newurl) {
    rewrites.put(originalurl, newurl);
  }

  public static String rewrite(String uri) {
    if (rewrites.containsKey(uri)) {
      return rewrites.get(uri);
    }
    return uri;
  }

}
