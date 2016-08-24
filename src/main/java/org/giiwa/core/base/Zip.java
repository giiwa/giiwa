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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.giiwa.core.json.JSON;

/**
 * The {@code Zip} Class used to Zip File operations,
 * 
 * @author joe
 *
 */
public class Zip {

  /**
   * Zip.
   * 
   * @param b
   *          the b
   * @return the byte[]
   * @throws Exception
   *           the exception
   */
  public static byte[] zip(byte[] b) throws Exception {

    Deflater def = new Deflater();
    def.setInput(b);
    def.finish();

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buf = new byte[1024];

    while (!def.finished()) {
      int l = def.deflate(buf);
      out.write(buf, 0, l);
    }

    out.flush();

    out.close();

    return out.toByteArray();
  }

  /**
   * Unzip.
   * 
   * @param b
   *          the b
   * @return the byte[]
   * @throws Exception
   *           the exception
   */
  public static byte[] unzip(byte[] b) throws Exception {
    Inflater in = new Inflater();
    in.setInput(b);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buf = new byte[1024];
    while (!in.finished()) {
      int l = in.inflate(buf);
      out.write(buf, 0, l);
    }

    in.end();
    out.flush();
    out.close();
    return out.toByteArray();
  }

  /**
   * find the filename in zip file.
   *
   * @param filename
   *          the filename
   * @param zip
   *          the zip
   * @return InputStream
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public static InputStream find(String filename, ZipFile zip) throws IOException {

    ZipEntry e = zip.getEntry(filename);
    if (e != null) {
      return zip.getInputStream(e);
    }
    return null;

  }

  /**
   * get json object from the filename in zip file.
   *
   * @param filename
   *          the filename
   * @param zip
   *          the zip
   * @return JSONObject
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public static JSON getJSON(String filename, ZipFile zip) throws IOException {
    InputStream in = find(filename, zip);
    if (in != null) {
      ByteArrayOutputStream out = null;
      try {
        out = new ByteArrayOutputStream();
        byte[] bb = new byte[16 * 1024];
        int len = in.read(bb);
        while (len > 0) {
          out.write(bb, 0, len);
          len = in.read(bb);
        }
        bb = null;

        out.flush();

        return JSON.fromObject(out.toByteArray());
      } finally {
        if (in != null) {
          in.close();
        }
        if (out != null) {
          out.close();
        }
      }
    }

    return null;
  }
}
