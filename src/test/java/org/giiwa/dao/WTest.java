package org.giiwa.dao;

import java.io.File;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.bson.types.ObjectId;
import org.giiwa.bean.Data;
import org.giiwa.conf.Config;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.junit.Test;

public class WTest {

	@Test
	public void testDate() throws SQLException {
		// var q = X.query('cip_info').and('cip_createdate >= 1641744000000').and('isbn
		// != null').toSQL()
		W q = W.create();
		q.and("cip_createdate >= 1641744000000");
		q.and("isbn != null");
		System.out.println(q.toSQL());

		q = W.create();
		q.and("date", new Date(System.currentTimeMillis()));
		System.out.println(q.toSQL());
		System.out.println(q.query());

	}

	@Test
	public void testBoost() throws SQLException {

		W q = W.create();
		q.and(new String[] { "a", "b", "c" }, "1");
		System.out.println(q.toSQL());

		q = W.create();
		q.and(new String[] { "a", "b" }, "1");
		System.out.println(q.toSQL());

		q = W.create();
		q.and(new String[] { "a", "b", "c" }, "1", new int[] { 1000 });
		System.out.println(q.toSQL());

	}

	@Test
	public void testSortkey() {
		try {
			W q = W.create();
			q.and("a=1 and b=1 order by a");
			System.out.println(q.toString());
			System.out.println(q.sortkeys());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testQuery() {

		try {
			Config.init(new File("/Users/joe/Downloads/temp/giiwa.properties"));

			Configuration conf = Config.getConf();

			Helper.init2(conf);

			long id = 2116252800807107236L;

			W q = W.create();
			q.and("id", id);

			System.out.println(q.query());
			Data d = Helper.primary.load("face_image", q, Data.class);
			System.out.println(d);

//			Beans<Data> l1 = Helper.load("face_image", W.create(), 0, 10, Data.class);
//			System.out.println(l1.get(0).json().toPrettyString());

			q = W.create().query("face_image");
			Beans<Bean> l2 = q.load(0, 10);
			System.out.println(l2.get(0).json().toPrettyString());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testUuid() {

		try {
			Config.init(new File("/Users/joe/Downloads/temp/giiwa.properties"));

			Configuration conf = Config.getConf();

			Helper.init2(conf);

			V v = V.create();
			v.append("id", UID.uuid());
			v.append("a", 3);
			Helper.primary.insertTable("demo", v);

			Beans<Bean> l1 = Helper.primary.load("demo", W.create(), 0, 100, Bean.class);
			for (Bean e : l1) {
				Object o = e.get("_id");
				System.out.println(o.getClass() + "/" + o);

				if (o instanceof ObjectId) {
					ObjectId o1 = (ObjectId) o;
					System.out.println(o1.toString());
					ObjectId o2 = new ObjectId(o.toString());
					System.out.println(o2);
				}
			}

			W q = W.create().query("demo");
			q.and("id=uuid('" + UID.uuid() + "')");
			System.out.println(q.query());

			Bean e = Helper.primary.load("demo", q, Bean.class);

			System.out.println(e);

			q = W.create().query("demo");
//			q.and("_id=objectid('644aea3badf7b773bad2b185')");
			q.and("_id", new ObjectId("644aea3badf7b773bad2b185"));
			System.out.println(q.query());

			e = Helper.primary.load("demo", q, Bean.class);

			if (e != null) {
				System.out.println(e.json());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testInsert() {

		try {
			Config.init(new File("/Users/joe/Downloads/temp/giiwa.properties"));

			Configuration conf = Config.getConf();

			Helper.init2(conf);

			V v = V.create();
			v.append("id", UID.uuid());
			v.append("a", "aaaa\\ddd");
			Helper.primary.insertTable("demo", v);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testQueryUuid() {

		try {
			Config.init(new File("/Users/joe/Downloads/temp/giiwa.properties"));

			Configuration conf = Config.getConf();

			Helper.init2(conf);

			W q = W.create();
			q.query("demo");
			q.and("id", 4);
//			q.and("_id", UUID.fromString("3b241101-e2bb-4255-8caf-4136c566a962"));
//
//			System.out.println(q.query());

			Bean e = Helper.primary.load(q.table(), q, Bean.class);
			if (e != null) {
				System.out.println(e.get("_id").getClass());

				q = W.create();
				q.query("demo");
				q.and("_id", e.get("_id"));
				System.out.println(q.query());

				e = Helper.primary.load(q.table(), q, Bean.class);

				if (e != null) {
					System.out.println(e.json());
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testA() {

		try {
			Config.init(new File("/Users/joe/Downloads/temp/giiwa.properties"));

			Configuration conf = Config.getConf();

			Helper.init2(conf);

			Pattern p1 = Pattern.compile("a\\\\");

			W q = W.create().query("demo");
			q.and("id like 'a\\'");
			System.out.println(q.query());

			Bean e = Helper.primary.load("demo", q, Bean.class);

			if (e != null) {
				System.out.println(e.json());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testLoad() {

		try {
			Config.init(new File("/Users/joe/Downloads/temp/giiwa.properties"));

			Configuration conf = Config.getConf();

			Helper.init2(conf);

			W q = W.create().query("demo").and("id", "01718497-6e1d-4e69-a28e-4d8e87245b2f");
			Bean e = Helper.primary.load(q.table(), q, Bean.class);
			if (e != null) {
				System.out.println(e.json());
				System.out.println(e.getString("a"));
			}

			q = W.create().query("demo").and("a like 'a\\'");
			e = Helper.primary.load(q.table(), q, Bean.class);
			if (e != null) {
				System.out.println(e.json());
				System.out.println(e.getString("a"));
			}

			q = W.create().query("demo").and("a like 'a$'");
			e = Helper.primary.load(q.table(), q, Bean.class);
			if (e != null) {
				System.out.println(e.json());
				System.out.println(e.getString("a"));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testNull() {

		try {
			W q = W.create();

			q.and("");
			System.out.println(q);

			q.and(W.create());
			System.out.println(q);

			q.and(W.create().and("a=1"));
			System.out.println(q);

			q.and(W.create());
			System.out.println(q);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testWhere() {

		W q = W.create();
		q.and("a", 1);
		System.out.println(q.where(null));

		q.and("b", 1);
		System.out.println(q.where(null));

		q.and(W.create().or("c", 1).or("c", 2));
		System.out.println(q.where(null));

	}

	@Test
	public void testAndOr() {

		try {
			Config.init();
			
			W q = W.create();
			q.and("a", 1);
			System.out.println(q.where(null));

			W q1 = W.create();
			q1.or("a", 1);
			q1.or("b", 2);
			q1 = q.copy().and(q1);
			System.out.println(q1.where(null));
			System.out.println(q1.query());

			q1 = q.copy().and("a=1 or b=2");
			System.out.println(q1.where(null));
			System.out.println(q1.query());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
