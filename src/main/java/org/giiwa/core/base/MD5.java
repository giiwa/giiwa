package org.giiwa.core.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MD5 {
  private static Log log = LogFactory.getLog(MD5.class);

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

  public static String md5(InputStream fis) throws Exception {
    MessageDigest md = MessageDigest.getInstance("MD5");

    byte[] buffer = new byte[8192];
    int length;
    while ((length = fis.read(buffer)) != -1) {
      md.update(buffer, 0, length);
    }

    return new String(org.apache.commons.codec.binary.Hex.encodeHex(md.digest()));
  }

  public static String md5(String target) {
    return DigestUtils.md5Hex(target);
  }

  public static void main(String[] args) {
    File f = new File("/Users/wujun/d/workspace/giiwa/README.md");
    System.out.println(md5(f));
  }

}
