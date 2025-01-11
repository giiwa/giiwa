package org.giiwa.misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.giiwa.dao.X;
import org.junit.Test;

public class IOUtilTest {

	@Test
	public void testRead() {

		File f = new File("/Users/joe/Downloads/test.xml");
		try {
			byte[] bb = X.IO.read(new FileInputStream(f), true);

			System.out.println(new String(bb));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
