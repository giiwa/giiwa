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

import org.giiwa.dao.Comment;
import org.giiwa.dao.X;

/**
 * 
 * @author joe
 *
 */
@Comment(text = "字符串工具")
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

	@Comment(hide = true)
	public StringFinder pair(char ch) {
		_pair.add(new char[] { ch, ch });
		return this;
	}

	@Comment(hide = true)
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
	@Comment(text = "剪取空, \r, \n, \t")
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
	@Comment(text = "从当前位置开始查找子串，找到后移动到新位置并返回，否则-1")
	public int find(@Comment(text = "substring") String sub) {
		int s1 = s != null ? s.toLowerCase().indexOf(sub.toLowerCase(), pos) : -1;
		if (s1 > -1) {
			pos = s1;
		}
		return s1;
	}

	public int backfind(@Comment(text = "substring") String sub) {
		int s1 = s != null ? s.toLowerCase().lastIndexOf(sub.toLowerCase(), pos) : -1;
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
	@Comment(text = "返回当前位置到下一个字串中间的字符串")
	public String nextTo(@Comment(text = "deli") String deli) {
		return get(deli);
	}

	@Comment(text = "返回当前位置开始长度为len的字符串")
	public String get(@Comment(text = "len") int len) {

		String s1 = null;
		s1 = s.substring(pos, pos + len);
		pos += len;

		return s1.trim();
	}

	@Comment(text = "返回当前位置到end子串中间的字符串，字串是｜分隔的多个子串")
	public String get(@Comment(text = "end") String end) {
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
	@Comment(text = "返回当前位置剩余的所有字串")
	public String remain() {
		if (s == null || pos >= s.length()) {
			return null;
		}
		String s1 = s.substring(pos);
		pos = s.length();
		return s1;
	}

	@Comment(text = "是否还有字符")
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
	@Comment(text = "下一个字符")
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
	@Comment(text = "下offset位置的字符")
	public char charOf(@Comment(text = "offset") int offset) {
		return charAt(pos + offset);
	}

	/**
	 * get the char by the absolute position.
	 *
	 * @param position the position
	 * @return the char
	 */
	@Comment(text = "position位置的字符")
	public char charAt(@Comment(text = "position") int position) {
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
	@Comment(text = "跳过len字符")
	public int skip(@Comment(text = "len") int len) {
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
	@Comment(text = "从当前位置开始，begin和end之间的子串")
	public String bracket(@Comment(text = "begin") char begin, @Comment(text = "end") char end) {
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

	@Comment(text = "移动当前位置到pos")
	public int seek(@Comment(text = "pos") int pos) {
		this.pos = pos;
		return this.pos;
	}

	/**
	 * Skip.
	 *
	 * @param c0 the c0
	 * @return the int
	 */
	@Comment(text = "跳到char的位置")
	public int skip(@Comment(text = "char") char c0) {
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
	@Comment(text = "从当前位置，查找substring的位置")
	public int indexOf(@Comment(text = "substring") String str) {
		return s.indexOf(str, pos);
	}

	/**
	 * skip to the end of the str
	 * 
	 * @param str the string to skip
	 * @return the new position
	 */
	@Comment(text = "跳过substring")
	public int skip(@Comment(text = "substring") String str) {

		int i = s.indexOf(str, pos);
		if (i > -1) {
			pos = i + str.length();
		} else {
			pos = this.len;
		}

		return pos;
	}

	@Comment(text = "返回begin和end之间的子串")
	public String get(@Comment(text = "begin") int begin, @Comment(text = "end") int end) {
		return s.substring(begin, end);
	}

	/**
	 * get the sub string begin to end.
	 *
	 * @param begin the begin
	 * @param end   the end
	 * @return String
	 */
	@Comment(text = "从当前位置，返回begin和end之间的子串，begin和end可以是以｜分隔的多个子串")
	public String get(@Comment(text = "begin") String begin, @Comment(text = "end") String end) {
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

	@Comment(text = "返回begin子串后， 长度是size的子串")
	public String get(@Comment(text = "begin") String begin, @Comment(text = "size") int size) {
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
	@Comment(text = "返回begin和end之间的子串")
	public String substring(@Comment(text = "start") String begin, @Comment(text = "end") String end) {
		return get(begin, end);
	}

	/**
	 * merge two string to as a pattern string.
	 *
	 * @param s1 the s1
	 * @param s2 the s2
	 * @return String the min pattern string
	 */
	@Comment(hide = true)
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
	@Comment(hide = true)
	public String toString() {
		return s;
	}

	private Stack<Integer> _mark;

	@Comment(text = "标记位置")
	public StringFinder mark() {
		if (_mark == null) {
			_mark = new Stack<Integer>();
		}
		_mark.push(pos);
		return this;
	}

	@Comment(text = "恢复上次标记的位置")
	public StringFinder reset() {
		if (_mark != null && !_mark.isEmpty()) {
			pos = _mark.pop();
		} else {
			pos = 0;
		}
		return this;
	}

	@Comment(text = "按照regex分割剩余子串")
	public String[] split(@Comment(text = "regex") String regex) {
		return X.split(this.remain(), regex);
	}

	@Comment(text = "测试剩余子串是否以substring开始")
	public boolean startsWith(@Comment(text = "substring") String str) {
		return this.s.startsWith(str, pos);
	}

	@Comment(text = "替换substring为replacestring")
	public StringFinder replace(@Comment(text = "substring") String s1, @Comment(text = "replacestring") String r1) {
		this.s = this.s.replaceFirst(s1, r1);
		this.s1 = this.s.toLowerCase();
		this.pos += r1.length();
		this.len = this.s.length();
		return this;
	}

	@Comment(text = "替换start到end之间的子串为replacestring")
	public StringFinder replace(@Comment(text = "start") int start, @Comment(text = "end") int end,
			@Comment(text = "replacestring") String r1) {
		this.s = this.s.substring(0, start) + r1 + this.s.substring(end);
		this.s1 = this.s.toLowerCase();
		this.pos = start + r1.length();
		this.len = this.s.length();
		return this;
	}

	@Comment(text = "从当前位置截取到符合regex的子串")
	public String word(@Comment(text = "regex") String regex) {
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

	@Comment(text = "删除start到end的子串")
	public String remove(@Comment(text = "start") int start, @Comment(text = "end") int end) {
		String r = s.substring(start, end);
		s = s.substring(0, start) + s.substring(end);
		pos = start;
		return r;
	}

	@Comment(text = "在当前位置添加子串")
	public void add(@Comment(text = "string") String str) {
		s = s.substring(0, pos) + str + s.substring(pos);
		pos += str.length();
	}

	@Comment(text = "从当前位置找出所有包含字串的字符串", demo = ".find('.MP4', '.M3U8')")
	public List<String> find2(String... str) {
		List<String> l1 = new ArrayList<String>();
		for (String s : str) {
			this.mark();
			int p = this.find(s);
			if (p > 0) {
				String s1 = this.s.substring(p + s.length(), p + s.length() + 1);
				int p1 = this.backfind(s1);
				if (p1 > 0) {
					s1 = this.s.substring(p1 + 1, p + s.length());
					l1.add(s1);
					pos = p + s.length();
				}
			}
			this.reset();
		}
		return l1;
	}

}
