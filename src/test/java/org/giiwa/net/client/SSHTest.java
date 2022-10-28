package org.giiwa.net.client;

import org.junit.Test;

public class SSHTest {

	@Test
	public void test() {

		try {
			SSH ssh = SSH.create();

			ssh.open("ssh://192.168.2.97?username=root&passwd=j1231234");

			String s1 = ssh.run("python2 -V");
			System.out.println(s1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
