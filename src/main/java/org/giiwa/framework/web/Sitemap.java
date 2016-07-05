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
package org.giiwa.framework.web;

import java.io.*;

import org.apache.commons.logging.*;

/**
 * create sitemap utility
 * 
 * @author yjiang
 * 
 */
public class Sitemap {

  static Log log = LogFactory.getLog(Sitemap.class);

  /**
   * Adds the.
   * 
   * @param uri
   *          the uri
   */
  public static void add(String uri) {

    /**
     * find the sitemap.txt file
     */
    File f = Module.home.getFile("/sitemap.txt");

    BufferedReader in = null;
    PrintStream out = null;
    try {
      /**
       * test has same uri ?, if so, ignore
       */
      in = new BufferedReader(new FileReader(f));
      String line = in.readLine();
      while (line != null) {
        if (line.equals(uri)) {
          return;
        }
      }

      /**
       * append the uri to the sitemap.txt
       */
      out = new PrintStream(new FileOutputStream(f, true));
      out.println(uri);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
          log.error(e);
        }
      }
      if (out != null) {
        out.close();
      }
    }
  }

}
