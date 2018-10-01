package org.giiwa.core.dfile;

import org.giiwa.core.dfile.FileServer.Request;

import io.netty.channel.Channel;

public interface IRequestHandler {

	void process(Request r, Channel ch);

	void closed(String name);

}
