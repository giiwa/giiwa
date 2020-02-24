package org.giiwa.app.web.portlet.node;

import java.util.Arrays;
import java.util.List;

import org.giiwa.app.web.portlet.portlet;
import org.giiwa.bean.Node;
import org.giiwa.bean.Stat;
import org.giiwa.dao.Beans;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.web.Path;

public class pending extends portlet {

	@Override
	public void get() {

		String id = this.getString("id");
		this.set("id", id);
		Node n = Node.dao.load(id);
		this.set("n", n);

		Beans<Stat> bs = Stat.load("node.load", Stat.TYPE.snapshot, Stat.SIZE.min, W.create().and("dataid", id)
				.and("time", System.currentTimeMillis() - X.AHOUR, W.OP.gte).sort("time", 1), 0, 60);
		if (bs != null && !bs.isEmpty()) {

			long max = Stat.max("n4", "node.load", Stat.TYPE.snapshot, Stat.SIZE.min,
					W.create().and("time", System.currentTimeMillis() - X.AHOUR, W.OP.gte));

			this.set("max", max);
			this.set("list", bs);

			this.show("/portlet/node/pending.html");
		}
	}

	@Path(path = "data", login = true)
	public void data() {

		String id = this.getString("id");
		Beans<Stat> bs = Stat.load("node.load", Stat.TYPE.snapshot, Stat.SIZE.min, W.create().and("dataid", id)
				.and("time", System.currentTimeMillis() - X.AHOUR, W.OP.gte).sort("time", 1), 0, 60);
		if (bs != null && !bs.isEmpty()) {

			long max = Stat.max("n4", "node.load", Stat.TYPE.snapshot, Stat.SIZE.min,
					W.create().and("time", System.currentTimeMillis() - X.AHOUR, W.OP.gte));

			JSON p = JSON.create();
			p.append("name", lang.get("cpu.usage")).append("color", "#25840a");
			List<JSON> l1 = JSON.createList();
			bs.forEach(e -> {
				l1.add(JSON.create().append("x", lang.time(e.getLong("time"), "m")).append("y", e.getLong("n4")));
			});
			p.append("data", l1);
			this.response(JSON.create().append(X.STATE, 200).append("data", Arrays.asList(p)).append("max", max));
			return;
		}
		this.response(JSON.create().append(X.STATE, 201));
	}

	@Path(path = "more", login = true)
	public void more() {

		String id = this.getString("id");
		Node n = Node.dao.load(id);
		this.set("n", n);

		long time = System.currentTimeMillis() - X.AMONTH;

		Beans<Stat> bs = Stat.load("node.load", Stat.TYPE.snapshot, Stat.SIZE.min,
				W.create().and("dataid", id).and("time", time, W.OP.gte).sort("time", 1), 0, 30 * 24 * 60);
		if (bs != null && !bs.isEmpty()) {
			this.set("list", bs);
		}
		this.show("/portlet/node/pending.more.html");

	}

}
