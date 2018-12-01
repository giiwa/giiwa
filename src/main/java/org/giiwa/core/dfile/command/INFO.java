package org.giiwa.core.dfile.command;

import java.io.File;

import org.giiwa.core.dfile.ICommand;
import org.giiwa.core.json.JSON;
import org.giiwa.core.nio.IResponseHandler;
import org.giiwa.core.nio.Request;
import org.giiwa.core.nio.Response;

public class INFO implements ICommand {

	@Override
	public void process(Request in, IResponseHandler handler) {
		String path = in.readString();
		String filename = in.readString();
		File f = new File(path + "/" + filename);

		JSON jo = JSON.create();
		jo.append("e", f.exists() ? 1 : 0);
		jo.append("f", f.isFile() ? 1 : 0);
		jo.append("l", f.length());
		jo.append("u", f.lastModified());

		Response out = Response.create(in.seq);

		out.writeString(jo.toString());
		handler.send(out);

	}

}
