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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Disk;
import org.giiwa.bean.GLog;
import org.giiwa.dao.X;
import org.giiwa.dfile.DFile;
import org.giiwa.task.BiConsumer;
import org.giiwa.web.Language;

/**
 * IO utility
 * 
 * @author wujun
 *
 */
public class IOUtil {

	private static Log log = LogFactory.getLog(IOUtil.class);

	/**
	 * the utility api of copying all data in "inputstream" to "outputstream".
	 * please refers copy(in, out, boolean)
	 *
	 * @param in  the inputstream
	 * @param out the outputstream
	 * @return int the size of copied
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static long copy(InputStream in, OutputStream out) throws IOException {
		return copy(in, out, true);
	}

	public static int delete(File f) throws IOException {
		return delete(f, -1, null);
	}

	/**
	 * delete the file or the path.
	 *
	 * @param f the file or the path
	 * @return the number deleted
	 * @throws IOException throw exception when delete the file or directory error
	 */
	public static int delete(File f, long age, Consumer<String> func) throws IOException {

		int count = 0;

		Language lang = Language.getLanguage();

		if ((f.isFile() || isLink(f)) && (age < 0 || (System.currentTimeMillis() - f.lastModified() > age))) {

			log.warn("delete file: " + f.getCanonicalPath() + ", age="
					+ (lang == null ? -1 : lang.past(f.lastModified())) + ", time=" + f.lastModified());

			if (func != null) {
				func.accept(f.getAbsolutePath());
			}

			f.delete();

			count++;
		} else if (f.isDirectory()) {
			File[] ff = f.listFiles();
			if (ff != null && ff.length > 0) {
				for (File f1 : ff) {
					count += delete(f1, age, func);
				}
			}
			ff = f.listFiles();
			if ((ff == null || ff.length == 0) && (age < 0 || (System.currentTimeMillis() - f.lastModified() > age))) {
//				log.warn("delete folder as empty: " + f.getCanonicalPath());

				f.delete();
			}

			count++;
		}
		return count;
	}

	public static int delete(DFile f, long age) throws IOException {

		int count = 0;

		Language lang = Language.getLanguage();

		if (f.isFile() && (age < 0 || (System.currentTimeMillis() - f.lastModified() > age))) {

			GLog.applog.info("dfile", "delete",
					"delete file: " + f.getFilename() + ", age=" + (lang == null ? -1 : lang.past(f.lastModified())));

			log.warn("delete dfile: " + f.getFilename() + ", age=" + (lang == null ? -1 : lang.past(f.lastModified())));

			f.delete();

			count++;
		} else if (f.isDirectory()) {

			DFile[] ff = f.listFiles();
			if (ff != null && ff.length > 0) {
				for (DFile f1 : ff) {
					count += delete(f1, age);
				}
			}

			ff = f.listFiles();
			if (ff == null || ff.length == 0) {

//				GLog.applog.info("dfile", "delete", "delete folder as empty: " + f.getFilename());

				log.warn("delete dfolder as empty: " + f.getFilename());

				f.delete();

			}

			count++;
		}
		return count;
	}

	public static int delete(DFile f) throws IOException {

		int count = 0;

		Language lang = Language.getLanguage();

		if (f.isFile()) {
			f.delete();

			GLog.applog.info("dfile", "delete",
					"delete file: " + f.getFilename() + ", age=" + (lang == null ? -1 : lang.past(f.lastModified())));
			log.warn("delete dfile: " + f.getFilename() + ", age=" + (lang == null ? -1 : lang.past(f.lastModified())));

			count++;
		} else if (f.isDirectory()) {
			try {
				DFile[] ff = f.listFiles();
				if (ff != null && ff.length > 0) {
					for (DFile f1 : ff) {
						count += delete(f1);
					}
				}
				f.delete();

				GLog.applog.info("dfile", "delete", "delete folder: " + f.getFilename() + ", age="
						+ (lang == null ? -1 : lang.past(f.lastModified())));
				log.warn("delete dfolder: " + f.getFilename() + ", age="
						+ (lang == null ? -1 : lang.past(f.lastModified())));

				count++;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return count;
	}

	private static boolean isLink(File f) throws IOException {
		return !X.isSame(f.getAbsolutePath(), f.getCanonicalPath());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static int copyDir(File src, File dest) throws IOException {
		return copyDir(src, dest, (BiConsumer) null);
	}

	/**
	 * copy files.
	 *
	 * @param src  the source file
	 * @param dest the destination file
	 * @return the number copied
	 * @throws IOException throw exception when copy failed
	 */
	public static int copyDir(File src, File dest, BiConsumer<String, Integer> func) throws IOException {
		return _copyDir(src, dest, 0, func);
	}

	private static int _copyDir(File src, File dest, int count, BiConsumer<String, Integer> func) throws IOException {

		X.IO.mkdirs(dest);
		if (src.isFile()) {
			// copy file
			count++;
			if (func != null) {
				func.accept(src.getName(), count);
			}
			copy(src, new File(dest.getCanonicalPath() + "/" + src.getName()));

		} else if (src.isDirectory()) {
			// copy dir
			File[] ff = src.listFiles();
			if (ff != null && ff.length > 0) {
				for (File f : ff) {
					count += copyDir(f, new File(dest.getCanonicalPath() + "/" + src.getName()), func);
				}
			} else {
				X.IO.mkdirs(new File(dest.getCanonicalPath() + "/" + src.getName()));
			}
		}
		return count;
	}

	public static int copyDir(DFile src, DFile dest) throws IOException {
		return copyDir(src, dest, null);
	}

	public static int copyDir(DFile src, DFile dest, BiConsumer<String, Integer> func) throws IOException {
		return _copyDir(src, dest, 0, func);
	}

	private static int _copyDir(DFile src, DFile dest, int count, BiConsumer<String, Integer> func) throws IOException {

		if (src.isFile()) {
			// copy file
			count++;
			if (func != null) {
				func.accept(src.getName(), count);
			}
			copy(src, Disk.seek(dest.getFilename() + "/" + src.getName()));

		} else if (src.isDirectory()) {
			// copy dir
			DFile[] ff = src.listFiles();
			if (ff != null && ff.length > 0) {
				for (DFile f : ff) {
					count += copyDir(f, Disk.seek(dest.getFilename() + "/" + src.getName()), func);
				}
			}
		}
		return count;
	}

	/**
	 * copy all the files except.
	 *
	 * @param src    the source dir
	 * @param dest   the destination dir
	 * @param except the files
	 * @return the number files copied
	 * @throws IOException throw IOException if error
	 */
	public static int copyDir(File src, File dest, String[] except) throws IOException {

		Set<String> ex = new HashSet<String>();
		for (String s : except) {
			ex.add(s);
		}

		X.IO.mkdirs(dest);
		int count = 0;
		if (src.isFile()) {
			// copy file
			count++;
			copy(src, new File(dest.getCanonicalPath() + "/" + src.getName()));
		} else if (src.isDirectory()) {
			// copy dir
			File[] ff = src.listFiles();
			if (ff != null && ff.length > 0) {
				for (File f : ff) {
					if (!ex.contains(f.getName())) {
						count += copyDir(f, new File(dest.getCanonicalPath() + "/" + src.getName()));
					}
				}
			} else {
				X.IO.mkdirs(new File(dest.getCanonicalPath() + "/" + src.getName()));
			}
		}
		return count;
	}

	/**
	 * copy file src to file destination.
	 *
	 * @param src  the source file
	 * @param dest the destination file
	 * @return int of copied
	 * @throws IOException throw exception when copy file failed
	 */
	public static long copy(File src, File dest) throws IOException {

		if (src.isDirectory()) {
			return IOUtil.copyDir(src, dest);
		} else if (src.isFile()) {
			X.IO.mkdirs(dest.getParentFile());
			long n = copy(new FileInputStream(src), new FileOutputStream(dest), true);

			return n;
		}
		return 0;

	}

	public static long copy(DFile src, DFile dest) throws IOException {
		return copy(src.getInputStream(), dest.getOutputStream(), true);
	}

	/**
	 * copy the data in "inputstream" to "outputstream", from start to end.
	 *
	 * @param in             the inputstream
	 * @param out            the outputstream
	 * @param start          the start position of started
	 * @param end            the end position of ended
	 * @param closeAfterDone close after done, true: close if done, false: not close
	 * @return int the size of copied
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static int copy(InputStream in, OutputStream out, long start, long end, boolean closeAfterDone)
			throws IOException {

		try {
			if (in == null || out == null)
				return 0;

			byte[] bb = new byte[1024 * 16];
			int total = 0;

			// log.debug("skip=" + start);
			if (start > 0) {
				in.skip(start);
			}

			int ii = (int) Math.min((end - start + 1), bb.length);
			int len = in.read(bb, 0, ii);
			// log.debug("len=" + len + ", ii=" + ii);

			while (len > 0) {
				out.write(bb, 0, len);

				total += len;
				ii = (int) Math.min((end - start - total + 1), bb.length);
				if (ii > 0) {
					len = in.read(bb, 0, ii);
					// log.debug("len=" + len + ", ii=" + ii);
				} else {
					len = 0;
				}
			}
			out.flush();
			return total;
		} finally {
			if (closeAfterDone) {
				X.close(in, out);
			}
		}
	}

	/**
	 * Copy data in "inputstream" to "outputstream".
	 *
	 * @param in             the inputstream
	 * @param out            the outputstream
	 * @param closeAfterDone close after done, true: close if done, false: not close
	 * @return int the size of copied
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static long copy(InputStream in, OutputStream out, boolean closeAfterDone) throws IOException {

		try {
			if (in == null || out == null)
				return 0;

			byte[] bb = new byte[1024 * 16];

			long total = 0;
			int len = in.read(bb);
			while (len > 0) {
				out.write(bb, 0, len);
				total += len;
				len = in.read(bb);
//				if (Console._DEBUG) {
//					Console.inst.log("downloading ... " + len);
//				}
			}
			out.flush();
			return total;

		} finally {
			if (closeAfterDone) {
				X.close(in, out);
			}
		}
	}

	public static String read(File f, String encoding) {

		FileInputStream in = null;

		try {
			if (X.isEmpty(encoding)) {
				encoding = "UTF-8";
			}
			in = new FileInputStream(f);

			return read(in, encoding);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			X.close(in);
		}
		return null;
	}

	public static void write(File f, String encoding, String str) {

		FileOutputStream out = null;

		try {
			if (X.isEmpty(encoding)) {
				encoding = "UTF-8";
			}
			out = new FileOutputStream(f);

			write(out, encoding, str);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			X.close(out);
		}
	}

	public static void write(OutputStream out, String encoding, String str) {

		BufferedWriter wri = null;

		try {
			if (X.isEmpty(encoding)) {
				encoding = "UTF-8";
			}
			wri = new BufferedWriter(new OutputStreamWriter(out, encoding));
			wri.write(str);
			wri.flush();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}

	public static void saveObjectTo(Object obj, OutputStream out) throws Exception {

		try {

			byte[] bb = X.getBytes(obj);
			out.write(bb);

		} finally {
			X.close(out);
		}

	}

	public static Object readObjectFrom(InputStream in) throws Exception {

		try {

			byte[] bb = new byte[in.available()];
			in.read(bb);

			return X.fromBytes(bb);
		} finally {
			X.close(in);
		}
	}

	public static String read(InputStream in, String encoding) {

		StringBuilder sb = new StringBuilder();

		BufferedReader read = null;

		try {
			if (X.isEmpty(encoding)) {
				encoding = "UTF-8";
			}
			read = new BufferedReader(new InputStreamReader(in, encoding));
			String line = null;
			while ((line = read.readLine()) != null) {
				sb.append(line).append("\n");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return sb.toString();

	}

	public static byte[] read(InputStream in) {

		byte[] bb = null;
		try {
			bb = new byte[in.available()];
			in.read(bb, 0, bb.length);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return bb;

	}

	public static long count(DFile f) {
		if (f == null) {
			return 0;
		}

		long n = 0;
		if (f.isFile()) {
			n = 1;
		} else if (f.isDirectory()) {
			try {
				DFile[] ff = f.listFiles();
				if (ff != null) {
					for (DFile f1 : ff) {
						n += count(f1);
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return n;

	}

	public static long count(File f) {
		if (f == null) {
			return 0;
		}

		long n = 0;
		if (f.isFile()) {
			n = 1;
		} else if (f.isDirectory()) {
			File[] ff = f.listFiles();
			if (ff != null) {
				for (File f1 : ff) {
					n += count(f1);
				}
			}
		}
		return n;
	}

	/**
	 * delete the file if length=0, or empty folder
	 * 
	 * @param f
	 */
	public static void cleanup(File f) {

		if (f.isFile()) {
			if (f.length() == 0) {
				f.delete();
			}
			return;
		}

		if (f.isDirectory()) {
			File[] ff = f.listFiles();
			if (ff == null || ff.length == 0) {
				f.delete();
			} else {
				for (File f1 : ff) {
					cleanup(f1);
				}
				ff = f.listFiles();
				if (ff == null || ff.length == 0) {
					f.delete();
				}
			}
		}

	}

	/**
	 * delete the file if length=0 or folder if empty
	 * 
	 * @param f
	 * @throws IOException
	 */
	public static void cleanup(DFile f) throws IOException {
		if (f.isFile()) {
			if (f.length() == 0) {
				f.delete();
			}
			return;
		}

		if (f.isDirectory()) {
			DFile[] ff = f.listFiles();
			if (ff == null || ff.length == 0) {
				f.delete();
			} else {
				for (DFile f1 : ff) {
					cleanup(f1);
				}
				ff = f.listFiles();
				if (ff == null || ff.length == 0) {
					f.delete();
				}
			}
		}
	}

	/**
	 * @Deprecated replace by readcsv
	 * @param re
	 * @return
	 * @throws IOException
	 */
	public static String readcvs(BufferedReader re) throws IOException {
		return readcsv(re);
	}

	/**
	 * 
	 * @param re
	 * @return
	 * @throws IOException
	 */
	public static String readcsv(BufferedReader re) throws IOException {
		String line = re.readLine();
		if (log.isDebugEnabled())
			log.debug("line=" + line);
		while (line != null && ((count(line, "\"") & 1) == 1)) {
			String s1 = re.readLine();
			if (s1 == null) {
				return line;
			}
			line += "\r\n" + s1;

			if (log.isDebugEnabled())
				log.debug("link, line=" + line);

		}
		return line;
	}

	private static int count(String line, String substr) {
		if (substr.length() == 0)
			return 0;

		int n = 0;
		int i = line.indexOf(substr);
		while (i >= 0) {
			n++;
			i = line.indexOf(substr, i + substr.length());
		}
		return n;
	}

	public static void lines(String lines, BiConsumer<String, BufferedReader> func) {
		BufferedReader re = null;

		try {
			re = new BufferedReader(new StringReader(lines));
			String line = re.readLine();
			while (line != null) {
				func.accept(line, re);
				line = re.readLine();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			X.close(re);
		}

	}

	public static boolean mkdirs(File f) {

		// create one by one instead of mkdirs (which may cause bug, the dir-> 777);
		if (f.exists()) {
			return true;
		}

		boolean b = f.mkdirs();

		f.setReadable(false, false);
		f.setWritable(false, false);
		f.setExecutable(false, false);

		f.setReadable(true, true);
		f.setWritable(true, true);
		f.setExecutable(true, true);

		return b;

	}

}
