package org.giiwa.mq;

import javax.jms.DeliveryMode;

import org.giiwa.core.bean.X;

public class Request {

  public long   seq        = -1;
  public int    type       = 0;

  public String from;
  public int    priotiry   = 1;
  public int    ttl        = (int) X.AMINUTE;
  public int    persistent = DeliveryMode.NON_PERSISTENT;
  public byte[] data;

  public void setBody(byte[] bb) {
    data = bb;
  }
}
