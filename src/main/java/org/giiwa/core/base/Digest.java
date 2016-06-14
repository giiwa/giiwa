/**
 * Copyright (C) 2010 Gifox Networks
 * 
 * @project mms
 * @author jjiang
 * @date 2010-10-23
 */
package org.giiwa.core.base;

import java.security.*;

import javax.crypto.*;
import javax.crypto.spec.DESKeySpec;

// TODO: Auto-generated Javadoc
/**
 * The Class Digest.
 */
public class Digest {

  /** The md. */
  private static MessageDigest md;

  static {
    try {
      // SHA, MD5
      md = MessageDigest.getInstance("MD5");
    } catch (Exception e) {
    }
  }

  /**
   * Digest.
   * 
   * @param s
   *          the s
   * @return the byte[]
   */
  public static byte[] digest(byte[] s) {
    return md.digest(s);
  }

  /**
   * Md5.
   *
   * @param str the str
   * @return the string
   */
  public static String md5(String str) {
    MessageDigest messageDigest = null;
    try {
      messageDigest = MessageDigest.getInstance("MD5");
      messageDigest.reset();
      messageDigest.update(str.getBytes("UTF-8"));
    } catch (Exception e) {
      return null;
    }

    byte[] byteArray = messageDigest.digest();

    StringBuffer md5StrBuff = new StringBuffer();

    for (int i = 0; i < byteArray.length; i++) {
      if (Integer.toHexString(0xFF & byteArray[i]).length() == 1)
        md5StrBuff.append("0").append(Integer.toHexString(0xFF & byteArray[i]));
      else
        md5StrBuff.append(Integer.toHexString(0xFF & byteArray[i]));
    }
    return md5StrBuff.toString();
  }

  /**
   * Decrypt.
   *
   * @param key the key
   * @param str the str
   * @return the byte[]
   * @throws Exception the exception
   */
  public static byte[] decrypt(String key, String str) throws Exception {
    DESKeySpec desKeySpec = new DESKeySpec(key.getBytes());
    SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
    Key k = keyFactory.generateSecret(desKeySpec);

    Cipher cipher = Cipher.getInstance("DES");
    cipher.init(Cipher.DECRYPT_MODE, k);
    byte b[] = Hex.decode(str);

    return cipher.doFinal(b);

  }

  /**
   * Encrypt.
   *
   * @param key the key
   * @param str the str
   * @return the string
   * @throws Exception the exception
   */
  public static String encrypt(String key, byte[] str) throws Exception {
    DESKeySpec desKeySpec = new DESKeySpec(key.getBytes());
    SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
    Key k = keyFactory.generateSecret(desKeySpec);

    Cipher cipher = Cipher.getInstance("DES");
    cipher.init(Cipher.ENCRYPT_MODE, k);

    return new String(Hex.encode(cipher.doFinal(str)));
  }
}
