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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.X;

// TODO: Auto-generated Javadoc
public class FileVersion {

  static Log log = LogFactory.getLog(FileVersion.class);

  public static enum R {
    SAME, DIFF, HIGH, LOW
  }

  String  name;
  Version ver;
  File    f;

  public String getName() {
    return name;
  }

  /**
   * Instantiates a new file util.
   *
   * @param f
   *          the f
   */
  public FileVersion(File f) {
    this.f = f;
    name = f.getName();
    String[] ss = name.split("[-_]");
    if (ss.length > 1) {
      String ver = ss[ss.length - 1];
      name = name.substring(0, name.length() - ver.length() - 1);
      ver = ver.substring(0, ver.length() - 4); // cut off ".jar"
      this.ver = new Version(ver);
    }
  }

  public File getFile() {
    return f;
  }

  /**
   * Compare to.
   *
   * @param f1
   *          the f1
   * @return the r
   */
  public R compareTo(File f1) {
    return compareTo(new FileVersion(f1));
  }

  public static class Version {
    String   ver;
    String[] ss;

    /**
     * Instantiates a new version.
     *
     * @param s
     *          the s
     */
    Version(String s) {
      ver = s;
      if (s != null) {
        ss = s.split("\\.");
      }
    }

    /**
     * Compare to.
     *
     * @param v1
     *          the v1
     * @return the r
     */
    R compareTo(Version v1) {
      try {
        for (int i = 0; i < ss.length; i++) {
          int i1 = X.toInt(ss[i]);
          int i2 = v1.ss.length > i ? X.toInt(v1.ss[i]) : 0;

          if (i1 > i2) {
            return R.HIGH;
          } else if (i1 < i2) {
            return R.LOW;
          } else if (v1.ss.length == i) {
            return R.HIGH;
          }
        }
        return R.LOW;
      } catch (Exception e) {
        log.error("this=" + this + ", v1=" + v1, e);
      }

      return R.DIFF;
    }

    transient String _string;

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      if (_string == null) {
        _string = new StringBuilder("(ver=").append(ver).append(Helper.toString(ss)).append(")").toString();
      }

      return _string;
    }

    /**
     * Compare.
     *
     * @param v1
     *          the version string
     * @param v2
     *          the version string
     * @return 1: v1 &gt; v2 <br>
     *         -1: v1 &lt; v2 <br>
     *         0: v1 = v2
     */
    public static int compare(String v1, String v2) {
      if (X.isSame(v1, v2)) {
        return 0;
      }
      if (X.isEmpty(v1) || X.isSame(v2, "*")) {
        return -1;
      }
      if (X.isEmpty(v2) || X.isSame(v1, "*")) {
        return 1;
      }

      String[] s1 = v1.split("\\.");
      String[] s2 = v2.split("\\.");
      int len = Math.min(s1.length, s2.length);

      for (int i = 0; i < len; i++) {
        if (X.isSame(s1[i], "*")) {
          return 1;
        } else if (X.isSame(s2[i], "*")) {
          return -1;
        }

        int i1 = X.toInt(s1[i]);
        int i2 = X.toInt(s2[i]);
        if (i1 > i2) {
          return 1;
        } else if (i1 < i2) {
          return -1;
        }
      }

      if (s1.length > s2.length) {
        return 1;
      } else if (s1.length < s2.length) {
        return -1;
      }
      return 0;
    }

  }

  /**
   * Compare to.
   *
   * @param f1
   *          the f1
   * @return the r
   */
  public R compareTo(FileVersion f1) {
    if (!this.name.equalsIgnoreCase(f1.name)) {
      return R.DIFF;
    }

    if (ver != null) {
      if (f1.ver == null) {
        return R.HIGH;
      } else {
        return ver.compareTo(f1.ver);
      }
    } else if (f1.ver == null) {
      return R.SAME;
    } else {
      return R.LOW;
    }
  }

}
