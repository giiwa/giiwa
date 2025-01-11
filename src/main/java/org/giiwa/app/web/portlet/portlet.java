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
package org.giiwa.app.web.portlet;

import org.giiwa.dao.Bean;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.web.Controller;
import org.giiwa.web.Path;

public class portlet extends Controller {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Path()
	public final void onGet() {

		login = this.user();
		W q = W.create().and("uri", this.uri);
		if (login == null) {
			q.and("uid", 0);
		} else {
			q.and("uid", login.getId());
		}

		get();
	}

	public void get() {
	}

	/**
	 * @Deprecated
	 * @param b
	 * @return
	 */
	public long time(Bean b) {
		return b.getCreated() + X.AHOUR * 8;
	}

}
