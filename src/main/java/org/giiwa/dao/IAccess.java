package org.giiwa.dao;

public interface IAccess {

	boolean read(String table, Object d);

	boolean write(String table, Object d);

}
