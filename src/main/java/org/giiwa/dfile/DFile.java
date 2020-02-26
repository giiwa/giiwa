package org.giiwa.dfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Disk;
import org.giiwa.bean.Node;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.misc.Base32;
import org.giiwa.misc.IOUtil;

/**
 * Demo bean
 * 
 * @author joe
 * 
 */

public class DFile {

	private static Log log = LogFactory.getLog(DFile.class);

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
		}

		if (disk_obj != null) {
			path = disk_obj.getPath();
			node_obj = disk_obj.getNode_obj();

			if (node_obj != null) {
				url = node_obj.getUrl();

				return true;
			}
		}

		return false;
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

			return FileClient.get(url).delete(path, filename, age);

		} catch (Exception e) {
			log.error(url, e);

			Disk.dao.update(this.disk, V.create("bad", 1));

		} finally {
			// dao.delete(W.create("disk", disk).and("filename", filename));
		}

		return false;
	}

	public InputStream getInputStream() {
		check();

		return DFileInputStream.create(this.getDisk_obj(), filename);
	}

	public OutputStream getOutputStream() throws FileNotFoundException {
		return this.getOutputStream(0);
	}

	public OutputStream getOutputStream(long offset) throws FileNotFoundException {

		check();

		// if (offset == 0) {
		// GLog.applog.info(dfile.class, "put", filename, null, null);
		// }

		return DFileOutputStream.create(this.getDisk_obj(), filename, offset);
	}

	public boolean mkdirs() {
		check();

		try {
			return FileClient.get(url).mkdirs(path, this.filename);
		} catch (Exception e) {
			log.error(url, e);
			Disk.dao.update(this.disk, V.create("bad", 1));
		}
		return true;
	}

	public DFile getParentFile() {
		int i = filename.lastIndexOf("/");
		if (i > 0) {
			return create(disk_obj, filename.substring(0, i));
		} else {
			return null;
		}
	}

	private FileInfo getInfo() {
		if (info == null) {
			try {
				info = FileClient.get(url).info(path, filename);
			} catch (IOException e) {
				log.error(url, e);
				Disk.dao.update(this.disk, V.create("bad", 1));

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

		List<FileInfo> l1 = FileClient.get(url).list(path, filename);

		DFile[] l2 = new DFile[l1.size()];
		int i = 0;

		for (FileInfo j1 : l1) {
			DFile d1 = DFile.create(disk_obj, X.getCanonicalPath("/" + filename + "/" + j1.name), j1);
			l2[i++] = d1;
		}

		return l2;
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
			return FileClient.get(url).move(path, filename, file.path, file.filename);
		} catch (Exception e) {
			log.error(url, e);

			Disk.dao.update(this.disk, V.create("bad", 1));

		}
		return false;
	}

	public static DFile create(Disk d, String filename) {
		return create(d, filename, null);
	}

	public static DFile create(Disk d, String filename, FileInfo info) {

		DFile e = new DFile();

		e.filename = filename;
		e.info = info;

		if (d != null) {
			e.disk = d.getId();
			e.disk_obj = d;
			e.node_obj = d.getNode_obj();
			if (d.getNode_obj() != null) {
				e.url = d.getNode_obj().getUrl();
			}
			e.path = d.getPath();
		}

		return e;

	}

	@Override
	public String toString() {
		return "DFile [" + url + filename + ", exists=" + this.exists() + ", dir=" + this.isDirectory() + "]";
	}

	/**
	 * copy the file and upload to disk
	 * 
	 * @param f the File
	 * @return the actually length
	 */
	public long upload(File f) {
		try {
			return IOUtil.copy(new FileInputStream(f), this.getOutputStream());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Disk.dao.update(this.disk, V.create("bad", 1));

		}
		return -1;
	}

	public long upload(InputStream in) {
		return upload(0, in);
	}

	/**
	 * upload the inputsteam to the file
	 * 
	 * @param pos the position
	 * @param in  the inputstream
	 * @return the size
	 */
	public long upload(long pos, InputStream in) {
		try {
			return IOUtil.copy(in, this.getOutputStream(pos));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Disk.dao.update(this.disk, V.create("bad", 1));

		}
		return -1;
	}

	/**
	 * download the file to local file
	 * 
	 * @param f the local file
	 * @return the size
	 */
	public long download(File f) {
		try {
			f.getParentFile().mkdirs();
			return IOUtil.copy(this.getInputStream(), new FileOutputStream(f));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Disk.dao.update(this.disk, V.create("bad", 1));

		}
		return -1;
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
