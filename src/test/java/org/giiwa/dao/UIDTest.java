package org.giiwa.dao;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class UIDTest {

	@Test
	public void test() {

		try {

			String id = UID.random();
			System.out.println(id);
			System.out.println(id.length());

			TimeStamp t = TimeStamp.create();
			int[] ii = UID.random("1234567890", 100);
			System.out.println("cost=" + t.past() + ", " + Arrays.toString(ii));

			StringBuilder sb = new StringBuilder();
			sb.append(UID.digital(4));
			sb.append("-");
			sb.append(UID.digital(4));
			sb.append("-");
			sb.append(UID.digital(4));
			sb.append("-");
			sb.append(UID.digital(4));

			System.out.println(sb.toString());

		} catch (Exception e) {
			e.printStackTrace();

			fail(e.getMessage());
		}
	}

	@Test
	public void testHash() {
		String s = "aaaa";

		System.out.println(UID.hash(s));
		System.out.println(UID.hash52(s));

	}

}
