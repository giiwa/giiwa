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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * IO utility
 * 
 * @author wujun
 *
 */
public final class IOUtil {

  /**
   * the utility api of copying all data in "inputstream" to "outputstream".
   * please refers copy(in, out, boolean)
   *
   * @param in
   *          the inputstream
   * @param out
   *          the outputstream
   * @return int the size of copied
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public static int copy(InputStream in, OutputStream out) throws IOException {
    return copy(in, out, true);
  }

  /**
   * delete the file or the path.
   *
   * @param f
   *          the file or the path
   * @return the number deleted
   * @throws IOException
   *           throw exception when delete the file or directory error
   */
  public static int delete(File f) throws IOException {
    int count = 0;
    if (f.isFile()) {
      f.delete();
      count++;
    } else if (f.isDirectory()) {
      File[] ff = f.listFiles();
      if (ff != null && ff.length > 0) {
        for (File f1 : ff) {
          count += delete(f1);
        }
      }
      f.delete();
      count++;
    }
    return count;
  }

  /**
   * copy files.
   *
   * @param src
   *          the source file
   * @param dest
   *          the destination file
   * @return the number copied
   * @throws IOException
   *           throw exception when copy failed
   */
  public static int copyDir(File src, File dest) throws IOException {
    dest.mkdirs();
    int count = 0;
    if (src.isFile()) {
      // copy file
      count++;
      copy(src, new File(dest.getCanonicalPath() + "/" + src.getName()));

    } else if (src.isDirectory()) {
      // copy dir
      File[] ff = src.listFiles();
      if (ff != null && ff.length > 0) {
        for (File f : ff) {
          count += copyDir(f, new File(dest.getCanonicalPath() + "/" + src.getName()));
        }
      } else {
        new File(dest.getCanonicalPath() + "/" + src.getName()).mkdirs();
      }
    }
    return count;
  }

  /**
   * copy all the files except
   * 
   * @param src
   * @param dest
   * @param except
   * @return
   * @throws IOException
   */
  public static int copyDir(File src, File dest, String[] except) throws IOException {

    Set<String> ex = new HashSet<String>();
    for (String s : except) {
      ex.add(s);
    }

    dest.mkdirs();
    int count = 0;
    if (src.isFile()) {
      // copy file
      count++;
      copy(src, new File(dest.getCanonicalPath() + "/" + src.getName()));
    } else if (src.isDirectory()) {
      // copy dir
      File[] ff = src.listFiles();
      if (ff != null && ff.length > 0) {
        for (File f : ff) {
          if (!ex.contains(f.getName())) {
            count += copyDir(f, new File(dest.getCanonicalPath() + "/" + src.getName()));
          }
        }
      } else {
        new File(dest.getCanonicalPath() + "/" + src.getName()).mkdirs();
      }
    }
    return count;
  }

  /**
   * copy file src to file destination.
   *
   * @param src
   *          the source file
   * @param dest
   *          the destination file
   * @return int of copied
   * @throws IOException
   *           throw exception when copy file failed
   */
  public static int copy(File src, File dest) throws IOException {
    return copy(new FileInputStream(src), new FileOutputStream(dest), true);
  }

  /**
   * copy the data in "inputstream" to "outputstream", from start to end.
   *
   * @param in
   *          the inputstream
   * @param out
   *          the outputstream
   * @param start
   *          the start position of started
   * @param end
   *          the end position of ended
   * @param closeAfterDone
   *          close after done, true: close if done, false: not close
   * @return int the size of copied
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public static int copy(InputStream in, OutputStream out, long start, long end, boolean closeAfterDone)
      throws IOException {
    try {
      if (in == null || out == null)
        return 0;

      byte[] bb = new byte[1024 * 4];
      int total = 0;
      in.skip(start);
      int ii = (int) Math.min((end - start), bb.length);
      int len = in.read(bb, 0, ii);
      while (len > 0) {
        out.write(bb, 0, len);
        total += len;
        ii = (int) Math.min((end - start - total), bb.length);
        len = in.read(bb, 0, ii);
        out.flush();
      }
      return total;
    } finally {
      if (closeAfterDone) {
        if (in != null) {
          in.close();
        }
        if (out != null) {
          out.close();
        }
      }
    }
  }

  /**
   * Copy data in "inputstream" to "outputstream".
   *
   * @param in
   *          the inputstream
   * @param out
   *          the outputstream
   * @param closeAfterDone
   *          close after done, true: close if done, false: not close
   * @return int the size of copied
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public static int copy(InputStream in, OutputStream out, boolean closeAfterDone) throws IOException {
    try {
      if (in == null || out == null)
        return 0;

      byte[] bb = new byte[1024 * 4];
      int total = 0;
      int len = in.read(bb);
      while (len > 0) {
        out.write(bb, 0, len);
        total += len;
        len = in.read(bb);
        out.flush();
      }
      return total;
    } finally {
      if (closeAfterDone) {
        if (in != null) {
          in.close();
        }
        if (out != null) {
          out.close();
        }
      }
    }
  }

}
