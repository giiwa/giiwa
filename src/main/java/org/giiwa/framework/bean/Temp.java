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
package org.giiwa.framework.bean;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.base.IOUtil;
import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;
import org.giiwa.core.dfile.DFile;
import org.giiwa.core.task.Callable;
import org.giiwa.framework.web.Model;
import org.h2.util.IOUtils;

/**
 * Create Temporary file, which can be accessed by web api, please refer
 * model("/temp")
 * 
 * @author joe
 *
 */
public class Temp {

	static Log log = LogFactory.getLog(Temp.class);

	private static String ROOT = "/temp";

	/**
	 * Initialize the Temp object, this will be invoke in giiwa startup.
	 *
	 * @param conf the conf
	 */
	public static void init(Configuration conf) {

		log.info("temp has been initialized.");

	}

	private String id = null;
	private String name = null;
	private File file = null;

	private Temp() {

	}

	/**
	 * get the Temp file for name.
	 *
	 * @param name the file name
	 * @return the Temp
	 */
	public static Temp create(String name) {
		Temp t = new Temp();
		t.name = name;
		t.id = UID.id(System.currentTimeMillis(), UID.random());
		return t;
	}

	/**
	 * get the Id
	 * 
	 * @return String of id
	 */
	public String getId() {
		return id;
	}

	/**
	 * get the File
	 * 
	 * @return File the file
	 */
	public File getFile() {
		if (file == null) {
			file = new File(Model.GIIWA_HOME + path(id, name));
		}
		return file;
	}

	public File getLocalFile() {
		return getFile();
	}

	/**
	 * get the web access uri directly
	 * 
	 * @return String of uri
	 */
	public String getUri() {
		return ROOT + "/" + id + "/" + name + "?" + ((file == null || !file.exists()) ? 0 : file.lastModified());
	}

	public static File get(String id, String name) {
		return new File(Model.GIIWA_HOME + path(id, name));
	}

	public static String path(String path, String name) {
		long id = Math.abs(UID.hash(path));
		char p1 = (char) (id % 23 + 'a');
		char p2 = (char) (id % 19 + 'A');
		char p3 = (char) (id % 17 + 'a');
		char p4 = (char) (id % 13 + 'A');

		StringBuilder sb = new StringBuilder(ROOT);

		sb.append("/").append(p1).append("/").append(p2).append("/").append(p3).append("/").append(p4).append("/")
				.append(id);

		if (name != null)
			sb.append("_").append(name);

		return sb.toString();
	}

	public <V extends Bean> Exporter<V> export(String charset, Exporter.FORMAT format) {
		File f = this.getLocalFile();
		if (f.exists()) {
			f.delete();
		} else {
			f.getParentFile().mkdirs();
		}
		return export(f, charset, format);
	}

	public <V extends Bean> Exporter<V> export(File f, String charset, Exporter.FORMAT format) {
		Exporter<V> e = Exporter.create(f, charset, format);
		return e;
	}

	public static class Exporter<V extends Bean> {
		public enum FORMAT {
			csv
		}

		BufferedWriter out = null;
		FORMAT format;
		Callable<Object[], V> cols = null;

		private static <V extends Bean> Exporter<V> create(File file, String charset, FORMAT format) {
			try {
				Exporter<V> s = new Exporter<V>();
				s.out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), charset));
				s.format = format;

				return s;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			return null;
		}

		public Exporter<V> createSheet(Callable<Object[], V> cols) {
			this.cols = cols;
			return this;
		}

		public void close() {
			X.close(out);
			out = null;
		}

		public void print(String... cols) throws IOException {
			if (out == null) {
				throw new IOException("out is null?");
			}

			for (int i = 0; i < cols.length; i++) {
				String s = cols[i];
				if (i > 0) {
					out.write(",");
				}
				out.write("\"");
				if (s != null) {
					s = s.replaceAll("\"", "\\\"").replaceAll("\r\n", "");
					out.write(s);
				}
				out.write("\"");
			}
			out.write("\r\n");
		}

		public void print(List<V> bs) throws IOException {
			if (out == null) {
				throw new IOException("out is null? ");
			}

			if (bs != null && !bs.isEmpty()) {
				for (V b : bs) {
					print(b);
				}
			}
		}

		public void print(V b) throws IOException {
			if (out == null) {
				throw new IOException("out is null? ");
			}

			Object[] ss = cols.call(200, b);
			if (ss != null) {

				for (int i = 0; i < ss.length; i++) {
					if (i > 0) {
						out.write(",");
					}

					out.write("\"");

					Object o = ss[i];

					String s = X.EMPTY;
					if (o != null) {
						if (o instanceof String) {
							s = ((String) o).replaceAll("\"", "\\\"").replaceAll("\r\n", "");
						} else {
							s = o.toString();
						}
						out.write(s);
					}
					out.write("\"");
				}
				out.write("\r\n");
			}

		}

	}

	public long save(DFile f) throws Exception {
		if (f.exists()) {
			f.delete();
		}

		File file = getFile();
		if (file != null && file.exists()) {
			return IOUtil.copy(new FileInputStream(file), f.getOutputStream());
		}
		return 0;
	}

	public void delete() throws IOException {

		File f = this.getFile();
		delete(f);

	}

	private void delete(File f) throws IOException {
		IOUtil.delete(f);
		cleanup(f.getParentFile());
	}

	private static void cleanup(File f) throws IOException {

		if (f == null || f.isFile())
			return;

		String root = X.getCanonicalPath(Model.GIIWA_HOME + ROOT);
		if (root.startsWith(f.getCanonicalPath()) || X.isSame(root, f.getCanonicalPath())) {
			return;
		}

		File[] ff = f.listFiles();
		if (ff == null || ff.length == 0) {
			f.delete();
			cleanup(f.getParentFile());
		} else {
			for (File f1 : ff) {
				if (f1.isFile()) {
					return;
				} else {
					IOUtil.cleanup(f1);
				}
			}

			ff = f.listFiles();
			if (ff == null || ff.length == 0) {
				f.delete();
				cleanup(f.getParentFile());
			}
		}
	}

	public static void cleanup(long age) {

		{
			try {
				File f1 = new File(Model.GIIWA_HOME + ROOT);
				File[] ff = f1.listFiles();
				if (ff == null || ff.length == 0) {
					f1.delete();
					cleanup(f1);
				} else {
					for (File f2 : ff) {
						IOUtil.delete(f2, age);
					}

					ff = f1.listFiles();
					if (ff == null || ff.length == 0) {
						f1.delete();
						cleanup(f1.getParentFile());
					}

				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	public long copy(InputStream in) throws IOException {
		File f = getFile();
		if (f.exists()) {
			f.delete();
		} else {
			f.getParentFile().mkdirs();
		}
		return IOUtils.copy(in, new FileOutputStream(f));
	}

	public long zipcopy(String name, InputStream in) throws IOException {

		File f = getFile();
		if (f.exists()) {
			f.delete();
		} else {
			f.getParentFile().mkdirs();
		}
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f));
		try {
			ZipEntry e = new ZipEntry(name);
			out.putNextEntry(e);
			return IOUtil.copy(in, out, false);
		} finally {
			out.closeEntry();
			X.close(in, out);
		}

	}

}
