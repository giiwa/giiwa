package org.giiwa.dfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Disk;
import org.giiwa.bean.Temp;
import org.giiwa.dao.Counter;
import org.giiwa.dao.X;
import org.giiwa.misc.Base32;
import org.giiwa.misc.IOUtil;
import org.giiwa.task.Consumer;
import org.giiwa.task.Function;
import org.giiwa.task.Task;

/**
 * Demo bean
 * 
 * @author joe
 * 
 */

public abstract class DFile implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(DFile.class);

	protected String filename;

	public String getFilename() {
		return filename;
	}

	public abstract Disk[] getDisk_obj();

	public abstract boolean exists();

	public abstract void refresh();

	protected String prefix;

	public DFile limit(String prefix) {
		if (X.isEmpty(this.prefix)) {
			this.prefix = prefix;
		}
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((filename == null) ? 0 : filename.hashCode());
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
		DFile other = (DFile) obj;
		if (this.filename == null) {
			if (other.filename != null)
				return false;
		} else if (!filename.equals(other.filename))
			return false;
		return true;
	}

	public boolean is(String root) {
		return this.getFilename().startsWith("/" + root + "/");
	}

	public abstract boolean delete();

	public String getId() {
		return Base32.encode(this.getFilename().getBytes());
	}

	public abstract boolean delete(long age);

	public abstract InputStream getInputStream() throws IOException;

	public OutputStream getOutputStream() throws IOException {
		return this.getOutputStream(0);
	}

	public abstract OutputStream getOutputStream(long offset) throws IOException;

	public abstract boolean mkdirs();

	public abstract DFile getParentFile();

	public abstract boolean isDirectory();

	public abstract boolean isFile();

	public String getName() {
		String[] ss = X.split(getFilename(), "[/]");
		if (ss != null && ss.length > 0) {
			return ss[ss.length - 1];
		}
		return X.EMPTY;
	}

	transient DFile[] ff;

	public DFile[] listFiles() throws IOException {
		if (ff == null) {
			ff = list();
		}
		return ff;
	}

	protected abstract DFile[] list() throws IOException;

	public abstract long getCreation();

	public abstract long lastModified();

	public abstract long length();

	public abstract boolean move(DFile file);

	public abstract boolean move(String filename) throws IOException;

	/**
	 * copy the file and upload to disk
	 * 
	 * @param f the File
	 * @return the actually length
	 * @throws IOException
	 */
	public long upload(File f) throws IOException {
		return upload(0, new FileInputStream(f));
	}

	public long upload(InputStream in) throws IOException {
		return upload(0, in);
	}

	public int upload(byte[] bb) throws IOException {
		return upload(0, bb);
	}

	/**
	 * upload the inputsteam to the file
	 * 
	 * @param pos the position
	 * @param in  the inputstream
	 * @return the size
	 * @throws IOException
	 */
	public long upload(long pos, InputStream in) throws IOException {
		return IOUtil.copy(in, this.getOutputStream(pos));
	}

	public int upload(long pos, byte[] bb) throws IOException {
		this.getOutputStream(pos).write(bb);
		return bb.length;
	}

	/**
	 * download the file to local file
	 * 
	 * @param f the local file
	 * @return the size
	 * @throws IOException
	 */
	public long download(File f) throws IOException {
		X.IO.mkdirs(f.getParentFile());
		return IOUtil.copy(this.getInputStream(), new FileOutputStream(f));
	}

	/**
	 * 下载分布式文件到本地临时文件中
	 * 
	 * @return
	 * @throws IOException
	 */
	public Temp download() throws IOException {

		Temp t = Temp.create(this.getName());
		download(t.getFile());
		return t;

	}

	public abstract long count(Consumer<String> moni);

	public abstract long sum(Consumer<String> moni);

	public abstract Path getPath();

	/**
	 * get file size or directory size
	 * 
	 * @return
	 */
	public long size() {
		long size = this.length();
		if (this.isDirectory()) {
			try {
				DFile[] ff = this.listFiles();
				if (ff != null) {
					for (DFile f : ff) {
						size += f.size();
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return size;
	}

	/**
	 * scan all files, stopped by return false
	 * 
	 * @param func
	 * @throws IOException
	 */
	public void scan(Function<DFile, Boolean> func) throws IOException {
		this.scan(func, -1);
	}

	/**
	 * scan all files
	 * 
	 * @param func callback
	 * @param deep -1 for all
	 * @throws IOException
	 */
	public void scan(Function<DFile, Boolean> func, int deep) throws IOException {
		DFile[] ff = this.listFiles();
		if (ff != null && ff.length > 0 && func != null) {
			for (DFile f1 : ff) {

				boolean b = func.apply(f1);

				if (b && f1.isDirectory() && (deep != 0)) {
					f1.scan(func, deep - 1);
				}
			}
		}
	}

	public void merge(DFile d) throws IOException {
		if (this.isDirectory() && d != null && d.isDirectory()) {
			DFile[] f1 = this.listFiles();
			List<DFile> l1 = new ArrayList<DFile>();
			if (f1 != null) {
				for (DFile f : f1) {
					l1.add(f);
				}
			}
			DFile[] f2 = d.listFiles();
			if (f1 != null) {
				for (DFile f : f2) {
					if (!l1.contains(f)) {
						l1.add(f);
					}
				}
			}
		}
	}

	/**
	 * this may using by upper layer app
	 */
	@Override
	final public String toString() {
		return this.getFilename();
	}

	protected static Counter read = new Counter("read");
	protected static Counter write = new Counter("write");

	public static Counter.Stat statRead() {
		return read.get();
	}

	public static Counter.Stat statWrite() {
		return write.get();
	}

	// add file/directory monitor
	public void addListener(IMonitor monitor) throws Exception {

		List<IMonitor> l1 = _monitors.get(filename);
		if (l1 == null) {
			l1 = new ArrayList<IMonitor>();
			_monitors.put(filename, l1);
		}
		if (!l1.contains(monitor)) {
			l1.add(monitor);
		}

	}

	static void onChange(String filename) {

		if (!_monitors.isEmpty()) {
			String[] ff = _monitors.keySet().toArray(new String[_monitors.size()]);
			Task.schedule(t -> {
				for (String f : ff) {
					if (filename.startsWith(f)) {
						List<IMonitor> l1 = _monitors.get(f);
						Task.forEach(l1, e -> e.onFileChange(f));
					}
				}
			});
		}
	}

	static void onDelete(String filename) {

		if (!_monitors.isEmpty()) {
			String[] ff = _monitors.keySet().toArray(new String[_monitors.size()]);
			Task.schedule(t -> {
				for (String f : ff) {
					if (filename.startsWith(f)) {
						List<IMonitor> l1 = _monitors.get(f);
						Task.forEach(l1, e -> e.onFileDelete(f));
					}
				}
			});
		}

	}

	private static Map<String, List<IMonitor>> _monitors = new HashMap<String, List<IMonitor>>();

	public static interface IMonitor {

		public void onFileCreate(String filename);

		public void onFileChange(String filename);

		public void onFileDelete(String filename);

	}

}
