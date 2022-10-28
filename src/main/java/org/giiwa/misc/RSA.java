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

import java.io.StringWriter;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import java.util.Base64;

import javax.crypto.Cipher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

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
			log.error(e.getMessage(), e);
		}

		return null;
	}

	public static byte[] decode(byte[] data, java.security.Key key) {

		try {
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

			cipher.init(Cipher.DECRYPT_MODE, key);

			byte[] deBytes = cipher.doFinal(data);

			return deBytes;
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage(), e);
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
	 * RSA encode with RSA/ECB/PKCS1Padding
	 * 
	 * @param data bytes of data
	 * @param key  key of private or public
	 * @return
	 */
	public static byte[] encode(byte[] data, java.security.Key key) {

		try {

			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

			cipher.init(Cipher.ENCRYPT_MODE, key);
			byte[] enBytes = cipher.doFinal(data);

			return enBytes;
		} catch (Exception e) {
			log.error("key=" + key + ", data.length=" + data.length, e);
			e.printStackTrace();
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
	public static PublicKey getPublicKey(String key) throws Exception {
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
	public static PrivateKey getPrivateKey(String key) throws Exception {
		byte[] keyBytes;
		keyBytes = Base64.getDecoder().decode(key);

		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
		return privateKey;

	}

	public static void main(String[] args) {

		Key key = RSA.generate(2048);
		RSA.pemPrikey(key.pri_key);
		RSA.pemPubkey(key.pub_key);

	}

	public static String pemPrikey(String pri_key) {

		try {
			PrivateKey key = getPrivateKey(pri_key);
			PemObject obj = new PemObject("PRIVATE KEY", key.getEncoded());
			StringWriter out = new StringWriter();
			PemWriter pem = new PemWriter(out);
			pem.writeObject(obj);
			pem.close();
			out.close();
			String s1 = out.toString();

//			System.out.println(s1);

			return s1;
		} catch (Exception e) {
			// ignore
		}
		return null;

	}

	public static String pemPubkey(String pub_key) {

		try {
			PublicKey key = getPublicKey(pub_key);
			PemObject obj = new PemObject("PUBLIC KEY", key.getEncoded());
			StringWriter out = new StringWriter();
			PemWriter pem = new PemWriter(out);
			pem.writeObject(obj);
			pem.close();
			out.close();
			String s1 = out.toString();

//			System.out.println(s1);

			return s1;
		} catch (Exception e) {
			// ignore
		}
		return null;

	}

}
