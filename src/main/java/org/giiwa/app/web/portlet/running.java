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
import org.giiwa.bean.Stat;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.Beans;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.web.Path;

public class running extends portlet {

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
			this.set("name1", n.label);
		} else {
			this.set("name1", id);
		}

		Beans<Stat> bs = Stat.load("node.load", Stat.TYPE.snapshot, Stat.SIZE.min, W.create().and("dataid", id)
				.and("time", Global.now() - X.AHOUR, W.OP.gte).sort("time", 1), 0, 60);
		if (bs != null && !bs.isEmpty()) {

			this.set("list", bs);
		}

		long max = X.toLong(1.1 * Stat.max("n3", "node.load", Stat.TYPE.snapshot, Stat.SIZE.min,
				W.create().and("time", Global.now() - X.AHOUR, W.OP.gte)));
		
		this.set("max", max);

		this.show("/portlet/running.html");
	}

	@Path(path = "data", login = true)
	public void data() {

		String id = this.getString("id");
		if (X.isEmpty(id)) {
			id = Local.id();
		}
		this.set(X.ID, id);
		Node n = Node.dao.load(id);

		Beans<Stat> bs = Stat.load("node.load", Stat.TYPE.snapshot, Stat.SIZE.min, W.create().and("dataid", id)
				.and("time", Global.now() - X.AHOUR, W.OP.gte).sort("time", 1), 0, 60);
		if (bs != null && !bs.isEmpty()) {

			long max = Stat.max("n3", "node.load", Stat.TYPE.snapshot, Stat.SIZE.min,
					W.create().and("time", Global.now() - X.AHOUR, W.OP.gte));

			JSON p = JSON.create();
			p.append("name", (n != null ? n.label : "") + " - " + lang.get("cpu.usage")).append("color", "#25840a");
			List<JSON> l1 = JSON.createList();
			bs.forEach(e -> {
				l1.add(JSON.create().append("x", lang.time(e.getLong("time"), "m")).append("y", e.getLong("n3")));
			});
			p.append("data", l1);
			this.send(JSON.create().append(X.STATE, 200).append("data", Arrays.asList(p)).append("max", max));
			return;
		}
		this.send(JSON.create().append(X.STATE, 201));
	}

	@Path(path = "more", login = true)
	public void more() {

		String id = this.getString("id");
		if (X.isEmpty(id)) {
			id = Local.id();
		}
		this.set(X.ID, id);
		Node n = Node.dao.load(id);
		if (n != null) {
			this.set("name1", n.label);
		} else {
			this.set("name1", id);
		}

		long time = Global.now() - X.AMONTH;

		Beans<Stat> bs = Stat.load("node.load", Stat.TYPE.snapshot, Stat.SIZE.min,
				W.create().and("dataid", id).and("time", time, W.OP.gte).sort("time", -1), 0, 30 * 24 * 60);
		if (bs != null && !bs.isEmpty()) {
			Collections.reverse(bs);
			this.set("list", bs);
		}
		this.show("/portlet/running.more.html");

	}

}
