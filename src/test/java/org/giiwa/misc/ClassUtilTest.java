package org.giiwa.misc;

import static org.junit.Assert.*;

import org.giiwa.dao.Bean;
import org.junit.Test;

public class ClassUtilTest {

	@Test
	public void test() {
		System.out.println(ClassUtil.listSubType("", Bean.class));
	}

}
