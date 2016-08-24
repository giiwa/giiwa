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
package org.giiwa.app.web;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;
import org.giiwa.core.json.JSON;
import org.giiwa.framework.bean.*;
import org.giiwa.framework.web.*;

/**
 * web api：/upload <br>
 * used to upload file and return the file id in file repository, it support
 * "resume“ file upload, the "Content-Range: bytes 0-1024/2048"
 * 
 * @author joe
 * 
 */
public class upload extends Model {

  /*
   * (non-Javadoc)
   * 
   * @see org.giiwa.framework.web.Model#onPost()
   */
  @Override
  public void onPost() {

    JSON jo = new JSON();
    User me = this.getUser();

    if (me != null) {
      // String access = Module.home.get("upload.require.access");

      FileItem file = this.getFile("file");
      store(me.getId(), file, jo);

    } else {
      this.set(X.ERROR, HttpServletResponse.SC_UNAUTHORIZED);
      jo.put(X.MESSAGE, lang.get("login.required"));
    }

    // /**
    // * test
    // */
    // jo.put("error", "error");
    this.response(jo);

  }

  private boolean store(long me, FileItem file, JSON jo) {
    String tag = this.getString("tag");

    try {
      String range = this.getHeader("Content-Range");
      if (range == null) {
        range = this.getString("Content-Range");
      }
      long position = 0;
      long total = 0;
      String lastModified = this.getHeader("lastModified");
      if (X.isEmpty(lastModified)) {
        lastModified = this.getString("lastModified");
      }
      if (X.isEmpty(lastModified)) {
        lastModified = this.getString("lastModifiedDate");
      }

      if (range != null) {

        // bytes 0-9999/22775650
        String[] ss = range.split(" ");
        if (ss.length > 1) {
          range = ss[1];
        }
        ss = range.split("-|/");
        if (ss.length == 3) {
          position = X.toLong(ss[0]);
          total = X.toLong(ss[2]);
        }

        // log.debug(range + ", " + position + "/" + total);
      }

      String id = UID.id(me, tag, file.getName(), total, lastModified);

      log.debug("storing, id=" + id + ", name=" + file.getName() + ", tag=" + tag + ", total=" + total + ", last="
          + lastModified);

      String share = this.getString("share");
      String folder = this.getString("folder");

      long pos = Repo.store(folder, id, file.getName(), tag, position, total, file.getInputStream(), -1,
          !"no".equals(share), me);
      if (pos >= 0) {
        if (jo == null) {
          this.put("url", "/repo/" + id + "/" + file.getName());
          this.put(X.ERROR, 0);
          this.put("repo", id);
          if (total > 0) {
            this.put("name", file.getName());
            this.put("pos", pos);
            this.put("size", total);
          }
        } else {
          jo.put("url", "/repo/" + id + "/" + file.getName());
          jo.put("repo", id);
          jo.put(X.ERROR, 0);
          if (total > 0) {
            jo.put("name", file.getName());
            jo.put("pos", pos);
            jo.put("size", total);
          }
        }

        // Session.load(sid()).set("access.repo." + id, 1).store();
      } else {
        if (jo == null) {
          this.set(X.ERROR, HttpServletResponse.SC_BAD_REQUEST);
          this.put(X.MESSAGE, lang.get("repo.locked"));
        } else {
          jo.put(X.ERROR, HttpServletResponse.SC_BAD_REQUEST);
          jo.put(X.MESSAGE, lang.get("repo.locked"));
        }
        return false;
      }
      return true;
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      OpLog.error(upload.class, "", e.getMessage(), e, login, this.getRemoteHost());

      if (jo == null) {
        this.set(X.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        this.put(X.MESSAGE, lang.get(e.getMessage()));
      } else {
        jo.put(X.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        jo.put(X.MESSAGE, lang.get(e.getMessage()));
      }
    }

    return false;
  }
}
