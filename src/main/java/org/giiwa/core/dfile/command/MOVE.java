package org.giiwa.core.dfile.command;

import java.io.File;
import org.giiwa.core.dfile.ICommand;
import org.giiwa.core.dfile.IResponseHandler;
import org.giiwa.core.dfile.Request;
import org.giiwa.core.dfile.Response;

public class MOVE implements ICommand {

	@Override
	public void process(Request in, IResponseHandler handler) {
		String path = in.readString().replaceAll("[/\\\\]", "/");
		String filename = in.readString().replaceAll("[/\\\\]", "/");

		String path2 = in.readString().replaceAll("[/\\\\]", "/");
		String filename2 = in.readString().replaceAll("[/\\\\]", "/");

		if (log.isDebugEnabled())
			log.debug("move, file1=" + filename + ", path1=" + path + ", file2=" + filename2 + ", path2=" + path2);

		File f1 = new File(path, filename);
		File f2 = new File(path2, filename2);

		Response out = Response.create(in.seq, Request.SMALL);
		if (f1.renameTo(f2)) {
			out.writeByte((byte) 1);
		} else {
			out.writeByte((byte) 0);
		}
		handler.send(out);

	}

}
