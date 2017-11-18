package org.giiwa.demo.bean;

import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.BeanDAO;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Table;
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.bean.Column;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;

/**
 * Demo bean
 * 
 * @author joe
 * 
 */
@Table(name = "tbldemo")
public class Demo extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final BeanDAO<Demo> dao = BeanDAO.create(Demo.class);

	@Column(name = X.ID)
	String id;

	@Column(name = "name")
	String name;

	@Column(name = "content")
	String content;

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getContent() {
		return content;
	}

	// ------------

	public static String create(V v) {
		/**
		 * generate a unique id in distribute system
		 */
		String id = "d" + UID.next("demo.id");
		try {
			while (dao.exists(id)) {
				id = "d" + UID.next("demo.id");
			}
			dao.insert(v.force(X.ID, id));
			return id;
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
		}
		return null;
	}

}
