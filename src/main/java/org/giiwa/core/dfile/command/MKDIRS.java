package org.giiwa.core.dfile.command;

import java.io.File;

import org.giiwa.core.dfile.ICommand;
import org.giiwa.core.nio.IResponseHandler;
import org.giiwa.core.nio.Request;
import org.giiwa.core.nio.Response;

public class MKDIRS implements ICommand {

	@Override
	public void process(Request in, IResponseHandler handler) {

		String path = in.readString();
		String filename = in.readString();

		Response out = Response.create(in.seq);

		File f = new File(path + "/" + filename);
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
