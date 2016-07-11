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

import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;

/**
 * web api: /device <br>
 * used to test the device user agent
 * 
 * @author joe
 *
 */
public class device extends Model {

  /*
   * (non-Javadoc)
   * 
   * @see org.giiwa.framework.web.Model#onGet()
   */
  @Path()
  public void onGet() {
    // AccessLog.create(this.getRemoteHost(), this.path, V.create("agent",
    // this.browser()).set("status", 200));

    this.set("ip", this.getRemoteHost());
    this.set("headers", this.getHeaders());
    this.set("node", Model.node());

    this.set("host", this.getHost());
    this.set("port", this.getPort());
    this.set("remote", this.getRemoteHost());

    this.show("/device.html");
  }
}
