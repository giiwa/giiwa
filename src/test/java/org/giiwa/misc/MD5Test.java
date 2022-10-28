package org.giiwa.misc;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

public class MD5Test {

	@Test
	public void test() {
		File f = new File("/Users/wujun/d/workspace/giiwa/README.md");
		System.out.println(MD5.md5(f));
		
		
	}

}
