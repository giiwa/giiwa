package org.giiwa.net.nio;

import java.util.function.Function;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;

public class IoResponse {

	private IoBuffer out;
	private IoSession ch;

	public void close() {
		if (ch != null) {
			ch.closeOnFlush();
			ch = null;
		}
	}

	public IoResponse send(Function<IoBuffer, IoBuffer> func) {
		if (out != null && ch != null) {
			IoBuffer b = func.apply(out);
			if (b != null) {
				b.flip();
				ch.write(b);
			}
		}
		return this;

	}

	public IoResponse write(long s) {
		if (out == null) {
			out = IoBuffer.allocate(1024);
			out.setAutoExpand(true);
		}

		out.putLong(s);
		return this;

	}

	public IoResponse write(int s) {
		if (out == null) {
			out = IoBuffer.allocate(1024);
			out.setAutoExpand(true);
		}

		out.putInt(s);
		return this;

	}

	public IoResponse write(byte[] bb) {
		if (out == null) {
			out = IoBuffer.allocate(1024);
			out.setAutoExpand(true);
		}

		out.put(bb);
		return this;

	}

	public IoResponse write(byte[] bb, int offset, int len) {
		if (out == null) {
			out = IoBuffer.allocate(1024);
			out.setAutoExpand(true);
		}

		out.put(bb, offset, len);

		return this;

	}

	public IoResponse write(byte b) {
		if (out == null) {
			out = IoBuffer.allocate(1024);
			out.setAutoExpand(true);
		}

		out.put(b);

		return this;

	}

	public static IoResponse create(IoSession ch) {

		IoResponse r = new IoResponse();
		r.ch = ch;
		return r;

	}

	public void release() {
		if (out != null) {
			out.free();
			out = null;
		}
	}

	public void flush() {
		send(e -> e);
	}

}
