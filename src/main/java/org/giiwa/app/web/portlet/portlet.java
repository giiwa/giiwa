package org.giiwa.app.web.portlet;

import org.giiwa.dao.Bean;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.web.Controller;
import org.giiwa.web.Path;

public class portlet extends Controller {

	@Path()
	public final void onGet() {

		login = this.getUser();
		W q = W.create("uri", this.uri);
		if (login == null) {
			q.and("uid", 0);
		} else {
			q.and("uid", login.getId());
		}

		get();
	}

	public void get() {
	}

	/**
	 * @deprecated
	 * @param b
	 * @return
	 */
	public long time(Bean b) {
		return b.getCreated() + X.AHOUR * 8;
	}

}
