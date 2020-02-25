package org.giiwa.net.nio;

import org.apache.mina.core.buffer.IoBuffer;

public class IoRequest {

	public static final int BIG = 1024 * 128;
	public static final int MID = 1024 * 16;
	public static final int SMALL = 1024;

	public static IoRequest create() {
		IoRequest r = new IoRequest();
		r.data = IoBuffer.allocate(1024);
		r.data.setAutoExpand(true);
		return r;
	}

	public static IoRequest create(IoBuffer bb) {

		IoRequest r = new IoRequest();
		r.data = IoBuffer.allocate(bb.remaining());
		r.data.setAutoExpand(true);
		r.data.put(bb);

		return r;
	}

	public static IoRequest create(byte[] bb) {

		IoRequest r = new IoRequest();
		r.data = IoBuffer.allocate(bb.length);
		r.data.put(bb);
		return r;
	}

	public IoRequest mark() {
		data.mark();
		return this;
	}

	public IoRequest reset() {
		data.reset();
		return this;
	}

	public int size() {
		return data.remaining();
	}

	public long readLong() {
		return data.getLong();
	}

	public byte readByte() {
		return data.get();
	}

	public int readInt() {
		return data.getInt();
	}

	public int readBytes(byte[] bb) {
		int len = Math.min(bb.length, data.remaining());
		data.get(bb, 0, len);
		return len;
	}

	public byte[] readBytes(int len) {
		byte[] bb = new byte[len];
		data.get(bb, 0, len);
		return bb;
	}

	private IoBuffer data = null;

	public IoRequest put(IoBuffer bb) {
		data.put(bb);
		return this;
	}

	public void release() {
		if (data != null) {
			data.free();
			data = null;
		}
	}

	public IoRequest compact() {
		data.compact();
		return this;
	}

	public IoRequest flip() {
		data.flip();
		return this;
	}

}
