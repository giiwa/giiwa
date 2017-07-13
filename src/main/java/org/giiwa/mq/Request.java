package org.giiwa.mq;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

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

  public DataInputStream getInput() {
    return new DataInputStream(new ByteArrayInputStream(data));
  }

  public DataOutputStream getOutput() {
    final ByteArrayOutputStream bb = new ByteArrayOutputStream();
    return new DataOutputStream(bb) {

      @Override
      public void close() throws IOException {
        super.close();
        data = bb.toByteArray();
      }

    };
  }

  public void setBody(byte[] bb) {
    data = bb;
  }

  public static Request create() {
    return new Request();
  }

  public Request seq(long seq) {
    this.seq = seq;
    return this;
  }

  public Request type(int type) {
    this.type = type;
    return this;
  }

  public Request from(String from) {
    this.from = from;
    return this;
  }

  public Request data(byte[] data) {
    this.data = data;
    return this;
  }

}
