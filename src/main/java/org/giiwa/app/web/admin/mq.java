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

import org.giiwa.bean.GLog;
import org.giiwa.conf.Global;
import org.giiwa.dao.Beans;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.web.Path;

public class mq extends setting {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model.onGet()
	 */
	@Path(path = "log", login = true, access = "access.config.admin")
	public void log() {
		int s = this.getInt("s");
		int n = this.getInt("n", 10);

		W q = W.create("model", "admin.mq").sort("created", -1);

		Beans<GLog> bs = GLog.dao.load(q, s, n);
		this.set(bs, s, n);

		this.show("/admin/mq.logs.html");
	}

	public void set() {
		Global.setConfig("mq.type", this.getString("mq.type"));

		Global.setConfig("activemq.url", this.getString("activemq.url"));
		Global.setConfig("activemq.user", this.getString("activemq.user"));
		Global.setConfig("activemq.passwd", this.getString("activemq.passwd"));

		Global.setConfig("rabbitmq.url", this.getString("rabbitmq.url"));
		// Global.setConfig("rabbitmq.user", this.getString("rabbitmq.user"));
		// Global.setConfig("rabbitmq.passwd", this.getString("rabbitmq.passwd"));

		Global.setConfig("kafkamq.url", this.getString("kafkamq.url"));
		Global.setConfig("zoo.url", this.getString("zoo.url"));
		// Global.setConfig("zookeeper.url", this.getString("zookeeper.url"));

		// int logger = X.isSame("on", this.getString("mq.logger")) ? 1 : 0;
		// Global.setConfig("mq.logger", logger);
		//
		// MQ.logger(logger == 1 ? true : false);

		this.response(JSON.create().append(X.MESSAGE, lang.get("restart.required")).append(X.STATE, 201));
	}

	public void get() {
		this.settingPage("/admin/mq.setting.html");
	}

}
