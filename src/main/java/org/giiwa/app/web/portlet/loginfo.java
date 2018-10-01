package org.giiwa.app.web.portlet;

import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.framework.bean.GLog;

public class loginfo extends portlet {

	@Override
	public void get() {

		Beans<GLog> bs = GLog.dao
				.load(W.create("type1", GLog.TYPE_SECURITY).and("uid", login.getId()).sort("created", -1), 0, 5);

		this.set("list", bs);

		this.show("/portlet/loginfo.html");
	}

}
