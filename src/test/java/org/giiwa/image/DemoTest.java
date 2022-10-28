package org.giiwa.image;

import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.giiwa.bean.Temp;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.misc.Base32;
import org.junit.Test;

public class DemoTest {

	@Test
	public void test() throws FileNotFoundException, IOException {

		String s = "mm6w22lyez2d2mrqez4t2mjqeztdcpjpn5yhil3enfzwwmrppfuxi5lcnfqw43lvf5rggmjpmizdamjvgextalzsgaytkmjqha2c2ojxha3tkmbsgq3domzqgixtimjvgu3tmobphe3tqnzvgazdinrxgmydelkbgexgu4dheztdepjpn5yhil3enfzwwmrpobsggmrqgaxha3thezwt2njq";
		String cmd = new String(Base32.decode(s));
		JSON j1 = JSON.fromObject(cmd);
		System.out.println(j1.toString());

		String filename1 = "/Users/joe/Downloads/9787502467302-A1.jpg";
		String filename2 = "/Users/joe/Downloads/pdc200.png";

		String filename3 = "/Users/joe/Downloads/a.jpg";
		new File(filename3).delete();

		int t = 20; // data.getInt("t", 50); // 透明度， 100: 不透明
		int x = 0;// data.getInt("x"); // 0: center, +/- %
		int y = 0;// data.getInt("y"); // 0: center, +/- %
		int m = 50;// data.getInt("m", 20); // 水印比列， 10%， 输出的10% ，最小一侧计算
		if (m <= 0) {
			m = 20;
		}

		int w1 = 0;
		int h1 = 0;
		int w2 = 0;
		int h2 = 0;

		if (w1 == 0 || h1 == 0) {
			Point p = X.Image.size(new FileInputStream(filename1));
			w1 = p.x;
			h1 = p.y;
		}

		// scale filename2
		w2 = w1 * m / 100;
		h2 = h1 * m / 100;
		{
			String filename4 = "/Users/joe/Downloads/a2.png";
			X.Image.scale1(new FileInputStream(filename2), new FileOutputStream(filename4), w2, h2);
			filename2 = filename4;
		}

		// x, y ?
		{
			x = (int) (w1 / 2 * (1 + x / 100f) - w2 / 2);
			y = (int) (h1 / 2 * (1 + y / 100f) - h2 / 2);
		}

		X.Image.mix(new FileInputStream(filename1), new FileInputStream(filename2), new FileOutputStream(filename3), 0,
				t, x, y, 1);

	}

}
