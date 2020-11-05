package org.giiwa.net.client;

import static org.junit.Assert.fail;

import java.util.Vector;

import org.giiwa.misc.Url;
import org.junit.Test;

import com.jcraft.jsch.ChannelSftp.LsEntry;

class SFTPTest {

	@Test
	public void list() {

		try {

			SFTP s = SFTP.create(Url.create("sftp://188.131.146.157:22?username=ftpuser1&passwd=ftp@666"));
			Vector<LsEntry> l1 = s.list("/home/ftpuser1/data");
			System.out.println(l1);

			s.close();

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

}
