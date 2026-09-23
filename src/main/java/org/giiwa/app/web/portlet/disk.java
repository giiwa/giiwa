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

import java.util.Collections;
import java.util.List;

import org.giiwa.bean.Node;
import org.giiwa.bean.m._Disk;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.Beans;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.web.Path;

public class disk extends portlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void get() {

		String id = this.getString(X.ID);
		if (X.isEmpty(id)) {
			id = Local.id();
		}
		this.set(X.ID, id);
		Node n = Node.dao.load(id);
		if (n != null) {
			this.set(X.NAME, n.label);
		} else {
			this.set(X.NAME, id);
		}

		W q = W.create().and(X.NODE, id).and("updated", Global.now() - X.AHOUR, W.OP.gte).sort("path", 1);
		_Disk.dao.optimize(q);

		Beans<_Disk> bs = _Disk.dao.load(q, 0, 100);
		if (bs != null && !bs.isEmpty()) {
			// Collections.reverse(bs);
//			this.set("disk", bs.get(0));
			this.set(X.LIST, bs);
		}

		this.show("/portlet/disk.html");
	}

	@Path(path = "data", login = true)
	public void data() {

		String id = this.get(X.ID);
		if (X.isEmpty(id)) {
			id = Local.id();
		}

		int hours = this.getInt(X.N, 1);

		Node n = Node.dao.load(id);

		W q = W.create().and(X.NODE, id);

		_Disk.dao.optimize(q);
		Beans<_Disk> l0 = _Disk.dao.load(q, 0, 24);

		List<JSON> l1 = JSON.createList();

		l0.forEach(d -> {

			W q1 = q.copy().and("path", d.path).and(X.CREATED, Global.now() - X.AHOUR * hours, W.OP.gte)
					.sort(X.CREATED, -1);

			_Disk.Record.dao.optimize(q1);
			Beans<_Disk.Record> bs = _Disk.Record.dao.load(q1, 0, 60 * hours);
			if (bs != null) {

				Collections.reverse(bs);

				List<JSON> free = JSON.createList();
				List<JSON> used = JSON.createList();
				bs.forEach(e -> {
					free.add(JSON.create().append("x", lang.time(e.getCreated(), "m")).append("y", e.getFree()));
					used.add(JSON.create().append("x", lang.time(e.getCreated(), "m")).append("y", e.getUsed()));
				});

				l1.add(JSON.create().append(X.NAME, d.path + "-已经使用").append("data", used));
				l1.add(JSON.create().append(X.NAME, d.path + "-空余").append("data", free));

			}

		});

		this.set(X.NAME, (n != null ? n.label : "") + " - " + lang.get("disk.usage"));
		this.set(X.LIST, l1).send(200);
	}

	@Path(path = "more", login = true)
	public void more() {

		String id = this.getString(X.ID);
		if (X.isEmpty(id)) {
			id = Local.id();
		}
		this.set(X.ID, id);
		Node n = Node.dao.load(id);
		if (n != null) {
			this.set(X.NAME, n.label);
		} else {
			this.set(X.NAME, id);
		}

		String name = this.getString(X.NAME);
		this.set(X.NAME, name);

		long time = Global.now() - X.AWEEK;

		W q = W.create().and(X.NODE, Local.id()).and(X.NAME, name).and("updated", time, W.OP.gte).sort(X.CREATED, 1);

		Beans<_Disk.Record> bs = _Disk.Record.dao.load(q, 0, 24 * 60 * 7);

		if (bs != null && !bs.isEmpty()) {
			this.set(X.LIST, bs);
		}
		this.show("/portlet/disk.more.html");

	}

}
