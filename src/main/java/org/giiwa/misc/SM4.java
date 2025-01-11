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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 * SM4 => DES/AES
 * 
 * @author yjiang
 */
public class SM4 {

	static Log log = LogFactory.getLog(SM4.class);

	public static final int BLOCK_SIZE = 16;

	/**
	 * ECB
	 * 
	 * @param in
	 * @param keyBytes
	 * @return
	 */
	public static byte[] encode(byte[] in, byte[] keyBytes) {

		SM4Engine engine = new SM4Engine();
		engine.init(true, new KeyParameter(keyBytes));
		int inLen = in.length;
		byte[] out = new byte[inLen];

		int times = inLen / BLOCK_SIZE;

		for (int i = 0; i < times; i++) {
			engine.processBlock(in, i * BLOCK_SIZE, out, i * BLOCK_SIZE);
		}

		return out;
	}

	/**
	 * 
	 * @param in
	 * @param keyBytes
	 * @return
	 */
	public static String encode_string(byte[] in, byte[] keyBytes) {
		byte[] out = encode(in, keyBytes);
		String cipher = org.bouncycastle.util.encoders.Hex.toHexString(out);
		return cipher;
	}

	/**
	 * 
	 * @param content
	 * @param key
	 * @return
	 */
	public static String encode(String content, String key) {
		byte[] in = Hex.decode(content);
		byte[] keyBytes = Hex.decode(key);

		String cipher = encode_string(in, keyBytes);
		return cipher;
	}

	/**
	 * 
	 * @param in
	 * @param keyBytes
	 * @return
	 */
	public static byte[] decode(byte[] in, byte[] keyBytes) {

		SM4Engine engine = new SM4Engine();
		engine.init(false, new KeyParameter(keyBytes));
		int inLen = in.length;
		byte[] out = new byte[inLen];

		int times = inLen / BLOCK_SIZE;

		for (int i = 0; i < times; i++) {
			engine.processBlock(in, i * BLOCK_SIZE, out, i * BLOCK_SIZE);
		}

		return out;

	}

	/**
	 * 
	 * @param in
	 * @param keyBytes
	 * @return
	 */
	public static String decode_string(byte[] in, byte[] keyBytes) {
		byte[] out = decode(in, keyBytes);
		String plain = org.bouncycastle.util.encoders.Hex.toHexString(out);
		return plain;
	}

	/**
	 * 
	 * @param cipher
	 * @param key
	 * @return
	 */
	public static String decode(String cipher, String key) {
		byte[] in = Hex.decode(cipher);
		byte[] keyBytes = Hex.decode(key);

		String plain = decode_string(in, keyBytes);
		return plain;
	}

}
