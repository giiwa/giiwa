package org.giiwa.core.base;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.X;

public class Exporter<V> {

	static Log log = LogFactory.getLog(Exporter.class);

	public enum FORMAT {
		csv, plain
	}

	BufferedWriter out = null;
	FORMAT format;

	Function<V, Object[]> cols = null;

	public static <V> Exporter<V> create(File file, String charset, FORMAT format) {
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

	public static <V> Exporter<V> create(OutputStream out, String charset, FORMAT format) {
		try {
			Exporter<V> s = new Exporter<V>();
			s.out = new BufferedWriter(new OutputStreamWriter(out, charset));
			s.format = format;

			return s;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
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

		if (log.isDebugEnabled())
			log.debug("print, cols=" + Arrays.toString(cols) + ", out=" + out);

		for (int i = 0; i < cols.length; i++) {
			Object s = cols[i];
			if (i > 0) {
				out.write(",");
			}
			if (format.equals(FORMAT.csv))
				out.write("\"");
			if (s != null) {
				if (format.equals(FORMAT.csv))
					s = s.toString().replaceAll("\"", "\\\"").replaceAll("\r\n", "");
				out.write(s.toString());
			}
			if (format.equals(FORMAT.csv))
				out.write("\"");
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
