package org.giiwa.bean;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.conf.Config;
import org.giiwa.dao.Bean;
import org.giiwa.dao.BeanDAO;
import org.giiwa.dao.Beans;
import org.giiwa.dao.Column;
import org.giiwa.dao.Helper;
import org.giiwa.dao.Table;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.dfile.DFile;
import org.giiwa.dfile.LocalDFile;
import org.giiwa.dfile.NfsDFile;
import org.giiwa.misc.Base32;
import org.giiwa.misc.IOUtil;

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

	@Column(memo = "唯一序号")
	public long id;

	@Column(memo = "节点", value = "nfs://host:9091, local://, minio://")
	public String url;

	@Column(memo = "磁盘位置")
	public String path;

	@Column(memo = "挂载点", value = "/temp, ...")
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

	@Column(memo = "文件数")
	public long count;

	public long getTotal() {
		return total;
	}

	public long getFree() {
		return free;
	}

//	public boolean isLocal() {
//		return X.isSame(node, Local.id());
//	}
//
//	public boolean getLive() {
//		boolean b = (this.getNode_obj() != null) && (this.getNode_obj().getState() == 1);
//		log.debug("disk alive=" + b + ", last="
//				+ (this.getNode_obj() == null ? null : (System.currentTimeMillis() - this.getNode_obj().getUpdated())));
//		return b;
//	}

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
		long id = UID.next("disk.id");
		try {
			while (dao.exists(id)) {
				id = UID.next("disk.id");
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

		DFile f = null;

		if (Helper.isConfigured() && !X.isEmpty(filename)) {

			filename = X.getCanonicalPath(filename);

			Beans<Disk> bs = disks();

			if (bs != null) {
				for (Disk e : bs) {
					DFile d = e.create(filename);
					if (d != null && d.exists()) {
						if (f == null) {
							f = d;
						} else {
							f.merge(d);
						}
					}
				}
			}

			if (f != null) {
				return f;
			}

		}

		Disk d1 = Disk._get(filename);
		if (d1 == null) {
			return null;
		}

		f = d1.create(filename);
		return f;

	}

	private static Disk _get(String filename) throws IOException {
		Selector s = Selector.get();
		return s.pick(filename);
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
		try {

			if (!filename.startsWith("/")) {
				filename = "/" + filename;
			}

			if (url.startsWith("local://")) {
				return LocalDFile.create(this, filename);
			} else if (url.startsWith("nfs://")) {
				return NfsDFile.create(this, filename);
//		} else if (url.startsWith("minio://") || url.startsWith("minios://")) {
//			return MinioDFile.create(this, filename);
			}
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public static Collection<DFile> list(String filename) throws Exception {

		Map<String, DFile> l1 = new TreeMap<String, DFile>();

		Beans<Disk> bs = disks();

		if (bs != null) {
			for (Disk e : bs) {
				DFile f = e.create(filename);
				try {
					if (f != null && f.exists()) {
						DFile[] ff = f.listFiles();
						if (ff != null) {
							for (DFile f1 : ff) {
								String name = (f1.isDirectory() ? 0 : 1) + f1.getName();
								DFile f2 = l1.get(name);
								if (f2 == null || f1.lastModified() > f2.lastModified()) {
									l1.put(name, f1);
								}
							}
						}
						if (log.isDebugEnabled())
							log.debug("l1=" + l1);
					} else {
						log.info("exists? " + f.exists() + ", filename=" + f.getFilename() + ", path=" + e.path);
					}
				} catch (Exception e1) {
					log.error(e1.getMessage(), e1);
				}
			}
		}

		return l1.values();
	}

	public static void delete(String filename, long age) throws Exception {
		delete(filename, age, true);
	}

	public static void delete(String filename) throws Exception {
		delete(filename, -1, true);
	}

	public static void delete(String filename, long age, boolean global) throws Exception {

		Beans<Disk> bs = disks();

		for (Disk e : bs) {

			DFile f = e.create(filename);
			if (f != null) {
				f.delete(age);
			}

		}

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
			W q = W.create().and("enabled", 1).sort("priority", -1).sort("path", 1);
			_disks = dao.load(q, 0, 100);
		}

		if (X.isEmpty(_disks)) {
			Disk.repair();
		}

		return _disks;
	}

	public static void reset() {
		if (_disks != null) {
			_disks.clear();
		}
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
					File f = new File(Config.getConf().getString("dfile.home", "/home/disk1"));
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

	static class Selector {

		private long age = System.currentTimeMillis();
		private static Selector _inst = null;

		List<Disk> ss1 = new ArrayList<Disk>(); // sort by mount

		static synchronized Selector get() {

			if (_inst == null || System.currentTimeMillis() - _inst.age > X.AMINUTE) {
				_inst = new Selector();

				W q = W.create().sort("priority", -1).sort("path", 1);
				try {
					Beans<Disk> bs = dao.load(q, 0, 1000);
					if (bs != null) {
						for (Disk e : bs) {
//					long f1 = e.free * e.priority;
//					_inst.add(e, f1);
							_inst.ss1.add(e);
						}
					}
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}

				Collections.sort(_inst.ss1, new Comparator<Disk>() {

					@Override
					public int compare(Disk o1, Disk o2) {
						long m1 = o1.mount.length();
						long m2 = o2.mount.length();
						if (m1 > m2) {
							return -1;
						} else if (m1 < m2) {
							return 1;
						}
						return 0;
					}

				});

			}

			return _inst;
		}

//		private void add(Disk d, long rate) {
//			d.set("_rate", rate);
//			d.set("_len", d.mount.length());
//
//			ss1.add(d);
//
//		}

		Disk pick(String filename) throws IOException {

//			List<Disk> dd = new ArrayList<Disk>();
//			int n = Global.getInt("dfile.copies", 0) + 1;

			for (Disk d1 : ss1) {
				if (filename.startsWith(d1.mount)) {
					return d1;
//					dd.add(d1);
				}
//				if (dd.size() >= n) {
//					break;
//				}
			}

			if (ss1 == null || ss1.isEmpty()) {
				return null;
			}

			return ss1.get(ss1.size() - 1);

//			if (dd.size() < n) {
//				for (Disk d1 : ss1) {
//					if (!dd.contains(d1)) {
//						dd.add(d1);
//					}
//					if (dd.size() >= n) {
//						break;
//					}
//				}
//			}
//
//			return dd.toArray(new Disk[dd.size()]);
		}

	}

	/**
	 * @return
	 */
	public static long getTotalSpace() {

		long[] total = new long[] { 0 };
		if (Helper.isConfigured()) {
			W q = W.create().sort("created", 1);
			try {
				dao.stream(q, 0, e -> {
//					if (e.getNode_obj().isAlive()) {
					total[0] += e.total;
//					}
					return true;
				});
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return total[0];
	}

	/**
	 * @return
	 */
	public static long getFreeSpace() {

		long[] total = new long[] { 0 };
		if (Helper.isConfigured()) {
			W q = W.create().sort("created", 1);
			try {
				dao.stream(q, 0, e -> {
//					if (e.getNode_obj().isAlive()) {
					total[0] += e.free;
//					}
					return true;
				});
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return total[0];

	}

}
