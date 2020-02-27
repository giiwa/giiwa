package org.giiwa.dfile.command;

import java.io.File;
import java.io.FileInputStream;

import org.giiwa.dao.X;
import org.giiwa.dfile.ICommand;
import org.giiwa.net.nio.IoRequest;
import org.giiwa.net.nio.IoResponse;

public class GET implements ICommand {

	@Override
	public void process(long seq, IoRequest req, IoResponse resp) {

		String path = new String(req.readBytes(req.readInt())).replaceAll("[/\\\\]", "/");
		String filename = new String(req.readBytes(req.readInt())).replaceAll("[/\\\\]", "/");

		long offset = req.readLong();
		int len = req.readInt();

		if (log.isDebugEnabled())
			log.debug("get, file=" + filename + ", offset=" + offset + ", len=" + len);

		File f = new File(path + File.separator + filename);

		FileInputStream f1 = null;
		try {
			f1 = new FileInputStream(f);
			f1.skip(offset);

			byte[] bb = new byte[Math.min(len, f1.available())];
			f1.read(bb);

			resp.write((int) bb.length);
			resp.write(bb);

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			resp.write((int) 0);
		} finally {
			X.close(f1);
		}

		resp.send(resp.size() + 8, seq);

	}

}
