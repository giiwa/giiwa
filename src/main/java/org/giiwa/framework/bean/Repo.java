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
package org.giiwa.framework.bean;

import java.io.*;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.*;
import org.giiwa.core.bean.*;
import org.giiwa.core.dfile.DFile;

/**
 * repository of file system bean. <br>
 * table="gi_repo"
 * 
 * @author yjiang
 * 
 */
public class Repo {

	private static Log log = LogFactory.getLog(Repo.class);

	private static String ROOT = "/_repo";

	/**
	 * Initialize the Repo, this will be invoke when giiwa startup
	 * 
	 * @param conf the conf
	 */
	public static void init(Configuration conf) {
		// ROOT = conf.getString("repo.path", "/opt/repo");

		log.info("repo has been initialized.");

	}

	/**
	 * store the input data into the repo with a random id
	 * 
	 * @param filename
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static String append(String filename, InputStream in) throws IOException {
		try {
			String id = UID.id(filename, System.currentTimeMillis(), UID.random());

			append(id, filename, 0, in.available(), in, -1, null);

			return id;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Store the input data to associated id
	 * 
	 * @param id       the id
	 * @param name     the name
	 * @param position the position
	 * @param total    the total
	 * @param in       the in
	 * @param uid      the uid
	 * @return the long
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static long append(String id, String filename, long position, long total, InputStream in, long uid,
			String ip) throws IOException {
		Entity e = new Entity(id, filename);
		return e.store(position, in, total);
	}

	/**
	 * Gets the id.
	 * 
	 * @param uri the uri
	 * @return the id
	 */
	public static String getId(String uri) {
		if (X.isEmpty(uri))
			return null;

		String id = uri;
		int i = id.indexOf("/");
		while (i >= 0) {
			if (i > 0) {
				String s = id.substring(0, i);
				if (s.equals("repo") || s.equals("download")) {
					id = id.substring(i + 1);
					i = id.indexOf("/");
					if (i > 0) {
						id = id.substring(0, i);
					}
				} else {
					id = s;
					break;
				}
			} else {
				id = id.substring(1);
			}

			i = id.indexOf("/");
		}

		log.info("loadbyuri: uri=" + uri + ", id=" + id);
		return id;
	}

	/**
	 * Load by uri, please using load(string id), it also auto check the id is id"
	 * or a uri.
	 *
	 * @param uri the uri
	 * @return the entity
	 * @deprecated
	 */
	public static Entity loadByUri(String uri) {
		return load(uri);
	}

	/**
	 * Load.
	 * 
	 * @param folder the folder
	 * @param id     the id
	 * @return the entity
	 */
	public static Entity load(String id) {
		try {
			id = getId(id);
			return new Entity(id, null);
		} catch (Exception e) {
			log.error(id, e);
		}
		return null;
	}

	/**
	 * Delete.
	 * 
	 * @param id the id
	 * @return the int
	 */
	public static int delete(String id) {
		/**
		 * delete the file in the repo
		 */
		Entity e = load(id);
		e.delete();

		return 1;
	}

	/**
	 * entity of repo
	 * 
	 * @author yjiang
	 * 
	 */
	public static class Entity {

		String id;
		DFile file;
		String name;
		private InputStream in;

		public long length() {
			return file.length();
		}

		public long lastModified() {
			return file.lastModified();
		}

		public String getFiletype() {
			String name = this.getName();
			if (name != null) {
				int i = name.lastIndexOf(".");
				if (i > 0) {
					return name.substring(i + 1);
				}
			}
			return X.EMPTY;
		}

		public String getName() {
			return file.getName();
		}

		public long getCreated() {
			return file.lastModified();
		}

		/**
		 * Delete.
		 */
		public void delete() {
			file.delete();
		}

		private Entity(String id, String name) throws IOException {
			this.id = id;
			this.name = name;

			this.getFile();
		}

		private void getFile() throws IOException {

			String path = path(id);
			if (X.isEmpty(name)) {
				DFile f = Disk.seek(path);
				DFile[] ff = f.listFiles();
				if (ff != null && ff.length > 0) {
					file = ff[0];
				}
			} else {
				file = Disk.seek(path + name);
			}

//			log.debug("id=" + id + ", path=" + path(id) + ", name=" + name + ", file=" + file);
		}

		private long store(long position, InputStream in, long total) throws IOException {

			file.upload(position, in);

			this.getFile();
			return file.length();

		}

		/**
		 * get the inputstream of the repo Entity.
		 * 
		 * @return InputStream
		 * @throws IOException occur error where get the inputstream from Repo
		 */
		public InputStream getInputStream() throws IOException {
			if (in == null) {
				in = file.getInputStream();
			}

			return in;
		}

		/**
		 * Close.
		 */
		public synchronized void close() {
			X.close(in);
			in = null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object.finalize()
		 */
		@Override
		protected void finalize() throws Throwable {
			close();
			super.finalize();
		}

		public String getId() {
			return id;
		}

	}

	static private String path(String path) {
		long id = Math.abs(UID.hash(path));
		char p1 = (char) (id % 23 + 'a');
		char p2 = (char) (id % 19 + 'A');
		char p3 = (char) (id % 17 + 'a');
		char p4 = (char) (id % 13 + 'A');

		StringBuilder sb = new StringBuilder(ROOT);

		sb.append("/").append(p1).append("/").append(p2).append("/").append(p3).append("/").append(p4).append("/")
				.append(id).append("/");
		return sb.toString();
	}

	private static AtomicLong total = new AtomicLong(0); // byte
	private static AtomicLong cost = new AtomicLong(0); // ms

	public static long getSpeed() {
		if (cost.get() > 0) {
			return total.get() * 1000L / cost.get();
		}
		return 0;
	}

	public static void cleanup(long expired) {
		// TODO

	}

}
