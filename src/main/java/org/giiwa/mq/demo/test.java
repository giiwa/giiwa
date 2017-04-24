package org.giiwa.mq.demo;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import org.giiwa.core.bean.TimeStamp;
import org.giiwa.core.conf.Config;
import org.giiwa.core.json.JSON;
import org.giiwa.core.task.Task;
import org.giiwa.mq.IStub;
import org.giiwa.mq.MQ;
import org.giiwa.mq.Request;

public class test {

  public static void main(String[] args) {

    Config.init();

    String url = "failover:(tcp://joe.mac:61616)?timeout=3000&jms.prefetchPolicy.all=2&jms.useAsyncSend=true";

    Task.init(200);

    if (MQ.init("test", "demo", url)) {
      MQ.logger(true);

      TimeStamp t2 = TimeStamp.create();
      int n = 0;
      int c = 0;
      Tester[] t = new Tester[c];
      for (int i = 0; i < t.length; i++) {
        t[i] = new Tester("t" + i, n);
        try {
          t[i].bind();
        } catch (Exception e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }
        // "t" + i
        t[i].send("echo", JSON.create());
      }
      if (t2.past() > 0)
        System.out.println("sent: " + c * n + ", cost: " + t2.past() + "ms, send TPS: " + (c * n * 1000 / t2.past()));

      synchronized (t) {
        int i = 0;
        try {
          for (Tester t1 : t) {
            while (!t1.isFinished() && i < 100) {
              t.wait(1000);
              i++;
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      for (Tester t1 : t) {
        t1.println();
      }

      if (t2.past() > 0)
        System.out.println("transation TPS: " + (n * c) * 1000 / t2.past());
    }

    System.out.println("done");

    byte[] bb = new byte[] { 17, 123, 10, 32, 32, 32, 34, 97, 34, 32, 58, 32, 34, 97, 34, 10, 125, 10 };
    String s = new String(bb).trim();
    // JSON j1 = JSON.fromObject(bb);
    JSON j2 = JSON.fromObject(s);

    System.out.println(s);
    System.out.println(Arrays.toString(s.trim().getBytes()));

    System.exit(0);

  }

  public static class Tester extends IStub {

    int        n;
    AtomicLong seq     = new AtomicLong();
    AtomicLong back    = new AtomicLong();
    AtomicLong total   = new AtomicLong();

    JSON       status  = JSON.create();
    JSON       msg;
    String     to;
    long       created = System.currentTimeMillis();

    public Tester(String name, int n) {
      super(name);
      this.n = n;
    }

    public void println() {
      System.out.println(status.toString());
    }

    public void send(String to, JSON msg) {
      this.msg = msg;
      this.to = to;

      long s = seq.incrementAndGet();
      msg.put("sendtime", System.currentTimeMillis());

      try {
        Request r = new Request();
        r.type = 0;
        r.data = msg.toString().getBytes();
        this.send(to, r);
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    public boolean isFinished() {
      return back.get() == n;
    }

    @Override
    public void onRequest(long seq, Request req) {
      // System.out.println("from:" + from);

      long min = status.getLong("min", Long.MAX_VALUE);
      long max = status.getLong("max", Long.MIN_VALUE);

      long t = System.currentTimeMillis() - msg.getLong("sendtime");
      total.addAndGet(t);
      if (t < min) {
        status.put("min", t);
      }
      if (t > max) {
        status.put("max", t);
      }
      status.put("total", total.get());

      back.incrementAndGet();
      status.put("aver", total.get() / back.get());

      if (this.seq.get() < n) {
        send(this.to, msg);
      }

      if (back.get() == n) {
        status.put("duration", System.currentTimeMillis() - created);
      }
    }

  }
}
