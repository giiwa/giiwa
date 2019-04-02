package org.giiwa.core.dfile;

import org.apache.mina.core.buffer.IoBuffer;

public class Response {

	public long seq;
	public IoBuffer out;

	public static Response create(long seq, int size) {
		Response r = new Response();
		r.out = IoBuffer.allocate(size);
		r.out.setAutoExpand(true);
		r.out.putLong(seq);
		r.seq = seq;
		return r;
	}

	public void writeString(String s) {

		byte[] bb = s.getBytes();
		short l = (short) bb.length;
		out.putShort(l);
		out.put(bb);

	}

	public void writeLong(long s) {
		out.putLong(s);
	}

	public void writeInt(int s) {
		out.putInt(s);
	}

	public void writeBytes(byte[] bb) {

		int l = bb == null ? 0 : bb.length;
		out.putInt(l);
		if (l > 0) {
			out.put(bb);
		}

	}

	public void writeBytes(byte[] bb, int len) {

		out.putInt(len);
		out.put(bb, 0, len);

	}

	public void writeByte(byte b) {
		out.put(b);
	}

}
