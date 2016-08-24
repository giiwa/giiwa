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
package org.giiwa.core.bean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The {@code X} Class used to define contains.
 * 
 * @author joe
 *
 */
public class X {

  private static Log         log         = LogFactory.getLog(X.class);

  /** The Constant 60*1000. */
  public static final long   AMINUTE     = 1000 * 60;

  /** The Constant 60*AMINUTE. */
  public static final long   AHOUR       = AMINUTE * 60;

  /** The Constant 24*AHOUR. */
  public static final long   ADAY        = 24 * AHOUR;

  /** The Constant 7*ADAY. */
  final static public long   AWEEK       = 7 * ADAY;

  /** The Constant 30*ADAY. */
  final static public long   AMONTH      = 30 * ADAY;

  /** The Constant 365*ADAY. */
  final static public long   AYEAR       = 365 * ADAY;

  /** The Constant "title". */
  public static final String TITLE       = "title";

  /** The Constant "type". */
  public static final String TYPE        = "type";

  /** The Constant "id". */
  public static final String ID          = "id";

  /** The Constant "keyword". */
  public static final String KEYWORD     = "keyword";

  /** The Constant "state". */
  public static final String STATE       = "state";

  /** The Constant "name". */
  public static final String NAME        = "name";

  /** The Constant "audit". */
  public static final String AUDIT       = "audit";

  /** The Constant "". */
  public static final String EMPTY       = "";

  /** The Constant "all". */
  public static final String ALL         = "all";

  /** The Constant "url". */
  public static final String URL         = "url";

  /** The Constant "uri". */
  public static final String URI         = "uri";

  /** The Constant "tags". */
  public static final String TAGS        = "tags";

  /** The Constant "query". */
  public static final String QUERY       = "query";

  /** The Constant "output". */
  public static final String OUTPUT      = "output";

  /** The Constant "results". */
  public static final String RESULTS     = "results";

  /** The Constant "result". */
  public static final String RESULT      = "result";

  /** The Constant "status". */
  public static final String STATUS      = "status";

  /** The Constant "ok". */
  public static final int    OK          = 200;

  /** The Constant "UTF-8". */
  public static final String UTF8        = "UTF-8";

  /** The Constant "uid". */
  public static final String UID         = "uid";
  /** The Constant "key". */
  public static final String KEY         = "key";
  /** The Constant "none". */
  public static final String NONE        = "none";

  /** The Constant "code". */
  public static final String CODE        = "code";

  /** The Constant "message". */
  public static final String MESSAGE     = "message";

  /** The Constant "warn". */
  public static final String WARN        = "warn";

  /** The Constant "param". */
  public static final String PARAM       = "param";

  /** The Constant "callback". */
  public static final String CALLBACK    = "callback";

  /** The Constant "contenttype". */
  public static final String CONTENTTYPE = "contenttype";

  /** The Constant "error". */
  public static final String ERROR       = "error";
  /** The Constant "data". */
  public static final String DATA        = "data";

  /** The Constant "file". */
  public static final String FILE        = "file";
  /** The Constant "length". */
  public static final String LENGTH      = "length";
  /** The Constant "total". */
  public static final String TOTAL       = "total";
  /** The Constant "position". */
  public static final String POSITION    = "position";
  /** The Constant "done". */
  public static final String DONE        = "done";

  /** The Constant "list". */
  public static final String LIST        = "list";
  /** The Constant "s". */
  public static final String S           = "s";
  /** The Constant "n". */
  public static final String E           = "e";
  /** The Constant "version". */
  public static final String VERSION     = "version";
  /** The Constant "seq". */
  public static final String SEQ         = "seq";

  private X() {
  }

  /**
   * test whether equals the two objects.
   *
   * @param s1
   *          the objectßßß s1
   * @param s2
   *          the object s2
   * @return boolean
   */
  public static boolean isSame(Object s1, Object s2) {
    if (s1 == s2)
      return true;
    if (X.isEmpty(s1) && X.isEmpty(s2))
      return true;

    if (s1 != null) {
      return s1.equals(s2);
    }

    return false;
  }

  /**
   * convert the v to long, if failed using defaultValue.
   *
   * @param v
   *          the v
   * @param defaultValue
   *          the default value
   * @return long
   */
  public static long toLong(Object v, long defaultValue) {
    if (v != null) {
      if (v instanceof Long) {
        return (Long) v;
      }
      if (v instanceof Integer) {
        return (Integer) v;
      }

      if (v instanceof Float) {
        return (long) ((Float) v).floatValue();
      }
      if (v instanceof Double) {
        return (long) ((Double) v).doubleValue();
      }

      StringBuilder sb = new StringBuilder();
      String s = v.toString();
      for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);

        c = X.getNumber(c);
        if (c >= '0' && c <= '9') {
          sb.append(c);
        } else if (c == '-' && sb.length() == 0) {
          sb.append(c);
        } else if (sb.length() > 0) {
          break;
        }
      }
      s = sb.toString();
      if (s.length() > 0) {
        try {
          return Long.parseLong(s);
        } catch (Exception e) {
          log.error(e);
        }
      }
    }
    return defaultValue;
  }

  /**
   * To int.
   *
   * @param v
   *          the v
   * @return the int
   */
  public static int toInt(Object v) {
    return X.toInt(v, 0);
  }

  /**
   * parse a object to a integer.
   * 
   * @param v
   *          the v
   * @param defaultValue
   *          the default value
   * @return int
   */
  public static int toInt(Object v, int defaultValue) {
    if (v != null) {
      if (v instanceof Integer) {
        return (Integer) v;
      }
      if (v instanceof Float) {
        return (int) ((Float) v).floatValue();
      }
      if (v instanceof Double) {
        return (int) ((Double) v).doubleValue();
      }
      String s = v.toString();
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < s.length(); i++) {
        char c = X.getNumber(s.charAt(i));
        if (c >= '0' && c <= '9') {
          sb.append(c);
        } else if (c == '-' && sb.length() == 0) {
          sb.append(c);
        } else if (sb.length() > 0) {
          break;
        }
      }
      s = sb.toString();

      if (s.length() > 0) {
        try {
          return Integer.parseInt(s);
        } catch (Exception e) {
          log.error(e);
        }
      }
    }

    return defaultValue;
  }

  /**
   * same, which test null.
   *
   * @param s
   *          the s
   * @return boolean
   */
  public static boolean isEmpty(Object s) {
    return s == null || X.EMPTY.equals(s) || X.EMPTY.equals(s.toString().trim());
  }

  /**
   * parse a object to a float with defaultvalue.
   * 
   * @param v
   *          the v
   * @param defaultValue
   *          the default value
   * @return float
   */
  public static float toFloat(Object v, float defaultValue) {
    if (v != null) {
      if (v instanceof Integer) {
        return (Integer) v;
      }
      if (v instanceof Float) {
        return (Float) v;
      }
      if (v instanceof Double) {
        return (float) ((Double) v).doubleValue();
      }
      String s = v.toString();

      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < s.length(); i++) {
        char c = X.getNumber(s.charAt(i));
        if (c >= '0' && c <= '9') {
          sb.append(c);
        } else if (c == '-' && sb.length() == 0) {
          sb.append(c);
        } else if (c == '.') {
          if (sb.indexOf(".") > -1) {
            break;
          } else {
            sb.append(c);
          }
        } else if (sb.length() > 0) {
          break;
        }
      }
      s = sb.toString();

      if (s.length() > 0) {
        try {
          return Float.parseFloat(s);
        } catch (Exception e) {
          log.error(e);
        }
      }
    }
    return defaultValue;
  }

  // public static boolean isEmpty(String s) {
  // return s == null || X.EMPTY.equals(s) || X.EMPTY.equals(s.trim());
  // }

  private static final char[][] DIGS = { "０１２３４５６７８９".toCharArray(), "零一二三四五六七八九".toCharArray(),
      "零壹贰叁肆伍陆柒捌玖".toCharArray() };

  /**
   * test the "s" and return a number, that convert Chinese number to real
   * number.
   *
   * @param s
   *          the s
   * @return char
   */
  public static char getNumber(char s) {
    if (s >= '0' && s <= '9') {
      return s;
    }

    for (char[] d : DIGS) {
      for (int i = 0; i < d.length; i++) {
        if (s == d[i]) {
          return (char) ('0' + i);
        }
      }
    }
    return s;
  }

  /**
   * To double.
   * 
   * @param v
   *          the value object
   * @param defaultValue
   *          the default value when the v is null or parse error
   * @return the double
   */
  public static double toDouble(Object v, double defaultValue) {
    if (v != null) {
      if (v instanceof Integer) {
        return (Integer) v;
      }
      if (v instanceof Float) {
        return (Float) v;
      }
      if (v instanceof Double) {
        return ((Double) v).doubleValue();
      }
      String s = v.toString().trim();
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        if (c >= '0' && c <= '9') {
          sb.append(c);
        } else if (c == '-' && sb.length() == 0) {
          sb.append(c);
        } else if (c == '.') {
          if (sb.indexOf(".") > -1) {
            break;
          } else {
            sb.append(c);
          }
        } else if (sb.length() > 0) {
          break;
        }
      }
      s = sb.toString();

      try {
        return Double.parseDouble(s);
      } catch (Exception e) {
        log.error(e);

      }
    }
    return defaultValue;
  }

  /**
   * test the "s" is number.
   *
   * @param s
   *          the s
   * @return boolean
   */
  public static boolean isNumber(Object s) {
    if (s == null) {
      return false;
    }
    if (s instanceof Number) {
      return true;
    }

    String s1 = s.toString();
    if (s1.length() == 0) {
      return false;
    }

    int dot = 0;
    for (int i = 0; i < s1.length(); i++) {
      char c = s1.charAt(i);
      if (c == '.') {
        dot++;
        if (dot > 1) {
          return false;
        }
      } else if (c == '-' && i == 0) {
        continue;
      } else if (c < '0' || c > '9') {
        return false;
      }
    }

    return true;
  }

  /**
   * To long.
   *
   * @param v
   *          the v
   * @return the long
   */
  public static long toLong(Object v) {
    return toLong(v, 0);
  }

}
