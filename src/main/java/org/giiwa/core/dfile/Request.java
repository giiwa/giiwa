package org.giiwa.core.dfile;

import java.nio.ByteBuffer;

import org.apache.mina.core.buffer.IoBuffer;

public class Request {

	public static final int BIG = 1024 * 128;
	public static final int MID = 1024 * 16;
	public static final int SMALL = 1024;

	private ByteBuffer in;
	public long seq;

	public static Request create(byte[] b) {

		Request r = new Request();
		r.in = ByteBuffer.allocate(b.length);
		r.in.put(b);
		r.in.flip();

		r.seq = r.in.getLong();
		return r;
	}

	public boolean hasRemaining() {
		return in != null && in.hasRemaining();
	}

	public String readString() {
		short l = in.getShort();
		byte[] bb = new byte[l];
		in.get(bb);
		return new String(bb);
	}

	public long readLong() {
		return in.getLong();
	}

	public byte readByte() {
		return in.get();
	}

	public int readInt() {
		return in.getInt();
	}

	public byte[] readBytes() {
		int s = in.getInt();
		if (s == 0)
			return null;

		byte[] bb = new byte[s];
		in.get(bb);
		return bb;
	}

	public static Request read(IoBuffer bb) {

		if (bb == null)
			return null;

		if (bb.remaining() < 4)
			return null;

		bb.mark();
		int len = bb.getInt();
		if (bb.remaining() < len) {
			bb.reset();
			return null;
		}

		byte[] d = new byte[len];
		bb.get(d);
		Request r = create(d);
		return r;
	}

}
