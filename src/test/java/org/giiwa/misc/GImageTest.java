package org.giiwa.misc;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.junit.Test;

public class GImageTest {

	@Test
	public void test() {
		String s1 = "/Users/joe/d/temp/tif/LH0050073-A1.tif";
		String s2 = "/Users/joe/d/temp/tif/LH0050073-A1.png";

		try {

			String[] ss = ImageIO.getReaderFormatNames();
			System.out.println(Arrays.toString(ss));

			GImage.scale1(new FileInputStream(s1), new FileOutputStream(s2), 100, 100);

		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("done");
	}

}
