package org.giiwa.framework.bean;

import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.BeanDAO;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Column;
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.bean.Table;
import org.giiwa.core.bean.X;

@Table(name = "gi_footprint")
public class Footprint extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static BeanDAO<String, Footprint> dao = BeanDAO.create(Footprint.class);

	@Column(name = "table")
	String table;

	@Column(name = "dataid")
	String dataid;

	@Column(name = "field")
	String field;

	@Column(name = "uid")
	long uid;

	@Column(name = "data")
	Object data;

	transient User uid_obj;

	public User getUid_obj() {
		if (uid_obj == null) {
			uid_obj = User.dao.load(uid);
		}
		return uid_obj;
	}

	public String getTable() {
		return table;
	}

	public String getDataid() {
		return dataid;
	}

	public String getField() {
		return field;
	}

	public long getUid() {
		return uid;
	}

	public Object getData() {
		return data;
	}

	public static boolean create(Bean p, V v, long uid) {
		/**
		 * compare each data in V
		 */
		if (p == null || v == null)
			return false;

		String table = Helper.getTable(p.getClass());
		if (X.isEmpty(table)) {
			return false;
		}

		String dataid = p.get(X.ID).toString();

		for (String name : v.names()) {
			if (X.isIn(name, "updated", "created", "_id", "id"))
				continue;
			Object v0 = p == null ? null : p.get(name);
			Object v1 = v.value(name);
			if (!X.isSame(v0, v1)) {
				dao.insert(V.create("table", table).append("dataid", dataid).append("field", name).append("data", v1)
						.append("uid", uid));
			}
		}

		return true;
	}

	public static boolean create(String table, V v, long uid) {
		/**
		 * compare each data in V
		 */
		if (X.isEmpty(table) || v == null)
			return false;

		String dataid = v.value(X.ID).toString();
		if (dataid == null)
			return false;

		for (String name : v.names()) {
			if (X.isIn(name, "updated", "created", "_id", "id"))
				continue;
			Object v1 = v.value(name);
			dao.insert(V.create("table", table).append("dataid", dataid).append("field", name).append("data", v1)
					.append("uid", uid));
		}

		return true;
	}

	public static boolean create(BeanDAO<?, ? extends Bean> b, V v, long uid) {
		/**
		 * compare each data in V
		 */
		String table = b.tableName();
		if (X.isEmpty(table) || v == null)
			return false;

		String dataid = v.value(X.ID).toString();
		if (dataid == null)
			return false;

		for (String name : v.names()) {
			if (X.isIn(name, "updated", "created", "_id", "id"))
				continue;
			Object v1 = v.value(name);
			dao.insert(V.create("table", table).append("dataid", dataid).append("field", name).append("data", v1)
					.append("uid", uid));
		}

		return true;
	}

	public static Beans<Footprint> load(String table, String dataid, String field, int s, int n) {
		return dao.load(W.create().and("table", table).and("dataid", dataid).and("field", field).sort("created", -1), s,
				n);
	}

	public static Beans<Footprint> load(BeanDAO<?, ? extends Bean> b, String dataid, String field, int s, int n) {
		return dao.load(
				W.create().and("table", b.tableName()).and("dataid", dataid).and("field", field).sort("created", -1), s,
				n);
	}

}
