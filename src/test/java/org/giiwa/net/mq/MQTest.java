package org.giiwa.net.mq;

import java.util.Arrays;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.giiwa.conf.Global;
import org.giiwa.dao.X;
import org.giiwa.net.mq.MQ.Request;
import org.junit.Test;

public class MQTest {

	@Test
	public void test() {

		String type = "mq/=";

		System.out.println(X.asList(type.getBytes(), e -> Long.toHexString(X.toLong(e))));
		
//		MqttClient client = new MqttClient(broker, clientId);
//		MqttConnectOptions options = new MqttConnectOptions();
//		client.connect(options);
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
