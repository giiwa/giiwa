package org.giiwa.misc;

import static org.junit.Assert.*;

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

}
