
package org.giiwa.core.dfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.app.task.DiskHeartbeat;
import org.giiwa.core.base.IOUtil;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Config;
import org.giiwa.core.json.JSON;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.AttributeKey;

public class FileServer implements IRequestHandler {

	private static Log log = LogFactory.getLog(FileServer.class);

	public static FileServer inst = new FileServer();

	static final int BUFFER_SIZE = 1024 * 32;
	public static int PORT = 9099;

	public static final byte CMD_INFO = 1;
	public static final byte CMD_GET = 2;
	public static final byte CMD_PUT = 3;
	public static final byte CMD_DELETE = 4;
	public static final byte CMD_LIST = 5;
	public static final byte CMD_MKDIRS = 6;
	public static final byte CMD_MOVE = 7;

	EventLoopGroup group = new NioEventLoopGroup();

	public void stop() throws InterruptedException {
		group.shutdownGracefully().sync();
	}

	public void start() throws Exception {

		PORT = Config.getConf().getInt("dfile.port", 9099);

		ServerBootstrap b = new ServerBootstrap();

		b.group(group);
		b.channel(NioServerSocketChannel.class);

		b.childHandler(new ChannelInitializer<SocketChannel>() {
			@Override
			public void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(new DFileHandler(FileServer.this, ""));
			}
		});

		b.bind(PORT).sync();
		DiskHeartbeat.inst.schedule(0);

		log.info("fileserver started.");

	}

	public void process(Request in, Channel ch) {

		byte cmd = in.readByte();
		// System.out.println("cmd=" + cmd);

		switch (cmd) {
		case CMD_DELETE: {
			String path = in.readString();
			String filename = in.readString();

			File f = new File(path + "/" + filename);

			Response out = Response.create(in.seq);
			try {
				IOUtil.delete(f);
				out.writeByte((byte) 1);
			} catch (Exception e) {
				out.writeByte((byte) 0);
			}
			out.send(ch);
			break;
		}
		case CMD_INFO: {
			String path = in.readString();
			String filename = in.readString();
			File f = new File(path + "/" + filename);

			JSON jo = JSON.create();
			jo.append("e", f.exists() ? 1 : 0);
			jo.append("f", f.isFile() ? 1 : 0);
			jo.append("l", f.length());
			jo.append("u", f.lastModified());

			Response out = Response.create(in.seq);
			out.writeString(jo.toString());
			out.send(ch);

			break;

		}
		case CMD_LIST: {
			String path = in.readString();
			String filename = in.readString();

			File f = new File(path + "/" + filename);
			File[] ff = f.listFiles();

			List<JSON> l1 = JSON.createList();
			for (File f1 : ff) {
				JSON jo = JSON.create();
				jo.append("name", f1.getName());
				jo.append("e", f1.exists() ? 1 : 0);
				jo.append("f", f1.isFile() ? 1 : 0);
				jo.append("l", f1.length());
				jo.append("u", f1.lastModified());
				l1.add(jo);
			}
			JSON jo = JSON.create().append("list", l1);

			Response out = Response.create(in.seq);
			out.writeString(jo.toString());
			out.send(ch);

			break;

		}
		case CMD_MKDIRS: {
			String path = in.readString();
			String filename = in.readString();

			Response out = Response.create(in.seq);

			File f = new File(path + "/" + filename);
			if (!f.exists()) {
				if (f.mkdirs()) {
					out.writeByte((byte) 1);
				} else {
					out.writeByte((byte) 0);
				}
			} else {
				out.writeByte((byte) 1);
			}
			out.send(ch);

			break;
		}

		case CMD_PUT: {

			String path = in.readString();
			String filename = in.readString();
			File f = new File(path + "/" + filename);
			long offset = in.readLong();
			byte[] bb = in.readBytes();

			RandomAccessFile a = null;

			Response out = Response.create(in.seq);

			try {
				if (!f.exists()) {
					f.getParentFile().mkdirs();
					f.createNewFile();
				}
				a = new RandomAccessFile(f, "rws");
				a.seek(offset);
				a.write(bb);

				out.writeLong(offset + bb.length);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				out.writeLong(-1);
			} finally {
				X.close(a);
			}
			out.send(ch);
			break;
		}
		case CMD_GET: {
			String path = in.readString();
			String filename = in.readString();
			long offset = in.readLong();
			int len = in.readInt();

			File f = new File(path + "/" + filename);
			Response out = Response.create(in.seq);

			FileInputStream f1 = null;
			try {
				f1 = new FileInputStream(f);
				f1.skip(offset);

				byte[] bb = new byte[Math.min(len, f1.available())];
				f1.read(bb);

				out.writeBytes(bb);

			} catch (Exception e) {
				log.error(e.getMessage(), e);
				out.writeInt(0);
			} finally {
				X.close(f1);
			}

			out.send(ch);

			break;
		}
		case CMD_MOVE: {
			String path = in.readString();
			String filename = in.readString();
			String path2 = in.readString();
			String filename2 = in.readString();

			File f1 = new File(path, filename);
			File f2 = new File(path2, filename2);

			Response out = Response.create(in.seq);
			if (f1.renameTo(f2)) {
				out.writeByte((byte) 1);
			} else {
				out.writeByte((byte) 0);
			}
			out.send(ch);

			break;
		}
		default: {
			Response out = Response.create(in.seq);
			out.writeString("unknown cmd");
			out.send(ch);
		}

		}

	}

	static class DFileHandler extends ChannelInboundHandlerAdapter {

		IRequestHandler handler;
		String name;

		DFileHandler(IRequestHandler h, String name) {
			this.handler = h;
			this.name = name;
		}

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			log.debug("client connected: " + ctx.channel().remoteAddress());
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			super.channelInactive(ctx);
			log.debug("client closed: " + ctx.channel().remoteAddress());
			handler.closed(name);
		}

		@SuppressWarnings("deprecation")
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

			AttributeKey<Integer> lenkey = AttributeKey.valueOf("len");
			Integer len = ctx.attr(lenkey).get();

			AttributeKey<ByteBuf> bufkey = AttributeKey.valueOf("buf");
			ByteBuf buf = ctx.attr(bufkey).get();

			ByteBuf m = (ByteBuf) msg;

			if (buf == null) {
				buf = Unpooled.buffer(BUFFER_SIZE);
				ctx.attr(bufkey).set(buf);
			}

			buf.writeBytes(m);

			while (len == null || buf.readableBytes() >= len) {

				if (len == null) {
					if (buf.readableBytes() < 4) {
						return;
					}
					len = buf.readInt();
					ctx.attr(lenkey).set(len);
				}

				if (buf.readableBytes() >= len) {

					log.debug(len + "<= " + ctx.channel().remoteAddress());

					ByteBuf b = Unpooled.buffer(len);
					buf.readBytes(b, len);
					Request r = Request.create(b);
					handler.process(r, ctx.channel());

					len = null;
					ctx.attr(lenkey).remove();
				}
			}

		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			ctx.close();
			log.error(ctx, cause);
		}

		@Override
		public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
			ctx.flush();
		}

	}

	static class Request {
		ByteBuf in;
		long seq;

		static Request create(ByteBuf b) {
			Request r = new Request();
			r.in = b;
			r.seq = b.readLong();
			return r;
		}

		String readString() {
			short l = in.readShort();
			byte[] bb = new byte[l];
			in.readBytes(bb);
			return new String(bb);
		}

		long readLong() {
			return in.readLong();
		}

		byte readByte() {
			return in.readByte();
		}

		int readInt() {
			return in.readInt();
		}

		byte[] readBytes() {
			int s = in.readInt();
			if (s == 0)
				return null;

			byte[] bb = new byte[s];
			in.readBytes(bb);
			return bb;
		}

	}

	static class Response {

		long seq;
		ByteBuf out;

		static Response create(long seq) {
			Response r = new Response();
			r.out = PooledByteBufAllocator.DEFAULT.buffer();
			r.out.writeLong(seq);
			r.seq = seq;
			return r;
		}

		void writeString(String s) {

			byte[] bb = s.getBytes();
			short l = (short) bb.length;
			out.writeShort(l);
			out.writeBytes(bb);

		}

		void writeLong(long s) {
			out.writeLong(s);
		}

		void writeInt(int s) {
			out.writeInt(s);
		}

		void writeBytes(byte[] bb) {

			int l = bb == null ? 0 : bb.length;
			out.writeInt(l);
			if (l > 0) {
				out.writeBytes(bb);
			}

		}

		void writeBytes(byte[] bb, int len) {

			out.writeInt(len);
			out.writeBytes(bb, 0, len);

		}

		void writeByte(byte b) {
			out.writeByte(b);
		}

		void send(Channel ch) {

			ByteBuf b = Unpooled.buffer(BUFFER_SIZE);

			byte[] bb = new byte[out.readableBytes()];
			out.readBytes(bb);

			b.writeInt(bb.length);
			b.writeBytes(bb);

			synchronized (ch) {
				ch.write(b);
				ch.flush();
			}

			log.debug(bb.length + "=>" + ch.remoteAddress());

		}

	}

	public static void main(String[] args) {
		try {
			new FileServer().start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void closed(String name) {
		// do nothing
	}

}
