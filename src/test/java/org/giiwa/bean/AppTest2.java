package org.giiwa.bean;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.giiwa.json.JSON;
import org.giiwa.net.client.Http;

public class AppTest2 {

	public static void main(String[] args) {

		// 应用接入号 和 密钥
		String appid = "test";
		String secret = "a0RkDLbe7L3nRYRybOYLzeVgN2QGHs1G";

		// 构造请求参数
		JSON j1 = JSON.create();
		// 请求数据表
		j1.append("table", "org_person");
		// 过滤条件
		j1.append("sql", "");
		// 请求起始位置，按照数据创建顺序
		j1.append("s", 0);
		// 一次请求多少条数据
		j1.append("n", 10);

		// 用密钥加密请求参数
		String d = _encode(j1.toString(), secret);

		// 请求数据
		Http h = Http.create();
		Http.Response r = h.post("http://iportal.giisoo.com/f/data/" + appid, JSON.create().append("d", d));

		// 输出请求结果
		System.out.println(r.body);

		// 直接获取返回结果的json对象
		j1 = r.json();

		// 获取返回结果的list数据
		String s = j1.getString("list");
		// 使用密钥对list数据进行解密
		List<JSON> l1 = JSON.fromObjects(_decode(s, secret));

		// 数据解密后的数据
		System.out.println(JSON.toString(l1));

	}

	public static String _decode(String data, String secret) {
		try {
			// Base64解码
			byte[] bb = Base64.getDecoder().decode(data);
			// AES解密
			return new String(aes_decode(bb, secret));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// AES解密
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

	private static String _encode(String data, String secret) {
		try {
			// AES加密
			byte[] bb = aes_encode(data.getBytes(), secret);
			// Base64编码
			return Base64.getEncoder().encodeToString(bb);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// AES加密
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
