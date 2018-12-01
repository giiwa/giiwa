package org.giiwa.core.dfile.command;

import java.io.File;
import java.util.List;

import org.giiwa.core.dfile.ICommand;
import org.giiwa.core.json.JSON;
import org.giiwa.core.nio.IResponseHandler;
import org.giiwa.core.nio.Request;
import org.giiwa.core.nio.Response;

public class LIST implements ICommand {

	@Override
	public void process(Request in, IResponseHandler handler) {
		String path = in.readString();
		String filename = in.readString();

		File f = new File(path + "/" + filename);
		List<JSON> l1 = JSON.createList();

		File[] ff = f.listFiles();
		if (ff != null) {
			for (File f1 : ff) {
				JSON jo = JSON.create();
				jo.append("name", f1.getName());
				jo.append("e", f1.exists() ? 1 : 0);
				jo.append("f", f1.isFile() ? 1 : 0);
				jo.append("l", f1.length());
				jo.append("u", f1.lastModified());
				l1.add(jo);
			}
		}
		JSON jo = JSON.create().append("list", l1);

		Response out = Response.create(in.seq);
		out.writeString(jo.toString());
		handler.send(out);

	}

}
