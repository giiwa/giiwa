package org.giiwa.dao;

import java.sql.SQLException;

import org.giiwa.dao.Helper.W;

public interface IAccess {

	boolean read(String table, Object d);

	boolean checkWrite(String table, Object d) throws SQLException;

	W filter(String table);

}
