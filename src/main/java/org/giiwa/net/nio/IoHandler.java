package org.giiwa.net.nio;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

public abstract class IoHandler extends ChannelInboundHandlerAdapter {

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {

		System.out.println(ctx.channel().remoteAddress());

		ByteBuf m = (ByteBuf) msg;

		AttributeKey<IoRequest> a = AttributeKey.valueOf("req");
		Attribute<IoRequest> at = ctx.channel().attr(a);

		IoRequest req = at.get();
		if (req == null) {
			req = IoRequest.create(m);
			at.set(req);
		} else {
			req.put(m);
		}

		IoResponse resp = IoResponse.create(ctx.channel());
		process(req, resp);
		resp.release();

	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
		e.printStackTrace();
		ctx.close();
	}

	public abstract void process(IoRequest req, IoResponse resp);

}
