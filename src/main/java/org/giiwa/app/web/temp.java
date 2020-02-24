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

import java.io.File;
import java.io.FileInputStream;

import org.giiwa.bean.GLog;
import org.giiwa.bean.Temp;
import org.giiwa.dao.X;
import org.giiwa.misc.Url;
import org.giiwa.web.Controller;

/**
 * web api： /temp <br>
 * used to access temporary file which created by Temp
 * @deprecated
 * @author joe
 * 
 */
public class temp extends Controller {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model.onGet()
	 */
	public void onGet() {

		if (log.isDebugEnabled())
			log.debug("temp: " + this.path);
		if (this.path == null) {
			this.notfound();
			return;
		}

		String[] ss = X.split(Url.decode(this.path), "[/]");
		if (ss.length != 2) {
			this.notfound();
			return;
		}

		try {

			String name = ss[1];
			File f1 = Temp.get(ss[0], name);
			if (!f1.exists()) {
				this.notfound();
				return;
			} else {
				this.response(name, new FileInputStream(f1), f1.length());
			}

		} catch (Exception e) {
			log.error(path, e);
			GLog.oplog.error(temp.class, "", e.getMessage(), e, login, this.getRemoteHost());
		}

		this.notfound();
	}

}
