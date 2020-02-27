package org.giiwa.dfile.command;

import java.io.File;

import org.giiwa.dfile.ICommand;
import org.giiwa.net.nio.IoRequest;
import org.giiwa.net.nio.IoResponse;

public class LIST implements ICommand {

	@Override
	public void process(long seq, IoRequest req, IoResponse out) {

		String path = new String(req.readBytes(req.readInt())).replaceAll("[/\\\\]", "/");
		String filename = new String(req.readBytes(req.readInt())).replaceAll("[/\\\\]", "/");

		if (log.isDebugEnabled())
			log.debug("list, file=" + filename);

		File f = new File(path + File.separator + filename);

		File[] ff = f.listFiles();
		if (ff != null) {
			for (File f1 : ff) {
				byte[] b = f1.getName().getBytes();
				out.write(b.length);
				out.write(b);
				out.write(f1.exists() ? 1 : 0);
				out.write(f1.isFile() ? 1 : 0);
				out.write(f1.length());
				out.write(f1.lastModified());

			}
		}

		out.send(out.size() + 8, seq);

	}

}
