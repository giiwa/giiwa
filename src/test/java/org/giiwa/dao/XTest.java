package org.giiwa.dao;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

public class XTest {

	@Test
	public void test() {
		String s = "a-c";
		char[] ss = X.range2(s, "-");
		System.out.println(Arrays.toString(ss));

		System.out.println(X.toLong("9700262001", -1));
	}

}
