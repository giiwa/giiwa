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
import org.giiwa.bean.m._Mem2;
import org.giiwa.conf.Local;
import org.giiwa.dao.Beans;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.web.Path;

public class mem2 extends portlet {

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
		} else {
			this.set("name", id);
		}

		W q = W.create().and("node", id).and("created", System.currentTimeMillis() - X.AHOUR, W.OP.gte).sort("created",
				-1);
		_Mem2.Record.dao.optimize(q);

		Beans<_Mem2.Record> bs = _Mem2.Record.dao.load(q, 0, 60);
		if (bs != null && !bs.isEmpty()) {
			Collections.reverse(bs);

			this.set("list", bs);
		}

		long max = X.toLong(1.1 * X.toLong(_Mem2.Record.dao.max("used",
				W.create().and("created", System.currentTimeMillis() - X.AHOUR, W.OP.gte))));

		this.set("max", max);

		this.show("/portlet/mem2.html");

	}

	@Path(path = "data", login = true)
	public void data() {
		String id = this.getString("id");
		if (X.isEmpty(id)) {
			id = Local.id();
		}
		this.set(X.ID, id);
		Node n = Node.dao.load(id);

		int hours = this.getInt("n", 1);

		Beans<_Mem2.Record> bs = _Mem2.Record.dao.load(W.create().and("node", id)
				.and("created", System.currentTimeMillis() - X.AHOUR * hours, W.OP.gte).sort("created", -1), 0,
				60 * hours);
		if (bs != null && !bs.isEmpty()) {
			Collections.reverse(bs);

			// {name: "$lang.get('mem.used')", color:'.860606', data: [.foreach($c in $list)
			// {x:$this.time($c), y:$c.used, hint:"$lang.size($c.used)"},.end]},
			// {name: "$lang.get('mem.free')", color:'.0dad76', data: [.foreach($c in $list)
			// {x:$this.time($c), y:$c.free, hint:"$lang.size($c.free)"},.end]}

			JSON p1 = JSON.create().append("name", (n != null ? n.label : "") + " - " + lang.get("mem.used"))
					.append("color", "#0a5ea0");

			List<JSON> l1 = JSON.createList();
			bs.forEach(e -> {
				l1.add(JSON.create().append("x", lang.time(e.getCreated(), "m")).append("y", e.used).append("hint",
						lang.size(e.used)));
			});
			p1.append("data", l1);
			this.send(JSON.create().append(X.STATE, 200).append("data", Arrays.asList(p1)));

			return;

		}
		this.send(JSON.create().append(X.STATE, 201));

	}

	@Path(path = "more", login = true)
	public void more() {
//		long id = this.getLong("id");
//		this.set("id", id);

		String id = this.getString("id");
		if (X.isEmpty(id)) {
			id = Local.id();
		}
		this.set(X.ID, id);
		Node n = Node.dao.load(id);
		if (n != null) {
			this.set("name", n.label);
		}

		long time = System.currentTimeMillis() - X.AWEEK;

		Beans<_Mem2.Record> bs = _Mem2.Record.dao
				.load(W.create().and("node", id).and("created", time, W.OP.gte).sort("created", -1), 0, 60 * 24 * 2);

		if (bs != null && !bs.isEmpty()) {
			Collections.reverse(bs);
			this.set("list", bs);
		}
		this.show("/portlet/mem2.more.html");
	}

}
