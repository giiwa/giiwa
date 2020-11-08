package org.giiwa.misc;

import static org.junit.Assert.*;

import java.util.Base64;

import org.giiwa.misc.RSA.Key;
import org.junit.Test;

public class RSATest {

	@Test
	public void test() {
		Key k = RSA.generate(512);

		System.out.println(k);

		String s = "hello";

		String pub_key = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBANckZ1iK/1sOb7N1n2xuwiIoHZtJ3mgaV3s0PCcJKhdV5MsjQ/yzQ5N4lnQd9RyLjVfDH6M6KNDSmPc+rmRFRH0CAwEAAQ==";
		String pri_key = "MIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEA1yRnWIr/Ww5vs3WfbG7CIigdm0neaBpXezQ8JwkqF1XkyyND/LNDk3iWdB31HIuNV8Mfozoo0NKY9z6uZEVEfQIDAQABAkArmSv8TIa9DCrkwkRhc/yRcXG2g3y3ugbaZ9Z8zqWh/p2bU0ih2EdhqCl1M9QzOlmwdgL6dOZtupr93cvPwb2dAiEA/8plzQ4y0xGqbRjDai4KfEwgNQ57T0f74giFqErHzRsCIQDXUXzpRbnMqksB/SrT45BzPUH4eEIoYQ2ZBuEVuLJGRwIhANufHlU30a+kRV4ymuZ57YrXmfe0HW/u8HgctRXQT0jtAiBqPCNkOOm+KDtP5OhPmRS5Nv0oqbUClTgPS4ycmf8jmwIgfKUvHfL+DBr0mhee0kXE//RVOHUORv9jgyFL7TK1W6s=";

		byte[] ss = RSA.encode(s.getBytes(), pub_key);

		System.out.println(Base64.getEncoder().encodeToString(ss));

		byte[] ss1 = RSA.decode(ss, pri_key);

		System.out.println(new String(ss1));
	}

}
