package org.giiwa.misc;

import static org.junit.Assert.*;

import java.util.List;

import org.giiwa.json.JSON;
import org.giiwa.net.client.Http;
import org.junit.Test;

public class HtmlTest {

	@Test
	public void test() {
		String url = "https://www.af.mil/";
		Http h = Http.create();
		Http.Response r = h.get(url);
		Html h1 = r.html();

		try {
			List<JSON> l1 = h1.a("http(s|)://www.af.mil/News/.*", u -> {
				u.proto = "https";
			});
			l1.forEach(e -> {
				System.out.println(e.get("href"));
			});

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
