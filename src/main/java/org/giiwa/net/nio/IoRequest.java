package org.giiwa.net.nio;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class IoRequest {

	public static final int BIG = 1024 * 128;
	public static final int MID = 1024 * 16;
	public static final int SMALL = 1024;

	public static IoRequest create() {
		IoRequest r = new IoRequest();
		r.data = Unpooled.buffer();
		return r;
	}

	public static IoRequest create(ByteBuf bb) {

		System.out.println("new request");

		IoRequest r = new IoRequest();
		r.data = bb.duplicate();
		return r;
	}

	public void mark() {
		data.markReaderIndex();
	}

	public void reset() {
		data.resetReaderIndex();
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

	public void put(ByteBuf bb) {
		data.writeBytes(bb);
	}

	public void release() {
		if (data != null) {
			data.release();
			data = null;
		}
	}

}
