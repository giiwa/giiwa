package org.giiwa.net.nio;

import static org.junit.Assert.*;

import org.giiwa.task.Task;
import org.junit.Test;

public class ServerTest {

	@Test
	public void test() {
		try {

			Task.init(100);

			Server.create().group(2, 8).handler((req, resp) -> {

//				System.out.println(req.size());

				if (req.size() > 4) {
					req.mark();
					byte[] b = new byte[128];
					int n = req.readBytes(b);
					if (b[n - 4] == 13 && b[n - 3] == 10 && b[n - 2] == 13 && b[n - 1] == 10) {

						System.out.println(new String(b, 0, n));

						String s = "HTTP/1.1 200\n\nhello world!\n\n\n\n";

						resp.write(s.getBytes());
						resp.send();
						resp.close();
					} else {
						req.reset();
					}
				}

			}).bind("127.0.0.1", 9092);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());

		}
	}

}
