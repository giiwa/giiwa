package org.giiwa.core.nio;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

public class IoProtocol extends IoHandlerAdapter {

	@Override
	final public void messageReceived(IoSession session, Object message) throws Exception {
		messageReceived(session, (IoBuffer) message);
	}

	public void messageReceived(IoSession session, IoBuffer message) throws Exception {

	}

	@Override
	final public void messageSent(IoSession session, Object message) throws Exception {
		messageSent(session, (IoBuffer) message);
	}

	final public void messageSent(IoSession session, IoBuffer message) throws Exception {

	}

}
