package org.giiwa.bean;

public interface IRole {
	public boolean hasAccess(long uid, String... name) throws Exception;
}
