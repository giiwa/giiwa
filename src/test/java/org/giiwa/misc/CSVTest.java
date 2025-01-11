package org.giiwa.misc;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.giiwa.dao.X;
import org.junit.Test;

public class CSVTest {

	@Test
	public void test() {

		String filename = "/Users/joe/Documents/ads_road_section_speed_rank.csv";
//		String filename = "/Users/joe/Documents/数据_20241212/f2_damage.csv";
		try {
			InputStream in = new FileInputStream(filename);
			test(filename, in);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void test2() {
		String filename = "/Users/joe/Documents/数据_20241213.zip";

		try {
			ZipInputStream in = new ZipInputStream(new FileInputStream(filename));

			ZipEntry e = in.getNextEntry();
			while (e != null) {
				try {
					if (!e.isDirectory()) {
						String name0 = e.getName();
						String name = name0.toLowerCase();
						int i = name.lastIndexOf("/");
						if (i > 0) {
							name = name.substring(i + 1);
						}
						i = name.lastIndexOf("\\");
						if (i > 0) {
							name = name.substring(i + 1);
						}
						if (name.startsWith(".")) {

						} else {
							if (name.endsWith(".csv")) {
								test(name, in);
							}
						}
					}
				} catch (Exception err) {
					err.printStackTrace();
				}
				e = in.getNextEntry();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void test(String name, InputStream in) throws IOException {

		System.out.println("===\r\n" + name);
		CSV e = null;
		BufferedInputStream in1 = new BufferedInputStream(in);

		in1.mark(10);
		byte[] bb = new byte[4];
		in1.read(bb);
		char c = new String(bb).charAt(0);
		System.out.println((int) c);
		in1.reset();
		in = in1;
		if (c == 65279) {
			e = CSV.create(new BufferedReader(new InputStreamReader(in, "UTF8")));
		} else {
			e = CSV.create(new BufferedReader(new InputStreamReader(in, "GBK")));
		}
		Object[] ss = e.next();
		int n = 0;
		while (ss != null && n < 10) {
			System.out.println("ss=>" + X.asList(ss, s1 -> (s1 instanceof String) ? "'" + s1 + "'" : s1));
			ss = e.next();
//				if (X.isSame(ss[0], 118485)) {
//					System.out.println("ok!");
//				}
			n++;
		}
		System.out.println("n=" + n);

	}

}
