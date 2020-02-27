package org.giiwa.dfile.command;

import java.io.File;

import org.giiwa.dfile.ICommand;
import org.giiwa.misc.IOUtil;
import org.giiwa.net.nio.IoRequest;
import org.giiwa.net.nio.IoResponse;

public class DELETE implements ICommand {

	@Override
	public void process(long seq, IoRequest req, IoResponse resp) {

		String path = new String(req.readBytes(req.readInt())).replaceAll("[/\\\\]", "/");
		String filename = new String(req.readBytes(req.readInt())).replaceAll("[/\\\\]", "/");
		long age = req.readLong();

		File f = new File(path + File.separator + filename);

		try {
			IOUtil.delete(f, age);

			resp.write((byte) 1);

		} catch (Exception e) {
			resp.write((byte) 0);
		}

		resp.send(resp.size() + 8, seq);

	}

}
