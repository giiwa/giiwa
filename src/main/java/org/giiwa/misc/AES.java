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
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.giiwa.conf.Global;

/**
 * The Class Digest.
 */
public class AES {

	/**
	 * AES decode
	 * 
	 * @param content
	 * @param code    AES code
	 * @return
	 * @throws Exception
	 */
	public synchronized static byte[] decode(byte[] content, byte[] code) throws Exception {

		byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		IvParameterSpec ivspec = new IvParameterSpec(iv);

		SecretKeySpec key = new SecretKeySpec(code, "AES");

		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
		cipher.init(Cipher.DECRYPT_MODE, key, ivspec);

		byte[] result = cipher.doFinal(content);
		return result;

	}

	public static void init() {
		Security.addProvider(new BouncyCastleProvider());
	}

	/**
	 * AES encode
	 * 
	 * @param content
	 * @param code    AES code
	 * @return
	 * @throws Exception
	 */
	public synchronized static byte[] encode(byte[] content, byte[] code) throws Exception {

		if (content == null || code == null) {
			return null;
		}

		byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		IvParameterSpec ivspec = new IvParameterSpec(iv);

		SecretKeySpec key = new SecretKeySpec(code, "AES");
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");

		cipher.init(Cipher.ENCRYPT_MODE, key, ivspec);

		byte[] result = cipher.doFinal(content);
		return result;

	}

	/**
	 * generate 256 AES key
	 * 
	 * @return
	 * @throws Exception
	 */
	public static byte[] aeskey() throws Exception {

		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		random.setSeed(Long.toString(Global.now()).getBytes());

		KeyGenerator kgen = KeyGenerator.getInstance("AES");
		kgen.init(128, random);

		SecretKey key = kgen.generateKey();
		return key.getEncoded();

	}

	public static byte[] aeskey(String seed) throws Exception {

		if (seed == null) {
			return null;
		}

		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		random.setSeed(seed.getBytes());

		KeyGenerator kgen = KeyGenerator.getInstance("AES");
		kgen.init(128, random);

		SecretKey key = kgen.generateKey();
		return key.getEncoded();

	}

}
