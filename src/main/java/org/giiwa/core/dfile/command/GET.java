package org.giiwa.core.dfile.command;

import java.io.File;
import java.io.FileInputStream;

import org.giiwa.core.bean.X;
import org.giiwa.core.dfile.ICommand;
import org.giiwa.core.nio.IResponseHandler;
import org.giiwa.core.nio.Request;
import org.giiwa.core.nio.Response;

public class GET implements ICommand {

	@Override
	public void process(Request in, IResponseHandler handler) {

		String path = in.readString();
		String filename = in.readString();
		long offset = in.readLong();
		int len = in.readInt();

		File f = new File(path + "/" + filename);
		Response out = Response.create(in.seq);

		FileInputStream f1 = null;
		try {
			f1 = new FileInputStream(f);
			f1.skip(offset);

			byte[] bb = new byte[Math.min(len, f1.available())];
			f1.read(bb);

			out.writeBytes(bb);

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			out.writeInt(0);
		} finally {
			X.close(f1);
		}
		handler.send(out);

	}

}
