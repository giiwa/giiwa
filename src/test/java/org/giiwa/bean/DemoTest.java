package org.giiwa.bean;

import org.bson.Document;
import org.giiwa.dao.Bean;
import org.giiwa.dao.Column;
import org.junit.Test;

public class DemoTest extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	String name;

	@Column(name = "_type")
	String type;

	@Test
	public void test() {

		DemoTest e = new DemoTest();

		Document doc = new Document();
		doc.put("name", "a");
		doc.put("_type", "bbbbb");
		doc.put("type", null);

		e.load(doc);

		System.out.println(e.name);
		System.out.println(e.type);

	}

}
