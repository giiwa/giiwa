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

import java.util.HashMap;
import java.util.Map;

import org.giiwa.core.bean.X;
import org.giiwa.core.json.JSON;
import org.giiwa.core.task.Task;
import org.giiwa.framework.web.Model;

/**
 * web api: /alive, just simple response with the uptime, thread info
 * 
 * @author wujun
 *
 */
public class alive extends Model {

  /*
   * (non-Javadoc)
   * 
   * @see org.giiwa.framework.web.Model#onGet()
   */
  public void onGet() {
    JSON jo = new JSON();
    jo. put(X.STATE, 200);
    jo.put("uptime", Model.UPTIME);
    jo.put("idle", Task.idleThread());
    jo.put("tasks", Task.tasksInQueue());
    this.response(jo);
  }

}
