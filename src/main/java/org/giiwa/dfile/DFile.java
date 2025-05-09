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
package org.giiwa.dfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Disk;
import org.giiwa.bean.S;
import org.giiwa.bean.Temp;
import org.giiwa.dao.Comment;
import org.giiwa.dao.Counter;
import org.giiwa.dao.X;
import org.giiwa.misc.Base32;
import org.giiwa.misc.IOUtil;
import org.giiwa.task.Consumer;
import org.giiwa.task.Function;
import org.giiwa.task.Task;

/**
 * DFile bean
 * 
 * @author joe
 * 
 */

public abstract class DFile implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 查询目录中最大文件数量, 防止大量文件死机
	 */
	public static final int LIMIT_SIZE = 10000;

	private static Log log = LogFactory.getLog(DFile.class);

	transient protected Disk disk_obj;
	transient protected FileInfo info;

	public Disk getDisk_obj() {
		return disk_obj;
	}

	private List<DFile> linked;

	protected String filename;

	public String getFilename() {
		return filename;
	}

	public abstract boolean exists() throws IOException;

	public abstract void refresh();

	protected String prefix;

	protected String rewrite(String filename) {
		return this.getDisk_obj().filename(filename);
	}

	/**
	 * 设置DFile的读写空间路径范围
	 * 
	 * @param prefix 前缀名
	 * @return
	 */
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

	public boolean delete() {
		return delete(-1);
	}

	public boolean delete(long age) {

		boolean done = false;
		try {
			if (!this.getDisk_obj().isOk(filename)) {
				return false;
			}

			if (this.isFile()) {
				done = this.delete0(age);
			} else if (this.isDirectory()) {
				Collection<DFile> l1 = Disk.list(filename);
				if (l1 != null) {
					for (DFile f1 : l1) {
						f1.delete0(age);
					}
				}
				done = this.delete0(age);
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return done;
	}

	public String getId() {
		// shortname or fullname
		return Base32.encode(this.getFilename().getBytes());
	}

	protected abstract boolean delete0(long age);

	public abstract InputStream getInputStream() throws IOException;

	public OutputStream getOutputStream() throws IOException {
		return this.getOutputStream(0);
	}

	public OutputStream getOut() throws IOException {
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
			Map<String, DFile> m = new TreeMap<String, DFile>();
			if (this.isDirectory()) {
				DFile[] ff = list();
				if (ff != null) {
					for (DFile f1 : ff) {
						if (f1.getDisk_obj().isOk(f1.getFilename())) {
							m.put(f1.filename, f1);
							if (m.size() >= DFile.LIMIT_SIZE) {
								break;
							}
						}
					}
				}
			}

			if (linked != null) {
				for (DFile f1 : linked) {
					if (f1.isDirectory()) {
						DFile[] ff = f1.list();
						if (ff != null) {
							for (DFile f2 : ff) {
								DFile f3 = m.get(f2.filename);
								if (f3 != null) {
									f3.merge(f3);
								} else {
									m.put(f2.filename, f2);
									if (m.size() >= DFile.LIMIT_SIZE) {
										break;
									}
								}
							}
							if (m.size() >= DFile.LIMIT_SIZE) {
								break;
							}
						}
					}
				}
			}
			ff = m.values().toArray(new DFile[m.size()]);
		}
		return ff;
	}

	protected abstract DFile[] list() throws IOException;

	public abstract long getCreation();

	public abstract long lastModified();

	public abstract long length();

	public abstract boolean move(DFile file);

	public abstract boolean move(String filename) throws IOException;

	public abstract boolean rename(String name) throws IOException;// {

//	public DFile rename(String name) throws IOException {
//
//		String filename = this.getFilename();
//		int i = filename.lastIndexOf("/");
//		if (i < 0) {
//			filename = "/" + name;
//		} else {
//			filename = filename.substring(0, i + 1) + name;
//		}
//
//		
//		DFile f1 = Disk.seek(filename);
//		if (this.move(f1)) {
//			return f1;
//		}
//		throw new IOException("rename failed!");
//
//	}

	/**
	 * copy the file and upload to disk
	 * 
	 * @param f the File
	 * @return the actually length
	 * @throws IOException
	 */
	public long upload(File f) throws IOException {
		if (f.isDirectory()) {
//			Zip.zip(this, f);
			X.IO.copyDir(f, this);
			return this.length();
		} else {
			return upload(0, new FileInputStream(f), true);
		}
	}

	public long upload(InputStream in) throws IOException {
		if (in instanceof ZipInputStream) {
			return upload(0, in, false);
		} else {
			return upload(0, in, true);
		}
	}

	public long upload(InputStream in, boolean close) throws IOException {
		return upload(0, in, close);
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
		return upload(pos, in, true);
	}

	public long upload(long pos, InputStream in, boolean close) throws IOException {
		OutputStream out = this.getOutputStream(pos);
		try {
			return IOUtil.copy(in, out, close);
		} finally {
			X.close(out);
		}
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
	@SuppressWarnings("resource")
	public long download(File f) throws IOException {
		if (this.isFile()) {
			X.IO.mkdirs(f.getParentFile());
			return IOUtil.copy(this.getInputStream(), new FileOutputStream(f));
		} else {
			// dir
			X.IO.mkdirs(f);
			Collection<DFile> ff = Disk.list(filename);
			long total = 0;
			if (ff != null) {
				for (DFile f1 : ff) {
					total += f1.download(new File(f.getAbsolutePath() + "/" + f1.getName()));
				}
			}
			return total;
		}
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

	public Path getPath() {
		return Paths.get(filename);
	}

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

		Collection<DFile> ff = Disk.list(filename);
		if (ff != null && !ff.isEmpty() && func != null) {
			for (DFile f1 : ff) {

				boolean b = func.apply(f1);

				if (b && f1.isDirectory() && (deep != 0)) {
					f1.scan(func, deep - 1);
				}
			}
		}

	}

	public void merge(DFile d) throws IOException {

		if (linked == null) {
			linked = new ArrayList<>();
		}
		if (!linked.contains(d)) {
			linked.add(d);
		}
		d.linked = linked;

//		if (this.isDirectory() && d != null && d.isDirectory()) {
//			DFile[] f1 = this.listFiles();
//			List<DFile> l1 = new ArrayList<DFile>();
//			if (f1 != null) {
//				for (DFile f : f1) {
//					l1.add(f);
//				}
//			}
//			DFile[] f2 = d.listFiles();
//			if (f1 != null) {
//				for (DFile f : f2) {
//					if (!l1.contains(f)) {
//						l1.add(f);
//					}
//				}
//			}
//		}
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

	public abstract long getFreeSpace();

	public abstract long getTotalSpace();

	public String getUrl() {
		return "/f/g/" + this.getId() + "/" + this.getName();
	}

	public String getDownloadUrl() {
		return "/f/d/" + this.getId() + "/" + this.getName();
	}

	public DFile last() {
		if (linked == null || linked.isEmpty()) {
			return this;
		}
		long time = this.lastModified();
		DFile last = this;
		for (DFile e : linked) {
			long t1 = e.lastModified();
			if (t1 > time) {
				time = t1;
				last = e;
			}
		}
		return last;
	}

	public static void search(String filename, String word, int deep, Function<DFile, Boolean> func)
			throws IOException {

		if (func == null) {
			return;
		}
		if (deep < 0) {
			return;
		}

		Set<String> checked = new HashSet<String>();
		List<String> dirs0 = new ArrayList<String>();
		dirs0.add(filename);

		while (deep >= 0) {

			List<String> dirs1 = new ArrayList<String>();
			for (String filename1 : dirs0) {
				Collection<DFile> l1 = Disk.list(filename1);
				if (l1 != null) {
					for (DFile f1 : l1) {

						String name = f1.getName();
						if (name.contains(word) || name.matches(word)) {
							if (!func.apply(f1)) {
								return;
							}
						}

						if (deep > 0 && f1.isDirectory()) {
							if (!checked.contains(f1.getFilename())) {
								checked.add(f1.getFilename());
								dirs1.add(f1.getFilename());
							}
						}

					}
				}
			}

			dirs0 = dirs1;
			deep--;
		}
	}

	@Comment(text = "获取访问链接")
	public String url() {
		String url = "/f/" + this.getId() + "/" + this.getName();
		return S.create(url);
	}

	public Temp zip() throws Exception {

		Temp t = Temp.create(this.getName() + ".zip");
		ZipOutputStream out = t.getZipOutputStream();
		_zip(out, this.getName(), this);
		X.close(out);
		return t;
	}

	private void _zip(ZipOutputStream out, String filename, DFile f1) throws IOException {
		if (f1.isFile()) {
			ZipEntry e1 = new ZipEntry(filename);
			out.putNextEntry(e1);
			InputStream in = f1.getInputStream();
			X.IO.copy(in, out, false);
			X.close(in);
		} else {
			Collection<DFile> ff = Disk.list(f1.getFilename());
			if (ff != null) {
				for (DFile f2 : ff) {
					_zip(out, filename + "/" + f2.getName(), f2);
				}
			}
		}
	}

}
