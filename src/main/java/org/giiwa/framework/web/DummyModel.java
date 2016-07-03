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

import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;
import org.giiwa.core.bean.Bean;
import org.giiwa.core.conf.Global;

// TODO: Auto-generated Javadoc
/**
 * default model for which model has not found
 * 
 * @author yjiang
 * 
 */
public class DummyModel extends Model {

  /*
   * (non-Javadoc)
   * 
   * @see com.giiwa.framework.web.Model#onGet()
   */
  @Override
  public void onGet() {
    onPost();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.giiwa.framework.web.Model#onPost()
   */
  @Override
  public void onPost() {
    /**
     * if the file exists, and the extension is not .html and htm then get back
     * directly, and set contenttype
     */
    // log.debug("uri=" + uri);

    File f = Module.home.loadResource(uri);
    if (f == null || !f.exists()) {
      int i = uri.lastIndexOf("_v");
      if (i > 0) {
        int j = uri.lastIndexOf(".");
        if (j > 0) {
          uri = uri.substring(0, i) + uri.substring(j);
          f = Module.home.loadResource(uri);
        }
      }
    }

    if (f != null && f.exists()) {
      /**
       * this file exists, check is end with ".htm|.html"
       */
      if (uri.endsWith(".htm") || uri.endsWith(".html") || uri.endsWith(".jsp")) {
        /**
         * parse it as template
         */
        this.set(this.getJSON());

        this.set("me", this.getUser());
        this.put("lang", lang);
        this.put("uri", uri);
        this.put("module", Module.home);
        this.put("path", path);
        this.put("request", req);
        this.put("this", this);
        this.put("response", resp);
        this.set("session", this.getSession());
        this.set("system", Global.getInstance());

        show(uri);

        return;
      } else if (f.isFile()) {
        /**
         * copy the file directly
         */
        // log.debug(f.getAbsolutePath());

        InputStream in = null;
        OutputStream out = null;
        try {
          in = new FileInputStream(f);
          out = resp.getOutputStream();
          this.setContentType(getMimeType(uri));

          String date = this.getHeader("If-Modified-Since");
          String date2 = lang.format(f.lastModified(), "yyyy-MM-dd HH:mm:ss z");
          if (date != null && date.equals(date2)) {
            resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
          }

          this.setHeader("Last-Modified", date2);
          this.setHeader("Content-Length", Long.toString(f.length()));
          this.setHeader("Accept-Ranges", "bytes");

          // RANGE: bytes=2000070-
          String range = this.getHeader("RANGE");
          long start = 0;
          long end = f.length();
          if (range != null) {
            String[] ss = range.split("=| |-");
            if (ss.length > 1) {
              start = Bean.toLong(ss[1]);
            }
            if (ss.length > 2) {
              end = Bean.toLong(ss[2]);
            }
            // Content-Range=bytes 2000070-106786027/106786028
            this.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + f.length());

          }

          Model.copy(in, out, start, end, false);
          out.flush();

          return;
        } catch (Exception e) {
          log.error(uri, e);
        } finally {
          if (in != null) {
            try {
              in.close();
            } catch (IOException e) {
              log.error(e);
            }
          }
        }
      }
    }

    // check where has .html or htm
    Template t1 = getTemplate(uri + ".html", false);
    if (t1 != null) {
      this.set(this.getJSON());
      show(uri + ".html");

      return;
    }

    // check where has .html or htm
    t1 = getTemplate(uri + ".htm", false);
    if (t1 != null) {
      this.set(this.getJSON());
      show(uri + ".htm");

      return;
    }

    // not found
    this.notfound();

  }

}
