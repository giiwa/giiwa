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
package org.giiwa.crypto;

import java.io.IOException;
import java.io.InputStream;

import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.giiwa.dao.X;

/**
 * SM3 => MD5
 * 
 * @author joe
 *
 */
public class SM3 {

	/**
	 * 
	 * hash the src and return hex string
	 * 
	 * @param src
	 * @return
	 */
	public static String hash(String src) {
		byte[] srcData = src.getBytes();
		byte[] encrypt = hash(srcData);
		String cipherStr = org.bouncycastle.util.encoders.Hex.toHexString(encrypt);
		return cipherStr;
	}

	/**
	 * hash the src by code , and return hex string
	 * 
	 * @param src
	 * @param code
	 * @return
	 */
	public static String hash(String src, String code) {
		byte[] encrypt = hash(src.getBytes(), code.getBytes());
		return org.bouncycastle.util.encoders.Hex.toHexString(encrypt);
	}

	public static String hash(String code, InputStream... ins) throws IOException {
		try {

			KeyParameter keyParameter = new KeyParameter(code.getBytes());
			SM3Digest digest = new SM3Digest();
			HMac hmac = new HMac(digest);
			hmac.init(keyParameter);

			byte[] buffer = new byte[1024 * 32];
			int bytesRead;

			// 2. 流式读取并更新摘要
			// update 方法可以多次调用，模拟数据流不断写入
			for (InputStream in : ins) {
				while ((bytesRead = in.read(buffer)) != -1) {
					hmac.update(buffer, 0, bytesRead);
				}
			}

			// 3. 完成计算并获取结果
			byte[] result = new byte[hmac.getMacSize()]; // SM3 固定为 32 字节
			hmac.doFinal(result, 0);

			// 4. 转换为 16 进制字符串返回
			return org.bouncycastle.util.encoders.Hex.toHexString(result);
		} finally {
			for (InputStream in : ins) {
				X.close(in);
			}
		}
	}

	/**
	 * hash the src
	 * 
	 * @param src
	 * @return
	 */
	public static byte[] hash(byte[] src) {
		SM3Digest sm3Digest = new SM3Digest();
		sm3Digest.update(src, 0, src.length);
		byte[] encrypt = new byte[sm3Digest.getDigestSize()];
		sm3Digest.doFinal(encrypt, 0);
		return encrypt;
	}

	public static String hash(InputStream... ins) throws IOException {
		try {
			SM3Digest sm3Digest = new SM3Digest();

			byte[] buffer = new byte[1024 * 32];
			int bytesRead;

			// 2. 流式读取并更新摘要
			// update 方法可以多次调用，模拟数据流不断写入
			for (InputStream in : ins) {
				while ((bytesRead = in.read(buffer)) != -1) {
					sm3Digest.update(buffer, 0, bytesRead);
				}
			}

			// 3. 完成计算并获取结果
			byte[] result = new byte[sm3Digest.getDigestSize()]; // SM3 固定为 32 字节
			sm3Digest.doFinal(result, 0);

			// 4. 转换为 16 进制字符串返回
			return org.bouncycastle.util.encoders.Hex.toHexString(result);
		} finally {
			for (InputStream in : ins) {
				X.close(in);
			}
		}
	}

	/**
	 * hash the src by code
	 * 
	 * @param src
	 * @param code
	 * @return
	 */
	public static byte[] hash(byte[] src, byte[] code) {
		KeyParameter keyParameter = new KeyParameter(code);
		SM3Digest digest = new SM3Digest();
		HMac mac = new HMac(digest);
		mac.init(keyParameter);
		mac.update(src, 0, src.length);
		byte[] result = new byte[mac.getMacSize()];
		mac.doFinal(result, 0);
		return result;
	}

}
