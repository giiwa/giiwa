package org.giiwa.net.nio;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

public class IoResponse {

//	private static Log log = LogFactory.getLog(IoResponse.class);

	public ByteBuf data;
	private Channel ch;

	public void close() {

		if (ch != null) {
			ch.disconnect();
//			ch.close();
		}
	}

	public void send(Object... head) {

//		System.out.println("size=" + data.readableBytes());

		if (head != null && head.length > 0) {

			ByteBuf o1 = Unpooled.buffer(data.readableBytes() + 128);

			for (Object o : head) {
				if (o instanceof Byte) {
					o1.writeByte((byte) o);
				} else if (o instanceof Short) {
					o1.writeShort((short) o);
				} else if (o instanceof Integer) {
					o1.writeInt((int) o);
				} else if (o instanceof Long) {
					o1.writeLong((long) o);
				} else if (o instanceof ByteBuf) {
					o1.writeBytes((ByteBuf) o);
				} else if (o.getClass().isArray()) {
					o1.writeBytes((byte[]) o);
				}
			}

//			log.debug("data=" + data.readableBytes() + "<" + data.writableBytes() + "<" + data.capacity());
//			log.debug("o1=" + o1.readableBytes() + "<" + o1.writableBytes() + "<" + o1.capacity());

			o1.writeBytes(data);
			ch.writeAndFlush(o1);

		} else {
			data.retain();
			ch.writeAndFlush(data);
		}

		_compact();

	}

	public void write(long s) {
		data.writeLong(s);
	}

	public void write(int s) {
		data.writeInt(s);
	}

	public void write(byte[] bb) {
		data.writeBytes(bb);
	}

	public void write(byte[] bb, int offset, int len) {
		data.writeBytes(bb, offset, len);
	}

	public void write(byte b) {
		data.writeByte(b);
	}

	public static IoResponse create(Channel ch) {

		IoResponse r = new IoResponse();
		r.ch = ch;
		r.data = Unpooled.buffer(1024);
		return r;

	}

	private void _compact() {

		if (data.readerIndex() < 1024)
			return;

		ByteBuf b = Unpooled.buffer(data.readableBytes() + 1024);
		if (data.readableBytes() > 0) {
			b.writeBytes(data);
		}

		data.release();
		data = b;

	}

	public int size() {
		return data.readableBytes();
	}

	public void release() {
		data.release();
	}

//	public void retain() {
//		data.retain();
//	}

}
