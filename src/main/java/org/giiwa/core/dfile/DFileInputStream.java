package org.giiwa.core.dfile;

import java.io.IOException;
import java.io.InputStream;

import org.giiwa.framework.bean.Disk;

public class DFileInputStream extends InputStream {

//	private static Log log = LogFactory.getLog(DFileInputStream.class);

	String ip;
	int port;
	String path;
	String filename;
	Disk disk;

	byte[] bb;
	int pos = 0;
	long offset = 0;
	int last = 0;

	public static DFileInputStream create(Disk disk, String filename) {
		DFileInputStream d = new DFileInputStream();
		d.disk = disk;
		d.ip = disk.getNode_obj().getIp();
		d.port = disk.getNode_obj().getPort();
		d.path = disk.getPath();
		d.filename = filename;
		return d;
	}

	@Override
	public int read() throws IOException {
		prepare();

		if (bb == null || pos >= bb.length) {
			if (last == 1) {
				return -1;
			} else {
				throw new IOException("unknown error");
			}
		}

		return ((int) bb[pos++]) & 0xFF;
	}

	private void prepare() throws IOException {
		try {
			if (last == 1)
				return;

			if (offset == 0 || pos >= bb.length) {
				bb = FileClient.get(ip, port).get(path, filename, offset, FileServer.BUFFER_SIZE);
				if (bb == null || bb.length < FileServer.BUFFER_SIZE) {
					// log.debug("last packet, bb=" + bb.length);
					last = 1;
				}
				offset += bb != null ? bb.length : 0;
				pos = 0;
			}
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

}
