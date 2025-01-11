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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Disk;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.misc.IOUtil;
import org.giiwa.task.Consumer;

import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;

/**
 * 
 * Samba File System
 * 
 * @author joe
 * 
 */

public class SmbDFile extends DFile {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(SmbDFile.class);

	private String url; // smb://username:password@host1

//	private String domain;
//	private String username;
//	private String password;

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

			SmbFile f = get();
			if (f != null && f.exists()) {
				delete(f);
			}

			return true;
		} catch (Exception e) {
			log.error(url + ":" + disk_obj.path + ":" + filename, e);
		}

		return false;
	}

	private void delete(SmbFile f) throws SmbException {
		if (f.isDirectory()) {
			SmbFile[] ff = f.listFiles();
			if (ff != null) {
				for (SmbFile f1 : ff) {
					delete(f1);
				}
			}
		}
		f.delete();
	}

	private static BaseContext ctx;
	transient SmbFile file;
	transient NtlmPasswordAuthenticator auth;

	private SmbFile get() throws IOException {

		if (file == null) {
			file = this.get(filename);
		}

		return file;

	}

	private SmbFile get(String filename) throws IOException {

		String remoteurl = url + X.getCanonicalPath(disk_obj.path + "/" + this.rewrite(filename));

		if (auth == null) {
			auth = new NtlmPasswordAuthenticator(disk_obj.domain, disk_obj.username, disk_obj.password);
		}
		if (ctx == null) {
			ctx = new BaseContext(new PropertyConfiguration(new Properties()));
		}

		SmbFile e = new SmbFile(remoteurl, ctx.withCredentials(auth));

		return e;
	}

	public InputStream getInputStream() throws IOException {

		try {
			SmbFile f = get();
			return DFileInputStream.create(this, f.getInputStream());
		} catch (IOException e) {
			log.error(filename, e);
			throw e;
		}
	}

	public OutputStream getOutputStream() throws IOException {
		return this.getOutputStream(0);
	}

	public OutputStream getOutputStream(long offset) throws IOException {

		SmbFile f = get();

		if (!f.exists()) {
			try {
				DFile f1 = this.getParentFile();
				if (!f1.exists()) {
					f1.mkdirs();
				}
				f.createNewFile();
			} catch (Exception e) {
				log.error("paht=" + disk_obj.path + ", filename=" + filename + ", file=" + f.getCanonicalPath(), e);
			}
		} else if (offset == 0) {
			if (f.length() > 0) {
				f.delete();
			}
			f.createNewFile();
		}

		long[] size = new long[] { offset };

		SmbFileOutputStream a = new SmbFileOutputStream(f, offset > 0);

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
				} finally {
					Disk.Counter.write(disk_obj).add(len, System.currentTimeMillis() - t);
				}

			}

			return size[0];

		});

	}

	public boolean mkdirs() {

		try {
			SmbFile f = get();
			if (!f.exists()) {
				f.mkdirs();
			}

			return true;
		} catch (Exception e) {
			log.error(url + ":" + disk_obj.path + ":" + filename, e);
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
//			try {

			SmbFile f = get();

			info = new FileInfo();
			info.exists = (f != null && f.exists()) ? true : false;
			info.isfile = (info.exists && f.isFile()) ? true : false;
			info.length = info.exists ? f.length() : 0;
			info.lastmodified = info.exists ? f.lastModified() : 0;

//			} catch (Throwable e) {
//				log.error(url, e);
//			}

		}
		return info;
	}

	public boolean isDirectory() {

		try {
			getInfo();
		} catch (Exception e) {
			log.error(url + ":" + disk_obj.path + ":" + filename, e);
		}
		return info != null && !info.isfile;
	}

	public boolean isFile() {

		try {
			getInfo();
		} catch (Exception e) {
			log.error(url + ":" + disk_obj.path + ":" + filename, e);
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
			if (!filename.endsWith("/")) {
				filename = filename + "/";
				file = null;
			}

			SmbFile f = get();

			SmbFile[] ff = f.listFiles();
			if (ff != null) {

//				String prev = new File(filename).getName();
				DFile[] l2 = new SmbDFile[ff.length];

				for (int i = 0; i < ff.length; i++) {

					SmbFile f1 = ff[i];

					String name = f1.getName();
					/**
					 * fix issue: some samba server has bug,
					 * filename='/test/i/demo5/images/work-4.jpg', but f1.name='imageswork-4.jpg',
					 * this word 'images' duplicated in name<br>
					 * 
					 * but pdc1/s41 no this issue 2023-09-26
					 */
//					if (name.startsWith(prev)) {
//						name = name.substring(prev.length());
//					}

					if (log.isDebugEnabled()) {
						log.debug("list, " + filename + ", f1=" + f1.getCanonicalPath() + ", name=" + name);
					}

					FileInfo j1 = new FileInfo();
					j1.name = name;
					j1.exists = true;
					j1.isfile = f1.isFile();
					j1.length = f1.length();
					j1.lastmodified = f1.lastModified();

					l2[i] = SmbDFile.create(disk_obj, X.getCanonicalPath("/" + filename + "/" + j1.name), j1);
					((SmbDFile) l2[i]).file = f1;

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
			log.error(url + ":" + disk_obj.path + ":" + filename, e);
		}
		return info == null ? 0 : info.creation;
	}

	public long lastModified() {

		try {
			getInfo();
		} catch (Exception e) {
			log.error(url + ":" + disk_obj.path + ":" + filename, e);
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
			log.error(url + ":" + disk_obj.path + ":" + filename, e);
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
			log.error(url + ":" + disk_obj.path + ":" + filename, e);

		} finally {
			write.add(t.pastms(), "filename=%s", filename);
		}
		return false;
	}

	public static DFile create(Disk d, String filename) {
		return create(d, filename, null);
	}

	public static DFile create(Disk d, String filename, FileInfo info) {

		SmbDFile e = new SmbDFile();

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
					log.error(url + ":" + disk_obj.path + ":" + filename, e);
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
				log.error(url + ":" + disk_obj.path + ":" + filename, e);
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
			d.url = "smb://g30";
			d.path = "/home/disk2";

			DFile f1 = SmbDFile.create(d, "/temp/a/b/a");
//			System.out.println("f1=" + f1.getFilename());
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
			SmbFile f1 = this.get();
			return f1.getDiskFreeSpace();
		} catch (Exception e) {
			log.error(url + ":" + disk_obj.path + ":" + filename, e);
		}
		return 0;
	}

	@Override
	public long getTotalSpace() {
		long free = this.getFreeSpace();
		SmbFile f1 = null;
		try {
			String share = disk_obj.path;
			if (share.startsWith("/")) {
				share = share.substring(1);
			}
			int i = share.indexOf("/");
			if (i > 0) {
				share = share.substring(0, i);
			}
			// url + /{share}

			String remoteurl = url + "/" + share;

			if (auth == null) {
				auth = new NtlmPasswordAuthenticator(disk_obj.domain, disk_obj.username, disk_obj.password);
			}
			if (ctx == null) {
				ctx = new BaseContext(new PropertyConfiguration(new Properties()));
			}

			f1 = new SmbFile(remoteurl, ctx.withCredentials(auth));
			long n = f1.length();
			if (n < free) {
				return free;
			}

			return n;
		} catch (Exception e) {
			log.error(url + ":" + disk_obj.path + ":" + filename, e);
		} finally {
			if (f1 != null) {
				f1.close();
			}
		}
		return 0;
	}

	@Override
	public boolean rename(String name) throws IOException {

		int i = filename.lastIndexOf("/");
		if (i < 0) {
			name = "/" + name;
		} else {
			if (filename.endsWith("/")) {
				i = filename.substring(0, i).lastIndexOf("/");
			}
			name = filename.substring(0, i + 1) + name;
			if (filename.endsWith("/")) {
				if (!name.endsWith("/")) {
					name += "/";
				}
			}
		}

		SmbFile f1 = this.get();
		f1.renameTo(this.get(name), true);

		return true;

	}

}
