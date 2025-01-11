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
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Disk;

class DFileInputStream extends InputStream {

	private static Log log = LogFactory.getLog(DFileInputStream.class);

	DFile file;
	InputStream in;

	public static DFileInputStream create(DFile file, InputStream in) {
		DFileInputStream d = new DFileInputStream();
		d.file = file;
		d.in = in;

		return d;
	}

	@Override
	public int read() throws IOException {
		long t = System.currentTimeMillis();
		try {
			return in.read();
		} finally {
			Disk.Counter.read(file.getDisk_obj()).add(1, System.currentTimeMillis() - t);
		}
	}

	@Override
	public int read(byte[] b) throws IOException {
		long t = System.currentTimeMillis();
		int len = 0;
		try {
			len = in.read(b);
		} catch (IOException e) {
			log.error("disk=" + file.getDisk_obj() + ", filename=" + file.filename, e);
			throw e;
		} finally {
			Disk.Counter.read(file.getDisk_obj()).add(len, System.currentTimeMillis() - t);
		}
		return len;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		long t = System.currentTimeMillis();
		int len1 = 0;
		try {
			len1 = in.read(b, off, len);
		} catch (IOException e) {
			log.error("disk=" + file.getDisk_obj() + ", filename=" + file.filename, e);
			throw e;
		} finally {
			Disk.Counter.read(file.getDisk_obj()).add(len1, System.currentTimeMillis() - t);
		}
		return len1;

	}

	@Override
	public byte[] readAllBytes() throws IOException {
		long t = System.currentTimeMillis();
		byte[] bb = null;
		try {
			bb = in.readAllBytes();
		} finally {
			Disk.Counter.read(file.getDisk_obj()).add(bb == null ? 0 : bb.length, System.currentTimeMillis() - t);
		}
		return bb;

	}

	@Override
	public byte[] readNBytes(int len) throws IOException {
		long t = System.currentTimeMillis();
		byte[] bb = null;
		try {
			in.readNBytes(len);
		} finally {
			Disk.Counter.read(file.getDisk_obj()).add(bb == null ? 0 : bb.length, System.currentTimeMillis() - t);
		}
		return bb;
	}

	@Override
	public int readNBytes(byte[] b, int off, int len) throws IOException {
		long t = System.currentTimeMillis();
		int len1 = 0;
		try {
			len1 = in.readNBytes(b, off, len);
		} finally {
			Disk.Counter.read(file.getDisk_obj()).add(len1, System.currentTimeMillis() - t);
		}
		return len1;
	}

	@Override
	public long skip(long n) throws IOException {
		return in.skip(n);
	}

//	@Override
//	public void skipNBytes(long n) throws IOException {
//		in.skipNBytes(n);
//	}

	@Override
	public int available() throws IOException {
		return in.available();
	}

	@Override
	public void close() throws IOException {
		in.close();
	}

	@Override
	public synchronized void mark(int readlimit) {
		in.mark(readlimit);
	}

	@Override
	public synchronized void reset() throws IOException {
		in.reset();
	}

	@Override
	public boolean markSupported() {
		return in.markSupported();
	}

	@Override
	public long transferTo(OutputStream out) throws IOException {
		return in.transferTo(out);
	}

}
