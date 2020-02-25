package org.giiwa.net.nio;

import java.util.function.Function;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class IoResponse {

	public ByteBuf out;
	private Channel ch;

	public void close() {
		if (ch != null) {
//			ctx.write(out).addListener(ChannelFutureListener.CLOSE);
//			System.out.println(ctx.channel().remoteAddress());
			ch.close();
		}
	}

	public IoResponse send(Function<ByteBuf, ByteBuf> func) {
		if (out != null && ch != null) {
			ByteBuf b = func.apply(out);
			if (b != null) {
				ch.write(b);
				ch.flush();
			}
		}
		return this;

	}

	public IoResponse write(long s) {
		if (out == null)
			out = ch.alloc().buffer();

		out.writeLong(s);
		return this;

	}

	public IoResponse write(int s) {
		out.writeInt(s);
		return this;

	}

	public IoResponse write(byte[] bb) {
		if (out == null)
			out = ch.alloc().buffer();

		out.writeBytes(bb);
		return this;

	}

	public IoResponse write(byte[] bb, int offset, int len) {
		if (out == null)
			out = ch.alloc().buffer();

		out.writeBytes(bb, offset, len);

		return this;

	}

	public IoResponse write(byte b) {
		if (out == null)
			out = ch.alloc().buffer();

		out.writeByte(b);

		return this;

	}

	public static IoResponse create(Channel ch) {

		IoResponse r = new IoResponse();
		r.ch = ch;
		return r;

	}

	public void release() {
		if (out != null) {
//			out.release();
			out = null;
		}
	}

	public void flush() {
		send(e -> e);
	}

}
