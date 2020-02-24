package org.giiwa.app.web.portlet;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.giiwa.conf.Local;
import org.giiwa.dao.Beans;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.dao.bean.m._CPU;
import org.giiwa.json.JSON;
import org.giiwa.web.Path;

public class cpu extends portlet {

	@Override
	public void get() {

		Beans<_CPU.Record> bs = _CPU.Record.dao.load(W.create("node", Local.id())
				.and("created", System.currentTimeMillis() - X.AHOUR, W.OP.gte).sort("created", -1), 0, 60);
		if (bs != null && !bs.isEmpty()) {
			Collections.reverse(bs);

			this.set("list", bs);
			this.show("/portlet/cpu.html");
		}
	}

	@Path(path = "data", login = true)
	public void data() {

		Beans<_CPU.Record> bs = _CPU.Record.dao.load(W.create("node", Local.id())
				.and("created", System.currentTimeMillis() - X.AHOUR, W.OP.gte).sort("created", -1), 0, 60);
		if (bs != null && !bs.isEmpty()) {
			Collections.reverse(bs);

			this.set("list", bs);

			JSON p = JSON.create();
			p.append("name", lang.get("cpu.usage")).append("color", "#860606");
			List<JSON> l1 = JSON.createList();
			bs.forEach(e -> {
				l1.add(JSON.create().append("x", lang.time(e.getCreated(), "m")).append("y", e.getUsage()));
			});
			p.append("data", l1);
			this.response(JSON.create().append(X.STATE, 200).append("data", Arrays.asList(p)));
			return;
		}
		this.response(JSON.create().append(X.STATE, 201));
	}

	@Path(path = "more", login = true)
	public void more() {

		long time = System.currentTimeMillis() - X.AWEEK;

		Beans<_CPU.Record> bs = _CPU.Record.dao
				.load(W.create("node", Local.id()).and("created", time, W.OP.gte).sort("created", 1), 0, 24 * 60 * 7);
		if (bs != null && !bs.isEmpty()) {
			this.set("list", bs);
		}
		this.show("/portlet/cpu.more.html");

	}

}
