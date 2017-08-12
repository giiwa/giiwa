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
package org.giiwa.framework.web.view;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.giiwa.core.base.IOUtil;
import org.giiwa.core.bean.X;
import org.giiwa.framework.web.Language;
import org.giiwa.framework.web.Model;

public class FileView extends View {

  /**
   * copy the file to front-end, and {giiwa}/html/ too
   */
  @Override
  public boolean parse(File file, Model m, String viewname) throws IOException {

    InputStream in = null;
    try {
      in = new FileInputStream(file);
      /**
       * copy the local html first
       */
      File f1 = new File(Model.GIIWA_HOME + "/html/" + viewname);
      if (!f1.exists()) {
        f1.getParentFile().mkdirs();
        FileOutputStream out1 = new FileOutputStream(f1);
        IOUtil.copy(in, out1);
      }

      in = new FileInputStream(file);
      m.setContentType(Model.getMimeType(file.getName()));

      String date = m.getHeader("If-Modified-Since");

      String range = m.getHeader("Range");
      String date2 = Language.getLanguage().format(file.lastModified(), "yyyy-MM-dd HH:mm:ss z");
      if (X.isEmpty(range) && date != null && date.equals(date2)) {
        m.resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        return true;
      }

      long total = f1.length();
      long start = 0;
      long end = total;
      if (!X.isEmpty(range)) {
        String[] ss = X.split(range, "[=-]");
        if (ss.length > 1) {
          start = X.toLong(ss[1]);
        }

        if (ss.length > 2) {
          end = Math.min(total, X.toLong(ss[2]));

          if (end < start) {
            end = start + 16 * 1024;
          }
        }
      }

      if (end > total) {
        end = total;
      }

      long length = end - start;

      m.setHeader("Content-Length", Long.toString(length));
      m.setHeader("Last-Modified", date2);
      m.setHeader("Content-Range", "bytes " + start + "-" + (end - 1) + "/" + total);
      if (start == 0) {
        m.setHeader("Accept-Ranges", "bytes");
      }
      if (end < total) {
        m.setStatus(206);
      }

      IOUtil.copy(new FileInputStream(f1), m.getOutputStream(), start, end, true);

      return true;
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
