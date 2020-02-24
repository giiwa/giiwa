package org.giiwa.dfile.command;

import java.io.File;
import java.io.FileInputStream;

import org.giiwa.dao.X;
import org.giiwa.dfile.ICommand;
import org.giiwa.dfile.IResponseHandler;
import org.giiwa.dfile.Request;
import org.giiwa.dfile.Response;

public class GET implements ICommand {

	@Override
	public void process(Request in, IResponseHandler handler) {

		String path = in.readString().replaceAll("[/\\\\]", "/");
		String filename = in.readString().replaceAll("[/\\\\]", "/");

		long offset = in.readLong();
		int len = in.readInt();

		if (log.isDebugEnabled())
			log.debug("get, file=" + filename + ", offset=" + offset + ", len=" + len + ", path=" + path);

		File f = new File(path + File.separator + filename);
		Response out = Response.create(in.seq, Request.BIG);

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
