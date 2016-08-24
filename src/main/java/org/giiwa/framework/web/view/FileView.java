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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.giiwa.core.bean.X;
import org.giiwa.framework.web.Language;
import org.giiwa.framework.web.Model;

public class FileView extends View {

  @SuppressWarnings("deprecation")
  @Override
  public boolean parse(File file, Model m) throws IOException {

    InputStream in = null;
    OutputStream out = null;
    try {
      in = new FileInputStream(file);
      out = m.getOutputStream();
      m.setContentType(Model.getMimeType(file.getName()));

      String date = m.getHeader("If-Modified-Since");
      String date2 = Language.getLanguage().format(file.lastModified(), "yyyy-MM-dd HH:mm:ss z");
      if (date != null && date.equals(date2)) {
        m.resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        return true;
      }

      m.setHeader("Last-Modified", date2);
      m.setHeader("Content-Length", Long.toString(file.length()));
      m.setHeader("Accept-Ranges", "bytes");

      // RANGE: bytes=2000070-
      String range = m.getHeader("RANGE");
      long start = 0;
      long end = file.length();
      if (range != null) {
        String[] ss = range.split("=| |-");
        if (ss.length > 1) {
          start = X.toLong(ss[1], 0);
        }
        if (ss.length > 2) {
          end = X.toLong(ss[2], 0);
        }
        // Content-Range=bytes 2000070-106786027/106786028
        m.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + file.length());

      }

      Model.copy(in, out, start, end, false);
      out.flush();

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
