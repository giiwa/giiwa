package org.giiwa.framework.bean;

public interface IRole {
	public boolean hasAccess(long uid, String... name) throws Exception;
}
