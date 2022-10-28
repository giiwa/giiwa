package org.giiwa.app.web.portlet;

import org.giiwa.bean.GLog;
import org.giiwa.dao.Beans;
import org.giiwa.dao.Helper.W;

public class loginfo extends portlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void get() {

		Beans<GLog> bs = GLog.dao
				.load(W.create().and("type1", GLog.TYPE_SECURITY).and("uid", login.getId()).sort("created", -1), 0, 5);

		this.set("list", bs);

		this.show("/portlet/loginfo.html");
	}

}
