package org.giiwa.net.nio;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class IoRequest {

//	private static Log log = LogFactory.getLog(IoRequest.class);

	public static final int BIG = 1024 * 32;
	public static final int MID = 1024 * 16;
	public static final int SMALL = 1024;

	public static IoRequest create() {
		IoRequest r = new IoRequest();
		r.data = Unpooled.buffer(1024);
		return r;
	}

	public static IoRequest create(ByteBuf bb) {

		IoRequest r = new IoRequest();
		r.data = Unpooled.buffer(bb.readableBytes());
		r.data.writeBytes(bb);

		return r;
	}

	public static IoRequest create(byte[] bb) {

		IoRequest r = new IoRequest();
		r.data = Unpooled.wrappedBuffer(bb);
		return r;
	}

	public IoRequest mark() {
		data.markReaderIndex();
		return this;
	}

	public IoRequest reset() {
		data.resetReaderIndex();
		return this;
	}

	public int size() {
		return data.readableBytes();
	}

	public long readLong() {
		return data.readLong();
	}

	public byte readByte() {
		return data.readByte();
	}

	public int readInt() {
		return data.readInt();
	}

	public int readBytes(byte[] bb) {
		int len = Math.min(bb.length, data.readableBytes());
		data.readBytes(bb, 0, len);
		return len;
	}

	public byte[] readBytes(int len) {
		byte[] bb = new byte[len];
		data.readBytes(bb, 0, len);
		return bb;
	}

	private ByteBuf data = null;

	public IoRequest put(ByteBuf bb) {
		data.writeBytes(bb);
		return this;
	}

	public void release() {
		if (data != null) {
			data.release();
			data = null;
		}
	}

	public void compact() {
//		log.debug("data=" + data.readerIndex() + "<" + data.writerIndex() + "<" + data.capacity());
		ByteBuf b = Unpooled.buffer(data.readableBytes() + 1024);
		b.writeBytes(data);
		data.release();
		data = b;
//		log.debug("data=" + data.readerIndex() + "<" + data.writerIndex() + "<" + data.capacity());
	}

}
