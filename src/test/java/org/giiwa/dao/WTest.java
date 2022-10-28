package org.giiwa.dao;

import org.giiwa.dao.Helper.W;
import org.junit.Test;

public class WTest {

	@Test
	public void test() {
		// var q = X.query('cip_info').and('cip_createdate >= 1641744000000').and('isbn
		// != null').toSQL()
		W q = W.create();
		q.and("cip_createdate >= 1641744000000");
		q.and("isbn != null");
		System.out.println(q.toSQL());
	}

}
