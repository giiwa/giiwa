/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.net.mq;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.giiwa.bean.GLog;
import org.giiwa.conf.Global;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;

class MQTT extends MQ {

	private static Log log = LogFactory.getLog(MQTT.class);

//	private String group = X.EMPTY;
	private MqttClient conn;

	/**
	 * Creates the.
	 *
	 * @return the mq
	 */
	public static MQ create() {

		MQTT m = new MQTT();

		String broker = Global.getString("mqtt.broker", "tcp://broker.emqx.io:1883");
		String clientid = Global.getString("mqtt.clientid", "demo_client");

		String user = Global.getString("mqtt.user", null);
		String password = Global.getString("mqtt.passwd", null);

		try {
			if (broker.startsWith("ssl://")) {

			} else {
				m.conn = new MqttClient(broker, clientid);
				MqttConnectOptions options = new MqttConnectOptions();

				if (!X.isEmpty(user)) {
					options.setUserName(user);
					options.setPassword(password.toCharArray());
				}

				// Set whether to clear the session
				options.setCleanSession(true);

				// Set the heartbeat interval in seconds
				options.setKeepAliveInterval(300);

				// Set the connection timeout in seconds
				options.setConnectionTimeout(30);

				// Set whether to automatically reconnect
				options.setAutomaticReconnect(true);

				m.conn.connect(options);
				if (m.conn.isConnected()) {
					m.conn.setCallback(new MqttCallback() {
						public void messageArrived(String topic, MqttMessage message) throws Exception {
							System.out.println("topic: " + topic);
							System.out.println("qos: " + message.getQos());
							System.out.println("message content: " + new String(message.getPayload()));
						}

						public void connectionLost(Throwable cause) {
							System.out.println("connectionLost: " + cause.getMessage());
						}

						public void deliveryComplete(IMqttDeliveryToken token) {
							System.out.println("deliveryComplete: " + token.isComplete());
						}
					});
				}
			}
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			GLog.applog.error("admin.mq", "startup", "failed MQTT with [" + broker + "]", e, null, null);
		}

		return m;
	}

	private transient List<WeakReference<R>> cached = new ArrayList<WeakReference<R>>();

	/**
	 * QueueTask
	 * 
	 * @author joe
	 * 
	 */
	public class R implements IMqttMessageListener {

		public String name;
		IStub cb;
		TimeStamp t = TimeStamp.create();
		int count = 0;

		@Override
		public String toString() {
			return "R [name=" + name + "]";
		}

		/**
		 * Close.
		 */
		public void close() {
			if (conn != null) {
				try {
					conn.unsubscribe(name);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		}

		private R(String name, IStub cb, Mode mode) throws Exception {
			this.name = name;
			this.cb = cb;

			if (conn != null) {

				conn.subscribe(name, this);
				cached.add(new WeakReference<R>(this));

			} else {
				if (log.isDebugEnabled()) {
					log.debug("MQ not init yet!");
				}
				throw new Exception("MQ not init yet!");
			}

		}

		@Override
		public void messageArrived(String topic, MqttMessage message) throws Exception {

			// TODO

		}

	}

	@Override
	protected void _bind(String name, IStub stub, Mode mode) throws Exception {
		if (conn == null) {
			throw new JMSException("MQ not init yet");
		}

		conn.subscribe(name, 1);

		new R(name, stub, mode);
	}

	@Override
	protected long _topic(String to, MQ.Request r) throws Exception {

		// if (X.isEmpty(r.data))
		// throw new Exception("message can not be empty");

		return _send(to, r);
	}

	@Override
	protected long _send(String to, MQ.Request r) throws Exception {

		// if (X.isEmpty(r.data))
		// throw new Exception("message can not be empty");

		if (conn == null) {
			throw new Exception("MQ not init yet");
		}

		MqttMessage message = new MqttMessage(r.packet());
		message.setQos(1);
		conn.publish(to, message);

		return 1;

	}

	@Override
	protected void _unbind(IStub stub) throws Exception {
		// find R
		for (int i = cached.size() - 1; i >= 0; i--) {
			WeakReference<R> w = cached.get(i);

			if (w == null) {
				cached.remove(i);
			} else {
				R r = w.get();
				if (r == null || r.cb == null) {
					cached.remove(i);
				} else if (r.cb == stub) {
					r.close();
					cached.remove(i);
				}
			}
		}
	}

	@Override
	protected void _stop() {
		if (conn != null) {
			try {
				conn.disconnect();
				conn.close();
				conn = null;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	@Override
	public void destroy(String name, Mode mode) {
		if (conn != null) {

			try {

				// TODO

			} catch (Exception e) {
				log.error("destory dest, name=" + name + ", mode=" + mode, e);
			}
		}
	}

}
