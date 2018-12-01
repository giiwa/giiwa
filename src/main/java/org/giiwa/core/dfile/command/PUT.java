package org.giiwa.core.dfile.command;

import java.io.File;
import java.io.RandomAccessFile;

import org.giiwa.core.bean.X;
import org.giiwa.core.dfile.ICommand;
import org.giiwa.core.nio.IResponseHandler;
import org.giiwa.core.nio.Request;
import org.giiwa.core.nio.Response;

public class PUT implements ICommand {

	@Override
	public void process(Request in, IResponseHandler handler) {

		String path = in.readString();
		String filename = in.readString();
		File f = new File(path + "/" + filename);
		long offset = in.readLong();
		byte[] bb = in.readBytes();

		RandomAccessFile a = null;

		Response out = Response.create(in.seq);

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
