package org.giiwa.app.web;

import java.util.List;

import org.giiwa.bean.Data;
import org.giiwa.dao.Beans;
import org.giiwa.dao.Table;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.web.Controller;
import org.giiwa.web.Path;

/**
 * simple controller for bean directly access db
 * 
 * @author joe
 *
 */
public class beancontroller extends Controller {

	@Path(path = "list", login = true)
	public void list() {

		if (!_method("list")) {
			this.notfound();
			return;
		}

		W q = W.create();

		int s = this.getInt("s");
		int n = this.getInt("n", 10);

		_query(q);

		Beans<Data> bs = Data.load(_table(), q, s, n);
		bs.count();

		this.set("list", bs.asList(e -> {
			return _refine(e);
		})).set("total", bs.getTotal()).set("s", s).set("n", n).set("pages", bs.getTotal() / n).send(200);

	}

	private boolean _method(String s) {
		Table mapping = (Table) this.getClass().getAnnotation(Table.class);
		if (mapping == null) {
			if (log.isErrorEnabled())
				log.error("mapping missed in [" + this.getClass() + "] declaretion");
			return false;
		}
		if (X.isEmpty(mapping.method())) {
			return true;
		}
		List<String> ss = X.asList(X.split(mapping.method(), "[,;]"), s1 -> s1.toString());
		return ss.contains(s);

	}

	/**
	 * get query from request
	 * 
	 * @param q
	 */
	protected void _query(W q) {

	}

	@Path(path = "create", login = true)
	public void create() {

		if (!_method("create")) {
			this.notfound();
			return;
		}

		V v = V.create();

		_fetch(v);

		for (String name : this.names()) {
			v.append(name, this.get(name));
		}

		Data.insert(_table(), v);

		this.set("id", v.value(X.ID)).set(X.MESSAGE, lang.get("save.success")).send(200);
	}

	@Path(path = "detail", login = true)
	public void detail() {

		if (!_method("detail")) {
			this.notfound();
			return;
		}

		Object id = _id(this.get("id"));
		Data d = Data.load(_table(), W.create("id", id));
		if (d == null) {
			this.set(X.ERROR, "参数错误, [id]").send(201);
		} else {
			this.set("data", _refine(d)).send(200);
		}

	}

	@Path(path = "edit", login = true)
	public void edit() {

		if (!_method("edit")) {
			this.notfound();
			return;
		}

		Object id = _id(this.get("id"));

		V v = V.create().ignore(X.ID);

		_fetch(v);

		for (String name : this.names()) {
			v.append(name, this.get(name));
		}

		Data.update(_table(), W.create("id", id), v);

		this.set(X.MESSAGE, lang.get("save.success")).send(200);
	}

	/**
	 * get data from request
	 * 
	 * @param v
	 */
	protected void _fetch(V v) {
		// TODO Auto-generated method stub

	}

	protected JSON _refine(Data e) {
		return e.json();
	}

	@Path(path = "delete", login = true)
	public void delete() {

		if (!_method("delete")) {
			this.notfound();
			return;
		}

		Object id = _id(this.get("id"));

		Data.remove(_table(), W.create(X.ID, id));

		this.set(X.MESSAGE, lang.get("delete.success")).send(200);

	}

	/**
	 * convert the id
	 * 
	 * @param id
	 * @return
	 */
	public Object _id(String id) {
		return id;
	}

	public String _table() {

		Table mapping = (Table) this.getClass().getAnnotation(Table.class);
		if (mapping == null) {
			if (log.isErrorEnabled())
				log.error("mapping missed in [" + this.getClass() + "] declaretion");
			return null;
		}
		return mapping.name();

	}

}
