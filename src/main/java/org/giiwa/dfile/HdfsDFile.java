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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.giiwa.bean.Disk;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.misc.IOUtil;
import org.giiwa.task.Consumer;

/**
 * 
 * HDFS File System
 * 
 * @author joe
 * 
 */

public class HdfsDFile extends DFile {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(HdfsDFile.class);

	private String url;

	private Disk disk_obj;
	private FileInfo info;

	public Disk getDisk_obj() {
		return disk_obj;
	}

	public boolean exists() throws IOException {

		TimeStamp t = TimeStamp.create();
		try {
			getInfo();
			return info != null && info.exists;
		} finally {
			read.add(t.pastms(), "filename=%s", filename);
		}

	}

	protected boolean delete0(long age) {

		try {

			FileSystem fs = get();
			Path path = _getFile();
			if (fs != null && fs.exists(path)) {
				fs.delete(path, true);
				return true;
			}

		} catch (Exception e) {
			log.error(url + ":" + disk_obj.path + ":" + filename, e);
		}

		return false;
	}

	private Path _getFile() {
		return new Path(filename);
	}

	private static Map<Long, FileSystem> cached = new HashMap<Long, FileSystem>();

	transient FileSystem file;

	private FileSystem get() throws IOException {

		if (file == null) {
			file = get(filename);
		}

		return file;

	}

	private FileSystem get(String filename) throws IOException {

		Disk d1 = disk_obj;
		synchronized (cached) {
			FileSystem fs = cached.get(d1.id);

			if (fs == null) {
				Configuration conf = new Configuration();
				fs = FileSystem.get(URI.create(d1.url), conf);
				cached.put(d1.id, fs);
			}
			return fs;
		}

	}

	public InputStream getInputStream() throws IOException {

		FileSystem fs = get();
		Path file = _getFile();
		return DFileInputStream.create(this, fs.open(file));
	}

	public OutputStream getOutputStream() throws IOException {
		return this.getOutputStream(0);
	}

	public OutputStream getOutputStream(long offset) throws IOException {

		FileSystem fs = get();
		Path file = _getFile();

		if (fs != null && !fs.exists(file)) {
			try {
				DFile f1 = this.getParentFile();
				if (!f1.exists()) {
					f1.mkdirs();
				}
				fs.create(file).close();
			} catch (Exception e) {
				log.error("paht=" + disk_obj.path + ", filename=" + filename + ", file=" + filename, e);
			}
		} else if (offset == 0) {
			delete();
			fs.create(file).close();
		}

		long[] size = new long[] { offset };

		OutputStream a = fs.create(file, true);

		return DFileOutputStream.create(this.getDisk_obj(), a, filename, offset, (o1, bb, len) -> {

			if (log.isDebugEnabled()) {
				log.debug("nfs flush, file=" + filename + ", offset=" + o1 + ", len=" + bb.length);
			}

			if (bb != null && o1 == size[0]) {

				long t = System.currentTimeMillis();
				try {
					a.write(bb, 0, len);
					size[0] += len;
					a.flush();
				} catch (Exception e) {
					log.error(filename, e);
				} finally {
					Disk.Counter.write(disk_obj).add(len, System.currentTimeMillis() - t);
				}

			}

			return size[0];

		});

	}

	private void mkdirs(Path f) throws IOException {

		FileSystem fs = get();
		Path f1 = f.getParent();
		if (fs != null && !fs.exists(f1)) {
			mkdirs(f1);
		}
	}

	public boolean mkdirs() {

		try {
			FileSystem fs = get();
			Path f = _getFile();
			if (fs != null && !fs.exists(f)) {
				mkdirs(f);
			}

			return true;
		} catch (Exception e) {
			log.error(url + ":" + this.disk_obj.path + ":" + filename, e);
		}
		return false;
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

	private FileInfo getInfo() throws IOException {
		if (info == null) {
			try {

				FileSystem fs = get();
				Path f = _getFile();

				info = new FileInfo();
				info.exists = (fs != null && fs.exists(f)) ? true : false;
				if (info.exists) {
					FileStatus st = fs.getFileStatus(f);
					info.isfile = st.isFile();
					info.length = st.getLen();
					info.lastmodified = st.getModificationTime();
				} else {
					info.isfile = false;
					info.length = 0;
					info.lastmodified = 0;
				}

			} catch (Throwable e) {
				log.error(url + ", filename=" + filename, e);
				throw e;
			}

		}
		return info;
	}

	public boolean isDirectory() {

		try {
			getInfo();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return info != null && !info.isfile;
	}

	public boolean isFile() {

		try {
			getInfo();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return info != null && info.isfile;
	}

	public String getName() {
		String[] ss = X.split(filename, "[/]");
		if (ss != null && ss.length > 0) {
			return ss[ss.length - 1];
		}
		return X.EMPTY;
	}

	protected DFile[] list() throws IOException {

		TimeStamp t = TimeStamp.create();
		try {
			FileSystem fs = get();
			Path f = _getFile();

			FileStatus[] ff = fs.listStatus(f);
			if (ff != null) {

				DFile[] l2 = new HdfsDFile[ff.length];

				for (int i = 0; i < ff.length; i++) {

					FileStatus f1 = ff[i];

					FileInfo j1 = new FileInfo();
					j1.name = f1.getPath().getName();
					j1.exists = true;
					j1.isfile = f1.isFile();
					j1.length = f1.getLen();
					j1.lastmodified = f1.getModificationTime();

					l2[i] = HdfsDFile.create(disk_obj, X.getCanonicalPath("/" + filename + "/" + j1.name), j1);

				}
				return l2;
			}
		} finally {
			read.add(t.pastms(), "filename=%s", filename);
		}
		return null;
	}

	public long getCreation() {

		try {
			getInfo();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return info == null ? 0 : info.creation;
	}

	public long lastModified() {

		try {
			getInfo();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return info == null ? 0 : info.lastmodified;
	}

	public String getCanonicalPath() {
		return filename;
	}

	public long length() {

		try {
			getInfo();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return info == null ? 0 : info.length;
	}

	public boolean move(String filename) throws IOException {
		return move(Disk.seek(filename));
	}

	public boolean move(DFile file) {

		TimeStamp t = TimeStamp.create();
		try {

			X.IO.copy(this, file);

			this.delete();

		} catch (Exception e) {
			log.error(url, e);

		} finally {
			write.add(t.pastms(), "filename=%s", filename);
		}
		return false;
	}

	public static DFile create(Disk d, String filename) {
		return create(d, filename, null);
	}

	public static DFile create(Disk d, String filename, FileInfo info) {

		HdfsDFile e = new HdfsDFile();

		e.url = d.url;
		e.disk_obj = d;
		e.filename = filename;
		e.info = info;

		return e;

	}

	public long count(Consumer<String> moni) {

		TimeStamp t = TimeStamp.create();
		long n = 0;
		try {
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
		} finally {
			read.add(t.pastms(), "filename=%s", filename);
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
			moni.accept(this.filename);
		}

		return n;
	}

	public long save(File f) throws IOException {
		return save(new FileInputStream(f), 0);
	}

	public long save(InputStream in) throws IOException {
		return save(in, 0);
	}

	public long save(InputStream in, long pos) throws IOException {

		TimeStamp t = TimeStamp.create();
		try {
			if (pos == 0) {
				if (exists()) {
					delete();
				}
			}

			return IOUtil.copy(in, getOutputStream(pos));
		} finally {
			write.add(t.pastms(), "filename=%s", filename);
		}
	}

	@Override
	public void refresh() {
		info = null;
	}

	public static void main(String[] args) {

		try {

			Disk d = new Disk();
			d.id = 1;
			d.url = "nfs://g30";
			d.path = "/home/disk2";

			DFile f1 = HdfsDFile.create(d, "/temp/a/b/a");
			System.out.println("f1=" + f1.filename);
//			f1.getParentFile().mkdirs();
			OutputStream out = f1.getOutputStream();
			out.write("abc".getBytes());
			out.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public long getFreeSpace() {
		try {
			FileSystem f1 = this.get();
			return f1.getStatus().getRemaining();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return 0;
	}

	@Override
	public long getTotalSpace() {
		try {
			FileSystem f1 = this.get();
			return f1.getStatus().getCapacity();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return 0;
	}

	@Override
	public boolean rename(String name) throws IOException {

		int i = filename.lastIndexOf("/");
		if (i < 0) {
			name = "/" + name;
		} else {
			name = filename.substring(0, i + 1) + name;
		}

		FileSystem fs = this.get();
		return fs.rename(this._getFile(), new Path(name));

	}

}
