package org.giiwa.dfile.command;

import java.io.File;
import java.io.RandomAccessFile;

import org.apache.mina.core.buffer.IoBuffer;
import org.giiwa.dao.X;
import org.giiwa.dfile.ICommand;
import org.giiwa.net.nio.IoRequest;
import org.giiwa.net.nio.IoResponse;

public class PUT implements ICommand {

	@Override
	public void process(long seq, IoRequest req, IoResponse resp) {

		String path = new String(req.readBytes(req.readInt())).replaceAll("[/\\\\]", "/");
		String filename = new String(req.readBytes(req.readInt())).replaceAll("[/\\\\]", "/");

		long offset = req.readLong();
		byte[] bb = req.readBytes(req.readInt());

		if (log.isDebugEnabled())
			log.debug("put, file=" + filename + ", offset=" + offset + ", len=" + bb.length + ", path=" + path);

		File f = new File(path + File.separator + filename);

		RandomAccessFile a = null;

		try {
			if (bb != null) {
				if (!f.exists()) {
					f.getParentFile().mkdirs();
					f.createNewFile();
				}
				a = new RandomAccessFile(f, "rws");
				a.seek(offset);
				a.write(bb);
			}

			resp.write((long) (offset + (bb == null ? 0 : bb.length)));

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			resp.write(-1L);
		} finally {
			X.close(a);
		}

		resp.send(e -> {
			IoBuffer b = IoBuffer.allocate(1024);
			b.setAutoExpand(true);
			b.putInt(e.remaining() + 8);
			b.putLong(seq);
			b.put(e);
			return b;
		});

	}

}
