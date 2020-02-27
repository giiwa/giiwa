package org.giiwa.demo.server;

import org.giiwa.net.nio.Server;

public class DemoServer {

	public static DemoServer inst = new DemoServer();

	public static void start() {

		try {
			Server.create().group(2, 8).handler((req, resp) -> {

				String s = "HTTP/1.1 200\nAccess-Control-Allow-Origin: no\nContent-Type: text/html;charset=UTF-8\nVary: Accept-Encoding\n\nhello world!\n";

				resp.write(s.getBytes());
				resp.send();
				resp.release();
				resp.close();

			}).bind("tcp://127.0.0.1:8091");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
