package org.giiwa.net.mq;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.giiwa.conf.Config;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.json.JSON;
import org.giiwa.net.mq.MQ.Mode;
import org.giiwa.net.mq.MQ.Request;
import org.giiwa.task.Task;
import org.junit.Test;

public class RocketMQTest {

	@Test
	public void test() {

		Config.init();

		String url = "g40:9876";

		Local._id = "1111";

		Task.init(10);

		Global.setConfig("mq.type", "rocketmq");
		Global.setConfig("rocketmq.url", url);
		MQ.init();

		try {
			{
				IStub st = new IStub("ttt") {

					@Override
					public void onRequest(long seq, Request req) {
						try {
							System.out.println("got.1 [" + req.seq + ": " + req.get() + "]");
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

				};

				st.bindAs(Mode.BOTH);
			}

			{
				IStub st = new IStub("ttt") {

					@Override
					public void onRequest(long seq, Request req) {
						try {
							System.out.println("got.2 [" + req + ": " + req.get() + "]");
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

				};

				st.bindAs(Mode.BOTH);
			}

			for (int i = 0; i < 100; i++) {
				Request a = Request.create().put(JSON.create().append("a", 1).append("b", "222222"));
				a.from = "a";
//				a.cmd = "kill";

				MQ.send("ttt", a);
				Thread.sleep(1000);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Test
	public void test1() {

		try {

			DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("demo1");

			// Uncomment the following line while debugging, namesrvAddr should be set to
			// your local address
			consumer.setNamesrvAddr("g40:9876");

			consumer.subscribe("test1", "a");
//			consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
			// wrong time format 2017_0422_221800
//			consumer.setConsumeTimestamp("20181109221800");
			consumer.registerMessageListener(new MessageListenerConcurrently() {

				@Override
				public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
						ConsumeConcurrentlyContext context) {
					System.out.printf("%s Receive New Messages: %s %n", Thread.currentThread().getName(), msgs);
					return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
				}
			});
			consumer.start();
			System.out.printf("Consumer Started.%n");

			// producer
			DefaultMQProducer producer = new DefaultMQProducer("demo1");

			// Uncomment the following line while debugging, namesrvAddr should be set to
			// your local address
			producer.setNamesrvAddr("g40:9876");

			producer.start();
			for (int i = 0; i < 128; i++) {
				try {
					Message msg = new Message("test1", "a", "OrderID188",
							"Hello world".getBytes(StandardCharsets.UTF_8));
					SendResult sendResult = producer.send(msg);
					System.out.printf("%s%n", sendResult);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			producer.shutdown();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testBytes() {

		try {
			byte[] body = null;
			{
				Request r = Request.create().put(JSON.create().append("a", 1).append("b", "222222"));
				r.seq = System.currentTimeMillis();
				r.from = "a";

//		a.cmd = "kill";

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				DataOutputStream m = new DataOutputStream(out);

				m.writeLong(r.seq);
				m.writeByte(r.ver);
				m.writeLong(r.tt);
				m.writeInt(r.type);
				m.writeInt(r.priority);

				byte[] ff = (r.from == null) ? null : r.from.getBytes();
				if (ff == null) {
					m.writeInt(0);
				} else {
					System.out.println("len=" + ff.length);
					m.writeInt(ff.length);
					m.write(ff);
				}

				ff = (r.cmd == null) ? null : r.cmd.getBytes();
				if (ff == null) {
					m.writeInt(0);
				} else {
					System.out.println("len=" + ff.length);
					m.writeInt(ff.length);
					m.write(ff);
				}

				if (r.data == null) {
					m.writeInt(0);
				} else {
					System.out.println("len=" + r.data.length);
					m.writeInt(r.data.length);
					m.write(r.data);
				}

				m.close();
				body = out.toByteArray();

				System.out.println("r=" + r);
			}

			System.out.println("body.len=" + body.length);

			{
				Request r = new Request();

				DataInputStream m1 = new DataInputStream(new ByteArrayInputStream(body));

				r.seq = m1.readLong();
				r.ver = m1.readByte();
				r.tt = m1.readLong();
				r.type = m1.readInt();
				r.priority = m1.readInt();

				int len = m1.readInt();
				if (len > 0) {
					System.out.println("len=" + len);
					byte[] bb = new byte[len];
					m1.read(bb);
					r.from = new String(bb);
				}

				len = m1.readInt();
				if (len > 0) {
					System.out.println("len=" + len);
					byte[] bb = new byte[len];
					m1.read(bb);
					r.cmd = new String(bb);
				}

				len = m1.readInt();
				if (len > 0) {
					System.out.println("len=" + len);
					r.data = new byte[len];
					m1.read(r.data);
				}

				System.out.println(r);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
