package org.giiwa.zookeeper;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.giiwa.conf.Global;
import org.giiwa.dao.X;

/**
 * bug, may cause slow
 * 
 * @author joe
 *
 */
public class ZkLock implements Lock {

	private static Log log = LogFactory.getLog(ZkLock.class);

	private static CuratorFramework client = null;

	private String name;
	private InterProcessMutex lock = null;

	public static boolean isOk() {

		if (client != null) {
			return true;
		}

		if (zkserver == null) {
			return check() != null;
		}

		return false;

	}

	public static Lock create(String name) {

		check();

		ZkLock e = new ZkLock();
		e.name = name.replaceAll("/", "_");
		String path = "/lock/" + e.name;
		e.lock = new InterProcessMutex(client, path);
		return e;
	}

	@Override
	public String toString() {
		return "ZkLock [name=" + name + "]";
	}

	private static String zkserver = null;

	private static CuratorFramework check() {

		if (client == null) {

			zkserver = Global.getString("zookeeper.server", null);
			if (!X.isEmpty(zkserver)) {
				ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3, 5000);
				CuratorFramework zkClient = CuratorFrameworkFactory.builder().connectString(zkserver)
						.sessionTimeoutMs(5000).connectionTimeoutMs(5000).retryPolicy(retryPolicy).build();
				zkClient.start();

//				log.warn("zkclient=" + zkClient.getState());

				if (CuratorFrameworkState.STARTED.equals(zkClient.getState())) {
					client = zkClient;
				}
			}
		}
		return client;

	}

	@Override
	public void lock() {
		try {
			lock.acquire();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public void lockInterruptibly() throws InterruptedException {
		// TODO
	}

	@Override
	public boolean tryLock() {
		try {
			return tryLock(0, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return false;
	}

	@Override
	public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
		try {
			return lock.acquire(time, unit);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return false;
	}

	@Override
	public void unlock() {
		try {
			lock.release();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public Condition newCondition() {
		// TODO Auto-generated method stub
		return null;
	}

}
