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
package org.giiwa.core.base;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.X;
import org.giiwa.core.dfile.DFile;

/**
 * IO utility
 * 
 * @author wujun
 *
 */
public final class IOUtil {

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
	public static int copy(InputStream in, OutputStream out) throws IOException {
		return copy(in, out, true);
	}

	public static int delete(File f) throws IOException {
		return delete(f, -1);
	}

	/**
	 * delete the file or the path.
	 *
	 * @param f the file or the path
	 * @return the number deleted
	 * @throws IOException throw exception when delete the file or directory error
	 */
	public static int delete(File f, long age) throws IOException {
		int count = 0;

		if ((f.isFile() || isLink(f)) && (age < 0 || System.currentTimeMillis() - f.lastModified() > age)) {
			f.delete();

			if (log.isInfoEnabled()) {
				log.info("delete file: " + f.getCanonicalPath());
			}

			count++;
		} else if (f.isDirectory()) {
			File[] ff = f.listFiles();
			if (ff != null && ff.length > 0) {
				for (File f1 : ff) {
					count += delete(f1, age);
				}
			}
			ff = f.listFiles();
			if (ff == null || ff.length == 0) {
				f.delete();
				if (log.isInfoEnabled()) {
					log.info("delete file: " + f.getCanonicalPath());
				}
			}

			count++;
		}
		return count;
	}

	public static int delete(DFile f, long age) throws IOException {
		int count = 0;

		if (f.isFile() && (age < 0 || (System.currentTimeMillis() - f.lastModified() > age))) {

			f.delete();

			if (log.isInfoEnabled()) {
				log.info("delete file: " + f.getCanonicalPath());
			}

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
				f.delete();
				if (log.isInfoEnabled()) {
					log.info("delete folder: " + f.getCanonicalPath());
				}
			}

			count++;
		}
		return count;
	}

	public static int delete(DFile f) throws IOException {
		int count = 0;

		if (f.isFile()) {
			f.delete();

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
				// if (log.isInfoEnabled()) {
				// log.info("delete folder: " + f.getCanonicalPath());
				// }

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

	/**
	 * copy files.
	 *
	 * @param src  the source file
	 * @param dest the destination file
	 * @return the number copied
	 * @throws IOException throw exception when copy failed
	 */
	public static int copyDir(File src, File dest) throws IOException {
		dest.mkdirs();
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
					count += copyDir(f, new File(dest.getCanonicalPath() + "/" + src.getName()));
				}
			} else {
				new File(dest.getCanonicalPath() + "/" + src.getName()).mkdirs();
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

		dest.mkdirs();
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
				new File(dest.getCanonicalPath() + "/" + src.getName()).mkdirs();
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
	public static int copy(File src, File dest) throws IOException {
		if (src.isDirectory()) {
			return IOUtil.copyDir(src, dest);
		} else if (src.isFile()) {
			dest.getParentFile().mkdirs();
			return copy(new FileInputStream(src), new FileOutputStream(dest), true);
		}
		return 0;
	}

	public static int copy(DFile src, DFile dest) throws IOException {
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

			byte[] bb = new byte[1024 * 32];
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
				out.flush();

				total += len;
				ii = (int) Math.min((end - start - total + 1), bb.length);
				if (ii > 0) {
					len = in.read(bb, 0, ii);
					// log.debug("len=" + len + ", ii=" + ii);
				} else {
					len = 0;
				}
			}
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
	public static int copy(InputStream in, OutputStream out, boolean closeAfterDone) throws IOException {

		try {
			if (in == null || out == null)
				return 0;

			byte[] bb = new byte[1024 * 4];
			int total = 0;
			int len = in.read(bb);
			while (len > 0) {
				out.write(bb, 0, len);
				total += len;
				len = in.read(bb);
				out.flush();
			}
			return total;
		} finally {
			if (closeAfterDone) {
				X.close(in, out);
			}
		}
	}

	public static String read(File f, String encoding) {
		StringBuilder sb = new StringBuilder();

		BufferedReader in = null;

		try {
			if (X.isEmpty(encoding)) {
				encoding = "UTF-8";
			}
			in = new BufferedReader(new InputStreamReader(new FileInputStream(f), encoding));
			String line = null;
			while ((line = in.readLine()) != null) {
				sb.append(line).append("\r\n");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			X.close(in);
		}
		return sb.toString();
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

	public static String readcvs(BufferedReader re) throws IOException {
		String line = re.readLine();
		while (line != null && ((count(line, "\"") & 1) == 1)) {
			String s1 = re.readLine();
			if (s1 == null) {
				return line;
			}
			line += "\r\n" + s1;
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

}
