package org.giiwa.mq;

public class Response extends Request {

  int    state;
  String error;

  public void copy(Request r) {
    seq = r.seq;
    type = r.type;
    data = r.data;
    from = r.from;
  }

}
