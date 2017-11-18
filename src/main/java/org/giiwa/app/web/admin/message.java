/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.app.web.admin;

import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.X;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.json.JSON;
import org.giiwa.framework.bean.Message;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;

/**
 * web api: /admin/accesslog <br>
 * used to access the "accesslog", <br>
 * required "access.logs.admin"
 * 
 * @author joe
 *
 */
public class message extends Model {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model#onGet()
	 */
	@Path(login = true)
	public void onGet() {
		int s = this.getInt("s");
		int n = this.getInt("n", 10);

		W q = W.create("touid", login.getId());
		String name = this.getString("name");
		if (!X.isEmpty(name)) {
			q.and("title", name, W.OP.like);
			this.set("name", name);
		}
		Beans<Message> bs = Message.dao.load(q, s, n);
		bs.setTotal(Message.dao.count(q));
		this.set(bs, s, n);

		this.query.path("/admin/message");
		this.show("/admin/message.index.html");
	}

	@Path(path = "delete", login = true)
	public void delete() {
		String id = this.getString(X.ID);
		String[] ss = X.split(id, "[,; ]");
		if (ss != null) {
			for (String s : ss) {
				Message.dao.delete(W.create("touid", login.getId()).and(X.ID, X.toLong(s)));
			}
		}

		this.response(JSON.create().append(X.STATE, 200).append(X.MESSAGE, "ok"));
	}

	@Path(path = "detail", login = true)
	public void detail() {

		long id = this.getLong("id");

		Message d = Message.dao.load(id);
		if (d.getTouid() != login.getId()) {
			this.deny();
			return;
		}
		if (d.getFlag() == Message.FLAG_UNREAD) {
			Message.dao.update(d.getId(), V.create("flag", Message.FLAG_READ));
		}

		this.set("b", d);
		this.set("id", id);
		this.show("/admin/message.detail.html");
	}

	@Path(path = "count", login = true)
	public void count() {
		int flag = this.getInt("flag", Message.FLAG_UNREAD);
		int n = (int) Message.dao.count(W.create("touid", login.getId()).and("flag", flag));

		this.response(JSON.create().append(X.STATE, 200).append(X.MESSAGE, "ok").append("count", n));
	}

	@Path(path = "star", login = true)
	public void star() {
		long id = this.getLong("id");
		Message m = Message.dao.load(id);
		if (m != null) {
			int star = m.getStar() == 0 ? 1 : 0;
			Message.dao.update(id, V.create("star", star));
			this.response(JSON.create().append(X.STATE, 201).append(X.MESSAGE, "ok").append("star", star));
		} else {
			this.response(JSON.create().append(X.STATE, 201).append(X.MESSAGE, "not found"));
		}
	}

}
