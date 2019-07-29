package org.giiwa.core.dfile.command;

import java.io.File;
import java.io.RandomAccessFile;

import org.giiwa.core.bean.X;
import org.giiwa.core.dfile.ICommand;
import org.giiwa.core.dfile.IResponseHandler;
import org.giiwa.core.dfile.Request;
import org.giiwa.core.dfile.Response;

public class PUT implements ICommand {

	@Override
	public void process(Request in, IResponseHandler handler) {

		String path = in.readString().replaceAll("[/\\\\]", "/");
		String filename = in.readString().replaceAll("[/\\\\]", "/");

		long offset = in.readLong();
		byte[] bb = in.readBytes();

		log.debug("put, file=" + filename + ", offset=" + offset + ", len=" + bb.length + ", path=" + path);

		File f = new File(path + File.separator + filename);

		RandomAccessFile a = null;

		Response out = Response.create(in.seq, Request.SMALL);

		try {
			if (bb != null) {
				if (!f.exists()) {
					f.getParentFile().mkdirs();
					f.createNewFile();
				}
				a = new RandomAccessFile(f, "rws");
				a.seek(offset);
				a.write(bb);
			}

			out.writeLong(offset + (bb == null ? 0 : bb.length));

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			out.writeLong(-1);
		} finally {
			X.close(a);
		}
		handler.send(out);

	}

}
