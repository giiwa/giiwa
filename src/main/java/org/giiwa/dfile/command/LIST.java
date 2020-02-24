package org.giiwa.dfile.command;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;

import org.giiwa.dfile.ICommand;
import org.giiwa.dfile.IResponseHandler;
import org.giiwa.dfile.Request;
import org.giiwa.dfile.Response;

public class LIST implements ICommand {

	@Override
	public void process(Request in, IResponseHandler handler) {

		String path = in.readString().replaceAll("[/\\\\]", "/");
		String filename = in.readString().replaceAll("[/\\\\]", "/");

		if (log.isDebugEnabled())
			log.debug("list, file=" + filename + ", path=" + path);

		File f = new File(path + File.separator + filename);
		// List<JSON> l1 = JSON.createList();

		Response out = Response.create(in.seq, Request.MID);

		File[] ff = f.listFiles();
		if (ff != null) {
			for (File f1 : ff) {
				out.writeString(f1.getName());
				out.writeInt(f1.exists() ? 1 : 0);
				out.writeInt(f1.isFile() ? 1 : 0);
				out.writeLong(f1.length());
				out.writeLong(f1.lastModified());

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
		handler.send(out);

	}

}
