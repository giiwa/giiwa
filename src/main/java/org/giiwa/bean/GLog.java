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

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.*;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.misc.Host;
import org.giiwa.task.Task;
import org.giiwa.web.Controller;
import org.giiwa.web.Language;
import org.graylog2.syslog4j.Syslog;
import org.graylog2.syslog4j.SyslogConstants;
import org.graylog2.syslog4j.SyslogIF;

/**
 * Operation Log bean. <br>
 * Used to record info/warn/error log in database <br>
 * Beside this, the module also can add personal ILogger for other use by
 * OpLog.addLogger() <br>
 * 
 * @author yjiang
 * 
 */
@Table(name = "gi_glog", memo = "GI-系统日志")
public final class GLog extends Bean {

	private static final long serialVersionUID = 1L;

	public static final BeanDAO<String, GLog> dao = BeanDAO.create(GLog.class);

	private static final Log log = LogFactory.getLog(GLog.class);

	public static final int TYPE_SECURITY = 0;
	public static final int TYPE_APP = 1;
	public static final int TYPE_OPLOG = 2;
	public static final int TYPE_DB = 3;

	public static final int LEVEL_ERROR = 1;
	public static final int LEVEL_WARN = 2;
	public static final int LEVEL_INFO = 3;

	@Column(memo = "主键", unique = true, size = 50)
	String id;

	@Column(memo = "消息HASH", size = 50)
	String iid;

	@Column(name = "_type", memo = "类型")
	int type;

	@Column(memo = "节点", size = 50)
	String node;

	@Column(memo = "模块", size = 50)
	String model;

	@Column(memo = "操作", size = 50)
	String op;

	@Column(memo = "内容", size = 2048)
	String message;

	@Column(memo = "线程名称", size = 100)
	String thread;

	@Column(memo = "调用栈", size = 2048)
	String trace;

	@Column(name = "_level", memo = "日志级别")
	int level;

	@Column(memo = "用户ID")
	long uid;

	@Column(memo = "IP地址", size = 50)
	String ip;

	@Column(memo = "调用者", size = 100)
	String logger;

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
		return node;
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

		public abstract boolean exists(String message);

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
			info(Local.id(), model, op, message, X.toString(trace), u, ip);
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
		public void info(Class<?> model, String op, String message, User u, String ip) {
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
		public void info(Class<?> model, String op, String message, String trace, User u, String ip) {
			info(model.getSimpleName(), op, message, trace, u, ip);
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
		 * info log
		 * 
		 * @param model
		 * @param op
		 * @param message
		 */
		public void info(Controller model, String op, String message) {
			info(model.getClass(), op, message, model.user(), model.ip());
		}

		/**
		 * record the info log
		 * 
		 * @param model
		 * @param op
		 * @param message
		 */
		public void info(Class<?> model, String op, String message) {
			info(model.getSimpleName(), op, message, null, null);
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
		 * warn log
		 * 
		 * @param model
		 * @param op
		 * @param message
		 */
		public void warn(Controller model, String op, String message) {
			warn(model.getClass().getSimpleName(), op, message, model.user(), model.ip());
		}

		/**
		 * record the warn log
		 * 
		 * @param model
		 * @param op
		 * @param message
		 */
		public void warn(Class<?> model, String op, String message) {
			warn(model.getSimpleName(), op, message, null, null);
		}

		public void error(Controller model, String op, String message, Throwable e) {
			error(model.getClass().getSimpleName(), op, message, model.user(), model.ip());
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
				log.error("restart as outofmemory", e);

				Task.schedule(t -> {
//					System.exit(0);
				}, 1000);
			}

			error(model, op, message, X.toString(e), null, null);
		}

		/**
		 * record the error log
		 * 
		 * @param model
		 * @param op
		 * @param message
		 */
		public void error(Class<?> model, String op, String message, Throwable e) {

			if (e instanceof OutOfMemoryError) {
				log.error("restart as outofmemory", e);
				Task.schedule(t -> {
//					System.exit(0);
				}, 5000);
			}

			error(model.getSimpleName(), op, message, X.toString(e), null, null);
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

		private SyslogIF syslog = null;

		protected String _log(int type, int level, String node, String model, String op, String message, String trace,
				User u, String ip) {

			if (X.isEmpty(message)) {
				return null;
			}

			int l1 = Global.getInt("oplog.level", GLog.LEVEL_WARN);
			if (type != GLog.TYPE_SECURITY && l1 > level) {
				return null;
			}

			if (Helper.isConfigured()) {

				if (message != null && message.length() > 1024) {
					message = message.substring(0, 1024);
				}
				if (trace != null && trace.length() > 8192) {
					trace = trace.substring(0, 8192);
				}

				String id = UID.uuid();
				V v = V.create("id", id).set("node", node).set("model", model).set("op", op)
						.set("uid", u == null ? -1 : u.getId()).set("ip", ip).set("_type", type).append("level", level);
				v.set("message", message);

				String threadname = Thread.currentThread().getName();
				v.append("thread", threadname);
				v.set("trace", trace == null ? null : trace);
				v.set("iid", UID.id(node, type, message));

				String logger = _logger();
				v.append("logger", logger);
				dao.insert(v);

				if (Global.getInt("glog.rsyslog", 0) == 1) {
					// enabled rsyslog
					String message1 = message;

					Task.schedule(t -> {

						if (syslog == null) {
							syslog = Syslog.getInstance(SyslogConstants.UDP);
							syslog.getConfig().setHost(Global.getString("glog.rsyslog.host", "127.0.0.1"));
							syslog.getConfig().setPort(X.toInt(Global.getLong("glog.rsyslog.port", 32376)));
						}

						// <165>1 2003-08-24T05:14:15.000003-07:00 192.0.2.1 myproc 8710 - - %% It's
						// time to make the do-nuts.
						StringBuilder sb = new StringBuilder();
						sb.append("<" + seq.incrementAndGet() + ">");
						sb.append(level);
						Language lang = Language.getLanguage();
						sb.append(" " + lang.format(System.currentTimeMillis(), "yyyy-MM-dd") + "T"
								+ lang.format(System.currentTimeMillis(), "HH:mm:ss.S"));
						sb.append(" " + Host.getLocalip());
						sb.append(" giiwa ").append(Host.getPid());
						sb.append(" - -");
						sb.append(" BOM" + message1);

						syslog.log(level, sb.toString(), new Date());
					});
				}
				return id;
			}
			return null;
		}

		private static AtomicLong seq = new AtomicLong(0);

		private String _logger() {

			Exception e = new Exception();
			StackTraceElement[] ss = e.getStackTrace();
			if (ss != null) {
				for (StackTraceElement s : ss) {
					String s1 = s.getClassName();
					if (!s1.startsWith(GLog.class.getName()) && !s1.startsWith(Controller.class.getName())) {
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
			warn(Local.id(), model, op, message, X.toString(trace), u, ip);
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
		public void warn(Class<?> model, String op, String message, User u, String ip) {
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
		public void warn(Class<?> model, String op, String message, String trace, User u, String ip) {
			warn(model.getSimpleName(), op, message, trace, u, ip);
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
		public void error(Class<?> model, String op, String message, User u, String ip) {
			error(model, op, message, (String) null, u, ip);
		}

		/**
		 * error log
		 * 
		 * @param model
		 * @param op
		 * @param message
		 * @param e
		 */
		public void error(Controller model, String op, String message, Exception e) {
			error(model.getClass(), op, message, e, model.user(), model.ip());
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
		public void error(Class<?> model, String op, String message, Exception e, User u, String ip) {
			error(model.getSimpleName(), op, message, e, u, ip);
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
				log.error("Restart as OutOfMemory", e);
				Task.schedule(t -> {
					System.exit(0);
				}, 5000);
			}

			error(model, op, message, X.toString(e), u, ip);
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
		public void error(Class<?> model, String op, String message, String trace, User u, String ip) {
			error(model.getSimpleName(), op, message, trace, u, ip);
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
			_log(GLog.TYPE_SECURITY, GLog.LEVEL_WARN, node, model, op, message, trace, u, ip);
		}

		protected void error(String node, String model, String op, String message, String trace, User u, String ip) {
			_log(GLog.TYPE_SECURITY, GLog.LEVEL_ERROR, node, model, op, message, trace, u, ip);
		}

		@Override
		public boolean exists(String message) {
			try {
				String iid = UID.id(Local.id(), GLog.TYPE_SECURITY, message);
				W q = W.create().and("iid", iid);
				dao.optimize(q);
				return dao.exists(q);
			} catch (Exception e) {
				// ignore
			}
			return false;
		}

	}

	private static class OpLog extends ILog {

		protected void info(String node, String model, String op, String message, String trace, User u, String ip) {
			_log(GLog.TYPE_OPLOG, GLog.LEVEL_INFO, node, model, op, message, trace, u, ip);
		}

		protected void warn(String node, String model, String op, String message, String trace, User u, String ip) {
			_log(GLog.TYPE_OPLOG, GLog.LEVEL_WARN, node, model, op, message, trace, u, ip);
		}

		protected void error(String node, String model, String op, String message, String trace, User u, String ip) {
			_log(GLog.TYPE_OPLOG, GLog.LEVEL_ERROR, node, model, op, message, trace, u, ip);
		}

		public boolean exists(String message) {
			try {
				String iid = UID.id(Local.id(), GLog.TYPE_OPLOG, message);
				return dao.exists(W.create().and("iid", iid));
			} catch (Exception e) {
				// ignore
			}
			return false;
		}

	}

	private static class AppLog extends ILog {

		protected void info(String node, String model, String op, String message, String trace, User u, String ip) {
			_log(GLog.TYPE_APP, GLog.LEVEL_INFO, node, model, op, message, trace, u, ip);
		}

		protected void warn(String node, String model, String op, String message, String trace, User u, String ip) {
			_log(GLog.TYPE_APP, GLog.LEVEL_WARN, node, model, op, message, trace, u, ip);
		}

		protected void error(String node, String model, String op, String message, String trace, User u, String ip) {
			_log(GLog.TYPE_APP, GLog.LEVEL_ERROR, node, model, op, message, trace, u, ip);
		}

		public boolean exists(String message) {
			try {
				String iid = UID.id(Local.id(), GLog.TYPE_APP, message);
				return dao.exists(W.create().and("iid", iid));
			} catch (Exception e) {
				// ignore
			}
			return false;
		}

	}
}
