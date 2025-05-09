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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Disk;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.misc.IOUtil;
import org.giiwa.misc.Url;
import org.giiwa.task.Consumer;

import com.emc.ecs.nfsclient.nfs.NfsCreateMode;
import com.emc.ecs.nfsclient.nfs.NfsRenameResponse;
import com.emc.ecs.nfsclient.nfs.NfsSetAttributes;
import com.emc.ecs.nfsclient.nfs.NfsWriteRequest;
import com.emc.ecs.nfsclient.nfs.io.Nfs3File;
import com.emc.ecs.nfsclient.nfs.io.NfsFileInputStream;
import com.emc.ecs.nfsclient.nfs.io.NfsFileOutputStream;
import com.emc.ecs.nfsclient.nfs.nfs3.Nfs3;
import com.emc.ecs.nfsclient.rpc.CredentialUnix;

/**
 * 
 * NFS File System
 * 
 * @author joe
 * 
 */

public class NfsDFile extends DFile {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(NfsDFile.class);

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

			Nfs3File f = get();
			if (f != null && f.exists()) {
				delete(f);
			}

			return true;
		} catch (Exception e) {
			log.error(url + ":" + disk_obj.path + ":" + filename, e);
		}

		return false;
	}

	private void delete(Nfs3File f) throws IOException {

		if (f.isDirectory()) {
			List<Nfs3File> ff = f.listFiles();
			if (ff != null) {
				for (Nfs3File f1 : ff) {
					delete(f1);
				}
			}
		}

//		log.warn("delete, filename=" + filename, new Exception());

		f.delete();

	}

	private static Map<Long, Nfs3> cached = new HashMap<Long, Nfs3>();

	transient Nfs3File file;

	private Nfs3File get() throws IOException {

		if (file == null) {
			file = get(filename);
		}

		return file;

	}

	private Nfs3File get(String filename) throws IOException {

		Disk d1 = disk_obj;
		synchronized (cached) {
			Nfs3 fs = cached.get(d1.id);

			if (fs == null) {
				Url u1 = Url.create(d1.url);
				fs = new Nfs3(u1.getHost(), d1.path, new CredentialUnix(0, 0, null), 3);

				cached.put(d1.id, fs);
			}

			return new Nfs3File(fs, this.rewrite(filename));
		}

	}

	public InputStream getInputStream() throws IOException {
		Nfs3File f = get();
		return DFileInputStream.create(this, new NfsFileInputStream(f));
	}

	public OutputStream getOutputStream() throws IOException {
		return this.getOutputStream(0);
	}

	public OutputStream getOutputStream(long offset) throws IOException {

		Nfs3File f = get();

		NfsSetAttributes att = new NfsSetAttributes();
		att.setMode((long) (0x00100 + 0x00080 + 0x00040));
		if (!f.exists()) {
			try {
				DFile f1 = this.getParentFile();
				if (!f1.exists()) {
					f1.mkdirs();
				}
				f.create(NfsCreateMode.GUARDED, att, null);
			} catch (Exception e) {
				log.error("paht=" + disk_obj.path + ", filename=" + filename + ", file=" + f.getAbsolutePath(), e);
			}
		} else if (offset == 0) {
			if (f.length() > 0) {
				f.delete();
				log.warn("delete file=" + f.getAbsolutePath() + ", disk=" + disk_obj);
				f.create(NfsCreateMode.GUARDED, att, null);
			}
		}

//		log.warn("filename=" + filename + ", offset=" + offset);

		long[] size = new long[] { offset };

		NfsFileOutputStream a = new NfsFileOutputStream(f, offset, NfsWriteRequest.DATA_SYNC);

		return DFileOutputStream.create(this.getDisk_obj(), a, filename, offset, (o1, bb, len) -> {

//			if (log.isDebugEnabled()) {
//			log.warn("flush, file=" + filename + ", offset=" + o1 + ", len=" + len + ", a=" + a);
//			}

			if (bb != null && o1 == size[0]) {

				try {
					a.write(bb, 0, len);
					size[0] += len;
					a.flush();
				} catch (Exception e) {
					log.error("filename=" + filename + ", disk=" + disk_obj, e);
				}

			}

			return size[0];

		});

	}

	private void mkdirs(Nfs3File f) throws IOException {
		Nfs3File f1 = f.getParentFile();
		if (!f1.exists()) {
			mkdirs(f1);
		}
		NfsSetAttributes attrs = new NfsSetAttributes();
		attrs.setMode(0x00711L);
		f.mkdir(attrs);
	}

	public boolean mkdirs() {

		try {
			Nfs3File f = get();
			if (!f.exists()) {
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

				Nfs3File f = get();

				info = new FileInfo();
				info.exists = (f != null && f.exists()) ? true : false;
				info.isfile = (info.exists && f.isFile()) ? true : false;
				info.length = info.exists ? f.length() : 0;
				info.lastmodified = info.exists ? f.lastModified() : 0;

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
			Nfs3File f = get();

			List<Nfs3File> ff = f.listFiles();
			if (ff != null) {

				DFile[] l2 = new NfsDFile[ff.size()];

				for (int i = 0; i < ff.size(); i++) {

					Nfs3File f1 = ff.get(i);

					FileInfo j1 = new FileInfo();
					j1.name = f1.getName();
					j1.exists = true;
					j1.isfile = f1.isFile();
					j1.length = f1.length();
					j1.lastmodified = f1.lastModified();

					l2[i] = NfsDFile.create(disk_obj, X.getCanonicalPath("/" + filename + "/" + j1.name), j1);

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

		NfsDFile e = new NfsDFile();

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
			d.url = "nfs://g30";
			d.path = "/home/disk2";

			DFile f1 = NfsDFile.create(d, "/temp/a/b/a");
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
			Nfs3File f1 = this.get();
			return f1.getFreeSpace();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return 0;
	}

	@Override
	public long getTotalSpace() {
		try {
			Nfs3File f1 = this.get();
			return f1.getTotalSpace();
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

		Nfs3File f1 = this.get();
		NfsRenameResponse r = f1.rename(this.get(name));
		return r.stateIsOk();

	}

}
