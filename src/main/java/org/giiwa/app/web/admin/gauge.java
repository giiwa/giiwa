/*/*
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
package org.giiwa.app.web.admin;

import org.giiwa.core.base.Host;
import org.giiwa.core.json.JSON;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;
import org.giiwa.framework.web.Tps;

/**
 * web api: /admin/gauge <br>
 * used to get "computer" status
 * 
 * @author joe
 *
 */
public class gauge extends Model {

  /*
   * (non-Javadoc)
   * 
   * @see org.giiwa.framework.web.Model#onGet()
   */
  public void onGet() {
    this.redirect("/user");
  }

  /**
   * Cpu.
   */
  @Path(path = "tps", login = true, access = "access.config.admin", accesslog = false)
  public void tps() {
    this.set("max", Tps.max());
    this.show("/admin/gauge.tps.html");
  }

  /**
   * Cpu_status.
   */
  @Path(path = "tps/status", login = true, access = "access.config.admin", accesslog = false)
  public void tps_status() {
    // todo
    JSON jo = new JSON();
    jo.put("total", Tps.get());

    this.response(jo);

  }

  /**
   * Mem_status.
   */
  @Path(path = "mem/status", login = true, access = "access.config.admin", accesslog = false)
  public void mem_status() {
    // todo
    JSON jo = new JSON();
    jo.put("used", Host.getMemUsed());

    this.response(jo);

  }

  /**
   * Mem.
   */
  @Path(path = "mem", login = true, access = "access.config.admin", accesslog = false)
  public void mem() {
    this.set("total", Host.getMemTotal());
    this.show("/admin/gauge.mem.html");
  }

  /**
   * Disk.
   */
  @Path(path = "disk", login = true, access = "access.config.admin", accesslog = false)
  public void disk() {
    this.set("list", Host.getDisks());
    this.show("/admin/gauge.disk.html");
  }

}
