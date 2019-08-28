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
import java.io.InputStream;

import org.giiwa.core.base.IOUtil;
import org.giiwa.core.base.Url;
import org.giiwa.core.bean.X;
import org.giiwa.framework.bean.GLog;
import org.giiwa.framework.bean.Temp;
import org.giiwa.framework.web.Model;

/**
 * web api： /temp <br>
 * used to access temporary file which created by Temp
 * 
 * @author joe
 * 
 */
public class temp extends Model {

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

			InputStream in = null;

			String name = ss[1];
			File f1 = Temp.get(ss[0], name);
			long total = 0;
			if (!f1.exists()) {

				File f = Temp.get(ss[0], name);
				if (!f.exists()) {
					this.notfound();
					return;
				}
				if (log.isDebugEnabled())
					log.debug("filename=" + f.getCanonicalPath());
				total = f.length();
				in = new FileInputStream(f);
			} else {
				if (log.isDebugEnabled())
					log.debug("filename=" + f1.getCanonicalPath());
				in = new FileInputStream(f1);
				total = f1.length();
			}

			String range = this.getHeader("Range");
			long start = 0;
			long end = total;
			if (!X.isEmpty(range)) {
				String[] s1 = X.split(range, "[=-]");
				if (s1.length > 1) {
					start = X.toLong(s1[1]);
				}

				if (s1.length > 2) {
					end = Math.min(total, X.toLong(s1[2]));

					if (end < start) {
						end = start + 16 * 1024;
					}
				}
			}

			if (end > total) {
				end = total;
			}

			long length = end - start;

			this.setContentType("application/octet");
			this.setHeader("Content-Disposition", "attachment; filename=\"" + Url.encode(name) + "\"");
			this.setHeader("Content-Length", Long.toString(length));
			this.setHeader("Content-Range", "bytes " + start + "-" + (end - 1) + "/" + total);
			if (start == 0) {
				this.setHeader("Accept-Ranges", "bytes");
			}
			if (end < total) {
				this.setStatus(206);
			}

			IOUtil.copy(in, this.getOutputStream(), start, end, true);

			return;

		} catch (Exception e) {
			log.error(path, e);
			GLog.oplog.error(temp.class, "", e.getMessage(), e, login, this.getRemoteHost());
		}

		this.notfound();
	}

}
