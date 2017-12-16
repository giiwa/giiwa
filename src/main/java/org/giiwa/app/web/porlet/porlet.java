package org.giiwa.app.web.porlet;

import java.util.ArrayList;
import java.util.List;

import org.giiwa.app.web.admin.profile;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.framework.bean.Porlet;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;

public class porlet extends Model {

	private static List<Class<? extends porlet>> porlets = new ArrayList<Class<? extends porlet>>();

	public static final void register(Class<? extends porlet> p) {
		if (!porlets.contains(p)) {
			porlets.add(p);
		}
		profile.register("myporlet", myporlet.class);
	}

	@Path(path = "list", login = true)
	public final void list() {
		this.set("list", porlets);
		this.show("/admin/porlet.html");
	}

	@Path()
	public final void onGet() {

		// log.debug("porlet/onGet");

		login = this.getUser();
		W q = W.create("uri", this.uri);
		if (login == null) {
			q.and("uid", 0);
		} else {
			q.and("uid", login.getId());
		}

		porlet = Porlet.dao.load(q);
		if (porlet == null) {
			porlet = Porlet.dao.load(W.create("uri", this.uri).and("uid", 0));
		}

		get();
	}

	protected Porlet porlet;

	public void get() {

	}

	@Path(path = "setup")
	public void setup() {

	}

	public static class myporlet extends profile {

		@Override
		public void set() {
		}

		@Override
		public void get() {
		}

	}

}
