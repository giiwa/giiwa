package org.giiwa.dao;

import org.giiwa.dao.Helper.W;

public interface IAccess {

	boolean read(String table, Object d);

	boolean write(String table, Object d);
	
	W filter(String table);

}
