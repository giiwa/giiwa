/*
 * Copyright 2015 Giiwa, Inc. and/or its affiliates.
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

import org.giiwa.core.bean.Beans;
import org.giiwa.framework.bean.Menu;
import org.giiwa.framework.web.*;

/**
 * Web home
 * 
 * @author joe
 * 
 */
public class index extends Model {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model#onGet()
	 */
	@Override
	public void onGet() {
		this.set("me", this.getUser());

		Menu m = Menu.load(0, "home");
		if (m != null) {
			Beans<Menu> bs = m.submenu();
			if (bs != null) {
				this.set("menu", bs.getList());
			}
		}

		// if (isMobile()) {
		// this.show("/docs/mobile.html");
		// } else {
		this.show("/docs/home.html");
		// }
	}

	/**
	 * test the request is from mobile
	 * 
	 * @return boolean
	 */
	// public final boolean isMobile() {
	// String useragent = module.get("mobile", ".*(iPhone|Android).*");
	// return Pattern.matches(useragent, this.browser());
	// }

}
