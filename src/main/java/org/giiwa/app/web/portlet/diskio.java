package org.giiwa.app.web.portlet;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.giiwa.bean.m._DiskIO;
import org.giiwa.conf.Local;
import org.giiwa.dao.Beans;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.web.Path;

public class diskio extends portlet {

	@Override
	public void get() {

		String name = this.getString("name");
		this.set("node", Local.id());
		this.set("name", name);

		Beans<_DiskIO.Record> bs = _DiskIO.Record.dao.load(W.create("node", Local.id()).and("path", name)
				.and("created", System.currentTimeMillis() - X.AHOUR, W.OP.gte).sort("created", -1), 0, 60);
		if (bs != null && !bs.isEmpty()) {
			Collections.reverse(bs);

			this.set("path", bs.get(0).get("inet"));
			this.set("list", bs);
			this.show("/portlet/diskio.html");
		}

	}

	@Path(path = "data", login = true)
	public void data() {
		String name = this.getString("name");
		this.set("node", Local.id());
		this.set("name", name);

		Beans<_DiskIO.Record> bs = _DiskIO.Record.dao.load(W.create("node", Local.id()).and("path", name)
				.and("created", System.currentTimeMillis() - X.AHOUR, W.OP.gte).sort("created", -1), 0, 60);
		if (bs != null && !bs.isEmpty()) {
			Collections.reverse(bs);

			JSON p1 = JSON.create().append("name", lang.get("disk.reads")).append("color", "#0dad76");
			JSON p2 = JSON.create().append("name", lang.get("disk.writes")).append("color", "#0a5ea0");

			List<JSON> l1 = JSON.createList();
			List<JSON> l2 = JSON.createList();
			bs.forEach(e -> {
				l1.add(JSON.create().append("x", lang.time(e.getCreated(), "m")).append("y", e.reads).append("hint",
						lang.size(e.reads)));
				l2.add(JSON.create().append("x", lang.time(e.getCreated(), "m")).append("y", e.writes).append("hint",
						lang.size(e.writes)));
			});
			p1.append("data", l1);
			p2.append("data", l2);
			this.send(JSON.create().append(X.STATE, 200).append("data", Arrays.asList(p1, p2)));

			return;

		}
		this.send(JSON.create().append(X.STATE, 201));

	}

	@Path(path = "more", login = true)
	public void more() {
		long id = this.getLong("id");
		this.set("id", id);
		String name = this.getString("name");
		this.set("name", name);

		long time = System.currentTimeMillis() - X.AWEEK;

		Beans<_DiskIO.Record> bs = _DiskIO.Record.dao.load(
				W.create("node", Local.id()).and("path", name).and("created", time, W.OP.gte).sort("created", 1), 0,
				7 * 24 * 60);

		if (bs != null && !bs.isEmpty()) {
			this.set("list", bs);
		}
		this.show("/portlet/diskio.more.html");

	}

}
