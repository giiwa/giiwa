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

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.base.IOUtil;
import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;
import org.giiwa.core.task.Callable;

/**
 * Create Temporary file, which can be accessed by web api, please refer
 * model("/temp")
 * 
 * @author joe
 *
 */
public class Temp {

	static Log log = LogFactory.getLog(Temp.class);

	public static String ROOT;

	/**
	 * Initialize the Temp object, this will be invoke in giiwa startup.
	 *
	 * @param conf
	 *            the conf
	 */
	public static void init(Configuration conf) {
		ROOT = conf.getString("temp.path", "/opt/temp/");

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
	 * @param name
	 *            the file name
	 * @return the Temp
	 */
	public static Temp create(String name) {
		Temp t = new Temp();
		t.name = name;
		t.id = UID.id(System.currentTimeMillis(), UID.random());
		t.file = get(t.id, name);
		if (t.file.exists()) {
			t.file.delete();
		} else {
			t.file.getParentFile().mkdirs();
		}
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
		return file;
	}

	/**
	 * get the web access uri directly
	 * 
	 * @return String of uri
	 */
	public String getUri() {
		return "/temp/" + id + "/" + name + "?" + ((file == null || !file.exists()) ? 0 : file.lastModified());
	}

	/**
	 * Gets the.
	 * 
	 * @param id
	 *            the id
	 * @param name
	 *            the name
	 * @return the file
	 */
	public static File get(String id, String name) {
		return new File(path(id, name));
	}

	static private String path(String path, String name) {
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

	/**
	 * Copy.
	 *
	 * @param f
	 *            the f
	 * @return the int
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public int copy(File f) throws IOException {
		return copy(new FileInputStream(f));
	}

	/**
	 * Copy.
	 *
	 * @param in
	 *            the in
	 * @return the int
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public int copy(InputStream in) throws IOException {
		FileOutputStream out = new FileOutputStream(this.getFile());
		return IOUtil.copy(in, out);
	}

	/**
	 * Zipcopy.
	 *
	 * @param name
	 *            the name
	 * @param f
	 *            the f
	 * @return the int
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public int zipcopy(String name, File f) throws IOException {
		return zipcopy(name, new FileInputStream(f));
	}

	/**
	 * Zipcopy.
	 *
	 * @param name
	 *            the name
	 * @param in
	 *            the in
	 * @return the int
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public int zipcopy(String name, InputStream in) throws IOException {
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(this.getFile()));
		ZipEntry e = new ZipEntry(name);
		out.putNextEntry(e);
		int i = IOUtil.copy(in, out, false);
		out.closeEntry();
		out.close();
		in.close();
		return i;
	}

	@SuppressWarnings("unchecked")
	public <V extends Bean> Exporter<V> export(String charset, Exporter.FORMAT format) {
		Exporter<V> e = Exporter.create(this.getFile(), charset, format);
		return e;
	}

	public static class Exporter<V extends Bean> {
		public enum FORMAT {
			csv
		}

		BufferedWriter out = null;
		FORMAT format;
		Callable<Object[], V> cols = null;

		@SuppressWarnings({ "rawtypes", "unchecked" })
		private static Exporter create(File file, String charset, FORMAT format) {
			try {
				Exporter s = new Exporter();
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

	private static void _cleanup(String path, long expired) {

		try {
			File f = new File(path);

			/**
			 * test the file last modified exceed the cache time
			 */
			if (f.isFile()) {
				if (System.currentTimeMillis() - f.lastModified() > expired) {
					IOUtil.delete(f);
				}
			} else if (f.isDirectory()) {
				File[] list = f.listFiles();
				if (list == null || list.length == 0) {
					IOUtil.delete(f);
				} else if (list != null) {
					/**
					 * cleanup the sub folder
					 */
					for (File f1 : list) {
						_cleanup(f1.getAbsolutePath(), expired);
					}
				}
			}
		} catch (Exception e) {
			if (log.isErrorEnabled()) {
				log.error(e.getMessage(), e);
				GLog.applog.error("cleanup", "cleanup", e.getMessage(), e, null, null);
			}
		}

	}

	public static void cleanup(long expired) {
		if (!X.isEmpty(ROOT)) {
			_cleanup(ROOT, expired);
		}
	}

}
