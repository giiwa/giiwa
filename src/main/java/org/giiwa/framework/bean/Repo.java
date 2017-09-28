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
import org.giiwa.core.base.IOUtil;
import org.giiwa.core.bean.*;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.conf.Global;
import org.giiwa.core.task.Task;

/**
 * repository of file system bean. <br>
 * table="gi_repo"
 * 
 * @author yjiang
 * 
 */
@Table(name = "gi_repo")
public class Repo extends Bean {

	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(Repo.class);

	private static String ROOT;

	/**
	 * Initialize the Repo, this will be invoke when giiwa startup
	 * 
	 * @param conf
	 *            the conf
	 */
	public static void init(Configuration conf) {
		ROOT = conf.getString("repo.path", "/opt/repo");

		log.info("repo has been initialized.");

	}

	/**
	 * get the unique id of repo
	 * 
	 * @return the id
	 */
	public static String id() {
		String id = UID.id(System.currentTimeMillis(), UID.random());
		try {
			while (Helper.exists(id, Repo.class)) {
				id = UID.id(System.currentTimeMillis(), UID.random());
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return id;
	}

	/**
	 * List.
	 * 
	 * @param uid
	 *            the uid
	 * @param offset
	 *            the offset
	 * @param limit
	 *            the limit
	 * @return the beans
	 */
	public static Beans<Entity> list(long uid, int offset, int limit) {
		return Helper.load(W.create("uid", uid).sort(X.CREATED, -1), offset, limit, Entity.class);
	}

	/**
	 * List.
	 * 
	 * @param tag
	 *            the tag
	 * @param offset
	 *            the offset
	 * @param limit
	 *            the limit
	 * @return the beans
	 */
	public static Beans<Entity> list(String tag, int offset, int limit) {
		return Helper.load(W.create("tag", tag).sort(X.CREATED, -1), offset, limit, Entity.class);
	}

	/**
	 * store the inputstream data in repo.
	 *
	 * @param id
	 *            the id
	 * @param name
	 *            the name
	 * @param in
	 *            the in
	 * @return long
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public static long store(String id, String name, InputStream in) throws IOException {
		return store(X.EMPTY, id, name, X.EMPTY, 0, in.available(), in, -1, true, -1);
	}

	/**
	 * store the input stream to the repo, and return id
	 * 
	 * @param name
	 *            the name
	 * @param in
	 *            the inputstream
	 * @return the ID
	 * @throws IOException
	 *             throw IOException if failed
	 */
	public static String store(String name, InputStream in) throws IOException {
		String id = id();
		store(X.EMPTY, id, name, X.EMPTY, 0, in.available(), in, -1, true, -1);
		return id;
	}

	/**
	 * store the file in repo
	 * 
	 * @param name
	 *            the name
	 * @param file
	 *            the file
	 * @return the id
	 * @throws IOException
	 *             throw exception if failed
	 */
	public static String store(String name, File file) throws IOException {
		FileInputStream in = null;

		try {
			in = new FileInputStream(file);
			String id = id();
			store(X.EMPTY, id, name, X.EMPTY, 0, in.available(), in, -1, true, -1);
			return id;
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	/**
	 * Store.
	 * 
	 * @param folder
	 *            the folder
	 * @param id
	 *            the id
	 * @param name
	 *            the name
	 * @param tag
	 *            the tag
	 * @param position
	 *            the position
	 * @param total
	 *            the total
	 * @param in
	 *            the in
	 * @param expired
	 *            the expired
	 * @param share
	 *            the share
	 * @param uid
	 *            the uid
	 * @return the long
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public static long store(String folder, String id, String name, String tag, long position, long total,
			InputStream in, long expired, boolean share, long uid) throws IOException {
		Entity e = new Entity();
		e.set("folder", folder);
		e.set("name", name);
		e.set(X.ID, id);
		e.set("total", total);
		e.set("expired", expired);
		e.set("uid", uid);

		return e.store(tag, position, in, total, name, (byte) (share ? 0x01 : 0));
	}

	/**
	 * Gets the id.
	 * 
	 * @param uri
	 *            the uri
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
	 * @param uri
	 *            the uri
	 * @return the entity
	 * @deprecated
	 */
	public static Entity loadByUri(String uri) {
		return load(uri);
	}

	/**
	 * Load.
	 *
	 * @param folder
	 *            the folder
	 * @param id
	 *            the id
	 * @param f
	 *            the f
	 * @return the entity
	 */
	public static Entity load(String folder, String id, File f) {
		if (f.exists()) {
			Entity e = null;
			if (!X.isEmpty(id)) {
				e = Helper.load(id, Entity.class);
			}

			if (e == null) {
				try {
					InputStream in = new FileInputStream(f);

					/**
					 * will not close the inputstream
					 */
					return Entity.create(in);

				} catch (Exception e1) {
					log.error("load: id=" + id, e1);
				}
			}

			return e;
		} else {
			try {
				log.warn("not find the file: " + f.getCanonicalPath() + ", id=" + id);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return null;
	}

	/**
	 * Load.
	 * 
	 * @param id
	 *            the id
	 * @return the entity
	 */
	public static Entity load(String id) {
		id = getId(id);
		if (!X.isEmpty(id)) {
			return load(null, id);
		}
		return null;
	}

	/**
	 * Delete.
	 *
	 * @param folder
	 *            the folder
	 * @param id
	 *            the id
	 */
	public static void delete(String folder, String id) {
		File f = new File(path(folder, id));

		if (f.exists()) {
			f.delete();
		}
	}

	/**
	 * Load.
	 * 
	 * @param folder
	 *            the folder
	 * @param id
	 *            the id
	 * @return the entity
	 */
	public static Entity load(String folder, String id) {
		String path = path(folder, id);
		return load(folder, id, new File(path));
	}

	/**
	 * Delete.
	 * 
	 * @param id
	 *            the id
	 * @return the int
	 */
	public static int delete(String id) {
		/**
		 * delete the file in the repo
		 */
		Repo.delete(null, id);

		/**
		 * delete the info in table
		 */
		Helper.delete(id, Entity.class);

		return 1;
	}

	/**
	 * entity of repo
	 * 
	 * @author yjiang
	 * 
	 */
	@Table(name = "gi_repo")
	public static class Entity extends Bean {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		// private byte version = 1;
		//
		// public long pos;
		// public int flag;
		// public long expired;
		// public long total;
		// public long uid;
		// public String id;
		// public String name;
		// public long created;
		// public String folder;
		// String memo;

		private transient InputStream in;
		private transient int headsize;

		public String getMemo() {
			return getString("memo");
		}

		public String getUrl() {
			return "/repo/" + getId() + "/" + getName();
		}

		public byte getVersion() {
			return (byte) getInt("version");
		}

		public long getPos() {
			return getLong("pos");
		}

		public int getFlag() {
			return getInt("flag");
		}

		public long getExpired() {
			return getLong("expired");
		}

		public long getTotal() {
			return getLong("total");
		}

		public String getId() {
			return this.getString(X.ID);
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
			return getString("name");
		}

		public long getCreated() {
			return getLong(X.CREATED);
		}

		transient User user;

		public User getUser() {
			if (user == null) {
				user = User.loadById(this.getLong("uid"));
			}
			return user;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return new StringBuilder("Repo.Entity[").append(getId()).append(", name=").append(getName())
					.append(", pos:").append(getPos()).append(", total:").append(getTotal()).append("]").toString();
		}

		/**
		 * Delete.
		 */
		public void delete() {
			Repo.delete(getId());
		}

		@SuppressWarnings("resource")
		private long store(String tag, long position, InputStream in, long total, String name, int flag)
				throws IOException {
			File f = new File(path(getFolder(), getId()));

			if (f.exists()) {
				InputStream tmp = null;
				try {
					tmp = new FileInputStream(f);
					if (!load(tmp)) {// && (total != this.getTotal() ||
						// !name.equals(this.getName()))) {

						log.error("file: " + f.getCanonicalPath());

						/**
						 * this file is not original file
						 */
						throw new IOException("same filename[" + getId() + "/" + this.getName()
								+ "], but different size, old.total=" + this.getTotal() + ", new.total=" + total
								+ ", old.name=" + this.getName() + ", new.name=" + name + ", ?"
								+ (total != this.getTotal() || !name.equals(this.getName())));
					}
				} finally {
					close();
				}
			} else {
				f.getParentFile().mkdirs();
			}

			if (!f.exists() || total != this.getTotal()) {
				/**
				 * initialize the storage, otherwise append
				 */
				OutputStream out = null;
				try {
					out = new FileOutputStream(f);
					set("pos", in.available());

					Response resp = new Response();
					resp.writeLong(getPos());
					resp.writeInt(flag);
					resp.writeLong(getExpired());
					resp.writeLong(total);
					resp.writeInt((int) 0);
					resp.writeString(getId());
					resp.writeString(name);
					byte[] bb = resp.getBytes();
					resp = new Response();

					resp.writeByte(getVersion());
					resp.writeInt(bb.length);

					resp.writeBytes(bb);
					bb = resp.getBytes();
					out.write(bb);
					long pos = 0;
					bb = new byte[32 * 1024];

					int len = in.read(bb);
					while (len > 0) {
						out.write(bb, 0, len);
						pos += len;
						len = in.read(bb);
					}

					long pp = pos;
					if (total > 0) {
						while (pp < total) {
							len = (int) Math.min(total - pp, bb.length);
							out.write(bb, 0, len);
							pp += len;
						}
					}

					try {
						if (Helper.exists(getId(), Entity.class)) {
							Helper.update(getId(), V.create("total", pp).set("tag", tag).set("expired", getExpired()),
									Entity.class);
						} else {
							Helper.insert(V.create(X.ID, getId()).set("uid", 0).set("total", pp).set("tag", tag)
									.set("expired", getExpired()).set(X.CREATED, System.currentTimeMillis())
									.set("flag", flag).set("name", name), Entity.class);
						}
					} catch (Exception e1) {
						log.error(e1.getMessage(), e1);
					}

					/**
					 * check the free of the user
					 */
					// long free = User.checkFree(getUid());
					// if (free < 0) {
					// throw new IOException("repo.no.space");
					// }

					log.debug("stored, id=" + this.getId() + ", pos=" + pos);

					return pos;
				} catch (IOException e) {
					Repo.delete(getId());

					throw e;
				} finally {
					if (out != null) {
						try {
							out.close();
						} catch (IOException e) {
							log.error(e);
						}
					}

					try {
						in.close();
					} catch (IOException e) {
						log.error(e);
					}
				}

			} else {
				/**
				 * append
				 */
				RandomAccessFile raf = null;
				/**
				 * load head, and skip
				 */
				try {
					raf = new RandomAccessFile(f, "rws");
					byte[] bb = new byte[17]; // version(1) + head.length(4) +
					// pos(8) + flag(4)
					raf.read(bb);
					Request req = new Request(bb, 0);

					set("version", req.readByte());
					int head = req.readInt();
					set("pos", req.readLong());

					if (getPos() >= position) {
						raf.seek(head + 5 + position);

						bb = new byte[32 * 1024];
						int len = in.read(bb);
						while (len > 0) {
							raf.write(bb, 0, len);
							position += len;
							len = in.read(bb);
						}

						if (position > getPos()) {
							Response resp = new Response();
							resp.writeLong(position);
							raf.seek(5);
							raf.write(resp.getBytes());
							set("pos", position);
						}
					}

					return getPos();
				} finally {
					if (raf != null) {
						try {
							raf.close();
						} catch (IOException e) {
							log.error(e);
						}
					}
					if (in != null) {
						try {
							in.close();
						} catch (IOException e) {
							log.error(e);
						}
					}
				}
			}

		}

		/**
		 * get the inputstream of the repo Entity.
		 * 
		 * @return InputStream
		 * @throws IOException
		 *             occur error where get the inputstream from Repo
		 */
		public InputStream getInputStream() throws IOException {
			if (in == null) {
				File f = new File(path(getFolder(), getId()));

				if (f.exists()) {
					try {
						in = new FileInputStream(f);
						load(in);
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				}
			}

			return in;
		}

		private String getFolder() {
			return getString("folder");
		}

		/**
		 * Close.
		 */
		public synchronized void close() {
			if (in != null) {
				try {
					in.close();
					in = null;
				} catch (IOException e) {
					log.error(e);
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#finalize()
		 */
		@Override
		protected void finalize() throws Throwable {
			close();
			super.finalize();
		}

		private boolean load(InputStream in) {
			try {
				byte[] bb = new byte[1];
				in.read(bb);

				set("version", bb[0]);
				bb = new byte[4];
				in.read(bb);
				Request req = new Request(bb, 0);
				headsize = req.readInt();
				bb = new byte[headsize];
				in.read(bb);
				req = new Request(bb, 0);

				set("pos", req.readLong());
				set("flag", req.readInt());
				set("expired", req.readLong());
				set("total", req.readLong());
				set("uid", req.readInt());
				set("id", req.readString());
				set("name", req.readString());

				this.in = in;

				return true;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			return false;
		}

		public boolean isShared() {
			return (getFlag() & 0x01) != 0;
		}

		private static Entity create(InputStream in) throws IOException {
			Entity e = new Entity();

			e.load(in);
			return e;
		}

		/**
		 * Update.
		 * 
		 * @param v
		 *            the v
		 * @return the int
		 */
		public int update(V v) {
			return Helper.update(getId(), v, Entity.class);
		}

		/**
		 * Move to.
		 * 
		 * @param folder
		 *            the folder
		 */
		public void moveTo(String folder) {

			File f1 = new File(path(this.getFolder(), getId()));
			File f2 = new File(path(folder, getId()));
			if (f2.exists()) {
				f2.delete();
			} else {
				f2.getParentFile().mkdirs();
			}
			if (!f1.renameTo(f2)) {
				// TODO
				log.error("rename file failed!!! dest=" + f2.getName() + ", src=" + f1.getName());
			}

			Helper.update(getId(), V.create("folder", folder), Entity.class);

		}

		/**
		 * Reset.
		 */
		public void reset() {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
			in = null;
		}
	}

	static private String path(String folder, String path) {
		long id = Math.abs(UID.hash(path));
		char p1 = (char) (id % 23 + 'a');
		char p2 = (char) (id % 19 + 'A');
		char p3 = (char) (id % 17 + 'a');
		char p4 = (char) (id % 13 + 'A');

		StringBuilder sb = new StringBuilder(ROOT);

		if (folder != null && "".equals(folder)) {
			sb.append("/").append(folder);
		}

		sb.append("/").append(p1).append("/").append(p2).append("/").append(p3).append("/").append(p4).append("/")
				.append(id);
		return sb.toString();
	}

	/**
	 * Cleanup.
	 */
	public static void cleanup() {
		File f = new File(ROOT);

		File[] fs = f.listFiles();
		if (fs != null) {
			for (File f1 : fs) {
				delete(f1);
			}
		}

	}

	private static void delete(File f) {
		if (f.isFile()) {
			if (System.currentTimeMillis() - f.lastModified() > X.ADAY) {
				// check the file is fine?
				Entity e = Repo.load(null, null, f);
				if (e.getTotal() > e.getPos()) {
					e.delete();
				}
			}
		} else if (f.isDirectory()) {
			File[] fs = f.listFiles();
			if (fs != null) {
				for (File f1 : fs) {
					delete(f1);
				}
			}

			/**
			 * delete the empty directory
			 */
			fs = f.listFiles();
			if (fs == null || fs.length == 0) {
				f.delete();
			}

		}
	}

	/**
	 * Load.
	 *
	 * @param q
	 *            the query and order
	 * @param s
	 *            the start number
	 * @param n
	 *            the number of items
	 * @return the beans
	 */
	public static Beans<Entity> load(W q, int s, int n) {
		return Helper.load(q, s, n, Entity.class);
	}

	private static AtomicLong total = new AtomicLong(0); // byte
	private static AtomicLong cost = new AtomicLong(0); // ms

	public static long getSpeed() {
		if (cost.get() > 0) {
			return total.get() * 1000L / cost.get();
		}
		return 0;
	}

	public static synchronized void test() {

		if (Global.getInt("repo.speed", 0) == 0) {
			return;
		}

		new Task() {

			@Override
			public String getName() {
				return "repo.speed";
			}

			@Override
			public void onFinish() {
				if (Global.getInt("repo.speed", 0) == 1) {
					this.schedule(X.AMINUTE);
				}
			}

			@Override
			public void onExecute() {
				total.set(0);
				cost.set(0);
				int n = 10;
				for (int i = 0; i < n; i++) {
					_test(n);
				}

				try {
					IOUtil.delete(new File(ROOT + "/test"));
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
			}
		}.schedule(2000);

	}

	private static long _test(int n) {

		OutputStream out = null;
		InputStream in = null;
		File f = new File(ROOT + "/test/" + n);
		try {
			TimeStamp t = TimeStamp.create();

			f.getParentFile().mkdirs();
			byte[] bb = new byte[32 * 1024];

			out = new FileOutputStream(f);
			for (int i = 0; i < 32; i++) {
				out.write(bb, 0, bb.length);
				out.flush();
			}
			out.close();
			out = null;
			in = new FileInputStream(f);
			while (in.read(bb, 0, bb.length) > 0)
				;

			in.close();
			in = null;

			total.addAndGet(n * bb.length);
			cost.addAndGet(t.pastms());

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			X.close(out, in);
		}
		return 0;
	}

}
