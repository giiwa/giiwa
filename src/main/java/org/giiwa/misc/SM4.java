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

import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * SM4 => DES/AES
 * 
 * @author yjiang
 */
@Deprecated
public class SM4 {

	static Log log = LogFactory.getLog(SM4.class);

	private static final String ALGORITHM_NAME = "SM4";
	private static final String ALGORITHM_ECB_PKCS5PADDING = "SM4/ECB/PKCS5Padding";

	/**
	 * SM4算法目前只支持128位（即密钥16字节）
	 */
	private static final int DEFAULT_KEY_SIZE = 128;

	static {
		if (null == Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	private static byte[] key(String seed) {
		try {
			KeyGenerator kg = KeyGenerator.getInstance(ALGORITHM_NAME, BouncyCastleProvider.PROVIDER_NAME);
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			random.setSeed(seed.getBytes());
			kg.init(DEFAULT_KEY_SIZE, random);
			return kg.generateKey().getEncoded();
		} catch (Exception err) {
			log.error(err.getMessage(), err);
		}
		return null;
	}

	/**
	 * 
	 * @param in
	 * @param seed
	 * @return
	 */
	public static byte[] encode(byte[] in, String seed) {

		try {
			SecretKeySpec sm4Key = new SecretKeySpec(key(seed), ALGORITHM_NAME);
			Cipher cipher = Cipher.getInstance(ALGORITHM_ECB_PKCS5PADDING, BouncyCastleProvider.PROVIDER_NAME);
			cipher.init(Cipher.ENCRYPT_MODE, sm4Key);
			return cipher.doFinal(in);
		} catch (Exception err) {
			log.error(err.getMessage(), err);
		}

		return null;
	}

	/**
	 * 
	 * @param in
	 * @param seed
	 * @return
	 */
	public static String encode_string(byte[] in, String seed) {
		byte[] out = encode(in, seed);
		String cipher = Base64.getEncoder().encodeToString(out);
		return cipher;
	}

	/**
	 * 
	 * @param content
	 * @param key
	 * @return
	 */
	public static String encode(String content, String seed) {
		String cipher = encode_string(content.getBytes(), seed);
		return cipher;
	}

	/**
	 * 
	 * @param in
	 * @param seed
	 * @return
	 */
	public static byte[] decode(byte[] in, String seed) {

		try {
			SecretKeySpec sm4Key = new SecretKeySpec(key(seed), ALGORITHM_NAME);
			Cipher cipher = Cipher.getInstance(ALGORITHM_ECB_PKCS5PADDING, BouncyCastleProvider.PROVIDER_NAME);
			cipher.init(Cipher.DECRYPT_MODE, sm4Key);
			return cipher.doFinal(in);
		} catch (Exception err) {
			log.error(err.getMessage(), err);
		}

		return null;

	}

	/**
	 * 
	 * @param in
	 * @param seed
	 * @return
	 */
	public static String decode_string(byte[] in, String seed) {
		byte[] out = decode(in, seed);
		String plain = new String(out);
		return plain;
	}

	/**
	 * 
	 * @param content
	 * @param seed
	 * @return
	 */
	public static String decode(String content, String seed) {
		String plain = decode_string(Base64.getDecoder().decode(content), seed);
		return plain;
	}

}
