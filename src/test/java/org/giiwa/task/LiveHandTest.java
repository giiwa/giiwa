package org.giiwa.task;

import static org.junit.Assert.*;

import org.junit.Test;

public class LiveHandTest {

	@Test
	public void test() {
		LiveHand h = LiveHand.create(5);
		System.out.println("holding");
		try {
			h.lock();
			h.release();
			h.await(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("done");
	}

}
