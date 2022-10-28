package org.giiwa.misc;

import static org.junit.Assert.*;

import org.giiwa.dao.X;
import org.junit.Test;

public class StringFinderTest {

	@Test
	public void test() {
		// System.out.println(merge("aaaaaa", "aabbaabaab"));
		// System.out.println(merge("aaaaaa", "bbaabbbb"));
		// System.out.println(merge("aaaaaa", ".*aa.*"));
		// System.out.println(merge("http://www.haodf.com/wenda/adele_g_4491037166.htm",
		// "http://www.haodf.com/wenda/aiai6905_g_4377802805.htm"));
		// System.out.println(merge("http://www.haodf.com/wenda/y.*_g_44959.*9.*9.*.htm",
		// "http://www.haodf.com/wenda/doc.*_g_4.*4.*5.*7.*.htm"));

		StringFinder s = StringFinder.create("{asd\\{a\"s}d\"asd}");
		System.out.println(s.bracket('{', '}'));
	}

	@Test
	public void testRemove() {
		String s1 = "aaa,bbb,ddd,ggg,hhh";
		StringFinder sf = StringFinder.create(s1);
		while (sf.find(",") > -1) {
			sf.remove(sf.pos, sf.pos + 1);
			sf.add(";");
		}
		System.out.println(sf.s);
	}

	@Test
	public void testFind() {

		String s1 = "aad<page name='aaa' value='aaaa'/>ggg";
		StringFinder sf = StringFinder.create(s1);
		while (sf.find("<page ") > -1) {

			int start = sf.pos;

			sf.nextTo(" name| value |/>|>");

			sf.mark();
			String s2 = sf.nextTo(" name| value|/>|>");

			while (s2.startsWith("name") || s2.startsWith("value")) {
//				System.out.println(s2);
				int i = s2.indexOf("=");
				String name = s2.substring(0, i);
				String value = s2.substring(i + 1).trim();
				if (value.startsWith("'") || value.startsWith("\"")) {
					value = value.substring(1, value.length() - 1);
				}

				System.out.println(name + "=>" + value);

				sf.mark();
				s2 = sf.nextTo(" name| value|/>|>");
			}
			sf.reset();
			sf.skip(">");
			
			sf.remove(start, sf.pos);
			sf.add(" ");
		}

		System.out.println(sf.s);

	}

}
