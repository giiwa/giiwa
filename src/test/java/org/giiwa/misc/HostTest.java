package org.giiwa.misc;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;

import org.junit.Test;

public class HostTest {

	@Test
	public void test() {
		System.out.println(Host.getLocalip());

		System.out.println(Host.getPid());

		File[] ff = File.listRoots();

		System.out.println(Arrays.toString(ff));
	}

}
