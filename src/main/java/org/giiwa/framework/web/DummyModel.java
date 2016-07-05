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

    File f = Module.home.getFile(uri);
    if (f != null && f.exists()) {
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
    }

    // not found
    this.notfound();

  }

}
