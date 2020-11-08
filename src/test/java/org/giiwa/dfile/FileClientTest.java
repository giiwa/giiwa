package org.giiwa.dfile;

import static org.junit.Assert.*;

import org.junit.Test;

public class FileClientTest {

	@Test
	public void test() {
		try {
			FileClient c = FileClient.get("tcp://g14:9099", "/Users/joe/d/temp");

			// System.out.println(c.info("/Users/joe/d/temp", "/"));
			//
			// System.out.println(c.list("/Users/joe/d/temp", "/"));

			// c.mkdirs("/Users/joe/d/temp", "/tttttt");

			// c.delete("/Users/joe/d/temp", "/tttttt");

			// c.put("/Users/joe/d/temp", "/tttttt/t.txt", 0, "abcde".getBytes());

			// byte[] bb = c.get("/Users/joe/d/temp", "/tttttt/t.txt", 0, 100);
			// System.out.println(new String(bb));

			// DFileOutputStream out = DFileOutputStream.create("127.0.0.1", 9099,
			// "/Users/joe/d/temp", "/tttttt/t.txt");
			// out.write("asdasdasdasdasdasdasda".getBytes());
			// out.flush();
			// out.close();
			//
			// Disk d = new Disk();
			// d.set("path", "/Users/joe/d/temp");
			// Node n = new Node();
			// n.set("ip", "127.0.0.1");
			// n.set("port", 9099);
			// d.node_obj = n;

			// DFileInputStream in = DFileInputStream.create(d,
			// "/tttttt/WechatIMG3431.jpeg");
			// IOUtil.copy(in, new FileOutputStream("/Users/joe/d/temp/tttttt/a.jpg"));

			MockRequest req = new MockRequest();
			MockResponse resp = new MockResponse();

			c.http("/admin/device", req, resp, "get", "");

			System.out.println(resp);

			System.out.println("ok");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());

		}
	}

}
