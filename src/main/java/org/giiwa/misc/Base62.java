package org.giiwa.misc;

import java.math.BigInteger;

@Deprecated
public class Base62 {

	private static final char[] BASE62_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
			.toCharArray();
	private static final BigInteger BASE = BigInteger.valueOf(62);

	public static String encode(long num) {
		return encode(BigInteger.valueOf(num));
	}

	public static String encode(BigInteger num) {
		if (num.compareTo(BigInteger.ZERO) < 0) {
			throw new IllegalArgumentException("num >=0 required");
		}
		if (num.equals(BigInteger.ZERO)) {
			return "0";
		}
		StringBuilder sb = new StringBuilder();
		BigInteger n = num;
		while (n.compareTo(BigInteger.ZERO) > 0) {
			BigInteger[] divAndMod = n.divideAndRemainder(BASE);
			int mod = divAndMod[1].intValue();
			sb.append(BASE62_CHARS[mod]);
			n = divAndMod[0];
		}
		return sb.reverse().toString();
	}

	public static BigInteger decode(String str) {
		BigInteger res = BigInteger.ZERO;
		for (char c : str.toCharArray()) {
			int idx = charToIndex(c);
			res = res.multiply(BASE).add(BigInteger.valueOf(idx));
		}
		return res;
	}

	private static int charToIndex(char c) {
		if (c >= '0' && c <= '9')
			return c - '0';
		else if (c >= 'a' && c <= 'z')
			return c - 'a' + 10;
		else if (c >= 'A' && c <= 'Z')
			return c - 'A' + 36;
		else
			throw new IllegalArgumentException("invalid char:" + c);
	}

}
