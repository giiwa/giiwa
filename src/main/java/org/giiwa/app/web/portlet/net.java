package org.giiwa.app.web.portlet;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.X;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.conf.Local;
import org.giiwa.core.json.JSON;
import org.giiwa.framework.bean.m._Net;
import org.giiwa.framework.web.Path;

public class net extends portlet {

	@Override
	public void get() {

		String name = this.getString("name");
		this.set("node", Local.id());
		this.set("name", name);

		Beans<_Net.Record> bs = _Net.Record.dao.load(W.create("node", Local.id()).and("name", name).sort("created", -1),
				0, 60);
		if (bs != null && !bs.isEmpty()) {
			Collections.reverse(bs);

			this.set("inet", bs.get(0).get("inet"));
			this.set("list", bs);
			this.show("/portlet/net.html");
		}

	}

	@Path(path = "data", login = true)
	public void data() {
		String name = this.getString("name");
		this.set("node", Local.id());
		this.set("name", name);

		Beans<_Net.Record> bs = _Net.Record.dao.load(W.create("node", Local.id()).and("name", name).sort("created", -1),
				0, 60);
		if (bs != null && !bs.isEmpty()) {
			Collections.reverse(bs);

			// {name: "$lang.get('net.rxbytes.speed')", color:'#0dad76', data: [#foreach($c
			// in $list) {x:$this.time($c), y:$c.rxbytes,
			// hint:"$!lang.size($c.rxbytes)"},#end]},
			// {name: "$lang.get('net.txbytes.speed')", color:'#0a5ea0', data: [#foreach($c
			// in $list) {x:$this.time($c), y:$c.txbytes,
			// hint:"$!lang.size($c.txbytes)"},#end]}

			JSON p1 = JSON.create().append("name", lang.get("net.rxbytes.speed")).append("color", "#0dad76");
			JSON p2 = JSON.create().append("name", lang.get("net.txbytes.speed")).append("color", "#0a5ea0");

			List<JSON> l1 = JSON.createList();
			List<JSON> l2 = JSON.createList();
			bs.forEach(e -> {
				l1.add(JSON.create().append("x", lang.time(e.getCreated(), "m")).append("y", e.getRxbytes())
						.append("hint", lang.size(e.getRxbytes())));
				l2.add(JSON.create().append("x", lang.time(e.getCreated(), "m")).append("y", e.getTxbytes())
						.append("hint", lang.size(e.getTxbytes())));
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
		String name = this.getString("name");
		this.set("name", name);

		long time = System.currentTimeMillis() - X.AMONTH;

		Beans<_Net.Record> bs = _Net.Record.dao.load(
				W.create("node", Local.id()).and("name", name).and("created", time, W.OP.gte).sort("created", 1), 0,
				30 * 24 * 60);

		if (bs != null && !bs.isEmpty()) {
			this.set("list", bs);
		}
		this.show("/portlet/net.more.html");

	}

}
