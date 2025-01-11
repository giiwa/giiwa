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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.dao.X;

/**
 * MD5 utility
 * 
 * @author wujun
 *
 */
public class MD5 {
	private static Log log = LogFactory.getLog(MD5.class);

	/**
	 * Generate Md5 string for the file.
	 *
	 * @param f the file
	 * @return the string
	 */
	public static String md5(File f) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(f);
			return md5(fis);
		} catch (Exception e) {
			log.error("f=" + f.getAbsolutePath(), e);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
				}
			}
		}
		return null;
	}

	/**
	 * Generate Md5 string for the string.
	 *
	 * @param target the target
	 * @return the string
	 */
	public static String md5(String target) {
		return DigestUtils.md5Hex(target);
	}

	public static String md5(InputStream fis) throws Exception {
		try {
			return DigestUtils.md5Hex(fis);
		} finally {
			X.close(fis);
		}
	}

	public static String md5(byte[] bb) throws Exception {
		return DigestUtils.md5Hex(bb);
	}

	/**
	 * Generate SHA1 string for the string.
	 *
	 * @param data the string
	 * @return the sha1 string
	 */
	public static String sha1(String data) {
		return DigestUtils.sha1Hex(data);
	}

	public static String sha1(InputStream fis) throws Exception {
		try {
			return DigestUtils.sha1Hex(fis);
		} finally {
			X.close(fis);
		}
	}

	public static String sha256(InputStream fis) throws Exception {
		try {
			return DigestUtils.sha256Hex(fis);
		} finally {
			X.close(fis);
		}
	}

}
