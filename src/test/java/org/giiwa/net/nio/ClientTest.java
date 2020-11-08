package org.giiwa.net.nio;

import static org.junit.Assert.*;

import java.io.IOException;

import org.giiwa.task.Task;
import org.junit.Test;

public class ClientTest {

	@Test
	public void test() {
		Task.init(10);
		try {
			Client c = Client.create();
			c.connect("tcp://127.0.0.1:9092", (resp) -> {
				int n = resp.size();
				byte[] bb = new byte[n];
				n = resp.readBytes(bb);
				System.out.println("------------------");
				System.out.println(new String(bb, 0, n));
			});

			Task[] tt = new Task[1];
			for (int i = 0; i < tt.length; i++) {
				tt[i] = new Task() {
					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					int n = 1000;

					@Override
					public void onFinish() {
						if (n > 0)
							this.schedule(0);
					}

					@Override
					public void onExecute() {
						n--;
						IoResponse r = c.packet();
						r.write(("n=" + n).getBytes());
						r.send();
					}

				};
			}

			for (Task t : tt) {
				t.schedule(0);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());

		}
	}

}
