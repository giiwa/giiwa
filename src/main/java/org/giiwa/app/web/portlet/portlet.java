package org.giiwa.app.web.portlet;

import java.util.ArrayList;
import java.util.List;

import org.giiwa.app.web.admin.profile;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.json.JSON;
import org.giiwa.framework.bean.Portlet;
import org.giiwa.framework.bean.User;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;

public class portlet extends Model {

	@Path(path = "delete", login = true)
	public final void delete() {

		this.show("/admin/portlet.html");
	}

	@Path()
	public final void onGet() {

		login = this.getUser();
		W q = W.create("uri", this.uri);
		if (login == null) {
			q.and("uid", 0);
		} else {
			q.and("uid", login.getId());
		}

		portlet = Portlet.dao.load(q);
		if (portlet == null) {
			portlet = Portlet.dao.load(W.create("uri", this.uri).and("uid", 0));
		}

		get();
	}

	protected Portlet portlet;

	public void get() {

	}

}
