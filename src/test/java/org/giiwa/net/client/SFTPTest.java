package org.giiwa.net.client;

import static org.junit.Assert.fail;

import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.PropertyConfigurator;
import org.giiwa.misc.Url;
import org.junit.Test;

import com.jcraft.jsch.ChannelSftp.LsEntry;

public class SFTPTest {

	@Test
	public void list() {

		init();
		
		try {

			SFTP s = SFTP.create(Url.create("sftp://188.131.146.157:22?"));
			Vector<LsEntry> l1 = s.list("/home/ftpuser1/data");
			System.out.println(l1);

			s.close();

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	public void init() {

		Properties prop = new Properties();
		prop.put("log4j.rootLogger", "WARN,Log1");
		prop.put("log4j.appender.Log1", "org.apache.log4j.ConsoleAppender");
		prop.put("log4j.appender.Log1.layout", "org.apache.log4j.PatternLayout");
		prop.put("log4j.appender.G.layout.ConversionPattern", "%p [%t] [%d] - %m - [%l]%n");
		prop.put("log4j.logger.org.giiwa", "debug");

		PropertyConfigurator.configure(prop);

	}

}
