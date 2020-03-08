package org.giiwa.demo.web.admin;


import org.giiwa.conf.Global;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;

public class demosetting extends org.giiwa.app.web.admin.setting {

	@Override
	public void get() {

		this.settingPage("/admin/demo.setting.html");

	}

	@Override
	public void set() {

		Global.setConfig("demo.url", this.getString("demo.url"));
		Global.setConfig("demo.enabled", X.isSame(this.getString("demo.enabled"), "on") ? 1 : 0);

		this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE, lang.get("save.success")));

	}

}
