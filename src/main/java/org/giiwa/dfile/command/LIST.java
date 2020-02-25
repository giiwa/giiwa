package org.giiwa.dfile.command;

import java.io.File;

import org.apache.mina.core.buffer.IoBuffer;
import org.giiwa.dfile.ICommand;
import org.giiwa.net.nio.IoRequest;
import org.giiwa.net.nio.IoResponse;

public class LIST implements ICommand {

	@Override
	public void process(long seq, IoRequest req, IoResponse out) {

		String path = new String(req.readBytes(req.readInt())).replaceAll("[/\\\\]", "/");
		String filename = new String(req.readBytes(req.readInt())).replaceAll("[/\\\\]", "/");

		if (log.isDebugEnabled())
			log.debug("list, file=" + filename + ", path=" + path);

		File f = new File(path + File.separator + filename);
		// List<JSON> l1 = JSON.createList();

		File[] ff = f.listFiles();
		if (ff != null) {
			for (File f1 : ff) {
				out.write((short) f1.getName().getBytes().length).write(f1.getName().getBytes());
				out.write(f1.exists() ? 1 : 0);
				out.write(f1.isFile() ? 1 : 0);
				out.write(f1.length());
				out.write(f1.lastModified());

//				try {
//					Path p1 = Paths.get(f1.getAbsolutePath());
//					BasicFileAttributeView basicview = Files.getFileAttributeView(p1, BasicFileAttributeView.class,
//							LinkOption.NOFOLLOW_LINKS);
//					BasicFileAttributes attr = basicview.readAttributes();
//					out.writeLong(attr.creationTime().toMillis());
//				} catch (Exception e) {
//					log.error(e.getMessage(), e);
//					out.writeLong(-1);
//				}

				// JSON jo = JSON.create();
				// jo.append("name", f1.getName());
				// jo.append("e", f1.exists() ? 1 : 0);
				// jo.append("f", f1.isFile() ? 1 : 0);
				// jo.append("l", f1.length());
				// jo.append("u", f1.lastModified());
				// l1.add(jo);
			}
		}
		// JSON jo = JSON.create().append("list", l1);

		// out.writeString(jo.toString());
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
