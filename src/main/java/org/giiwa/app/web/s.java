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
package org.giiwa.app.web;

import org.giiwa.bean.S;
import org.giiwa.json.JSON;
import org.giiwa.web.Controller;

public class s extends Controller {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public void onGet() {
		S e = S.dao.load(path);
		if (e != null) {
			JSON j1 = this.json();
			if (j1 != null && !j1.isEmpty()) {
				if (e.url.indexOf("?") > 0) {
					this.redirect(e.url + "&" + j1.toUrl());
				} else {
					this.redirect(e.url + "?" + j1.toUrl());
				}
			} else {
				this.redirect(e.url);
			}
		}
	}

}
