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
import org.giiwa.task.Monitor;
import org.giiwa.web.Controller;
import org.giiwa.web.Path;

public class monitor extends Controller {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Path(path = "checking", login = true)
	public void checking() {

		long id = this.getLong("id");
		String access = this.get("access");

		JSON jo = Monitor.get(id, access);

		this.send(JSON.create().append(X.STATE, 200).append("data", jo));

	}

}
