package org.giiwa.mq;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.X;
import org.giiwa.core.json.JSON;
import org.giiwa.core.task.Task;

class Logger extends Task {

  private final static LinkedBlockingQueue<JSON> queue  = new LinkedBlockingQueue<JSON>();

  private static boolean                         islog  = false;
  private static final Log                       log    = LogFactory.getLog(Logger.class);
  private static Logger                          owner  = new Logger();
  private static StatusTask                      status = owner.new StatusTask();

  private static long                            flag   = System.currentTimeMillis();

  @Override
  public String getName() {
    return "logger";
  }

  public static boolean isEnabled() {
    return islog;
  }

  @Override
  public void onExecute() {
    if (islog) {
      try {
        JSON p = queue.poll(10, TimeUnit.SECONDS);
        while (p != null) {
          MQ.log(p);
          p = queue.poll(10, TimeUnit.SECONDS);
        }
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
    }

  }

  @Override
  public void onFinish() {
    if (islog) {
      this.schedule(0);
    }
  }

  public static void logger(boolean log) {
    Logger.islog = log;
    if (islog) {
      owner.schedule(0);
      status.schedule(0);
    }
  }

  public static void log(long seq, String direction, String to, Request r) {
    if (islog) {
      try {
        JSON p = JSON.create();
        p.put("node", MQ._node);
        p.put("to1", to);
        p.put("direction", direction);
        p.put("from1", r.from);
        p.put("type", r.type);
        p.put("time1", System.currentTimeMillis());
        p.put("seq", seq);
        p.put("flag", flag);
        p.put("len", r.data == null ? 0 : r.data.length);
        p.put("message", r.type == 0 ? new String(r.data) : toString(r.data));
        queue.add(p);
      } catch (Throwable e) {
        log.error(e.getMessage(), e);
      }
    }
  }

  private static String toString(byte[] bb) {
    StringBuilder sb = new StringBuilder();
    if (bb != null) {
      for (byte b : bb) {
        sb.append(X.toHex(b));
      }
    }

    return sb.toString();
  }

  class StatusTask extends Task {

    AtomicLong seq = new AtomicLong(0);

    @Override
    public void onExecute() {
      if (!islog) {
        queue.clear();
        return;
      }

      JSON p = JSON.create();
      p.put("stub", "Java");
      p.put("node", MQ._node);
      p.put("flag", flag);
      p.put("task.pending", Task.tasksInQueue());
      p.put("task.running", Task.tasksInRunning());
      p.put("logger.queue", queue.size());
      p.put("caller.queue", MQ.caller.get());
      p.put("total.sent", MQ.totalSent.get());
      p.put("total.got", MQ.totalGot.get());

      String s = p.toString();
      MQ.log(JSON.create().append("message", s).append("time1", System.currentTimeMillis()).append("node", MQ._node)
          .append("len", s.length()).append("flag", flag).append("type", 0).append("seq", seq.incrementAndGet()));
    }

    public void onFinish() {
      this.schedule(10 * X.AMINUTE);
    }

    @Override
    public String getName() {
      return "logger.status";
    }

  }
}
