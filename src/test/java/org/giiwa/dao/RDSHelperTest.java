package org.giiwa.dao;

import static org.junit.Assert.*;

import java.sql.Connection;

import org.apache.commons.configuration2.Configuration;
import org.giiwa.bean.Data;
import org.giiwa.conf.Config;
import org.giiwa.dao.Helper.W;
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
			fail(e.getMessage());

		}
	}

	@Test
	public void testDM() {
		String url = "jdbc:dm://192.168.0.112:5236/DAMENG?user=SYSDBA&password=j12312345";
		try {

			Config.getConf().addProperty("db[default].url", url);
			RDB.init();
			RDSHelper h1 = new RDSHelper();
			W q1 = W.create();
			Beans<Data> bs = h1.load("gi_user", q1, 0, 10, Data.class);
			System.out.println(bs.size());

//			Connection con = RDB.getConnectionByUrl("dm", url, null, null);
//			System.out.println(con);
//
//			PreparedStatement ps = con.prepareStatement("select * from gi_user where id=?");
//			ps.setLong(1, 0);
//
//			ResultSet r = ps.executeQuery();
//			while (r.next()) {
//				System.out.println(r.getString(1));
//			}
//
//			con.close();

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());

		}
	}

}
