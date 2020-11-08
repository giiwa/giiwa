package org.giiwa.engine;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.giiwa.task.Task;
import org.junit.Test;

public class RTest {

	@Test
	public void test() {
		Task.init(10);

		R.ROOT = "/Users/joe/d/temp/";

		R.serve();

		List<Double> l1 = new ArrayList<Double>();
		l1.add(0.1);
		l1.add(10d);
		l1.add(11d);
		l1.add(1d);
		l1.add(2d);

		System.out.println("mean=" + R.inst.mean(l1));
		System.out.println("sd=" + R.inst.sd(l1));
		System.out.println("cv=" + R.inst.cv(l1));
	}

}
