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
package org.giiwa.framework.bean;

import org.giiwa.core.bean.X;

/**
 * utility for request.
 *
 * @author yjiang
 * 
 */
public class Request {

  byte[] data;
  int    offset;

  /**
   * Instantiates a new request.
   * 
   * @param b
   *          the b
   * @param offset
   *          the offset
   */
  public Request(byte[] b, int offset) {
    this.data = b;
    this.offset = offset;
  }

  /**
   * Read byte.
   * 
   * @return the byte
   */
  public byte readByte() {
    if (offset < data.length) {
      return data[offset++];
    }
    return 0;
  }

  /**
   * Read short.
   * 
   * @return the short
   */
  public short readShort() {
    return (short) ((readByte() << 8) | (readByte() & 0xFF));
  }

  /**
   * Read int.
   * 
   * @return the int
   */
  public int readInt() {
    return (readShort() << 16) | (readShort() & 0xFFFF);
  }

  /**
   * Read long.
   * 
   * @return the long
   */
  public long readLong() {
    long h = (((long) readInt()) & 0x0FFFFFFFFL) << 32;
    long l = ((long) readInt()) & (0x0FFFFFFFFL);

    return h | l;
  }

  /**
   * Read string.
   * 
   * @return the string
   */
  public String readString() {
    int s = readInt();
    if (s == 0) {
      return null;
    }
    byte[] b = new byte[s];
    System.arraycopy(data, offset, b, 0, s);
    offset += s;
    return new String(b);
  }

  /**
   * Read bytes.
   * 
   * @param len
   *          the len
   * @return the byte[]
   */
  public byte[] readBytes(int len) {
    if (len <= 0)
      return null;

    len = Math.min(len, data.length - offset);
    byte[] b = new byte[len];
    System.arraycopy(data, offset, b, 0, len);
    offset += len;
    return b;
  }

  /**
   * Read double.
   * 
   * @return the double
   */
  public double readDouble() {
    return X.toDouble(readString(), 0);
  }

  /**
   * Read float.
   * 
   * @return the float
   */
  public float readFloat() {
    return X.toFloat(readString(), 0);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Request:").append(offset).append("[");
    for (int i = 0; i < offset; i++) {
      if (i > 0)
        sb.append(" ");
      sb.append(data[i]);
    }
    return sb.append("]").toString();
  }

}
