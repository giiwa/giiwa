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
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.task.LiveHand;
import org.giiwa.task.Task;
import org.giiwa.web.Controller;

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
	 * @param conf the conf
	 * @return the i cache system
	 */
	public static ICacheSystem create() {
		FileCache f = new FileCache();
		f.root = Controller.GIIWA_HOME + "/temp/_cache/";
		f.cache_size = 10000;
		return f;
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
			if (cache.containsKey(id)) {
				return _fromBytes(_read(id));
			} else {
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

						_cache(id, b);

						return _fromBytes(b);
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
				_cache(id, b);

				Task.schedule(() -> {

					/**
					 * write to file
					 */
					String path = _path(id);
					new File(path).getParentFile().mkdirs();
					FileOutputStream out = null;
					try {
						out = new FileOutputStream(path);
						out.write(b);
						out.flush();
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					} finally {
						X.close(out);
					}

				});
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
		cache.remove(id);
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

	/**
	 * Read.
	 *
	 * @param id the id
	 * @return the byte[]
	 */
	private byte[] _read(String id) {

		Object[] oo = cache.get(id);
		byte[] b = (byte[]) oo[0];
		oo[1] = System.currentTimeMillis();

		// set to last
//		if (queue.contains(id)) {
//			queue.remove(id);
//			queue.add(id);
//		}
		return b;
	}

	/**
	 * Save.
	 *
	 * @param id the id
	 * @param b  the b
	 */
	private void _cache(String id, byte[] b) {

		cache.put(id, new Object[] { b, System.currentTimeMillis(), id });

		// set to last
//		queue.remove(id);
//		queue.add(id);

		if (cache.size() > cache_size) {
			Task.schedule(() -> {
				_clearup();
			});
		}
	}

	private void _clearup() {

		List<Object[]> l1 = new ArrayList<Object[]>(cache.values());
		Collections.sort(l1, new Comparator<Object[]>() {

			@Override
			public int compare(Object[] o1, Object[] o2) {

				long t1 = X.toLong(o1[1]);
				long t2 = X.toLong(o2[1]);

				if (t1 < t2)
					return 1;
				if (t1 > t2)
					return -1;

				return 0;
			}

		});

		for (int i = cache_size * 4 / 5; i < l1.size(); i++) {
			String id = (String) l1.get(i)[2];
			cache.remove(id);
		}

	}

	@Override
	public String toString() {
		return "FileCache [root=" + root + "]";
	}

	/** The cache_size. */
	int cache_size = 1000;

	/** The cache. */
	Map<String, Object[]> cache = new HashMap<String, Object[]>();

	/** The queue. */
//	List<String> queue = new ArrayList<String>();

	private static Map<String, LiveHand> _local = new HashMap<String, LiveHand>();

	@Override
	public boolean trylock(String name) {
		synchronized (_local) {
			LiveHand d = _local.get(name);
			if (d == null) {
				d = LiveHand.create(-1, 1);
				_local.put(name, d);
			}

			if (d.tryHold()) {
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
	public boolean unlock(String name, String value) {
		try {
			synchronized (_local) {
				LiveHand d = _local.remove(name);
				if (d != null) {
					d.drop();
					return true;
				}
			}
		} catch (Exception e) {
			// eat it
			log.error("unlock error, name=" + name, e);
		}

		return true;
	}

}
