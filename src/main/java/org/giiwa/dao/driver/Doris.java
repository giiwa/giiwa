package org.giiwa.dao.driver;

import org.giiwa.dao.X;

public class Doris extends MySQL {

	@Override
	public boolean check(String driverinfo) {
		return false;
	}

	@Override
	public String type(String type, int size) {
		String s = super.type(type, size);
		if (X.isIn(s, "text", "longtext")) {
			s = "string";
		}
		return s;
	}

}
