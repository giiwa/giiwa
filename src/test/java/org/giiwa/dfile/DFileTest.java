package org.giiwa.dfile;

import org.giiwa.bean.Disk;
import org.giiwa.misc.Url;
import org.junit.Test;

public class DFileTest {

	@SuppressWarnings("deprecation")
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

	@SuppressWarnings("unused")
	@Test
	public void testPdf() {

		try {
			String filename = "/Users/joe/d/temp/a.pdf";

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testFilename() {
		String s = "svg%2Bxml%3Bcharset%3Dutf_8%2C%253Csvg%2520xmlns%253D%2527http%253A%252F%252Fwww.w3.org%252F2000%252_g14uoyykthuh4.org%252F2000%252Fsvg%2527%2520width%253D%25271200%2527%2520height%253D%2527800%2527%2520style%253D%2527background%253Atransparent%2527%252F%253E";
		System.out.println(Url.decode(s));
	}

	@Test
	public void testMount() {

		Disk e = new Disk();
		e.path = "/home/disk1/";

		e.mount = "/temp";
		String filename = "/temp/a/b/demo.txt";
		filename = e.filename(filename);
		System.out.println(filename);

		e = new Disk();
		e.path = "/home/disk1/";
		e.mount = "/dput/[a-zA-Z0-9]+/temp/";
		filename = "/dput/admin/temp/a/b/demo.txt";
		filename = e.filename(filename);
		System.out.println(filename);

		filename = "/dput/admin/aaaa/temp/a/b/demo.txt";
		filename = e.filename(filename);
		System.out.println(filename);

		filename = "/dput/admin/temp/";
		String s = e.filename(filename);
		System.out.println(s + ", mount=" + e.isMount(filename));

	}

}
