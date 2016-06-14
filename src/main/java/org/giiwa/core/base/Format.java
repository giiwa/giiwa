/**
 * Copyright (C) 2010 Gifox Networks
 *
 * @project mms
 * @author jjiang 
 * @date 2010-10-23
 */
package org.giiwa.core.base;

import java.text.SimpleDateFormat;
import java.util.Date;

// TODO: Auto-generated Javadoc
/**
 * The Class Format.
 */
public class Format {

	/** The df. */
	static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss Z");

	/**
	 * Parses the.
	 * 
	 * @param s
	 *            the s
	 * @return the date
	 */
	public static Date parse(String s) {
		try {
			
			return new Date(Long.parseLong(s));
//			return df.parse(s);
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Format date.
	 * 
	 * @param d
	 *            the d
	 * @return the string
	 */
	public static String formatDate(Date d) {
		if (d != null) {
			return Long.toString(d.getTime());
			
//			df.setTimeZone(TimeZone.getDefault());
//			return df.format(d);
		}

		return null;
	}

	/**
	 * Format phone.
	 * 
	 * @param p
	 *            the p
	 * @return the string
	 */
	public static String formatPhone(String p) {
		return p;
	}

	/**
	 * Format.
	 * 
	 * @param sentdate
	 *            the sentdate
	 * @return the long
	 */
	public static long format(Date sentdate) {
		return sentdate.getTime();
	}

}
