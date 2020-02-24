package org.giiwa.web;

import org.giiwa.conf.Global;

/**
 * @deprecated
 * @author joe
 *
 */
class URL {

	/**
	 * force rewrite some url to new
	 * 
	 * @param originalurl the original url
	 * @param newurl      the new url
	 */
	public static void rewrite(String originalurl, String newurl) {
		Global.setConfig("rewrite/" + originalurl, newurl);
	}

	public static String rewrite(String uri) {
		return Global.getString("rewrite/" + uri, uri);
	}

}
