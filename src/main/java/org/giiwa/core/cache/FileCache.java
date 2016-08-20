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
package org.giiwa.core.cache;

import java.io.*;
import java.util.*;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.*;
import org.giiwa.core.bean.UID;
import org.giiwa.framework.web.Model;

/**
 * The Class FileCache is used to simple cache when no cache configured in
 * system
 */
class FileCache implements ICacheSystem {

	/** The log. */
	static Log log = LogFactory.getLog(FileCache.class);

	/** The root. */
	private String root;

	/**
   * Inits the.
   *
   * @param conf
   *          the conf
   * @return the i cache system
   */
	public static ICacheSystem create(Configuration conf) {
		FileCache f = new FileCache();
		f.root = Model.GIIWA_HOME + "/temp/_cache/";
		f.cache_size = conf.getInt("file.cache.size", 10000);
		return f;
	}

	/**
	 * get object.
	 *
	 * @param id
	 *            the id
	 * @return the object
	 */
	public synchronized Cachable get(String id) {

		/**
		 * test cache first
		 */
		try {
			if (cache.containsKey(id)) {
				return fromBytes(read(id));
			} else {
				/**
				 * if not in cache, then read from file
				 */
				String path = path(id);
				if (new File(path).exists()) {
					FileInputStream in = null;
					try {
						in = new FileInputStream(path);
						byte[] b = new byte[in.available()];
						in.read(b);

						save(id, b);
						return fromBytes(b);
					} finally {
						if (in != null) {
							in.close();
						}
					}
				}
			}
		} catch (Exception e) {
		}
		return null;
	}

	/**
	 * Sets the.
	 *
	 * @param id
	 *            the id
	 * @param o
	 *            the o
	 * @return true, if successful
	 */
	public synchronized boolean set(String id, Cachable o) {
		try {
			if (o == null) {
				return delete(id);
			} else {
				byte[] b = toBytes(o);

				/**
				 * cache it
				 */
				save(id, b);

				/**
				 * write to file
				 */
				String path = path(id);
				new File(path).getParentFile().mkdirs();
				FileOutputStream out = null;
				try {
					out = new FileOutputStream(path);
					out.write(b);
					out.flush();
					return true;
				} finally {
					if (out != null) {
						out.close();
					}
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return false;
	}

	/**
	 * To bytes.
	 *
	 * @param o
	 *            the o
	 * @return the byte[]
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private static byte[] toBytes(Object o) throws IOException {
		ByteArrayOutputStream out = null;
		ObjectOutputStream d = null;

		try {
			out = new ByteArrayOutputStream();
			d = new ObjectOutputStream(out);
			d.writeObject(o);
			d.flush();
			return out.toByteArray();
		} finally {
			if (d != null) {
				d.close();
			}
			if (out != null) {
				out.close();
			}
		}

	}

	/**
	 * From bytes.
	 *
	 * @param b
	 *            the b
	 * @return the object
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws ClassNotFoundException
	 *             the class not found exception
	 */
	private static Cachable fromBytes(byte[] b) throws IOException, ClassNotFoundException {
		ByteArrayInputStream in = null;
		ObjectInputStream d = null;
		try {
			in = new ByteArrayInputStream(b);
			d = new ObjectInputStream(in);
			return (Cachable) d.readObject();
		} finally {
			if (d != null) {
				d.close();
			}
			if (in != null) {
				in.close();
			}
		}
	}

	/**
	 * Delete.
	 *
	 * @param id
	 *            the id
	 * @return true, if successful
	 */
	public synchronized boolean delete(String id) {
		new File(path(id)).delete();
		cache.remove(id);
		return queue.remove(id);
	}

	/**
	 * Path.
	 *
	 * @param path
	 *            the path
	 * @return the string
	 */
	private String path(String path) {
		long id = Math.abs(UID.hash(path));
		char p1 = (char) (id % 23 + 'a');
		char p2 = (char) (id % 13 + 'A');
		char p3 = (char) (id % 7 + '0');

		StringBuilder sb = new StringBuilder(root).append(p1).append("/").append(p2).append("/").append(p3).append("/")
				.append(id);
		return sb.toString();
	}

	/**
	 * Read.
	 *
	 * @param id
	 *            the id
	 * @return the byte[]
	 */
	private byte[] read(String id) {
		byte[] b = cache.get(id);

		// set to last
		if (queue.contains(id)) {
			queue.remove(id);
			queue.add(id);
		}
		return b;
	}

	/**
	 * Save.
	 *
	 * @param id
	 *            the id
	 * @param b
	 *            the b
	 */
	private void save(String id, byte[] b) {
		cache.put(id, b);

		// set to last
		queue.remove(id);
		queue.add(id);

		while (queue.size() > cache_size) {
			id = queue.remove(0);
			cache.remove(id);
		}
	}

	/** The cache_size. */
	int cache_size = 1000;

	/** The cache. */
	Map<String, byte[]> cache = new HashMap<String, byte[]>();

	/** The queue. */
	List<String> queue = new ArrayList<String>();
}
