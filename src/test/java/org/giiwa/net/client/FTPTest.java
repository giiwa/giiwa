package org.giiwa.net.client;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.giiwa.bean.Temp;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.misc.Url;
import org.giiwa.web.Language;
import org.junit.Test;

public class FTPTest {

	@Test
	public void list() {

		init();

		try {

			FTP s = FTP.create();
			s.open("ftp://192.168.0.106:21/?username=demo&passwd=123123", "GBK");
			File[] l1 = s.list("/");

			System.out.println(X.asList(l1, f -> ((File) f).getName()));

//			s.mkdirs("/home/ftpuser1/done");
//			s.mkdirs("/home/ftpuser1/error");
//			s.cp("/home/ftpuser1/data/test.jl.zip", "/home/ftpuser1/done/test.jl.zip");
//			s.mv("/home/ftpuser1/done/test.jl.zip", "/home/ftpuser1/error/test.jl.zip");

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

	@Test
	public void testGet() {

		init();

		try {

			FTP s = FTP.create(Url.create("ftp://188.131.146.157?username=ftpuser1&passwd=ftp@666"));
			File[] l1 = s.list("/home/ftpuser1/error");
			for (File f1 : l1) {

				Temp t = s.get(f1);
				File f2 = t.getFile();

				System.out.println(f1.length() + "/" + f2.length() + ", filename=" + f2.getAbsolutePath());

			}

			s.close();

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testGet2() {

		Language lang = Language.getLanguage("zh_cn");

		init();

		try {

			FTP s = FTP.create(Url.create("ftp://188.131.146.157?username=ftpuser1&passwd=jkgzysj@6"));
			File[] l1 = s.list("/home/ftpuser1/data");

			for (File f1 : l1) {

				System.out.println("starting download, file=" + f1.getName());
				TimeStamp t1 = TimeStamp.create();
				Temp t = s.get(f1);
				File f2 = t.getFile();

				System.out.println("size=" + lang.size(f1.length()) + ", cost=" + t1.past() + ", file=" + f1.getName());
//				System.out.println(f1.length() + "/" + f2.length() + ", size=" + lang.size(f1.length()));

			}

			s.close();

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

}
