package org.giiwa.dfile.command;

import org.apache.mina.core.buffer.IoBuffer;
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
		JSON head = JSON.fromObject(new String(req.readBytes(req.readInt())));
		JSON body = JSON.fromObject(new String(req.readBytes(req.readInt())));

		// log.debug("head=" + head.toString());
		// log.debug("body=" + body.toString());

		try {
			MockResponse resp1 = MockResponse.create();
			Controller.process(uri, MockRequest.create(uri, head, body), resp1, m, t);

			resp.write(resp1.status);
			resp.write((short) resp1.head.toString().getBytes().length).write(resp1.head.toString().getBytes());
			resp.write(resp1.out.toByteArray());
			X.close(resp1);

			resp.send(e -> {
				IoBuffer b = IoBuffer.allocate(1024);
				b.setAutoExpand(true);
				b.putInt(e.remaining() + 8);
				b.putLong(seq);
				b.put(e);
				return b;
			});

		} catch (Exception e1) {
			try {

				resp.write((int) 500);
				resp.write((short) X.EMPTY.getBytes().length).write(X.EMPTY.getBytes());
				resp.write(e1.getMessage().getBytes());

				resp.send(e -> {
					IoBuffer b = IoBuffer.allocate(1024);
					b.setAutoExpand(true);
					b.putInt(e.remaining() + 8);
					b.putLong(seq);
					b.put(e);
					return b;
				});

			} catch (Exception e2) {

			}
		} finally {
			if (log.isInfoEnabled())
				log.info(m + " - " + uri + ", cost=" + t.past());
		}
	}

}
