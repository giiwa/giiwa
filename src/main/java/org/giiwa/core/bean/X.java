/*
 * Copyright 2015 Giiwa, Inc. and/or its affiliates.
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

import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// TODO: Auto-generated Javadoc
/**
 * The {@code X} Class used to define contains.
 * 
 * @author joe
 *
 */
public class X {

  private static Log         log             = LogFactory.getLog(X.class);

  /** The Constant AMINUTE. */
  public static final long   AMINUTE         = 1000 * 60;

  /** The Constant AHOUR. */
  public static final long   AHOUR           = AMINUTE * 60;

  /** The Constant ADAY. */
  public static final long   ADAY            = 24 * AHOUR;

  /** The Constant AWEEK. */
  final static public long   AWEEK           = 7 * ADAY;

  /** The Constant AMONTH. */
  final static public long   AMONTH          = 30 * ADAY;

  /** The Constant AYEAR. */
  final static public long   AYEAR           = 365 * ADAY;

  /** The Constant TITLE. */
  public static final String TITLE           = "title";

  /** The Constant TYPE. */
  public static final String TYPE            = "type";

  /** The Constant ID. */
  public static final String ID              = "id";

  /** The Constant _TYPE. */
  public static final String _TYPE           = "_type";

  /** The Constant _LIKE. */
  public static final String _LIKE           = "_like";

  /** The Constant _ID. */
  public static final String _ID             = "_id";

  /** The Constant KEYWORD. */
  public static final String KEYWORD         = "keyword";

  /** The Constant STATE. */
  public static final String STATE           = "state";

  /** The Constant NAME. */
  public static final String NAME            = "name";

  /** The Constant BRAND. */
  public static final String BRAND           = "brand";

  /** The Constant AUDIT. */
  public static final String AUDIT           = "audit";

  /** The Constant EMPTY. */
  public static final String EMPTY           = "";

  /** The Constant ALL. */
  public static final String ALL             = "all";

  /** The Constant TAGS. */
  public static final String TAGS            = "tags";

  /** The Constant DATE. */
  public static final String DATE            = "_date";

  /** The Constant GROUP. */
  public static final String GROUP           = "_group";

  /** The Constant query. */
  public static final String query           = "query";

  /** The Constant wd. */
  public static final String wd              = "wd";

  /** The Constant key. */
  public static final String key             = "key";

  /** The Constant output. */
  public static final String output          = "output";

  /** The Constant RESULTS. */
  public static final String RESULTS         = "results";

  /** The Constant PN. */
  public static final String PN              = "pn";

  /** The Constant STATUS. */
  public static final String STATUS          = "status";

  /** The Constant OK. */
  public static final int    OK              = 200;

  /** The Constant UTF8. */
  public static final String UTF8            = "UTF-8";

  /** The Constant RATE_MILE_TO_KM. */
  public static final double RATE_MILE_TO_KM = 1.609344;                  // 英里和公里的比率

  public static final String CAPABILITY      = "capability";
  public static final String UID             = "uid";
  public static final String CLIENTID        = "clientid";
  public static final String KEY             = "key";
  public static final String NONE            = "none";

  public static final int    OK_200          = 200;
  public static final int    FAIL            = 201;
  public static final int    FAIL201         = 201;
  public static final int    FAIL301         = 301;
  public static final int    FAIL401         = 401;

  public static final String URI             = "uri";
  public static final String CODE            = "code";

  public static final String MESSAGE         = "message";

  public static final String WARN            = "warn";

  public static final String PARAM           = "param";

  public static final String CALLBACK        = "callback";

  public static final String CONTENTTYPE     = "contenttype";

  public static final String ERROR           = "error";
  public static final String DATA            = "data";

  public static final String FILE            = "file";
  public static final String LENGTH          = "length";
  public static final String TOTAL           = "total";
  public static final String POSITION        = "position";
  public static final String DONE            = "done";

  public static final String LIST            = "list";
  public static final String S               = "s";
  public static final String E               = "e";
  public static final String VERSION         = "version";
  public static final String SEQ             = "seq";
  public static final String RESULT          = "result";

  private X() {
  }

  /**
   * test whether equals the two objects.
   *
   * @param s1
   *          the s1
   * @param s2
   *          the s2
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

      try {
        return Long.parseLong(s);
      } catch (Exception e) {
        log.error(e);
      }
    }
    return defaultValue;
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
      try {
        return Integer.parseInt(s);
      } catch (Exception e) {
        log.error(e);
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

      try {
        return Float.parseFloat(s);
      } catch (Exception e) {
        log.error(e);

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
   * Mile2 meter.
   * 
   * @param miles
   *          the miles
   * @return int
   */
  public static int mile2Meter(double miles) {
    double dMeter = miles * RATE_MILE_TO_KM * 1000;
    return (int) dMeter;
  }

  /**
   * Meter2 mile.
   * 
   * @param meter
   *          the meter
   * @return double
   */
  public static double meter2Mile(double meter) {
    return meter / RATE_MILE_TO_KM / 1000;
  }

  /** The Constant UNIT. */
  public static final String[] UNIT = { "", "0", "00", "000", "万", "0万", "00万", "000万", "亿", "0亿" };

  /** The Constant tz. */
  public static final TimeZone tz   = TimeZone.getTimeZone("Asia/Shanghai");

}
