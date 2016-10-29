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
package org.giiwa.core.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * MD5 utility
 * 
 * @author wujun
 *
 */
public class MD5 {
  private static Log log = LogFactory.getLog(MD5.class);

  /**
   * Generate Md5 string for the file
   *
   * @param f
   *          the file
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
   * Generate Md5 string for the inputstream.
   *
   * @param fis
   *          the inputstream
   * @return the string
   * @throws Exception
   *           the exception
   */
  public static String md5(InputStream fis) throws Exception {
    MessageDigest md = MessageDigest.getInstance("MD5");

    byte[] buffer = new byte[8192];
    int length;
    while ((length = fis.read(buffer)) != -1) {
      md.update(buffer, 0, length);
    }

    return new String(org.apache.commons.codec.binary.Hex.encodeHex(md.digest()));
  }

  /**
   * Generate Md5 string for the string
   *
   * @param target
   *          the target
   * @return the string
   */
  public static String md5(String target) {
    return DigestUtils.md5Hex(target);
  }

  /**
   * The main method.
   *
   * @param args
   *          the arguments
   */
  public static void main(String[] args) {
    File f = new File("/Users/wujun/d/workspace/giiwa/README.md");
    System.out.println(md5(f));
  }

}
