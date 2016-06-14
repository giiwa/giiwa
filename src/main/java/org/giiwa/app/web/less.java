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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.giiwa.core.bean.X;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;

// TODO: Auto-generated Javadoc
public class less extends Model {

	/**
   * Css.
   */
	@Path(path = "css")
	public void css() {
		String css = this.getString("css");
		String[] ss = css.split(",");

		this.setContentType(getMimeType("a.css"));
		List<File> list = new ArrayList<File>();
		long last = 0;
		for (String s : ss) {
			String name = s.trim();
			if (!name.startsWith("/")) {
				name = "/css/" + name;
			}
			File f = module.getFile(name);
			if (f.exists()) {
				list.add(f);
				if (f.lastModified() > last) {
					last = f.lastModified();
				}
			}
		}

		String date = this.getHeader("If-Modified-Since");
		String date2 = lang.format(last, "yyyy-MM-dd HH:mm:ss z");
		if (X.isSame(date, date2)) {
			resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			return;
		}
		this.setHeader("Last-Modified", date2);

		// merge the files
		try {
			PrintStream out = new PrintStream(this.getOutputStream());
			for (File f : list) {
				BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
				out.println("/*" + f.getName() + "*/");
				StringBuilder sb = new StringBuilder();
				String line = in.readLine();
				while (line != null) {
					line = line.trim();
					if (!X.isEmpty(line)) {
						sb.append(line).append(" ");
						if (line.endsWith("}")) {
							// wrap
							line = sb.toString().replaceAll("  ", " ");
							out.println(line);
						}
					}
					line = in.readLine();
				}
				in.close();
			}
		} catch (Exception e) {
			log.error(css, e);
		}
	}

	/**
   * Js.
   */
	@Path(path = "js")
	public void js() {

	}

}
