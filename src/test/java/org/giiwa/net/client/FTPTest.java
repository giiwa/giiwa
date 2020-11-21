package org.giiwa.net.client;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.giiwa.dao.X;
import org.giiwa.misc.Url;
import org.junit.Test;

public class FTPTest {

	@Test
	public void list() {

		init();

		try {

			FTP s = FTP.create(Url.create("ftp://188.131.146.157?"));
			File[] l1 = s.list("/home/ftpuser1/done");

			System.out.println(X.asList(l1, f -> ((File) f).getName()));

//			s.mkdirs("/home/ftpuser1/done");
//			s.mkdirs("/home/ftpuser1/error");
//			s.cp("/home/ftpuser1/data/test.jl.zip", "/home/ftpuser1/done/test.jl.zip");
			s.mv("/home/ftpuser1/done/test.jl.zip", "/home/ftpuser1/error/test.jl.zip");

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
