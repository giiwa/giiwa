package org.giiwa.core.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtil {

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
