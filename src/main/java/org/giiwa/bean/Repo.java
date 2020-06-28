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
package org.giiwa.bean;

import java.io.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.logging.*;
import org.giiwa.conf.Global;
import org.giiwa.dao.*;
import org.giiwa.dao.Helper.V;
import org.giiwa.dfile.DFile;
import org.giiwa.misc.IOUtil;
import org.giiwa.web.Language;

/**
 * repository of file system bean. <br>
 * table="gi_repo"
 * 
 * @author yjiang
 * 
 */
public class Repo {

	private static Log log = LogFactory.getLog(Repo.class);

	private static String ROOT = "/repo";

	/**
	 * Initialize the Repo, this will be invoke when giiwa startup
	 * 
	 * @param conf the conf
	 */
	public static void init(Configuration conf) {

		log.info("repo has been initialized.");

	}

	/**
	 * store the input data into the repo with a random id
	 * 
	 * @param filename the filename
	 * @param in       the inputstream
	 * @return the repo id
	 * @throws IOException the ioexception
	 */
	public static String append(String path, String filename, InputStream in) throws IOException {
		try {
			String id = UID.id(filename, System.currentTimeMillis(), UID.random());

			append(id, path, filename, 0, in.available(), in, -1, null);

			return id;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Store the input data to associated id
	 * 
	 * @deprecated
	 * @param id       the id
	 * @param filename the filename
	 * @param position the position
	 * @param total    the total
	 * @param in       the inputstream
	 * @param uid      the uid
	 * @param ip       the report ip
	 * @return the long
	 * @throws Exception
	 */
	public static long append(String id, String path, String filename, long position, long total, InputStream in,
			long uid, String ip) throws Exception {
		Entity e = Entity.load(id, path, filename);
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
				if (X.isIn(s, "f", "repo", "download")) {
					id = id.substring(i + 1);
//					i = id.indexOf("/");
//					if (i > 0) {
//						id = id.substring(0, i);
//					}
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
	 * @ @param id the id
	 * @return the entity
	 */
	public static Entity load(String id) {
		try {
			id = getId(id);
//			System.out.println("id=" + id);
			return Entity.load(id, null, null);
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
		if (e != null) {
			e.delete();
		}

		return 1;
	}

	/**
	 * entity of repo
	 * 
	 * @author yjiang
	 * 
	 */
	@Table(name = "gi_repo", memo = "GI-文件仓库")
	public static class Entity extends Bean {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public static BeanDAO<String, Entity> dao = BeanDAO.create(Entity.class);

		@Column(memo = "唯一序号ID")
		String id;

		@Column(memo = "文件名")
		String name;

		@Column(memo = "文件路径")
		String path;

		transient DFile file;
		transient InputStream in;

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
			if (file != null) {
				file.delete();
			}
		}

		private static Entity load(String id, String path, String name) throws Exception {

			Entity e = dao.load(id);
			if (e == null) {
				V v = V.create();
				v.append(X.ID, id);
				v.append("name", name);
				v.append("path", path(id, path));

				dao.insert(v);
				e = dao.load(id);
			}
			e.getFile();
			return e;
		}

		private void getFile() throws Exception {

			if (X.isEmpty(path)) {
				throw new IOException("path is empty");
			}

			if (X.isEmpty(name)) {
				DFile f = Disk.seek(path);
				DFile[] ff = f.listFiles();
				if (ff != null && ff.length > 0) {
					for (DFile f1 : ff) {
						if (X.isSame(f1.getDisk_obj().type, Disk.TYPE_DATA)) {
							file = ff[0];
							break;
						}
					}
				}
			} else {
				file = Disk.seek(path + name);
			}

			log.debug("id=" + id + ", path=" + path + ", name=" + name + ", file=" + file + ", type="
					+ file.getDisk_obj().type);
		}

		public long store(long position, InputStream in, long total) throws Exception {

			file.upload(position, in);

			this.getFile();
			return file.length();

		}

		public boolean exists() throws IOException {
			return file.exists();
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

		public DFile getDFile() {
			return file;
		}

	}

	static private String path(String id, String path) {
		if (!X.isEmpty(path)) {
			return path + "/" + Language.getLanguage().format(System.currentTimeMillis(), "yyyy/MM/dd/HH/mm/ss/");
		}
		return ROOT + "/" + Language.getLanguage().format(System.currentTimeMillis(), "yyyy/MM/dd/HH/mm/ss/");
	}

	private static AtomicLong total = new AtomicLong(0); // byte
	private static AtomicLong cost = new AtomicLong(0); // ms

	public static long getSpeed() {
		if (cost.get() > 0) {
			return total.get() * 1000L / cost.get();
		}
		return 0;
	}

	public static int cleanup(long age) {

		Lock door = Global.getLock("repo.clean");

		int count = 0;
		if (door.tryLock()) {
			try {
				DFile f = Disk.seek(ROOT);

				DFile[] ff = f.listFiles();
				if (ff != null) {
					for (DFile f1 : ff) {
						count += IOUtil.delete(f1, age);
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			} finally {
				door.unlock();
			}
		}
		return count;
	}

	public static void main(String[] args) {
		String s = "123/aaa.jpg";
		Entity e = Repo.load(s);
		System.out.println(e.id);
	}

	public static Entity get(String id, String path, String filename) throws Exception {
		return Entity.load(id, path, filename);
	}

}
