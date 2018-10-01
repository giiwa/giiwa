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
package org.giiwa.framework.web;

import java.io.*;

import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Config;
import org.giiwa.core.conf.Global;
import org.giiwa.core.conf.Local;
import org.giiwa.framework.bean.DFile;
import org.giiwa.framework.bean.Disk;

/**
 * default model for which model has not found
 * 
 * @author yjiang
 * 
 */
public class DefaultModel extends Model {

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.giiwa.framework.web.Model#onGet()
	 */
	@Override
	public void onGet() {
		onPost();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.giiwa.framework.web.Model#onPost()
	 */
	@Override
	public void onPost() {
		/**
		 * if the file exists, and the extension is not .html and htm then get back
		 * directly, and set contenttype
		 */
		log.debug("uri=" + uri + ", remote=" + this.getRemoteHost());

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
			this.set(this.getJSON());

			this.set("me", this.getUser());
			this.put("lang", lang);
			this.put(X.URI, uri);
			this.put("module", Module.home);
			this.put("path", path);
			this.put("request", req);
			this.put("this", this);
			this.put("response", resp);
			this.set("session", this.getSession());
			this.set("global", Global.getInstance());
			this.set("conf", Config.getConf());
			this.set("local", Local.getInstance());

			createQuery();

			show(uri);
			return true;
		}

		// check dfile
		if (Global.getInt("dfile.web", 0) == 1) {
			DFile d = Disk.seek(uri);
			if (d.exists()) {
				// show it
				this.set(this.getJSON());

				this.set("me", this.getUser());
				this.put("lang", lang);
				this.put(X.URI, uri);
				this.put("module", Module.home);
				this.put("path", path);
				this.put("request", req);
				this.put("this", this);
				this.put("response", resp);
				this.set("session", this.getSession());
				this.set("global", Global.getInstance());
				this.set("conf", Config.getConf());
				this.set("local", Local.getInstance());

				createQuery();

				show(uri);
				return true;
			}
		}

		return false;
	}

}
