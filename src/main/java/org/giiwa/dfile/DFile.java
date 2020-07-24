package org.giiwa.dfile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Disk;
import org.giiwa.bean.Node;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.misc.Base32;
import org.giiwa.misc.IOUtil;

import jdk.nashorn.api.scripting.JSObject;

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

	public abstract String getFilename();

	public abstract Node getNode_obj();

	public abstract Disk getDisk_obj();

	public abstract boolean check();

	public abstract boolean exists();

	public abstract String getAbsolutePath();

	public boolean is(String root) {
		return this.getFilename().startsWith("/" + root + "/");
	}

	public String getCanonicalPath() {
		return X.getCanonicalPath(this.getAbsolutePath());
	}

	public abstract boolean delete();

	public String getId() {
		return Base32.encode(this.getFilename().getBytes());
	}

	public abstract boolean delete(long age);

	public abstract InputStream getInputStream();

	public OutputStream getOutputStream() throws FileNotFoundException {
		return this.getOutputStream(0);
	}

	public abstract OutputStream getOutputStream(long offset) throws FileNotFoundException;

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

	public abstract DFile[] listFiles() throws IOException;

	public abstract long getCreation();

	public abstract long lastModified();

	public abstract long length();

	public abstract boolean move(DFile file);

	/**
	 * copy the file and upload to disk
	 * 
	 * @param f the File
	 * @return the actually length
	 * @throws FileNotFoundException
	 */
	public DFile upload(File f) throws FileNotFoundException {
		return upload(0, new FileInputStream(f));
	}

	public DFile upload(InputStream in) {
		return upload(0, in);
	}

	/**
	 * upload the inputsteam to the file
	 * 
	 * @param pos the position
	 * @param in  the inputstream
	 * @return the size
	 */
	public DFile upload(long pos, InputStream in) {
		try {
			IOUtil.copy(in, this.getOutputStream(pos));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return this;
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
		}
		return -1;
	}

	public abstract long count(Consumer<String> moni);

	public abstract long sum(Consumer<String> moni);

	public abstract Path getPath();

	public _Zip zip() {
		return new _Zip();
	}

	public class _Zip {

		/**
		 * @deprecated
		 * @param name
		 * @return
		 * @throws IOException
		 */
		public JSON json(String name) throws IOException {

			ZipInputStream in = null;

			try {
				in = new ZipInputStream(getInputStream());
				ZipEntry e = in.getNextEntry();
				while (e != null) {
					if (X.isSame(name, e.getName())) {
						return JSON.fromObject(in);
					}
					e = in.getNextEntry();
				}
				return null;
			} finally {
				X.close(in);
			}
		}

		public void json(String name, BiConsumer<String, JSON> func) throws IOException {

			ZipInputStream in = null;

			try {
				in = new ZipInputStream(getInputStream());
				ZipEntry e = in.getNextEntry();
				while (e != null) {
					if (e.getName().matches(name)) {
						JSON j1 = JSON.fromObject(in);
						if (j1 != null && !j1.isEmpty()) {
							func.accept(e.getName(), j1);
						}
					}
					e = in.getNextEntry();
				}
			} finally {
				X.close(in);
			}
		}

		@SuppressWarnings("restriction")
		public void json(String name, JSObject func) throws IOException {

			json(name, (filename, j1) -> {
				func.call(this, filename, j1);
			});

		}

		/**
		 * @deprecated
		 * @param name
		 * @return
		 * @throws IOException
		 */
		public InputStream stream(String name) throws IOException {
			ZipInputStream in = null;

			in = new ZipInputStream(getInputStream());
			ZipEntry e = in.getNextEntry();
			while (e != null) {
				if (X.isSame(name, e.getName())) {
					return in;
				}
				e = in.getNextEntry();
			}
			return null;
		}

		public void stream(String name, BiConsumer<String, InputStream> func) throws IOException {
			ZipInputStream in = null;

			in = new ZipInputStream(getInputStream());
			ZipEntry e = in.getNextEntry();
			while (e != null) {
				if (e.getName().matches(name)) {
					func.accept(e.getName(), in);
				}
				e = in.getNextEntry();
			}
		}

		@SuppressWarnings("restriction")
		public void stream(String name, JSObject func) throws IOException {

			stream(name, (filename, j1) -> {
				func.call(this, filename, j1);
			});

		}

		public _CSV csv(String name, String deli) throws IOException {

			ZipInputStream in = null;

			in = new ZipInputStream(getInputStream());
			ZipEntry e = in.getNextEntry();
			while (e != null) {
				if (X.isSame(name, e.getName())) {
					return new _CSV(in, deli);
				}
				e = in.getNextEntry();
			}
			return null;
		}

		public void csv(String name, String deli, BiConsumer<String, _CSV> func) throws IOException {

			ZipInputStream in = null;

			try {
				in = new ZipInputStream(getInputStream());
				ZipEntry e = in.getNextEntry();
				while (e != null) {
					if (e.getName().matches(name)) {
						func.accept(e.getName(), new _CSV(in, deli));
					}
					e = in.getNextEntry();
				}
			} finally {
				X.close(in);
			}
		}

		@SuppressWarnings("restriction")
		public void csv(String name, String deli, JSObject func) throws IOException {
			csv(name, deli, (filename, e) -> {
				func.call(this, filename, e);
			});
		}

		/**
		 * @deprecated
		 * @param name
		 * @param deli
		 * @return
		 * @throws IOException
		 */
		public _TEXT text(String name, String deli) throws IOException {

			ZipInputStream in = null;

			in = new ZipInputStream(getInputStream());
			ZipEntry e = in.getNextEntry();
			while (e != null) {
				if (X.isSame(name, e.getName())) {
					return new _TEXT(in, deli);
				}
				e = in.getNextEntry();
			}
			return null;

		}

		public void text(String name, String deli, BiConsumer<String, _TEXT> func) throws IOException {

			ZipInputStream in = null;

			try {
				in = new ZipInputStream(getInputStream());
				ZipEntry e = in.getNextEntry();
				while (e != null) {
					if (e.getName().matches(name)) {
						func.accept(e.getName(), new _TEXT(in, deli));
					}
					e = in.getNextEntry();
				}
			} finally {
				X.clone(in);
			}

		}

		@SuppressWarnings("restriction")
		public void text(String name, String deli, JSObject func) throws IOException {
			text(name, deli, (filename, e) -> {
				func.call(this, filename, e);
			});
		}

	}

	public class _CSV {

		BufferedReader re = null;
		List<Character> deli;

		_CSV(InputStream in, String deli) {
			re = new BufferedReader(new InputStreamReader(in));
			if (X.isEmpty(deli)) {
				this.deli = Arrays.asList(',');
			} else {
				this.deli = X.asList(deli.toCharArray(), e -> (Character) e);
			}
		}

		public Object[] next() throws IOException {
			String line = IOUtil.readcvs(re);
			if (line == null)
				return null;
			return X.csv(line, deli);
		}

	}

	public class _TEXT {

		BufferedReader re = null;
		List<Character> deli;

		_TEXT(InputStream in, String deli) {
			re = new BufferedReader(new InputStreamReader(in));
			if (X.isEmpty(deli)) {
				this.deli = Arrays.asList(',');
			} else {
				this.deli = X.asList(deli.toCharArray(), e -> (Character) e);
			}
		}

		public Object[] next() throws IOException {
			String line = IOUtil.readcvs(re);
			if (line == null)
				return null;
			return X.csv(line, deli);
		}

		public String read() throws IOException {
			return re.readLine();
		}

	}

	public void scan(Consumer<DFile> func) throws IOException {
		DFile[] ff = this.listFiles();
		if (ff != null && ff.length > 0 && func != null) {
			for (DFile f1 : ff) {

				func.accept(f1);

				if (f1.isDirectory()) {
					f1.scan(func);
				}
			}
		}
	}

}
