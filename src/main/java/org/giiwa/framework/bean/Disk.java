package org.giiwa.framework.bean;

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
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.base.Base32;
import org.giiwa.core.base.IOUtil;
import org.giiwa.core.base.Url;
import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.BeanDAO;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Table;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.conf.Config;
import org.giiwa.core.conf.Local;
import org.giiwa.core.dfile.DFile;
import org.giiwa.core.dfile.FileServer;
import org.giiwa.framework.bean.Node;
import org.giiwa.framework.web.Controller;
import org.giiwa.core.bean.Column;
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;
import org.giiwa.core.cache.Cache;

/**
 * Demo bean
 * 
 * @author joe
 * 
 */
@Table(name = "gi_disk")
public class Disk extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(Disk.class);

	public static BeanDAO<Long, Disk> dao = BeanDAO.create(Disk.class);

	public static Disk DEFAULT = new Disk();

	// private final static String RESETNAME = "disk.reset";

	@Column(name = X.ID)
	long id;

	@Column(name = "node")
	String node;

	@Column(name = "path")
	String path;

	@Column(name = "priority")
	int priority;

	@Column(name = "checktime")
	long checktime;

	@Column(name = "bad")
	int bad; // 0:ok, 1: bad

	@Column(name = "total")
	long total;

	@Column(name = "free")
	long free;

	@Column(name = "count")
	long count;

	public boolean isLocal() {
		return X.isSame(node, Local.id());
	}

	public boolean getLive() {
		return (bad != 1) && (this.getNode_obj() != null) && (this.getNode_obj().getState() == 1)
				&& (System.currentTimeMillis() - this.getNode_obj().getUpdated() < Node.LOST);
	}

	public long getId() {
		return id;
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
				file_obj.mkdirs();
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

	public long reloadTotal() {

		File f = this.getFile_obj();
		if (f.getTotalSpace() > 0) {
			return f.getTotalSpace();
		}

		try {
			FileStore s = this.getFilestore_obj();
			if (s != null) {
				return s.getTotalSpace();
			}
		} catch (Exception e) {
			log.error(path, e);
		}
		return 0;
	}

	public long reloadFree() {
		File f = this.getFile_obj();
		if (f.getFreeSpace() > 0) {
			return f.getFreeSpace();
		}

		try {
			FileStore s = this.getFilestore_obj();
			if (s != null) {
				return s.getUsableSpace();
			}
		} catch (Exception e) {
			log.error(path, e);
		}
		return 0;

	}

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

	public static DFile seek(Path filename) {
		return seek(filename.toString());
	}

	public static DFile get(String id) {
		return seek(new String(Base32.decode(id)));
	}

	public static DFile seek(String filename) {

		if (Helper.isConfigured()) {
			filename = X.getCanonicalPath(filename);

			Beans<Disk> bs = disks();

			for (Disk e : bs) {
				DFile d = DFile.create(e, filename);
				if (d.exists()) {
					return d;
				}
			}

			try {
				// log.debug("seek, not found, filename=" + filename, new Exception());
				DFile f = DFile.create(Disk.get(), filename);

				return f;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

		return null;

	}

	public static boolean exists(String filename) {

		Beans<Disk> bs = disks();

		for (Disk e : bs) {
			DFile d = DFile.create(e, filename);
			if (d.exists()) {
				return true;
			}
		}

		return false;
	}

	public static Disk get() throws IOException {
		Selector s = Selector.get();
		return s.pick();
	}

	public static Collection<DFile> list(String filename) {
		Map<String, DFile> l1 = new TreeMap<String, DFile>();

		Beans<Disk> bs = disks();

		for (Disk e : bs) {
			DFile f = DFile.create(e, filename);
			try {
				if (f.exists()) {
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
					log.info("exists? " + f.exists() + ", filename=" + f.getCanonicalPath() + ", path=" + e.path);
				}
			} catch (Exception e1) {
				log.error(e1.getMessage(), e1);
			}
		}

		return l1.values();
	}

	public static void delete(String filename, long age) {
		delete(filename, age, true);
	}

	public static void delete(String filename) {
		delete(filename, -1, true);
	}

	public static void delete(String filename, long age, boolean global) {

		Beans<Disk> bs = disks();

		for (Disk e : bs) {

			DFile f = DFile.create(e, filename);

			f.delete(age);

		}

	}

	public static long move(DFile src, DFile dest) throws IOException {
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

	public static long copy(DFile src, DFile dest, Consumer<String> moni) throws IOException {
		long len = 0;

		if (moni != null) {
			moni.accept(src.getFilename());
		}

		if (src.isDirectory()) {
			dest.mkdirs();

			DFile[] ff = src.listFiles();
			if (ff != null) {
				for (DFile f : ff) {
					len += copy(f, Disk.seek(dest.getFilename() + "/" + f.getName()), moni);
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

	private static Beans<Disk> disks() {
		Beans<Disk> b1 = Cache.get("disk");
		if (b1 == null) {
			W q = W.create().and("bad", 0).sort("priority", -1).sort("path", 1);
			b1 = dao.load(q, 0, 100);
			Cache.set("disk", b1, X.AMINUTE);
		}
		if (b1 == null || b1.isEmpty()) {
			b1 = Beans.create();
			b1.add(Disk.DEFAULT);
		}
		return b1;
	}

	public static void reset() {
		Cache.remove("disk");
	}

	public String getNode() {
		return node;
	}

	private transient Node node_obj;

	public Node getNode_obj() {
		if (node_obj == null) {
			node_obj = Node.dao.load(node);
		}
		return node_obj;
	}

	public static void repair() {

		{
			DEFAULT = new Disk();
			DEFAULT.path = Controller.GIIWA_HOME + "/data";
			DEFAULT.node_obj = new Node();
			Url u = Url.create(Config.getConf().getString("dfile.bind", FileServer.URL));
			String s1 = u.getProtocol() + "://" + (X.isSame(u.getIp(), "0.0.0.0") ? "127.0.0.1" : u.getIp()) + ":"
					+ u.getPort();

			DEFAULT.node_obj.set("url", s1);
		}

		if (Helper.isConfigured()) {
			int s = 0;
			W q = W.create().sort("created", 1);
			Beans<Disk> bs = dao.load(q, s, 10);
			if (bs == null || bs.isEmpty()) {
				// add a default
				try {
//					File f = new File(Controller.GIIWA_HOME + "/data");
//					if (!f.exists()) {
//						f.mkdirs();
//					}
//					Disk.create(
//							V.create("path", f.getCanonicalPath()).append("priority", 1).append("node", Local.id()));

				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		}
	}

	public void check() {

		V v = V.create("checktime", System.currentTimeMillis());
		v.append("total", this.reloadTotal()).append("free", this.reloadFree());
		dao.update(id, v);

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
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
		private static Selector _inst;

		long pos = 0;
		Map<Disk, Long[]> ss = new HashMap<Disk, Long[]>();

		static synchronized Selector get() {

			if (_inst == null || System.currentTimeMillis() - _inst.age > X.AMINUTE) {
				_inst = new Selector();

				W q = W.create().sort("priority", -1).sort("path", 1);
				Beans<Disk> bs = dao.load(q, 0, 1000);
				for (Disk e : bs) {
					long f1 = e.free * e.priority;
					_inst.add(e, f1);
				}
			}

			return _inst;
		}

		private void add(Disk d, long rate) {
			ss.put(d, new Long[] { pos, pos + rate });
			pos += rate;
		}

		Disk pick() throws IOException {
			if (ss.size() == 1)
				return ss.keySet().iterator().next();

			long r = (long) (pos * Math.random());
			for (Disk d : ss.keySet()) {
				Long[] ll = ss.get(d);
				if (r >= ll[0] && r < ll[1]) {
					return d;
				}
			}
//			return DEFAULT;
			throw new IOException("no disk, ss=" + ss.size() + ", pos=" + pos);
		}

	}

	public static long getTotalSpace() {
		long total = 0;
		if (Helper.isConfigured()) {
			int s = 0;
			W q = W.create().sort("created", 1);
			Beans<Disk> bs = dao.load(q, s, 10);
			while (bs != null && !bs.isEmpty()) {

				for (Disk e : bs) {
					total += e.total;
				}
				s += bs.size();
				bs = dao.load(q, s, 10);
			}
		}
		return total;
	}

	public static long getFreeSpace() {
		long total = 0;
		if (Helper.isConfigured()) {
			int s = 0;
			W q = W.create().sort("created", 1);
			Beans<Disk> bs = dao.load(q, s, 10);
			while (bs != null && !bs.isEmpty()) {

				for (Disk e : bs) {
					total += e.free;
				}
				s += bs.size();
				bs = dao.load(q, s, 10);
			}
		}
		return total;
	}

}
