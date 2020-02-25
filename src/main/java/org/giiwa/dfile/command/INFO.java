package org.giiwa.dfile.command;

import java.io.File;

import org.giiwa.dfile.ICommand;
import org.giiwa.net.nio.IoRequest;
import org.giiwa.net.nio.IoResponse;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class INFO implements ICommand {

	@Override
	public void process(long seq, IoRequest in, IoResponse out) {

		String path = new String(in.readBytes(in.readInt())).replaceAll("[/\\\\]", "/");
		String filename = new String(in.readBytes(in.readInt())).replaceAll("[/\\\\]", "/");

		if (log.isDebugEnabled())
			log.debug("info, file=" + filename + ", path=" + path);

		File f = new File(path + File.separator + filename);

		// JSON jo = JSON.create();
		// jo.append("e", f.exists() ? 1 : 0);
		// jo.append("f", f.isFile() ? 1 : 0);
		// jo.append("l", f.length());
		// jo.append("u", f.lastModified());

		out.write((int) (f.exists() ? 1 : 0));
		out.write((int) (f.isFile() ? 1 : 0));
		out.write((long) f.length());
		out.write((long) f.lastModified());

//		try {
//			Path p1 = Paths.get(f.getAbsolutePath());
//			BasicFileAttributeView basicview = Files.getFileAttributeView(p1, BasicFileAttributeView.class,
//					LinkOption.NOFOLLOW_LINKS);
//			BasicFileAttributes attr = basicview.readAttributes();
//			out.writeLong(attr.creationTime().toMillis());
//		} catch (Exception e) {
//			log.error(e.getMessage(), e);
//			out.writeLong(-1);
//		}
		// out.writeString(jo.toString());

		out.send(e -> {
			ByteBuf b = Unpooled.buffer();
			b.writeInt(e.readableBytes() + 8);
			b.writeLong(seq);
			b.writeBytes(e);
			return b;
		});

	}

}
