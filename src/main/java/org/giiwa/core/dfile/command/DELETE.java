package org.giiwa.core.dfile.command;

import java.io.File;

import org.giiwa.core.base.IOUtil;
import org.giiwa.core.dfile.ICommand;
import org.giiwa.core.dfile.IResponseHandler;
import org.giiwa.core.dfile.Request;
import org.giiwa.core.dfile.Response;

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
