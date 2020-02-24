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
package org.giiwa.web.view;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.misc.IOUtil;
import org.giiwa.web.Controller;
import org.giiwa.web.Language;

public class FileView extends View {

	private String caching = null;

	/**
	 * copy the file to front-end, and {giiwa}/html/ too
	 */
	@Override
	public boolean parse(Object file, Controller m, String viewname) throws IOException {

		InputStream in = null;
		try {
			in = View.getInputStream(file);

			/**
			 * copy the local html first
			 */
			if (caching == null) {
				caching = Global.getString("web.cache", X.EMPTY);
			}

			if (Local.getInt("web.debug", 0) == 0) {
				// not debug
				if (!X.isEmpty(caching) && viewname.matches(caching)) {

					File f1 = new File(Controller.GIIWA_HOME + "/html/" + viewname);
					if (!f1.exists()) {
						f1.getParentFile().mkdirs();
						FileOutputStream out1 = new FileOutputStream(f1);
						IOUtil.copy(in, out1);
						in = View.getInputStream(file);
					}
				}
			}

			String filetype = Controller.getMimeType(View.getName(file));
			m.setContentType(filetype);

			boolean media = (filetype != null && (filetype.startsWith("video") || filetype.startsWith("audio"))) ? true
					: false;

			String date = m.getHeader("If-Modified-Since");

			String range = m.getHeader("Range");
			String date2 = Language.getLanguage().format(View.lastModified(file), "yyyy-MM-dd HH:mm:ss z");
			if (X.isEmpty(range) && date != null && date.equals(date2)) {
				m.resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				return true;
			}

			// GLog.applog.info("file", "download", "range=" + range, m.getUser(),
			// m.getRemoteHost());

			long total = View.length(file);
			long start = 0;
			long end = total - 1;
			if (!X.isEmpty(range)) {
				String[] ss = X.split(range, "[=-]");
				if (ss.length > 1) {
					start = X.toLong(ss[1]);
				}

				if (ss.length > 2) {
					end = Math.min(total, X.toLong(ss[2]));

					if (end < start) {
						end = start + 16 * 1024 - 1;
					}
				}
			}

			if (end > total - 1) {
				end = total - 1;
			}

			long length = end - start + 1;

			m.setHeader("Content-Length", Long.toString(length));
			m.setHeader("Last-Modified", date2);

			if (start == 0) {
				m.setHeader("Accept-Ranges", "bytes");
			}

			m.setHeader("Content-Length", Long.toString(length));
			if (media || (!X.isEmpty(range) && total > length)) {
				m.setHeader("Content-Range", "bytes " + start + "-" + (start + length - 1) + "/" + total);
			}

			if (media || end < total - 1) {
				// if (media) {
				m.setStatus(206);
				// GLog.applog.info("file", "download", "range=" + range + ", status=206",
				// m.getUser(), m.getRemoteHost());
			}

			TimeStamp t = TimeStamp.create();
			try {
				IOUtil.copy(in, m.getOutputStream(), start, end, true);
				if (log.isDebugEnabled())
					log.debug("cost t=" + t.past() + ", file=" + file + ", start=" + start + ",end=" + end);
			} catch (Exception e) {
				log.error("cost t=" + t.past() + ", file=" + file + ", start=" + start + ", end=" + end, e);
			}

			return true;
		} finally {
			X.close(in);
		}
	}

	@Override
	public String parse(Object file, JSON params) {
		// TODO Auto-generated method stub
		return null;
	}

}
