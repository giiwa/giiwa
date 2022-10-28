package org.giiwa.app.web.portlet;

import org.giiwa.bean.Node;
import org.giiwa.bean.m._Disk;
import org.giiwa.conf.Local;
import org.giiwa.dao.Beans;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.web.Path;

public class disk extends portlet {

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
			this.set("name", n.label);
		} else {
			this.set("name", id);
		}

		W q = W.create().and("node", id).and("updated", System.currentTimeMillis() - X.AHOUR, W.OP.gte).sort("path", 1);
		_Disk.dao.optimize(q);

		Beans<_Disk> bs = _Disk.dao.load(q, 0, 100);
		if (bs != null && !bs.isEmpty()) {
			// Collections.reverse(bs);
//			this.set("disk", bs.get(0));
			this.set("list", bs);
		}

		this.show("/portlet/disk.html");
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
			this.set("name", n.label);
		} else {
			this.set("name", id);
		}

		String name = this.getString("name");
		this.set("name", name);

		long time = System.currentTimeMillis() - X.AWEEK;

		W q = W.create().and("node", Local.id()).and("name", name).and("updated", time, W.OP.gte).sort("created", 1);

		Beans<_Disk.Record> bs = _Disk.Record.dao.load(q, 0, 24 * 60 * 7);

		if (bs != null && !bs.isEmpty()) {
			this.set("list", bs);
		}
		this.show("/portlet/disk.more.html");

	}

}
