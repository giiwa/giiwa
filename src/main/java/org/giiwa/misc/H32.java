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

/**
 * The {@code H32} Class used to generate a "base32" string.
 * 
 * @author joe
 *
 */
public class H32 {

    /**
     * To string.
     *
     * @param l
     *            the l
     * @return the string
     */
    public static String toString(long l) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 13; i++) {
            int t = (int) (l & 0x1f);
            l = l >> 5;
            sb.append(chars[t]);
        }
        return sb.reverse().toString();
    }

    /** The Constant DIGITAL. */
    static final int DIGITAL = 32;

    /** The Constant chars. */
    static final char[] chars = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
}
