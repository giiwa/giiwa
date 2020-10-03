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
package org.giiwa.web;

import java.io.File;
import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

import org.apache.commons.logging.*;
import org.giiwa.bean.Repo;
import org.giiwa.conf.Global;
import org.giiwa.dao.X;
import org.giiwa.engine.Velocity;
import org.giiwa.json.JSON;
import org.giiwa.misc.Html;

/**
 * language data which located at /modules/[module]/i18n/
 * 
 * @author yjiang
 * 
 */
public class Language implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	final static Log log = LogFactory.getLog(Language.class);

	/**
	 * locale of language
	 */
	private String locale;

	/**
	 * the language mapping data
	 */
	private Map<String, String[]> data = new HashMap<String, String[]>();

	/**
	 * missed key-value
	 */
	private Map<String, String> missed = new HashMap<String, String>();

	/**
	 * cache the language data withe locale
	 */
	private static Map<String, Language> locales = new HashMap<String, Language>();

	public static Language inst = null;

	public synchronized static Language getLanguage() {

		if (inst == null && Module.home != null)
			inst = getLanguage(Global.getString("language", "zh_cn"));

		return inst;
	}

	public String getLocale() {
		return this.locale;
	}

	/**
	 * Prints the.
	 * 
	 * @param format the format
	 * @param args   the args
	 * @return the string
	 */
	public String print(String format, Object... args) {
		return String.format(format, args);
	}

	/**
	 * Clean.
	 */
	public static void clean() {
		locales.clear();
	}

	/**
	 * Truncate.
	 *
	 * @param s      the s
	 * @param length the length
	 * @return the object
	 */
	public Object truncate(Object s, int length) {
		if (X.isEmpty(s)) {
			return s;
		}

		if (s instanceof String) {
			String s1 = Html.create((String) s).text();
			if (s1.length() > length) {
				return s1.substring(0, length - 3) + "...";
			}
			return s1;
		}
		return s;
	}

	/**
	 * Color.
	 * 
	 * @param d    the d
	 * @param bits the bits
	 * @return the string
	 */
	public String color(long d, int bits) {
		String s = Long.toHexString(d);
		StringBuilder sb = new StringBuilder();
		for (int i = s.length() - 1; i >= 0; i--) {
			sb.append(s.charAt(i));
		}
		if (sb.length() < bits) {
			for (int i = sb.length(); i < bits; i++) {
				sb.append("0");
			}
			return "." + sb.toString();
		} else {
			return "." + sb.substring(0, bits);
		}
	}

	/**
	 * Now.
	 * 
	 * @param format the format
	 * @return the string
	 */
	public String now(String format) {
		return format(System.currentTimeMillis(), format);
	}

	/**
	 * Bitmaps.
	 * 
	 * @param f the f
	 * @return the list
	 */
	public List<Integer> bitmaps(int f) {
		List<Integer> list = new ArrayList<Integer>();
		int m = 1;
		for (int i = 0; i < 32; i++) {
			if ((m & f) > 0) {
				list.add(m);
			}
			m <<= 1;
		}
		return list.size() > 0 ? list : null;
	}

	/**
	 * Bits.
	 * 
	 * @param f the f
	 * @param s the s
	 * @param n the n
	 * @return the int
	 */
	public int bits(int f, int s, int n) {
		f = f >> s;
		return f - (f >> n) << n;
	}

	/**
	 * Gets the language.
	 * 
	 * @param locale the locale
	 * @return the language
	 */
	public static Language getLanguage(String locale) {

		Language l = locales.get(locale);
		if (l == null) {
			l = new Language(locale);
			locales.put(locale, l);
		}
		return l;

	}

	private Language(String locale) {
		this.locale = locale;
		if (Module.home != null && !Module.home.supportLocale(locale)) {
			this.locale = Global.getString("language", "en_us");
		}

		load();
	}

	/**
	 * Checks for.
	 * 
	 * @param name the name
	 * @return true, if successful
	 */
	public boolean has(String name) {
		if (name == null)
			return false;
		return data.containsKey(name);
	}

	/**
	 * get All
	 * 
	 * @return Map
	 */
	public Map<String, String[]> getData() {
		return data;
	}

	/**
	 * get missed keys
	 * 
	 * @return Map
	 */
	public Map<String, String> getMissed() {
		return missed;
	}

	/**
	 * Parses the string with the model using velocity engine
	 *
	 * @param str the string
	 * @param m   the model
	 * @return the string
	 */
	public String parse(String str, Controller m) {

		try {
			str = Velocity.parse(str, m.data);
		} catch (Exception e) {
			log.error(str, e);
		}
		return str;
	}

	/**
	 * Gets the.
	 * 
	 * @param name the name
	 * @return the string
	 */
	public String get(String name) {
		return get(name, name);
	}

	public String random(String name) {
		String s = Global.getString(name, "");
		String[] ss = X.split(s, "[\\$\\$]");
		if (ss != null && ss.length > 0) {
			return ss[(int) (Math.random() * ss.length)];
		}
		return name;
	}

	public String get(String name, String defaultString) {
		if (X.isEmpty(name)) {
			return defaultString;
		}

		if (data.containsKey(name)) {
			return data.get(name)[0];
		} else if (missed.containsKey(name)) {
			return missed.get(name);
		} else {
			if (name.startsWith("$,")) {
				return name.substring(2);
			}

			// if (name.indexOf("$") > -1) {
			// return null;
			// }

			missed.put(name, defaultString);
			return defaultString;
		}
	}

	/**
	 * Load.
	 */
	public void load() {
		data = new HashMap<String, String[]>();
		if (Module.home != null) {
			Module.home.loadLang(data, locale);
		}

		if (data.isEmpty()) {
			// log.error("doesnt support the locale: " + locale);
		}
	}

	private static Map<String, SimpleDateFormat> formats = new HashMap<String, SimpleDateFormat>();

	/**
	 * Format.
	 * 
	 * @param t      the t
	 * @param format the format
	 * @return the string
	 */
	public String format(String t, String format) {
		try {
			SimpleDateFormat sdf = formats.get(format);
			if (sdf == null) {
				sdf = new SimpleDateFormat(format);

				// if (data.containsKey("date.timezone")) {
				// sdf.setTimeZone(TimeZone.getTimeZone(get("date.timezone")));
				// }

				formats.put(format, sdf);
			}

			// parse t to datetime, assume the datetime is
			// "YYYY ? MM ? dd hh:mm:ss"

			synchronized (sdf) {
				return sdf.format(new Date(parsetime(t)));
			}
		} catch (Exception e) {
			log.error(t, e);
		}
		return t;
	}

	public long parsetime(String date) {
		if (X.isEmpty(date)) {
			return 0;
		}

		String[] ss = date.split("[-/_:H ]");

		if (ss.length < 3) {
			return 0;
		}
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(0);
		int[] d = new int[ss.length];
		for (int i = 0; i < d.length; i++) {
			d[i] = X.toInt(ss[i]);
		}

		if (d[0] > 100) {
			c.set(Calendar.YEAR, d[0]);
			if (d[1] > 12) {
				c.set(Calendar.MONTH, d[2] - 1);
				c.set(Calendar.DATE, d[1]);
			} else {
				c.set(Calendar.MONTH, d[1] - 1);
				c.set(Calendar.DATE, d[2]);
			}
		} else if (d[2] > 100) {
			c.set(Calendar.YEAR, d[2]);

			if (d[0] > 12) {
				c.set(Calendar.MONTH, d[1] - 1);
				c.set(Calendar.DATE, d[0]);
			} else {
				c.set(Calendar.MONTH, d[0] - 1);
				c.set(Calendar.DATE, d[1]);
			}
		}

		if (d.length > 3) {
			c.set(Calendar.HOUR, d[3]);
		}
		if (d.length > 4) {
			c.set(Calendar.MINUTE, d[4]);
		}
		if (d.length > 5) {
			c.set(Calendar.SECOND, d[5]);
		}

		return c.getTimeInMillis();
	}

	/**
	 * Format.
	 * 
	 * @param t      the t
	 * @param format the format
	 * @return the string
	 */
	public String format(long t, String format) {
		// if (t <= X.ADAY)
		// return X.EMPTY;

		try {
			SimpleDateFormat sdf = formats.get(format);
			if (sdf == null) {
				sdf = new SimpleDateFormat(format);

				// if (data.containsKey("date.timezone")) {
				// sdf.setTimeZone(TimeZone.getTimeZone(get("date.timezone")));
				// }

				formats.put(format, sdf);
			}
			synchronized (sdf) {
				return sdf.format(new Date(t));
			}
		} catch (Exception e) {
			log.error(t, e);
			e.printStackTrace();
		}
		return Long.toString(t);
	}

	public String percent(double t, String format) {
		return String.format(format, t * 100);
	}

	/**
	 * Convert.
	 * 
	 * @param date   the date
	 * @param from   the from
	 * @param format the format
	 * @return the string
	 */
	public String convert(int date, String from, String format) {
		if (date == 0)
			return X.EMPTY;

		long t = parse(Integer.toString(date), from);
		if (t == 0)
			return X.EMPTY;

		return format(t, format);
	}

	/**
	 * Convert.
	 * 
	 * @param date   the date
	 * @param from   the from
	 * @param format the format
	 * @return the string
	 */
	public String convert(String date, String from, String format) {
		if (date == null || date.length() < 8) {
			return date;
		}

		long t = parse(date, from);
		if (t == 0)
			return X.EMPTY;

		return format(t, format);
	}

	/**
	 * Parses the.
	 * 
	 * @param t      the t
	 * @param format the format
	 * @return the long
	 */
	public long parse(String t, String format) {
		if (t == null || "".equals(t))
			return 0;

		try {
			SimpleDateFormat sdf = formats.get(format);
			if (sdf == null) {
				sdf = new SimpleDateFormat(format);
				// sdf.setTimeZone(TimeZone.getTimeZone(get("date.timezone")));
				formats.put(format, sdf);
			}
			synchronized (sdf) {
				return sdf.parse(t).getTime();
			}
		} catch (Exception e) {
			log.error(t + ", format=" + format, e);
		}

		return 0;
	}

	public long parse(String t, String format, Locale loc) {
		if (t == null || "".equals(t))
			return 0;

		try {
			SimpleDateFormat sdf = new SimpleDateFormat(format, loc);
			// sdf.setTimeZone(TimeZone.getTimeZone(get("date.timezone")));
			synchronized (sdf) {
				return sdf.parse(t).getTime();
			}
		} catch (Exception e) {
			log.error(t + ", format=" + format, e);
		}

		return 0;
	}

	/**
	 * Format.
	 * 
	 * @param t the t
	 * @return the string
	 */
	public String format(long t) {
		return format(t, get("date.format"));
	}

	public String full(int n, int len) {
		String s = Integer.toString(n);
		while (s.length() < len) {
			s = "0" + s;
		}
		return s;
	}

	public String size(long length) {
		return size(length, 1024);
	}

	private static String[] UNITS = new String[] { "", "k", "M", "G", "T", "P" };

	/**
	 * Size.
	 * 
	 * @param length the length
	 * @return the string
	 */
	public String size(long length, int step) {

		if (length > 0.00001 && length < 0.00001) {
			return X.EMPTY;
		}

		float d = Math.abs(length);

		int i = 0;
		while (d > step && i < UNITS.length) {
			d /= step;
			i++;
		}

		float d1 = d - (int) d;
		if (d1 > 0.01) {
			return (length >= 0 ? "" : "-") + String.format("%.1f", d) + UNITS[i];
		} else {
			return (length >= 0 ? "" : "-") + ((int) d) + UNITS[i];
		}
	}

	/**
	 * Past.
	 * 
	 * @param base the base
	 * @return the string
	 */
	public String past(long base) {
		if (base <= 0) {
			return X.EMPTY;
		}

		int t = (int) ((System.currentTimeMillis() - base) / 1000);
		if (t < 60) {
			return t + get("past.s");
		}

		t /= 60;
		if (t < 60) {
			return t + get("past.m");
		}

		t /= 60;
		if (t < 24) {
			return t + get("past.h");
		}

		t /= 24;
		if (t < 30) {
			return t + get("past.d");
		}

		int t1 = t / 30;
		if (t < 365) {
			return t1 + get("past.M");
		}

		t1 = t / 365;
		return t1 + get("past.y");
	}

	public long time(long time, String m) {
		return time(time, 8, m);
	}

	public long time(long time, int timeoffset, String m) {
		time += timeoffset * X.AHOUR;
		if ("ms".equals(m)) {
			return time;
		} else if ("s".equals(m)) {
			return time / 1000 * 1000;
		} else if ("m".equals(m)) {
			return time / X.AMINUTE * X.AMINUTE;
		} else if ("h".equals(m)) {
			return time / X.AHOUR * X.AHOUR;
		} else if ("d".equals(m)) {
			return time / X.ADAY * X.ADAY;
		} else if ("M".equals(m)) {
			return time / X.AMONTH * X.AMONTH;
		} else if ("y".equalsIgnoreCase(m)) {
			return time / X.AYEAR * X.AYEAR;
		}
		return time;
	}

	public String time(long duration) {
		if (duration <= 0) {
			return X.EMPTY;
		}

		int i = 0;
		StringBuilder sb = new StringBuilder();
		if (duration > X.ADAY) {
			sb.append(duration / X.ADAY).append(get("time.d"));
			duration %= X.ADAY;
			i++;
		}
		if (duration > X.AHOUR) {
			sb.append(duration / X.AHOUR).append(get("time.h"));
			duration %= X.AHOUR;
			i++;
		}
		if (duration > X.AMINUTE && i < 2) {
			sb.append(duration / X.AMINUTE).append(get("time.m"));
			duration %= X.AMINUTE;
			i++;
		}
		if (duration > 1000 && i < 2) {
			sb.append(duration / 1000).append(get("time.s"));
			duration %= 1000;
			i++;
		}
		if (duration > 0 && i < 1) {
			sb.append(duration).append(get("time.ms"));
			i++;
		}
		return sb.toString();
	}

	public long pastms(long t) {
		return System.currentTimeMillis() - t;
	}

	/**
	 * Parses the.
	 * 
	 * @param body the body
	 * @return the html
	 */
	public Html parse(String body) {
		return Html.create(body);
	}

	public Object html(Object str) {
		if (str == null)
			return null;
		if (str instanceof String) {
			return ((String) str).replaceAll("<", "&lt;").replaceAll(">", "&gt;");
		}
		return str;
	}

	public String cost(float cost) {
		if (cost < 0.1) {
			return ((int) (cost * 100000)) / 100f + get("label.ms");
		} else {
			return cost + get("label.ms");
		}
	}

	public String icon(String file) {
		if (X.isEmpty(file)) {
			return "icon-file-empty";
		}

		String mime = Controller.getMimeType(file);
		if (log.isDebugEnabled())
			log.debug("mime=" + mime + ", file=" + file);

		// image/png
		String icon = get(mime);
		if (mime != null && (X.isEmpty(icon) || X.isSame(icon, mime))) {
			if (mime.startsWith("audio/")) {
				icon = "icon-music";
			} else if (mime.startsWith("image/")) {
				icon = "icon-image";
			} else if (mime.startsWith("text/")) {
				icon = "icon-file-text2";
			} else if (mime.contains("officedocument")) {
				icon = "icon-file-word";
			} else {
				icon = "icon-file-text2";
			}
		} else if (X.isEmpty(mime)) {
			if (file.contains(".gsp")) {
				icon = "icon-file-video";
			}
		}

		if (X.isEmpty(icon)) {
			icon = "icon-file-empty";
		}
		return icon;
	}

	public boolean isImage(String file) {
		String mime = Controller.getMimeType(file);
		if (log.isDebugEnabled())
			log.debug("mime=" + mime);

		return mime != null && mime.startsWith("image/");
	}

	public Repo.Entity repo(String repo) {
		return Repo.load(repo);
	}

	public String eclipse(String path) {
		return path.replaceAll("\\\\", "/");
	}

	public synchronized String theme(int min, String name) {

		if (themeplugin != null) {
			String s = themeplugin.apply(name);
			if (!X.isEmpty(s)) {
				return s;
			}
		}

		if (theme == null) {
			theme = JSON.create();
		}
		if (theme.get(name) == null) {
			theme.append(name, JSON.create());
		}
		JSON j = (JSON) theme.get(name);
		if (System.currentTimeMillis() - j.getLong("created") > min * X.AMINUTE) {
			File f = Module.home.getFile("/images/theme/");
			if (f != null) {
				String[] ss = f.list();
				if (ss != null && ss.length > 0) {
					j.append("created", System.currentTimeMillis()).append("image",
							"/images/theme/" + ss[(int) (ss.length * Math.random())]);
				}
			}
		}

		if (log.isDebugEnabled())
			log.debug("theme=" + j);

		return j.getString("image");
	}

	private JSON theme = null;
	private Function<String, String> themeplugin;

	public void setTheme(Function<String, String> m) {
		themeplugin = m;
	}

	public long parse(Timestamp t) {
		return t.getTime();
	}

}
