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
package org.giiwa.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.giiwa.dao.X;

/**
 * 
 * @author joe
 *
 */
public class StringFinder {

//	private static Log log = LogFactory.getLog(StringFinder.class);

	/**
	 * Creates the.
	 * 
	 * @param s the s
	 * @return the g string
	 */
	public static StringFinder create(String s) {
		StringFinder s1 = new StringFinder(s);
		s1.pair('\'');
		s1.pair('"');
		return s1;
	}

	public StringFinder pair(char ch) {
		_pair.add(new char[] { ch, ch });
		return this;
	}

	public StringFinder pair(char ch1, char ch2) {
		_pair.add(new char[] { ch1, ch2 });
		return this;
	}

	public int pos = 0;
	public int len = 0;
	public String s;
	private String s1;

	private List<char[]> _pair = new ArrayList<char[]>();

	private StringFinder(String s) {
		this.s = s;
		this.s1 = s == null ? null : s.toLowerCase();
		pos = 0;
		len = s == null ? 0 : s.length();
	}

	/**
	 * skip the " " in current position
	 */
	public StringFinder trim() {
		while (hasMore()) {
			char c = next();
			if (c == ' ' || c == '\r' || c == '\n' || c == '\t')
				continue;

			skip(-1);
			break;
		}
		return this;
	}

	/**
	 * find a substring.
	 *
	 * @param sub the sub
	 * @return int the position
	 */
	public int find(String sub) {
		int s1 = s != null ? s.toLowerCase().indexOf(sub.toLowerCase(), pos) : -1;
		if (s1 > -1) {
			pos = s1;
		}
		return s1;
	}

	/**
	 * Next to the deli, and return the substring.
	 *
	 * @param deli the deli
	 * @return the string
	 */
	public String nextTo(String deli) {
		return get(deli);
	}

	public String get(int len) {

		String s1 = null;
		s1 = s.substring(pos, pos + len);
		pos += len;

		return s1.trim();
	}

	public String get(String end) {
		if (s == null || pos >= len) {
			return null;
		}

		String s1 = null;

		int min = Integer.MAX_VALUE;
		String[] ss = end.split("\\|");
		for (String s2 : ss) {
			int i = _pos(s2, pos);
			if (i > -1 && i < min) {
				min = i;
			}

		}
		if (min >= pos && min < len) {
			s1 = s.substring(pos, min);
			pos = min;
		} else {
			s1 = s.substring(pos);
			pos = len;
		}

		return s1.trim();
	}

	private int _pos(String s2, int p) {

		char[] cc = s2.toLowerCase().toCharArray();
		out: for (int i = p; i < len; i++) {

			char c = s1.charAt(i);
			if (c == cc[0]) {
				for (int j = 1; j < cc.length && (i + j < len); j++) {
					if (s1.charAt(i + j) != cc[j]) {
						continue out;
					}
				}
				// found
				return i;

			} else if (c == '\'') {
				//
				i++;
				for (; i < len - cc.length; i++) {
					c = s1.charAt(i);
					if (c == '\'') {
						break;
					}
				}
			} else if (c == '"') {
				//
				i++;
				for (; i < len - cc.length; i++) {
					c = s1.charAt(i);
					if (c == '"') {
						break;
					}
				}
			}
		}

		return -1;

	}

	/**
	 * Get all the remain substring
	 *
	 * @return the string
	 */
	public String remain() {
		if (s == null || pos >= s.length()) {
			return null;
		}
		String s1 = s.substring(pos);
		pos = s.length();
		return s1;
	}

	public boolean hasMore() {
		return (s != null && pos < s.length());
	}

	/**
	 * get the next string between the pair char
	 * 
	 * @param c
	 * @return
	 */
//	public String pair(char c) {
//		StringBuilder sb = new StringBuilder();
//
//		while (hasMore()) {
//			char c1 = next();
//			if (c1 == '\\') {
//				sb.append(next());
//			} else if (c == c1) {
//				break;
//			} else {
//				sb.append(c1);
//			}
//
//		}
//		return sb.toString();
//	}

	// private static String wordchars =
	// "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

	/**
	 * the next char
	 * 
	 * @return the char
	 */
	public char next() {
		if (pos >= s.length())
			return 0;

		return s.charAt(pos++);
	}

	/**
	 * get the char by offset refer the current position.
	 *
	 * @param offset the offset
	 * @return the char
	 */
	public char charOf(int offset) {
		return charAt(pos + offset);
	}

	/**
	 * get the char by the absolute position.
	 *
	 * @param position the position
	 * @return the char
	 */
	public char charAt(int position) {
		if (s != null && position >= 0 && position < s.length()) {
			return s.charAt(position);
		}
		return 0;
	}

	/**
	 * skip a length.
	 *
	 * @param len the len
	 * @return int the new position
	 */
	public int skip(int len) {
		pos += len;
		return pos;
	}

	/**
	 * Bracket.
	 *
	 * @param begin the begin
	 * @param end   the end
	 * @return the string
	 */
	public String bracket(char begin, char end) {
		if (s == null)
			return null;

		int s0 = -1;
		int mark = pos;

		// find the begin
		while (pos < len) {
			char c = s.charAt(pos);
			if (c == begin) {
				pos++;
				s0 = pos;
				break;
			} else if (c == '\\') {
				pos++;
			} else if (c == '"') {
				// skip to next "
				pos++;
				skip('"');
			} else if (c == '\'') {
				// skip to next '
				pos++;
				skip('\'');
			}
			pos++;
		}

		if (s0 > 0) {
			// find the end
			int e0 = -1;
			Stack<Character> ss = new Stack<Character>();

			while (pos < len) {
				char c = s.charAt(pos);
				if (c == end && ss.isEmpty()) {
					e0 = pos;
					pos++;
					break;
				} else if (c == '(' || c == '{' || c == '[') {
					ss.push(c);
				} else if (c == ')' || c == '}' || c == ']') {
					ss.pop();
				} else if (c == '\\') {
					pos++;
				} else if (c == '"') {
					// skip to next "
					pos++;
					skip('"');
				} else if (c == '\'') {
					// skip to next '
					pos++;
					skip('\'');
				}
				pos++;
			}
			if (e0 > 0) {
				return s.substring(s0, e0);
			}
		}

		pos = mark;
		return null;
	}

	public int seek(int pos) {
		this.pos = pos;
		return this.pos;
	}

	/**
	 * Skip.
	 *
	 * @param c0 the c0
	 * @return the int
	 */
	public int skip(char c0) {
		char c = s.charAt(pos);
		while (pos < len) {
			if (c == c0) {
				// found
				break;
			} else if (c == '\\') {
				pos++;
			}
			pos++;
			c = s.charAt(pos);
		}

		return pos;
	}

	/**
	 * check the index of the string
	 * 
	 * @param str the string
	 * @return
	 */
	public int indexOf(String str) {
		return s.indexOf(str, pos);
	}

	/**
	 * skip to the end of the str
	 * 
	 * @param str the string to skip
	 * @return the new position
	 */
	public int skip(String str) {

		int i = s.indexOf(str, pos);
		if (i > -1) {
			pos = i + str.length();
		} else {
			pos = this.len;
		}

		return pos;
	}

	public String get(int begin, int end) {
		return s.substring(begin, end);
	}

	/**
	 * get the sub string begin to end.
	 *
	 * @param begin the begin
	 * @param end   the end
	 * @return String
	 */
	public String get(String begin, String end) {
		if (s == null)
			return null;

		String[] bb = begin.split("\\|");

		int b = this.len;
		for (String s1 : bb) {
			int b1 = s.indexOf(s1, pos);
			if (b1 > -1 && b1 < b) {
				b = b1 + s1.length();
			}
		}

		if (b == this.len)
			return null;

		String[] ee = end.split("\\|");
		int e = this.len;
		for (String s1 : ee) {
			int e1 = s.indexOf(s1, b);
//			log.debug("e1=" + e1 + ", s1=" + s1);
			if (e1 > 0 && e1 < e) {
				e = e1;
			}
		}
		pos = e + end.length();
		return s.substring(b, e);
	}

	public String get(String begin, int size) {
		if (s == null)
			return null;

		String[] bb = begin.split("\\|");

		int b = this.len;
		for (String s1 : bb) {
			int b1 = s.indexOf(s1, pos);
			if (b1 > -1 && b1 < b) {
				b = b1 + s1.length();
			}
		}

		if (b == this.len)
			return null;

		int e = b + size;
		if (e > this.len) {
			e = this.len;
		}
		pos = e;
		return s.substring(b, e);
	}

	/**
	 * Substring.
	 *
	 * @param begin the begin
	 * @param end   the end
	 * @return String
	 */
	public String substring(String begin, String end) {
		return get(begin, end);
	}

	/**
	 * merge two string to as a pattern string.
	 *
	 * @param s1 the s1
	 * @param s2 the s2
	 * @return String the min pattern string
	 */
	public static String merge(String s1, String s2) {
		if (X.isEmpty(s1) || X.isEmpty(s2)) {
			return ".*";
		}

		int l1 = s1.length();
		int l2 = s2.length();
		if (l1 > l2) {
			return merge(s2, s1);
		}

		int j = 0;
		StringBuilder sb = new StringBuilder();
		boolean all = false;
		for (int i = 0; i < l1 && j < l2;) {
			if (s1.charAt(i) == s2.charAt(j)) {
				sb.append(s1.charAt(i));
				i++;
				j++;
				all = false;
			} else {
				if (!all) {
					sb.append(".*");
					all = true;
				}

				if (s1.charAt(i) == '.' && s1.charAt(i + 1) == '*') {
					i += 2;
				}
				if (s2.charAt(j) == '.' && s2.charAt(j + 1) == '*') {
					j += 2;
				}
				String c1 = _getmaxstr(s1.substring(i), s2.substring(j));
				if (c1 == null) {
					i++;
					continue;
				}
				if (i + 2 < l1) {
					String c2 = _getmaxstr(s1.substring(i + 1), s2.substring(j));
					if (c2 == null || c1.length() >= c2.length()) {
						sb.append(c1);
						all = false;
						j = c1.length() + s2.indexOf(c1, j);
						continue;
					} else if (c2 != null) {
						// skip this letter, but leave to next, the next to next
						// may max ?!:)
						i++;
					}
				} else {
					sb.append(c1);
					all = false;
					j = c1.length() + s2.indexOf(c1, j);
					break;
				}

			}
		}
		if ((!all) && j < l2) {
			sb.append(".*");
		}

		return sb.toString();
	}

	private static String _getmaxstr(String s1, String s2) {
		if (s2.indexOf(s1) > -1) {
			return s1;
		}

		String s0 = null;
		for (int i = 1; i < s1.length(); i++) {
			String s = s1.substring(0, i);
			if (s2.indexOf(s) < 0) {
				return s0;
			}
			s0 = s;
		}
		return s0;
	}

	@Override
	public String toString() {
		return s;
	}

	private Stack<Integer> _mark;

	public StringFinder mark() {
		if (_mark == null) {
			_mark = new Stack<Integer>();
		}
		_mark.push(pos);
		return this;
	}

	public StringFinder reset() {
		if (_mark != null && !_mark.isEmpty()) {
			pos = _mark.pop();
		} else {
			pos = 0;
		}
		return this;
	}

	public String[] split(String regex) {
		return X.split(this.remain(), regex);
	}

	public boolean startsWith(String str) {
		return this.s.startsWith(str, pos);
	}

	public StringFinder replace(String s1, String r1) {
		this.s = this.s.replaceFirst(s1, r1);
		this.s1 = this.s.toLowerCase();
		this.pos += r1.length();
		this.len = this.s.length();
		return this;
	}

	public StringFinder replace(int start, int end, String r1) {
		this.s = this.s.substring(0, start) + r1 + this.s.substring(end);
		this.s1 = this.s.toLowerCase();
		this.pos = start + r1.length();
		this.len = this.s.length();
		return this;
	}

	public String word(String regex) {
		String s = "";
		while (this.hasMore()) {
			String s1 = s + this.next();
			if (!s1.matches(regex)) {
				pos--;
				return s;
			}
			s = s1;
		}
		return s;
	}

	public String remove(int start, int end) {
		String r = s.substring(start, end);
		s = s.substring(0, start) + s.substring(end);
		pos = start;
		return r;
	}

	public void add(String str) {
		s = s.substring(0, pos) + str + s.substring(pos);
		pos += str.length();
	}

}
