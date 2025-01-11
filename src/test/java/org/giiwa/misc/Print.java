package org.giiwa.misc;

import java.math.BigDecimal;

import org.junit.Test;

public class Print {

	@Test
	public void printDouble() {

		Double d = 1.00001;
		System.out.println(d.toString());
		d = 0.10001;
		System.out.println(d.toString());
		d = 0.00001;
		System.out.println(d.toString());
		System.out.println(Double.toString(d));
		BigDecimal b = new BigDecimal(d);
		System.out.println(b);

	}

}
