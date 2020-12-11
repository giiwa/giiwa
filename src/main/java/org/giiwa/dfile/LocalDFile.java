package org.giiwa.dfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Disk;
import org.giiwa.bean.Node;
import org.giiwa.dao.X;
import org.giiwa.misc.Base32;
import org.giiwa.misc.IOUtil;

/**
 * 
 * Local File System
 * 
 * @author joe
 * 
 */

public class LocalDFile extends DFile {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(LocalDFile.class);

	private long disk;

	private String filename;

	private String url;

	private transient String path;
	private transient Node node_obj;
	private transient Disk disk_obj;
	private transient FileInfo info;

	public String getFilename() {
		return filename;
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

	public boolean check() {

		if (disk_obj == null && disk > 0) {
			disk_obj = Disk.dao.load(disk);

			if (disk_obj != null) {
				path = disk_obj.getPath();
				node_obj = disk_obj.getNode_obj();

				if (node_obj != null) {
					url = node_obj.getUrl();
					return true;
				}
			}

		}

		return disk > 0;
	}

	public boolean exists() {

		check();

		getInfo();
		return info != null && info.exists;

	}

	public String getAbsolutePath() {
		return X.getCanonicalPath(path + "/" + filename);
	}

	public boolean delete() {
		return delete(-1);
	}

	public String getId() {
		return Base32.encode(this.getFilename().getBytes());
	}

	public boolean delete(long age) {
		check();

		try {

			File f = new File(path + File.separator + filename);
			return IOUtil.delete(f, age) > 0;

		} catch (Exception e) {
			log.error(url, e);

//			Disk.dao.update(this.disk, V.create("bad", 1));

		} finally {
			// dao.delete(W.create("disk", disk).and("filename", filename));
		}

		return false;
	}

	public InputStream getInputStream() {

		try {
			check();

			return new FileInputStream(new File(path + File.separator + filename));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public OutputStream getOutputStream() throws FileNotFoundException {
		return this.getOutputStream(0);
	}

	public OutputStream getOutputStream(long offset) throws FileNotFoundException {

		check();

		return DFileOutputStream.create(this.getDisk_obj(), filename, offset, (o1, bb, len) -> {

			RandomAccessFile a = null;

			try {
				if (bb != null) {

					File f = new File(path + File.separator + filename);

					if (!f.exists()) {
						f.getParentFile().mkdirs();
						f.createNewFile();
					}
					a = new RandomAccessFile(f, "rws");
					a.seek(offset);
					a.write(bb, 0, len);
				}

				return o1 + len;

			} catch (Exception e) {
				log.error(e.getMessage(), e);
			} finally {
				X.close(a);
			}

			return o1;
		});
	}

	public boolean mkdirs() {
		check();

		try {
			File f = new File(path + File.separator + filename);
			return f.mkdirs();
		} catch (Exception e) {
			log.error(url, e);
//			Disk.dao.update(this.disk, V.create("bad", 1));
		}
		return true;
	}

	public DFile getParentFile() {
		int i = filename.lastIndexOf("/", filename.length() - 1);
		if (i > 0) {
			return create(disk_obj, filename.substring(0, i));
		} else if (i == 0) {
			return create(disk_obj, "/");
		} else {
			return null;
		}
	}

	private FileInfo getInfo() {
		if (info == null) {
			try {

				File f = new File(path + File.separator + filename);

//				log.debug("f=" + f.getAbsolutePath());

				info = new FileInfo();
				info.exists = f.exists() ? true : false;
				info.isfile = f.isFile() ? true : false;
				info.length = f.length();
				info.lastmodified = f.lastModified();

			} catch (Exception e) {
				log.error(url, e);
//				Disk.dao.update(this.disk, V.create("bad", 1));

			}
		}
		return info;
	}

	public boolean isDirectory() {
		check();

		getInfo();
		return info != null && !info.isfile;
	}

	public boolean isFile() {
		check();

		getInfo();
		return info != null && info.isfile;
	}

	public String getName() {
		String[] ss = X.split(filename, "[/]");
		if (ss != null && ss.length > 0) {
			return ss[ss.length - 1];
		}
		return X.EMPTY;
	}

	public DFile[] listFiles() throws IOException {

		check();

		File f = new File(path + File.separator + filename);

		File[] ff = f.listFiles();
		if (ff != null) {

			DFile[] l2 = new LocalDFile[ff.length];

			for (int i = 0; i < ff.length; i++) {

				File f1 = ff[i];

				FileInfo j1 = new FileInfo();
				j1.name = f1.getName();
				j1.exists = true;
				j1.isfile = f1.isFile();
				j1.length = f1.length();
				j1.lastmodified = f1.lastModified();

				l2[i] = LocalDFile.create(disk_obj, X.getCanonicalPath("/" + filename + "/" + j1.name), j1);

			}
			return l2;
		}
		return null;
	}

	public long getCreation() {

		check();

		getInfo();
		return info == null ? 0 : info.creation;
	}

	public long lastModified() {

		check();

		getInfo();
		return info == null ? 0 : info.lastmodified;
	}

	public String getCanonicalPath() {
		return filename;
	}

	public long length() {
		check();

		getInfo();

		return info == null ? 0 : info.length;
	}

	public boolean move(DFile file) {

		try {

			File f1 = new File(path + File.separator + filename);
			File f2 = new File(path + File.separator + file.getName());

			return f1.renameTo(f2);
		} catch (Exception e) {
			log.error(url, e);

//			Disk.dao.update(this.disk, V.create("bad", 1));

		}
		return false;
	}

	public static DFile create(Disk d, String filename) {
		return create(d, filename, null);
	}

	public static DFile create(Disk d, String filename, FileInfo info) {

		LocalDFile e = new LocalDFile();

		e.filename = filename;
		e.info = info;

		if (d != null) {
			e.disk = d.getId();
			e.disk_obj = d;
			e.node_obj = d.getNode_obj();
			if (d.getNode_obj() != null) {
				e.url = d.getNode_obj().getUrl();
			}
			e.path = d.path;
		}

		return e;

	}

	@Override
	public String toString() {
		return "LocalDFile [" + url + filename + ", exists=" + this.exists() + ", dir=" + this.isDirectory() + "]";
	}

	public long count(Consumer<String> moni) {
		long n = 0;
		if (this.isDirectory()) {
			try {
				DFile[] ff = this.listFiles();
				if (ff != null) {
					for (DFile f : ff) {
						n += f.count(moni);
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		} else {
			n++;
		}

		if (moni != null) {
			moni.accept(this.getFilename());
		}

		return n;
	}

	public long sum(Consumer<String> moni) {
		long n = 0;
		if (this.isDirectory()) {
			try {
				DFile[] ff = this.listFiles();
				if (ff != null) {
					for (DFile f : ff) {
						n += f.sum(moni);
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

		n += this.length();

		if (moni != null) {
			moni.accept(this.getFilename());
		}

		return n;
	}

	public Path getPath() {
		return Paths.get(URI.create(filename));
	}

	public long save(File f) throws IOException {
		return save(new FileInputStream(f), 0);
	}

	public long save(InputStream in) throws IOException {
		return save(in, 0);
	}

	public long save(InputStream in, long pos) throws IOException {
		if (pos == 0) {
			if (exists()) {
				delete();
			}
		}

		return IOUtil.copy(in, getOutputStream(pos));
	}

}
