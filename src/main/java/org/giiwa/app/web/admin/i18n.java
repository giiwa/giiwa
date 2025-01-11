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

import java.util.Arrays;
import java.util.Map;

import org.giiwa.web.Controller;
import org.giiwa.web.Path;

/**
 * some setting of the module
 * 
 * @author joe
 *
 */
public class i18n extends Controller {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Path(login = true, access = "access.config.admin|access.config.module.admin")
	public void onGet() {
		Map<String, String> missed = lang.getMissed();
		StringBuilder sb = new StringBuilder();
		for (String n : missed.keySet()) {
			sb.append(n).append("=").append("<br>");
		}
		if (sb.length() > 0) {
			this.set("missed", sb.toString());
		}

		Map<String, String[]> d = lang.getData();
		for (String s : d.keySet()) {
			String[] ss = d.get(s);
			if (ss.length != 2) {
				log.error("bad data in language, key=" + s + ", value=" + Arrays.toString(ss));
			}
		}
		this.set("d", d);
		this.show("/admin/i18n.index.html");

	}

}
