//begin of the Demo Bean
package org.giiwa.demo.bean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.BeanDAO;
import org.giiwa.core.bean.Table;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Column;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;

/**
 * Demo bean
 * 
 * @author joe
 * 
 */
// mapping the table name in database
@Table(name = "tbl_demo")
public class Demo extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(Demo.class);

	// define a DAO for each Bean
	public static final BeanDAO<Long, Demo> dao = BeanDAO.create(Demo.class);

	// mapping the column and field
	@Column(name = X.ID)
	public long id;

	@Column(name = "name")
	public String name;


	// ------------

	// insert a data in database
	public static long create(V v) {
		/**
		 * generate a unique id in distribute system
		 */
		long id = UID.next("demo.id");
		try {
			while (dao.exists(id)) {
				id = UID.next("demo.id");
			}
			dao.insert(v.force(X.ID, id));
			return id;
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
		}
		return -1;
	}

}
// end of the Demo Bean