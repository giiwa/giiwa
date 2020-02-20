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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.base.Exporter;
import org.giiwa.core.base.IOUtil;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;
import org.giiwa.core.dfile.DFile;
import org.giiwa.framework.web.Controller;
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

	public static String ROOT = "/temp";

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
	private String root = null;

	private Temp() {

	}

	private static Temp create(String root, String name) {
		Temp t = new Temp();
		t.id = UID.id(System.currentTimeMillis(), UID.random());
		t.root = root;
		t.name = name;

		return t;
	}

	/**
	 * get the Temp file for name.
	 *
	 * @param name the file name
	 * @return the Temp
	 */
	public static Temp create(String name) {
		return create(Controller.GIIWA_HOME, name);
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
			file = new File(root + path(id, name));
		}
		return file;
	}

	public File setFile(String filename) throws IOException {
		String r1 = new File(root + "/" + ROOT + "/").getCanonicalPath();
		file = new File(r1 + "/" + filename);
		if (!file.getCanonicalPath().startsWith(r1)) {
			file = null;
			throw new IOException("bad filename=" + filename);
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
		return new File(Controller.GIIWA_HOME + path(id, name));
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

	public <V> Exporter<V> export(String charset, Exporter.FORMAT format) {
		File f = this.getLocalFile();
		if (f.exists()) {
			f.delete();
		} else {
			f.getParentFile().mkdirs();
		}
		return export(f, charset, format);
	}

	public <V> Exporter<V> export(File f, String charset, Exporter.FORMAT format) {
		Exporter<V> e = Exporter.create(f, charset, format);
		return e;
	}

	public long save(DFile f) throws Exception {
		return f.save(this.getFile());
	}

	public long save(String filename) throws Exception {
		DFile f = Disk.seek(filename);
		return save(f);
	}

	public void delete() {

		try {
			File f = this.getFile();
			delete(f);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void delete(File f) throws IOException {
		IOUtil.delete(f);
	}

	public static int cleanup(long age) {

		int count = 0;
		try {
			File f1 = new File(Controller.GIIWA_HOME + ROOT);
			File[] ff = f1.listFiles();
			if (ff != null) {
				for (File f2 : ff) {
					count += IOUtil.delete(f2, age);
				}

			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return count;
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
