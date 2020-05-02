package org.giiwa.app.web.portlet;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.giiwa.bean.m._Memory2;
import org.giiwa.conf.Local;
import org.giiwa.dao.Beans;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.web.Path;

public class mem2 extends portlet {

	@Override
	public void get() {

		long id = this.getLong("id");
		this.set("id", id);

		Beans<_Memory2.Record> bs = _Memory2.Record.dao.load(W.create("node", Local.id())
				.and("created", System.currentTimeMillis() - X.AHOUR, W.OP.gte).sort("created", -1), 0, 60);
		if (bs != null && !bs.isEmpty()) {
//			this.set("total", bs.get(0).getLong("total"));
			Collections.reverse(bs);

			this.set("list", bs);
			this.show("/portlet/mem2.html");
		}

	}

	@Path(path = "data", login = true)
	public void data() {
		long id = this.getLong("id");
		this.set("id", id);

		Beans<_Memory2.Record> bs = _Memory2.Record.dao.load(W.create("node", Local.id())
				.and("created", System.currentTimeMillis() - X.AHOUR, W.OP.gte).sort("created", -1), 0, 60);
		if (bs != null && !bs.isEmpty()) {
			Collections.reverse(bs);

			// {name: "$lang.get('mem.used')", color:'.860606', data: [.foreach($c in $list)
			// {x:$this.time($c), y:$c.used, hint:"$lang.size($c.used)"},.end]},
			// {name: "$lang.get('mem.free')", color:'.0dad76', data: [.foreach($c in $list)
			// {x:$this.time($c), y:$c.free, hint:"$lang.size($c.free)"},.end]}

			JSON p1 = JSON.create().append("name", lang.get("mem.used")).append("color", "#860606");

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

		long time = System.currentTimeMillis() - X.AWEEK;

		Beans<_Memory2.Record> bs = _Memory2.Record.dao
				.load(W.create("node", Local.id()).and("created", time, W.OP.gte).sort("created", -1), 0, 60 * 24 * 2);

		if (bs != null && !bs.isEmpty()) {
			Collections.reverse(bs);
			this.set("list", bs);
		}
		this.show("/portlet/mem2.more.html");
	}

}
