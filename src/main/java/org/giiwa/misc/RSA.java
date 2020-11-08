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
import java.security.interfaces.*;
import java.security.spec.*;
import java.util.Base64;

import javax.crypto.Cipher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * RSA utility, RSA is using for encode some short key (such as password or DES
 * key), it's slow, and output longer data, NOT suitable for encode file.
 * 
 * @author yjiang
 */
public class RSA {
	static Log log = LogFactory.getLog(RSA.class);

	/**
	 * decode data with private key.
	 * 
	 * @param data    the data
	 * @param pri_key the pri_key
	 * @return the byte[]
	 */
	public static byte[] decode(byte[] data, String pri_key) {
		try {
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

			java.security.Key k = getPrivateKey(pri_key);

			cipher.init(Cipher.DECRYPT_MODE, k);

			byte[] deBytes = cipher.doFinal(data);

			return deBytes;
		} catch (Exception e) {
			// ignore error
//			log.error(pri_key, e);

		}
		return null;
	}

	/**
	 * encode the data with the public key.
	 * 
	 * @param data    the data
	 * @param pub_key the pub_key
	 * @return the byte[]
	 */
	public static byte[] encode(byte[] data, String pub_key) {
		try {
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

			java.security.Key k = getPublicKey(pub_key);

			cipher.init(Cipher.ENCRYPT_MODE, k);
			byte[] enBytes = cipher.doFinal(data);

			return enBytes;
		} catch (Exception e) {
			log.error("pubkey=" + pub_key + ", data.length=" + data.length, e);
		}

		return null;
	}

	/**
	 * generate key pair which include public and private key.
	 * 
	 * @param length the length
	 * @return the key
	 */
	public static Key generate(int length) {
		try {
			KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");

			keyPairGen.initialize(length);

			KeyPair keyPair = keyPairGen.generateKeyPair();

			PublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

			PrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

			Key k = new Key();
			k.pub_key = getKeyString(publicKey);
			k.pri_key = getKeyString(privateKey);

			return k;
		} catch (Exception e) {

		}

		return null;
	}

	/**
	 * Gets the key string.
	 * 
	 * @param key the key
	 * @return the key string
	 * @throws Exception the exception
	 */
	private static String getKeyString(java.security.Key key) throws Exception {
		byte[] keyBytes = key.getEncoded();
		String s = Base64.getEncoder().encodeToString(keyBytes);
		return s;
	}

	/**
	 * The Class Key.
	 */
	public static class Key {

		/** The pub_key. */
		public String pub_key;

		/** The pri_key. */
		public String pri_key;

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object.toString()
		 */
		@Override
		public String toString() {
			return "Key:[pub:" + pub_key + ", pri:" + pri_key + "]";
		}
	}

	/**
	 * Gets the public key.
	 * 
	 * @param key the key
	 * @return the public key
	 * @throws Exception the exception
	 */
	private static PublicKey getPublicKey(String key) throws Exception {
		byte[] keyBytes;
		keyBytes = Base64.getDecoder().decode(key);
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PublicKey publicKey = keyFactory.generatePublic(keySpec);
		return publicKey;
	}

	/**
	 * Gets the private key.
	 * 
	 * @param key the key
	 * @return the private key
	 * @throws Exception the exception
	 */
	private static PrivateKey getPrivateKey(String key) throws Exception {
		byte[] keyBytes;
		keyBytes = Base64.getDecoder().decode(key);

		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
		return privateKey;
	}
}
