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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.conf.Config;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.Bean;
import org.giiwa.dao.BeanDAO;
import org.giiwa.dao.Beans;
import org.giiwa.dao.Column;
import org.giiwa.dao.Helper;
import org.giiwa.dao.Table;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.dfile.DFile;
import org.giiwa.dfile.HdfsDFile;
import org.giiwa.dfile.LocalDFile;
import org.giiwa.dfile.NfsDFile;
import org.giiwa.dfile.SmbDFile;
import org.giiwa.misc.Base32;
import org.giiwa.misc.IOUtil;
import org.giiwa.task.Task;

/**
 * Demo bean
 * 
 * @author joe
 * 
 */
@Table(name = "gi_disk", memo = "GI-磁盘")
public final class Disk extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(Disk.class);

	public static final BeanDAO<Long, Disk> dao = BeanDAO.create(Disk.class);

	@Column(memo = "主键", unique = true)
	public long id;

	@Column(memo = "节点", value = "nfs://host:9091, local://, minio://", size = 100)
	public String url;

	@Column(memo = "磁盘位置", size = 100)
	public String path;

	@Column(memo = "挂载点", value = "/temp, ...", size = 100)
	public String mount;

//	private int _len;

	@Column(memo = "优先级")
	public int priority;

	public String code;

	@Column(memo = "检查时间")
	long checktime;

	@Column(memo = "开关")
	public int enabled; // 1: ok, 0: disabled

	@Column(memo = "总空间")
	public long total;

	@Column(memo = "可用空间")
	public long free;

	@Column(memo = "状态", value = "1:good, 0:bad")
	int state;

	@Column(memo = "配额", value = "Byte")
	long quota;

	@Column(name = "_domain", memo = "域名", size = 50)
	public String domain;

	@Column(memo = "用户名", size = 50)
	public String username;

	@Column(memo = "密码", size = 100)
	public String password;

	String _error;

	@Column(memo = "文件数")
	public long count;

	@Column(memo = "读KBPS")
	long stat_read_avg;

	@Column(memo = "写KBPS")
	long stat_write_avg;

	public long getTotal() {
		return total;
	}

	public long getFree() {
		return free;
	}

	public int getPriority() {
		return priority;
	}

	public String getPath() {
		return path;
	}

	public static long create(V v) {
		/**
		 * generate a unique id in distribute system
		 */
		try {
			long id = dao.next();
			while (dao.exists(id)) {
				id = dao.next();
			}
			dao.insert(v.force(X.ID, id));
			return id;
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
		}
		return -1;
	}

	transient File file_obj;

	public File getFile_obj() {
		if (file_obj == null) {
			file_obj = new File(path);
			if (!file_obj.exists()) {
				X.IO.mkdirs(file_obj);
			}
		}
		return file_obj;
	}

	transient FileStore filestore_obj;

	public FileStore getFilestore_obj() throws IOException {
		if (filestore_obj == null) {
			filestore_obj = Files.getFileStore(Paths.get(this.getFile_obj().toURI()).toRealPath());
		}
		return filestore_obj;
	}

	// ---------------
	public long getUsed() {

		return this.total - this.free;
	}

	public long reloadCount() {
		// scan files
		return IOUtil.count(this.getFile_obj());
	}

	public int getUsage() {

		if (this.total > 0) {
			return (int) ((this.total - this.free) * 100 / this.total);
		}
		return 0;
	}

	@Override
	public String toString() {
		return "Disk [id=" + id + ", url=" + url + ", path=" + path + ", mount=" + mount + ", enabled=" + enabled
				+ ", state=" + state + "]";
	}

	public static DFile seek(Path filename) throws Exception {
		return seek(filename.toString());
	}

	public static DFile get(String id) throws Exception {
		return seek(new String(Base32.decode(id)));
	}

	/**
	 * @deprecated
	 * @param url
	 * @return
	 * @throws Exception
	 */
	public static DFile getByUrl(String url) throws Exception {
		return seek(url);
	}

	public static DFile seek(String filename) throws IOException {

		DFile f = null;// TimingCache.get(DFile.class, filename);
//		if (f != null) {
//			return f;
//		}

		Beans<Disk> bs = null;
		TimeStamp t = TimeStamp.create();
		try {
			if (filename.startsWith("/f/g/") || filename.startsWith("/f/d/")) {
				String[] ss = X.split(filename, "/");
				if (ss.length > 2) {
					filename = new String(Base32.decode(ss[2]));
				}
			} else if (filename.startsWith("/ghp/") || filename.startsWith("/wsf/")) {
				String[] ss = X.split(filename, "/");
				if (ss.length > 1) {
					filename = new String(Base32.decode(ss[1]));
				}
			} else if (filename.startsWith("/f/s/")) {
				filename = filename.substring(4);
			}

			if (!filename.startsWith("/")) {
				// trying to decode
				try {
					filename = new String(Base32.decode(filename));
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}

			if (Helper.isConfigured() && !X.isEmpty(filename)) {

				filename = X.getCanonicalPath(filename);

				bs = disks();

				if (bs != null) {
					for (Disk e : bs) {
						if (e.isOk(filename)) {
							if (e.isMount(filename)) {
								// is mount point, should create path in "/" disk
								Beans<Disk> l1 = disks("/");
//							log.warn("ismount, created=" + filename + ", disks=" + l1);
								if (l1 != null) {
									for (Disk e1 : l1) {
										DFile d = e1.create(filename);
										try {
											if (d != null && !d.exists()) {
												d.mkdirs();
											}
										} catch (Exception err) {
											log.error(filename, err);
										}
									}
								}
							}

							DFile d = e.create(filename);
							try {
								if (d != null && d.exists()) {
									if (f == null) {
										f = d;
									} else {
										f.merge(d);
									}
								}
							} catch (Exception e1) {
								// mark e bad
								e.bad(e1.getMessage());
							}
						}
					}
				}

				if (f != null) {
					return f.last();
				}

			}

			Disk d1 = _pickup(filename);
			if (d1 == null) {
				return null;
			}

			f = d1.create(filename);

//			TimingCache.set(DFile.class, filename, f);

			return f;
		} finally {
			if (t.pastms() > 1000) {
				log.warn("cost " + t.past() + ", filename=" + filename + ", disks=" + bs);
			}
		}

	}

	public boolean isMount(String filename) {
		if (m == null) {
			String s1 = "^" + mount + "$";
			m = Pattern.compile(s1);
		}

		if (!filename.endsWith("/")) {
			filename += "/";
		}

		Matcher m1 = m.matcher(filename);
		return m1.find();
	}

	private void bad(String error) {
		dao.update(id, V.create().append("state", 0).append("_error", error + "/" + Local.label()));
	}

	public static boolean exists(String filename) throws Exception {

		Beans<Disk> bs = disks();

		for (Disk e : bs) {
			DFile d = e.create(filename);
			if (d != null && d.exists()) {
				return true;
			}
		}

		return false;
	}

	public DFile create(String filename) {

		if (!filename.startsWith("/")) {
			filename = "/" + filename;
		}

		if (url.startsWith("local://")) {
			return LocalDFile.create(this, filename);
		} else if (url.startsWith("nfs://")) {
			return NfsDFile.create(this, filename);
		} else if (url.startsWith("smb://")) {
			return SmbDFile.create(this, filename);
		} else if (url.startsWith("hdfs://")) {
			return HdfsDFile.create(this, filename);
		}

		return null;
	}

	public static Collection<DFile> list(String filename) throws IOException {

		Map<String, DFile> l1 = new TreeMap<String, DFile>();

		Beans<Disk> bs = disks();

		if (bs != null) {

			if (log.isDebugEnabled()) {
				log.debug("disks=" + bs);
			}

			for (Disk e : bs) {
				if (e.isOk(filename)) {
					DFile f = e.create(filename);
					try {
						if (f != null && f.exists()) {
							if (f.isFile()) {
								l1.put(f.getName(), f);
							} else {
								DFile[] ff = f.listFiles();
								if (ff != null) {
									for (DFile f1 : ff) {
										String name = f1.getName();
										DFile f2 = l1.get(name);
										if (f2 == null || f1.lastModified() > f2.lastModified()) {
											// 放最近修改的
											l1.put(name, f1);
										}
									}
								}
							}
							if (log.isDebugEnabled())
								log.debug("l1=" + l1);
						}
					} catch (Exception e1) {
						log.error(e1.getMessage(), e1);
					}
				}
			}
		}

		return l1.values();
	}

	public static void delete(String filename, long age) throws Exception {
		delete(filename, age, true);
	}

	public static boolean delete(String filename) throws IOException {
		if (log.isInfoEnabled()) {
			log.info("delete " + filename);
		}
		return delete(filename, -1, true);
	}

	public static boolean delete(String filename, long age, boolean global) throws IOException {

		boolean done = false;
		Beans<Disk> bs = disks();

		for (Disk e : bs) {
			DFile f = e.create(filename);
			if (f != null) {
				if (f.delete(age)) {
					done = true;
				}
			}
		}
		return done;
	}

	public static long move(DFile src, DFile dest) throws Exception {

		long len = 0;
		if (src.isDirectory()) {
			// for
			dest.mkdirs();
			DFile[] ff = src.listFiles();
			if (ff != null) {
				for (DFile f : ff) {
					len += move(f, Disk.seek(dest.getFilename() + "/" + f.getName()));
				}
			}
			src.delete();
		} else {
			len = IOUtil.copy(src.getInputStream(), dest.getOutputStream());
			src.delete();
		}
		return len;
	}

	public static long copy(DFile src, DFile dest) throws Exception {

		long len = 0;

		if (src.isDirectory()) {
			dest.mkdirs();

			DFile[] ff = src.listFiles();
			if (ff != null) {
				for (DFile f : ff) {
					len += copy(f, Disk.seek(dest.getFilename() + "/" + f.getName()));
				}
			}
		} else {
			len = copy(src.getInputStream(), dest.getOutputStream());
		}
		return len;
	}

	public static long copy(InputStream in, OutputStream out) throws IOException {
		return IOUtil.copy(in, out);
	}

	private static Beans<Disk> _disks = null;

	private static Beans<Disk> disks() {

		if (X.isEmpty(_disks) || System.currentTimeMillis() - _disks.created > X.AMINUTE) {
			W q = W.create().and("enabled", 1).and("state", 1);
			_disks = dao.load(q, 0, 128);
		}

		if (X.isEmpty(_disks)) {
			Disk.repair();
		}

		return _disks;
	}

	private static Beans<Disk> disks(String mount) {
		W q = W.create().and("mount", mount).and("enabled", 1).and("state", 1);
		return dao.load(q, 0, 128);
	}

	public static void reset() {

		log.warn("disk reset!");

		if (_disks != null) {
			_disks.clear();
		}

		check0();

	}

	public static void repair() {

		if (Helper.isConfigured()) {
			// migrate
			try {

				dao.stream(W.create(), 0, e -> {
					if (X.isEmpty(e.url) || e.create("/") == null) {
						dao.delete(e.id);
					}
					return true;
				});

				if (!dao.exists(W.create())) {
					// add a default
					File f = new File(Config.getConf().getString("dfile.home", "/data/disk1"));
					if (!f.exists()) {
						X.IO.mkdirs(f);
					}
					Disk.create(V.create("path", f.getCanonicalPath()).append("url", "local://").append("enabled", 1)
							.append("mount", "/").append("priority", 1));

				}

			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Disk other = (Disk) obj;
		if (id != other.id)
			return false;
		return true;
	}

	public boolean isOk(String filename) {

		_check();

		Matcher m1 = p.matcher(filename);
		return m1.find();

	}

	private static Disk _pickup(String filename) throws IOException {

		if (_disks == null || _disks.isEmpty()) {
			return null;
		}

		long max = 0;
		Disk d = null;
		int matches = 0;

		for (Disk d1 : _disks) {
			if (d1.isOk(filename)) {
				int len = d1.mount.length();
				if (len > matches) {
					long m = d1.getFree() * d1.priority;
					max = m;
					d = d1;
					matches = len;
				} else if (len == matches) {
					long m = d1.getFree() * d1.priority;
					if (m > max) {
						max = m;
						d = d1;
					}
				}
			}
		}

		return d;

	}

	public static void check0() {
		if (X.isIn(Config.getConf().getString("disk.check", "yes"), "yes", "1") && !check0.isScheduled()) {
			check0.schedule(0);
		}
	}

	private static Task check0 = new Task() {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public String getName() {
			return "disk.check0";
		}

		public void onExecute() {

			Beans<Disk> l1 = dao.load(W.create(), 0, 128);
			log.info("disk checking ... n=" + l1.size());

			for (Disk e : l1) {
				V v = V.create();
				if (e.enabled == 1) {

//					log.info("disk checking: " + e.url);

					if (!e.mount.endsWith("/")) {
						e.mount += "/";
						v.append("mount", e.mount);
					}

					DFile f1 = e.create("/");
					try {
						if (f1.exists()) {
							long free = f1.getFreeSpace();
							long total = f1.getTotalSpace();
							long used = total - free;

							if (e.quota > 0) {
								total = Math.min(e.quota, f1.getTotalSpace());
								free = total - used;
								if (free < 0) {
									free = 0;
								}
							}

							v.append("state", free > 0 ? 1 : 0);
							v.append("total", total);
							v.append("free", free);

						} else {
							throw new Exception("[" + e.path + "] not found!");
						}
					} catch (Exception e1) {
						log.error(e1.getMessage(), e1);
						v.append("_error", e1.getMessage() + "/" + Local.label());
						v.append("state", 0);
						GLog.applog.error("sys", "disk", e1.getMessage(), e1);
					}
				} else {
					v.append("state", 0);
				}

				dao.update(e.id, v);
			}
		}

		@Override
		public void onFinish() {
			this.schedule(10000); // 10 seconds
		}

	};

	public static long getTotalSpace() {
		Beans<Disk> bs = disks();
		long total = 0;
		if (bs != null) {
			for (Disk e : bs) {
				total += e.getTotal();
			}
		}
		return total;
	}

	public static long getFreeSpace() {
		Beans<Disk> bs = disks();
		long total = 0;
		if (bs != null) {
			for (Disk e : bs) {
				total += e.getFree();
			}
		}
		return total;
	}

	public static class Counter {

		static Map<Long, Counter> read = new HashMap<Long, Counter>();
		static Map<Long, Counter> write = new HashMap<Long, Counter>();

		long bytes;
		long cost;

		public static Counter read(Disk d) {
			Counter e = read.get(d.id);
			if (e == null) {
				e = new Counter();
				read.put(d.id, e);
			}
			return e;
		}

		public static Counter write(Disk d) {
			Counter e = write.get(d.id);
			if (e == null) {
				e = new Counter();
				write.put(d.id, e);
			}
			return e;
		}

		public synchronized void add(long bytes, long cost) {
			this.bytes += bytes;
			this.cost += cost;
		}

		public synchronized long avg() {

			if (cost > 0) {
				try {
					return bytes * 1000 / cost; // KB/s
				} finally {
					cost = 0;
					bytes = 0;
				}
			}
			return 0;
		}

	}

	// stat
	public static void stat() {

		Lock door = Global.getLock("giiwa.disk.stat");

		if (door.tryLock()) {
			try {
				Beans<Disk> l1 = dao.load(W.create(), 0, 128);

				for (Disk e : l1) {

					V v = V.create();
					Counter str = Counter.read(e);
					long r = str.avg();
					if (r > 0) {
						v.append("stat_read_avg", r);
					}

					Counter stw = Counter.write(e);
					long w = stw.avg();
					if (w > 0) {
						v.append("stat_write_avg", w);
					}

					if (!v.isEmpty()) {

						dao.update(e.id, v);

						Stat.snapshot(System.currentTimeMillis(), "disk.stat", W.create().and("dataid", e.id),
								V.create().append("dataid", e.id), new long[] { r, w });
					}
				}
			} finally {
				door.unlock();
			}
		}

	}

	private Pattern p = null;
	private Pattern m = null;

	public String filename(String filename) {

		_check();

		Matcher m1 = p.matcher(filename);

		if (m1.find()) {
			filename = m1.group(m1.groupCount());
		}
		if (!filename.startsWith("/")) {
			filename = "/" + filename;
		}

		return filename;
	}

	private void _check() {
		if (p == null) {
			String s1 = "^" + mount + "(.*)";
			p = Pattern.compile(s1);
		}
	}

}
