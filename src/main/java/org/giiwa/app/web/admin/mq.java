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

import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.X;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.conf.Global;
import org.giiwa.framework.bean.OpLog;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;
import org.giiwa.mq.MQ;

public class mq extends Model {

  /*
   * (non-Javadoc)
   * 
   * @see org.giiwa.framework.web.Model#onGet()
   */
  @Path(path = "log", login = true, access = "access.config.admin")
  public void log() {
    int s = this.getInt("s");
    int n = this.getInt("n", 10, "number.per.page");

    W q = W.create("model", "admin.mq").sort("created", -1);

    Beans<OpLog> bs = OpLog.load(q, s, n);
    this.set(bs, s, n);

    this.show("/admin/mq.logs.html");
  }

  /**
   * Setting.
   */
  @Path(path = "setting", login = true, access = "access.config.admin")
  public void setting() {
    if (method.isPost()) {
      Global.setConfig("mq.type", this.getString("mq.type"));
      Global.setConfig("mq.group", this.getString("group"));
      Global.setConfig("activemq.url", this.getString("activemq.url"));
      Global.setConfig("activemq.user", this.getString("activemq.user"));
      Global.setConfig("activemq.passwd", this.getString("activemq.passwd"));

      Global.setConfig("rabbitmq.url", this.getString("rabbitmq.url"));
      Global.setConfig("rabbitmq.user", this.getString("rabbitmq.user"));
      Global.setConfig("rabbitmq.passwd", this.getString("rabbitmq.passwd"));

      // Global.setConfig("kafka.broker", this.getString("kafka.broker"));
      // Global.setConfig("zookeeper.url", this.getString("zookeeper.url"));

      int logger = X.isSame("on", this.getString("mq.logger")) ? 1 : 0;
      Global.setConfig("mq.logger", logger);

      MQ.logger(logger == 1 ? true : false);

      this.set(X.WARN, lang.get("restart.required"));
    }
    this.show("/admin/mq.setting.html");
  }

}
