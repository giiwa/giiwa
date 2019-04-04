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

import org.giiwa.framework.web.*;

/**
 * web api: /admin/token <br>
 * used to manage user<br>
 * required "access.user.admin"
 * 
 * @author joe
 *
 */
public class browserinfo extends Model {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model.onGet()
	 */
	@Override
	@Path()
	public void onGet() {
		this.set("list", this.getHeaders());
		this.show("/admin/browserinfo.html");

	}

}
