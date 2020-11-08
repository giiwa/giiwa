package org.giiwa.dao;

import static org.junit.Assert.*;

import java.sql.Connection;

import org.apache.commons.configuration2.Configuration;
import org.giiwa.conf.Config;
import org.junit.Test;

public class RDSHelperTest {

	@Test
	public void test() {
		String url = "jdbc:h2:file:/Users/joe/d/temp/db/demo1";
		try {
			Connection con = RDB.getConnectionByUrl("h2", url, null, null);
			System.out.println(con);
			con.close();

			Configuration conf = Config.getConf();
			conf.setProperty("db[default].url", url);
			RDB.init();

			con = RDB.getConnection("default");
			System.out.println(con);
			con.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
