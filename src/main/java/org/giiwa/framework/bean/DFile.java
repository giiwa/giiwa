package org.giiwa.framework.bean;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.base.IOUtil;
import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.BeanDAO;
import org.giiwa.core.bean.Column;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.bean.Table;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;
import org.giiwa.core.dfile.DFileInputStream;
import org.giiwa.core.dfile.DFileOutputStream;
import org.giiwa.core.dfile.FileClient;
import org.giiwa.core.json.JSON;

/**
 * Demo bean
 * 
 * @author joe
 * 
 */

@Table(name = "gi_dfile")
public class DFile extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public final static BeanDAO<Long, DFile> dao = BeanDAO.create(DFile.class);

	private static Log log = LogFactory.getLog(DFile.class);

	@Column(name = X.ID)
	private long id;

	@Column(name = "disk")
	private long disk;

	@Column(name = "filename")
	private String filename;

	private String ip;
	private int port;

	private transient String path;
	private transient Node node_obj;
	private transient Disk disk_obj;
	private transient JSON info;

	private transient File file_obj;

	public String getFilename() {
		return filename;
	}

	public long getId() {
		return id;
	}

	public Node getNode_obj() {
		if (node_obj == null) {
			check();
		}
		return node_obj;
	}

	public Disk getDisk_obj() {
		if (disk_obj == null) {
			check();
		}
		return disk_obj;
	}

	public File getFile_obj() {
		if (file_obj == null) {
			check();
		}
		return file_obj;
	}

	public boolean check() {

		if (disk_obj == null && disk > 0) {
			disk_obj = Disk.dao.load(disk);
		}

		if (disk_obj != null) {
			path = disk_obj.getPath();
			node_obj = disk_obj.getNode_obj();

			if (node_obj != null) {
				ip = node_obj.getIp();
				port = node_obj.getPort();

				if (file_obj == null && node_obj.isLocal()) {
					file_obj = new File(path + "/" + filename);
				}

				return true;
			}
		}

		return false;
	}

	public boolean exists() {
		check();

		if (file_obj != null) {
			return file_obj.exists();
		}

		getInfo();
		return info == null ? false : info.getInt("e") == 1;
	}

	public String getAbsolutePath() {
		return X.getCanonicalPath(path + "/" + filename);
	}

	public boolean delete() {
		check();

		try {
			if (file_obj != null) {
				return IOUtil.delete(file_obj) > 0;
			}

			return FileClient.get(ip, port).delete(path, filename);

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			dao.delete(id);
		}

		return false;
	}

	public InputStream getInputStream() throws FileNotFoundException {
		check();

		if (file_obj != null) {
			return new FileInputStream(file_obj);
		}

		return DFileInputStream.create(this.getDisk_obj(), filename);
	}

	public OutputStream getOutputStream() throws FileNotFoundException {

		check();

		try {
			if (!DFile.dao.exists(W.create().and("disk", disk).and("filename", filename))) {
				DFile.create(V.create().append("disk", disk).append("filename", filename));
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		if (file_obj != null) {
			file_obj.getParentFile().mkdirs();
			return new FileOutputStream(file_obj);
		}

		return DFileOutputStream.create(this.getDisk_obj(), filename);
	}

	public boolean mkdirs() {
		check();

		if (file_obj != null) {
			return file_obj.mkdirs();
		}

		try {
			return FileClient.get(ip, port).mkdirs(path, this.filename);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return true;
	}

	public DFile getParentFile() {
		String[] ss = X.split(filename, "/");
		return create(disk_obj, ss[ss.length - 1]);
	}

	private JSON getInfo() {
		if (info == null) {
			try {
				info = FileClient.get(ip, port).info(path, filename);
			} catch (InterruptedException e) {
				log.error(e.getMessage(), e);
			}
		}
		return info;
	}

	public boolean isDirectory() {
		check();

		if (file_obj != null) {
			return file_obj.isDirectory();
		}

		getInfo();
		return info == null ? false : info.getInt("f") != 1;
	}

	public boolean isFile() {
		check();

		if (file_obj != null) {
			return file_obj.isFile();
		}

		getInfo();
		return info == null ? false : info.getInt("f") == 1;
	}

	public String getName() {
		String[] ss = X.split(filename, "[ ]");
		return ss[ss.length - 1];
	}

	public DFile[] listFiles() throws Exception {
		check();

		if (file_obj != null) {
			if (file_obj.exists()) {
				File[] f1 = file_obj.listFiles();
				if (f1 != null && f1.length > 0) {
					DFile[] f2 = new DFile[f1.length];
					for (int i = 0; i < f1.length; i++) {
						File f = f1[i];
						String filename = f.getCanonicalPath().replace(path, "");
						f2[i] = DFile.create(disk_obj, filename);
					}
					return f2;
				}
			}
			return null;
		}

		JSON jo = FileClient.get(ip, port).list(path, filename);
		Collection<JSON> l1 = jo.getList("list");

		String s1 = X.getCanonicalPath("/" + filename);
		int j = s1.lastIndexOf("/");
		String p1 = s1.substring(0, j + 1);

		DFile[] l2 = new DFile[l1.size()];
		int i = 0;

		for (JSON j1 : l1) {
			DFile d1 = DFile.create(disk_obj, p1 + j1.getString("name"), j1);
			l2[i++] = d1;
		}

		return l2;
	}

	public long lastModified() {

		check();

		if (file_obj != null) {
			return file_obj.lastModified();
		}
		getInfo();
		return info == null ? 0 : info.getLong("u");
	}

	public String getCanonicalPath() {
		return filename;
	}

	public long length() {
		check();

		if (file_obj != null) {
			return file_obj.length();
		}

		getInfo();

		return info == null ? 0 : info.getLong("l");
	}

	public boolean renameTo(DFile file) {
		// TODO

		try {
			if (X.isSame(ip, file.ip)) {
				return FileClient.get(ip, port).move(path, filename, file.path, file.filename);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return false;
	}

	public static DFile create(Disk d, String filename) {
		return create(d, filename, null);
	}

	public static DFile create(Disk d, String filename, JSON info) {
		DFile e = new DFile();

		e.disk = d.id;
		e.filename = filename;

		e.disk_obj = d;
		e.node_obj = d.getNode_obj();
		e.ip = d.getNode_obj().getIp();
		e.port = d.getNode_obj().getPort();
		e.path = d.getPath();
		e.info = info;

		if (e.node_obj != null && e.node_obj.isLocal()) {
			e.file_obj = new File(d.getPath() + "/" + filename);
		}

		return e;

	}

	public static long create(V v) {
		/**
		 * generate a unique id in distribute system
		 */
		long id = UID.next("dfile.id");
		try {
			while (dao.exists(id)) {
				id = UID.next("dfile.id");
			}
			dao.insert(v.force(X.ID, id));
			return id;
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
		}
		return -1;
	}

	public void create() {

	}

}
