package org.giiwa.core.dfile.command;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;

import org.giiwa.core.dfile.ICommand;
import org.giiwa.core.dfile.IResponseHandler;
import org.giiwa.core.dfile.Request;
import org.giiwa.core.dfile.Response;

public class INFO implements ICommand {

	@Override
	public void process(Request in, IResponseHandler handler) {

		String path = in.readString().replaceAll("[/\\\\]", "/");
		String filename = in.readString().replaceAll("[/\\\\]", "/");

		if (log.isDebugEnabled())
			log.debug("info, file=" + filename + ", path=" + path);

		File f = new File(path + File.separator + filename);

		// JSON jo = JSON.create();
		// jo.append("e", f.exists() ? 1 : 0);
		// jo.append("f", f.isFile() ? 1 : 0);
		// jo.append("l", f.length());
		// jo.append("u", f.lastModified());

		Response out = Response.create(in.seq, Request.SMALL);
		out.writeInt(f.exists() ? 1 : 0);
		out.writeInt(f.isFile() ? 1 : 0);
		out.writeLong(f.length());
		out.writeLong(f.lastModified());

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
		handler.send(out);

	}

}
