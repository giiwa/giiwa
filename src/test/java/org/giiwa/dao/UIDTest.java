package org.giiwa.dao;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

public class UIDTest {

	@Test
	public void test() {
		try {
			TimeStamp t = TimeStamp.create();
			int[] ii = UID.random("12131", 100);
			System.out.println("cost=" + t.past() + ", " + Arrays.toString(ii));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
