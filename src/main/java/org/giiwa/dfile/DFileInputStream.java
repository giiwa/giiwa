package org.giiwa.dfile;

import java.io.IOException;
import java.io.InputStream;

import org.giiwa.bean.Disk;
import org.giiwa.net.nio.IoRequest;

 class DFileInputStream extends InputStream {

	// private static Log log = LogFactory.getLog(DFileInputStream.class);

	String url;
	String path;
	String filename;
	Disk disk;

	byte[] bb;
	int pos = 0;
	long offset = 0;
	boolean last = false;

	public static DFileInputStream create(Disk disk, String filename) {
		DFileInputStream d = new DFileInputStream();
		d.disk = disk;
		d.url = disk.getNode_obj().getUrl();
		d.path = disk.getPath();
		d.filename = filename;
		return d;
	}

	@Override
	public int read() throws IOException {
		prepare();

		if (EOF())
			return -1;

		return ((int) bb[pos++]) & 0xFF;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {

		if (b == null) {
			throw new NullPointerException();
		} else if (off < 0 || len < 0 || len > b.length - off) {
			throw new IndexOutOfBoundsException();
		} else if (len == 0) {
			return 0;
		}

		prepare();

		if (EOF())
			return -1;

		int n = 0;
		while (n < len) {
			prepare();

			if (EOF())
				break;

			int n1 = Math.min(len - n, bb.length - pos);
			System.arraycopy(bb, pos, b, n + off, n1);
			n += n1;
			pos += n1;
		}
		return n;

	}

	@Override
	public long skip(long n) throws IOException {

		if (n <= 0)
			return 0;

		if (bb == null) {
			offset = n;
			return n;
		}

		if (bb.length - pos >= n) {
			pos += n;
			return n;
		}

		offset += n - (bb.length - pos);
		pos = bb.length;
		return n;
	}

	private boolean EOF() throws IOException {
		if (bb == null || pos >= bb.length) {
			if (last) {
				return true;
			} else {
				throw new IOException("unknown error");
			}
		}
		return false;
	}

	private void prepare() throws IOException {
		if (last)
			return;

		if (bb == null || pos >= bb.length) {
			bb = FileClient.get(url, path).get(filename, offset, IoRequest.BIG);
			if (bb == null || bb.length < IoRequest.BIG) {
				// log.debug("last packet, bb=" + bb.length);
				last = true;
			}
			offset += (bb != null) ? bb.length : 0;
			pos = 0;
		}
	}

}
