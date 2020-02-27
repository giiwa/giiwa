package org.giiwa.dfile.command;

import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.dfile.ICommand;
import org.giiwa.dfile.MockRequest;
import org.giiwa.dfile.MockResponse;
import org.giiwa.json.JSON;
import org.giiwa.net.nio.IoRequest;
import org.giiwa.net.nio.IoResponse;
import org.giiwa.web.Controller;

public class HTTP implements ICommand {

	@Override
	public void process(long seq, IoRequest req, IoResponse resp) {

		TimeStamp t = TimeStamp.create();

		String m = new String(req.readBytes(req.readInt()));
		String uri = new String(req.readBytes(req.readInt()));
		JSON head = JSON.fromObject(req.readBytes(req.readInt()));
		JSON body = JSON.fromObject(req.readBytes(req.readInt()));

		// log.debug("head=" + head.toString());
		// log.debug("body=" + body.toString());

		try {
			MockResponse resp1 = MockResponse.create();
			Controller.process(uri, MockRequest.create(uri, head, body), resp1, m, t);

			resp.write(resp1.status);
			byte[] b1 = resp1.head.toString().getBytes();
			resp.write(b1.length);
			resp.write(b1);
			b1 = resp1.out.toByteArray();
			resp.write(b1.length);
			resp.write(b1);

			X.close(resp1);

			resp.send(resp.size() + 8, seq);

		} catch (Exception e1) {
			try {

				resp.write((int) 500);

				byte[] b = e1.getMessage().getBytes();
				resp.write(b.length);
				resp.write(b);

				resp.send(resp.size() + 8, seq);

			} catch (Exception e2) {

			}
		}
	}

}
