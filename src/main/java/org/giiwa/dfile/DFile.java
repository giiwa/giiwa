package org.giiwa.dfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.giiwa.bean.Disk;
import org.giiwa.bean.Node;
import org.giiwa.dao.X;
import org.giiwa.misc.Base32;

/**
 * Demo bean
 * 
 * @author joe
 * 
 */

public abstract class DFile {

	public abstract String getFilename();

	public abstract Node getNode_obj();

	public abstract Disk getDisk_obj();

	public abstract boolean check();

	public abstract boolean exists();

	public abstract String getAbsolutePath();

	public String getCanonicalPath() {
		return X.getCanonicalPath(this.getAbsolutePath());
	}

	public abstract boolean delete();

	public String getId() {
		return Base32.encode(this.getFilename().getBytes());
	}

	public abstract boolean delete(long age);

	public abstract InputStream getInputStream();

	public OutputStream getOutputStream() throws FileNotFoundException {
		return this.getOutputStream(0);
	}

	public abstract OutputStream getOutputStream(long offset) throws FileNotFoundException;

	public abstract boolean mkdirs();

	public abstract DFile getParentFile();

	public abstract boolean isDirectory();

	public abstract boolean isFile();

	public String getName() {
		String[] ss = X.split(getFilename(), "[/]");
		if (ss != null && ss.length > 0) {
			return ss[ss.length - 1];
		}
		return X.EMPTY;
	}

	public abstract DFile[] listFiles() throws IOException;

	public abstract long getCreation();

	public abstract long lastModified();

	public abstract long length();

	public abstract boolean move(DFile file);

	/**
	 * copy the file and upload to disk
	 * 
	 * @param f the File
	 * @return the actually length
	 * @throws FileNotFoundException
	 */
	public long upload(File f) throws FileNotFoundException {
		return upload(0, new FileInputStream(f));
	}

	public long upload(InputStream in) {
		return upload(0, in);
	}

	/**
	 * upload the inputsteam to the file
	 * 
	 * @param pos the position
	 * @param in  the inputstream
	 * @return the size
	 */
	public abstract long upload(long pos, InputStream in);

	/**
	 * download the file to local file
	 * 
	 * @param f the local file
	 * @return the size
	 */
	public abstract long download(File f);

	public abstract long count(Consumer<String> moni);

	public abstract long sum(Consumer<String> moni);

	public abstract Path getPath();

}
