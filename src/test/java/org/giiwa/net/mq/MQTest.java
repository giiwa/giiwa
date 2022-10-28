package org.giiwa.net.mq;

import static org.junit.Assert.*;

import org.apache.activemq.ActiveMQConnection;
import org.giiwa.conf.Global;
import org.giiwa.dao.X;
import org.giiwa.net.mq.MQ.Request;
import org.junit.Test;

public class MQTest {

	@Test
	public void test() {
		String type = "queue".toUpperCase();

		System.out.println(MQ.Mode.valueOf(type));
	}

	@Test
	public void testArtemis() {
		String url = "failover:(tcp://g106:61616)?timeout=3000&jms.prefetchPolicy.all=2&jms.useAsyncSend=true";

		Global.setConfig("mq.type", "activemq");

		Global.setConfig("activemq.url", url);

//		String user = Global.getString("activemq.user", ActiveMQConnection.DEFAULT_USER);
//		String password = Global.getString("activemq.passwd", ActiveMQConnection.DEFAULT_PASSWORD);

		MQ mq = ActiveMQ.create();
		
		try {

			mq.send("ttt", Request.create().put("aaaa"));

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
