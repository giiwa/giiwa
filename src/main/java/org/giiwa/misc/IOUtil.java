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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Disk;
import org.giiwa.bean.GLog;
import org.giiwa.conf.Global;
import org.giiwa.dao.X;
import org.giiwa.dfile.DFile;
import org.giiwa.task.BiConsumer;
import org.giiwa.web.Language;

/**
 * IO工具类
 * 
 * @author wujun
 *
 */
public class IOUtil {

	private static Log log = LogFactory.getLog(IOUtil.class);

	public static int BUFFER_SIZE = 1024 * 1024 * 4;

	/**
	 * 复制输入流到输出流，并关闭输入输出流
	 *
	 * @param in  - 输入流
	 * @param out - 输出流
	 * @return 复制总字节数
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static long copy(InputStream in, OutputStream out) throws IOException {
		return copy(in, out, true);
	}

	/**
	 * 删除本地文件
	 * 
	 * @param f - 本地文件
	 * @return 删除文件总个数
	 * @throws IOException
	 */
	public static int delete(File f) throws IOException {
		return delete(f, -1, null);
	}

	/**
	 * 删除本地文件
	 *
	 * @param f - 本地文件/文件夹
	 * @return 删除总个数
	 * @throws IOException throw exception when delete the file or directory error
	 */
	public static int delete(File f, long age, Consumer<String> func) throws IOException {

		int count = 0;

		Language lang = Language.getLanguage();

		if ((f.isFile() || isLink(f)) && (age < 0 || (Global.now() - f.lastModified() > age))) {

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
			if ((ff == null || ff.length == 0) && (age < 0 || (Global.now() - f.lastModified() > age))) {
//				log.warn("delete folder as empty: " + f.getCanonicalPath());

				f.delete();
			}

			count++;
		}
		return count;
	}

	public static int delete2(File f, long age, String name) throws IOException {

		int count = 0;

		Language lang = Language.getLanguage();

		if ((f.isFile() || isLink(f)) && (age < 0 || (Global.now() - f.lastModified() > age))) {

			log.warn("delete file: " + f.getCanonicalPath() + ", age="
					+ (lang == null ? -1 : lang.past(f.lastModified())) + ", time=" + f.lastModified());

			if (X.isEmpty(name) || f.getName().matches(name)) {
				f.delete();
				count++;
			}

		} else if (f.isDirectory()) {
			File[] ff = f.listFiles();
			if (ff != null && ff.length > 0) {
				for (File f1 : ff) {
					count += delete2(f1, age, name);
				}
			}
			ff = f.listFiles();
			if ((ff == null || ff.length == 0) && (age < 0 || (Global.now() - f.lastModified() > age))) {
//				log.warn("delete folder as empty: " + f.getCanonicalPath());

				f.delete();
			}

			count++;
		}
		return count;
	}

	/**
	 * 删除文件，老于age的
	 * 
	 * @param f   - 文件仓库文件
	 * @param age - 毫秒，最后修改时间早于age的文件
	 * @return 删除文件个数
	 * @throws Exception
	 */
	public static int delete(DFile f, long age) throws Exception {

		int count = 0;

		Language lang = Language.getLanguage();

		if (f.isFile() && (age < 0 || (Global.now() - f.lastModified() > age))) {

			GLog.applog.info("dfile", "delete",
					"delete file: " + f.getFilename() + ", age=" + (lang == null ? -1 : lang.past(f.lastModified())));

			log.warn("delete dfile: " + f.getFilename() + ", age=" + (lang == null ? -1 : lang.past(f.lastModified())));

			f.delete();

			count++;
		} else if (f.isDirectory()) {

			Collection<DFile> ff = Disk.list(f.getFilename());
			if (ff != null) {
				for (DFile f1 : ff) {
					count += delete(f1, age);
				}
			}

			ff = Disk.list(f.getFilename());
			if (ff == null || ff.isEmpty()) {

//				GLog.applog.info("dfile", "delete", "delete folder as empty: " + f.getFilename());

				log.warn("delete dfolder as empty: " + f.getFilename());

				f.delete();

			}

			count++;
		}
		return count;
	}

	/**
	 * 删除文件仓库文件/文件夹
	 * 
	 * @param f - 文件仓库文件/文件夹
	 * @return 删除文件总个数
	 * @throws IOException
	 */
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
				Collection<DFile> ff = Disk.list(f.getFilename());
				if (ff != null && !ff.isEmpty()) {
					for (DFile f1 : ff) {
						count += delete(f1);
						f1.delete();
					}
				}

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

	/**
	 * 复制文件夹
	 * 
	 * @param src  - 源本地文件夹
	 * @param dest - 目的文件位置
	 * @return 复制所有文件总长度
	 * @throws IOException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static int copyDir(File src, File dest) throws IOException {
		return copyDir(src, dest, (BiConsumer) null);
	}

	/**
	 * 复制本地文件夹到文件仓库
	 * 
	 * @param src  - 本地源文件夹
	 * @param dest - 文件仓库目的位置
	 * @return 复制所有文件的总长度
	 * @throws IOException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static int copyDir(File src, DFile dest) throws IOException {
		return copyDir(src, dest, (BiConsumer) null);
	}

	/**
	 * 复制文件夹
	 *
	 * @param src  - 源文件夹
	 * @param dest - 目的文件位置
	 * @return 复制所有文件的总长度
	 * @throws IOException throw exception when copy failed
	 */
	public static int copyDir(File src, File dest, BiConsumer<String, Integer> func) throws IOException {
		return _copyDir(src, dest, 0, func);
	}

	/**
	 * 复制文件夹
	 * 
	 * @param src  - 源文件仓库文件夹
	 * @param dest - 目的文件位置
	 * @param func - 回调函数 《文件名，长度》
	 * @return 复制所有文件的总长度
	 * @throws IOException
	 */
	public static int copyDir(File src, DFile dest, BiConsumer<String, Integer> func) throws IOException {
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

	private static int _copyDir(File src, DFile dest, int count, BiConsumer<String, Integer> func) throws IOException {

		dest.mkdirs();
		if (src.isFile()) {
			// copy file
			count++;
			if (func != null) {
				func.accept(src.getName(), count);
			}

			copy(new FileInputStream(src), Disk.seek(dest.getFilename() + "/" + src.getName()).getOutputStream());

		} else if (src.isDirectory()) {
			// copy dir
			File[] ff = src.listFiles();
			if (ff != null && ff.length > 0) {
				for (File f : ff) {
					if (f.isFile()) {
						count += _copyDir(f, dest, count, func);
					} else {
						count += _copyDir(f, Disk.seek(dest.getFilename() + "/" + f.getName()), count, func);
					}
				}
			}
		}
		return count;
	}

	/**
	 * 复制文件夹
	 * 
	 * @param src  - 源文件仓库文件夹
	 * @param dest - 目的文件位置
	 * @return 复制所有文件的总长度
	 * @throws IOException
	 */
	public static int copyDir(DFile src, DFile dest) throws IOException {
		return copyDir(src, dest, null);
	}

	/**
	 * 复制文件夹
	 * 
	 * @param src  - 源文件仓库文件/文件夹
	 * @param dest - 目的文件仓库位置
	 * @param func - 回调函数 《文件名，长度》
	 * @return 复制所有文件的总长度
	 * @throws IOException
	 */
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
			Collection<DFile> ff = Disk.list(src.getFilename());
			if (ff != null) {
				for (DFile f : ff) {
					if (f.isFile()) {
						count++;
						if (func != null) {
							func.accept(src.getName(), count);
						}
						copy(f, Disk.seek(dest.getFilename() + "/" + f.getName()));
					} else {
						count += copyDir(f, Disk.seek(dest.getFilename() + "/" + f.getName()), func);
					}
				}
			}
		}
		return count;
	}

	/**
	 * 复制文件夹
	 *
	 * @param src    - 源文件夹
	 * @param dest   - 目的文件夹
	 * @param except - 排除文件名
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
	 * 复制文件/文件夹到目的文件位置
	 *
	 * @param src  - 源文件/文件夹
	 * @param dest - 目的文件问之
	 * @return 复制所有文件的总长度
	 * @throws IOException throw exception when copy file failed
	 */
	public static long copy(File src, File dest) throws IOException {

		if (src.equals(dest)) {
			throw new IOException("复制目的地不能本身!");
		}

		if (src.isDirectory()) {
			return IOUtil.copyDir(src, dest);
		} else if (src.isFile()) {
			X.IO.mkdirs(dest.getParentFile());
			long n = copy(new FileInputStream(src), new FileOutputStream(dest), true);

			return n;
		}
		return 0;

	}

	/**
	 * 复制文件仓库文文件/文件夹到目的文件仓库
	 * 
	 * @param src  - 源文件仓库文件/文件夹
	 * @param dest - 目的文件仓库位置
	 * @return 复制的所有文件的总长度
	 * @throws IOException
	 */
	public static long copy(DFile src, DFile dest) throws IOException {

		if (src.equals(dest)) {
			throw new IOException("复制目的地不能本身!");
		}

		if (src.isDirectory()) {
			dest.mkdirs();
			return IOUtil.copyDir(src, dest);
		} else {
			return copy(src.getInputStream(), dest.getOutputStream(), true);
		}

	}

	/**
	 * 复制输入流到输出流
	 *
	 * @param in             - 输入流
	 * @param out            - 输出流
	 * @param start          - 输入流开始位置
	 * @param end            - 输入流结束位置
	 * @param closeAfterDone - True， 自动关闭输入输出流
	 * @return 复制的总长度
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static int copy(InputStream in, OutputStream out, long start, long end, boolean closeAfterDone)
			throws IOException {

		try {
			if (in == null || out == null)
				return 0;

			byte[] bb = new byte[BUFFER_SIZE];
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
	 * 复制输入流数据到输出流
	 *
	 * @param in             - 输入流
	 * @param out            - 输出流
	 * @param closeAfterDone - True，自动关闭输入输出流
	 * @return 复制的总长度
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static long copy(InputStream in, OutputStream out, boolean closeAfterDone) throws IOException {

		try {
			if (in == null || out == null)
				return 0;

			byte[] bb = new byte[BUFFER_SIZE];

			long total = 0;
			int len = in.read(bb);
			while (len > 0) {
				out.write(bb, 0, len);
				total += len;
				len = in.read(bb);
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
	 * 复制输入流数据到输出流，并关闭输入输出流
	 * 
	 * @param in   - 输入流
	 * @param out  - 输出流
	 * @param func - 回调函数，通过回调，《总长度，当前长度》
	 * @return 复制的长度
	 * @throws IOException
	 */
	public static long copy(InputStream in, OutputStream out, BiConsumer<Long, Long> func) throws IOException {

		try {
			if (in == null || out == null)
				return 0;

			byte[] bb = new byte[BUFFER_SIZE];

			long total = 0;
			int len = in.read(bb);
			while (len > 0) {
				if (func != null) {
					func.accept(total, (long) len);
				}
				out.write(bb, 0, len);
				total += len;
				len = in.read(bb);
			}
			out.flush();
			return total;

		} finally {
			X.close(in, out);
		}
	}

	/**
	 * 读取文件为字符串
	 * 
	 * @param f        - 文件对象
	 * @param encoding - 编码，缺省UTF-8
	 * @return 字符串
	 */
	public static String read(File f, String encoding) {

		FileInputStream in = null;

		try {
			if (X.isEmpty(encoding)) {
				encoding = X.UTF8;
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

	/**
	 * 读取所有内容为XML，并关闭输入流
	 * 
	 * @param in      - 输入流
	 * @param chatset - 编码
	 * @return XML的字符串
	 * @throws Exception
	 */
	public static String readxml(InputStream in, String chatset) throws Exception {

		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();

			Source src = new StreamSource(in);

			StringWriter writer = new StringWriter();

			transformer.transform(src, new StreamResult(writer));

			return writer.getBuffer().toString();

		} finally {
			X.close(in);
		}
	}

	/**
	 * 把字符串写入文件
	 * 
	 * @param f        - 文件对象
	 * @param encoding - 编码，缺省UTF8
	 * @param str      - 字符串
	 */
	public static void write(File f, String encoding, String str) {

		FileOutputStream out = null;

		try {
			if (X.isEmpty(encoding)) {
				encoding = X.UTF8;
			}
			out = new FileOutputStream(f);

			write(out, encoding, str);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			X.close(out);
		}
	}

	/**
	 * 写入字符串到输出流，并关闭输出流
	 * 
	 * @param out      - 输出流
	 * @param encoding - 编码，缺省UTF8
	 * @param str      - 字符串
	 */
	public static void write(OutputStream out, String encoding, String str) {

		BufferedWriter wri = null;

		try {
			if (X.isEmpty(encoding)) {
				encoding = X.UTF8;
			}
			wri = new BufferedWriter(new OutputStreamWriter(out, encoding));
			wri.write(str);
			wri.flush();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			X.close(wri);
		}

	}

	/**
	 * 保存对象到输出流，并关闭输出流
	 * 
	 * @param obj - 可序列化对象
	 * @param out - 输出流
	 * @throws Exception
	 */
	public static void saveObjectTo(Object obj, OutputStream out) throws Exception {

		try {

			byte[] bb = X.getBytes(obj);
			out.write(bb);

		} finally {
			X.close(out);
		}

	}

	/**
	 * 从输入流读取对象，并关闭输入流
	 * 
	 * @param in - 输入流
	 * @return 可反序列化对象
	 * @throws Exception
	 */
	public static Object readObjectFrom(InputStream in) throws Exception {

		try {

			byte[] bb = new byte[in.available()];
			in.read(bb);

			return X.fromBytes(bb);
		} finally {
			X.close(in);
		}
	}

	/**
	 * 读取所有数据，并关闭输入流
	 * 
	 * @param in       - 输入流
	 * @param encoding - 编码，缺省UTF-8
	 * @return
	 */
	public static String read(InputStream in, String encoding) {

		StringBuilder sb = new StringBuilder();

		BufferedReader read = null;

		try {
			if (X.isEmpty(encoding)) {
				encoding = X.UTF8;
			}
			read = new BufferedReader(new InputStreamReader(in, encoding));
			String line = null;
			while ((line = read.readLine()) != null) {
				sb.append(line).append("\n");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			X.close(in);
		}
		return sb.toString();

	}

	/**
	 * 读取所有数据，并关闭输入流
	 * 
	 * @param in - 输入流
	 * @return 字节流
	 */
	public static byte[] read(InputStream in) {
		return read(in, true);
	}

	/**
	 * 读取所有数据
	 * 
	 * @param in    - 输入流
	 * @param close - True 自动关闭输入流
	 * @return 字节流
	 */
	public static byte[] read(InputStream in, boolean close) {

		ByteArrayOutputStream out = null;
		try {

			out = new ByteArrayOutputStream();

			byte[] bb = new byte[16 * 1024];
			int len = in.read(bb);
			while (len > 0) {
				out.write(bb, 0, len);
				len = in.read(bb);
			}
			return out.toByteArray();

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			X.close(out);
			if (close) {
				X.close(in);
			}
		}
		return null;

	}

	/**
	 * 读取所有数据，并关闭输入流
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static String read(Reader in) throws IOException {

		if (in == null) {
			return null;
		}

		try {

			StringBuilder sb = new StringBuilder();
			char[] buff = new char[1024];
			int len;
			while ((len = in.read(buff)) != -1) {
				sb.append(buff, 0, len);
			}

			return sb.toString();
		} finally {
			X.close(in);
		}

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
	 * 按照CSV格式，读取一行 <br>
	 * 注意， 需要外层调用关闭输入流
	 * 
	 * @Deprecated replace by readcsv
	 * @param re
	 * @return
	 * @throws IOException
	 */
	@Deprecated
	public static String readcvs(BufferedReader re) throws IOException {
		return readcsv(re);
	}

	/**
	 * 按照CSV格式，读取一行<br>
	 * 注意， 需要外层调用关闭输入流
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

		File parent = f.getParentFile();
		if (!parent.exists()) {
			mkdirs(parent);
		}

		boolean b = f.mkdir();

		try {
			Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
			perms.add(PosixFilePermission.OWNER_READ);
			perms.add(PosixFilePermission.OWNER_WRITE);
			perms.add(PosixFilePermission.OWNER_EXECUTE);
			Files.setPosixFilePermissions(Paths.get(f.getAbsolutePath()), perms);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return b;

	}

	public static InputStream copy(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		copy(in, out);
		return new ByteArrayInputStream(out.toByteArray());
	}

}
