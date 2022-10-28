package org.giiwa.misc;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.giiwa.dao.X;
import org.giiwa.misc.RSA.Key;
import org.junit.Test;

public class DigestTest {

	@Test
	public void test() {

		try {
			String s = "123";
			String code = "1231231";
			byte[] bb = Digest.encode(s.getBytes(), Digest.code(code, 32));
			s = new String(Digest.decode(bb, Digest.code(code, 32)));

			System.out.println(s);

//			Cipher cipher = Cipher.getInstance("AES");///CBC/PKCS5Padding");
//
//			String code = Digest.code(Global.getString("encode.code", null), 32);
//			KeyGenerator kgen = KeyGenerator.getInstance("AES");
//			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
//			random.setSeed(code.getBytes());
//			kgen.init(128, random);
//			SecretKey secretKey = kgen.generateKey();
//
//			byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
//			IvParameterSpec ivspec = new IvParameterSpec(iv);
//
//			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
//			KeySpec spec = new PBEKeySpec(secretKey.toCharArray(), salt.getBytes(), 65536, 256);
//
//			SecretKey tmp = factory.generateSecret(spec);
//			SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");
//
//			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
//			cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);
//
//			SecretKeySpec keySpec = new SecretKeySpec(code.getBytes(), "AES");
//
//			cipher.init(Cipher.DECRYPT_MODE, keySpec);
//
//			KeyGenerator kgen = KeyGenerator.getInstance("AES");
//			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
//			random.setSeed(code.getBytes());
//			kgen.init(128, random);
//			SecretKey secretKey = kgen.generateKey();
//			byte[] enCodeFormat = secretKey.getEncoded();
//			SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");
//			
//			Cipher cipher = Cipher.getInstance("AES");//CBC/PKCS5Padding");
//			cipher.init(Cipher.DECRYPT_MODE, key);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());

		}
	}

	@Test
	public void testRSA() {

		String key = "0.51446426574443160.167361834489";
		String pubkey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuhDHNtylFKG+TnHD8+k+q+8cmcWYCmEd9ct5O4MgEOdRl7Y5Ro6e92IM+QuvqbvJXHHqtP3BDfYAFzb4DMeajAhz8upooQ8urFETZLR9awjINuCjv8pCSHK4djCGlbpFFBfMQdqfnr9SfMq2lYJySeI+qcPAxukFExZF7MLdd5VumF0Exv1OMVmLlhrqIQvGcilYK9XchNfLbraMG84xBw4jdLrgoCntGPqcsybluyWLWuFNGQGzRVcszQ7IxWIci9ePfAGt45z/KFn0eLYyrNJYO05EBzGRG4gKRSv3yR9E6bwVyXkp2E+mm6ZL2MH7L4VLwJQC9btEtRvY8szkrwIDAQAB";
		String prikey = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC6EMc23KUUob5OccPz6T6r7xyZxZgKYR31y3k7gyAQ51GXtjlGjp73Ygz5C6+pu8lcceq0/cEN9gAXNvgMx5qMCHPy6mihDy6sURNktH1rCMg24KO/ykJIcrh2MIaVukUUF8xB2p+ev1J8yraVgnJJ4j6pw8DG6QUTFkXswt13lW6YXQTG/U4xWYuWGuohC8ZyKVgr1dyE18tutowbzjEHDiN0uuCgKe0Y+pyzJuW7JYta4U0ZAbNFVyzNDsjFYhyL1498Aa3jnP8oWfR4tjKs0lg7TkQHMZEbiApFK/fJH0TpvBXJeSnYT6abpkvYwfsvhUvAlAL1u0S1G9jyzOSvAgMBAAECggEACEYouw19dc5qKmOFZY+mDs/s7gBLGOMQCaxzsVbNEpNcHdVSSgtGPGqqza8HBRLptb0HGpH83MRnZUuMz5ysswoehdswreuBQCkW0K/0snwgfc2S2pDW+REeQiXkRjzonNJ2GH5CrN+F09t72mguRuTTCTTq71gmC/Q98HULPimzsa2FT4AKdGedusvS93caiki+bvEEFK+lljuK1aNdm8uR1o58aBPZGft0KDANOeauyz/YFsyxh376GwXrtpCivqdvUGK1o8GjZ3WAMmlkANkM1g4T7Oj8iyoMN9iaNGLpLnVSIf3R4DaHtxCRG5Z5MaMEc+GEQr3eqCvagbtBgQKBgQD9ZWmRuUGINHAX113fRyMqnfiJldFhTEucYX9ufP7nJkcdbLwN1w0BCLc1Krm5yykZtUSDDKv118kJ6eXGq0Oe3KbvyGJyFJsNd9T0xRI04GsrkjIOk78I3NJ4Og0IDgtujX7hLGoij9iMTOHXAyzvKu1KnWhuSQ0LQhhWEcci7wKBgQC7+j6zlg/uAS4/6VoBzrAgQpUtIZ331fvdfI7VmoRlgTgjJ4CKHst6JdLWcy4BoZSrzdiyZDmaClZkTNTZiPlWA1OWb3vrRoGm3n+VN0sAJjOrHTcXDw1T90VzbOAJjqYSu9NGav47qcIYu5uYR4y4ngFgmdjZyIPSBtXzHltaQQKBgQCkXPmjSnVVch4beNktESmgZSNnq0RLRn/tCdjsxCtbqBqM3ZEFsS6AMzUTSYl7GGqqgEfWcYvBRZzjz1H6EfEkxJ+L/YDN2svQaqA5vPLYVZFui7/ocLGDgCkNTypQxTtpFGLukC6wCHpAV0ZOf4LQCBovQfcRQlUxrke23IU9BQKBgD5vuDPePBxJuBryXzsKc+XDN9ltuRKAuM+wd6DrWd59NTA4BrHZ2KwRtB4W08km6kLGdJuMPrWziU53VpuKq4auRC1LilVzbc0HSkk9vGOoTLEhWxMFpN+m2iKknyWNhAvk5yAUma0njZi5d8z0twD7Omnjr+tmdqJkMtw10RIBAoGBAITwsKz7mHW22U2jjr+3n5TFDPKRLGTa9SIT3T1hpimLCddXADEcHxHLbyN9wsINQ2C71uFShbfXEKyZTqYPeIKobDSokKoyplX9grl9EYST6QB32ntvKPvjh9ZeTN3CwhPLv0xS8PUMKohhDqOvNKfiguFmDSf0njTmmmI7hcZq";

		int[] code = new int[] { 24, 157, 76, 22, 108, 81, 173, 180, 132, 181, 25, 213, 210, 96, 28, 114, 211, 17, 62,
				185, 78, 47, 230, 168, 159, 201, 187, 40, 147, 149, 221, 123, 177, 91, 176, 88, 244, 104, 155, 155, 209,
				147, 43, 0, 62, 37, 91, 173, 180, 47, 176, 151, 213, 119, 201, 253, 201, 112, 41, 249, 96, 188, 140, 3,
				88, 171, 19, 243, 7, 176, 102, 167, 225, 150, 168, 124, 225, 233, 24, 113, 126, 224, 147, 192, 25, 154,
				206, 68, 58, 124, 8, 163, 1, 248, 28, 236, 47, 216, 203, 125, 46, 113, 216, 79, 154, 235, 122, 166, 152,
				135, 250, 224, 237, 143, 48, 227, 58, 229, 220, 155, 23, 121, 84, 175, 15, 31, 237, 158, 115, 26, 241,
				2, 230, 143, 147, 126, 114, 103, 61, 177, 69, 217, 181, 83, 83, 191, 12, 97, 168, 136, 217, 151, 86, 73,
				240, 123, 158, 129, 180, 38, 91, 201, 178, 166, 71, 216, 125, 141, 166, 195, 94, 230, 116, 143, 204,
				219, 124, 181, 184, 254, 156, 152, 207, 127, 40, 162, 181, 76, 135, 172, 86, 131, 47, 99, 224, 199, 22,
				244, 45, 96, 245, 136, 153, 130, 196, 117, 180, 172, 140, 69, 193, 250, 139, 4, 144, 182, 127, 30, 107,
				23, 117, 89, 60, 76, 167, 101, 194, 43, 222, 25, 76, 49, 41, 96, 99, 42, 65, 26, 80, 142, 11, 229, 46,
				220, 14, 112, 196, 120, 82, 218, 145, 192, 75, 82, 249, 110 };

		try {

			AES.init();

			String s1 = RSA.pemPubkey(pubkey);
			System.out.println(pubkey);
			System.out.println(s1);

			byte[] bb = RSA.encode(key.getBytes(), RSA.getPublicKey(pubkey));
			System.out.println(X.asList(bb, e -> {
				return X.toInt(e) & 0xFF;
			}));

			bb = RSA.decode(bb, RSA.getPrivateKey(prikey));
			System.out.println(new String(bb));

			bb = new byte[code.length];
			for (int i = 0; i < bb.length; i++) {
				bb[i] = (byte) code[i];
			}
			bb = RSA.decode(bb, RSA.getPrivateKey(prikey));
			System.out.println(new String(bb));

			String filename = "/Users/joe/Downloads/CentOS-Base.repo3.ENC";
			InputStream in = new FileInputStream(filename);
			bb = new byte[16];
			int len = in.read(bb);
			System.out.println(len);
			System.out.println(new String(bb));

			len = (int) ((in.read() & 0xFF) << 8) | in.read();
			byte[] kk = new byte[len];
			in.read(kk);

			System.out.println(len);
			kk = RSA.decode(kk, RSA.getPrivateKey(prikey));

			String s2 = "";
			for (int i = 0; i < 1024; i++) {
				s2 += "1";
			}

			bb = AES.encode(s2.getBytes(), kk);
			System.out.println(X.asList(bb, e -> {
				return X.toInt(e) & 0xFF;
			}));
			System.out.println(bb.length);

			String s3 = new String(AES.decode(bb, kk));
			System.out.println(s3);

			len = (int) ((in.read() & 0xFF) << 8) | in.read();
			byte[] dd = new byte[len];
			int len1 = in.read(dd);
			System.out.println(len + "/" + len1);

			bb = AES.decode(dd, kk);
			System.out.println(new String(bb));

			X.close(in);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testPem() {

		Key e = RSA.generate(2048);
		String s1 = RSA.pemPubkey(e.pub_key);
		System.out.println(e.pub_key);
		System.out.println(s1);

		s1 = RSA.pemPrikey(e.pri_key);
		System.out.println(e.pri_key);
		System.out.println(s1);

	}

}
