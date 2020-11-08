package org.giiwa.misc;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;

import org.giiwa.dao.X;
import org.junit.Test;

public class CaptchaTest {

	@Test
	public void test() {

		try {
			File dir = new File("/tmp/verifies");
			int w = 200, h = 80;
			for (int i = 0; i < 50; i++) {
				File file = new File(dir, i + ".jpg");
				Captcha.create("1", System.currentTimeMillis() + 6 * X.AMINUTE, w, h, new FileOutputStream(file), 4);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());

		}
	}

}
