package org.giiwa.core.dfile.command;

import java.io.File;
import org.giiwa.core.dfile.ICommand;
import org.giiwa.core.dfile.IResponseHandler;
import org.giiwa.core.dfile.Request;
import org.giiwa.core.dfile.Response;

public class MOVE implements ICommand {

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
		String path2 = in.readString();
		try {
			path2 = path2.replaceAll("[/\\\\]", "/");
		} catch (Exception e) {
			log.error(path2, e);
		}
		String filename2 = in.readString();
		try {
			filename2 = filename2.replaceAll("[/\\\\]", "/");
		} catch (Exception e) {
			log.error(filename2, e);
		}

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
