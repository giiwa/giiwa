package org.giiwa.app.web.portlet;

import org.giiwa.bean.m._Disk;
import org.giiwa.conf.Local;
import org.giiwa.dao.Beans;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.web.Path;

public class disk extends portlet {

	@Override
	public void get() {

		Beans<_Disk> bs = _Disk.dao.load(W.create("node", Local.id()).and("updated", System.currentTimeMillis() - X.AHOUR, W.OP.gte).sort("path", 1), 0, 100);
		if (bs != null && !bs.isEmpty()) {
			// Collections.reverse(bs);

			// this.set("disk", bs.get(0));
			this.set("list", bs);
			this.show("/portlet/disk.html");
		}
	}

	@Path(path = "more", login = true)
	public void more() {
		long id = this.getLong("id");
		this.set("id", id);
		String name = this.getString("name");
		this.set("name", name);

		long time = System.currentTimeMillis() - X.AWEEK;

		Beans<_Disk.Record> bs = _Disk.Record.dao.load(
				W.create("node", Local.id()).and("name", name).and("updated", time, W.OP.gte).sort("created", 1), 0,
				24 * 60 * 7);

		if (bs != null && !bs.isEmpty()) {
			this.set("list", bs);
		}
		this.show("/portlet/disk.more.html");

	}

}
