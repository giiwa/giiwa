package org.giiwa.bean;

import static org.junit.Assert.fail;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.net.client.Http;
import org.junit.Test;

public class AppTest {

	@Test
	public void test() {

		App a = new App();
		a.appid = "1";
		a.secret = "123123";

		JSON j1 = JSON.create();
		j1.put("name", "1");
		j1.put("key", "122");

		try {
			String data = App.encode(j1.toPrettyString(), a.secret);
			System.out.println("data=" + data);
			String jo = App.decode(data, a.secret);
			System.out.println("jo=" + jo);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());

		}

		long s = 1652664665000L;
		System.out.println(new Date(s));
		System.out.println(X.AWEEK);

	}

	@Test
	public void testSSO() {

		String s1 = "root//1623217103101";
		String secret = "i6psmzKr0W32UfPb430KwfCwwXiR8TgT";
		String s2 = "BEQhOwee2if4EW1tdE77j+PscZArynLXY3NtcDrS7pI=";
		System.out.println(App.encode(s1, secret));
		System.out.println(App.decode(s2, secret));

	}

	@Test
	public void testData() {

		String appid = "test";
		String secret = "a0RkDLbe7L3nRYRybOYLzeVgN2QGHs1G";

		JSON j1 = JSON.create();
		j1.append("s", 0);
		j1.append("n", 10);
		j1.append("table", "gi_user");

		String d = _encode(j1.toString(), secret);

		System.out.println(d);

		Http h = Http.owner;
		Http.Response r = h.post("http://iportal.giisoo.com/f/data/" + appid, JSON.create().append("d", d));

		System.out.println("....");

		System.out.println(r.body);

		j1 = r.json();

		String s = j1.getString("list");
		List<JSON> l1 = JSON.fromObjects(App.decode(s, secret));

		System.out.println(JSON.toString(l1));

	}

	public static String _decode(String data, String secret) {
		try {
			byte[] bb = Base64.getDecoder().decode(data);
			return new String(aes_decode(bb, secret));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static byte[] aes_decode(byte[] content, String seed) throws Exception {

		KeyGenerator kgen = KeyGenerator.getInstance("AES");
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		random.setSeed(seed.getBytes());
		kgen.init(128, random);
		SecretKey secretKey = kgen.generateKey();
		byte[] enCodeFormat = secretKey.getEncoded();
		SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");

		Cipher cipher = Cipher.getInstance("AES");// CBC/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, key);
		byte[] result = cipher.doFinal(content);
		return result;

	}

	private String _encode(String data, String secret) {
		try {
			byte[] bb = aes_encode(data.getBytes(), secret);
			return Base64.getEncoder().encodeToString(bb);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * AES encode
	 * 
	 * @param content
	 * @param seed    for code
	 * @return
	 * @throws Exception
	 */
	public static byte[] aes_encode(byte[] content, String seed) throws Exception {

		KeyGenerator kgen = KeyGenerator.getInstance("AES");

		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");

		random.setSeed(seed.getBytes());
		kgen.init(128, random);
		SecretKey secretKey = kgen.generateKey();
		byte[] enCodeFormat = secretKey.getEncoded();
		SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");
		Cipher cipher = Cipher.getInstance("AES");// CBC/PKCS5Padding");

		cipher.init(Cipher.ENCRYPT_MODE, key);
		byte[] result = cipher.doFinal(content);
		return result;

	}

}
