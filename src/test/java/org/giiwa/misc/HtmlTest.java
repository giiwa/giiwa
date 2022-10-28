package org.giiwa.misc;

import static org.junit.Assert.*;

import java.util.List;

import org.giiwa.json.JSON;
import org.giiwa.net.client.Http;
import org.jsoup.select.Elements;
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
			fail(e.getMessage());

		}
	}

	@Test
	public void testSelect() {

		Http h = Http.create();

		String url = "https://raw.githubusercontent.com/freeCodeCamp/freeCodeCamp/HEAD/README.md";
		Http.Response r = h.get(url);
//		System.out.println(r.body);

		Html h1 = r.html();

		try {

//			System.out.println(h1.body());
			Elements es = h1.select("div");
			System.out.println(es.get(0));
//			System.out.println(es.get(0).toString());

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());

		}

	}

	@Test
	public void testFormat() {
		Html h1 = Html.create("");
		h1.url = "https://www.sigsauer.swiss/en/sg-551-swat-assault-rifle.php";

		String img = "../media/images/ammunition/551-munition.png";

		System.out.println(h1.format(img));
	}

}
