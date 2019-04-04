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
package org.giiwa.app.web.admin;

import org.giiwa.framework.bean.*;
import org.giiwa.framework.web.*;

/**
 * web api: <a href='/admin' target='_blank'>/admin</a> <br>
 * used to show home of admin
 * 
 * @author joe
 *
 */
public class index extends Model {

  /*
   * (non-Javadoc)
   * 
   * @see org.giiwa.framework.web.Model.onGet()
   */
  @Override
  @Path(login = true, method = Model.METHOD_GET)
  public void onGet() {
    /**
     * let's post method to handle it
     */
    onPost();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.giiwa.framework.web.Model.onPost()
   */
  @Path(login = true, method = Model.METHOD_POST)
  public void onPost() {

    User me = this.getUser();
    /**
     * put the user in mode
     */
    this.put("me", me);

    /**
     * show view ...
     */
    this.show("/admin/index.html");

  }

}
