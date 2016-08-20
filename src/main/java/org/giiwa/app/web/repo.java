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
package org.giiwa.app.web;

import java.io.*;

import javax.servlet.http.HttpServletResponse;

import org.giiwa.core.base.GImage;
import org.giiwa.core.bean.X;
import org.giiwa.core.json.JSON;
import org.giiwa.framework.bean.*;
import org.giiwa.framework.bean.Repo.Entity;
import org.giiwa.framework.web.*;

/**
 * web api： /repo <br>
 * used to access the file in file repository
 * 
 * @author yjiang
 * 
 */
public class repo extends Model {

  /**
   * Download.
   */
  @SuppressWarnings("deprecation")
  @Path(path = "download", login = true)
  public void download() {
    if (path != null) {
      String id = path;
      Entity e = null;
      // log.debug("e:" + e);

      User me = this.getUser();

      try {

        e = Repo.loadByUri(id);

        /**
         * check the privilege via session, the app will put the access in
         * session according to the app logic
         */
        if (e != null) {
          if (e.isShared() || (me != null)) {

            this.setContentType("application/octet-stream");
            this.addHeader("Content-Disposition", "attachment; filename=\"" + e.getName() + "\"");

            String date2 = lang.format(e.getCreated(), "yyyy-MM-dd HH:mm:ss z");

            /**
             * if not point-transfer, then check the if-modified-since
             */
            String range = this.getString("RANGE");
            if (X.isEmpty(range)) {
              String date = this.getHeader("If-Modified-Since");
              if (date != null && date.equals(date2)) {
                resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return;
              }
            }

            this.addHeader("Last-Modified", date2);

            try {

              String size = this.getString("size");
              if (size != null && size.indexOf("x") < 0) {
                size = lang.get("size." + size);
              }

              if (size != null) {
                String[] ss = size.split("x");

                if (ss.length == 2) {
                  boolean failed = false;
                  File f = Temp.get(id, size);
                  if (!f.exists()) {
                    f.getParentFile().mkdirs();

                    File src = Temp.get(id, null);
                    if (!src.exists()) {
                      src.getParentFile().mkdirs();
                    }
                    OutputStream out = new FileOutputStream(src);
                    Model.copy(e.getInputStream(), out, false);
                    out.close();

                    if (GImage.scale3(src.getAbsolutePath(), f.getAbsolutePath(), X.toInt(ss[0]), X.toInt(ss[1])) < 0) {
                      failed = true;
                      e.reset();
                    }
                  }

                  if (f.exists() && !failed) {
                    InputStream in = new FileInputStream(f);
                    OutputStream out = this.getOutputStream();

                    Model.copy(in, out, false);
                    in.close();
                    return;
                  }
                }
              }

              OutputStream out = this.getOutputStream();
              InputStream in = e.getInputStream();

              long total = e.getTotal() <= 0 ? in.available() : e.getTotal();
              long start = 0;
              long end = total;
              if (range != null) {
                String[] ss = range.split("(=|-)");
                if (ss.length > 1) {
                  start = X.toLong(ss[1]);
                }

                if (ss.length > 2) {
                  end = Math.min(total, X.toLong(ss[2]));
                }
              }

              if (end <= start) {
                end = start + 16 * 1024;
              }

              this.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + total);

              log.info(start + "-" + end + "/" + total);
              Model.copy(in, out, start, end, true);

              return;
            } catch (IOException e1) {
              log.error(e1);
              OpLog.error(repo.class, "download", e1.getMessage(), e1, login, this.getRemoteHost());
            }
          }
        }
      } finally {
        if (e != null) {
          e.close();
        }
      }
    }

    this.notfound();
  }

  /**
   * Delete.
   */
  @SuppressWarnings("deprecation")
  @Path(path = "delete", login = true)
  public void delete() {

    this.setContentType(Model.MIME_JSON);
    JSON jo = new JSON();

    String repo = this.getString("repo");
    Entity e = Repo.loadByUri(repo);
    if (e != null) {
      if (login.hasAccess("access.repo.admin")) {
        e.delete();

        jo.put(X.STATE, 200);
        jo.put(X.MESSAGE, "ok");

      } else {
        jo.put(X.STATE, 201);
        jo.put(X.MESSAGE, "no access");
      }
    } else {
      jo.put(X.STATE, 201);
      jo.put(X.MESSAGE, "parameters error");
    }

    this.response(jo);

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.giiwa.framework.web.Model#onGet()
   */
  @SuppressWarnings("deprecation")
  @Override
  @Path(login = false)
  public void onGet() {
    if ("download".equals(this.getString("op"))) {
      download();
      return;
    }

    /**
     * test session first
     */

    // log.debug("path:" + path);

    if (path != null) {
      String id = path;
      Entity e = null;
      // log.debug("e:" + e);

      // User me = this.getUser();

      try {

        e = Repo.loadByUri(id);

        /**
         * check the privilege via session, the app will put the access in
         * session according to the app logic
         */
        if (e != null) {

          this.setContentType(Model.getMimeType(e.getName()));

          String date2 = lang.format(e.getCreated(), "yyyy-MM-dd HH:mm:ss z");

          this.addHeader("Last-Modified", date2);

          try {

            String size = this.getString("size");

            /**
             * if "size" presented, and has "x"
             */
            if (!X.isEmpty(size)) {
              String[] ss = size.split("x");

              if (ss.length == 2) {
                File f = Temp.get(id, "s_" + size);
                boolean failed = false;

                if (!f.exists()) {
                  f.getParentFile().mkdirs();

                  File src = Temp.get(id, null);
                  if (!src.exists()) {
                    src.getParentFile().mkdirs();
                  } else {
                    src.delete();
                  }
                  OutputStream out = new FileOutputStream(src);
                  Model.copy(e.getInputStream(), out, false);
                  out.close();

                  /**
                   * using scale3 to cut the middle of the image
                   */
                  if (GImage.scale3(src.getAbsolutePath(), f.getAbsolutePath(), X.toInt(ss[0]), X.toInt(ss[1])) < 0) {
                    failed = true;
                    log.warn("scale3 image failed");
                    e.reset();
                  }
                } else {
                  log.debug("load the image from the temp cache, file=" + f.getCanonicalPath());
                }

                if (f.exists() && !failed) {
                  log.debug("load the scaled image from " + f.getCanonicalPath());

                  InputStream in = new FileInputStream(f);
                  OutputStream out = this.getOutputStream();

                  Model.copy(in, out, false);
                  in.close();
                  return;
                }
              }
            }

            /**
             * if "size1" presented, has "x"
             */
            size = this.getString("size1");

            if (!X.isEmpty(size)) {

              String[] ss = size.split("x");

              if (ss.length == 2) {
                boolean failed = false;
                File f = Temp.get(id, "s1_" + size);
                if (!f.exists()) {
                  f.getParentFile().mkdirs();

                  File src = Temp.get(id, null);
                  if (!src.exists()) {
                    src.getParentFile().mkdirs();
                  } else {
                    src.delete();
                  }
                  OutputStream out = new FileOutputStream(src);
                  Model.copy(e.getInputStream(), out, false);
                  out.close();

                  /**
                   * using scale to smooth the original image
                   */
                  if (GImage.scale(src.getAbsolutePath(), f.getAbsolutePath(), X.toInt(ss[0]), X.toInt(ss[1])) < 0) {
                    log.warn("scale3 image failed");
                    failed = true;
                    e.reset();
                  }
                } else {
                  log.debug("load the image from the temp cache, file=" + f.getCanonicalPath());
                }

                if (f.exists() && !failed) {
                  log.debug("load scaled image from " + f.getCanonicalPath());

                  InputStream in = new FileInputStream(f);
                  OutputStream out = this.getOutputStream();

                  Model.copy(in, out, false);
                  in.close();
                  return;
                }
              }
            }

            /**
             * if not point-transfer, then check the if-modified-since
             */
            String range = this.getString("RANGE");
            if (X.isEmpty(range)) {
              String date = this.getHeader("If-Modified-Since");
              if (date != null && date.equals(date2)) {
                this.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return;
              }
            }

            /**
             * else get all repo output to response
             */
            OutputStream out = this.getOutputStream();
            InputStream in = e.getInputStream();

            long total = e.getTotal() <= 0 ? in.available() : e.getTotal();
            long start = 0;
            long end = total;
            if (range != null) {
              String[] ss = range.split("(=|-)");
              if (ss.length > 1) {
                start = X.toLong(ss[1]);
              }

              if (ss.length > 2) {
                end = Math.min(total, X.toLong(ss[2]));
              }
            }

            if (end <= start) {
              end = start + 16 * 1024;
            }

            this.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + total);

            log.info(start + "-" + end + "/" + total);
            Model.copy(in, out, start, end, true);

            return;
          } catch (IOException e1) {
            log.error(e1);
            OpLog.error(repo.class, "", e1.getMessage(), e1, login, this.getRemoteHost());
          }
        }
      } finally {
        if (e != null) {
          e.close();
        }
      }
    }

    this.notfound();

  }

}
