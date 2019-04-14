package org.giiwa.core.dfile.command;

import java.io.File;

import org.giiwa.core.dfile.ICommand;
import org.giiwa.core.dfile.IResponseHandler;
import org.giiwa.core.dfile.Request;
import org.giiwa.core.dfile.Response;

public class MKDIRS implements ICommand {

	@Override
	public void process(Request in, IResponseHandler handler) {

		String path = in.readString();
		try {
			path = path.replaceAll("[/\\\\]", "/");
		} catch (Exception e) {
			log.error(path, e);
		}

		String filename = in.readString();
		try {
			filename = filename.replaceAll("[/\\\\]", "/");
		} catch (Exception e) {
			log.error(filename, e);
		}

		Response out = Response.create(in.seq, Request.SMALL);

		File f = new File(path + File.separator + filename);
		if (!f.exists()) {
			if (f.mkdirs()) {
				out.writeByte((byte) 1);
			} else {
				out.writeByte((byte) 0);
			}
		} else {
			out.writeByte((byte) 1);
		}
		handler.send(out);

	}

}
