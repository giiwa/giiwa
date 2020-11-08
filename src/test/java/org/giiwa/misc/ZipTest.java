package org.giiwa.misc;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class ZipTest {

	@Test
	public void test() {
		try {
			Zip.zip(new File("/Users/wujun/d/temp/aaa.zip"), new File("/Users/wujun/d/temp/logs"));

			Zip.unzip(new File("/Users/wujun/d/temp/aaa.zip"), new File("/Users/wujun/d/temp/aa"));

			System.out.println("done");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());

		}
	}

}
