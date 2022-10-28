package org.giiwa.conf;

import org.giiwa.dao.X;
import org.junit.Test;

public class GlobalTest {

	@Test
	public void test() {
		String s = "http://g14.giisoo.com";
		Global.setConfig("server", s);
		Local.setConfig("a", s);
		System.out.println("server=" + Global.getString("server", X.EMPTY));
		System.out.println("server=" + Local.getString("a", X.EMPTY));
	}

}
