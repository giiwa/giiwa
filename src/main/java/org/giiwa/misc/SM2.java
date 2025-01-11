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
import java.security.spec.*;
import java.util.Base64;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

/**
 * SM2 => RSA
 * 
 * @author yjiang
 */
public class SM2 {

	static Log log = LogFactory.getLog(SM2.class);

	/**
	 * decode data with private key.
	 * 
	 * @param data    the data
	 * @param pri_key the pri_key
	 * @return the byte[]
	 * @throws Exception
	 */
	public static byte[] decode(byte[] data, String pri_key) throws Exception {

		SM2Engine engine = new SM2Engine();

		PrivateKey privateKey = getPrivateKey(pri_key);

		BCECPrivateKey sm2PriK = (BCECPrivateKey) privateKey;
		org.bouncycastle.jce.spec.ECParameterSpec localECParameterSpec = sm2PriK.getParameters();
		ECDomainParameters localECDomainParameters = new ECDomainParameters(localECParameterSpec.getCurve(),
				localECParameterSpec.getG(), localECParameterSpec.getN());
		ECPrivateKeyParameters localECPrivateKeyParameters = new ECPrivateKeyParameters(sm2PriK.getD(),
				localECDomainParameters);
		engine.init(false, localECPrivateKeyParameters);
		byte[] arrayOfByte3 = engine.processBlock(data, 0, data.length);
		return arrayOfByte3;

	}

	/**
	 * encode the data with the public key.
	 * 
	 * @param data    the data
	 * @param pub_key the pub_key
	 * @return the byte[]
	 * @throws Exception
	 */
	public static byte[] encode(byte[] data, String pub_key) throws Exception {

		ECPublicKeyParameters localECPublicKeyParameters = null;

		PublicKey publicKey = getPublicKey(pub_key);

		if (publicKey instanceof BCECPublicKey) {
			BCECPublicKey localECPublicKey = (BCECPublicKey) publicKey;
			org.bouncycastle.jce.spec.ECParameterSpec localECParameterSpec = localECPublicKey.getParameters();
			ECDomainParameters localECDomainParameters = new ECDomainParameters(localECParameterSpec.getCurve(),
					localECParameterSpec.getG(), localECParameterSpec.getN());
			localECPublicKeyParameters = new ECPublicKeyParameters(localECPublicKey.getQ(), localECDomainParameters);
		}
		SM2Engine engine = new SM2Engine();
		engine.init(true, new ParametersWithRandom(localECPublicKeyParameters, new SecureRandom()));
		byte[] arrayOfByte2;
		arrayOfByte2 = engine.processBlock(data, 0, data.length);
		return arrayOfByte2;

	}

	/**
	 * generate key pair which include public and private key.
	 * 
	 * @param length the length
	 * @return the key
	 */
	public static Key generate(int length) {

		try {
			KeyPairGenerator keyPairGenerator = null;
			SecureRandom secureRandom = new SecureRandom();
			ECGenParameterSpec sm2Spec = new ECGenParameterSpec("sm2p256v1");
			keyPairGenerator = KeyPairGenerator.getInstance("EC", new BouncyCastleProvider());
			keyPairGenerator.initialize(sm2Spec);
			keyPairGenerator.initialize(sm2Spec, secureRandom);
			KeyPair keyPair = keyPairGenerator.generateKeyPair();

			PrivateKey privateKey = keyPair.getPrivate();
			PublicKey publicKey = keyPair.getPublic();

			Key k = new Key();
			k.pub_key = new String(Base64.getEncoder().encode(publicKey.getEncoded()));
			k.pri_key = new String(Base64.getEncoder().encode(privateKey.getEncoded()));

			return k;
		} catch (Exception e) {

		}
		return null;

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
	private static PublicKey getPublicKey(String publicKey) throws Exception {

		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey));
		KeyFactory keyFactory = KeyFactory.getInstance("EC", new BouncyCastleProvider());
		return keyFactory.generatePublic(publicKeySpec);

	}

	/**
	 * Gets the private key.
	 * 
	 * @param key the key
	 * @return the private key
	 * @throws Exception the exception
	 */
	private static PrivateKey getPrivateKey(String privateKey) throws Exception {

		PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey));
		KeyFactory keyFactory = KeyFactory.getInstance("EC", new BouncyCastleProvider());
		return keyFactory.generatePrivate(pkcs8EncodedKeySpec);

	}

	public static void main(String[] args) {

		Key key = SM2.generate(2048);
		SM2.pemPrikey(key.pri_key);
		SM2.pemPubkey(key.pub_key);

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
