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
package org.giiwa.app.web.portlet;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.giiwa.bean.Node;
import org.giiwa.bean.m._CPU;
import org.giiwa.conf.Local;
import org.giiwa.dao.Beans;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.web.Path;

public class cpu extends portlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void get() {

		String id = this.getString("id");
		if (X.isEmpty(id)) {
			id = Local.id();
		}
		this.set(X.ID, id);
		Node n = Node.dao.load(id);
		if (n != null) {
			this.set("name", n.label);
			this.set("cores", n.cores);
			this.set("ghz", n.ghz);
		} else {
			this.set("name", id);
		}

		W q = W.create().and("node", id).and("created", System.currentTimeMillis() - X.AHOUR, W.OP.gte).sort("created",
				-1);
		_CPU.Record.dao.optimize(q);

		Beans<_CPU.Record> bs = _CPU.Record.dao.load(q, 0, 60);
		if (bs != null && !bs.isEmpty()) {

			this.set("temp", bs.get(0).temp);

			Collections.reverse(bs);

			this.set("list", bs);
		}
		this.show("/portlet/cpu.html");
	}

	@Path(path = "data", login = true)
	public void data() {

		String id = this.get("id");
		if (X.isEmpty(id)) {
			id = Local.id();
		}
		this.set(X.ID, id);

		int hours = this.getInt("n", 1);

		Node n = Node.dao.load(id);

		W q = W.create().and("node", id).and("created", System.currentTimeMillis() - X.AHOUR * hours, W.OP.gte)
				.sort("created", -1);

		Beans<_CPU.Record> bs = _CPU.Record.dao.load(q, 0, 60 * hours);
		if (bs != null && !bs.isEmpty()) {

			String temp = bs.get(0).temp;

			Collections.reverse(bs);

			JSON p = JSON.create();
			p.append("name", (n != null ? n.label : "") + " - " + lang.get("cpu.usage")).append("color", "#860606");
			List<JSON> l1 = JSON.createList();
			bs.forEach(e -> {
				l1.add(JSON.create().append("x", lang.time(e.getCreated(), "m")).append("y", e.getUsage()));
			});
			p.append("data", l1);

			this.send(JSON.create().append(X.STATE, 200)
					.append("name", (n != null ? n.label : "") + " - " + lang.get("cpu.usage")).append("temp", temp)
					.append("data", Arrays.asList(p)));
			return;
		}
		this.send(JSON.create().append(X.STATE, 201));
	}

	@Path(path = "more", login = true)
	public void more() {

		long time = System.currentTimeMillis() - X.ADAY * 2;

		String id = this.get("id");
		if (X.isEmpty(id)) {
			id = Local.id();
		}
		this.set(X.ID, id);
		Node n = Node.dao.load(id);
		if (n != null) {
			this.set("cores", n.cores);
			this.set("name", n.label);
			this.set("ghz", n.ghz);
		}

		W q = W.create().and("node", id).and("created", time, W.OP.gte).sort("created", -1);

		Beans<_CPU.Record> bs = _CPU.Record.dao.load(q, 0, 24 * 60 * 2);

		if (bs != null && !bs.isEmpty()) {

			this.set("temp", bs.get(0).temp);
			Collections.reverse(bs);

			this.set("list", bs);
		}
		this.show("/portlet/cpu.more.html");

	}

}
