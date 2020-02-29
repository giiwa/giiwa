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
package org.giiwa.web;

import java.io.*;

import org.giiwa.bean.Disk;
import org.giiwa.dfile.DFile;

/**
 * default model for which model has not found
 * 
 * @author yjiang
 * 
 */
class DefaultController extends Controller {

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.giiwa.framework.web.Model.onGet()
	 */
	@Override
	public void onGet() {
		onPost();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.giiwa.framework.web.Model.onPost()
	 */
	@Override
	public void onPost() {
		/**
		 * if the file exists, and the extension is not .html and htm then get back
		 * directly, and set contenttype
		 */
//		log.debug("uri=" + uri + ", remote=" + this.getRemoteHost(), new Exception());

		if (!_onPost(uri)) {
			for (String suffix : Controller.welcomes) {
				if (_onPost(uri + "/" + suffix)) {
					return;
				}
			}

			// not found
			this.notfound();
		}

	}

	public boolean isMobile() {
		return this.browser().matches(".*(iPhone|Android).*");
	}

	private boolean _onPost(String uri) {
		uri = uri.replaceAll("//", "/");
		File f = Module.home.getFile(uri);
		if (f != null && f.exists() && f.isFile()) {
			this.copy(this.json());

//			this.put("me", this.getUser());
//			this.put("lang", lang);
//			this.put(X.URI, uri);
//			this.put("module", Module.home);
//			this.put("path", path);
//			this.put("request", req);
//			this.put("this", this);
//			this.put("response", resp);
//			this.put("session", this.getSession(false));
//			this.put("global", Global.getInstance());
//			this.put("conf", Config.getConf());
//			this.put("local", Local.getInstance());

			show(uri);
			return true;
		}

		// check dfile
		DFile d = Disk.seek(uri);
		if (d != null && d.exists() && d.isFile()) {
			// show it
			this.copy(this.json());

//			this.put("me", this.getUser());
//			this.put("lang", lang);
//			this.put(X.URI, uri);
//			this.put("module", Module.home);
//			this.put("path", path);
//			this.put("request", req);
//			this.put("this", this);
//			this.put("response", resp);
//			this.put("session", this.getSession(false));
//			this.put("global", Global.getInstance());
//			this.put("conf", Config.getConf());
//			this.put("local", Local.getInstance());

			show(uri);
			return true;
		}

		return false;
	}

}
