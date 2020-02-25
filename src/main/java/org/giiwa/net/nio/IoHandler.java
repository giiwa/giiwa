package org.giiwa.net.nio;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

public abstract class IoHandler extends IoHandlerAdapter {

	@Override
	public void sessionCreated(IoSession session) throws Exception {
//		System.out.println("created: " + Thread.currentThread().getName());
		super.sessionCreated(session);
	}

	@Override
	final public void messageReceived(IoSession session, Object message) throws Exception {

//		System.out.println("data: " + Thread.currentThread().getName());

		IoRequest req = (IoRequest) session.getAttribute("req");
		if (req == null) {
			req = IoRequest.create((IoBuffer) message);
			session.setAttribute("req", req);
		} else {
			req.put((IoBuffer) message);
		}

		IoResponse resp = IoResponse.create(session);
		req.flip();

		process(req, resp);

		resp.release();
		req.compact();

	}

	public abstract void process(IoRequest req, IoResponse resp);

}
