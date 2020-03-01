package org.giiwa.net.nio;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

@ChannelHandler.Sharable
public abstract class IoHandler extends ChannelInboundHandlerAdapter {

//	private static Log log = LogFactory.getLog(IoHandler.class);

	public IoHandler() {
//		System.out.println("new io");
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {

		AttributeKey<IoRequest> a = AttributeKey.valueOf("req");
		Attribute<IoRequest> at = ctx.channel().attr(a);
		IoRequest r = at.get();
		if (r != null) {
			r.release();
		}

		super.handlerRemoved(ctx);

//		System.out.println("remove");
	}

	public void channelRead(ChannelHandlerContext ctx, Object msg) {

//		log.debug("got data, client=" + ctx.channel().remoteAddress());

		ByteBuf m = (ByteBuf) msg;

		try {
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

			try {
				resp.retain();

				process(req, resp);

				req.compact();

			} finally {
				resp.release();
			}

		} finally {
			m.release();
		}

	}

	public abstract void process(IoRequest req, IoResponse resp);

}
