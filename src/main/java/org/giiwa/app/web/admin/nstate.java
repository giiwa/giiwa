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

import org.giiwa.bean.*;
import org.giiwa.dao.Beans;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.web.*;

/**
 * web api: /admin/node <br>
 * used to manage user<br>
 * required "access.user.admin"
 * 
 * @author joe
 *
 */
public class nstate extends Controller {


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Path(path = "stat", login = true, access = "access.config.admin")
	public void stat() {

		String id = this.getString("id");

		Beans<Stat> bs = Stat.load("node.load", Stat.TYPE.snapshot, Stat.SIZE.min, W.create().and("dataid", id)
				.and("time", System.currentTimeMillis() - X.AWEEK, W.OP.gte).sort("time", 1), 0, 24 * 60 * 7);

		this.set("list", bs);
		this.show("/admin/node.stat.html");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model.onGet()
	 */
	@Override
	@Path(login = true, access = "access.config.admin")
	public void onGet() {

		W q = W.create().sort("label", 1).sort("ip", 1);

		int s = this.getInt("s");
		int n = this.getInt("n", 50);

		String name = this.getString("name");
		if (!X.isEmpty(name)) {
			W q1 = W.create();
			q1.or("label", name, W.OP.like);
			q1.or("ip", name, W.OP.like);
			q1.or("id", name, W.OP.like);
			q.and(q1);
			this.set("name", name);
		}

		String tag = this.getString("tag");
		if(X.isEmpty(tag)) {
			tag = "cpu";
		}
		this.set("tag", tag);
		
		Beans<Node> bs = Node.dao.load(q, s, n);
		bs.count();

		this.pages(bs, s, n);

		this.show("/admin/nstate.index.html");
	}

	@Path(login = true, path = "tcpclosewait", access = "access.config.admin")
	public void tcpclosewait() {

		W q = W.create().sort("label", 1).sort("ip", 1);

		int s = this.getInt("s");
		int n = this.getInt("n", 50);

		Beans<Node> bs = Node.dao.load(q, s, n);
		bs.count();

		this.pages(bs, s, n);

		this.show("/admin/node.tcpclosewait.html");
	}

}
