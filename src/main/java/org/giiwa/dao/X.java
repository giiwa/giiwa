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
package org.giiwa.dao;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.conf.Global;
import org.giiwa.engine.JS;
import org.giiwa.json.JSON;
import org.giiwa.misc.CSV;
import org.giiwa.misc.GImage;
import org.giiwa.misc.IOUtil;
import org.giiwa.misc.Zip;
import org.giiwa.task.BiConsumer;
import org.giiwa.task.Function;
import org.giiwa.web.Language;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;
import org.openjdk.nashorn.internal.runtime.Undefined;

/**
 * The {@code X} Class used to define contains.
 * 
 * @author joe
 *
 */
public final class X {

	private static Log log = LogFactory.getLog(X.class);

	/** The Constant 60*1000. */
	public static final long AMINUTE = 1000 * 60;

	/** The Constant 60*AMINUTE. */
	public static final long AHOUR = AMINUTE * 60;

	/** The Constant 24*AHOUR. */
	public static final long ADAY = 24 * AHOUR;

	/** The Constant 7*ADAY. */
	final static public long AWEEK = 7 * ADAY;

	/** The Constant 30*ADAY. */
	final static public long AMONTH = (long) (365 * ADAY / 12f);

	/** The Constant 365*ADAY. */
	final static public long AYEAR = 365 * ADAY;

	/** The Constant "id". */
	public static final String ID = "id";

	/** The Constant "state". */
	public static final String STATE = "state";

	/** The Constant "". */
	public static final String EMPTY = "";

	/** The Constant "url". */
	public static final String URL = "url";

	/** The Constant "uri". */
	public static final String URI = "uri";

	/** The Constant "status". */
	public static final String STATUS = "status";

	/** The Constant "UTF-8". */
	public static final String UTF8 = "UTF-8";

	/** The Constant "none". */
	public static final String NONE = "none";

	/** The Constant "message". */
	public static final String MESSAGE = "message";

	public static final String TRACE = "trace";

	/** The Constant "warn". */
	public static final String WARN = "warn";

	/** the Constant "error" . */
	public static final String ERROR = "error";

	/** the Constant "created" */
	public static final String CREATED = "created";

	/** the Constant "updated" */
	public static final String UPDATED = "updated";

	public static final String VERSION = "_version";

	public static final int ITEMS_PER_PAGE = 20;

	public static final X inst = new X();

	private X() {
	}

	/**
	 * test whether equals the two objects.
	 *
	 * @param s1 the object s1
	 * @param s2 the object s2
	 * @return boolean
	 */
	public static boolean isSame(Object s1, Object s2) {
		if (s1 == s2)
			return true;

		if (s1 == null && s2 == null)
			return true;

		if (s1 == null || s2 == null) {
			return false;
		}

		if (s1.equals(s2)) {
			return true;
		}

		if (s1 instanceof Number && s2 instanceof Number) {
			if ((s1 instanceof Short || s1 instanceof Integer || s1 instanceof Long)
					&& (s2 instanceof Short || s2 instanceof Integer || s2 instanceof Long)) {
				return X.toLong(s1) == X.toLong(s2);
			}

			if ((s1 instanceof Float || s1 instanceof Double) && (s2 instanceof Float || s2 instanceof Double)) {
				double d1 = X.toDouble(s1);
				double d2 = X.toDouble(s2);
				// 0.10000000149011612
				return d1 > (d2 - 0.00000001) && d1 < (d2 + 0.00000001);
			}
		}

		if (s1 instanceof String && s2 instanceof String) {
			return ((String) s1).equalsIgnoreCase((String) s2);
		}

		if (s1.getClass().isArray()) {
			s1 = X.asList(s1, e -> e);

		}

		if (s2.getClass().isArray()) {
			s2 = X.asList(s2, e -> e);
		}

		if (s1 instanceof Collection && s2 instanceof Collection) {
			Collection<?> c1 = (Collection<?>) s1;
			Collection<?> c2 = (Collection<?>) s2;

			for (Object o1 : c1) {
				if (!c2.contains(o1))
					return false;
			}
			for (Object o2 : c2) {
				if (!c1.contains(o2))
					return false;
			}
			return true;
		}

		return s1.equals(s2);

	}

	public static boolean isSame2(Object s1, Object s2) {
		if (s1 == s2)
			return true;

		if (s1 == null && s2 == null)
			return true;

		if (s1 == null || s2 == null) {
			return false;
		}

		if (s1 instanceof Number && s2 instanceof Number) {
			if ((s1 instanceof Short || s1 instanceof Integer || s1 instanceof Long)
					&& (s2 instanceof Short || s2 instanceof Integer || s2 instanceof Long)) {
				return X.toLong(s1) == X.toLong(s2);
			}

			if ((s1 instanceof Float || s1 instanceof Double) && (s2 instanceof Float || s2 instanceof Double)) {
				double d1 = X.toDouble(s1);
				double d2 = X.toDouble(s2);
				return d1 > (d2 - 0.00000001) && d1 < (d2 + 0.00000001);
			}
		}

		if (s1 instanceof String && s2 instanceof String) {
			return ((String) s1).equals((String) s2);
		}

		if (s1.getClass().isArray()) {
			s1 = X.asList(s1, e -> e);

		}

		if (s2.getClass().isArray()) {
			s2 = X.asList(s2, e -> e);
		}

		if (s1 instanceof Collection && s2 instanceof Collection) {
			Collection<?> c1 = (Collection<?>) s1;
			Collection<?> c2 = (Collection<?>) s2;

			for (Object o1 : c1) {
				if (!c2.contains(o1))
					return false;
			}
			for (Object o2 : c2) {
				if (!c1.contains(o2))
					return false;
			}
			return true;
		}

		return s1.equals(s2);

	}

	/**
	 * safely parse the object to long, if failed return default value.
	 *
	 * @param v            the object
	 * @param defaultValue the default value
	 * @return long
	 */
	public static long toLong(Object v, long defaultValue) {

		if (v != null) {
			if (v instanceof Double) {
				return Math.round((Double) v);
			} else if (v instanceof Float) {
				return Math.round((Float) v);
			} else if (v instanceof Number) {
				return ((Number) v).longValue();
			} else if (v instanceof byte[]) {
				byte[] a = (byte[]) v;
				return ByteBuffer.wrap(a).order(ByteOrder.BIG_ENDIAN).getLong();
			}

			String s = v.toString();

			try {
				return Long.parseLong(s);
			} catch (Exception e) {
				// ignore
			}

			boolean f = false;
			StringBuilder sb = new StringBuilder();

			boolean fill = false;

			for (int i = 0; i < s.length(); i++) {
				char c = X.getNumber(s.charAt(i));
				if (c >= '0' && c <= '9') {
					sb.append(c);
					fill = false;
				} else if (c == ':') {
					if (sb.length() == 0) {
						sb.append('1');
					}
					fill = true;
				} else if (c == '-' || c == '/' || c == '+' || c == '*') {
					if (fill) {
						sb.append('0');
						fill = false;
					}
					sb.append(c);
					if (i > 0)
						f = true;
				} else if (c == ',' || c == ' ') {
					if (fill) {
						sb.append('0');
						fill = false;
					}
					// skip
				} else if (sb.length() > 0) {
					if (fill) {
						sb.append('0');
						fill = false;
					}
					break;
				}
			}
			if (fill) {
				sb.append('0');
				fill = false;
			}

			if (sb.length() > 0) {
				s = sb.toString();
				try {
					if (f) {
						v = JS.calculate(s);
						if (v instanceof Double) {
							return Math.round((Double) v);
						} else if (v instanceof Float) {
							return Math.round((Float) v);
						} else if (v instanceof Number) {
							return ((Number) v).longValue();
						}
					}
					return Long.parseLong(s);

				} catch (Throwable e) {
					log.error(e.getMessage(), e);
				}
			}
		}
		return defaultValue;
	}

	public static long toLong2(byte[] v) {

		if (v != null) {
			return ByteBuffer.wrap(v).order(ByteOrder.LITTLE_ENDIAN).getLong();
		}
		return 0;
	}

	/**
	 * safely parse the object to integer, if failed return 0.
	 *
	 * @param v the object
	 * @return int
	 */
	public static int toInt(Object v) {
		return X.toInt(v, 0);
	}

	/**
	 * safely parse the object to integer. if failed return the default value
	 * 
	 * @param v            the object
	 * @param defaultValue the default value
	 * @return int
	 */
	public static int toInt(Object v, int defaultValue) {
		if (v != null) {
			if (v instanceof Double) {
				return (int) Math.round((Double) v);
			} else if (v instanceof Float) {
				return Math.round((Float) v);
			} else if (v instanceof Number) {
				return ((Number) v).intValue();
			} else if (v instanceof byte[]) {
				byte[] a = (byte[]) v;
				return ByteBuffer.wrap(a).order(ByteOrder.BIG_ENDIAN).getInt();
			}

			String s = v.toString();

			try {
				return Integer.parseInt(s);
			} catch (Exception e) {
				// ignore
			}

			boolean f = false;
			StringBuilder sb = new StringBuilder();

			boolean fill = false;

			for (int i = 0; i < s.length(); i++) {
				char c = X.getNumber(s.charAt(i));
				if (c >= '0' && c <= '9') {
					sb.append(c);
					fill = false;
				} else if (c == ':') {
					if (sb.length() == 0) {
						sb.append('1');
					}
					fill = true;
				} else if (c == '-' || c == '/' || c == '+' || c == '*') {
					if (fill) {
						sb.append('0');
						fill = false;
					}
					sb.append(c);
					if (i > 0)
						f = true;
				} else if (c == ',' || c == ' ') {
					if (fill) {
						sb.append('0');
						fill = false;
					}
					// skip
				} else if (sb.length() > 0) {
					if (fill) {
						sb.append('0');
						fill = false;
					}
					break;
				}
			}
			if (fill) {
				sb.append('0');
				fill = false;
			}

			if (sb.length() > 0) {

				s = sb.toString();
				try {
					if (f) {
						v = JS.calculate(s);
						if (v instanceof Double) {
							return (int) Math.round((Double) v);
						} else if (v instanceof Float) {
							return Math.round((Float) v);
						} else if (v instanceof Number) {
							return ((Number) v).intValue();
						}
					}

					return Integer.parseInt(s);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		}

		return defaultValue;
	}

	/**
	 * 
	 * @param v
	 * @return
	 */
	public static int toInt2(byte[] v) {
		if (v != null) {
			return ByteBuffer.wrap(v).order(ByteOrder.LITTLE_ENDIAN).getInt();
		}

		return 0;
	}

	/**
	 * test the object is empty? null , empty string, empty collection, empty map.
	 *
	 * @param s the object, may string, list, map
	 * @return boolean, return true if null, or empty
	 */
	@SuppressWarnings({ "rawtypes" })
	public static boolean isEmpty(Object s) {
		if (s == null) {
			return true;
		}

		if (s instanceof Undefined) {
			return true;
		}

		if (s instanceof String) {
			return X.EMPTY.equals(s);
		} else if (s instanceof Collection) {
			return ((Collection) s).isEmpty();
		} else if (s instanceof Map) {
			return ((Map) s).isEmpty();
		}

		return false;

	}

	/**
	 * safely parse the object to float, if failed return 0.
	 * 
	 * @param v the object
	 * @return float
	 */
	public static float toFloat(Object v) {
		return toFloat(v, 0);
	}

	public static float toFloat(Object v, float defaultValue) {
		if (v != null) {
			if (v instanceof Number) {
				float f = ((Number) v).floatValue();
				if (Float.isFinite(f))
					return f;

				return defaultValue;
			} else if (v instanceof byte[]) {
				byte[] a = (byte[]) v;
				return ByteBuffer.wrap(a).order(ByteOrder.BIG_ENDIAN).getFloat();
			}

			String s = v.toString();

			try {
				float f = Float.parseFloat(s);
				if (Float.isFinite(f))
					return f;

				return defaultValue;
			} catch (Exception e) {
				// ignore
			}

			boolean f = false;

			StringBuilder sb = new StringBuilder();

			boolean fill = false;
			for (int i = 0; i < s.length(); i++) {
				char c = X.getNumber(s.charAt(i));
				if (c >= '0' && c <= '9') {
					sb.append(c);
					fill = false;
				} else if (c == ':') {
					if (sb.length() == 0) {
						sb.append('1');
					}
					fill = true;
				} else if (c == '-' || c == '/' || c == '+' || c == '*') {
					if (fill) {
						sb.append('0');
						fill = false;
					}
					sb.append(c);
					if (i > 0)
						f = true;
				} else if (c == ',' || c == ' ') {
					if (fill) {
						sb.append('0');
						fill = false;
					}
					// skip
				} else if (c == '.' && sb.indexOf(".") == -1) {
					if (fill) {
						sb.append('0');
						fill = false;
					}
					sb.append(c);
				} else if (sb.length() > 0) {
					if (fill) {
						sb.append('0');
						fill = false;
					}
					break;
				}
			}
			if (fill) {
				sb.append('0');
				fill = false;
			}

			if (sb.length() > 0) {

				s = sb.toString();
				try {
					if (f) {
						Object f1 = JS.calculate(s);
						if (f1 instanceof Number) {
							float f2 = ((Number) f1).floatValue();
							if (Float.isFinite(f2))
								return f2;

							return defaultValue;
						}

					} else {
						float f2 = Float.parseFloat(s);
						if (Float.isFinite(f2))
							return f2;

						return defaultValue;
					}
				} catch (Exception e) {
					log.error(e);
				}
			}
		}
		return defaultValue;
	}

	private static final char[][] DIGS = { "０１２３４５６７８９".toCharArray(), "零一二三四五六七八九十".toCharArray(),
			"零壹贰叁肆伍陆柒捌玖拾".toCharArray() };

	public static final int KB = 1024;
	public static final int MB = 1000 * KB;
	public static final int GB = 1000 * MB;
	public static final int TB = 1000 * GB;

	/**
	 * test the "s" and return a number, that convert Chinese number to real number.
	 *
	 * @param s the s
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
	 * safely parse the object to double, if failed return 0.
	 * 
	 * @param v the object
	 * @return the double result
	 */
	public static double toDouble(Object v) {
		return toDouble(v, 0);
	}

	public static double toDouble2(byte[] v) {
		return ByteBuffer.wrap(v).order(ByteOrder.LITTLE_ENDIAN).getDouble();
	}

	public static float toFloat2(byte[] v) {
		return ByteBuffer.wrap(v).order(ByteOrder.LITTLE_ENDIAN).getFloat();
	}

	/**
	 * safely parse the object to double, if failed return default value.
	 * 
	 * @param v            the object
	 * @param defaultValue the default value when the v is null or parse error
	 * @return the double
	 */
	public static double toDouble(Object v, double defaultValue) {
		if (v != null) {
			if (v instanceof Number) {
				double d = ((Number) v).doubleValue();
				if (Double.isFinite(d))
					return d;
				return defaultValue;
			} else if (v instanceof byte[]) {
				byte[] a = (byte[]) v;
				return ByteBuffer.wrap(a).order(ByteOrder.BIG_ENDIAN).getDouble();
			}

			String s = v.toString();

			try {
				double d = Double.parseDouble(s);
				if (Double.isFinite(d))
					return d;
				return defaultValue;
			} catch (Exception e) {
				// ignore
			}

			boolean f = false;
			StringBuilder sb = new StringBuilder();

			boolean fill = false;
			for (int i = 0; i < s.length(); i++) {
				char c = X.getNumber(s.charAt(i));
				if (c >= '0' && c <= '9') {
					sb.append(c);
					fill = false;
				} else if (c == ':') {
					if (sb.length() == 0) {
						sb.append('1');
					}
					fill = true;
				} else if (c == '-' || c == '/' || c == '+' || c == '*') {
					if (fill) {
						sb.append('0');
						fill = false;
					}
					sb.append(c);
					if (i > 0)
						f = true;
				} else if (c == ',' || c == ' ') {
					if (fill) {
						sb.append('0');
						fill = false;
					}
					// skip
				} else if (c == '.' && sb.indexOf(".") == -1) {
					if (fill) {
						sb.append('0');
						fill = false;
					}
					sb.append(c);
				} else if (sb.length() > 0) {
					if (fill) {
						sb.append('0');
						fill = false;
					}
					break;
				}
			}
			if (fill) {
				sb.append('0');
				fill = false;
			}

			if (sb.length() > 0) {
				s = sb.toString();
				try {
					if (f) {
						Object f1 = JS.calculate(s);
						if (f1 instanceof Number) {
							double d = ((Number) f1).doubleValue();
							if (Double.isFinite(d))
								return d;
							return defaultValue;
						}

					} else {
						double d = Double.parseDouble(s);
						if (Double.isFinite(d))
							return d;

						return defaultValue;
					}
				} catch (Exception e) {
					log.error("v=" + v, e);
				}
			}
		}
		return defaultValue;
	}

	/**
	 * 
	 * @param v       value
	 * @param format, "0.00", "#,##"
	 * @return
	 */
	public static String format(Object v, String format) {
		DecimalFormat df = new DecimalFormat(format);
		return df.format(X.toDouble(v));
	}

	public static boolean isFloat(Object s) {

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
	 * test the "s" is number.
	 *
	 * @param s the s
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

		for (int i = 0; i < s1.length(); i++) {
			char c = s1.charAt(i);
			if (c == '-' && i == 0) {
				continue;
			} else if (c < '0' || c > '9') {
				return false;
			}
		}

		return true;
	}

	/**
	 * test is ascii
	 * 
	 * @param s
	 * @return
	 */
	public static boolean isAscii(Object s) {
		if (s == null) {
			return false;
		}

		String s1 = s.toString();
		if (s1.length() == 0) {
			return false;
		}

		for (int i = 0; i < s1.length(); i++) {
			char c = s1.charAt(i);
			if (c < 10 || c > 127) {
				return false;
			}
		}

		return true;
	}

	/**
	 * safely parse the object to long, if failed return 0.
	 *
	 * @param v the object
	 * @return the long
	 */
	public static long toLong(Object v) {
		return toLong(v, 0);
	}

	/**
	 * 分割字符串，出现转意符则跳过
	 *
	 * @param src   the source string
	 * @param regex the split regex
	 * @return the String
	 */
	public static String[] split(String src, String regex) {

		List<String> l1 = new ArrayList<String>();
		if (src != null) {
			String[] ss = src.split(regex);
//			int pos = 0;
			for (int i = 0; i < ss.length; i++) {
				String s = ss[i];
//				pos = src.indexOf(s, pos);
//				while (s.endsWith("\\")) {
//					// 合并下一个
//					if (i < ss.length - 1) {
//						int pos2 = pos + s.length() + 1;
//						if (!X.isEmpty(ss[i + 1])) {
//							pos2 = src.indexOf(ss[i + 1], pos + s.length());
//						}
//						s = src.substring(pos, pos2 + ss[i + 1].length());
//						i++;
//					}
//				}
//				s = s.trim().replaceAll("\\\\\\\\", "\\\\");
				if (!X.isEmpty(s)) {
					l1.add(s);
				}
			}
		}

		return l1.toArray(new String[l1.size()]);
	}

	/**
	 * using patter.find to split the string <br>
	 * 
	 * eg: abc(.*)->aaa(.*)
	 * 
	 * @param src
	 * @param regex
	 * @return
	 */
	public static String[] split2(String src, String regex) {

		Pattern p = Pattern.compile(regex);
		Matcher m1 = p.matcher(src);

		/**
		 * find
		 */
		String[] params = null;
		if (m1.find()) {
			/**
			 * get all the params
			 */
			params = new String[m1.groupCount()];
			for (int i = 0; i < params.length; i++) {
				params[i] = m1.group(i + 1);
			}
		}
		return params;

	}

	public static String[] range(String s, String deli) {

		List<String> l1 = new ArrayList<String>();
		String[] ss = X.split(s, "[,;]");
		for (String s1 : ss) {

			String[] s2 = X.split(s1, deli);
			if (s2.length == 1) {
				if (!l1.contains(s1)) {
					l1.add(s1);
				}
			} else {
				String p1 = s2[0];
				String p2 = s2[1];

				String prefix = X.EMPTY;
				char c = p2.charAt(0);
				if (c >= '0' && c <= '9') {
					for (int i = p1.length() - 1; i >= 0; i--) {
						c = p1.charAt(i);
						if (c < '0' || c > '9') {
							prefix = p1.substring(0, i + 1);
							p1 = p1.substring(i + 1);
						}
					}
				} else {
					int i = p1.lastIndexOf(c);
					prefix = p1.substring(0, i + 1);
					p1 = p1.substring(i + 1);
					p2 = p2.substring(1);
				}

				if (!l1.contains(prefix + p1)) {
					l1.add(prefix + p1);
				}

				String p3 = p1;
				while (p3.compareTo(p2) < 0) {
					if (!l1.contains(prefix + p3)) {
						l1.add(prefix + p3);
					}
					p3 = X.add(p3, 1);
				}
				if (!l1.contains(prefix + p2)) {
					l1.add(prefix + p2);
				}
			}
		}
		return l1.toArray(new String[l1.size()]);
	}

	public static char[] range2(String s, String deli) {

		List<Character> l1 = new ArrayList<Character>();
		String[] ss = X.split(s, "[,;]");
		for (String s1 : ss) {

			String[] s2 = X.split(s1, deli);
			if (s2.length == 1) {
				if (!l1.contains(s1.charAt(0))) {
					l1.add(s1.charAt(0));
				}
			} else {
				char p1 = s2[0].charAt(0);
				char p2 = s2[1].charAt(0);
				char p3 = p1;

				while (p3 < p2) {
					if (!l1.contains(p3)) {
						l1.add(p3);
					}
					p3++;
				}
				if (!l1.contains(p2)) {
					l1.add(p2);
				}
			}
		}

		char[] cc = new char[l1.size()];
		for (int i = 0; i < cc.length; i++) {
			cc[i] = l1.get(i);
		}
		return cc;
	}

	/**
	 * close all
	 * 
	 * @param ss the cloeable object
	 */
	public static void close(Closeable... ss) {
		if (ss == null || ss.length == 0)
			return;

		for (Closeable s : ss) {
			if (s == null)
				continue;
			try {
				s.close();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

	}

	public static int sum(int[] ii) {
		int sum = 0;
		if (ii != null) {
			for (int i : ii) {
				sum += i;
			}
		}
		return sum;
	}

	public static String getCanonicalPath(String path) {
		if (path == null)
			return null;

		boolean begin = path.startsWith("\\") || path.startsWith("/");
		boolean end = path.endsWith("\\") || path.endsWith("/");

		List<String> l1 = new ArrayList<String>();
		String[] ss = X.split(path, "[\\\\/]");
		for (String s : ss) {
			if (X.isSame(s, ".") || X.isEmpty(s))
				continue;
			if (X.isSame(s, "..")) {
				if (!l1.isEmpty()) {
					l1.remove(l1.size() - 1);
				}
			} else {
				l1.add(s);
			}
		}

		StringBuilder sb = new StringBuilder();
		if (begin)
			sb.append(File.separator);

		int i = 0;
		for (String s : l1) {
			if (i > 0)
				sb.append(File.separator);
			sb.append(s);
			i++;
		}
		if (end && sb.charAt(sb.length() - 1) != File.separatorChar)
			sb.append(File.separator);

		return sb.toString();
	}

	public static int hexToInt(char b) {
		b = Character.toLowerCase(b);
		if (b >= '0' && b <= '9') {
			return b - '0';
		} else if (b >= 'a' && b <= 'f') {
			return b - 'a' + 10;
		} else if (b >= 'A' && b <= 'F') {
			return b - 'A' + 10;
		}
		return 0;
	}

	public static String toHex(byte b) {
		String s = Integer.toHexString(b & 0xFF);
		if (s.length() > 1)
			return s;
		return "0" + s;
	}

	public static boolean isIn(String s1, String... s2) {

		if (s1 == null)
			return false;

		if (s2 == null)
			return false;

		for (String s : s2) {
			if (X.isSame(s1, s) || s1.matches(s)) {
				return true;
			}
		}
		return false;

	}

	public static boolean isIn(Object s1, Object... s2) {

		if (s2 == null)
			return false;

		for (Object s : s2) {
			if (X.isArray(s) && !X.isArray(s1)) {
				for (Object ss : X.asList(s, null)) {
					if (X.isArray(ss)) {
						if (X.isIn(s1, ss)) {
							return true;
						}
					} else if (X.isSame(s1, ss)) {
						return true;
					}
				}
			} else if (X.isSame(s1, s)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isIn2(Object s1, Object... s2) {
		if (s1 == null)
			return false;

		if (s2 == null)
			return false;

		for (Object s : s2) {
			if (X.isSame2(s1, s)) {
				return true;
			}
		}
		return false;
	}

	public static <T> List<T> unique(List<T> l1) {
		List<T> l2 = new ArrayList<T>();
		l1.forEach(e -> {
			if (!l2.contains(e)) {
				l2.add(e);
			}
		});
		return l2;
	}

	public static boolean isArray(Object o) {

		if (o == null) {
			return false;
		}

		if (o instanceof ScriptObjectMirror) {
			ScriptObjectMirror m = (ScriptObjectMirror) o;
			if (m.isArray()) {
				return true;
//				for (Object o2 : m.values()) {
//					if (o2 instanceof ScriptObjectMirror) {
//						ScriptObjectMirror m1 = (ScriptObjectMirror) o2;
//						return m1.isArray();
//					} else {
//						return false;
//					}
//				}
			} else {
				return false;
			}
		} else if (o instanceof Iterable) {
			return true;
		} else if (o.getClass().isArray()) {
			return true;
		}
		return false;

	}

	/**
	 * to list
	 * 
	 * @param <T>
	 * @param <E>
	 * @param o
	 * @param cb
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T> List<T> asList(Object o, Function<Object, T> cb) {

		if (o == null) {
			return new ArrayList<T>();
		}

		List<T> l2 = new ArrayList<T>();

		if (o instanceof ScriptObjectMirror) {
			ScriptObjectMirror m = (ScriptObjectMirror) o;
			if (m.isArray()) {
				for (Object o2 : m.values()) {
					if (o2 instanceof ScriptObjectMirror) {
						ScriptObjectMirror m1 = (ScriptObjectMirror) o2;
						if (m1.isArray()) {
							List<?> l1 = JSON.fromObjects(m1);
							if (cb != null) {
								T t = cb.apply(l1);
								if (t != null) {
									l2.add(t);
								}
							} else {
								l2.add((T) l1);
							}
						} else {
							Object e = JSON.fromObject(m1);
							if (cb != null) {
								T t = cb.apply(e);
								if (t != null) {
									l2.add(t);
								}
							} else {
								l2.add((T) e);
							}
						}
					} else {
						if (cb != null) {
							T t = cb.apply(o2);
							if (t != null) {
								l2.add(t);
							}
						} else {
							l2.add((T) o2);
						}
					}
				}
			} else {
				Object e = JSON.fromObject(o);
				if (cb != null) {
					T t = cb.apply(e);
					if (t != null) {
						l2.add(t);
					}
				} else {
					l2.add((T) e);
				}
			}
		} else if (o instanceof Iterable) {
			Collection l1 = (Collection) o;
			for (Object e : l1) {
				if (e != null) {
					if (cb != null) {
						T t = cb.apply(e);
						if (t != null) {
							l2.add(t);
						}
					} else {
						l2.add((T) e);
					}
				}
			}
		} else if (o.getClass().isArray()) {

			String name = o.getClass().getName();

			if (X.isSame(name, "[D")) {
				double[] l1 = (double[]) o;
				for (Object e : l1) {
					if (e != null) {
						if (cb != null) {
							T t = cb.apply(e);
							if (t != null) {
								l2.add(t);
							}
						} else {
							l2.add((T) e);
						}
					}
				}
			} else if (X.isSame(name, "[I")) {
				int[] l1 = (int[]) o;
				for (Object e : l1) {
					if (e != null) {
						if (cb != null) {
							T t = cb.apply(e);
							if (t != null) {
								l2.add(t);
							}
						} else {
							l2.add((T) e);
						}
					}
				}
			} else if (X.isSame(name, "[J")) {
				long[] l1 = (long[]) o;
				for (Object e : l1) {
					if (e != null) {
						if (cb != null) {
							T t = cb.apply(e);
							if (t != null) {
								l2.add(t);
							}
						} else {
							l2.add((T) e);
						}
					}
				}
			} else if (X.isSame(name, "[F")) {
				float[] l1 = (float[]) o;
				for (Object e : l1) {
					if (e != null) {
						if (cb != null) {
							T t = cb.apply(e);
							if (t != null) {
								l2.add(t);
							}
						} else {
							l2.add((T) e);
						}
					}
				}
			} else if (X.isSame(name, "[B")) {
				byte[] l1 = (byte[]) o;
				for (Object e : l1) {
					if (e != null) {
						if (cb != null) {
							T t = cb.apply(e);
							if (t != null) {
								l2.add(t);
							}
						} else {
							l2.add((T) e);
						}
					}
				}
			} else if (X.isSame(name, "[S")) {
				short[] l1 = (short[]) o;
				for (Object e : l1) {
					if (e != null) {
						if (cb != null) {
							T t = cb.apply(e);
							if (t != null) {
								l2.add(t);
							}
						} else {
							l2.add((T) e);
						}
					}
				}
			} else if (X.isSame(name, "[C")) {
				char[] l1 = (char[]) o;
				for (Object e : l1) {
					if (e != null) {
						if (cb != null) {
							T t = cb.apply(e);
							if (t != null) {
								l2.add(t);
							}
						} else {
							l2.add((T) e);
						}
					}
				}
			} else if (X.isSame(name, "[Z")) {
				boolean[] l1 = (boolean[]) o;
				for (Object e : l1) {
					if (e != null) {
						if (cb != null) {
							T t = cb.apply(e);
							if (t != null) {
								l2.add(t);
							}
						} else {
							l2.add((T) e);
						}
					}
				}
			} else {
				Object[] l1 = (Object[]) o;
				for (Object e : l1) {
					if (e != null) {
						if (cb != null) {
							T t = cb.apply(e);
							if (t != null) {
								l2.add(t);
							}
						} else {
							l2.add((T) e);
						}
					}
				}
			}
		} else if (o != null) {
			if (cb != null) {
				T t = cb.apply(o);
				if (t != null) {
					l2.add(t);
				}
			} else {
				l2.add((T) o);
			}
		}
		return l2;
	}

	/**
	 * count the char number in the string
	 * 
	 * @param s the string
	 * @param c the char
	 * @return the number
	 */
	public static int count(String s, char c) {
		if (s == null)
			return 0;
		int n = 0;
		int i = s.indexOf(c);
		while (i > -1) {
			n++;
			i = s.indexOf(c, i + 1);
		}
		return n;
	}

	/**
	 * fill a new string with the s
	 * 
	 * @param s the string
	 * @param n the number
	 * @return the String
	 */
	public static String fill(String s, int n) {
		StringBuilder sb = new StringBuilder();
		while (sb.length() < n) {
			sb.append(s);
		}
		return sb.toString();
	}

	/**
	 * fill the n with s to max length
	 * 
	 * @param s
	 * @param n
	 * @param max
	 * @return
	 */
	public static String fill(String s, int n, int max) {

		int len = Integer.toString(max).length();
		String n1 = Integer.toString(n);
		StringBuilder sb = new StringBuilder();
		len -= n1.length();

		while (sb.length() < len) {
			sb.append(s);
		}
		return sb.append(n1).toString();

	}

	/**
	 * Descartes list
	 * 
	 * @param l1 the list
	 * @param l2 the list
	 * @return the list of list of T
	 */
	public static <T> List<List<T>> descartes(List<List<T>> l1, List<List<T>> l2) {
		if (l2 == null || l2.isEmpty())
			return l1;

		List<List<T>> r = new ArrayList<List<T>>();
		List<T> l3 = l2.remove(0);
		if (l1.isEmpty()) {
			for (T t : l3) {
				List<T> l4 = new ArrayList<T>();
				l4.add(t);
				r.add(l4);
			}
		} else
			for (List<T> e : l1) {
				for (T t : l3) {
					List<T> l4 = new ArrayList<T>(e);
					l4.add(t);
					r.add(l4);
				}
			}
		return descartes(r, l2);
	}

	public static long max(long[] ll) {
		long t = ll[0];
		for (int i = 1; i < ll.length; i++) {
			if (t < ll[i]) {
				t = ll[i];
			}
		}
		return t;
	}

	public static long min(long[] ll) {
		long t = ll[0];
		for (int i = 1; i < ll.length; i++) {
			if (t > ll[i]) {
				t = ll[i];
			}
		}
		return t;
	}

	public static int max(int[] ll) {
		int t = ll[0];
		for (int i = 1; i < ll.length; i++) {
			if (t < ll[i]) {
				t = ll[i];
			}
		}
		return t;
	}

	public static int min(int[] ll) {
		int t = ll[0];
		for (int i = 1; i < ll.length; i++) {
			if (t > ll[i]) {
				t = ll[i];
			}
		}
		return t;
	}

	public static double max(double[] ll) {
		double t = ll[0];
		for (int i = 1; i < ll.length; i++) {
			if (t < ll[i]) {
				t = ll[i];
			}
		}
		return t;
	}

	public static double min(double[] ll) {
		double t = ll[0];
		for (int i = 1; i < ll.length; i++) {
			if (t > ll[i]) {
				t = ll[i];
			}
		}
		return t;
	}

	public static boolean isIn(long id, long[] tt) {
		if (tt == null)
			return false;

		for (long t : tt) {
			if (t == id)
				return true;
		}
		return false;
	}

	public static String add(String number, int n) {
		long a = X.toLong(number) + n;
		String s = Long.toString(a);

		if (s.length() >= number.length()) {
			return s;
		}

		return number.substring(0, number.length() - s.length()) + s;
	}

	public static boolean isCauseBy(Throwable e, String regex, Class<?>... ee) {
		if (e == null)
			return false;

		if (ee != null) {
			for (Class<?> e1 : ee) {
				if (e.getClass().equals(e1)) {
					return true;
				}
			}
		}
		String s = e.getMessage();
		if (!X.isEmpty(s)) {
			s = s.replaceAll("\r", X.EMPTY).replaceAll("\n", X.EMPTY);
			if (log.isDebugEnabled())
				log.debug("s=" + s + ", matches?" + s.matches(regex));

			if (s.matches(regex))
				return true;
		}

		return isCauseBy(e.getCause(), regex);
	}

	/**
	 * link the object by deli
	 * 
	 * @param l1,  the list or array object
	 * @param deli
	 * @return
	 */
	public static String join(Object l1, String deli) {
		if (X.isEmpty(l1))
			return X.EMPTY;

		StringBuilder sb = new StringBuilder();
		List<Object> l2 = X.asList(l1, e -> e);
		for (Object o : l2) {
			if (sb.length() > 0)
				sb.append(deli);
//			if (o instanceof String) {
//				String s = (String) o;
//				s = s.replaceAll("\\\\", "\\\\\\\\");
//				o = s.replaceAll(deli, "\\\\" + deli);
//			}
			sb.append(X.isEmpty(o) ? X.EMPTY : o);
		}
		return sb.toString();
	}

	public static List<long[]> split(long sdate, long edate, String size) {

		Language lang = Language.getLanguage("zh_cn");

		List<long[]> l1 = new ArrayList<long[]>();
		if (X.isIn(size, "hour")) {
			long t1 = lang.parse(lang.format(sdate, "yyyyMMddHH"), "yyyyMMddHH");
			if (t1 < sdate) {
				t1 += X.AHOUR;
				l1.add(new long[] { sdate, t1 });
			}
			while (t1 < edate - X.AHOUR) {
				l1.add(new long[] { t1, t1 + X.AHOUR });
				t1 += X.AHOUR;
			}
			if (t1 < edate) {
				l1.add(new long[] { t1, edate });
			}

		} else if (X.isIn(size, "day")) {
			long t1 = lang.parse(lang.format(sdate, "yyyyMMdd"), "yyyyMMdd");
			if (t1 < sdate) {
				t1 += X.ADAY;
				l1.add(new long[] { sdate, t1 });
			}
			while (t1 < edate - X.ADAY) {
				l1.add(new long[] { t1, t1 + X.ADAY });
				t1 += X.ADAY;
			}
			if (t1 < edate) {
				l1.add(new long[] { t1, edate });
			}

		} else if (X.isIn(size, "month")) {
			long t1 = lang.parse(lang.format(sdate, "yyyyMM"), "yyyyMM");
			if (t1 < sdate) {
				t1 = lang.parse((X.toInt(lang.format(t1, "yyyyMM")) + 1) + "", "yyyyMM");
				t1 = lang.parse(lang.format(t1, "yyyyMM"), "yyyyMM");
				l1.add(new long[] { sdate, t1 });
			}
			while (t1 < edate) {
				long t2 = lang.parse((X.toInt(lang.format(t1, "yyyyMM")) + 1) + "", "yyyyMM");
				t2 = lang.parse(lang.format(t2, "yyyyMM"), "yyyyMM");
				if (t2 > edate) {
					t2 = edate;
				}
				l1.add(new long[] { t1, t2 });
				t1 = t2;
			}
			if (t1 < edate) {
				l1.add(new long[] { t1, edate });
			}
		}
		return l1;

	}

	public static List<long[]> split(long sdate, long edate, long step) {

		List<long[]> l1 = new ArrayList<long[]>();
		long t1 = sdate;
		while (t1 < edate) {
			l1.add(new long[] { t1, t1 + step });
			t1 += step;
		}
		if (t1 < edate) {
			l1.add(new long[] { t1, edate });
		}

		return l1;

	}

	/**
	 * 生成矩阵
	 * 
	 * @param s
	 * @return
	 * @throws Exception
	 */
	public static List<Object[]> mat(String s) throws Exception {
		List<Object[]> l1 = new ArrayList<Object[]>();
		CSV e = null;

		try {
			e = CSV.create(s, Arrays.asList(',', ' ', '\t'));

			Object[] o = e.next();
			while (o != null) {
				l1.add(o);
				o = e.next();
			}
		} finally {
			X.close(e);
		}
		return l1;
	}

	@SuppressWarnings("rawtypes")
	public static String toString(Object o) {
		if (o == null) {
			return X.EMPTY;
		} else if (o instanceof Map || o instanceof ScriptObjectMirror) {
			return JSON.fromObject(o).toPrettyString();
		} else if (o instanceof List) {
			StringBuilder sb = new StringBuilder("[");
			List l1 = (List) o;
			for (int i = 0; i < l1.size(); i++) {
				if (i > 0)
					sb.append(",");
				sb.append(toString(l1.get(i)));
			}
			sb.append("]");
			return sb.toString();
		} else if (o.getClass().isArray()) {
			StringBuilder sb = new StringBuilder("[");
			Object[] l1 = (Object[]) o;
			for (int i = 0; i < l1.length; i++) {
				if (i > 0)
					sb.append(",");
				sb.append(toString(l1[i]));
			}
			sb.append("]");
			return sb.toString();
		} else if (o instanceof Throwable) {

			StringWriter sw = new StringWriter();
			PrintWriter out = new PrintWriter(sw);
			((Throwable) o).printStackTrace(out);

			String s = sw.toString();
			int i = s.indexOf("\n");
			if (i > 0) {
				String s1 = s.substring(0, i);
				int i1 = s1.indexOf(":");
				if (i1 > 0) {
					s1 = s1.substring(i1 + 1).trim();
					s = s1 + s.substring(i);
					s = s.replaceAll("\t", X.EMPTY);
				}
			}
			return s;
		} else {
			return o.toString();
		}
	}

	public static byte[] getBytes(Object o) throws Exception {

		if (o == null)
			return null;

		if (!(o instanceof Serializable)) {
			throw new Exception("obj is not Serializable! obj.class=" + o.getClass());
		}

		ByteArrayOutputStream bb = new ByteArrayOutputStream();
		try {
			ObjectOutputStream out = new ObjectOutputStream(bb);
			out.writeObject(o);
		} finally {
			X.close(bb);
		}

//		if (zip) {
//			return Zip.zip(bb.toByteArray());
//		} else {
		return bb.toByteArray();
//		}

	}

	public static void save(Object o, OutputStream out) throws Exception {

		if (o == null)
			return;

		if (!(o instanceof Serializable))
			throw new Exception("obj is not Serializable! obj.class=" + o.getClass());

		try {
			ObjectOutputStream out1 = new ObjectOutputStream(out);
			out1.writeObject(o);
		} finally {
			X.close(out);
		}

	}

	@SuppressWarnings("unchecked")
	public static <T> T fromBytes(byte[] data) throws Exception {

		if (data == null || data.length == 0)
			return null;

		ByteArrayInputStream bb = null;
		try {

//			if (zip) {
//				data = Zip.unzip(data);
//			}
			bb = new ByteArrayInputStream(data);
			ObjectInputStream in = new ObjectInputStream(bb);
			return (T) in.readObject();

		} finally {
			X.close(bb);
		}

	}

	@SuppressWarnings("unchecked")
	public static <T> T load(InputStream in) throws Exception {

		if (in == null)
			return null;

		try {

			ObjectInputStream in1 = new ObjectInputStream(in);
			return (T) in1.readObject();

		} finally {
			X.close(in);
		}

	}

	public static class Image extends GImage {

	}

	public static class IO extends IOUtil {

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T> T clone(T data) {

		if (data == null) {
			return null;
		}

		if (data instanceof Cloneable) {

			T r = null;
			if (data.getClass().isArray()) {
				r = (T) X.asList(data, s -> s);
			} else {
				try {

					Method m = data.getClass().getMethod("clone", (Class<?>[]) null);

					r = (T) m.invoke(data, (Object[]) null);
				} catch (Exception e) {
					log.error(data.getClass() + ", " + data.toString(), e);
					r = data;
				}
			}

			if (r instanceof List) {
				List l1 = (List) r;
				for (int i = l1.size() - 1; i >= 0; i--) {
					l1.set(i, X.clone(l1.get(i)));
				}
			} else if (r instanceof Map) {
				Map m1 = (Map) r;

				for (Object name : m1.keySet()) {
					m1.put(name, X.clone(m1.get(name)));
				}
			}

			if (data.getClass().isArray()) {
				List l1 = (List) r;
				r = (T) l1.toArray();
			}

			return r;
		}

		return data;

	}

	public static boolean matches(String src, String valid) {
		return src.matches(valid);
	}

	public static String toLine(Exception ex, int i) {
		StringWriter sw = new StringWriter();
		PrintWriter out = new PrintWriter(sw);
		((Throwable) ex).printStackTrace(out);

		String[] ss = X.split(sw.toString(), "\n");
		return ss[i];
	}

	public static void lines(String s, BiConsumer<String, BufferedReader> func) {
		IOUtil.lines(s, func);
	}

	public static int compareTo(Object o1, Object o2) {

		if (o1 == null && o2 == null) {
			return 0;
		}

		if (o1 == null) {
			return -1;
		} else if (o2 == null) {
			return 1;
		}

		if (o1 instanceof Byte || o1 instanceof Short || o1 instanceof Integer || o1 instanceof Long) {
			long c1 = X.toLong(o1);
			long c2 = X.toLong(o2);
			return c1 < c2 ? -1 : (c1 == c2) ? 0 : 1;
		} else if (o1 instanceof Float || o1 instanceof Double) {
			double c1 = X.toDouble(o1);
			double c2 = X.toDouble(o2);
			return c1 < c2 ? -1 : (c1 == c2) ? 0 : 1;
		} else if (o1 instanceof String) {
			return o1.toString().compareTo(o2.toString());
		}
		return 0;

	}

	/**
	 * 02:00-04:00, 04:00-02:00
	 * 
	 * @param range
	 * @return
	 */
	public static boolean timeIn(String range) {

//		if (Global.now() - Controller.UPTIME < X.AMINUTE * 10) {
//			return true;
//		}

		if (X.isEmpty(range)) {
			return true;
		}

		String[] rr = X.split(range, "[-]");
		if (rr == null || rr.length != 2) {

			if (log.isInfoEnabled()) {
				log.info("bad range, rr=" + Arrays.toString(rr));
			}

			return false;
		}

		String time = Language.getLanguage().format(Global.now(), "HH:mm");

		String t1 = rr[0];
		String t2 = rr[1];
		if (t1.compareTo(t2) < 0) {
			if (time.compareTo(t1) > 0 && time.compareTo(t2) < 0) {
				return true;
			}
		} else {
			if (time.compareTo(t1) > 0 || time.compareTo(t2) < 0) {
				return true;
			}
		}

		if (log.isDebugEnabled()) {
			log.debug("false, rr=" + Arrays.toString(rr) + ", time=" + time);
		}

		return false;
	}

	public String size(long length) {
		return size(length, 1024, 1000);
	}

	/**
	 * 
	 * @param s, 2g, 300m
	 * @return
	 */
	public long size(String s) {

		if (X.isEmpty(s)) {
			return 0;
		}
		s = s.trim();
		long n = X.toInt(s);

		char c = s.charAt(s.length() - 1);
		if (c == 'K' || c == 'k') {
			return n * 1024;
		} else if (c == 'M' || c == 'm') {
			return n * 1024 * 1000;
		} else if (c == 'G' || c == 'g') {
			return n * 1024 * 1000 * 1000;
		} else if (c == 'T' || c == 't') {
			return n * 1024 * 1000 * 1000 * 1000;
		} else if (c == 'P' || c == 'p') {
			return n * 1024 * 1000 * 1000 * 1000 * 1000;
		}

		return n;

	}

	private static String[] UNITS = new String[] { "", "k", "M", "G", "T", "P" };

	/**
	 * Size.
	 * 
	 * @param length the length
	 * @return the string
	 */
	public String size(long length, int step1, int step2) {

		if (length > 0.00001 && length < 0.00001) {
			return X.EMPTY;
		}

		long step = step1 * step2;

		float d = Math.abs(length);

		int i = 0;
		while (d > step && i < UNITS.length) {
			d /= step1;
			i++;
		}

		if (d > step2 && i < UNITS.length) {
			d /= step2;
			i++;
		}

		float d1 = d - (int) d;
		if (d1 > 0.01) {
			return (length >= 0 ? "" : "-") + X.toLong(d) + UNITS[i];
		} else {
			return (length >= 0 ? "" : "-") + ((int) d) + UNITS[i];
		}
	}

	public static long time(String time) {

		String[] ss = X.split(time, "[-]");
		if (ss.length == 0) {
			return X.AMINUTE;
		}

		long[] tt = new long[ss.length];
		for (int i = 0; i < tt.length; i++) {

			String s = ss[i];
			char c = s.charAt(0);
			if (c == '~' || c == '～') {
				s = s.substring(1);
			}

			long t2 = X.AMINUTE;
			if (s.endsWith("h") || s.endsWith("H")) {
				t2 = X.toInt(s.substring(0, s.length() - 1)) * X.AHOUR;
			} else if (s.endsWith("d") || s.endsWith("D")) {
				t2 = X.toInt(s.substring(0, s.length() - 1)) * X.ADAY;
			} else if (s.endsWith("m")) {
				t2 = X.toInt(s.substring(0, s.length() - 1)) * X.AMINUTE;
			} else if (s.endsWith("M")) {
				t2 = X.toInt(s.substring(0, s.length() - 1)) * X.AMONTH;
			} else if (s.endsWith("w") || s.endsWith("W")) {
				t2 = X.toInt(s.substring(0, s.length() - 1)) * X.AWEEK;
			} else if (s.endsWith("y") || s.endsWith("Y")) {
				t2 = X.toInt(s.substring(0, s.length() - 1)) * X.AYEAR;
			} else if (s.endsWith("s") || s.endsWith("S")) {
				t2 = X.toInt(s.substring(0, s.length() - 1)) * 1000;
			} else if (!X.isEmpty(s)) {
				// micro-seconds
				t2 = X.toInt(s);
				if (t2 < 10) {
					t2 = 10;
				}
			}

			if (c == '~' || c == '～') {
				t2 = X.toLong(t2 + t2 * Math.random()) * 2 / 3;
			}
			tt[i] = t2;
		}

		if (tt.length < 2) {
			return tt[0];
		}

		return X.toLong((tt[1] - tt[0]) * Math.random() + tt[0]);

	}

	public static byte[] zip(byte[] bytes) throws IOException {
		return Zip.zip(bytes);
	}

	public static byte[] unzip(byte[] bb) throws Exception {
		return Zip.unzip(bb);
	}

	public static boolean callBy(String... packages) {
		Exception e = new Exception();
		StackTraceElement[] ss = e.getStackTrace();
		if (ss.length > 1) {
//		for (StackTraceElement s : ss) {
			StackTraceElement s = ss[1];
			for (String pack : packages) {
				if (s.getClassName().indexOf(pack) > -1) {
					return true;
				}
			}
//		}
		}

		return false;
	}

	public static List<?> sort(List<?> l1, List<?> priority) {

		Collections.sort(l1, new Comparator<Object>() {

			@Override
			public int compare(Object o1, Object o2) {
				if (priority.contains(o1)) {
					return -1;
				}
				if (priority.contains(o2)) {
					return 1;
				}
				return X.compareTo(o1, o2);
			}

		});

		return l1;
	}

	public static short toShort(byte[] a) {
		return ByteBuffer.wrap(a).order(ByteOrder.BIG_ENDIAN).getShort();
	}

	public static short toShort2(byte[] a) {
		return ByteBuffer.wrap(a).order(ByteOrder.LITTLE_ENDIAN).getShort();
	}

	/**
	 * format filename
	 * 
	 * @param filename
	 * @return
	 */
	public static String filename(String filename) {
		File f1 = new File(filename);
		String name = f1.getName();
		if (f1.getParent() != null) {
			return f1.getParent() + "/" + name.replaceAll(FILE_NAME_EXCLUDE, "_");
		}
		return name.replaceAll(FILE_NAME_EXCLUDE, "_");
	}

	private static String FILE_NAME_EXCLUDE = "[\\/:*?\"<>|]";

	public static JSON get(List<JSON> l1, String name, Object value) {
		if (l1 == null) {
			return null;
		}
		for (JSON j1 : l1) {
			Object v2 = j1.get(name);
			if (value == v2 || X.isSame(value, v2)) {
				return j1;
			}
		}
		return null;
	}

	public static String getMessage(Throwable e) {
		String sb = e.getMessage();
		if (sb == null) {
			sb = "";
		}
		e = e.getCause();
		while (e != null) {
			if (!sb.contains(e.getMessage())) {
				sb += "//" + e.getMessage();
			}
			e = e.getCause();
		}

		return sb;
	}

}
