package org.giiwa.mq;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.core.json.JSON;
import org.giiwa.core.task.Task;
import org.giiwa.framework.web.Model;
import org.giiwa.mq.MQ.Mode;

public class Commission {

  private static Log                     log      = LogFactory.getLog(Commission.class);

  private static long                    TICKET   = Global.getInt("commission.ticket.time", 3000);
  String                                 name;
  IStub                                  stub;

  Map<String, Long>                      members  = new HashMap<String, Long>();

  String                                 leader;
  boolean                                isLeader = false;
  long                                   last     = 0;

  private static Map<String, Commission> comms    = new HashMap<String, Commission>();
  private static HBTask                  hb       = new HBTask();

  public boolean isLeader() {
    return isLeader && System.currentTimeMillis() - last < Global.getInt("commission.ticket.expired", 5000);
  }

  public boolean lostLeader() {
    return System.currentTimeMillis() - last > Global.getInt("commission.ticket.expired", 5000);
  }

  public static synchronized Commission get(String name) {
    Commission c = comms.get(name);
    if (c == null) {
      c = new Commission();
      c.name = "commission." + name;
      c.init();
      comms.put(name, c);
    }
    return c;
  }

  private void init() {
    if (stub == null) {
      stub = new IStub(name) {

        @Override
        public void onRequest(long seq, Request req) {
          JSON j = JSON.fromObject(req.data);
          process(j);
        }
      };
    }

    try {

      stub.bind(Mode.TOPIC);
      walkin();

    } catch (Exception e) {

      log.error(e.getMessage(), e);

      Task.create(new Runnable() {

        @Override
        public void run() {
          init();
        }

      }).schedule(3000);
    }
  }

  private void process(JSON req) {
    // log.debug("got=" + req);

    String name = req.getString("name");
    String m = req.getString("m");

    if (X.isSame(m, "in")) {
      // walkin
      synchronized (members) {
        members.put(name, System.currentTimeMillis());
      }
      sayhello();
    } else if (X.isSame(m, "hello")) {
      // sayhello
      synchronized (members) {
        members.put(name, System.currentTimeMillis());
      }
    } else if (X.isSame(m, "iamleader")) {
      // iamleader
      leader = name;
      last = System.currentTimeMillis();
      isLeader = X.isSame(leader, Model.node());

      promotion = 0;
      ticket = 0;
      synchronized (members) {
        members.put(name, last);
      }
    } else if (X.isSame(m, "promote")) {
      // promote
      if (X.isSame(name, Model.node())) {
        // it's me
        if (System.currentTimeMillis() - promotion < TICKET) {
          ticket++;
        } else {
          promotion = System.currentTimeMillis();
          ticket = 1;
        }

        if (ticket * 2 >= comms.size()) {
          iamleader();
        }
      }
    }
  }

  private long promotion = 0;
  private int  ticket    = 0;

  private void walkin() {
    JSON j = JSON.create();
    j.put("m", "in");
    j.put("name", Model.node());

    send(j);

    // after walkin, schedule the heart-beat
    hb.schedule(100);

  }

  private void iamleader() {
    JSON j = JSON.create();
    j.put("m", "iamleader");
    j.put("name", Model.node());

    send(j);
  }

  private void send(JSON j) {
    try {
      Request r = new Request();
      r.data = j.toString().getBytes();
      r.type = 1;
      stub.topic(name, r);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    String[] names = members.keySet().toArray(new String[members.size()]);
    for (String name : names) {
      synchronized (members) {
        Long l1 = members.get(name);
        if (l1 == null || System.currentTimeMillis() - l1 > 2 * TICKET) {
          members.remove(name);
        }
      }
    }
  }

  private void promote() {
    if (isLeader()) {
      iamleader();
      return;
    }

    JSON j = JSON.create();
    j.put("m", "promote");
    j.put("name", Model.node());

    /**
     * get first one
     */
    if (members.isEmpty()) {
      synchronized (members) {
        members.put(Model.node(), System.currentTimeMillis());
      }
    }

    List<String> l1 = new ArrayList<String>(members.keySet());
    Collections.sort(l1);
    j.put("leader", l1.get(0));

    send(j);
  }

  private void sayhello() {
    if (isLeader()) {
      iamleader();
      return;
    }

    JSON j = JSON.create();
    j.put("m", "hello");
    j.put("name", Model.node());

    send(j);
  }

  static class HBTask extends Task {

    @Override
    public void onExecute() {
      Commission[] cc = comms.values().toArray(new Commission[comms.size()]);
      if (cc != null && cc.length > 0) {
        for (Commission c : cc) {
          c.sayhello();

          if (c.lostLeader()) {
            c.promote();
          }
        }
      }
    }

    public void onFinish() {
      if (TICKET > 0 && comms.size() > 0) {
        this.schedule(TICKET);
      }
    }
  }

  public String getLeader() {
    return leader;
  }

}
