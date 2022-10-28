package org.giiwa.app.web.portlet.db;

import java.util.Collections;

import org.giiwa.app.web.portlet.portlet;
import org.giiwa.bean.m._DB;
import org.giiwa.conf.Global;
import org.giiwa.dao.Beans;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.web.Path;

public class cursor extends portlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void get() {

		W q = W.create().and("node", Global.id()).and("name", "status")
				.and("created", System.currentTimeMillis() - X.AHOUR, W.OP.gte).sort("created", -1);
		_DB.Record.dao.optimize(q);
		
		Beans<_DB.Record> bs = _DB.Record.dao.load(q, 0, 60);
		if (bs != null && !bs.isEmpty()) {
			Collections.reverse(bs);
			this.set("list", bs);
		}

		this.show("/portlet/db/cursor.html");
	}

	@Path(path = "more", login = true)
	public void more() {

		long time = System.currentTimeMillis() - X.ADAY * 2;

		Beans<_DB.Record> bs = _DB.Record.dao.load(
				W.create().and("node", Global.id()).and("name", "status").and("created", time, W.OP.gte).sort("created", -1), 0,
				24 * 60 * 2);
		if (bs != null && !bs.isEmpty()) {
			Collections.reverse(bs);
			this.set("list", bs);
		}

		this.show("/portlet/db/cursor.more.html");

	}

}
