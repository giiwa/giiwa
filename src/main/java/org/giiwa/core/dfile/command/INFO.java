package org.giiwa.core.dfile.command;

import java.io.File;

import org.giiwa.core.dfile.ICommand;
import org.giiwa.core.dfile.IResponseHandler;
import org.giiwa.core.dfile.Request;
import org.giiwa.core.dfile.Response;

public class INFO implements ICommand {

	@Override
	public void process(Request in, IResponseHandler handler) {

		String path = in.readString().replaceAll("[/\\\\]", "/");
		String filename = in.readString().replaceAll("[/\\\\]", "/");
		
		File f = new File(path + File.separator + filename);

		// JSON jo = JSON.create();
		// jo.append("e", f.exists() ? 1 : 0);
		// jo.append("f", f.isFile() ? 1 : 0);
		// jo.append("l", f.length());
		// jo.append("u", f.lastModified());

		Response out = Response.create(in.seq, Request.SMALL);
		out.writeInt(f.exists() ? 1 : 0);
		out.writeInt(f.isFile() ? 1 : 0);
		out.writeLong(f.length());
		out.writeLong(f.lastModified());

		// out.writeString(jo.toString());
		handler.send(out);

	}

}
