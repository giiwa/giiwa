package org.giiwa.bean;

import org.giiwa.bean.Stat.SIZE;
import org.giiwa.dao.X;
import org.junit.Test;

public class StatTest {

	@Test
	public void test() {
		long t = System.currentTimeMillis() + X.AMINUTE * 30;

		System.out.println(Stat.format(t, Stat.SIZE.min));
		System.out.println(Stat.format(t, Stat.SIZE.m10));
		System.out.println(Stat.format(t, Stat.SIZE.m15));
		System.out.println(Stat.format(t, Stat.SIZE.m30));

		SIZE s1 = SIZE.m10;
		String s2 = Stat.format(t, s1);
		long[] ss = Stat.time(s1, s2);

		System.out.println(s2 + ", " + Stat.format(ss[0], s1) + ", " + Stat.format(ss[1], s1));
	}

}
