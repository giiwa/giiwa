package org.giiwa.framework.web;

import static org.junit.Assert.*;

import org.giiwa.core.bean.X;
import org.junit.Test;

public class LanguageTest {

	@Test
	public void test() {
		// fail("Not yet implemented");
		long t = System.currentTimeMillis();
		Language lang = Language.getLanguage();
		System.out.println(lang.format(t, "yyyy-MM-dd HH:mm"));
		t = lang.parse("1900-11-11", "yyyy-MM-dd");
		System.out.println("t=" + t);
		System.out.println(lang.format(t, "yyyy-MM-dd HH:mm"));

		System.out.println(X.ADAY);

		// lang.data.put("test", new String[] { "您已经被添加到项目[%s]，角色[%s]。" });
		// System.out.println(lang.format("test", "yyyy-MM-dd HH:mm", "asdasd"));

		// yyyy-MM-ddTHH:mm:ss
		t = System.currentTimeMillis();
		System.out.println(lang.format(t, "yyyy-MM-dd'T'HH:mm"));
	}

}
