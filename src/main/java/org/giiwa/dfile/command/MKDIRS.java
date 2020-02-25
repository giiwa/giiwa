package org.giiwa.dfile.command;

import java.io.File;

import org.apache.mina.core.buffer.IoBuffer;
import org.giiwa.dfile.ICommand;
import org.giiwa.net.nio.IoRequest;
import org.giiwa.net.nio.IoResponse;

public class MKDIRS implements ICommand {

	@Override
	public void process(long seq, IoRequest req, IoResponse out) {

		String path = new String(req.readBytes(req.readInt())).replaceAll("[/\\\\]", "/");
		String filename = new String(req.readBytes(req.readInt())).replaceAll("[/\\\\]", "/");

		File f = new File(path + File.separator + filename);
		if (!f.exists()) {
			if (f.mkdirs()) {
				out.write((byte) 1);
			} else {
				out.write((byte) 0);
			}
		} else {
			out.write((byte) 1);
		}

		out.send(e -> {
			IoBuffer b = IoBuffer.allocate(1024);
			b.setAutoExpand(true);
			b.putInt(e.remaining() + 8);
			b.putLong(seq);
			b.put(e);
			return b;
		});

	}

}
