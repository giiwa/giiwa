package org.giiwa.dao;

import static org.junit.Assert.*;

import org.junit.Test;

public class TimeStampTest {

	@Test
	public void test() {
		TimeStamp t = TimeStamp.create();
		System.out.println(t.past());
		System.out.println(t.pastns());
		System.out.println(t.past());
	}

}
