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
package org.giiwa.cache;

import java.io.*;
import java.util.*;

import org.apache.commons.logging.*;
import org.giiwa.bean.Temp;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;

/**
 * The Class FileCache is used to simple cache when no cache configured in
 * system
 */
public class FileCache implements ICacheSystem {

	/** The log. */
	static Log log = LogFactory.getLog(FileCache.class);

	/** The root. */
	private String root;

	public static FileCache inst = new FileCache();

	private FileCache() {
		root = Temp.ROOT + "/_cache/";
//		cache_size = 10000;
	}

	/**
	 * Inits the.
	 *
	 * @param conf the conf
	 * @return the i cache system
	 */
	public static ICacheSystem create() {
		return inst;
	}

	/**
	 * get object.
	 *
	 * @param id the id
	 * @return the object
	 */
	public synchronized Object get(String id) {

		/**
		 * test cache first
		 */
		try {
//			if (cache.containsKey(id)) {
//				return _fromBytes(_read(id));
//			} else {
			/**
			 * if not in cache, then read from file
			 */
			String path = _path(id);
			if (new File(path).exists()) {
				FileInputStream in = null;
				try {
					in = new FileInputStream(path);
					byte[] b = new byte[in.available()];
					in.read(b);

					return _fromBytes(b);
				} finally {
					if (in != null) {
						in.close();
					}
				}
//				}
			}
		} catch (Exception e) {

		}
		return null;
	}

	public void touch(String name, long expired) {
		String path = _path(name);
		File f = new File(path);
		f.setLastModified(System.currentTimeMillis());
	}

	/**
	 * Sets the.
	 *
	 * @param id the id
	 * @param o  the o
	 * @return true, if successful
	 */
	public synchronized boolean set(String id, Object o, long expired) {
		try {
			if (o == null) {
				return delete(id);
			} else {
				byte[] b = _toBytes(o);

				/**
				 * cache it
				 */
//				_cache(id, b);
				String path = _path(id);
				X.IO.mkdirs(new File(path).getParentFile());
				FileOutputStream out = null;
				try {
//					byte[] b = (byte[]) l1.get(i)[0];
					out = new FileOutputStream(path);
					out.write(b);
					out.flush();
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				} finally {
					X.close(out);
				}

				return true;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return false;
	}

	/**
	 * To bytes.
	 *
	 * @param o the o
	 * @return the byte[]
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private static byte[] _toBytes(Object o) throws IOException {
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
	 * @param b the b
	 * @return the object
	 * @throws IOException            Signals that an I/O exception has occurred.
	 * @throws ClassNotFoundException the class not found exception
	 */
	private static Object _fromBytes(byte[] b) throws IOException, ClassNotFoundException {
		ByteArrayInputStream in = null;
		ObjectInputStream d = null;
		try {
			in = new ByteArrayInputStream(b);
			d = new ObjectInputStream(in);
			return d.readObject();
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
	 * @param id the id
	 * @return true, if successful
	 */
	public synchronized boolean delete(String id) {

		new File(_path(id)).delete();
		_local.remove(id);

		return true;
//		return queue.remove(id);
	}

	/**
	 * Path.
	 *
	 * @param path the path
	 * @return the string
	 */
	private String _path(String path) {
		long id = Math.abs(UID.hash(path));
		char p1 = (char) (id % 23 + 'a');
		char p2 = (char) (id % 13 + 'A');
		char p3 = (char) (id % 7 + '0');

		StringBuilder sb = new StringBuilder(root).append(p1).append("/").append(p2).append("/").append(p3).append("/")
				.append(id);
		return sb.toString();
	}

	@Override
	public String toString() {
		return "FileCache [root=" + root + "]";
	}

	private static Map<String, Integer> _local = new HashMap<String, Integer>();

	@Override
	public boolean trylock(String name, boolean debug) {
		synchronized (_local) {
			Integer d = _local.get(name);
			if (d == null) {
				d = 0;
				_local.put(name, 0);
			}

			if (d == 0) {
				_local.put(name, 1);
				return true;
			}
		}
		return false;
	}

	@Override
	public void expire(String id, long ms) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean unlock(String name, String value, boolean debug) {
		synchronized (_local) {
			_local.put(name, 0);
		}
		return true;
	}

	@Override
	public void close() {

		// save all cached data
//		List<Object[]> l1 = null;
//		synchronized (cache) {
//			l1 = new ArrayList<Object[]>(cache.values());
//		}
//
//		for (int i = 0; i < l1.size(); i++) {
//			String id = (String) l1.get(i)[2];
//
//			/**
//			 * write to file
//			 */
//			String path = _path(id);
//			X.IO.mkdirs(new File(path).getParentFile());
//			FileOutputStream out = null;
//			try {
//				byte[] b = (byte[]) l1.get(i)[0];
//				out = new FileOutputStream(path);
//				out.write(b);
//				out.flush();
//			} catch (Exception e) {
//				log.error(e.getMessage(), e);
//			} finally {
//				X.close(out);
//			}
//		}

	}

	@Override
	public long now() {
		return System.currentTimeMillis();
	}

}
