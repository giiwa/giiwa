package org.giiwa.core.dfile.command;

import java.io.File;

import org.giiwa.core.base.IOUtil;
import org.giiwa.core.dfile.ICommand;
import org.giiwa.core.nio.IResponseHandler;
import org.giiwa.core.nio.Request;
import org.giiwa.core.nio.Response;

public class DELETE implements ICommand {

	@Override
	public void process(Request in, IResponseHandler handler) {

		String path = in.readString();
		String filename = in.readString();
		long age = in.readLong();

		File f = new File(path + "/" + filename);

		Response out = Response.create(in.seq);

		try {
			IOUtil.delete(f, age);
			out.writeByte((byte) 1);
		} catch (Exception e) {
			out.writeByte((byte) 0);
		}
		handler.send(out);

	}

}
