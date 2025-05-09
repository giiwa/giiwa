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

public class DFileInputStream extends InputStream {

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
		return in.read();
	}

	@Override
	public int read(byte[] b) throws IOException {
		int len = 0;
		try {
			len = in.read(b);
		} catch (IOException e) {
			log.error("disk=" + file.getDisk_obj() + ", filename=" + file.filename, e);
			throw e;
		}
		return len;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int len1 = 0;
		try {
			len1 = in.read(b, off, len);
		} catch (IOException e) {
			log.error("disk=" + file.getDisk_obj() + ", filename=" + file.filename, e);
			throw e;
		}
		return len1;

	}

	@Override
	public byte[] readAllBytes() throws IOException {
		byte[] bb = in.readAllBytes();
		return bb;

	}

	@Override
	public byte[] readNBytes(int len) throws IOException {
		return in.readNBytes(len);
	}

	@Override
	public int readNBytes(byte[] b, int off, int len) throws IOException {
		return in.readNBytes(b, off, len);
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
