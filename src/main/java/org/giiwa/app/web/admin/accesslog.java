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

import org.giiwa.bean.AccessLog;
import org.giiwa.conf.Global;
import org.giiwa.dao.Beans;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.web.Controller;
import org.giiwa.web.Path;

import jakarta.servlet.http.HttpServletResponse;

/**
 * web api: /admin/accesslog <br>
 * used to access the "accesslog", <br>
 * required "access.logs.admin"
 * 
 * @author joe
 *
 */
public class accesslog extends Controller {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Path(path = "open", login = true, access = "access.config.admin|access.config.logs.admin")
	public void open() {
		JSON jo = JSON.create();
		int on = this.getInt("on");
		Global.setConfig("accesslog.on", on);
		jo.put(X.STATE, HttpServletResponse.SC_OK);
		jo.put("on", on);
		this.send(jo);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model.onGet()
	 */
	@Path(login = true, access = "access.config.admin|access.config.logs.admin")
	public void onGet() {
		String url = this.getString("url");
		String ip = this.getString("ip");
//		String sortby = this.getString("sortby");
//		int sortby_type = this.getInt("sortby_type", -1);

		W q = W.create();
		if (!X.isEmpty(url)) {
			q.and("url", url);
			this.set("url", url);
		}
		if (!X.isEmpty(ip)) {
			q.and("ip", ip);
			this.set("ip", ip);
		}
		int s = this.getInt("s");
		int n = this.getInt("n", X.ITEMS_PER_PAGE);

		q.sort("created", -1);
		Beans<AccessLog> bs = AccessLog.dao.load(q, s, n);
		if (bs != null) {
			bs.count();
		}

		this.pages(bs, s, n);

		this.show("/admin/accesslog.index.html");
	}

	/**
	 * Deleteall.
	 */
	@Path(path = "deleteall", login = true, access = "access.config.admin|access.config.logs.admin")
	public void deleteall() {
		AccessLog.dao.delete(W.create());
	}

}
