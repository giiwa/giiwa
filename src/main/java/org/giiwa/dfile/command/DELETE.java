package org.giiwa.dfile.command;

import java.io.File;

import org.giiwa.dfile.ICommand;
import org.giiwa.dfile.IResponseHandler;
import org.giiwa.dfile.Request;
import org.giiwa.dfile.Response;
import org.giiwa.misc.IOUtil;

public class DELETE implements ICommand {

	@Override
	public void process(Request in, IResponseHandler handler) {

		String path = in.readString().replaceAll("[/\\\\]", "/");
		String filename = in.readString().replaceAll("[/\\\\]", "/");
		long age = in.readLong();

		File f = new File(path + File.separator + filename);

		Response out = Response.create(in.seq, Request.SMALL);

		try {
			IOUtil.delete(f, age);
			out.writeByte((byte) 1);
		} catch (Exception e) {
			out.writeByte((byte) 0);
		}
		handler.send(out);

	}

}
