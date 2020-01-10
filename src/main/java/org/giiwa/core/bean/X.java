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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.framework.web.Language;

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

	public static final int ITEMS_PER_PAGE = 10;

	public static X inst = new X();

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

		if (X.isEmpty(s1) && X.isEmpty(s2))
			return true;

		if (X.isEmpty(s1) || X.isEmpty(s2)) {
			return false;
		}

		if (s1 instanceof String && s2 instanceof String) {
			return ((String) s1).equalsIgnoreCase((String) s2);
		}

		if (s1.getClass().isArray() && s2.getClass().isArray()) {
			s1 = Arrays.asList((Object[]) (s1));
			s2 = Arrays.asList((Object[]) s2);
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

		if (s1 != null) {
			return s1.equals(s2);
		}

		return false;
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
			if (v instanceof Number) {
				return ((Number) v).longValue();
			}

			String s = v.toString().replaceAll(",", "").trim();
			StringBuilder sb = new StringBuilder();
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
			if (v instanceof Number) {
				return ((Number) v).intValue();
			}

			String s = v.toString().replaceAll(",", "").trim();
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
	 * test the object is empty? null , empty string, empty collection, empty map.
	 *
	 * @param s the object, may string, list, map
	 * @return boolean, return true if null, or empty
	 */
	@SuppressWarnings("rawtypes")
	public static boolean isEmpty(Object s) {
		if (s == null) {
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

	/**
	 * safely parse a object to a float, if failed return default value.
	 * 
	 * @param v            the v
	 * @param defaultValue the default value
	 * @return float
	 */
	public static float toFloat(Object v, float defaultValue) {
		if (v != null) {
			if (v instanceof Number) {
				return ((Number) v).floatValue();
			}

			String s = v.toString().replaceAll(",", "").trim();

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

	private static final char[][] DIGS = { "０１２３４５６７８９".toCharArray(), "零一二三四五六七八九".toCharArray(),
			"零壹贰叁肆伍陆柒捌玖".toCharArray() };

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
				return ((Number) v).doubleValue();
			}

			String s = v.toString().replaceAll(",", "").trim();
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
	 * safely parse the object to long, if failed return 0.
	 *
	 * @param v the object
	 * @return the long
	 */
	public static long toLong(Object v) {
		return toLong(v, 0);
	}

	/**
	 * split the src string by the regex, and filter the empty.
	 *
	 * @param src   the source string
	 * @param regex the split regex
	 * @return the String
	 */
	public static String[] split(String src, String regex) {
		List<String> l1 = new ArrayList<String>();
		if (src != null) {
			String[] ss = src.split(regex);
			for (String s : ss) {
				s = s.trim();
				if (!X.isEmpty(s)) {
					l1.add(s);
				}
			}
		}

		return l1.toArray(new String[l1.size()]);
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

				String p3 = p1;
				while (p3.compareTo(p2) <= 0) {
					if (!l1.contains(prefix + p3)) {
						l1.add(prefix + p3);
					}
					p3 = X.add(p3, 1);
				}
			}
		}
		return l1.toArray(new String[l1.size()]);
	}

	public static Object[] csv(String src) {
		if (X.isEmpty(src))
			return null;
		src = src.trim();
		if (src.length() == 0)
			return null;

		List<Object> l1 = new ArrayList<Object>();

		StringBuilder sb = null;
		int p = 0;
		int len = src.length();
		while (p < len) {
			char c = src.charAt(p);
			if (c == '"') {
				// goto next "
				p++;
				c = src.charAt(p);
				if (sb == null) {
					sb = new StringBuilder();
				}

				while (c != '"' && p < len) {
					sb.append(c);
					if (c == '\\') {
						sb.append(src.charAt(++p));
					}
					p++;
					c = p == len ? '"' : src.charAt(p);
				}
			} else if (c == ',') {
				if (sb == null || sb.length() == 0) {
					l1.add(X.EMPTY);
				} else {
					l1.add(_parse(sb.toString()));
				}
				sb = null;
			} else {
				if (sb == null) {
					sb = new StringBuilder();
				}
				sb.append(c);
			}
			p++;
		}
		if (sb != null) {
			l1.add(_parse(sb.toString()));
		}

		return l1.toArray();
	}

	private static Object _parse(String s) {
		boolean dot = false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '.') {
				dot = true;
			} else if (c >= '0' && c <= '9') {
				continue;
			} else if ((c == '+' || c == '-') && i == 0) {
				continue;
			} else {
				return s;
			}
		}
		if (dot) {
			return X.toDouble(s);
		} else {
			return X.toLong(s);
		}
	}

	/**
	 * printstacktrace
	 * 
	 * @param e the throwable
	 * @return the string
	 */
	public static String toString(Throwable e) {
		if (e == null)
			return X.EMPTY;
		StringWriter sw = new StringWriter();
		PrintWriter out = new PrintWriter(sw);
		e.printStackTrace(out);
		return sw.toString();
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
		for (String s : s2) {
			if (X.isSame(s1, s)) {
				return true;
			}
		}
		return false;
	}

	public static List<Long> toLong(List<?> l1) {
		if (l1 == null || l1.isEmpty())
			return null;

		List<Long> l2 = new ArrayList<Long>(l1.size());
		for (Object e : l1) {
			long i = X.toLong(e);
			l2.add(i);
		}
		return l2;
	}

	public static List<String> toString(List<?> l1) {
		if (l1 == null || l1.isEmpty())
			return null;

		List<String> l2 = new ArrayList<String>(l1.size());
		for (Object e : l1) {
			if (e != null) {
				l2.add(e.toString());
			}
		}
		return l2;
	}

	public static String toString(List<?> l1, String deli) {
		if (l1 == null || l1.isEmpty())
			return X.EMPTY;

		StringBuilder sb = new StringBuilder();
		for (Object e : l1) {
			if (e != null) {
				if (sb.length() == 0)
					sb.append(deli);

				sb.append(e);
			}
		}
		return sb.toString();
	}

	public static List<Integer> toInt(List<?> l1) {
		if (l1 == null || l1.isEmpty())
			return null;

		List<Integer> l2 = new ArrayList<Integer>(l1.size());
		for (Object e : l1) {
			int i = X.toInt(e);
			l2.add(i);
		}
		return l2;
	}

	public static <T, E> List<T> toArray(List<E> l1, Function<E, T> cb) {
		if (l1 == null) {
			return new ArrayList<T>();
		}
		List<T> l2 = new ArrayList<T>(l1.size());
		for (E e : l1) {
			T t = cb.apply(e);
			if (t != null) {
				l2.add(t);
			}
		}
		return l2;
	}

	public static <T, E> List<T> toArray(E[] l1, Function<E, T> cb) {
		if (l1 == null) {
			return new ArrayList<T>();
		}

		List<T> l2 = new ArrayList<T>(l1.length);
		for (E e : l1) {
			T t = cb.apply(e);
			if (t != null) {
				l2.add(t);
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

	public static boolean isCauseBy(Throwable e, String regex) {
		if (e == null)
			return false;

		String s = e.getMessage();
		if (!X.isEmpty(s)) {
			s = s.replaceAll("\r", X.EMPTY).replaceAll("\n", X.EMPTY);
			if (log.isDebugEnabled())
				log.debug("s=" + s + ", matches?" + s.matches(regex));
//			System.out.println("s=" + s + ", matches?" + s.matches(regex));

			if (s.matches(regex))
				return true;
		}

		return isCauseBy(e.getCause(), regex);
	}

	public static String join(List<?> l1, String deli) {
		if (X.isEmpty(l1))
			return X.EMPTY;

		StringBuilder sb = new StringBuilder();
		for (Object o : l1) {
			if (sb.length() > 0)
				sb.append(deli);
			sb.append(X.isEmpty(o) ? X.EMPTY : o);
		}
		return sb.toString();
	}

	public static void main(String[] args) {
		String s = "-1,+2,c,d,e,\"a\"";
		System.out.println(Arrays.toString(X.csv(s)));

		s = "K1/117, K299;199;P1-093/-097";
		System.out.println(Arrays.toString(X.range(s, "/")));

		String filename = "/Users/joe/Downloads/cate.csv";
		try {
			BufferedReader re = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
			String line = re.readLine();
			while (line != null) {
				Object[] ss = X.csv(line);
				System.out.println(Arrays.toString(ss) + "-" + ss.length);
				line = re.readLine();
			}
			X.close(re);
		} catch (Exception e) {
			e.printStackTrace();
		}

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

}
