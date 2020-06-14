/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.bean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.*;
import org.giiwa.dao.Helper.V;
import org.giiwa.task.Task;
import org.giiwa.web.Controller;
import org.giiwa.web.Module;

/**
 * Operation Log bean. <br>
 * Used to record info/warn/error log in database <br>
 * Beside this, the module also can add personal ILogger for other use by
 * OpLog.addLogger() <br>
 * table="gi_oplog"
 * 
 * @author yjiang
 * 
 */
@Table(name = "gi_glog", memo = "GI-系统日志")
public class GLog extends Bean {

	private static final long serialVersionUID = 1L;

	public static final BeanDAO<String, GLog> dao = BeanDAO.create(GLog.class);

	private static final Log log = LogFactory.getLog(GLog.class);

	public static final int TYPE_SECURITY = 0;
	public static final int TYPE_APP = 1;
	public static final int TYPE_OPLOG = 2;
	public static final int TYPE_DB = 3;

	private static final int LEVEL_INFO = 0;
	private static final int LEVEL_WARN = 1;
	private static final int LEVEL_ERROR = 2;

	@Column(memo = "唯一序号")
	String id;

	@Column(name = "type1", memo = "类型")
	int type;

	public String getId() {
		return id;
	}

	public int getType() {
		return type;
	}

//	/**
//	 * Removes all the oplog.
//	 * 
//	 */
//	public void cleanup() {
//		dao.cleanup();
//	}

	/**
	 * get the node or subsystem name
	 * 
	 * @return string of node
	 */
	public String getNode() {
		return this.getString("node");
	}

	transient Node node_obj;

	public Node getNode_obj() {
		if (node_obj == null) {
			node_obj = Node.dao.load(this.getNode());
		}
		return node_obj;
	}

	/**
	 * get the model name
	 * 
	 * @return string of the model
	 */
	public String getModel() {
		return this.getString("model");
	}

	/**
	 * get the operation
	 * 
	 * @return string of operation
	 */
	public String getOp() {
		return this.getString("op");
	}

	/**
	 * get the message
	 * 
	 * @return string of the message
	 */
	public String getMessage() {
		return this.getString("message");
	}

	/**
	 * get the trace
	 * 
	 * @return string of trace
	 */
	public String getTrace() {
		return this.getString("trace");
	}

	/**
	 * get the user id
	 * 
	 * @return long of user id
	 */
	public long getUid() {
		return this.getLong("uid");
	}

	/**
	 * get the ip address
	 * 
	 * @return string og ip
	 */
	public String getIp() {
		return this.getString("ip");
	}

	private transient User user_obj;

	/**
	 * get the user object
	 * 
	 * @return User
	 */
	public User getUser_obj() {
		long uid = this.getUid();
		if (user_obj == null && uid > -1) {
			user_obj = User.dao.load(uid);
		}
		return user_obj;
	}

	/**
	 * operation logger
	 */
	public static ILog oplog = new OpLog();

	/**
	 * application/module logger
	 */
	public static ILog applog = new AppLog();

	/**
	 * security logger
	 */
	public static ILog securitylog = new SecurityLog();

	// --------------API

	public static abstract class ILog {

		/**
		 * record info log
		 * 
		 * @param model   the model name
		 * @param op      the operation
		 * @param message the message
		 * @param u       the user object
		 * @param ip      the ip address
		 */
		public void info(String model, String op, String message, User u, String ip) {
			info(model, op, message, (String) null, u, ip);
		}

		/**
		 * record the log
		 * 
		 * @param model   the model name
		 * @param op      the op name
		 * @param message the message
		 * @param trace   the Throwable
		 * @param u       the user object
		 * @param ip      the remote ip
		 */
		public void info(String model, String op, String message, Throwable trace, User u, String ip) {
			info(Local.id(), model, op, message, X.toString(trace).replaceAll(System.lineSeparator(), "<br/>")
					.replaceAll(" ", "&nbsp;").replaceAll("\t", "&nbsp;&nbsp;&nbsp;&nbsp;"), u, ip);
		}

		/**
		 * record info log
		 * 
		 * @param model   the model name
		 * @param op      the operation
		 * @param message the message
		 * @param trace   the trace info
		 * @param u       the user object
		 * @param ip      the ip address
		 */
		public void info(String model, String op, String message, String trace, User u, String ip) {
			info(Local.id(), model, op, message, trace, u, ip);
		}

		/**
		 * record info log
		 * 
		 * @param model   the subclass of Model
		 * @param op      the operation
		 * @param message the message
		 * @param u       the user object
		 * @param ip      the ip address
		 */
		public void info(Class<? extends Controller> model, String op, String message, User u, String ip) {
			info(model, op, message, null, u, ip);
		}

		/**
		 * record info log
		 * 
		 * @param model   the subclass of Model
		 * @param op      the operation
		 * @param message the message
		 * @param trace   the trace info
		 * @param u       the user object
		 * @param ip      the ip address
		 */
		public void info(Class<? extends Controller> model, String op, String message, String trace, User u,
				String ip) {
			info(Module.shortName(model), op, message, trace, u, ip);
		}

		/**
		 * record info log
		 * 
		 * @param model
		 * @param op
		 * @param message
		 */
		public void info(String model, String op, String message) {
			info(model, op, message, null, null);
		}

		/**
		 * record the info log
		 * 
		 * @param model
		 * @param op
		 * @param message
		 */
		public void info(Class<? extends Controller> model, String op, String message) {
			info(Module.shortName(model), op, message, null, null);
		}

		/**
		 * record warn log
		 * 
		 * @param model
		 * @param op
		 * @param message
		 */
		public void warn(String model, String op, String message) {
			warn(model, op, message, null, null);
		}

		/**
		 * record the warn log
		 * 
		 * @param model
		 * @param op
		 * @param message
		 */
		public void warn(Class<? extends Controller> model, String op, String message) {
			warn(Module.shortName(model), op, message, null, null);
		}

		/**
		 * record error log
		 * 
		 * @param model
		 * @param op
		 * @param message
		 */
		public void error(String model, String op, String message, Throwable e) {

			if (e instanceof OutOfMemoryError) {
				Task.schedule(() -> {
					log.error("restart as outofmemory", e);
					System.exit(0);
				}, 5000);
			}

			error(model, op, message, X.toString(e).replaceAll(System.lineSeparator(), "<br/>")
					.replaceAll(" ", "&nbsp;").replaceAll("\t", "&nbsp;&nbsp;&nbsp;&nbsp;"), null, null);
		}

		/**
		 * record the error log
		 * 
		 * @param model
		 * @param op
		 * @param message
		 */
		public void error(Class<? extends Controller> model, String op, String message, Throwable e) {

			if (e instanceof OutOfMemoryError) {
				Task.schedule(() -> {
					log.error("restart as outofmemory", e);
					System.exit(0);
				}, 5000);
			}

			error(Module.shortName(model), op, message, X.toString(e).replaceAll(System.lineSeparator(), "<br/>")
					.replaceAll(" ", "&nbsp;").replaceAll("\t", "&nbsp;&nbsp;&nbsp;&nbsp;"), null, null);
		}

		/**
		 * record info log
		 * 
		 * @param node    the node or subsystem node
		 * @param model   the model name
		 * @param op      the operation
		 * @param message the message
		 * @param trace   the trace info
		 * @param u       the user object
		 * @param ip      the ip address
		 */
		protected abstract void info(String node, String model, String op, String message, String trace, User u,
				String ip);

		protected String _log(int type, int level, String node, String model, String op, String message, String trace,
				User u, String ip) {

			int l1 = Global.getInt("oplog.level", GLog.LEVEL_WARN);
			if (type != GLog.TYPE_SECURITY && l1 > level)
				return null;

			if (Helper.isConfigured()) {
				if (message != null && message.length() > 1020) {
					message = message.substring(0, 1024);
				}
				if (trace != null && trace.length() > 8192) {
					trace = trace.substring(0, 8192);
				}
				if (!X.isEmpty(trace)) {
					message = message + "...";
				}

				String id = UID.uuid();
				V v = V.create("id", id).set("node", node).set("model", model).set("op", op)
						.set("uid", u == null ? -1 : u.getId()).set("ip", ip).set("type1", type).append("level", level);
				v.set("message", message);
				v.set("trace", trace);

				v.append("logger", _logger());
				dao.insert(v);
				return id;
			}
			return null;
		}

		private String _logger() {

			Exception e = new Exception();
			StackTraceElement[] ss = e.getStackTrace();
			if (ss != null) {
				for (StackTraceElement s : ss) {
					if (!s.getClassName().startsWith(GLog.class.getName())) {
						return (s.getClassName() + "." + s.getMethodName() + "(" + s.getFileName() + ":"
								+ s.getLineNumber() + ")");
					}
				}
			}

			return X.EMPTY;
		}

		/**
		 * record warn log
		 * 
		 * @param model   the model name
		 * @param op      the operation
		 * @param message the message
		 * @param u       the user object
		 * @param ip      the ip address
		 */
		public void warn(String model, String op, String message, User u, String ip) {
			warn(model, op, message, (String) null, u, ip);
		}

		/**
		 * record warn log
		 * 
		 * @param model   the model name
		 * @param op      the operation
		 * @param message the message
		 * @param trace   the trace info
		 * @param u       the user object
		 * @param ip      the ip address
		 */
		public void warn(String model, String op, String message, String trace, User u, String ip) {
			warn(Local.id(), model, op, message, trace, u, ip);
		}

		/**
		 * record the log
		 * 
		 * @param model
		 * @param op
		 * @param message
		 * @param trace
		 * @param u
		 * @param ip
		 */
		public void warn(String model, String op, String message, Throwable trace, User u, String ip) {
			warn(Local.id(), model, op, message, X.toString(trace).replaceAll(System.lineSeparator(), "<br/>")
					.replaceAll(" ", "&nbsp;").replaceAll("\t", "&nbsp;&nbsp;&nbsp;&nbsp;"), u, ip);
		}

		/**
		 * record warn log
		 * 
		 * @param model   the subclass of Model
		 * @param op      the operation
		 * @param message the message
		 * @param u       the user object
		 * @param ip      the ip address
		 */
		public void warn(Class<? extends Controller> model, String op, String message, User u, String ip) {
			warn(model, op, message, null, u, ip);
		}

		/**
		 * record warn log
		 * 
		 * @param model   the subclass of Model
		 * @param op      the operation
		 * @param message the message
		 * @param trace   the trace info
		 * @param u       the user object
		 * @param ip      the ip address
		 */
		public void warn(Class<? extends Controller> model, String op, String message, String trace, User u,
				String ip) {
			warn(Module.shortName(model), op, message, trace, u, ip);
		}

		/**
		 * record warn log
		 * 
		 * @param node    the node or subsystem name
		 * @param model   the model name
		 * @param op      the operation
		 * @param message the message
		 * @param trace   the trace info
		 * @param u       the user object
		 * @param ip      the ip address
		 */
		protected abstract void warn(String node, String model, String op, String message, String trace, User u,
				String ip);

		/**
		 * record error log
		 * 
		 * @param model   the subclass
		 * @param op      the operation
		 * @param message the message
		 * @param u       the user object
		 * @param ip      the ip address
		 */
		public void error(Class<? extends Controller> model, String op, String message, User u, String ip) {
			error(model, op, message, (String) null, u, ip);
		}

		/**
		 * record error log
		 * 
		 * @param model   the subclass of Model
		 * @param op      the operation
		 * @param message the message
		 * @param e       the Exception
		 * @param u       the user object
		 * @param ip      the ip address
		 */
		public void error(Class<? extends Controller> model, String op, String message, Exception e, User u,
				String ip) {
			error(Module.shortName(model), op, message, e, u, ip);
		}

		/**
		 * record error log
		 * 
		 * @param model   the model name
		 * @param op      the operation
		 * @param message the message
		 * @param e       the Exception
		 * @param u       the user object
		 * @param ip      the ip address
		 */
		public void error(String model, String op, String message, Throwable e, User u, String ip) {
			if (!isEnabled(model))
				return;

			if (e instanceof OutOfMemoryError) {
				Task.schedule(() -> {

					log.error("restart as outofmemory", e);

					System.exit(0);
				}, 5000);
			}

			error(model, op, message, X.toString(e).replaceAll(System.lineSeparator(), "<br/>")
					.replaceAll(" ", "&nbsp;").replaceAll("\t", "&nbsp;&nbsp;&nbsp;&nbsp;"), u, ip);
		}

		/**
		 * record error log
		 * 
		 * @param model   the subclass of Model
		 * @param op      the operation
		 * @param message the message
		 * @param trace   the trace info
		 * @param u       the user object
		 * @param ip      the ip address
		 */
		public void error(Class<? extends Controller> model, String op, String message, String trace, User u,
				String ip) {
			error(Module.shortName(model), op, message, trace, u, ip);
		}

		/**
		 * record error log
		 * 
		 * @param model   the model name
		 * @param op      the operation
		 * @param message the message
		 * @param u       the user object
		 * @param ip      the ip address
		 */
		public void error(String model, String op, String message, User u, String ip) {
			error(model, op, message, (String) null, u, ip);
		}

		/**
		 * record error log
		 * 
		 * @param model   the model name
		 * @param op      the operation
		 * @param message the message
		 * @param trace   the trace info
		 * @param u       the user object
		 * @param ip      the ip address
		 */
		public void error(String model, String op, String message, String trace, User u, String ip) {
			if (!isEnabled(model))
				return;

			error(Local.id(), model, op, message, trace, u, ip);
		}

		/**
		 * record error log
		 * 
		 * @param node    the node or subsystem name
		 * @param model   the model name
		 * @param op      the operation
		 * @param message the message
		 * @param trace   the trace info
		 * @param u       the user object
		 * @param ip      the ip address
		 */
		protected abstract void error(String node, String model, String op, String message, String trace, User u,
				String ip);

		protected boolean isEnabled(String model) {
			return true;
		}

	}

	private static class SecurityLog extends ILog {

		protected void info(String node, String model, String op, String message, String trace, User u, String ip) {
			_log(GLog.TYPE_SECURITY, GLog.LEVEL_INFO, node, model, op, message, trace, u, ip);
		}

		protected void warn(String node, String model, String op, String message, String trace, User u, String ip) {
			Counter.increase("warn.security");
			_log(GLog.TYPE_SECURITY, GLog.LEVEL_WARN, node, model, op, message, trace, u, ip);
		}

		protected void error(String node, String model, String op, String message, String trace, User u, String ip) {
			Counter.increase("error.security");
			_log(GLog.TYPE_SECURITY, GLog.LEVEL_ERROR, node, model, op, message, trace, u, ip);
		}

	}

	private static class OpLog extends ILog {

		protected void info(String node, String model, String op, String message, String trace, User u, String ip) {
			_log(GLog.TYPE_OPLOG, GLog.LEVEL_INFO, node, model, op, message, trace, u, ip);
		}

		protected void warn(String node, String model, String op, String message, String trace, User u, String ip) {
			Counter.increase("warn.op");
			_log(GLog.TYPE_OPLOG, GLog.LEVEL_WARN, node, model, op, message, trace, u, ip);
		}

		protected void error(String node, String model, String op, String message, String trace, User u, String ip) {
			Counter.increase("error.op");
			_log(GLog.TYPE_OPLOG, GLog.LEVEL_ERROR, node, model, op, message, trace, u, ip);
		}

	}

	private static class AppLog extends ILog {

		protected void info(String node, String model, String op, String message, String trace, User u, String ip) {
			_log(GLog.TYPE_APP, GLog.LEVEL_INFO, node, model, op, message, trace, u, ip);
		}

		protected void warn(String node, String model, String op, String message, String trace, User u, String ip) {
			Counter.increase("warn.app");
			_log(GLog.TYPE_APP, GLog.LEVEL_WARN, node, model, op, message, trace, u, ip);
		}

		protected void error(String node, String model, String op, String message, String trace, User u, String ip) {
			Counter.increase("error.app");
			_log(GLog.TYPE_APP, GLog.LEVEL_ERROR, node, model, op, message, trace, u, ip);
		}

	}

	public static void main(String[] aa) {
		oplog._logger();
	}

	static {
		Counter.set("error.app", 0);
		Counter.set("warn.app", 0);

		Counter.set("error.op", 0);
		Counter.set("warn.op", 0);

		Counter.set("error.security", 0);
		Counter.set("warn.security", 0);
	}

}
