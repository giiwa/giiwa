package org.giiwa.app.web.admin;

import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.task.Monitor;
import org.giiwa.web.Controller;
import org.giiwa.web.Path;

public class monitor extends Controller {

	@Path(path = "checking", login = true)
	public void checking() {

		long id = this.getLong("id");
		String access = this.get("access");

		JSON jo = Monitor.get(id, access);

		this.send(JSON.create().append(X.STATE, 200).append("data", jo));

	}

}
