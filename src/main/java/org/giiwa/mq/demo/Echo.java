package org.giiwa.mq.demo;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.TimeStamp;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.core.json.JSON;
import org.giiwa.core.task.Task;
import org.giiwa.framework.web.Tps;
import org.giiwa.mq.IStub;
import org.giiwa.mq.MQ;
import org.giiwa.mq.Request;

public class Echo extends IStub {

  static Log log = LogFactory.getLog(Echo.class);

  public Echo() {
    super("echo");
  }

  public Echo(String name) {
    super(name);
  }

  @Override
  public void onRequest(long seq, Request req) {
    try {
     // JSON msg = JSON.fromObject(req.data);
    //  msg.put("gottime", System.currentTimeMillis());
    //  Request r1 = new Request();
   //   r1.from = name;
  //    r1.type = req.type;
 //     r1.data = msg.toString().getBytes();
//      log.debug("seq=" + seq);
      String to = req.from;
      MQ.send(to, req);
//      r1.seq = 0;
//      MQ.send(req.from, r1);
      
      Tps.add(1);
    } catch (Exception e) {
      log.error(Arrays.toString(req.data), e);
    }
  }

  public static void main(String[] args) {

    Task.init(10);

    Global.setConfig("activemq.url",
        "failover:(tcp://joe.mac:61616)?timeout=3000&jms.prefetchPolicy.all=2&jms.useAsyncSend=true");
    Global.setConfig("activemq.user", X.EMPTY);
    Global.setConfig("activemq.passwd", X.EMPTY);
    Global.setConfig("activemq.group", "demo");

    MQ.init();

    Echo e = new Echo("echo");
    try {
      e.bind();
    } catch (Exception e1) {
      log.error(e1.getMessage(), e1);
    }

    System.out.println("done");

  }

}
