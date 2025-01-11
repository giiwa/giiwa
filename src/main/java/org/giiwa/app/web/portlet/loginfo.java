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

import org.giiwa.bean.GLog;
import org.giiwa.dao.Beans;
import org.giiwa.dao.Helper.W;

public class loginfo extends portlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void get() {

		Beans<GLog> bs = GLog.dao
				.load(W.create().and("type1", GLog.TYPE_SECURITY).and("uid", login.getId()).sort("created", -1), 0, 5);

		this.set("list", bs);

		this.show("/portlet/loginfo.html");
	}

}
