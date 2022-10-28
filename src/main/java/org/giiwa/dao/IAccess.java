package org.giiwa.dao;

import java.sql.SQLException;

import org.giiwa.dao.Helper.W;

public interface IAccess {

	boolean read(String type, String table, Object d);

	boolean checkWrite(String type, String table, Object d) throws SQLException;

	W filter(String type, String table);

}
