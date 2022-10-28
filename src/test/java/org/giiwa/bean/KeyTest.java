package org.giiwa.bean;

import org.giiwa.dao.UID;
import org.giiwa.misc.Base32;
import org.giiwa.misc.RSA;
import org.junit.Test;

public class KeyTest {

	@Test
	public void test() {

		String pri = "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAKisPnwbMokIkB4l+T15bny5L+B6XOpBDHwmV1yv1qg7sY600AtKLwC4/Vtd1hv+s244SG8/S66qCcNrVkFJb/VOttzBHXwm5E50BofNmjDrDbikkoeJt0Er1vcWGNwSSvX1kQ7cv8LQUehlCsq7w6/3v3W3kFO1i25pPy2ihOwhAgMBAAECgYAE1TCSIj5brkbg0kI2qHLJuI2eXysn3BbFPNz+bxxVpCY1eklGtYisuuApfMIMecr+0raCl3vt5HnUo0/snXPLdaQSevHwV5gnD0oFD/sDmZd/5XRUg9l6Z279EOF/8d+RKhZdejqhMFANV2pUc5neULOUlWNTAafXofjfwyhD1QJBAKr34T86xeCuDUCmA7pfI9W2/m80bSp7mX5jhn/rHWNhfJKTcwxF2pMt21J57l6e57nyFq7iIKTcLz2KQkFFqhUCQQD8kBnxAiG8Cezi7fKs76XUmdbPh57ly4Uyfi0V2I9tTMmzOATHOZpmfDQi78RYMSp1yAPu9cn0cziEBAdtGrjdAkBqmmU4p8z6a4yX4uwwKWKOv6uma2omMytiQ2x6FoAcl4y1WHtEC8peOxmmM6EOHsceinTaVuVD5ocOOEdXq7iBAkAIPj/KgPJO69gCdBe2kz/LV5YOQfPqtiDLzBh2nRHZVGdE5TAqvHOQor8k4MR8yGYFYBjYxMbpppUSofsIEZY1AkBzoCszJWJCsQoSspCwbZIVenu+9yR2D1KGPpPkYGkbM6f516v7LRt/oifqknFlSNp2306mp/TX7MCh0AabfRsB";
		String pub = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCorD58GzKJCJAeJfk9eW58uS/gelzqQQx8Jldcr9aoO7GOtNALSi8AuP1bXdYb/rNuOEhvP0uuqgnDa1ZBSW/1TrbcwR18JuROdAaHzZow6w24pJKHibdBK9b3FhjcEkr19ZEO3L/C0FHoZQrKu8Ov9791t5BTtYtuaT8tooTsIQIDAQAB";

		org.giiwa.misc.RSA.Key k1 = RSA.generate(1024);
		System.out.println("pri=" + k1.pri_key);
		System.out.println("pub=" + k1.pub_key);

		int len = 24;
		String s = UID.random(len);
		System.out.println(s);

		byte[] bb = RSA.encode(s.getBytes(), pub);

		s = Base32.encode(bb);
		System.out.println(s);

		s = new String(RSA.decode(bb, pri));
		System.out.println(s);

	}

}
