package org.giiwa.misc;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.giiwa.dao.X;

public class CSV implements Closeable {

	BufferedReader re;
	int pos;
	List<Character> deli;

	public static CSV create(String re) {
		return create(re, Arrays.asList(','));
	}

	public static CSV create(String re, List<Character> deli) {
		return create(new BufferedReader(new StringReader(re)), deli);
	}

	public static CSV create(BufferedReader re) {
		return create(re, Arrays.asList(','));
	}

	public static CSV create(BufferedReader re, List<Character> deli) {
		CSV e = new CSV();
		e.re = re;
		e.deli = deli;
		return e;
	}

	@Override
	public void close() throws IOException {
		X.close(re);
	}

	/**
	 * auto parse number
	 * 
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object[] next() throws IOException {

		String line = re.readLine();
		if (line == null)
			return null;

		List l1 = new ArrayList();

		int i = 0;
		int start = 0;
		int qa = 0;
		String s1 = null;

		while (i < line.length()) {

			s1 = null;
			char c = line.charAt(i);
			start = i;
			qa = 0;

			if (c == 65279) {
				// UTF8 format
				i++;
				continue;
			} else if (c == '"') {
				// end of '"'
				qa = 1;
				i++;
				start = i;
				if (i == line.length()) {
					String s2 = re.readLine();
					if (s2 != null) {
						line += "\r\n" + s2;
					}
				}
				while (i < line.length()) {
					char c1 = line.charAt(i);
					if (c1 == '"') {
						qa++;
					} else if (deli.contains(c1) && qa % 2 == 0) {
						s1 = line.substring(start, i - 1);
						i++;
						break;
					} else if (i == line.length() - 1) {
						String s2 = re.readLine();
						if (s2 != null) {
							line += "\r\n" + s2;
						}
					}
					i++;
				}
			} else if (deli.contains(c)) {
				s1 = X.EMPTY;
				i++;
			} else {
				// end of ,
				i++;
				while (i < line.length()) {
					char c1 = line.charAt(i);
					if (deli.contains(c1)) {
						s1 = line.substring(start, i);
						i++;
						break;
					}
					i++;
				}
			}

			if (s1 != null) {
				if (X.isNumber(s1)) {
					if (s1.indexOf(".") > -1) {
						l1.add(X.toDouble(s1));
					} else {
						l1.add(X.toLong(s1));
					}
				} else {
					l1.add(s1);
				}
				s1 = null;
				start = i;
			}
		}

		if (start < line.length()) {
			if (qa > 0) {
				if (qa % 2 == 0) {
					if (line.endsWith("\"")) {
						s1 = line.substring(start, i - 1);
					} else {
						s1 = line.substring(start);
					}
				} else {
					s1 = line.substring(start);
				}
			} else {
				s1 = line.substring(start);
				if (s1.length() > 0) {
					if (deli.contains(s1.charAt(s1.length() - 1))) {
						s1 = s1.substring(0, s1.length() - 1);
					}
				}
			}
		}

		if (s1 != null) {
			if (X.isNumber(s1)) {
				if (s1.indexOf(".") > -1) {
					l1.add(X.toDouble(s1));
				} else {
					l1.add(X.toLong(s1));
				}
			} else {
				l1.add(s1);
			}
		}

		return l1.toArray();

	}

	/**
	 * all string
	 * 
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object[] next2() throws IOException {

		String line = re.readLine();
		if (line == null)
			return null;

		List l1 = new ArrayList();

		int i = 0;
		int start = 0;
		int qa = 0;
		String s1 = null;

		while (i < line.length()) {

			s1 = null;
			char c = line.charAt(i);
			start = i;
			qa = 0;

			if (c == 65279) {
				// UTF8 format
				i++;
				continue;
			} else if (c == '"') {
				// end of '"'
				qa = 1;
				i++;
				start = i;
				if (i == line.length()) {
					String s2 = re.readLine();
					if (s2 != null) {
						line += "\r\n" + s2;
					}
				}
				while (i < line.length()) {
					char c1 = line.charAt(i);
					if (c1 == '"') {
						qa++;
					} else if (deli.contains(c1) && qa % 2 == 0) {
						s1 = line.substring(start, i - 1);
						i++;
						break;
					} else if (i == line.length() - 1) {
						String s2 = re.readLine();
						if (s2 != null) {
							line += "\r\n" + s2;
						}
					}
					i++;
				}
			} else if (deli.contains(c)) {
				s1 = X.EMPTY;
				i++;
			} else {
				// end of ,
				i++;
				while (i < line.length()) {
					char c1 = line.charAt(i);
					if (deli.contains(c1)) {
						s1 = line.substring(start, i);
						i++;
						break;
					}
					i++;
				}
			}

			if (s1 != null) {
				l1.add(s1);
				s1 = null;
				start = i;
			}
		}

		if (start < line.length()) {
			if (qa > 0) {
				if (qa % 2 == 0) {
					if (line.endsWith("\"")) {
						s1 = line.substring(start, i - 1);
					} else {
						s1 = line.substring(start);
					}
				} else {
					s1 = line.substring(start);
				}
			} else {
				s1 = line.substring(start);
				if (s1.length() > 0) {
					if (deli.contains(s1.charAt(s1.length() - 1))) {
						s1 = s1.substring(0, s1.length() - 1);
					}
				}
			}
		}

		if (s1 != null) {
			l1.add(s1);
		}

		return l1.toArray();

	}

}
