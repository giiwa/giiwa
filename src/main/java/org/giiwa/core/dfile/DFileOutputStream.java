package org.giiwa.core.dfile;

import java.io.IOException;
import java.io.OutputStream;

import org.giiwa.framework.bean.Disk;

public class DFileOutputStream extends OutputStream {

	String ip;
	int port;
	String path;
	String filename;
	Disk disk;

	byte[] bb = new byte[FileServer.BUFFER_SIZE];
	int pos = 0;
	long offset = 0;

	public static DFileOutputStream create(Disk disk, String filename) {
		DFileOutputStream d = new DFileOutputStream();
		d.disk = disk;
		d.ip = disk.getNode_obj().getIp();
		d.port = disk.getNode_obj().getPort();
		d.path = disk.getPath();
		d.filename = filename;
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
	public void flush() throws IOException {
		try {
			offset = FileClient.get(ip, port).put(path, filename, offset, bb, pos);
			pos = 0;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public void close() throws IOException {
		flush();
		super.close();
	}

}
