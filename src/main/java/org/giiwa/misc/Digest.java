/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.misc;

import java.security.*;

import javax.crypto.*;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.giiwa.dao.UID;
import org.giiwa.dao.X;

/**
 * The Class Digest.
 */
public class Digest {

	/** The md. */
	private static MessageDigest md;

	static {
		try {
			// SHA, MD5
			md = MessageDigest.getInstance("MD5");
		} catch (Exception e) {
		}
	}

	/**
	 * Digest.
	 * 
	 * @param s the s
	 * @return the byte[]
	 */
	public static byte[] digest(byte[] s) {
		return md.digest(s);
	}

	/**
	 * Md5.
	 *
	 * @param str the str
	 * @return the string
	 */
	public static String md5(String str) {
		MessageDigest messageDigest = null;
		try {
			messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.reset();
			messageDigest.update(str.getBytes("UTF-8"));
		} catch (Exception e) {
			return null;
		}

		byte[] byteArray = messageDigest.digest();

		StringBuffer md5StrBuff = new StringBuffer();

		for (int i = 0; i < byteArray.length; i++) {
			if (Integer.toHexString(0xFF & byteArray[i]).length() == 1)
				md5StrBuff.append("0").append(Integer.toHexString(0xFF & byteArray[i]));
			else
				md5StrBuff.append(Integer.toHexString(0xFF & byteArray[i]));
		}
		return md5StrBuff.toString();
	}

	/**
	 * Decrypt.
	 *
	 * @deprecated
	 * @param str  the str
	 * @param code the code
	 * @return the byte[]
	 * @throws Exception the exception
	 */
	public static byte[] des_decrypt(byte[] str, String code) throws Exception {
		DESKeySpec desKeySpec = new DESKeySpec(code.getBytes());
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
		Key k = keyFactory.generateSecret(desKeySpec);

		Cipher cipher = Cipher.getInstance("DES");
		cipher.init(Cipher.DECRYPT_MODE, k);

		return cipher.doFinal(str);

	}

	/**
	 * Encrypt.
	 *
	 * @deprecated
	 * @param str  the str
	 * @param code the code
	 * @return the string
	 * @throws Exception the exception
	 */
	public static byte[] des_encrypt(byte[] str, String code) throws Exception {
		DESKeySpec desKeySpec = new DESKeySpec(code.getBytes());
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
		Key k = keyFactory.generateSecret(desKeySpec);

		Cipher cipher = Cipher.getInstance("DES");
		cipher.init(Cipher.ENCRYPT_MODE, k);

		return cipher.doFinal(str);
	}

	/**
	 * AES decode
	 * 
	 * @param content
	 * @param seed    for 128 code
	 * @return
	 * @throws Exception
	 */
	public static byte[] decode(byte[] content, String seed) throws Exception {

		KeyGenerator kgen = KeyGenerator.getInstance("AES");
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		random.setSeed(seed.getBytes());
		kgen.init(128, random);
		SecretKey secretKey = kgen.generateKey();
		byte[] enCodeFormat = secretKey.getEncoded();
		SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");

		try {
			// 兼容老的解密方式

			Cipher cipher = Cipher.getInstance("AES");// CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, key);
			byte[] result = cipher.doFinal(content);
			return result;
		} catch (Exception e) {
			// 新的解密方式

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, key);
			byte[] result = cipher.doFinal(content);
			return result;
		}

	}

	/**
	 * AES encode
	 * 
	 * @param content
	 * @param seed    for code
	 * @return
	 * @throws Exception
	 */
	public static byte[] encode(byte[] content, String seed) throws Exception {

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

	/**
	 * random code
	 * 
	 * @param passwd
	 * @param len
	 * @return
	 * @throws Exception
	 */
	public static String code(String passwd, int len) throws Exception {
		int[] aa = UID.random(passwd, len);
		return X.join(aa, "");
	}

}
