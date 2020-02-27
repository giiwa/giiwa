package org.giiwa.dfile.command;

import java.io.File;

import org.giiwa.dfile.ICommand;
import org.giiwa.net.nio.IoRequest;
import org.giiwa.net.nio.IoResponse;

public class INFO implements ICommand {

	@Override
	public void process(long seq, IoRequest in, IoResponse out) {

		String path = new String(in.readBytes(in.readInt())).replaceAll("[/\\\\]", "/");
		String filename = new String(in.readBytes(in.readInt())).replaceAll("[/\\\\]", "/");

		if (log.isDebugEnabled())
			log.debug("info, file=" + filename);

		File f = new File(path + File.separator + filename);

		out.write((int) (f.exists() ? 1 : 0));
		out.write((int) (f.isFile() ? 1 : 0));
		out.write((long) f.length());
		out.write((long) f.lastModified());

		out.send(out.size() + 8, seq);

	}

}
