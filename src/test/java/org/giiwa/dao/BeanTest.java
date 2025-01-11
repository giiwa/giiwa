package org.giiwa.dao;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.giiwa.dao.Helper.V;
import org.giiwa.json.JSON;
import org.junit.Test;

public class BeanTest {

	@SuppressWarnings({ "serial", "unused" })
	@Test
	public void test() {
		try {
			Bean b = new Bean() {
				transient int aaa = 1;

				public String toString() {
					return json().append("aaa", aaa).toString();
				}
			};
			b.set("a.a", 1);
			System.out.println(b.toString());

			JSON j1 = b.json();
//			System.out.println(j1.toPrettyString());

			Bean a = (Bean) b.clone();
			System.out.println(a);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());

		}
	}

	@Test
	public void testUrl() {
		Bean b = new Bean();
		b.set("url", "https://123.com");
		System.out.println(b.json());
		System.out.println(b.json().toUrl());

	}

	@Test
	public void testparallelStream() {

		Beans<Bean> bs = Beans.create();
		for (int i = 0; i < 10; i++) {
			Bean b = new Bean();
			b.set("i", i);
			bs.add(b);
		}

		bs.parallelStream().forEach(e -> {
			System.out.println(Thread.currentThread().getName() + "/" + e.get("i"));
		});
	}

	@SuppressWarnings("serial")
	@Test
	public void testList() {
		Bean b = new Bean() {
			@SuppressWarnings("unused")
			public List<String> l1;
			@SuppressWarnings("unused")
			public List<String> l2;
		};

		String l2 = Arrays.asList("a", "b", "\",'c").toString();
		b.set("l1", "a,b,c");
		b.set("l2", l2);
		System.out.println(b.json());
		System.out.println(b.get("l2"));

	}

	@Test
	public void testContain() {

		JSON j1 = JSON.fromObject(
				"{\"created\":1640968370934,\"isbn\":\"9787308083782\",\"increment\":469052,\"increase\":0,\"_node\":\"3c060706-1df6-4284-a2de-1ec109e486e3\",\"volume\":469099,\"_indexed\":0,\"month\":202201,\"from\":\"dd\",\"_id\":\"dvvk0xqyz5ynn\",\"id\":\"dvvk0xqyz5ynn\",\"updated\":1641791792708}");
		Bean e = new Bean();
		e.putAll(j1);
		System.out.println(e.json());

		JSON j2 = JSON.fromObject(
				"{\"month\":202201,\"isbn\":\"9787308083782\",\"from\":\"dd\",\"volume\":469099,\"increase\":0,\"id\":\"dvvk0xqyz5ynn\"}");

		V v = V.fromJSON(j2);
		System.out.println(v);

		System.out.println(e.contains(v));

	}

	@Test
	public void testFilename() {
		String filename = " 1*2$3?5-6%";
		filename = filename.replaceAll("[ *$?\\-%]", "_");
		System.out.println(filename);

		filename = "type.keyword";
		filename = filename.replace(".keyword", "");
		System.out.println(filename);

	}

	@Test
	public void testStartswith() {

		String s1 = "type:1,tag:1";
		String s2 = "type:1,tag:1,is_military:1";

		System.out.println(s2.startsWith(s1));

	}

	@Test
	public void testNULL() {

		Bean b = new Bean();
		b.set("a", null);
		System.out.println(b.json());
		System.out.println(b.json().toUrl());

	}

}
