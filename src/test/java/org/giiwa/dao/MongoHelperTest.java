package org.giiwa.dao;

import static org.junit.Assert.*;

import java.util.List;

import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.junit.Test;

public class MongoHelperTest {

	@Test
	public void test() {
		MongoHelper h = MongoHelper.create("mongodb://127.0.0.1:27017/demo", "giiwa", "123123");
		long i = h.count("gi_user", W.create(), Helper.DEFAULT);
		System.out.println("count=" + i);

		List<JSON> l1 = h.count("gi_user", W.create().and("id", 1, W.OP.gt).sort("count", -1), new String[] { "a" },
				Helper.DEFAULT, 10);
		System.out.println("count=" + l1);

		l1 = h.max("gi_user", W.create().sort("max", -1), "b", new String[] { "a" }, Helper.DEFAULT);
		System.out.println("count=" + l1);

		l1 = h.aggregate("gi_user", new String[] { "count(b)", "min(a)" }, W.create(), new String[] { "a" },
				Helper.DEFAULT);
		System.out.println("count=" + l1);
	}

}
