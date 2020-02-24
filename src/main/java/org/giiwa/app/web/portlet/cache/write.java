package org.giiwa.app.web.portlet.cache;

import java.util.Collections;
import java.util.List;

import org.giiwa.app.web.portlet.portlet;
import org.giiwa.conf.Local;
import org.giiwa.dao.Beans;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.dao.bean.m._Cache;
import org.giiwa.json.JSON;
import org.giiwa.web.Path;

public class write extends portlet {

	@Override
	public void get() {

		Beans<_Cache.Record> bs = _Cache.Record.dao.load(W.create("node", Local.id()).and("name", "write")
				.and("created", System.currentTimeMillis() - X.AHOUR, W.OP.gte).sort("created", -1), 0, 60);
		if (bs != null && !bs.isEmpty()) {
			Collections.reverse(bs);

			this.set("list", bs);
			this.show("/portlet/cache/write.html");
		}
	}

	@Path(path = "data", login = true)
	public void data() {

		Beans<_Cache.Record> bs = _Cache.Record.dao.load(W.create("node", Local.id()).and("name", "write")
				.and("created", System.currentTimeMillis() - X.AHOUR, W.OP.gte).sort("created", -1), 0, 60);
		if (bs != null && !bs.isEmpty()) {
			Collections.reverse(bs);

			this.set("list", bs);

			List<JSON> data = JSON.createList();
			for (String[] s : new String[][] { { "max", "#860606" }, { "avg", "#0dad76" }, { "min", "#0a5ea0" } }) {
				JSON p = JSON.create();
				p.append("name", lang.get("mq.write." + s[0])).append("color", s[1]);
				List<JSON> l1 = JSON.createList();
				bs.forEach(e -> {
					l1.add(JSON.create().append("x", lang.time(e.getCreated(), "m")).append("y", e.get(s[0])));
				});
				p.append("data", l1);
				data.add(p);
			}

			this.response(JSON.create().append(X.STATE, 200).append("data", data));
			return;
		}

		this.response(JSON.create().append(X.STATE, 201));

	}

	@Path(path = "more", login = true)
	public void more() {

		long time = System.currentTimeMillis() - X.AWEEK;

		Beans<_Cache.Record> bs = _Cache.Record.dao.load(
				W.create("node", Local.id()).and("name", "write").and("created", time, W.OP.gte).sort("created", 1), 0,
				24 * 60 * 7);
		if (bs != null && !bs.isEmpty()) {
			this.set("list", bs);
		}
		this.show("/portlet/cache/write.more.html");

	}

}
