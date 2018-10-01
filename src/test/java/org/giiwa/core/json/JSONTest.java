package org.giiwa.core.json;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class JSONTest {

	@Test
	public void test() {

		List<String> l1 = Arrays.asList("aaaa", "bbbbb");
		List<JSON> l2 = JSON.createList();
		l2.add(JSON.create().append("a", 1));
		l2.add(JSON.create().append("a", 2));

		JSON jo = JSON.create();
		jo.append("root", JSON.create().append("bb", 1).append("aa", l1).append("l2", l2));

		System.out.println(jo.toPrettyString());

		System.out.println(jo.toXml("GBK"));

		String s = "<?xml version=\"1.0\" encoding=\"GBK\"?>\n"
				+ "<root><bb>kkkkkk</bb><aa id='0'>aaaa</aa><aa id='1'>kkk</aa></root>";

		jo = JSON.fromXml(s);
		System.out.println(jo.toPrettyString());

		jo = JSON.create();
		l2 = JSON.createList();
		for (int i = 0; i < 24; i++) {
			l2.add(JSON.create().append("name", "p" + (2 * i + 1)).append("x", i * 60).append("y", 100)
					.append("normal", "/images/panel/HW-S5720-56PC/1.normal.png")
					.append("green", "/images/panel/HW-S5720-56PC/1.green.png")
					.append("red", "/images/panel/HW-S5720-56PC/1.red.png"));
			l2.add(JSON.create().append("name", "p" + (2 * i + 2)).append("x", i * 60).append("y", 20)
					.append("normal", "/images/panel/HW-S5720-56PC/1.normal.png")
					.append("green", "/images/panel/HW-S5720-56PC/1.green.png")
					.append("red", "/images/panel/HW-S5720-56PC/1.red.png"));
		}
		jo.append("el", l2);

		System.out.println(jo.toPrettyString());
	}

}
