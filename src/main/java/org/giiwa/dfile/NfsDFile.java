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
import org.giiwa.misc.Base32;
import org.giiwa.misc.IOUtil;
import org.giiwa.misc.Url;
import org.giiwa.task.Consumer;

import com.emc.ecs.nfsclient.nfs.NfsSetAttributes;
import com.emc.ecs.nfsclient.nfs.NfsWriteRequest;
import com.emc.ecs.nfsclient.nfs.io.Nfs3File;
import com.emc.ecs.nfsclient.nfs.io.NfsFileInputStream;
import com.emc.ecs.nfsclient.nfs.io.NfsFileOutputStream;
import com.emc.ecs.nfsclient.nfs.nfs3.Nfs3;
import com.emc.ecs.nfsclient.rpc.CredentialUnix;

/**
 * 
 * Local File System
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

	private transient Disk disk_obj;
	private transient FileInfo info;

	public Disk[] getDisk_obj() {
		return new Disk[] { disk_obj };
	}

	public boolean exists() {

		TimeStamp t = TimeStamp.create();
		try {
			getInfo();
			return info != null && info.exists;
		} finally {
			read.add(t.pastms());
		}

	}

	public boolean delete() {
		return delete(-1);
	}

	public String getId() {
		return Base32.encode(this.getFilename().getBytes());
	}

	public boolean delete(long age) {

		try {

			Nfs3File f = get();
			f.delete();

			onDelete(filename);

			return true;
		} catch (Exception e) {
			log.error(url, e);
		}

		return false;
	}

	private static Map<Long, Nfs3> cached = new HashMap<Long, Nfs3>();

	transient Nfs3File file;

	private Nfs3File get() throws IOException {

		if (file == null) {
			for (Disk d1 : this.getDisk_obj()) {
				Nfs3 fs = cached.get(d1.id);
				if (fs == null) {
					Url u1 = Url.create(d1.url);
					fs = new Nfs3(u1.getHost(), d1.path, new CredentialUnix(0, 0, null), 3);
					cached.put(d1.id, fs);
				}

				file = new Nfs3File(fs, filename);
			}
		}

		return file;

	}

	public InputStream getInputStream() throws IOException {

		Nfs3File f = get();

		return new NfsFileInputStream(f);
	}

	public OutputStream getOutputStream() throws IOException {
		return this.getOutputStream(0);
	}

	public OutputStream getOutputStream(long offset) throws IOException {

		Nfs3File f = get();

		if (!f.exists()) {
			this.getParentFile().mkdirs();
			f.createNewFile();
		} else if (offset == 0) {
			delete();
			f.createNewFile();
		}

		long[] size = new long[] { offset };

		NfsFileOutputStream a = new NfsFileOutputStream(f, offset, NfsWriteRequest.FILE_SYNC);

		return DFileOutputStream.create(this.getDisk_obj(), a, filename, offset, (o1, bb, len) -> {

			if (log.isDebugEnabled()) {
				log.debug("nfs flush, file=" + filename + ", offset=" + o1 + ", len=" + bb.length);
			}

			if (bb != null && o1 == size[0]) {

				a.write(bb, 0, len);
				size[0] += len;
				a.flush();

				NfsSetAttributes attrs = new NfsSetAttributes();
				attrs.setMode(X.toLong(0x00800 | 0x00100 | 0x00080));
				f.setAttributes(attrs);

			}

			return size[0];

		});

	}

	public boolean mkdirs() {

		try {
			Nfs3File f = get();
			if (!f.exists()) {
				f.mkdirs();
			}

//			NfsSetAttributes attrs = new NfsSetAttributes();
//			attrs.setMode(X.toLong(0x00800 | 0x00100 | 0x00080));
//			f.setAttributes(attrs);

			return true;
		} catch (Exception e) {
			log.error("url=" + url + ", filename=" + filename, e);
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

	private FileInfo getInfo() {
		if (info == null) {
			try {

				Nfs3File f = get();

				info = new FileInfo();
				info.exists = (f != null && f.exists()) ? true : false;
				info.isfile = (info.exists && f.isFile()) ? true : false;
				info.length = info.exists ? f.length() : 0;
				info.lastmodified = info.exists ? f.lastModified() : 0;

			} catch (Throwable e) {
				log.error(url, e);
			}

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
			read.add(t.pastms());
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
			write.add(t.pastms());
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
			read.add(t.pastms());
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

		TimeStamp t = TimeStamp.create();
		try {
			if (pos == 0) {
				if (exists()) {
					delete();
				}
			}

			return IOUtil.copy(in, getOutputStream(pos));
		} finally {
			write.add(t.pastms());
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
			d.url = "nfs://g01";
			d.path = "/home/disk2";

			DFile f1 = NfsDFile.create(d, "/temp/a/b/a");
			System.out.println("f1=" + f1.getFilename());
//			f1.getParentFile().mkdirs();
			OutputStream out = f1.getOutputStream();
			out.write("abc".getBytes());
			out.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
