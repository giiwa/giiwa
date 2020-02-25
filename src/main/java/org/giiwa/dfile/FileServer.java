
package org.giiwa.dfile;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Disk;
import org.giiwa.conf.Config;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dfile.command.DELETE;
import org.giiwa.dfile.command.GET;
import org.giiwa.dfile.command.HTTP;
import org.giiwa.dfile.command.INFO;
import org.giiwa.dfile.command.LIST;
import org.giiwa.dfile.command.MKDIRS;
import org.giiwa.dfile.command.MOVE;
import org.giiwa.dfile.command.PUT;
import org.giiwa.net.mq.IStub;
import org.giiwa.net.mq.MQ;
import org.giiwa.net.nio.IoRequest;
import org.giiwa.net.nio.IoResponse;
import org.giiwa.net.nio.Server;
import org.giiwa.task.Task;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class FileServer {

	private static Log log = LogFactory.getLog(FileServer.class);

	public static FileServer inst = new FileServer();

	public static String URL = "tcp://0.0.0.0:9091";

	/**
	 * the number of call times
	 */
	public static AtomicLong times = new AtomicLong(0);

	/**
	 * the total cost of calling
	 */
	public static AtomicLong costs = new AtomicLong(0);

	/**
	 * the max cost
	 */
	public static long maxcost = Long.MIN_VALUE;

	/**
	 * the min cost
	 */
	public static long mincost = Long.MAX_VALUE;

	private static Map<Byte, ICommand> commands = new HashMap<Byte, ICommand>();

	private Server serv;

	public void stop() {
		X.close(serv);
	}

	public void start() {

		if (serv == null) {
			try {
				commands.put(ICommand.CMD_DELETE, new DELETE());
				commands.put(ICommand.CMD_INFO, new INFO());
				commands.put(ICommand.CMD_GET, new GET());
				commands.put(ICommand.CMD_PUT, new PUT());
				commands.put(ICommand.CMD_LIST, new LIST());
				commands.put(ICommand.CMD_MKDIRS, new MKDIRS());
				commands.put(ICommand.CMD_MOVE, new MOVE());
				commands.put(ICommand.CMD_HTTP, new HTTP());

				URL = Config.getConf().getString("dfile.bind", URL);

//				serv = Server.bind(URL, new RequestHandler(URL, this));
				serv = Server.create().bind(URL, (req, resp) -> {
					IoRequest r = born(req);
					while (r != null) {
						process(r, resp);
						r = born(req);
					}
				});

			} catch (Exception e) {

				log.error(URL, e);

				Task.schedule(() -> {
					start();
				}, 3000);
			}

			_bind();

		}
	}

	private void _bind() {
		try {
			// listen the reset
			new IStub("disk.reset") {

				@Override
				public void onRequest(long seq, org.giiwa.net.mq.MQ.Request req) {
					Disk.reset();
				}

			}.bind(MQ.Mode.TOPIC);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Task.schedule(() -> {
				_bind();
			}, 3000);
		}
	}

	public static void reset() {
		// listen the reset
		try {
			MQ.topic("disk.reset", MQ.Request.create().put("reset"));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public static IoRequest born(IoRequest p) {
		int size = p.size();
		if (size < 4)
			return null;

		p.mark();

		int len = p.readInt();
		if (p.size() < len) {
			p.reset();
			return null;
		}

		byte[] d = new byte[len];
		p.readBytes(d);
		IoRequest r = IoRequest.create(Unpooled.wrappedBuffer(d));
		return r;
	}

//	public void process(IoRequest in, IResponseHandler handler) {
	public void process(IoRequest req, IoResponse resp) {

		long seq = req.readLong();

		TimeStamp t = TimeStamp.create();
		Task.schedule(() -> {
			byte cmd = req.readByte();
			// System.out.println("cmd=" + cmd);

			ICommand c = commands.get(cmd);
			if (c != null) {
				// ICommand.log.debug("cmd=" + cmd + ", processor=" + c);
				c.process(seq, req, resp);

				costs.addAndGet(t.pastms());
				times.incrementAndGet();
				if (maxcost < t.pastms()) {
					maxcost = t.pastms();
				}
				if (mincost > t.pastms()) {
					mincost = t.pastms();
				}

//				if (log.isDebugEnabled())
//					log.debug("process, cmd=" + cmd + ", cost=" + t.past());

			} else {
				byte[] bb = "unknown cmd".getBytes();
				resp.write(bb.length).write(bb);

				resp.send(e -> {
					ByteBuf b = Unpooled.buffer();
					b.writeInt(e.readableBytes() + 8);
					b.writeLong(seq);
					b.writeBytes(e);
					return b;
				});
			}
		});

	}

	public void shutdown() {
		if (serv != null) {
			X.close(serv);
			serv = null;
		}
	}

	public static void measures(V v) {
		v.append("dfiletimes", FileServer.times.get());

		if (FileServer.times.get() > 0) {
			v.append("dfileavgcost", FileServer.costs.get() / FileServer.times.get());
			v.append("dfilemaxcost", FileServer.maxcost);
			v.append("dfilemincost", FileServer.mincost);
		} else {
			v.append("dfileavgcost", 0);
			v.append("dfilemaxcost", 0);
			v.append("dfilemincost", 0);
		}

	}

	public static void resetm() {

		FileServer.times.set(0);
		FileServer.costs.set(0);
		FileServer.maxcost = Long.MIN_VALUE;
		FileServer.mincost = Long.MAX_VALUE;

	}

}
