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

import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.web.*;

/**
 * web api: /admin/task <br>
 * used to manage task,<br>
 * required "access.config.admin"
 * 
 * @author joe
 *
 */
public class mo extends Controller {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model.onGet()
	 */
	@Path(login = true, access = "access.config.admin")
	public void onGet() {

		this.set("list", Processing.getAll());

		this.show("/admin/mo.index.html");
	}

	@Path(path = "kill", login = true, access = "access.config.admin", oplog = true)
	public void kill() {

		int id = this.getInt("id");
		Processing.kill(id);

		this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, "killed"));

	}

	@Path(path = "dump", login = true, access = "access.config.admin", oplog = true)
	public void dump() {

		int id = this.getInt("id");
		Controller mo = Processing.get(id);
		if (mo == null) {
			this.set(X.ERROR, lang.get("task.notfound")).send(201);
			return;
		}

		Thread t1 = mo.thread;

//		JSON j = JSON.create();

		StringBuilder sb = new StringBuilder();
		try {
			if (t1 != null) {
				StackTraceElement[] ss = t1.getStackTrace();
				sb.append("ID: ").append(t1.getId()).append("(0x").append(Long.toHexString(t1.getId()))
						.append("), Thread: ").append(t1.getName()).append(", State: <i style='color:green'>")
						.append(t1.getState()).append("</i>, Controller:").append(mo.getClass().getName()).append("\r");

				if (ss != null && ss.length > 0) {
					for (StackTraceElement e : ss) {

						sb.append("&nbsp;&nbsp;&nbsp;&nbsp;").append(e.toString()).append("\r");
					}
				}
			}
			if (sb.length() > 0) {
				this.set(X.MESSAGE, sb.toString()).send(200);
				return;
			} else {
				this.set(X.ERROR, lang.get("task.notfound")).send(201);
				return;
			}
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			this.set(X.ERROR, e.getMessage()).send(201);
			return;
		}

	}

}
