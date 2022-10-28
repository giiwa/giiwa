package org.giiwa.dfile;

import org.giiwa.bean.Disk;
import org.junit.Test;

public class DFileTest {

	@Test
	public void test() {

		try {
			String s = "/f/g/f5wws3c7mfzhi2ldnrss63jpiyxxcl2if4ytinjsgi3dinjwgeytknbwgi4dambxf5qs44demy/a.pdf";
			DFile f1 = Disk.getByUrl(s);
			System.out.println(f1.getFilename());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testPdf() {

		try {
			String filename = "/Users/joe/d/temp/a.pdf";
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
