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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.giiwa.core.bean.X;
import org.giiwa.framework.bean.OpLog;
import org.giiwa.framework.bean.Temp;
import org.giiwa.framework.web.Model;

/**
 * web api： /temp <br>
 * used to access temporary file which created by Temp
 * 
 * @author joe
 * 
 */
public class temp extends Model {

  /*
   * (non-Javadoc)
   * 
   * @see org.giiwa.framework.web.Model#onGet()
   */
  @SuppressWarnings("deprecation")
  public void onGet() {

    log.debug("temp: " + this.path);
    if (this.path == null) {
      this.notfound();
      return;
    }

    String[] ss = this.path.split("/");
    if (ss.length != 2) {
      this.notfound();
      return;
    }

    String name = ss[1];
    File f = Temp.get(ss[0], name);
    if (!f.exists()) {
      this.notfound();
      return;
    }

    try {
      String range = this.getString("RANGE");
      long total = f.length();
      long start = 0;
      long end = total;
      if (range != null) {
        ss = range.split("(=|-)");
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

      this.setContentType("application/octet");

      this.addHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");

      InputStream in = new FileInputStream(f);
      OutputStream out = this.getOutputStream();
      Model.copy(in, out, start, end, true);

      return;

    } catch (Exception e) {
      log.error(f.getAbsolutePath(), e);
      OpLog.error(temp.class, "", e.getMessage(), e, login, this.getRemoteHost());
    }

    this.notfound();
  }

}
