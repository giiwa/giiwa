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
package org.giiwa.dao;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.cache.Cache;
import org.giiwa.conf.Global;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.misc.*;
import org.giiwa.task.GlobalLock;

/**
 * The {@code UID} Class used to create unique id, or sequence, random string
 * 
 * @author joe
 *
 */
public final class UID {

	private static Log log = LogFactory.getLog(UID.class);

	/**
	 * increase and get the unique sequence number by key, <br>
	 * the number=[cluster.code] + seq
	 *
	 * @param key the key
	 * @return long of the unique sequence
	 */
	public static long next(String key) {

		Lock door = GlobalLock.create("uid." + key);

		try {
			if (door.tryLock(10, TimeUnit.SECONDS)) {
				try {
					return _next(key);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				} finally {
					door.unlock();
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return -1;
	}

	private static long _next(String key) throws Exception {

		long prefix = Global.getLong("cluster.code", 0) * 10000000000000L;

		/**
		 * remove cache
		 */
//		Cache.remove("global/" + key);

		Global f = Helper.load(key, Global.class);

		long v = Math.max(1, Global.getLong("uid.next.s1", 1));
		if (f == null) {
			String linkid = UID.random();

			Helper.insert(V.create(X.ID, key).append("l", v).append("linkid", linkid), Global.class);
			f = Helper.load(key, Global.class);
			if (f == null) {
//				log.error("occur error when create unique id, name=" + key);
				throw new Exception("get uid error! key=" + key);
			} else if (!X.isSame(f.getString("linkid"), linkid)) {
				return _next(key);
			}

		} else {
			// log.debug("v=" + v + ", f=" + f);
			long v1 = Math.max(f.getLong("l"), Global.getLong("uid.next.s1", 1));

			if (Helper.update(W.create(X.ID, key).and("l", f.getLong("l")), V.create("l", v1 + 1L),
					Global.class) <= 0) {
				return _next(key);
			}
			v = v1 + 1;
		}

//		long max = Global.getLong("uid.next.s2", -1);
//		if (max > -1 && v > max) {
//			throw new Exception("uid.next(" + key + ") is run out, max=" + max + ", cur=" + v);
//		}
		return prefix + v;
	}

	public static long get(String key) {

		Lock door = GlobalLock.create("uid." + key);

		try {
			door.lock();

			/**
			 * remove cache
			 */
			Cache.remove("global/" + key);

			Global f = Helper.load(key, Global.class);

			long v = 1;
			if (f == null) {
				return 0L;
			} else {
				v = f.getLong("l");
				return v;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			door.unlock();
		}

		return -1;
	}

	/**
	 * return string with the length
	 * 
	 * @param key the key
	 * @param len the length
	 * @return the string
	 */
	public static String next(String key, int len) {
		StringBuilder p = new StringBuilder("00000000000");
		while (p.length() < len) {
			p.append("0000000000");
		}
		String s = p.toString() + next(key);
		int i = s.length();
		return s.substring(i - len, i);
	}

	/**
	 * generate a global random string.
	 * 
	 * @return the string
	 */
	public static String random() {
		return UUID.randomUUID().toString();
	}

	/**
	 * convert the long data to a BASE32 string.
	 *
	 * @param hash the hash
	 * @return the string
	 */
	public static String id(long hash) {
		return H32.toString(hash);
	}

	/**
	 * generate the unique id by the parameter <br>
	 * if the parameter are same, the id will be same, the "id" is H32 of
	 * hash(64bit) of parameters.
	 *
	 * @param ss the parameters
	 * @return string
	 */
	public static String id(Object... ss) {
		StringBuilder sb = new StringBuilder();
		for (Object s : ss) {
			if (sb.length() > 0)
				sb.append("/");
			sb.append(s);
		}
		return id(hash(sb.toString()));
	}

	/**
	 * global id.
	 *
	 * @return String
	 */
	public static String uuid() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Hash (64bits) of string.
	 *
	 * @param s the parameter string
	 * @return the long
	 */
	public static long hash(String s) {
		if (s == null) {
			return 0;
		}

		int h = 0;
		int l = 0;
		int len = s.length();
		char[] val = s.toCharArray();
		for (int i = 0; i < len; i++) {
			h = 31 * h + val[i];
			l = 29 * l + val[i];
		}
		return Math.abs(((long) h << 32) | ((long) l & 0x0ffffffffL));
	}

	/**
	 * generate a random string with the length.
	 *
	 * @param length the length of the random string
	 * @return the string
	 */
	public static String random(int length) {

		Random rand = new Random(System.nanoTime());
		StringBuilder sb = new StringBuilder();
		while (length > 0) {
			sb.append(chars[rand.nextInt(chars.length - 1)]);
			length--;
		}
		return sb.toString();
	}

	/**
	 * Random.
	 *
	 * @param length  the length
	 * @param sources the sources
	 * @return the string
	 */
	public static String random(int length, String sources) {
		if (sources == null || sources.length() == 0) {
			sources = new String(digitals);
		}
		int codesLen = sources.length();
		Random rand = new Random(System.nanoTime());
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(sources.charAt(rand.nextInt(codesLen - 1)));
		}
		return sb.toString();
	}

	/**
	 * generate a firm random by code, no duplicated
	 * 
	 * @param code
	 * @param len
	 * @return
	 * @throws Exception
	 */
	public static int[] random(String code, int len) throws Exception {
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		random.setSeed(code.getBytes());
		int[] ii = new int[len];
		List<Integer> ss = new ArrayList<Integer>(len);
		for (int i = 0; i < len; i++) {
			ss.add(i);
		}
		int i = 0;
		while (len > 0) {
			int a = random.nextInt(len);
			ii[i] = ss.remove(a);
			i++;
			len--;
		}
//		System.out.println(i);
		return ii;
	}

	/**
	 * generate a digital string with the length.
	 *
	 * @param length the length of the digital string
	 * @return the string
	 */
	public static String digital(int length) {
		Random rand = new Random(System.nanoTime());
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			sb.append(digitals[rand.nextInt(digitals.length - 1)]);
		}
		return sb.toString();
	}

	private static final char[] digitals = "0123456789".toCharArray();
	private static final char[] chars = "0123456789abcdefghjiklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

}
