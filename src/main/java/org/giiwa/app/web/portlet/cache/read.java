package org.giiwa.app.web.portlet.cache;

import java.util.Collections;
import java.util.List;

import org.giiwa.app.web.portlet.portlet;
import org.giiwa.bean.Node;
import org.giiwa.bean.m._Cache;
import org.giiwa.conf.Local;
import org.giiwa.dao.Beans;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.web.Path;

public class read extends portlet {

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

		W q = W.create().and("node",id).and("name", "read")
				.and("created", System.currentTimeMillis() - X.AHOUR, W.OP.gte).sort("created", -1);
		_Cache.Record.dao.optimize(q);
		
		Beans<_Cache.Record> bs = _Cache.Record.dao.load(q, 0, 60);
		if (bs != null && !bs.isEmpty()) {
			Collections.reverse(bs);

			this.set("list", bs);
		}
		
		long max = X.toLong(1.1 *X.toLong(_Cache.Record.dao.max("max",
				W.create().and("name", "read").and("created", System.currentTimeMillis() - X.AHOUR, W.OP.gte))));
		this.set("max", max);

		this.show("/portlet/cache/read.html");
	}

	@Path(path = "data", login = true)
	public void data() {

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

		Beans<_Cache.Record> bs = _Cache.Record.dao.load(W.create().and("node", id).and("name", "read")
				.and("created", System.currentTimeMillis() - X.AHOUR, W.OP.gte).sort("created", -1), 0, 60);
		if (bs != null && !bs.isEmpty()) {
			Collections.reverse(bs);

			this.set("list", bs);

			List<JSON> data = JSON.createList();
			for (String[] s : new String[][] { { "max", "#860606" }, { "avg", "#0dad76" }, { "min", "#0a5ea0" } }) {
				JSON p = JSON.create();
				p.append("name", lang.get("cache.read." + s[0])).append("color", s[1]);
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

		long time = System.currentTimeMillis() - X.AWEEK;

		Beans<_Cache.Record> bs = _Cache.Record.dao.load(
				W.create().and("node", id).and("name", "read").and("created", time, W.OP.gte).sort("created", 1), 0,
				24 * 60 * 7);
		if (bs != null && !bs.isEmpty()) {
			this.set("list", bs);
		}
		this.show("/portlet/cache/read.more.html");

	}

}
