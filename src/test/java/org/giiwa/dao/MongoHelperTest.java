package org.giiwa.dao;

import java.sql.SQLException;
import java.util.List;

import org.bson.Document;
import org.giiwa.conf.Config;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.junit.Test;

import com.mongodb.BasicDBObject;

public class MongoHelperTest {

	@Test
	public void test() throws SQLException {

		MongoHelper h = MongoHelper.create("mongodb://g09:27018/demo", null, null);
		long i = h.count("gi_user", W.create());
		System.out.println("count=" + i);

//		List<JSON> l1 = h.count("gi_user", W.create().and("id", 1, W.OP.gt).sort("count", -1), new String[] { "a" },
//				Helper.DEFAULT, 10);
//		System.out.println("count=" + l1);

		List<JSON> l1 = h.aggregate("gi_user", new String[] { "count(b)", "min(a)" }, W.create(), new String[] { "a" });
		System.out.println("count=" + l1);
	}

	@Test
	public void testLike() throws SQLException {

		MongoHelper h = MongoHelper.create("mongodb://g09:27018/demo", null, null);

		W q = W.table("d_copy");
//		q.and("content like 'and(\"f_tag\", \"ys\")'");
//		q.and("content", "and(\"type\", \"NEWS\")", W.OP.like);
		q.and("content", "and(\"type\", \"NEWS\"", W.OP.like);

		Bean e = h.load("d_copy", q, Bean.class);
		System.out.println(e == null ? null : e.json("content"));

	}

	@Test
	public void testCount() throws SQLException {

		MongoHelper h = MongoHelper.create("mongodb://127.0.0.1:27018/demo", null, null);

		TimeStamp t = TimeStamp.create();
		long n = h.count("gi_stat_dput_dmeta", W.create());
		System.out.println("cost=" + t.past() + ", n=" + n);

		W q = W.create();
		q.and("created", System.currentTimeMillis() - X.AWEEK * 3 - X.ADAY * 3, W.OP.lte);
		t.reset();
		n = h.count("gi_stat_dput_dmeta", q);
		System.out.println("cost=" + t.past() + ", n=" + n);

	}

	@Test
	public void testRun() throws SQLException {

		MongoHelper h = MongoHelper.create("mongodb://127.0.0.1:27018/admin", null, null);

		BasicDBObject cmd = new BasicDBObject();
		cmd.append("currentOp", 1);

		Document doc = h.run(cmd);
		JSON jo = JSON.fromObject(doc);
		System.out.println(jo.toPrettyString());

		List<JSON> l1 = jo.getList("inprog");
		if (l1 != null) {
			List<JSON> l2 = JSON.createList();
			for (JSON j1 : l1) {
				JSON j2 = JSON.create();
				long sec = j1.getLong("secs_running");
//				if (sec > 0) {
				j2.append("op", j1.get("op"));
				j2.append("opid", j1.get("opid"));
				j2.append("secs_running", sec);
				l2.add(j2);
//				}
			}
			System.out.println(JSON.toPrettyString(l2));
		}

	}

	@Test
	public void testSequoia() throws SQLException {

		MongoHelper h = MongoHelper.create("mongodb://g30:11810/demo", null, null);
		System.out.println("h=" + h.listTables(null, 1000));

		long i = h.count("gi_user", W.create());
		System.out.println("count=" + i);

//		List<JSON> l1 = h.count("gi_user", W.create().and("id", 1, W.OP.gt).sort("count", -1), new String[] { "a" },
//				Helper.DEFAULT, 10);
//		System.out.println("count=" + l1);

		List<JSON> l1 = h.aggregate("gi_user", new String[] { "count(b)", "min(a)" }, W.create(), new String[] { "a" });
		System.out.println("count=" + l1);
	}

	@Test
	public void testMongo() throws SQLException {

		Config.init();

		MongoHelper h = MongoHelper.create("mongodb://g30:55011/demo", "demo", "demo");
		System.out.println("h=" + h.listTables(null, 1000));

		long i = h.count("gi_user", W.create());
		System.out.println("count=" + i);

//		List<JSON> l1 = h.count("gi_user", W.create().and("id", 1, W.OP.gt).sort("count", -1), new String[] { "a" },
//				Helper.DEFAULT, 10);
//		System.out.println("count=" + l1);

		List<JSON> l1 = h.aggregate("gi_user", new String[] { "count(b)", "min(a)" }, W.create(), new String[] { "a" });
		System.out.println("count=" + l1);
	}

}
