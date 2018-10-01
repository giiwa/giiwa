package org.giiwa.core.dfile;

import org.giiwa.core.dfile.FileServer.Request;
import org.giiwa.core.dfile.FileServer.Response;
import org.junit.Test;

public class FileServerTest {

	@Test
	public void test() {

		Response resp = Response.create(11);
		resp.writeString("aaaaa");
		resp.writeInt(10);
		resp.writeLong(100);
		resp.writeBytes("bbbbb".getBytes());

		Request req = Request.create(resp.out);
		System.out.println(req.readString());
		System.out.println(req.readInt());
		System.out.println(req.readLong());
		System.out.println(new String(req.readBytes()));

	}

}
