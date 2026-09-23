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

import org.giiwa.bean.Api;
import org.giiwa.bean.Temp;
import org.giiwa.conf.Global;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.misc.Exporter;
import org.giiwa.web.*;

/**
 * 下载接口文件
 * 
 * @author joe
 *
 */
public class api extends Controller {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 下载接口文件
	 */
	@Path(path = "csv", login = true, access = "access.config.admin", oplog = true, loglevel = "warn")
	public void csv() {

		W q = Api.dao.query().sort("module").sort("uri");
		String name = "api_" + lang.format(Global.now(), "yyyyMMdd") + ".csv";
		Temp t = Temp.create(name);
		try {
			Exporter<Api> ex = Exporter.create(t.getOutputStream(), Exporter.FORMAT.csv, true);
			Exporter<Api> e1 = ex.createSheet(e -> {
				Object[] ss = new Object[6];
				ss[0] = e.module;
				ss[1] = e.uri;
				ss[2] = e.memo;
				ss[3] = e.method;
				ss[4] = e.in;
				ss[5] = e.out;
				return ss;
			});
			e1.print(new Object[] { "模块", "接口链接", "说明", "方法", "输入", "输出" });
			Api.dao.stream(q, d -> {
				try {
					if (!X.isEmpty(d.memo)) {
						e1.print(d);
					}
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
				return true;
			});
			ex.close();
			ex = null;
			this.send(t.getName(), t.getInputStream());
		} catch (Exception err) {
			log.error(err.getMessage(), err);
			this.set(X.ERROR, err.getMessage());
			this.send(201);
		}
	}

}
