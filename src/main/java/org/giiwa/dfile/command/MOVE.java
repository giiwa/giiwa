package org.giiwa.dfile.command;

import java.io.File;

import org.giiwa.dfile.ICommand;
import org.giiwa.net.nio.IoRequest;
import org.giiwa.net.nio.IoResponse;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class MOVE implements ICommand {

	@Override
	public void process(long seq, IoRequest req, IoResponse resp) {
		String path = new String(req.readBytes(req.readInt())).replaceAll("[/\\\\]", "/");
		String filename = new String(req.readBytes(req.readInt())).replaceAll("[/\\\\]", "/");

		String path2 = new String(req.readBytes(req.readInt())).replaceAll("[/\\\\]", "/");
		String filename2 = new String(req.readBytes(req.readInt())).replaceAll("[/\\\\]", "/");

		if (log.isDebugEnabled())
			log.debug("move, file1=" + filename + ", path1=" + path + ", file2=" + filename2 + ", path2=" + path2);

		File f1 = new File(path, filename);
		File f2 = new File(path2, filename2);

		if (f1.renameTo(f2)) {
			resp.write((byte) 1);
		} else {
			resp.write((byte) 0);
		}

		resp.send(e -> {
			ByteBuf b = Unpooled.buffer();
			b.writeInt(e.readableBytes() + 8);
			b.writeLong(seq);
			b.writeBytes(e);
			return b;
		});

	}

}
