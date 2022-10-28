package org.giiwa.misc;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import org.giiwa.dao.X;
import org.junit.Test;

public class CSVTest {

	@Test
	public void test() {

		String filename = "/Users/joe/Downloads/测试数据-元素据-正式出版物(29-utf8)1.csv";
		CSV e = null;
		try {
			e = CSV.create(new BufferedReader(new InputStreamReader(new FileInputStream(filename))));
			Object[] ss = e.next();
			int n = 0;
			while (ss != null && n < 10) {
				System.out.println("ss=>" + X.asList(ss, s1 -> (s1 instanceof String) ? "'" + s1 + "'" : s1));
				ss = e.next();
				if (X.isSame(ss[0], 118485)) {
					System.out.println("ok!");
				}
				n++;
			}
			System.out.println("n=" + n);
		} catch (Exception e1) {
			e1.printStackTrace();
		} finally {
			X.close(e);
		}

	}

}
