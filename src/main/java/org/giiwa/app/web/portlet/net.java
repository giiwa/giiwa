package org.giiwa.app.web.portlet;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.giiwa.bean.Node;
import org.giiwa.bean.m._Net;
import org.giiwa.conf.Local;
import org.giiwa.dao.Beans;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.web.Path;

public class net extends portlet {

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

		String name = this.getString("name");
		this.set("name", name);

		W q = W.create().and("node", id).and("name", name)
				.and("created", System.currentTimeMillis() - X.AHOUR, W.OP.gte).sort("created", -1);
		_Net.Record.dao.optimize(q);
		
		Beans<_Net.Record> bs = _Net.Record.dao.load(q, 0, 60);
		if (bs != null && !bs.isEmpty()) {
			Collections.reverse(bs);

			this.set("inet", bs.get(0).get("inet"));
			this.set("list", bs);
		}

		long max1 = X.toLong(1.1 * X.toLong(_Net.Record.dao.max("rxbytes",
				W.create().and("created", System.currentTimeMillis() - X.AHOUR, W.OP.gte))));
		long max2 = X.toLong(1.1 * X.toLong(_Net.Record.dao.max("txbytes",
				W.create().and("created", System.currentTimeMillis() - X.AHOUR, W.OP.gte))));

		this.set("max", Math.max(max1, max2));

		this.show("/portlet/net.html");

	}

	@Path(path = "data", login = true)
	public void data() {

		String id = this.getString("id");
		if (X.isEmpty(id)) {
			id = Local.id();
		}
		this.set(X.ID, id);
		Node n = Node.dao.load(id);

		String name = this.getString("name");
		this.set("name", name);

		Beans<_Net.Record> bs = _Net.Record.dao.load(W.create().and("node", id).and("name", name)
				.and("created", System.currentTimeMillis() - X.AHOUR, W.OP.gte).sort("created", -1), 0, 60);
		if (bs != null && !bs.isEmpty()) {
			Collections.reverse(bs);

			// {name: "$lang.get('net.rxbytes.speed')", color:'.0dad76', data: [.foreach($c
			// in $list) {x:$this.time($c), y:$c.rxbytes,
			// hint:"$!lang.size($c.rxbytes)"},.end]},
			// {name: "$lang.get('net.txbytes.speed')", color:'.0a5ea0', data: [.foreach($c
			// in $list) {x:$this.time($c), y:$c.txbytes,
			// hint:"$!lang.size($c.txbytes)"},.end]}

			JSON p1 = JSON.create().append("name", (n != null ? n.label : "") + " - " + lang.get("net.rxbytes.speed"))
					.append("color", "#0dad76");
			JSON p2 = JSON.create().append("name", (n != null ? n.label : "") + " - " + lang.get("net.txbytes.speed"))
					.append("color", "#0a5ea0");

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
			this.send(JSON.create().append(X.STATE, 200).append("data", Arrays.asList(p1, p2)));

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

		String name = this.getString("name");
		this.set("name", name);

		long time = System.currentTimeMillis() - X.AWEEK;

		Beans<_Net.Record> bs = _Net.Record.dao.load(
				W.create().and("node", id).and("name", name).and("created", time, W.OP.gte).sort("created", 1), 0,
				7 * 24 * 60);

		if (bs != null && !bs.isEmpty()) {
			this.set("list", bs);
		}
		this.show("/portlet/net.more.html");

	}

}
