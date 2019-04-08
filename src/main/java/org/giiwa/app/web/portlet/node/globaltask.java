package org.giiwa.app.web.portlet.node;

import java.util.Arrays;
import java.util.List;

import org.giiwa.app.web.portlet.portlet;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.X;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.json.JSON;
import org.giiwa.framework.bean.Node;
import org.giiwa.framework.bean.Stat;
import org.giiwa.framework.web.Path;

public class globaltask extends portlet {

	@Override
	public void get() {

		String id = this.getString("id");
		this.set("id", id);
		Node n = Node.dao.load(id);
		this.set("n", n);

		Beans<Stat> bs = Stat.load("node.load." + id, Stat.TYPE.snapshot, Stat.SIZE.min,
				W.create().and("time", System.currentTimeMillis() - X.AHOUR, W.OP.gte).sort("time", 1), 0, 60);
		if (bs != null && !bs.isEmpty()) {

			this.set("list", bs);
			this.show("/portlet/node/globaltask.html");
		}
	}

	@Path(path = "data", login = true)
	public void data() {

		String id = this.getString("id");
		Beans<Stat> bs = Stat.load("node.load." + id, Stat.TYPE.snapshot, Stat.SIZE.min,
				W.create().and("time", System.currentTimeMillis() - X.AHOUR, W.OP.gte).sort("time", 1), 0, 60);
		if (bs != null && !bs.isEmpty()) {

			this.set("list", bs);

			JSON p = JSON.create();
			p.append("name", lang.get("cpu.usage")).append("color", "#666");
			List<JSON> l1 = JSON.createList();
			bs.forEach(e -> {
				l1.add(JSON.create().append("x", lang.time(e.getCreated(), "m")).append("y", e.getLong("n1")));
			});
			p.append("data", l1);
			this.response(JSON.create().append(X.STATE, 200).append("data", Arrays.asList(p)));
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

		Beans<Stat> bs = Stat.load("node.load." + id, Stat.TYPE.snapshot, Stat.SIZE.min,
				W.create().and("time", time, W.OP.gte).sort("time", 1), 0, 30 * 24 * 60);
		if (bs != null && !bs.isEmpty()) {
			this.set("list", bs);
		}
		this.show("/portlet/node/globaltask.more.html");

	}

}
