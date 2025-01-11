/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.dfile;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import org.giiwa.bean.Disk;

class DFileOutputStream extends OutputStream {

//	private static Log log = LogFactory.getLog(DFileOutputStream.class);

	String filename;
	Disk disk;
	FlushFunc flush;

	byte[] bb = new byte[1024 * 1024 * 4];
	int pos = 0;
	long offset = 0;

	public static DFileOutputStream create(Disk disk, RandomAccessFile raf, String filename, long offset,
			FlushFunc flush) {
		DFileOutputStream d = new DFileOutputStream();
		d.disk = disk;
		d.filename = filename;
		d.offset = offset;
		d.raf = raf;
		d.flush = flush;

		return d;
	}

	public static DFileOutputStream create(Disk disk, OutputStream out, String filename, long offset, FlushFunc flush) {
		DFileOutputStream d = new DFileOutputStream();
		d.disk = disk;
		d.filename = filename;
		d.offset = offset;
		d.out = out;
		d.flush = flush;

		return d;
	}

	@Override
	public void write(int b) throws IOException {
		if (pos >= bb.length) {
			flush();
		}
		bb[pos++] = (byte) b;
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {

		if (b == null) {
			throw new NullPointerException();
		} else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
			throw new IndexOutOfBoundsException();
		} else if (len == 0) {
			return;
		}

		int n = 0;
		while (n < len) {
			int n1 = Math.min(len - n, bb.length - pos);
			System.arraycopy(b, off + n, bb, pos, n1);
			n += n1;
			pos += n1;
			flush();
		}
	}

	@Override
	public void flush() throws IOException {

//		log.warn("flush, pos=" + pos + ", filename=" + filename);

		if (pos > 0) {
			offset = flush.accept(offset, bb, pos);
			pos = 0;
		}

	}

	private RandomAccessFile raf = null;
	private OutputStream out = null;

	@Override
	public void close() throws IOException {

		flush();

//		log.warn("close, filename=" + filename);

		if (raf != null) {
			raf.close();
			raf = null;
		}

		if (out != null) {
			out.close();
			out = null;
		}

	}

	@FunctionalInterface
	public interface FlushFunc {
		long accept(long offset, byte[] bb, int len) throws IOException;
	}

}
