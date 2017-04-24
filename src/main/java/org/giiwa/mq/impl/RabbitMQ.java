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
package org.giiwa.mq.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.TimeStamp;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.framework.bean.OpLog;
import org.giiwa.framework.bean.Request;
import org.giiwa.framework.bean.Response;
import org.giiwa.mq.IStub;
import org.giiwa.mq.MQ;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

// TODO: Auto-generated Javadoc
public class RabbitMQ extends MQ {

  private static Log log = LogFactory.getLog(RabbitMQ.class);

  @Override
  protected void _bind(String name, IStub stub, Mode mode) throws Exception {
    Channel ch = null;
    try {
      ch = connection.createChannel();
      Receiver r = new Receiver(ch, name, stub, mode);
      OpLog.info(org.giiwa.app.web.admin.mq.class, "bind",
          "[" + name + "], stub=" + stub.getClass().toString() + ", mode=" + mode, null, null);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      OpLog.warn(org.giiwa.app.web.admin.mq.class, "bind",
          "[" + name + "] failed, error=" + e.getMessage() + ", stub=" + stub.getClass().toString() + ", mode=" + mode,
          null, null);
      if (ch != null) {
        try {
          ch.close();
        } catch (Exception e1) {
        }
      }
    }

  }

  @Override
  protected long _topic(long seq, String to, org.giiwa.mq.Request r) throws Exception {
    // TODO
    throw new Exception("not support");
  }

  @Override
  protected long _send(long seq, String to, org.giiwa.mq.Request r) throws Exception {

    if (r.data == null)
      return -1;

    if (connection == null) {
      return -1;
    }

    try {

      /**
       * get the message producer by destination name
       */
      if (channel != null) {
        Response resp = new Response();

        // Response resp = new Response();
        resp.writeLong(seq);
        resp.writeString(to == null ? X.EMPTY : to);
        resp.writeString(r.from == null ? X.EMPTY : r.from);
        resp.writeInt(r.type);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(os);
        out.writeObject(r.data);
        out.close();
        byte[] ss = os.toByteArray();
        resp.writeInt(ss.length);
        resp.writeBytes(os.toByteArray());

        channel.queueDeclare(to, false, false, false, null);
        channel.basicPublish("", to, null, resp.getBytes());

        log.debug("Sending:" + to + ", " + r.data);

        return seq;
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    return -1;
  }

  private static Channel channel;
  private Connection     connection;

  /**
   * Creates the.
   *
   * @return the rabbit mq
   */
  public static RabbitMQ create() {
    RabbitMQ m = new RabbitMQ();
    String url = Global.getString("rabbitmq.url", X.EMPTY);

    try {

      ConnectionFactory factory = new ConnectionFactory();
      factory.setUri(url);
      factory.setAutomaticRecoveryEnabled(true);
      factory.setNetworkRecoveryInterval(10000);

      ExecutorService es = Executors.newFixedThreadPool(Global.getInt("rabbitmq.threads", 10));

      m.connection = factory.newConnection(es);
      channel = m.connection.createChannel();

      OpLog.info(org.giiwa.app.web.admin.mq.class, "startup", "connected RabbitMQ with [" + url + "]", null, null);

    } catch (Throwable e) {
      log.error(e.getMessage(), e);
      OpLog.warn(org.giiwa.app.web.admin.mq.class, "startup", "failed RabbitMQ with [" + url + "]", null, null);
    }
    return m;
  }

  public class Receiver extends DefaultConsumer {
    String    name;
    IStub     cb;
    TimeStamp t     = TimeStamp.create();
    int       count = 0;

    /**
     * Close.
     */
    public void close() {
      try {
        this.getChannel().close();
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
    }

    private Receiver(Channel ch, String name, IStub cb, Mode mode) {
      super(ch);

      this.cb = cb;

      if (connection != null) {
        try {

          ch.queueDeclare(name, false, false, false, null);
          ch.basicConsume(name, true, this);

        } catch (Exception e) {
          log.error(e.getMessage(), e);
        }

      } else {
        log.warn("no mq configured!");
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.rabbitmq.client.DefaultConsumer#handleDelivery(java.lang.String,
     * com.rabbitmq.client.Envelope, com.rabbitmq.client.AMQP.BasicProperties,
     * byte[])
     */
    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
        throws IOException {

      Request req = new Request(body, 0);

      count++;

      process(name, req, cb);

      if (count % 10000 == 0) {
        System.out.println("process the 10000 messages, cost " + t.reset() + "ms");
      }

    }
  }
}
