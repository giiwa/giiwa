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

import java.util.HashMap;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.TimeStamp;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.framework.bean.OpLog;
import org.giiwa.framework.bean.Request;
import org.giiwa.mq.IStub;
import org.giiwa.mq.MQ;

// TODO: Auto-generated Javadoc
public final class ActiveMQ extends MQ {

  private static Log log   = LogFactory.getLog(ActiveMQ.class);

  private String     group = X.EMPTY;
  private Session    session;

  /**
   * Creates the.
   *
   * @return the mq
   */
  public static MQ create() {
    ActiveMQ m = new ActiveMQ();

    String url = Global.getString("activemq.url", ActiveMQConnection.DEFAULT_BROKER_URL);
    String user = Global.getString("activemq.user", ActiveMQConnection.DEFAULT_USER);
    String password = Global.getString("activemq.passwd", ActiveMQConnection.DEFAULT_PASSWORD);

    m.group = Global.getString("mq.group", X.EMPTY);
    if (!m.group.endsWith(".")) {
      m.group += ".";
    }

    try {
      ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(user, password, url);

      Connection connection = factory.createConnection();
      connection.start();

      m.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

      OpLog.info(org.giiwa.app.web.admin.mq.class, "startup", "connected ActiveMQ with [" + url + "]", null, null);

    } catch (Throwable e) {
      log.error(e.getMessage(), e);
      // e.printStackTrace();
      OpLog.warn(org.giiwa.app.web.admin.mq.class, "startup", "failed ActiveMQ with [" + url + "]", null, null);
    }

    return m;
  }

  /**
   * QueueTask
   * 
   * @author joe
   * 
   */
  public class R implements MessageListener {
    public String   name;
    IStub           cb;
    MessageConsumer consumer;
    TimeStamp       t     = TimeStamp.create();
    int             count = 0;

    /**
     * Close.
     */
    public void close() {
      if (consumer != null) {
        try {
          consumer.close();
        } catch (JMSException e) {
          log.error(e.getMessage(), e);
        }
      }
    }

    private R(String name, IStub cb, Mode mode) throws JMSException {
      this.name = name;
      this.cb = cb;

      if (session != null) {
        Destination dest = null;
        if (mode == Mode.QUEUE) {
          dest = new ActiveMQQueue(group + name);
        } else {
          dest = new ActiveMQTopic(group + name);
        }

        consumer = session.createConsumer(dest);
        consumer.setMessageListener(this);

      } else {
        log.warn("MQ not init yet!");
        throw new JMSException("MQ not init yet!");
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
     */
    @Override
    public void onMessage(Message m) {
      try {
        // System.out.println("got a message.., " + t.reset() +
        // "ms");

        count++;
        if (m instanceof BytesMessage) {
          BytesMessage m1 = (BytesMessage) m;
          int len = (int) m1.getBodyLength();
          byte[] bb = new byte[len];
          m1.readBytes(bb);
          Request r = new Request(bb, 0);
          process(name, r, cb);
        } else {
          System.out.println(m);
        }

        if (count % 10000 == 0) {
          System.out.println("process the 10000 messages, cost " + t.reset() + "ms");
        }

      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
    }
  }

  @Override
  protected void _bind(String name, IStub stub, Mode mode) throws Exception {
    if (session == null)
      throw new JMSException("MQ not init yet");

    OpLog.info(org.giiwa.app.web.admin.mq.class, "bind",
        "[" + name + "], stub=" + stub.getClass().toString() + ", mode=" + mode, null, null);

    new R(name, stub, mode);
  }

  @Override
  protected long _topic(long seq, String to, org.giiwa.mq.Request r) throws Exception {

    if (X.isEmpty(r.data))
      throw new Exception("message can not be empty");

    if (session == null) {
      throw new Exception("MQ not init yet");
    }

    /**
     * get the message producer by destination name
     */
    MessageProducer p = getTopic(to);
    if (p == null) {
      throw new Exception("MQ not ready yet");
    }

    BytesMessage req = session.createBytesMessage();

    req.writeLong(seq);
    byte[] ff = r.from == null ? null : r.from.getBytes();
    if (ff == null) {
      req.writeInt(0);
    } else {
      req.writeInt(ff.length);
      req.writeBytes(ff);
    }
    req.writeInt(r.type);
    if (r.data == null) {
      req.writeInt(0);
    } else {
      req.writeInt(r.data.length);
      req.writeBytes(r.data);
    }

    p.send(req, r.persistent, r.priotiry, r.ttl);

    if (log.isDebugEnabled())
      log.debug("Broadcasting: " + to + ", len=" + r.data.length);

    return seq;
  }

  @Override
  protected long _send(long seq, String to, org.giiwa.mq.Request r) throws Exception {

    if (X.isEmpty(r.data))
      throw new Exception("message can not be empty");

    if (session == null) {
      throw new Exception("MQ not init yet");
    }

    /**
     * get the message producer by destination name
     */
    MessageProducer p = getQueue(to);
    if (p == null) {
      throw new Exception("MQ not ready yet");
    }

    BytesMessage req = session.createBytesMessage();

    req.writeLong(seq);
    byte[] ff = r.from == null ? null : r.from.getBytes();
    if (ff == null) {
      req.writeInt(0);
    } else {
      req.writeInt(ff.length);
      req.writeBytes(ff);
    }
    req.writeInt(r.type);
    if (r.data == null) {
      req.writeInt(0);
    } else {
      req.writeInt(r.data.length);
      req.writeBytes(r.data);
    }

    p.send(req, r.persistent, r.priotiry, r.ttl);

    if (log.isDebugEnabled())
      log.debug("Sending:" + to + ", len=" + (r.data == null ? 0 : r.data.length));

    return seq;

  }

  private MessageProducer getQueue(String name) {
    synchronized (queues) {
      if (session != null) {
        if (queues.containsKey(name)) {
          return queues.get(name);
        }

        try {
          Destination dest = new ActiveMQQueue(group + name);
          MessageProducer producer = session.createProducer(dest);
          producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
          queues.put(name, producer);

          return producer;
        } catch (Exception e) {
          log.error(name, e);
        }
      }
    }

    return null;
  }

  private MessageProducer getTopic(String name) {
    synchronized (topics) {
      if (session != null) {
        if (topics.containsKey(name)) {
          return topics.get(name);
        }

        try {
          Destination dest = new ActiveMQTopic(group + name);
          MessageProducer producer = session.createProducer(dest);
          producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
          topics.put(name, producer);

          return producer;
        } catch (Exception e) {
          log.error(name, e);
        }
      }
    }
    return null;
  }

  /**
   * queue producer cache
   */
  private Map<String, MessageProducer> queues = new HashMap<String, MessageProducer>();

  /**
   * topic producer cache
   */
  private Map<String, MessageProducer> topics = new HashMap<String, MessageProducer>();

}
