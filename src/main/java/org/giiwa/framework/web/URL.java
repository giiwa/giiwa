package org.giiwa.framework.web;

import org.giiwa.core.conf.Global;

public class URL {

  /**
   * force rewrite some url to new
   * 
   * @param originalurl
   *          the original url
   * @param newurl
   *          the new url
   */
  public static void rewrite(String originalurl, String newurl) {
    Global.setConfig("rewrite/" + originalurl, newurl);
  }

  public static String rewrite(String uri) {
    return Global.getString("rewrite/" + uri, uri);
  }

}
