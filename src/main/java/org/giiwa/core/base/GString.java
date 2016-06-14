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
package org.giiwa.core.base;

import org.giiwa.core.bean.X;

// TODO: Auto-generated Javadoc
public class GString {

	/**
   * Creates the.
   *
   * @param s
   *          the s
   * @return the g string
   */
	public static GString create(String s) {
		return new GString(s);
	}

	int pos = 0;
	String s;

	private GString(String s) {
		this.s = s;
	}

	/**
   * find a substring.
   *
   * @param sub
   *          the sub
   * @return int the position
   */
	public int find(String sub) {
		int s1 = s != null ? s.indexOf(sub, pos) : -1;
		if (s1 > -1) {
			pos = s1;
		}
		return s1;
	}

	/**
   * skip a length.
   *
   * @param len
   *          the len
   * @return int the new position
   */
	public int skip(int len) {
		pos += len;
		return pos;
	}

	/**
   * get the sub string begin to end.
   *
   * @param begin
   *          the begin
   * @param end
   *          the end
   * @return String
   */
	public String get(String begin, String end) {
		if (s == null)
			return null;
		int b = s.indexOf(begin, pos);
		if (b < 0)
			return null;
		int e = s.indexOf(end, b + begin.length());
		if (e < 0) {
			return null;
		}
		pos = e + end.length();
		return s.substring(b + begin.length(), e);
	}

	/**
   * Substring.
   *
   * @param begin
   *          the begin
   * @param end
   *          the end
   * @return String
   * @deprecated
   */
	public String substring(String begin, String end) {
		return get(begin, end);
	}

	/**
   * merge two string to as a pattern string.
   *
   * @param s1
   *          the s1
   * @param s2
   *          the s2
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
			// System.out.println(i + ":" + j);
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
				// System.out.println("c1=" + c1);
				if (c1 == null) {
					i++;
					continue;
				}
				if (i + 2 < l1) {
					String c2 = _getmaxstr(s1.substring(i + 1), s2.substring(j));
					// System.out.println("c2=" + c2);
					if (c2 == null || c1.length() >= c2.length()) {
						sb.append(c1);
						all = false;
						j = c1.length() + s2.indexOf(c1, j);
						// System.out.println("j=" + j);
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

		// System.out.println("j=" + j + ", l2=" + l2 + ", all=" + all);
		return sb.toString();
	}

	private static String _getmaxstr(String s1, String s2) {
		// System.out.println("s1=" + s1 + ",s2=" + s2);
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

	/**
   * The main method.
   *
   * @param args
   *          the arguments
   */
	public static void main(String[] args) {
		// System.out.println(merge("aaaaaa", "aabbaabaab"));
		// System.out.println(merge("aaaaaa", "bbaabbbb"));
		// System.out.println(merge("aaaaaa", ".*aa.*"));
		System.out.println(merge("http://www.haodf.com/wenda/adele_g_4491037166.htm",
				"http://www.haodf.com/wenda/aiai6905_g_4377802805.htm"));
		System.out.println(merge("http://www.haodf.com/wenda/y.*_g_44959.*9.*9.*.htm",
				"http://www.haodf.com/wenda/doc.*_g_4.*4.*5.*7.*.htm"));

	}
}
