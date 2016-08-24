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

import org.apache.commons.configuration.Configuration;
import org.giiwa.core.conf.Config;
import org.giiwa.framework.web.*;

/**
 * web api: /admin/dashboard <br>
 * used to show dashboard
 * 
 * @author yjiang
 * 
 */
public class dashboard extends Model {

  /*
   * (non-Javadoc)
   * 
   * @see org.giiwa.framework.web.Model#onGet()
   */
  @Override
  @Path(login = true, access = "access.config.admin")
  public void onGet() {

    Configuration conf = Config.getConfig();

    this.set("me", this.getUser());
    this.set("uptime", lang.format(Model.UPTIME, "yy-MM-dd"));
    this.set("past", lang.past(Model.UPTIME));
    this.set("node", conf.getString("node", ""));
    this.set("release", Module.load("default").getVersion());
    this.set("build", Module.load("default").getBuild());

    show("admin/dashboard.html");
  }

}
