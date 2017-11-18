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
package org.giiwa.framework.bean;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.giiwa.core.bean.*;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.conf.Global;
import org.giiwa.core.conf.Local;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Module;

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
@Table(name = "gi_glog")
public class GLog extends Bean {

	private static final long serialVersionUID = 1L;

	public static final BeanDAO<GLog> dao = BeanDAO.create(GLog.class);

	private static final int TYPE_SECURITY = 0;
	private static final int TYPE_APP = 1;
	private static final int TYPE_OPLOG = 2;

	private static final int LEVEL_INFO = 0;
	private static final int LEVEL_WARN = 1;
	private static final int LEVEL_ERROR = 2;

	/**
	 * Removes all the oplog.
	 * 
	 * @return the number was deleted
	 */
	public static int cleanup() {
		return dao.delete(W.create());
	}

	/**
	 * get the node or subsystem name
	 * 
	 * @return string of node
	 */
	public String getNode() {
		return this.getString("node");
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

	public static ILog oplog = new OpLog();
	public static ILog applog = new AppLog();
	public static ILog securitylog = new SecurityLog();

	// --------------API

	public static abstract class ILog {
		/**
		 * record info log
		 * 
		 * @param model
		 *            the model name
		 * @param op
		 *            the operation
		 * @param message
		 *            the message
		 * @param u
		 *            the user object
		 * @param ip
		 *            the ip address
		 */
		public void info(String model, String op, String message, User u, String ip) {
			info(model, op, message, null, u, ip);
		}

		/**
		 * record info log
		 * 
		 * @param model
		 *            the model name
		 * @param op
		 *            the operation
		 * @param message
		 *            the message
		 * @param trace
		 *            the trace info
		 * @param u
		 *            the user object
		 * @param ip
		 *            the ip address
		 */
		public void info(String model, String op, String message, String trace, User u, String ip) {
			info(Local.id(), model, op, message, trace, u, ip);
		}

		/**
		 * record info log
		 * 
		 * @param model
		 *            the subclass of Model
		 * @param op
		 *            the operation
		 * @param message
		 *            the message
		 * @param u
		 *            the user object
		 * @param ip
		 *            the ip address
		 */
		public void info(Class<? extends Model> model, String op, String message, User u, String ip) {
			info(model, op, message, null, u, ip);
		}

		/**
		 * record info log
		 * 
		 * @param model
		 *            the subclass of Model
		 * @param op
		 *            the operation
		 * @param message
		 *            the message
		 * @param trace
		 *            the trace info
		 * @param u
		 *            the user object
		 * @param ip
		 *            the ip address
		 */
		public void info(Class<? extends Model> model, String op, String message, String trace, User u, String ip) {
			info(Module.shortName(model), op, message, trace, u, ip);
		}

		/**
		 * record info log
		 * 
		 * @param node
		 *            the node or subsystem node
		 * @param model
		 *            the model name
		 * @param op
		 *            the operation
		 * @param message
		 *            the message
		 * @param trace
		 *            the trace info
		 * @param u
		 *            the user object
		 * @param ip
		 *            the ip address
		 */
		protected abstract void info(String node, String model, String op, String message, String trace, User u,
				String ip);

		protected void _log(int type, int level, String node, String model, String op, String message, String trace,
				User u, String ip) {

			int l1 = Global.getInt("oplog.level", GLog.LEVEL_WARN);
			if (l1 > level)
				return;

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

				long t = System.currentTimeMillis();
				String id = UID.id(t, op, message);
				V v = V.create("id", id).set(X.CREATED, t).set("node", node).set("model", model).set("op", op)
						.set("uid", u == null ? -1 : u.getId()).set("ip", ip).set("type", type).append("level", level);
				v.set("message", message);
				v.set("trace", trace);

				dao.insert(v);
			}
		}

		/**
		 * record warn log
		 * 
		 * @param model
		 *            the model name
		 * @param op
		 *            the operation
		 * @param message
		 *            the message
		 * @param u
		 *            the user object
		 * @param ip
		 *            the ip address
		 */
		public void warn(String model, String op, String message, User u, String ip) {
			warn(model, op, message, null, u, ip);
		}

		/**
		 * record warn log
		 * 
		 * @param model
		 *            the model name
		 * @param op
		 *            the operation
		 * @param message
		 *            the message
		 * @param trace
		 *            the trace info
		 * @param u
		 *            the user object
		 * @param ip
		 *            the ip address
		 */
		public void warn(String model, String op, String message, String trace, User u, String ip) {
			warn(Local.id(), model, op, message, trace, u, ip);
		}

		/**
		 * record warn log
		 * 
		 * @param model
		 *            the subclass of Model
		 * @param op
		 *            the operation
		 * @param message
		 *            the message
		 * @param u
		 *            the user object
		 * @param ip
		 *            the ip address
		 */
		public void warn(Class<? extends Model> model, String op, String message, User u, String ip) {
			warn(model, op, message, null, u, ip);
		}

		/**
		 * record warn log
		 * 
		 * @param model
		 *            the subclass of Model
		 * @param op
		 *            the operation
		 * @param message
		 *            the message
		 * @param trace
		 *            the trace info
		 * @param u
		 *            the user object
		 * @param ip
		 *            the ip address
		 */
		public void warn(Class<? extends Model> model, String op, String message, String trace, User u, String ip) {
			warn(Module.shortName(model), op, message, trace, u, ip);
		}

		/**
		 * record warn log
		 * 
		 * @param node
		 *            the node or subsystem name
		 * @param model
		 *            the model name
		 * @param op
		 *            the operation
		 * @param message
		 *            the message
		 * @param trace
		 *            the trace info
		 * @param u
		 *            the user object
		 * @param ip
		 *            the ip address
		 */
		protected abstract void warn(String node, String model, String op, String message, String trace, User u,
				String ip);

		/**
		 * record error log
		 * 
		 * @param model
		 *            the subclass
		 * @param op
		 *            the operation
		 * @param message
		 *            the message
		 * @param u
		 *            the user object
		 * @param ip
		 *            the ip address
		 */
		public void error(Class<? extends Model> model, String op, String message, User u, String ip) {
			error(model, op, message, (String) null, u, ip);
		}

		/**
		 * record error log
		 * 
		 * @param model
		 *            the subclass of Model
		 * @param op
		 *            the operation
		 * @param message
		 *            the message
		 * @param e
		 *            the Exception
		 * @param u
		 *            the user object
		 * @param ip
		 *            the ip address
		 */
		public void error(Class<? extends Model> model, String op, String message, Exception e, User u, String ip) {
			error(Module.shortName(model), op, message, e, u, ip);
		}

		/**
		 * record error log
		 * 
		 * @param model
		 *            the model name
		 * @param op
		 *            the operation
		 * @param message
		 *            the message
		 * @param e
		 *            the Exception
		 * @param u
		 *            the user object
		 * @param ip
		 *            the ip address
		 */
		public void error(String model, String op, String message, Exception e, User u, String ip) {
			String s = X.EMPTY;
			if (e != null) {
				StringWriter sw = new StringWriter();
				PrintWriter out = new PrintWriter(sw);
				e.printStackTrace(out);
				s = sw.toString();
				String lineSeparator = System.lineSeparator();
				s = s.replaceAll(lineSeparator, "<br/>");
				s = s.replaceAll(" ", "&nbsp;");
				s = s.replaceAll("\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
			}

			error(model, op, message, s, u, ip);
		}

		/**
		 * record error log
		 * 
		 * @param model
		 *            the subclass of Model
		 * @param op
		 *            the operation
		 * @param message
		 *            the message
		 * @param trace
		 *            the trace info
		 * @param u
		 *            the user object
		 * @param ip
		 *            the ip address
		 */
		public void error(Class<? extends Model> model, String op, String message, String trace, User u, String ip) {
			error(Module.shortName(model), op, message, trace, u, ip);
		}

		/**
		 * record error log
		 * 
		 * @param model
		 *            the model name
		 * @param op
		 *            the operation
		 * @param message
		 *            the message
		 * @param u
		 *            the user object
		 * @param ip
		 *            the ip address
		 */
		public void error(String model, String op, String message, User u, String ip) {
			error(model, op, message, (String) null, u, ip);
		}

		/**
		 * record error log
		 * 
		 * @param model
		 *            the model name
		 * @param op
		 *            the operation
		 * @param message
		 *            the message
		 * @param trace
		 *            the trace info
		 * @param u
		 *            the user object
		 * @param ip
		 *            the ip address
		 */
		public void error(String model, String op, String message, String trace, User u, String ip) {
			error(Local.id(), model, op, message, trace, u, ip);
		}

		/**
		 * record error log
		 * 
		 * @param node
		 *            the node or subsystem name
		 * @param model
		 *            the model name
		 * @param op
		 *            the operation
		 * @param message
		 *            the message
		 * @param trace
		 *            the trace info
		 * @param u
		 *            the user object
		 * @param ip
		 *            the ip address
		 */
		protected abstract void error(String node, String model, String op, String message, String trace, User u,
				String ip);

	}

	private static class SecurityLog extends ILog {

		protected void info(String node, String model, String op, String message, String trace, User u, String ip) {
			_log(GLog.TYPE_SECURITY, GLog.LEVEL_INFO, node, model, op, message, trace, u, ip);

			Message.create(V.create("touid", 0).append("title", message));
		}

		protected void warn(String node, String model, String op, String message, String trace, User u, String ip) {
			_log(GLog.TYPE_SECURITY, GLog.LEVEL_WARN, node, model, op, message, trace, u, ip);

			Message.create(V.create("touid", 0).append("title", message));
		}

		protected void error(String node, String model, String op, String message, String trace, User u, String ip) {
			_log(GLog.TYPE_SECURITY, GLog.LEVEL_ERROR, node, model, op, message, trace, u, ip);
			
			Message.create(V.create("touid", 0).append("title", message));
			
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

	}

}
