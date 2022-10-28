package org.giiwa.misc;

import java.io.File;

import org.junit.Test;

public class ExporterTest {

	@SuppressWarnings("rawtypes")
	@Test
	public void test() {
		try {
			Exporter ex = Exporter.create(new File("/Users/joe/d/temp/aaa.csv"), Exporter.FORMAT.csv);
			ex.print("a", "a,a", "a\"a\"a", "a\r\naa");
			
			ex.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
