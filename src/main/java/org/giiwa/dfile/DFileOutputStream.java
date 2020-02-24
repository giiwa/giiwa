package org.giiwa.dfile;

import java.io.IOException;
import java.io.OutputStream;

import org.giiwa.dao.bean.Disk;

public class DFileOutputStream extends OutputStream {

	String url;
	String path;
	String filename;
	Disk disk;

	byte[] bb = new byte[Request.BIG];
	int pos = 0;
	long offset = 0;

	public static DFileOutputStream create(Disk disk, String filename, long offset) {
		DFileOutputStream d = new DFileOutputStream();
		d.disk = disk;
		d.filename = filename;
		d.offset = offset;
		d.url = disk.getNode_obj().getUrl();
		d.path = disk.getPath();
		return d;
	}

	@Override
	public void write(int b) throws IOException {
		if (pos >= bb.length) {
			flush();
		}
		bb[pos++] = (byte) b;
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {

		if (b == null) {
			throw new NullPointerException();
		} else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
			throw new IndexOutOfBoundsException();
		} else if (len == 0) {
			return;
		}

		int n = 0;
		while (n < len) {
			int n1 = Math.min(len - n, bb.length - pos);
			System.arraycopy(b, off + n, bb, pos, n1);
			n += n1;
			pos += n1;
			flush();
		}
	}

	@Override
	public void flush() throws IOException {
		if (pos > 0) {
			offset = FileClient.get(url).put(path, filename, offset, bb, pos);
			pos = 0;
		}
	}

	@Override
	public void close() throws IOException {
		flush();
		super.close();
	}

}
