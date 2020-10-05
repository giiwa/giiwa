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

import java.io.*;
import java.net.*;
import java.util.*;

import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.misc.Url;

/**
 * 
 * @author yjiang
 * 
 */
public class QueryString implements Cloneable {

	TreeMap<String, String> query = new TreeMap<String, String>();

	String q;
	String path;

	/**
	 * Size.
	 * 
	 * @return the int
	 */
	public int size() {
		return query.size();
	}

	/**
	 * Query.
	 * 
	 * @return the string
	 */
	public String query() {
		StringBuilder sb = new StringBuilder();

		for (Iterator<String> it = query.keySet().iterator(); it.hasNext();) {
			String k = it.next();
			String s = query.get(k);
			if (k != null && s != null && s.length() > 0 && k.length() > 0) {
				if (sb.length() > 0)
					sb.append("&");
				try {
					sb.append(k).append("=").append(URLEncoder.encode(s, "utf-8"));
				} catch (UnsupportedEncodingException e) {
				}
			}
		}

		return sb.toString();
	}

	/**
	 * Values.
	 * 
	 * @param k the k
	 * @return the list
	 */
	public List<String> values(String k) {
		String s = query.get(k);
		if (s != null) {
			String[] ss = s.split("_");
			List<String> list = new ArrayList<String>(ss.length);
			for (String s1 : ss) {
				list.add(s1);
			}
			return list;
		}
		return null;
	}

	/**
	 * Dec.
	 * 
	 * @param o the o
	 * @param n the n
	 * @return the query string
	 */
	public QueryString dec(String o, String n) {
		int i1 = getInt(o);
		int i2 = getInt(n);
		set(o, i1 - i2);

		q = null;
		return this;
	}

	/**
	 * Checks for.
	 * 
	 * @param o the o
	 * @param n the n
	 * @return true, if successful
	 */
	public boolean has(String o, String n) {
		String s = get(o);
		if (s == null) {
			return false;
		} else {
			if (n.indexOf(":") > -1) {
				String[] nn = n.split(":");
				String[] ss = s.split("_");
				for (String s1 : ss) {
					String[] ss1 = s1.split(":");
					if (ss1[0].equals(nn[0])) {
						return true;
					}
				}
			} else {
				String[] ss = s.split("_");
				for (String s1 : ss) {
					if (s1.equals(n)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * Link.
	 * 
	 * @param o the o
	 * @param n the n
	 * @return the query string
	 */
	public QueryString link(String o, String n) {
		String s = get(o);
		if (s == null) {
			set(o, n);
		} else {
			if (n.indexOf(":") > 0) {
				String[] nn = n.split(":");
				String[] ss = s.split("_");
				StringBuilder sb = new StringBuilder();
				boolean found = false;
				for (String s1 : ss) {
					String[] ss1 = s1.split(":");
					if (ss1[0].equals(nn[0])) {
						found = true;
					} else {
						if (sb.length() > 0)
							sb.append("_");
						sb.append(s1);
					}
				}
				if (!found)
					sb.append("_").append(n);

				set(o, sb.toString());
			} else {
				String[] ss = s.split("_");
				StringBuilder sb = new StringBuilder();
				boolean found = false;
				for (String s1 : ss) {
					if (s1.equals(n)) {
						found = true;
					} else {
						if (sb.length() > 0)
							sb.append("_");
						sb.append(s1);
					}
				}
				if (!found)
					sb.append("_").append(n);

				set(o, sb.toString());
			}
		}

		q = null;
		return this;
	}

	/**
	 * Adds the.
	 * 
	 * @param o the o
	 * @param n the n
	 * @return the query string
	 */
	public QueryString add(String o, String n) {
		int i1 = getInt(o);
		int i2 = getInt(n);
		set(o, i1 + i2);

		q = null;
		return this;
	}

	public String getPath() {
		return path;
	}

	/**
	 * Append.
	 * 
	 * @param k the k
	 * @param s the s
	 * @return the query string
	 */
	public QueryString append(String k, String s) {
		String s1 = query.get(k);

		if (s1 == null) {
			query.put(k, s);
		} else {
			String[] ss = s1.split("[ ,;'\"]");
			for (String s2 : ss) {
				if (s2.equals(s)) {
					return this;
				}
			}
			s1 += " " + s;
			query.put(k, s1);
		}

		q = null;
		return this;
	}

	/**
	 * Instantiates a new query string.
	 * 
	 * @param path the path
	 */
	public QueryString(String path) {
		q = null;

		int j = path.lastIndexOf("#");
		if (j > 0) {
			path = path.substring(0, j);
		}

		int i = path.indexOf("?");
		if (i > 0) {
			this.path = path.substring(0, i);
			String s = path.substring(i + 1);

			this.append(s);
			return;
		}

		i = path.indexOf("&");
		if (i > 0) {
			this.path = path.substring(0, i);
			String s = path.substring(i + 1);

			this.append(s);
			return;
		}

		this.path = path;
	}

	public void append(String s) {
		String[] ss = X.split(s, "[&?]");
		if (ss != null) {
			for (String s1 : ss) {
				int j = s1.indexOf("=");
				if (j > 0) {
					this.append(s1.substring(0, j), s1.substring(j + 1));
				}
			}
		}
	}

	/**
	 * Copy.
	 * 
	 * @param jo    the jo
	 * @param names the names
	 * @return the query string
	 */
	public QueryString copy(JSON jo, String... names) {
		if (jo != null) {
			if (names != null && names.length > 0) {
				for (String name : names) {
					if (jo.has(name)) {
						this.set(name, jo.getString(name));
					}
				}
			} else {
				for (Object name : jo.keySet()) {
					this.set((String) name, jo.getString((String) name));
				}
			}
		}

		q = null;
		return this;
	}

	/**
	 * Path.
	 * 
	 * @param path the path
	 * @return the query string
	 */
	public QueryString path(String path) {
		this.path = path;
		this.q = null;

		return this;
	}

	/**
	 * Copy.
	 * 
	 * @return the query string
	 */
	public QueryString copy() {
		try {
			QueryString q = (QueryString) super.clone();
			q.query = new TreeMap<String, String>();
			q.q = null;

			for (Iterator<String> it = query.keySet().iterator(); it.hasNext();) {
				String s = it.next();
				q.query.put(s, query.get(s));
			}

			return q;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Copy.
	 * 
	 * @param m the m
	 * @return the query string
	 */
	public QueryString copy(Controller m) {
		Enumeration<String> it = m.req.getParameterNames();
		if (it != null) {
			while (it.hasMoreElements()) {
				String name = it.nextElement();
				this.set(name, m.getString(name));
			}
			q = null;
		}
		return this;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object.toString()
	 */
	public String toString() {

		if (q == null) {
			StringBuilder sb = new StringBuilder();

			for (Iterator<String> it = query.keySet().iterator(); it.hasNext();) {
				String k = it.next();
				String s = query.get(k);
				if (k != null && s != null && s.length() > 0 && k.length() > 0) {
					if (sb.length() > 0)
						sb.append("&");
					try {
						sb.append(k).append("=").append(URLEncoder.encode(s, "utf-8"));
					} catch (UnsupportedEncodingException e) {
					}
				}
			}

			if (path.endsWith("/index")) {
				path = path.substring(0, path.length() - 5);
			}

			path = _format(path);

			if (sb.length() > 0) {
				q = path + "?" + sb.toString();
			} else {
				q = path;
			}
		}

		return q;
	}

	private String _format(String path) {

		StringBuilder sb = new StringBuilder();
		int i = path.indexOf("/", 10);
		while (i > 0) {
			String s = path.substring(0, i);
			if (sb.length() > 0) {
				s = Url.encode(s);
			}
			sb.append(s).append("/");
			path = path.substring(i + 1);
			i = path.indexOf("/");
		}

		String s = path;
		if (!X.isEmpty(s)) {
			if (sb.length() > 0) {
				s = Url.encode(s);
			}
			sb.append(s);
		}

		return sb.toString();

	}

	/**
	 * Sets the.
	 * 
	 * @param jo the jo
	 * @return the query string
	 */
	public QueryString set(JSON jo) {
		for (Object name : jo.keySet()) {
			Object v = jo.get(name);

			set((String) name, (String) v);
		}

		q = null;
		return this;
	}

	/**
	 * Sets the.
	 * 
	 * @param k the k
	 * @param s the s
	 * @return the query string
	 */
	public QueryString set(String k, String s) {
		if (k != null && s != null) {
			k = k.trim();
			s = s.trim();
			if (k.length() > 0 && s.length() > 0) {
				query.put(k, s);
			} else {
				query.remove(k);
			}
		} else if (s == null) {
			query.remove(k);
		}

		q = null;
		return this;
	}

	/**
	 * Sets the.
	 * 
	 * @param k the k
	 * @param s the s
	 * @return the query string
	 */
	public QueryString set(String k, int s) {
		return set(k, Integer.toString(s));
	}

	/**
	 * Removes the.
	 * 
	 * @param k the k
	 * @return the query string
	 */
	public QueryString remove(String... k) {
		for (String s1 : k) {
			query.remove(s1);

			q = null;
		}
		return this;
	}

	/**
	 * Gets the.
	 * 
	 * @param k the k
	 * @return the string
	 */
	public String get(String k) {
		return query.get(k);
	}

	/**
	 * Encode.
	 * 
	 * @param k the k
	 * @return the string
	 */
	public String encode(String k) {
		String s = query.get(k);
		try {
			if (s != null) {
				return URLEncoder.encode(s, "utf8");
			}
		} catch (Exception e) {
		}

		return null;
	}

	/**
	 * Checks if is.
	 * 
	 * @param k     the k
	 * @param value the value
	 * @return true, if successful
	 */
	public boolean is(String k, String value) {
		String v = get(k);

		return v == null && value == null || v != null && v.equals(value);
	}

	/**
	 * Gets the.
	 * 
	 * @param k            the k
	 * @param defaultValue the default value
	 * @return the string
	 */
	public String get(String k, String defaultValue) {
		String v = get(k);
		if (v == null) {
			return defaultValue;
		} else {
			return v;
		}
	}

	/**
	 * Gets the int.
	 * 
	 * @param k the k
	 * @return the int
	 */
	public int getInt(String k) {
		String s = get(k);

		if (s != null) {
			try {
				return Integer.parseInt(s);
			} catch (Exception e) {

			}
		}

		return 0;
	}

	/**
	 * Gets the long.
	 * 
	 * @param k the k
	 * @return the long
	 */
	public long getLong(String k) {
		String s = get(k);

		if (s != null) {
			try {
				return Long.parseLong(s);
			} catch (Exception e) {

			}
		}

		return 0;
	}

	/**
	 * Gets the float.
	 * 
	 * @param k the k
	 * @return the float
	 */
	public float getFloat(String k) {
		String s = get(k);

		if (s != null) {
			try {
				return Float.parseFloat(s);
			} catch (Exception e) {

			}
		}

		return 0;
	}

	/**
	 * Gets the double.
	 * 
	 * @param k the k
	 * @return the double
	 */
	public double getDouble(String k) {
		String s = get(k);

		if (s != null) {
			try {
				return Double.parseDouble(s);
			} catch (Exception e) {

			}
		}

		return 0;
	}

	/**
	 * Parses the.
	 * 
	 * @param s the s
	 */
	public void parse(String s) {
		try {
			s = URLDecoder.decode(s, "utf-8");
			String[] ss = s.split("&");
			for (String s1 : ss) {
				int i = s1.indexOf("=");
				if (i > -1) {
					String n = s1.substring(0, i);
					String v = s1.substring(i + 1);
					set(n, v);
				}
			}
		} catch (Exception e) {
			// eat it
		}
	}

	/**
	 * Clean.
	 * 
	 * @return the query string
	 */
	public QueryString clean() {
		query.clear();
		q = null;
		return this;
	}

	public static void main(String[] args) {

		String url = "https://www.aa.com/?a=1a=2";
		QueryString s1 = new QueryString(url);
		System.out.println(s1.toString());

	}

}
