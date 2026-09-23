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
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.web.*;

/**
 * web api: /admin/unit <br>
 * used to manage user<br>
 * required "access.user.admin"
 * 
 * @author joe
 *
 */
public class unit extends Controller {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Adds the.
	 */
	@Path(path = "create", login = true, access = "access.config.admin", oplog = true)
	public void create() {
		if (method.isPost()) {

			try {

				V v = V.create(X.NAME, this.get(X.NAME));
				v.append("no", this.get("no"));
				v.append("memo", this.get("memo"));
				v.append("parent", this.getLong("parent"));

				long id = Unit.create(v);

				this.send(JSON.create().append(X.ID, id).append(X.STATE, 200).append(X.MESSAGE,
						lang.get("save.success")));
				return;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				GLog.oplog.error(unit.class, "create", e.getMessage(), e, login, this.ip());
				this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE,
						lang.get("save.failed") + ":" + e.getMessage()));
				return;
			}

		}

		Beans<Unit> l1 = Unit.dao.load(W.create().sort("no"), 0, 1024);
		this.set("units", l1);

		this.show("/admin/unit.create.html");
	}

	/**
	 * Delete.
	 */
	@Path(path = "delete", login = true, access = "access.config.admin", oplog = true, loglevel="warn")
	public void delete() {

		long id = this.getLong(X.ID);
		Unit e = Unit.dao.load(id);
		if (e == null) {
			this.set(X.ERROR, "miss parameter [id]").send(201);
			return;
		}

		try {
			if (User.exists(W.create().and("unitid", id))) {
				this.set(X.ERROR, "exists user under the unit[" + e.name + "]").send(201);
				return;
			}

			if (User.exists(W.create().and("parent", id))) {
				this.set(X.ERROR, "exists unit under the unit[" + e.name + "]").send(201);
				return;
			}

			Unit.dao.delete(id);

			this.set(X.MESSAGE, lang.get("delete.success")).send(200);
		} catch (Exception err) {
			log.error(err.getMessage(), err);
			this.set(X.ERROR, err.getMessage()).send(201);
		}

	}

	/**
	 * Edits the user.
	 */
	@Path(path = "edit", login = true, access = "access.config.admin", oplog = true)
	public void edit() {

		long id = this.getLong(X.ID);

		if (method.isPost()) {

			try {

				V v = V.create(X.NAME, this.get(X.NAME));
				v.append("no", this.get("no"));
				v.append("memo", this.get("memo"));
				v.append("parent", this.getLong("parent"));

				Unit.dao.update(id, v);

				this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("save.success")));
				return;
			} catch (Exception e) {
				this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE,
						lang.get("save.failed") + ":" + e.getMessage()));
				return;
			}

		} else {

			Unit u = Unit.dao.load(id);
			if (u != null) {
				this.copy(u.json());

				Beans<Unit> l1 = Unit.dao.load(W.create().sort("no"), 0, 1024);
				this.set("units", l1);

				this.set(X.ID, id);
				this.show("/admin/unit.edit.html");
				return;
			}

			this.set(X.ERROR, lang.get("select.required"));
			onGet();

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model.onGet()
	 */
	@Override
	@Path(login = true, access = "access.config.admin")
	public void onGet() {

		String name = this.getString(X.NAME);
		W q = W.create();
		if (X.isEmpty(this.path) && !X.isEmpty(name)) {
			W list = W.create();

			list.or("no", name, W.OP.like);
			list.or(X.NAME, name, W.OP.like);
			if (X.isNumber(name)) {
				list.or(X.ID, X.toLong(name));
			}
			q.and(list);

			this.set(X.NAME, name);
		}

		int s = this.getInt(X.S);
		int n = this.getInt(X.N, X.ITEMS_PER_PAGE);

		q.and(X.ID, 0, W.OP.gt).sort("no", 1);

		Unit.dao.optimize(q);

		Beans<Unit> bs = Unit.dao.load(q, s, n);
		if (bs != null) {
			bs.count();
		}
		this.pages(bs, s, n);

		this.show("/admin/unit.index.html");
	}

}
