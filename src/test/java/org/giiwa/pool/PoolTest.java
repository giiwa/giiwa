package org.giiwa.pool;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.giiwa.bean.User;
import org.giiwa.dao.Bean;
import org.giiwa.dao.X;
import org.giiwa.pool.Pool.IPoolFactory;
import org.junit.Test;

public class PoolTest {

	@Test
	public void testPool() {

		Pool<I> pool = Pool.create(10, 10, new IPoolFactory<I>() {

			@Override
			public I create() {
				return new A();
			}

			@Override
			public boolean check(I t) {
				return true;
			}

			@Override
			public void destroy(I t) {
				t.close();
			}

		});

		try {
			I a = pool.get(X.AMINUTE);
			System.out.println(pool.max());
			System.out.println(pool.avaliable());
			a.close();
			System.out.println(pool.max());
			System.out.println(pool.avaliable());
		} catch (Exception e) {
			e.printStackTrace();
		}

		pool.destroy();

	}

	@Test
	public void testDelegator() {
		A a = new A();
		try {
			I b = Delegator.create(a, null);
			System.out.println(b);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	interface I {
		void close();
	}

	public static class A implements I {
		public void close() {
			System.out.println("close me!");
		}
	}

	@Test
	public void testLock() {
		ReentrantLock lock = new ReentrantLock();

		try {
			lock.lock();
			System.out.println("1");
			lock.lock();
			System.out.println("2");
			lock.lock();
			System.out.println("3");

		} finally {
			lock.unlock();
		}

		System.out.println(lock.isLocked());

	}

}
