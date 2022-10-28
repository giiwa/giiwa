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
package org.giiwa.misc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.dao.X;
import org.giiwa.dfile.DFile;
import org.giiwa.json.JSON;
import org.giiwa.task.BiFunction;

import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;

/**
 * The {@code Zip} Class used to Zip File operations,
 * 
 * @author joe
 *
 */
public class Zip {

	private static Log log = LogFactory.getLog(Zip.class);

	/**
	 * Zip.
	 * 
	 * @param b the b
	 * @return the byte[]
	 * @throws Exception the exception
	 */
	public static byte[] zip(byte[] b) throws IOException {

		Deflater def = new Deflater();
		def.setInput(b);
		def.finish();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];

		while (!def.finished()) {
			int l = def.deflate(buf);
			out.write(buf, 0, l);
		}

		out.flush();

		out.close();

		return out.toByteArray();
	}

	/**
	 * Unzip.
	 * 
	 * @param b the b
	 * @return the byte[]
	 * @throws Exception the exception
	 */
	public static byte[] unzip(byte[] b) throws Exception {
		Inflater in = new Inflater();
		in.setInput(b);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		while (!in.finished()) {
			int l = in.inflate(buf);
			out.write(buf, 0, l);
		}

		in.end();
		out.flush();
		out.close();
		return out.toByteArray();
	}

	/**
	 * find the filename in zip file.
	 *
	 * @param filename the filename
	 * @param zip      the zip
	 * @return InputStream
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static InputStream find(String filename, ZipFile zip) throws IOException {

		ZipEntry e = zip.getEntry(filename);
		if (e != null) {
			return zip.getInputStream(e);
		}
		return null;

	}

	public static InputStream find(String filename, InputStream in) throws IOException {

//		log.debug("finding ... [" + filename + "]");
		ZipInputStream zip = new ZipInputStream(in);

		ZipEntry e = zip.getNextEntry();
		while (e != null) {

//			log.debug("next ... [" + e.getName() + "]");

			if (X.isSame(e.getName(), filename)) {
				return zip;
			}
			e = zip.getNextEntry();
		}

		X.close(in);

		return null;

	}

	/**
	 * find filename in the zipfile
	 * 
	 * @param filename
	 * @param zipfile  zip or rar(<5)
	 * @return
	 * @throws Exception
	 */
	public static InputStream find(String filename, DFile zipfile) throws Exception {

		String name = zipfile.getName().toLowerCase();
		if (name.endsWith(".rar")) {

			Archive a = null;
			try {
				a = new Archive(zipfile.getInputStream());

				FileHeader e = a.nextFileHeader();
				while (e != null) {

					if (X.isSame(e.getFileName(), filename)) {
						return a.getInputStream(e);
					}

					e = a.nextFileHeader();
				}
			} finally {
				X.close(a);
			}

		} else {
			// zip

			ZipInputStream zip = null;

			try {
				zip = new ZipInputStream(zipfile.getInputStream());

				ZipEntry e = zip.getNextEntry();
				while (e != null) {

					if (X.isSame(e.getName(), filename)) {
						return zip;
					}
					e = zip.getNextEntry();
				}
			} catch (Exception e) {
				X.close(zip);
				throw e;
			}

		}

		return null;

	}

	/**
	 * find the filename in the zip file
	 * 
	 * @param filename
	 * @param zipfile  zip or rar(<5)
	 * @return
	 * @throws Exception
	 */
	public static InputStream find(String filename, File zipfile) throws Exception {

		String name = zipfile.getName().toLowerCase();
		if (name.endsWith(".rar")) {

			Archive a = null;
			try {
				a = new Archive(new FileInputStream(zipfile));

				FileHeader e = a.nextFileHeader();
				while (e != null) {

					if (X.isSame(e.getFileName(), filename)) {
						return a.getInputStream(e);
					}

					e = a.nextFileHeader();
				}
			} finally {
				X.close(a);
			}

		} else {
			// zip

			ZipInputStream zip = null;

			try {
				zip = new ZipInputStream(new FileInputStream(zipfile));

				ZipEntry e = zip.getNextEntry();
				while (e != null) {

					if (X.isSame(e.getName(), filename)) {
						return zip;
					}
					e = zip.getNextEntry();
				}
			} catch (Exception e) {
				X.close(zip);
				throw e;
			}

		}

		return null;

	}

	/**
	 * get json object from the filename in zip file.
	 *
	 * @param filename the filename
	 * @param zip      the zip
	 * @return JSONObject
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static JSON getJSON(String filename, ZipFile zip) throws IOException {

		InputStream in = find(filename, zip);
		if (in != null) {
			ByteArrayOutputStream out = null;
			try {
				out = new ByteArrayOutputStream();
				byte[] bb = new byte[16 * 1024];
				int len = in.read(bb);
				while (len > 0) {
					out.write(bb, 0, len);
					len = in.read(bb);
				}
				bb = null;

				out.flush();

				return JSON.fromObject(out.toByteArray());
			} finally {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
			}
		}

		return null;
	}

	public static JSON json(String filename, InputStream zip) throws IOException {

		InputStream in = find(filename, zip);
		if (in != null) {
			ByteArrayOutputStream out = null;
			try {
				out = new ByteArrayOutputStream();
				byte[] bb = new byte[16 * 1024];
				int len = in.read(bb);
				while (len > 0) {
					out.write(bb, 0, len);
					len = in.read(bb);
				}
				bb = null;

				out.flush();

				return JSON.fromObject(out.toByteArray());
			} finally {
				X.close(in, out);
			}
		}

		return null;
	}

	public static List<JSON> jsons(String filename, InputStream zip) throws IOException {

		InputStream in = find(filename, zip);
		if (in != null) {
			ByteArrayOutputStream out = null;
			try {
				out = new ByteArrayOutputStream();
				byte[] bb = new byte[16 * 1024];
				int len = in.read(bb);
				while (len > 0) {
					out.write(bb, 0, len);
					len = in.read(bb);
				}
				bb = null;

				out.flush();

				return JSON.fromObjects(out.toByteArray());
			} finally {
				X.close(in, out);
			}
		}

		return null;
	}

	/**
	 * Unzip the src file to output place.
	 *
	 * @param zipfile the src zip file
	 * @param out     the out
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void unzip(File zipfile, File out) throws IOException {
		X.IO.mkdirs(out);
		ZipInputStream zip = new ZipInputStream(new FileInputStream(zipfile));
		try {

			ZipEntry e = zip.getNextEntry();
			while (e != null) {

				String name = e.getName();
				if (e.isDirectory()) {
					X.IO.mkdirs(new File(out.getCanonicalPath() + "/" + name));
				} else {
					File f = new File(out.getCanonicalPath() + "/" + name);
					X.IO.mkdirs(f.getParentFile());
					FileOutputStream o = new FileOutputStream(f);
					try {
						IOUtil.copy(zip, o, false);
					} finally {
						o.close();
					}
				}
				e = zip.getNextEntry();
			}
		} finally {
			zip.close();
		}
	}

	/**
	 * Zip the src to out file.
	 *
	 * @param zipfile the out zip file
	 * @param src     the src file or directory
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void zip(File zipfile, File src) throws IOException {
		X.IO.mkdirs(zipfile.getParentFile());
		ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(zipfile));
		try {
			if (src.exists()) {
				_zip(zip, src, src.getCanonicalPath());
			}
		} finally {
			zip.close();
		}
	}

	public static void zip(DFile zipfile, File src) throws IOException {

		zipfile.getParentFile().mkdirs();
		ZipOutputStream zip = new ZipOutputStream(zipfile.getOutputStream());
		try {
			if (src.exists()) {
				_zip(zip, src, src.getCanonicalPath());
			}
		} finally {
			zip.close();
		}
	}

	private static void _zip(ZipOutputStream out, File f, String working) throws IOException {
		if (f.isFile()) {
			String name = f.getCanonicalPath().replace(working, "");

			if (log.isDebugEnabled())
				log.debug("name=" + name);

			ZipEntry e = new ZipEntry(name);
			out.putNextEntry(e);

			InputStream in = new FileInputStream(f);
			try {
				IOUtil.copy(in, out, false);
			} finally {
				out.closeEntry();
				in.close();
			}
		} else if (f.isDirectory()) {
			String name = f.getCanonicalPath().replace(working, "") + "/";
			if (log.isDebugEnabled())
				log.debug("name=" + name);
			ZipEntry e = new ZipEntry(name);
			out.putNextEntry(e);
			out.closeEntry();

			File[] ff = f.listFiles();
			if (ff != null) {
				for (File f1 : ff) {
					_zip(out, f1, working);
				}
			}
		}
	}

	/**
	 * scan the zipfile and callback
	 * 
	 * @param zipfile
	 * @param func    (filename, inputstream), return true if continue, false to
	 *                stop scan
	 * @throws Exception
	 */
	public static void scan(DFile zipfile, BiFunction<String, InputStream, Boolean> func) throws Exception {

		String name = zipfile.getName().toLowerCase();
		if (name.endsWith(".rar")) {
			// rar

			Archive a = null;
			try {
				a = new Archive(zipfile.getInputStream());

				FileHeader e = a.nextFileHeader();
				while (e != null) {

					if (!e.isDirectory()) {
						InputStream in = null;
						try {
							in = a.getInputStream(e);
							if (func != null) {
								if (!func.apply(e.getFileName(), in)) {
									return;
								}
							}
						} finally {
							X.close(in);
						}
					}

					e = a.nextFileHeader();
				}
			} finally {
				X.close(a);
			}

		} else {
			// zip

			ZipInputStream zip = null;

			try {
				zip = new ZipInputStream(zipfile.getInputStream());

				ZipEntry e = zip.getNextEntry();
				while (e != null) {

					if (!e.isDirectory()) {
						if (func != null) {
							if (!func.apply(e.getName(), zip)) {
								return;
							}
						}
					}

					e = zip.getNextEntry();
				}
			} finally {
				X.close(zip);
			}

		}

	}

	/**
	 * scan the zipfile
	 * 
	 * @param zipfile
	 * @param func    (filename, inputstram), return true if continue, false to stop
	 * @throws Exception
	 */
	public static void scan(File zipfile, BiFunction<String, InputStream, Boolean> func) throws Exception {

		String name = zipfile.getName().toLowerCase();
		if (name.endsWith(".rar")) {
			// rar

			Archive a = null;
			try {
				a = new Archive(new FileInputStream(zipfile));

				FileHeader e = a.nextFileHeader();
				while (e != null) {

					if (!e.isDirectory()) {
						InputStream in = null;
						try {
							in = a.getInputStream(e);
							if (func != null) {
								if (!func.apply(e.getFileName(), in)) {
									return;
								}
							}
						} finally {
							X.close(in);
						}
					}

					e = a.nextFileHeader();
				}
			} finally {
				X.close(a);
			}

		} else {
			// zip

			ZipInputStream zip = null;

			try {
				zip = new ZipInputStream(new FileInputStream(zipfile));

				ZipEntry e = zip.getNextEntry();
				while (e != null) {

					if (!e.isDirectory()) {
						if (func != null) {
							if (!func.apply(e.getName(), zip)) {
								return;
							}
						}
					}

					e = zip.getNextEntry();
				}
			} finally {
				X.close(zip);
			}

		}

	}

	public static void main(String[] args) {

		String filename = "/Users/joe/e/项目/北京统御/ZDL/input.zip";
		filename = "/Users/joe/Downloads/夏令营试卷.rar";

		try {
			Zip.scan(new File(filename), (filename1, in) -> {
				System.out.println(filename1);
				X.close(in);
				return true;
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
