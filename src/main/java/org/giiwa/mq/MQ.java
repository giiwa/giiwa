package org.giiwa.mq;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.jms.JMSException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Config;
import org.giiwa.core.conf.Global;
import org.giiwa.core.json.JSON;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.OpLog;
import org.giiwa.framework.web.Model;
import org.giiwa.mq.impl.*;

/**
 * the distribute message system, <br>
 * the performance: sending 1w/300ms <br>
 * recving 1w/1500ms<br>
 * 
 * @author joe
 *
 */
public abstract class MQ {

  private static Log        log   = LogFactory.getLog(MQ.class);

  static String             _node = X.EMPTY;
  private static AtomicLong seq   = new AtomicLong(0);

  /**
   * the message stub type <br>
   * TOPIC: all stub will read it <br>
   * QUEUE: only one will read it
   * 
   * @author joe
   *
   */
  public static enum Mode {
    TOPIC, QUEUE
  };

  private static MQ mq = null;

  /**
   * initialize the MQ with the configuration in DB
   * 
   * @return true if success or false if failed.
   */
  public static boolean init() {
    if (mq == null) {
      _node = Model.node();
      String type = Global.getString("mq.type", X.EMPTY);
      if (X.isSame(type, "activemq")) {
        mq = ActiveMQ.create();
      } else if (X.isSame(type, "rabbitmq")) {
        mq = RabbitMQ.create();
      }
    }
    return mq != null;
  }

  /**
   * initialize the MQ with the node and url, the default is using ActiveMQ
   * 
   * @param node
   *          the local node name, MUST unique
   * @param url
   *          the activemq URL
   * @return true if success or false if failed.
   */
  public static boolean init(String node, String group, String url) {
    Global.setConfig("mq.type", "activemq");
    Global.setConfig("activemq.url", url);
    Config.getConf().setProperty("node.name", node);
    Global.setConfig("mq.group", group);
    MQ._node = node;
    return init();
  }

  /**
   * listen on the name
   * 
   * @param name
   *          the service name
   * @param stub
   *          the Stub object
   * @param mode
   *          the bind mode (topic, queue)
   * @throws JMSException
   *           the Exception
   */
  public static void bind(String name, IStub stub, Mode mode) throws Exception {
    if (mq == null) {
      OpLog.warn(org.giiwa.app.web.admin.mq.class, "bind",
          "failed bind, [" + name + "], stub=" + stub.getClass().toString() + ", mode=" + mode, null, null);

      throw new Exception("MQ not init yet");
    } else {
      mq._bind(name, stub, mode);
    }
  }

  public static void bind(String name, IStub stub) throws Exception {
    bind(name, stub, Mode.QUEUE);
  }

  protected abstract void _bind(String name, IStub stub, Mode mode) throws Exception;

  static AtomicInteger caller    = new AtomicInteger(0);
  static AtomicLong    totalSent = new AtomicLong(0);
  static AtomicLong    totalGot  = new AtomicLong(0);

  protected static void process(final String stub, final org.giiwa.framework.bean.Request req, final IStub cb) {

    totalGot.incrementAndGet();

    caller.incrementAndGet();
    Task.create(new Runnable() {

      @Override
      public void run() {

        caller.decrementAndGet();
        try {

          Request r1 = new Request();
          r1.seq = req.readLong();
          int len = req.readInt();
          r1.from = null;
          if (len > 0) {
            byte[] ff = req.readBytes(len);
            r1.from = new String(ff);
          }
          r1.type = req.readInt();
          len = req.readInt();
          r1.data = null;
          if (len > 0) {
            r1.data = req.readBytes(len);
          }
          if (log.isDebugEnabled())
            log.debug("got a message: from=" + r1.from + ", len=" + len);

          if (r1.type > 0) {
            if (Logger.isEnabled())
              Logger.log(r1.seq, "got", stub, r1);
          }

          try {
            cb.onRequest(r1.seq, r1);
          } catch (Throwable e1) {
            if (Logger.isEnabled())
              Logger.log(r1.seq, "error", stub, r1);
          }
        } catch (Exception e) {
          log.error(e.getMessage(), e);
        }

      }
    }).schedule(0);

  }

  protected abstract long _topic(long seq, String to, Request req) throws Exception;

  /**
   * broadcast the message as "topic" to all "dest:to", and return immediately
   * 
   * @param to
   *          the destination topic
   * @param message
   *          the message
   * @param from
   *          the source queue
   * @param type
   *          the message type
   * @return the sequence of the message
   * @throws Exception
   *           the Exception
   */

  public static long topic(String to, Request req) throws Exception {
    if (mq == null) {
      throw new Exception("MQ not init yet");
    }

    long s1 = seq.incrementAndGet();
    if (Logger.isEnabled())
      Logger.log(s1, "send", to, req);
    mq._topic(s1, to, req);
    return s1;
  }

  protected abstract long _send(long seq, String to, Request req) throws Exception;

  /**
   * send the message and return immediately
   * 
   * @param to
   *          the destination queue name
   * @param from
   *          the source queue
   * @param type
   *          the message type
   * @param message
   *          the message
   * @return the sequence of the payload
   * @throws Exception
   *           the Exception
   */
  public static long send(String to, Request req) throws Exception {
    long s1 = req.seq;
    if (s1 < 0) {
      s1 = seq.incrementAndGet();
      req.seq = s1;
    }
    return send(s1, to, req);
  }

  /**
   * send the message with the seq
   * 
   * @param seq
   * @param to
   * @param req
   * @return
   * @throws Exception
   */
  public static long send(long seq, String to, Request req) throws Exception {
    totalSent.incrementAndGet();
    if (Logger.isEnabled())
      Logger.log(seq, "send", to, req);

    mq._send(seq, to, req);
    return seq;
  }

  public static void log(JSON p) {
    try {
      Request r = new Request();
      r.from = _node;
      r.type = 0;
      r.setBody(p.toString().getBytes());
      mq._send(0, "logger", r);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  public static void logger(boolean log) {
    Logger.logger(log);
  }

}
