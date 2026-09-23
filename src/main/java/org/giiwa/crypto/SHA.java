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

import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * RSA utility, RSA is using for encode some short key (such as password or DES
 * key), it's slow, and output longer data, NOT suitable for encode file.
 * 
 * @author yjiang
 */
public class SHA {

	static Log log = LogFactory.getLog(SHA.class);

	private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

	/**
	 * 计算 HMAC-SHA256 签名并返回十六进制字符串（小写）
	 * 
	 * @param data      待签名的原始数据字符串
	 * @param secretKey 双方约定的共享密钥
	 * @return 十六进制格式的签名字符串
	 */
	public static String hmac(String data, String secretKey) {

		try {
			// 1. 初始化密钥规范
			SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8),
					HMAC_SHA256_ALGORITHM);

			// 2. 获取 Mac 实例并初始化
			Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
			mac.init(secretKeySpec);

			// 3. 执行 HMAC 计算，生成字节数组
			byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

			// 4. 将字节数组转换为十六进制字符串（便于传输与比对）
			return bytesToHex(hmacBytes);
		} catch (Exception err) {
			log.error(err.getMessage(), err);
		}
		return null;
	}

	/**
	 * 验证客户端传来的签名是否合法
	 */
	public static boolean verify(String data, String secretKey, String clientSignature) {
		String serverCalculatedSignature = hmac(data, secretKey);
		// 使用 equals 比较，防止时序攻击在实际生产中可使用 MessageDigest.isEqual()
		return serverCalculatedSignature.equals(clientSignature);
	}

	/**
	 * 字节数组转十六进制字符串（小写）
	 */
	private static String bytesToHex(byte[] bytes) {
		char[] hexChars = "0123456789abcdef".toCharArray();
		char[] result = new char[bytes.length * 2];
		for (int i = 0; i < bytes.length; i++) {
			int val = bytes[i] & 0xFF; // 消除符号位影响
			result[i * 2] = hexChars[val >>> 4]; // 取高 4 位
			result[i * 2 + 1] = hexChars[val & 0x0F]; // 取低 4 位
		}
		return new String(result);
	}

}
