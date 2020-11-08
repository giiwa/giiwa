package org.giiwa.misc;

import static org.junit.Assert.*;

import org.junit.Test;

public class DigestTest {

	@Test
	public void test() {
		try {
			String s = "123";
			String code = "12312312";
			byte[] bb = Digest.aes_encrypt(s.getBytes(), code);
			s = new String(Digest.aes_decrypt(bb, code));

			System.out.println(s);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
