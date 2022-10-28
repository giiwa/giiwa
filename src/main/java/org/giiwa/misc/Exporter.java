package org.giiwa.misc;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.dao.X;
import org.giiwa.task.Function;

public class Exporter<V> implements Closeable {

	static Log log = LogFactory.getLog(Exporter.class);

	public enum FORMAT {
		csv, plain
	}

	public BufferedWriter out = null;
	FORMAT format;

	Function<V, Object[]> cols = null;

	File file;

	public File getFile() {
		return file;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <V> Exporter<V> create(File file, FORMAT format) throws IOException {

		Exporter ex = null;
		if (file.exists()) {
			ex = create(new FileOutputStream(file, true), format, false);
		} else {
			X.IO.mkdirs(file.getParentFile());
			ex = create(new FileOutputStream(file), format, true);
		}
		ex.file = file;
		return ex;

	}

	public static <V> Exporter<V> create(OutputStream out, FORMAT format, boolean init) throws IOException {

		if (init && FORMAT.csv == format) {
			// BOM, UTF-8
			out.write(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF });
		}

		Exporter<V> s = new Exporter<V>();
		s.out = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
		s.format = format;

		return s;
	}

	public Exporter<V> createSheet(Function<V, Object[]> cols) {
		this.cols = cols;
		return this;
	}

	public void flush() {
		try {
			if (out != null)
				out.flush();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public void close() {
		X.close(out);
		out = null;
	}

	public void print(String... cols) throws IOException {
		print((Object[]) cols);
	}

	public void print(Object... cols) throws IOException {
		if (out == null) {
			throw new IOException("out is null?");
		}

		for (int i = 0; i < cols.length; i++) {
			Object s = cols[i];
			if (i > 0) {
				out.write(",");
			}
			if (X.isEmpty(s))
				continue;

			if (format.equals(FORMAT.csv)) {
				if (s instanceof Number) {
					out.write(s.toString());
				} else {
					out.write("\"");
					s = s.toString().replaceAll("\"", "\"\"");
					out.write(s.toString());
					out.write("\"");
				}
			} else {
				out.write(s.toString());
			}
		}
		out.write("\r\n");
		out.flush();

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

		Object[] ss = cols.apply(b);
		if (ss != null) {
			print(ss);
		}

	}

}
