package org.giiwa.core.dfile;

import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.giiwa.core.base.IOUtil;
import org.giiwa.core.bean.X;
import org.giiwa.core.dfile.FileServer.DFileHandler;
import org.giiwa.core.dfile.FileServer.Request;
import org.giiwa.core.dfile.FileServer.Response;
import org.giiwa.core.json.JSON;
import org.giiwa.framework.bean.Disk;
import org.giiwa.framework.bean.Node;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class FileClient implements IRequestHandler {

	private Map<Long, Request[]> pending = new HashMap<Long, Request[]>();

	private AtomicLong seq = new AtomicLong(0);

	private String name;
	private String host;
	private int port;
	private Channel ch;

	private static Map<String, FileClient> cached = new HashMap<String, FileClient>();

	public static FileClient get(String ip, int port) throws InterruptedException {

		String[] ss = X.split(ip, "[;,]");
		for (String s : ss) {
			String name = s + ":" + port;
			FileClient c = cached.get(name);
			if (c != null) {
				return c;
			}
		}

		for (String s : ss) {
			String name = s + ":" + port;
			FileClient c = create(s, port);
			if (c != null) {
				c.name = name;
				cached.put(name, c);
				return c;
			}
		}

		return new FileClient();
	}

	private static FileClient create(String host, int port) throws InterruptedException {
		FileClient c = new FileClient();
		c.host = host;
		c.port = port;
		if (c.connect()) {

			synchronized (c) {
				if (c.ch == null) {
					// System.out.println("waiting ...");
					c.wait(10000);
				}
			}

			return c;
		}
		return null;
	}

	private boolean connect() throws InterruptedException {

		EventLoopGroup workerGroup = new NioEventLoopGroup();

		Bootstrap b = new Bootstrap();
		b.group(workerGroup);
		b.channel(NioSocketChannel.class);
		b.option(ChannelOption.SO_KEEPALIVE, true);

		b.handler(new ChannelInitializer<SocketChannel>() {
			@Override
			public void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(new DFileHandler(FileClient.this, name));
			}
		});

		ChannelFuture f = b.connect(host, port).sync();
		ch = f.channel();

		synchronized (this) {
			this.notifyAll();
		}

		return true;
	}

	public boolean delete(String path, String filename) {
		if (ch == null)
			return false;

		Response r = Response.create(seq.incrementAndGet());
		try {

			r.writeByte(FileServer.CMD_DELETE);
			r.writeString(path);
			r.writeString(filename);

			Request[] aa = new Request[1];
			pending.put(r.seq, aa);
			synchronized (aa) {
				r.send(ch);
				if (aa[0] == null) {
					aa.wait(X.AMINUTE);
				}
			}

			if (aa[0] != null) {
				Request a = aa[0];
				return a.readByte() == 1 ? true : false;
			}

		} catch (Exception e) {
			close();
		} finally {
			pending.remove(r.seq);
		}
		return false;

	}

	private void close() {
		cached.remove(this.name);
	}

	public byte[] get(String path, String filename, long offset, int len) {

		if (ch == null)
			return null;

		Response r = Response.create(seq.incrementAndGet());

		try {
			r.writeByte(FileServer.CMD_GET);
			r.writeString(path);
			r.writeString(filename);
			r.writeLong(offset);
			r.writeInt(len);

			Request[] aa = new Request[1];
			pending.put(r.seq, aa);
			synchronized (aa) {
				r.send(ch);
				if (aa[0] == null) {
					aa.wait(X.AMINUTE);
				}
			}

			if (aa[0] != null) {
				Request a = aa[0];
				return a.readBytes();
			}

		} catch (Exception e) {
			close();
		} finally {
			pending.remove(r.seq);
		}
		return null;
	}

	public long put(String path, String filename, long offset, byte[] bb, int len) {

		if (ch == null)
			return -1;

		Response r = Response.create(seq.incrementAndGet());

		try {
			r.writeByte(FileServer.CMD_PUT);
			r.writeString(path);
			r.writeString(filename);
			r.writeLong(offset);
			r.writeBytes(bb, len);

			Request[] aa = new Request[1];
			pending.put(r.seq, aa);
			synchronized (aa) {
				r.send(ch);
				if (aa[0] == null) {
					aa.wait(X.AMINUTE);
				}
			}

			if (aa[0] != null) {
				Request a = aa[0];
				return a.readLong();
			}

		} catch (Exception e) {
			close();
		} finally {
			pending.remove(r.seq);
		}
		return 0;
	}

	@Override
	public void process(Request r, Channel ch) {
		Request[] aa = pending.get(r.seq);
		if (aa != null) {
			synchronized (aa) {
				aa[0] = r;
				aa.notify();
			}
		}
	}

	public boolean mkdirs(String path, String filename) {
		if (ch == null)
			return false;

		Response r = Response.create(seq.incrementAndGet());

		try {

			r.writeByte(FileServer.CMD_MKDIRS);
			r.writeString(path);
			r.writeString(filename);

			Request[] aa = new Request[1];
			pending.put(r.seq, aa);
			synchronized (aa) {
				r.send(ch);
				if (aa[0] == null) {
					aa.wait(X.AMINUTE);
				}
			}

			if (aa[0] != null) {
				Request a = aa[0];
				return a.readByte() == 1 ? true : false;
			}
		} catch (Exception e) {
			close();
		} finally {
			pending.remove(r.seq);
		}
		return false;

	}

	public JSON list(String path, String filename) {

		if (ch == null)
			return null;

		Response r = Response.create(seq.incrementAndGet());

		try {
			r.writeByte(FileServer.CMD_LIST);
			r.writeString(path);
			r.writeString(filename);

			Request[] aa = new Request[1];
			pending.put(r.seq, aa);
			synchronized (aa) {
				r.send(ch);
				if (aa[0] == null) {
					aa.wait(X.AMINUTE);
				}
			}

			if (aa[0] != null) {
				Request a = aa[0];
				return JSON.fromObject(a.readString());
			}

		} catch (Exception e) {
			close();
		} finally {
			pending.remove(r.seq);
		}
		return null;

	}

	public JSON info(String path, String filename) {

		if (ch == null)
			return null;

		Response r = Response.create(seq.incrementAndGet());

		try {

			r.writeByte(FileServer.CMD_INFO);
			r.writeString(path);
			r.writeString(filename);

			Request[] aa = new Request[1];
			pending.put(r.seq, aa);
			synchronized (aa) {
				r.send(ch);
				if (aa[0] == null) {
					aa.wait(X.AMINUTE);
				}
			}

			if (aa[0] != null) {
				Request a = aa[0];
				return JSON.fromObject(a.readString());
			}

		} catch (Exception e) {
			close();
		} finally {
			pending.remove(r.seq);
		}
		return null;
	}

	@Override
	public void closed(String name) {

		cached.remove(name);
	}

	public boolean move(String path, String filename, String path2, String filename2) {

		if (ch == null)
			return false;

		Response r = Response.create(seq.incrementAndGet());

		try {

			r.writeByte(FileServer.CMD_MOVE);
			r.writeString(path);
			r.writeString(filename);
			r.writeString(path2);
			r.writeString(filename2);

			Request[] aa = new Request[1];
			pending.put(r.seq, aa);
			synchronized (aa) {
				r.send(ch);
				if (aa[0] == null) {
					aa.wait(X.AMINUTE);
				}
			}

			if (aa[0] != null) {
				Request a = aa[0];
				return a.readByte() == 1 ? true : false;
			}
		} catch (Exception e) {
			close();
		} finally {
			pending.remove(r.seq);
		}
		return false;
	}

	public static void main(String[] args) throws Exception {

		FileClient c = FileClient.get("127.0.0.1", 9099);

		System.out.println(c.info("/Users/joe/d/temp", "/"));

		System.out.println(c.list("/Users/joe/d/temp", "/"));

		// c.mkdirs("/Users/joe/d/temp", "/tttttt");

		// c.delete("/Users/joe/d/temp", "/tttttt");

		// c.put("/Users/joe/d/temp", "/tttttt/t.txt", 0, "abcde".getBytes());

		byte[] bb = c.get("/Users/joe/d/temp", "/tttttt/t.txt", 0, 100);
		System.out.println(new String(bb));

		// DFileOutputStream out = DFileOutputStream.create("127.0.0.1", 9099,
		// "/Users/joe/d/temp", "/tttttt/t.txt");
		// out.write("asdasdasdasdasdasdasda".getBytes());
		// out.flush();
		// out.close();
		//
		Disk d = new Disk();
		d.set("path", "/Users/joe/d/temp");
		Node n = new Node();
		n.set("ip", "127.0.0.1");
		n.set("port", 9099);
		// d.node_obj = n;

		DFileInputStream in = DFileInputStream.create(d, "/tttttt/WechatIMG3431.jpeg");
		IOUtil.copy(in, new FileOutputStream("/Users/joe/d/temp/tttttt/a.jpg"));
		System.out.println("ok");
	}

}