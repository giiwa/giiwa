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
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Disk;
import org.giiwa.conf.Config;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.misc.Base32;
import org.giiwa.misc.IOUtil;
import org.giiwa.task.Consumer;

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

	private String url;

	private String path;
//	private transient Node node_obj;

	public boolean exists() {

		TimeStamp t = TimeStamp.create();
		try {
			getInfo();
			return info != null && info.exists;
		} finally {
			read.add(t.pastms(), "filename=%s", filename);
		}

	}

	public String getId() {
		return Base32.encode(this.filename.getBytes());
	}

	protected boolean delete0(long age) {

		TimeStamp t = TimeStamp.create();
		try {

			File f = new File(path + "/" + this.rewrite(filename));
			return IOUtil.delete(f, age, s -> {

				if (s.startsWith(path)) {
					String s1 = s.substring(path.length());
					if (!s1.startsWith("/")) {
						s1 = "/" + s1;
					}

				}

			}) > 0;

		} catch (Exception e) {
			log.error(url + ":" + disk_obj.path + ":" + filename, e);
		} finally {
			write.add(t.pastms(), "filename=%s", filename);
		}

		return false;
	}

	public InputStream getInputStream() throws IOException {

		return DFileInputStream.create(this, new FileInputStream(new File(path + "/" + this.rewrite(filename))));

	}

	public OutputStream getOutputStream() throws IOException {
		return this.getOutputStream(0);
	}

	public OutputStream getOutputStream(long offset) throws IOException {

		File f = new File(path + "/" + this.rewrite(filename));

		try {
			if (!f.exists()) {
				X.IO.mkdirs(f.getParentFile());
				f.createNewFile();
			} else if (offset == 0) {
				if (f.length() > 0) {
					f.delete();
				}
				f.createNewFile();
			}
		} catch (IOException e) {
			log.error(f.getAbsolutePath(), e);
			throw e;
		}

		RandomAccessFile a = new RandomAccessFile(f, "rws");

		return DFileOutputStream.create(this.getDisk_obj(), a, filename, offset, (o1, bb, len) -> {

			if (log.isDebugEnabled()) {
				log.debug("local flush, file=" + filename + ", offset=" + o1 + ", len=" + bb.length);
			}

			if (bb != null) {

				a.seek(o1);
				a.write(bb, 0, len);
				a.getFD().sync(); // not needs

			}

			return o1 + len;

		});

//		long[] size = new long[] { f.length() };
//		if (offset != size[0]) {
//			throw new IOException("bad offset[" + offset + "], size=" + size[0] + ", filename=" + filename);
//		}
//
//		OutputStream a = new FileOutputStream(f, true);
//		return DFileOutputStream.create(this.getDisk_obj(), a, filename, offset, (o1, bb, len) -> {
//
//			if (log.isDebugEnabled()) {
//				log.debug("local flush, file=" + filename + ", offset=" + o1 + ", len=" + bb.length);
//			}
//
//			if (bb != null && o1 == size[0]) {
//
//				a.write(bb, 0, len);
//				size[0] += len;
//				a.flush();
//
//			}
//
//			return o1 + len;
//
//		});

	}

	public boolean mkdirs() {

		TimeStamp t = TimeStamp.create();
		try {
			File f = new File(path + "/" + this.rewrite(filename));
			return X.IO.mkdirs(f);
		} catch (Exception e) {
			log.error(url, e);
//			Disk.dao.update(this.disk, V.create("bad", 1));
		} finally {
			write.add(t.pastms(), "filename=%s", filename);
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
//			try {

			File f = new File(path + "/" + this.rewrite(filename));

//				log.debug("f=" + f.getAbsolutePath());

			info = new FileInfo();
			info.exists = f.exists() ? true : false;
			info.isfile = info.exists && f.isFile() ? true : false;
			info.length = info.exists ? f.length() : 0;
			info.lastmodified = info.exists ? f.lastModified() : 0;

//			} catch (Exception e) {
//				log.error(url, e);
//				Disk.dao.update(this.disk, V.create("bad", 1));

//			}
		}
		return info;
	}

	public boolean isDirectory() {

		getInfo();
		return info != null && !info.isfile;
	}

	public boolean isFile() {

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

	protected DFile[] list() throws IOException {

		TimeStamp t = TimeStamp.create();
		try {
			File f = new File(path + "/" + this.rewrite(filename));

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
		} finally {
			read.add(t.pastms(), "filename=%s", filename);
		}
		return null;
	}

	public long getCreation() {

		getInfo();
		return info == null ? 0 : info.creation;
	}

	public long lastModified() {

		getInfo();
		return info == null ? 0 : info.lastmodified;
	}

	public String getCanonicalPath() {
		return filename;
	}

	public long length() {

		getInfo();

		return info == null ? 0 : info.length;
	}

	public boolean move(String filename) throws IOException {
		filename = X.getCanonicalPath(filename);
		if (!X.isEmpty(prefix)) {
			if (!filename.startsWith(prefix)) {
				throw new IOException("can't move limited file to outside!");
			}
		}
		return move(Disk.seek(filename));
	}

	public boolean move(DFile file) {

		TimeStamp t = TimeStamp.create();
		try {

			File f1 = new File(path + "/" + this.rewrite(filename));
			File f2 = new File(path + "/" + this.rewrite(file.filename));

			f2.getParentFile().mkdirs();

			if (log.isWarnEnabled()) {
				log.warn("move dfile: " + f1.getAbsolutePath() + " => " + f2.getAbsolutePath());
			}

			boolean b = f1.renameTo(f2);
			f2.setReadable(false, false);
			f2.setWritable(false, false);
			f2.setExecutable(false, false);

			return b;

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

		LocalDFile e = new LocalDFile();

		e.filename = filename;
		e.info = info;

		if (d != null) {
			e.disk_obj = d;
			e.url = d.url;
			e.path = d.path;
		} else {
			e.path = Config.getConf().getString("dfile.home", "/data/disk1");
		}

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
				moni.accept(this.filename);
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

	public DFile toDFile(File file) {

		String s1 = file.getAbsolutePath().replaceAll("\\\\", "/");

		if (!s1.startsWith(path)) {
			return null;
		}
		String filename = s1.substring(path.length());
		if (!filename.startsWith("/")) {
			filename = "/" + filename;
		}

		LocalDFile d1 = new LocalDFile();
		d1.path = path;
		d1.filename = filename;
		d1.disk_obj = disk_obj;
		return d1;

	}

	@Override
	public long getFreeSpace() {
		File f = new File(path + "/" + this.rewrite(filename));
		return f.getFreeSpace();
	}

	@Override
	public long getTotalSpace() {
		File f = new File(path + "/" + this.rewrite(filename));
		return f.getTotalSpace();
	}

	@Override
	public boolean rename(String name) throws IOException {

		int i = filename.lastIndexOf("/");
		if (i < 0) {
			name = "/" + name;
		} else {
			name = filename.substring(0, i + 1) + name;
		}

		return new File(path + "/" + this.rewrite(filename)).renameTo(new File(path + "/" + this.rewrite(name)));

	}

}
