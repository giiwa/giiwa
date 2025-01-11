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
import org.giiwa.bean.m._Cache;
import org.giiwa.bean.m._File;
import org.giiwa.conf.Local;
import org.giiwa.dao.Beans;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.web.Path;

public class times extends portlet {
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

			this.set("list1", bs);

			Beans<_Cache.Record> list2 = _Cache.Record.dao.load(W.create().and("node", Local.id()).and("name", "down")
					.and("created", System.currentTimeMillis() - X.AHOUR, W.OP.gte).sort("created", -1), 0, 60);
			Collections.reverse(list2);
			this.set("list2", list2);

			this.show("/portlet/file/times.html");
		}
	}

	@Path(path = "data", login = true)
	public void data() {

		Beans<_File.Record> bs = _File.Record.dao.load(W.create().and("node", Local.id()).and("name", "get")
				.and("created", System.currentTimeMillis() - X.AHOUR, W.OP.gte).sort("created", -1), 0, 60);
		if (bs != null && !bs.isEmpty()) {
			Collections.reverse(bs);

			List<JSON> data = JSON.createList();
			JSON p = JSON.create();
			p.append("name", lang.get("file.get.times")).append("color", "#0a5ea0");
			List<JSON> l1 = JSON.createList();
			bs.forEach(e -> {
				l1.add(JSON.create().append("x", lang.time(e.getCreated(), "m")).append("y", e.get("times")));
			});
			p.append("data", l1);
			data.add(p);

			bs = _File.Record.dao.load(W.create().and("node", Local.id()).and("name", "down")
					.and("created", System.currentTimeMillis() - X.AHOUR, W.OP.gte).sort("created", -1), 0, 60);
			if (bs != null && !bs.isEmpty()) {
				Collections.reverse(bs);
				p = JSON.create();
				p.append("name", lang.get("file.down.times")).append("color", "#0dad76");
				List<JSON> l2 = JSON.createList();
				bs.forEach(e -> {
					l2.add(JSON.create().append("x", lang.time(e.getCreated(), "m")).append("y", e.get("times")));
				});
				p.append("data", l2);
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
			this.set("list1", bs);
		}

		Beans<_File.Record> list2 = _File.Record.dao.load(
				W.create().and("node", Local.id()).and("name", "down").and("created", time, W.OP.gte).sort("created", -1), 0,
				24 * 60 * 2);
		if (list2 != null && !list2.isEmpty()) {
			Collections.reverse(list2);
			this.set("list2", list2);
		}

		this.show("/portlet/file/times.more.html");

	}

}
