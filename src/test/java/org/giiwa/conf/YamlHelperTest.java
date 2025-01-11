package org.giiwa.conf;

import java.io.FileInputStream;

import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.junit.Test;

public class YamlHelperTest {

	@Test
	public void test() {

		try {
			String filename = "/Users/joe/Documents/.config.yml";
			JSON j1 = JSON.fromObject(YamlHelper.load(new FileInputStream(filename)));

			System.out.println(j1.toPrettyString());
			System.out.println(j1.get("all.executable"));
			System.out.println(X.isIn("post", j1.get("all.executable")));

			String head = j1.getString("all.cache.id.head");
			System.out.println("cookie".matches(head));

			String body = j1.getString("all.cache.id.body");
			System.out.println("cookie".matches(body));

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
