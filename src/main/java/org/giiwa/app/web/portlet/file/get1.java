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
package org.giiwa.app.web.portlet.file;

import java.util.Collections;
import java.util.List;

import org.giiwa.app.web.portlet.portlet;
import org.giiwa.bean.m._File;
import org.giiwa.conf.Local;
import org.giiwa.dao.Beans;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.web.Path;

public class get1 extends portlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void get() {

		W q = W.create().and("node", Local.id()).and("name", "get")
				.and("created", System.currentTimeMillis() - X.AHOUR, W.OP.gte).sort("created", -1);
		_File.Record.dao.optimize(q);
		
		Beans<_File.Record> bs = _File.Record.dao.load(q, 0, 60);
		if (bs != null && !bs.isEmpty()) {
			Collections.reverse(bs);

			this.set("list", bs);
			this.show("/portlet/file/get1.html");
		}
	}

	@Path(path = "data", login = true)
	public void data() {

		Beans<_File.Record> bs = _File.Record.dao.load(W.create().and("node", Local.id()).and("name", "get")
				.and("created", System.currentTimeMillis() - X.AHOUR, W.OP.gte).sort("created", -1), 0, 60);
		if (bs != null && !bs.isEmpty()) {
			Collections.reverse(bs);

			this.set("list", bs);

			List<JSON> data = JSON.createList();
			for (String[] s : new String[][] { { "max", "#860606" }, { "avg", "#0dad76" }, { "min", "#0a5ea0" } }) {
				JSON p = JSON.create();
				p.append("name", lang.get("file.get." + s[0])).append("color", s[1]);
				List<JSON> l1 = JSON.createList();
				bs.forEach(e -> {
					l1.add(JSON.create().append("x", lang.time(e.getCreated(), "m")).append("y", e.get(s[0])));
				});
				p.append("data", l1);
				data.add(p);
			}

			this.send(JSON.create().append(X.STATE, 200).append("data", data));
			return;
		}

		this.send(JSON.create().append(X.STATE, 201));

	}

	@Path(path = "more", login = true)
	public void more() {

		long time = System.currentTimeMillis() - X.AWEEK;

		Beans<_File.Record> bs = _File.Record.dao.load(
				W.create().and("node", Local.id()).and("name", "get").and("created", time, W.OP.gte).sort("created", -1), 0,
				24 * 60 * 2);
		if (bs != null && !bs.isEmpty()) {
			Collections.reverse(bs);
			this.set("list", bs);
		}
		this.show("/portlet/file/get1.more.html");

	}

}
