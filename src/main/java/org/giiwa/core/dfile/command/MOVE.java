package org.giiwa.core.dfile.command;

import java.io.File;
import org.giiwa.core.dfile.ICommand;
import org.giiwa.core.nio.IResponseHandler;
import org.giiwa.core.nio.Request;
import org.giiwa.core.nio.Response;

public class MOVE implements ICommand {

	@Override
	public void process(Request in, IResponseHandler handler) {
		String path = in.readString();
		String filename = in.readString();
		String path2 = in.readString();
		String filename2 = in.readString();

		File f1 = new File(path, filename);
		File f2 = new File(path2, filename2);

		Response out = Response.create(in.seq);
		if (f1.renameTo(f2)) {
			out.writeByte((byte) 1);
		} else {
			out.writeByte((byte) 0);
		}
		handler.send(out);

	}

}
