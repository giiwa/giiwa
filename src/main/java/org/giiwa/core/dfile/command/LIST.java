package org.giiwa.core.dfile.command;

import java.io.File;

import org.giiwa.core.dfile.ICommand;
import org.giiwa.core.dfile.IResponseHandler;
import org.giiwa.core.dfile.Request;
import org.giiwa.core.dfile.Response;

public class LIST implements ICommand {

	@Override
	public void process(Request in, IResponseHandler handler) {

		String path = in.readString().replaceAll("[/\\\\]", "/");
		String filename = in.readString().replaceAll("[/\\\\]", "/");

		File f = new File(path + File.separator + filename);
		// List<JSON> l1 = JSON.createList();

		Response out = Response.create(in.seq, Request.MID);

		File[] ff = f.listFiles();
		if (ff != null) {
			for (File f1 : ff) {
				out.writeString(f1.getName());
				out.writeInt(f1.exists() ? 1 : 0);
				out.writeInt(f1.isFile() ? 1 : 0);
				out.writeLong(f1.length());
				out.writeLong(f1.lastModified());

				// JSON jo = JSON.create();
				// jo.append("name", f1.getName());
				// jo.append("e", f1.exists() ? 1 : 0);
				// jo.append("f", f1.isFile() ? 1 : 0);
				// jo.append("l", f1.length());
				// jo.append("u", f1.lastModified());
				// l1.add(jo);
			}
		}
		// JSON jo = JSON.create().append("list", l1);

		// out.writeString(jo.toString());
		handler.send(out);

	}

}
