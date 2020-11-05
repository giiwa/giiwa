package org.giiwa.net.client;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.Vector;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.giiwa.dao.X;
import org.giiwa.misc.Url;
import org.junit.Test;

import com.jcraft.jsch.ChannelSftp.LsEntry;

class FTPTest {

	@Test
	public void list() {

		try {

			FTP s = FTP.connect(Url.create("ftp://188.131.146.157?username=ftpuser1&passwd=ftp@666"));
			File[] l1 = s.list("/home/ftpuser1");
			System.out.println(X.asList(l1, f -> ((File) f).getName()));

			s.close();

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

}
