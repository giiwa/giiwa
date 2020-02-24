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
