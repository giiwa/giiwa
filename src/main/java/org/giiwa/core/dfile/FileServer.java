
package org.giiwa.core.dfile;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.app.task.DiskHeartbeat;
import org.giiwa.core.base.Host;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Config;
import org.giiwa.core.dfile.command.DELETE;
import org.giiwa.core.dfile.command.GET;
import org.giiwa.core.dfile.command.HTTP;
import org.giiwa.core.dfile.command.INFO;
import org.giiwa.core.dfile.command.LIST;
import org.giiwa.core.dfile.command.MKDIRS;
import org.giiwa.core.dfile.command.PUT;
import org.giiwa.core.dfile.command.MOVE;
import org.giiwa.core.dfile.command.NOTIFY;
import org.giiwa.core.nio.IRequestHandler;
import org.giiwa.core.nio.IResponseHandler;
import org.giiwa.core.nio.Request;
import org.giiwa.core.nio.Response;
import org.giiwa.core.nio.Server;
import org.giiwa.core.task.Task;

public class FileServer implements IRequestHandler {

	private static Log log = LogFactory.getLog(FileServer.class);

	public static FileServer inst = new FileServer();

	public static String URL = "udp://0.0.0.0:9091";

	private static Map<Byte, ICommand> commands = new HashMap<Byte, ICommand>();

	private Server serv;

	public void stop() {
		X.close(serv);
	}

	public void start() {

		try {
			commands.put(ICommand.CMD_DELETE, new DELETE());
			commands.put(ICommand.CMD_INFO, new INFO());
			commands.put(ICommand.CMD_GET, new GET());
			commands.put(ICommand.CMD_PUT, new PUT());
			commands.put(ICommand.CMD_LIST, new LIST());
			commands.put(ICommand.CMD_MKDIRS, new MKDIRS());
			commands.put(ICommand.CMD_MOVE, new MOVE());
			commands.put(ICommand.CMD_HTTP, new HTTP());
			commands.put(ICommand.CMD_NOTIFY, new NOTIFY());

			String[] ss = X.split(Host.getLocalip(), "[;,]");
			URL = Config.getConf().getString("dfile.url",
					"tcp://" + (ss == null || ss.length == 0 ? "127.0.0.1" : ss[0]) + ":9091");

			serv = Server.bind(URL, this);

			DiskHeartbeat.inst.schedule(0);

		} catch (Exception e) {
			log.error(URL, e);

			Task.schedule(() -> {
				start();
			}, 3000);
		}
	}

	public void process(Request in, IResponseHandler handler) {

		byte cmd = in.readByte();
		// System.out.println("cmd=" + cmd);

		ICommand c = commands.get(cmd);
		if (c != null) {
			// ICommand.log.debug("cmd=" + cmd + ", processor=" + c);
			c.process(in, handler);
		} else {
			Response out = Response.create(in.seq);
			out.writeString("unknown cmd");
			handler.send(out);
		}

	}

	// public static void main(String[] args) {
	// try {
	// new FileServer().start();
	// } catch (Exception e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }

	@Override
	public void closed(String name) {
		// do nothing
	}

	public void shutdown() {
		if (serv != null) {
			X.close(serv);
		}
	}

}
