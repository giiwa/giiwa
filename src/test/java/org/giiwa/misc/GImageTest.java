package org.giiwa.misc;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.junit.Test;

public class GImageTest {

	@SuppressWarnings("unused")
	@Test
	public void test() {

		String s1 = "/Users/joe/d/temp/tif/LH0050073-A1.tif";
		String s2 = "/Users/joe/d/temp/tif/LH0050073-A1.png";

		try {

			String[] ss = ImageIO.getReaderFormatNames();
			System.out.println(Arrays.toString(ss));

//			GImage.scale1(new FileInputStream(s1), new FileOutputStream(s2), 100, 100);

			System.out.println("done");

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());

		}

	}

	@Test
	public void test2() {

		String s1 = "/Users/joe/Downloads/微信-军鹰资讯_2021_01_27_001/caz3uhqbj4vpy/640";
		String s2 = "/Users/joe/d/a.png";

		try {

			GImage.scale1(new FileInputStream(s1), new FileOutputStream(s2), 60, -1);

			System.out.println("done");

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());

		}

	}

	@Test
	public void testJFIF() {

		String s1 = "/Users/joe/Downloads/9787535176721-11027015-A1";
		String s2 = "/Users/joe/d/a.png";

		try {

			GImage.scale1(new FileInputStream(s1), new FileOutputStream(s2), 60, -1);

			System.out.println("done");

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());

		}

	}

}
