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
package org.giiwa.bean;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.crypto.Base32;
import org.giiwa.crypto.Digest;
import org.giiwa.dao.*;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;

/**
 * 密码实体类
 * 
 * @author yjiang
 * 
 */
@Table(name = "gi_passwd", memo = "GI-密码")
public final class Passwd extends Bean {
	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(Passwd.class);

	private final static BeanDAO<String, Passwd> dao = BeanDAO.create(Passwd.class);

	public final static String RSA = "RSA";
	public final static String AES = "AES";
	public final static String MYSQL_NATIVE_PASSWORD = "mysql_native_password";
	public final static String SHA1 = "SHA1";
	public final static String SHA256 = "SHA256";
	public final static String MD5 = "MD5";
	public final static String SM2 = "SM2";
	public final static String SM3 = "SM3";
	public final static String SM4 = "SM4";

	@Column(memo = "主键", unique = true, size = 64)
	private String id;

	@Column(memo = "备注", size = 512)
	private String memo;

	@Column(memo = "类别", size = 128)
	private String clazz;

	@Column(memo = "参考id", size = 128)
	private String dataid;

	@Column(memo = "加密算法", size = 50, value = "RSA, AES, MYSQL, SHA256, SHA1, MD5, SM2, SM3, SM4")
	private String alg;

	@Column(memo = "密码", size = 1024)
	private String password;

	public String getPassword() {
		return password;
	}

	public static Passwd load(String clazz, String dataid) {
		Passwd e = dao.load(W.create().and("clazz", clazz).and("dataid", dataid));
		e.decode();
		return e;
	}

	private void decode() {
		if (X.isSame(alg, RSA)) {

		} else if (X.isSame(alg, AES)) {

		} else if (X.isSame(alg, SM2)) {

		} else if (X.isSame(alg, SM4)) {

		} else {
			// 其他不可解密
		}
	}

	public static boolean save(String clazz, String dataid, String plainpasswd, String alg) {
		try {
			String id = org.giiwa.crypto.SM3.hash(clazz + "/" + dataid + "/" + alg);
			String s = encode(plainpasswd, alg);
			if (dao.exists(id)) {
				dao.update(id, V.create().append("clazz", clazz).append("dataid", dataid).append("password", s)
						.append("alg", alg));
			} else {
				dao.insert(V.create().append(X.ID, id).append("clazz", clazz).append("dataid", dataid)
						.append("password", s).append("alg", alg));
			}
			return true;
		} catch (Exception err) {
			log.error(err.getMessage(), err);
			return false;
		}
	}

	private static String encode(String plainPassword, String alg) throws Exception {

		if (plainPassword == null) {
			return null;
		}

		if (X.isSame(alg, RSA)) {
			// 非对称加密
			
		} else if (X.isSame(alg, AES)) {
			// 对称加密
			return Base32.encode(Digest.encode(plainPassword.getBytes(), Key.get("giiwa", 24)));
		} else if (X.isSame(alg, Passwd.MYSQL_NATIVE_PASSWORD)) {
			// mysql
			MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
			// first sha1: SHA1(明文)
			byte[] sha1Pass = sha1.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
			// second sha1: SHA1(SHA1(明文)) → 这就是服务端要存储的值
			return toHex(sha1.digest(sha1Pass));
		}
		return null;
	}

	/**
	 * 将20字节转为40位十六进制字符串，方便存数据库varchar <br>
	 */
	private static String toHex(byte[] bytes) {
		if (bytes == null)
			return null;
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(String.format("%02x", b & 0xff));
		}
		return sb.toString();
	}

}
