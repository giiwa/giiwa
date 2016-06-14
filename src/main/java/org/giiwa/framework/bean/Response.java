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
package org.giiwa.framework.bean;

// TODO: Auto-generated Javadoc
/**
 * utility for generate a reponse packet
 * 
 * @author yjiang
 * 
 */
public class Response {

	/**
	 * require encode
	 */
	boolean requiredEncode = true;

	/**
	 * output byte array
	 */
	byte[] out;
	int offset;

	/**
	 * is require encode?
	 * 
	 * @return boolean
	 */
	public boolean isRequiredEncode() {
		return requiredEncode;
	}

	/**
	 * Sets the required encode.
	 * 
	 * @param requiredEncode
	 *            the required encode
	 * @return the response
	 */
	public Response setRequiredEncode(boolean requiredEncode) {
		this.requiredEncode = requiredEncode;
		return this;
	}

	/**
	 * Length.
	 * 
	 * @return the int
	 */
	public int length() {
		return offset;
	}

	/**
	 * Clear.
	 */
	public void clear() {
		out = new byte[1024];
		offset = 0;
	}

	/**
	 * Instantiates a new response.
	 */
	public Response() {
		this(1024);
		offset = 0;
	}

	/**
	 * Instantiates a new response.
	 * 
	 * @param init
	 *            the init
	 */
	public Response(int init) {
		out = new byte[init];
		offset = 0;
	}

	/**
	 * Instantiates a new response.
	 * 
	 * @param requiredencode
	 *            the requiredencode
	 */
	public Response(boolean requiredencode) {
		this();
		requiredEncode = requiredencode;
	}

	/**
	 * generate bytes array
	 * 
	 * @return byte[]
	 */
	public byte[] getBytes() {
		if (out.length > offset) {
			byte[] b = new byte[offset];
			System.arraycopy(out, 0, b, 0, offset);
			out = b;
		}
		return out;
	}

	private void expand(int len) {
		if (offset + len > out.length) {
			byte[] b = new byte[out.length + len + 1024];
			System.arraycopy(out, 0, b, 0, offset);
			out = b;
		}
	}

	/**
	 * Write byte.
	 * 
	 * @param b
	 *            the b
	 * @return the response
	 */
	public Response writeByte(byte b) {
		expand(1);
		out[offset++] = b;
		return this;
	}

	/**
	 * Write short.
	 * 
	 * @param s
	 *            the s
	 * @return the response
	 */
	public Response writeShort(short s) {
		writeByte((byte) ((s >> 8) & 0xFF));
		writeByte((byte) (s & 0xFF));
		return this;
	}

	/**
	 * Write int.
	 * 
	 * @param i
	 *            the i
	 * @return the response
	 */
	public Response writeInt(int i) {
		writeShort((short) ((i >> 16) & 0xFFFF));
		writeShort((short) (i & 0xFFFF));
		return this;
	}

	/**
	 * Write long.
	 * 
	 * @param l
	 *            the l
	 * @return the response
	 */
	public Response writeLong(long l) {
		writeInt((int) ((l >> 32) & 0xFFFFFFFF));
		writeInt((int) (l & 0xFFFFFFFF));
		return this;
	}

	/**
	 * Write string.
	 * 
	 * @param s
	 *            the s
	 * @return the response
	 */
	public Response writeString(String s) {
		if (s == null) {
			writeInt(0);
			return this;
		}

		byte[] b = s.getBytes();

		writeInt(b.length);
		expand(b.length);

		System.arraycopy(b, 0, out, offset, b.length);
		offset += b.length;

		return this;
	}

	/**
	 * Write bytes.
	 * 
	 * @param b
	 *            the b
	 * @return the response
	 */
	public Response writeBytes(byte[] b) {
		if (b == null)
			return this;

		expand(b.length);
		System.arraycopy(b, 0, out, offset, b.length);
		offset += b.length;

		return this;
	}

	/**
	 * Read double.
	 * 
	 * @param b
	 *            the b
	 * @return the response
	 */
	public Response readDouble(double b) {
		return writeString(Double.toString(b));
	}

	/**
	 * Write float.
	 * 
	 * @param b
	 *            the b
	 * @return the response
	 */
	public Response writeFloat(float b) {
		return writeString(Float.toString(b));
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder("Response:").append(offset)
				.append("[");
		for (int i = 0; i < offset; i++) {
			if (i > 0)
				sb.append(" ");
			sb.append(Integer.toHexString(out[i] & 0xFF));
		}
		return sb.append("]").toString();
	}

}
