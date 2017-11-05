package org.giiwa.framework.bean;

public interface IRole {
	public boolean hasAccess(String... name) throws Exception;
}
