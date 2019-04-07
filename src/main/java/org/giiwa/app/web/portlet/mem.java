package org.giiwa.app.web.portlet;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.X;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.conf.Local;
import org.giiwa.core.json.JSON;
import org.giiwa.framework.bean.m._Memory;
import org.giiwa.framework.web.Path;

public class mem extends portlet {

	@Override
	public void get() {

		long id = this.getLong("id");
		this.set("id", id);

		Beans<_Memory.Record> bs = _Memory.Record.dao.load(W.create("node", Local.id()).and("created", System.currentTimeMillis() - X.AHOUR, W.OP.gte).sort("created", -1), 0, 60);
		if (bs != null && !bs.isEmpty()) {
			Collections.reverse(bs);

			this.set("total", bs.get(0).getLong("total"));
			this.set("list", bs);
			this.show("/portlet/mem.html");
		}

	}

	@Path(path = "data", login = true)
	public void data() {
		long id = this.getLong("id");
		this.set("id", id);

		Beans<_Memory.Record> bs = _Memory.Record.dao.load(W.create("node", Local.id()).and("created", System.currentTimeMillis() - X.AHOUR, W.OP.gte).sort("created", -1), 0, 60);
		if (bs != null && !bs.isEmpty()) {
			Collections.reverse(bs);

			// {name: "$lang.get('mem.used')", color:'.860606', data: [.foreach($c in $list)
			// {x:$this.time($c), y:$c.used, hint:"$lang.size($c.used)"},.end]},
			// {name: "$lang.get('mem.free')", color:'.0dad76', data: [.foreach($c in $list)
			// {x:$this.time($c), y:$c.free, hint:"$lang.size($c.free)"},.end]}

			JSON p1 = JSON.create().append("name", lang.get("mem.used")).append("color", "#860606");
			JSON p2 = JSON.create().append("name", lang.get("mem.free")).append("color", "#0dad76");

			List<JSON> l1 = JSON.createList();
			List<JSON> l2 = JSON.createList();
			bs.forEach(e -> {
				l1.add(JSON.create().append("x", lang.time(e.getCreated(), "m")).append("y", e.getUsed()).append("hint",
						lang.size(e.getUsed())));
				l2.add(JSON.create().append("x", lang.time(e.getCreated(), "m")).append("y", e.getFree()).append("hint",
						lang.size(e.getFree())));
			});
			p1.append("data", l1);
			p2.append("data", l2);
			this.response(JSON.create().append(X.STATE, 200).append("data", Arrays.asList(p1, p2)));

			return;

		}
		this.response(JSON.create().append(X.STATE, 201));

	}

	@Path(path = "more", login = true)
	public void more() {
		long id = this.getLong("id");
		this.set("id", id);

		long time = System.currentTimeMillis() - X.AMONTH;

		Beans<_Memory.Record> bs = _Memory.Record.dao
				.load(W.create("node", Local.id()).and("created", time, W.OP.gte).sort("created", 1), 0, 30 * 24 * 60);

		if (bs != null && !bs.isEmpty()) {
			this.set("list", bs);
		}
		this.show("/portlet/mem.more.html");
	}

}
