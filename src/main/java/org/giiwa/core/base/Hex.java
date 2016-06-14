/**
 * Copyright (C) 2010 Gifox Networks
 *
 * @project mms
 * @author jjiang 
 * @date 2010-10-23
 */

package org.giiwa.core.base;

// TODO: Auto-generated Javadoc
/**
 * The Class Hex.
 *
 * @author jjiang
 */
public class Hex {

    /**
     * Encode.
     *
     * @param data
     *            the data
     * @return the string
     */
    public static String encode(byte[] data) {
        return Base32.encode(data);
    }

    /**
     * Decode.
     *
     * @param s
     *            the s
     * @return the byte[]
     */
    public static byte[] decode(String s) {
        return Base32.decode(s);
    }

}
