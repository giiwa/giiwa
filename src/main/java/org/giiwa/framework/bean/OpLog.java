/*
 * Copyright 2015 Giiwa, Inc. and/or its affiliates.
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

import org.giiwa.core.base.Shell;
import org.giiwa.core.bean.*;
import org.giiwa.core.conf.Global;
import org.giiwa.framework.web.Language;

import com.mongodb.BasicDBObject;

// TODO: Auto-generated Javadoc
/**
 * Operation Log. <br>
 * collection="gi_oplog"
 * 
 * @author yjiang
 * 
 */
@DBMapping(collection = "gi_oplog")
public class OpLog extends Bean {

  private static final long serialVersionUID = 1L;

  public static final int   TYPE_INFO        = 0;
  public static final int   TYPE_WARN        = 1;
  public static final int   TYPE_ERROR       = 2;

  /**
   * Removes the.
   * 
   * @return the int
   */
  public static int remove() {
    return Bean.delete(new BasicDBObject(), OpLog.class);
  }

  /**
   * Cleanup.
   * 
   */
  public static void cleanup() {
    // TODO
    Bean.delete(
        new BasicDBObject().append("created", new BasicDBObject().append("$lt", System.currentTimeMillis() - X.AMONTH)),
        OpLog.class);

  }

  /**
   * Load.
   *
   * @param query
   *          the query
   * @param order
   *          the order
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return Beans
   */
  public static Beans<OpLog> load(BasicDBObject query, BasicDBObject order, int offset, int limit) {
    return Bean.load(query, order, offset, limit, OpLog.class);
  }

  /**
   * Load.
   *
   * @param query
   *          the query
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @return the beans
   */
  public static Beans<OpLog> load(BasicDBObject query, int offset, int limit) {
    return load(query, new BasicDBObject().append("created", -1), offset, limit);
  }

  /**
   * Log.
   * 
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @return the int
   */
  public static int log(String op, String brief, String message) {
    return log(op, brief, message, -1, null);
  }

  /**
   * Log.
   * 
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @param uid
   *          the uid
   * @param ip
   *          the ip
   * @return the int
   */
  public static int log(String op, String brief, String message, long uid, String ip) {
    return log(X.EMPTY, op, brief, message, uid, ip);
  }

  /**
   * Log.
   * 
   * @param module
   *          the module
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @return the int
   */
  public static int log(String module, String op, String brief, String message) {
    return log(module, op, brief, message, -1, null);
  }

  /**
   * Log.
   *
   * @param op
   *          the op
   * @param message
   *          the message
   * @return int
   * @deprecated
   */
  public static int log(String op, String message) {
    return info("default", op, message, -1, null);
  }

  /**
   * Log.
   * 
   * @param module
   *          the module
   * @param op
   *          the op
   * @param message
   *          the message
   * @return the int
   */
  public static int log(Class<?> module, String op, String message) {
    return info(module.getName(), op, message, -1, null);
  }

  /**
   * Log.
   * 
   * @param module
   *          the module
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @param uid
   *          the uid
   * @param ip
   *          the ip
   * @return the int
   */
  public static int log(String module, String op, String brief, String message, long uid, String ip) {
    return info(Global.s("node", X.EMPTY), module, op, brief, message, uid, ip);
  }

  /**
   * Log.
   * 
   * @param module
   *          the module
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @param uid
   *          the uid
   * @param ip
   *          the ip
   * @return the int
   */
  public static int log(Class<?> module, String op, String brief, String message, long uid, String ip) {
    return info(Global.s("node", X.EMPTY), module.getName(), op, brief, message, uid, ip);
  }

  /**
   * Log.
   * 
   * @param system
   *          the system
   * @param module
   *          the module
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @param uid
   *          the uid
   * @param ip
   *          the ip
   * @return the int
   */
  public static int log(String system, Class<?> module, String op, String brief, String message, long uid, String ip) {
    return info(system, module.getName(), op, brief, message, uid, ip);
  }

  /**
   * Log.
   * 
   * @param system
   *          the system
   * @param module
   *          the module
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @param uid
   *          the uid
   * @param ip
   *          the ip
   * @return the int
   */
  public static int log(String system, String module, String op, String brief, String message, long uid, String ip) {
    return info(system, module, op, brief, message, uid, ip);
  }

  public String getId() {
    return this.getString(X._ID);
  }

  public User getUser() {
    if (!this.containsKey("user_obj")) {
      this.set("user_obj", User.loadById(this.getLong("uid")));
    }
    return (User) this.get("user_obj");
  }

  /**
   * Load.
   *
   * @param where
   *          the where
   * @param args
   *          the args
   * @param s
   *          the s
   * @param n
   *          the n
   * @return the beans
   */
  /*
   * (non-Javadoc)
   * 
   * @see com.giiwa.bean.Exportable#load(java.lang.String, java.lang.Object[],
   * int, int)
   */
  public Beans<OpLog> load(String where, Object[] args, int s, int n) {
    return Bean.load(where, args, "order by created", s, n, OpLog.class);
  }

  public String getExportableId() {
    return getId();
  }

  public String getExportableName() {
    return this.getString("message");
  }

  public long getExportableUpdated() {
    return this.getLong("created");
  }

  /**
   * Info.
   * 
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @return the int
   */
  public static int info(String op, String brief, String message) {
    return info(op, brief, message, -1, null);
  }

  /**
   * Info.
   * 
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @param uid
   *          the uid
   * @param ip
   *          the ip
   * @return the int
   */
  public static int info(String op, String brief, String message, long uid, String ip) {
    return info(X.EMPTY, op, brief, message, uid, ip);
  }

  /**
   * Info.
   * 
   * @param module
   *          the module
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @return the int
   */
  public static int info(String module, String op, String brief, String message) {
    return info(module, op, brief, message, -1, null);
  }

  /**
   * Info.
   * 
   * @param module
   *          the module
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @return the int
   */
  public static int info(Class<?> module, String op, String brief, String message) {
    return info(module.getName(), op, brief, message, -1, null);
  }

  /**
   * Info.
   * 
   * @param module
   *          the module
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @param uid
   *          the uid
   * @param ip
   *          the ip
   * @return the int
   */
  public static int info(String module, String op, String brief, String message, long uid, String ip) {
    return info(Global.s("node", X.EMPTY), module, op, brief, message, uid, ip);
  }

  /**
   * Info.
   * 
   * @param module
   *          the module
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @param uid
   *          the uid
   * @param ip
   *          the ip
   * @return the int
   */
  public static int info(Class<?> module, String op, String brief, String message, long uid, String ip) {
    return info(Global.s("node", X.EMPTY), module.getName(), op, brief, message, uid, ip);
  }

  /**
   * Info.
   * 
   * @param system
   *          the system
   * @param module
   *          the module
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @param uid
   *          the uid
   * @param ip
   *          the ip
   * @return the int
   */
  public static int info(String system, Class<?> module, String op, String brief, String message, long uid, String ip) {
    return info(system, module.getName(), op, brief, message, uid, ip);
  }

  /**
   * Info.
   * 
   * @param system
   *          the system
   * @param module
   *          the module
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @param uid
   *          the uid
   * @param ip
   *          the ip
   * @return the int
   */
  public static int info(String system, String module, String op, String brief, String message, long uid, String ip) {
    return _log(OpLog.TYPE_INFO, system, module, op, brief, message, uid, ip);
  }

  private static int _log(int type, String system, String module, String op, String brief, String message, long uid,
      String ip) {

    // brief = Language.getLanguage().truncate(brief, 1024);
    // message = Language.getLanguage().truncate(message, 8192);

    long t = System.currentTimeMillis();
    String id = UID.id(t, op, message);
    V v = V.create("id", id).set("created", t).set("system", system).set("module", module).set("op", op).set("uid", uid)
        .set("ip", ip).set("type", type);
    if (X.isEmpty(brief)) {
      v.set("brief", message);
    } else {
      v.set("brief", brief).set("message", message);
    }
    int i = Bean.insertCollection(v, OpLog.class);

    if (i > 0) {
      // Category.update(system, module, op);

      /**
       * 记录系统日志
       */
      if (Global.i("logger.rsyslog", 0) == 1) {
        Language lang = Language.getLanguage();
        // 192.168.1.1#系统名称#2014-10-31#ERROR#日志消息#程序名称
        if (type == OpLog.TYPE_INFO) {
          Shell.log(ip, Shell.Logger.info, lang.get("log.module_" + module),
              lang.get("log.opt_" + op) + "//" + brief + ", uid=" + uid);
        } else if (type == OpLog.TYPE_ERROR) {
          Shell.log(ip, Shell.Logger.error, lang.get("log.module_" + module),
              lang.get("log.opt_" + op) + "//" + brief + ", uid=" + uid);
        } else {
          Shell.log(ip, Shell.Logger.warn, lang.get("log.module_" + module),
              lang.get("log.opt_" + op) + "//" + brief + ", uid=" + uid);
        }
      }

      // onChanged("tbloplog", IData.OP_CREATE, "created=? and id=?", new
      // Object[] { t, id });
    }

    return i;
  }

  /**
   * Warn operation log.
   * 
   * @param op
   *          the operation
   * @param brief
   *          the brief info
   * @param message
   *          the message
   * @return int
   */
  public static int warn(String op, String brief, String message) {
    return warn(op, brief, message, -1, null);
  }

  /**
   * Warn operation log.
   * 
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @param uid
   *          the uid
   * @param ip
   *          the ip
   * @return the int
   */
  public static int warn(String op, String brief, String message, long uid, String ip) {
    return warn(X.EMPTY, op, brief, message, uid, ip);
  }

  /**
   * Warn.
   * 
   * @param module
   *          the module
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @return the int
   */
  public static int warn(String module, String op, String brief, String message) {
    return warn(module, op, brief, message, -1, null);
  }

  /**
   * Warn.
   * 
   * @param module
   *          the module
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @return the int
   */
  public static int warn(Class<?> module, String op, String brief, String message) {
    return warn(module.getName(), op, brief, message, -1, null);
  }

  /**
   * Warn.
   * 
   * @param module
   *          the module
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @param uid
   *          the uid
   * @param ip
   *          the ip
   * @return the int
   */
  public static int warn(String module, String op, String brief, String message, long uid, String ip) {
    return warn(Global.s("node", X.EMPTY), module, op, brief, message, uid, ip);
  }

  /**
   * Warn.
   * 
   * @param module
   *          the module
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @param uid
   *          the uid
   * @param ip
   *          the ip
   * @return the int
   */
  public static int warn(Class<?> module, String op, String brief, String message, long uid, String ip) {
    return warn(Global.s("node", X.EMPTY), module.getName(), op, brief, message, uid, ip);
  }

  /**
   * Warn.
   * 
   * @param system
   *          the system
   * @param module
   *          the module
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @param uid
   *          the uid
   * @param ip
   *          the ip
   * @return the int
   */
  public static int warn(String system, Class<?> module, String op, String brief, String message, long uid, String ip) {
    return warn(system, module.getName(), op, brief, message, uid, ip);
  }

  /**
   * Warn.
   * 
   * @param system
   *          the system
   * @param module
   *          the module
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @param uid
   *          the uid
   * @param ip
   *          the ip
   * @return the int
   */
  public static int warn(String system, String module, String op, String brief, String message, long uid, String ip) {
    return _log(OpLog.TYPE_WARN, system, module, op, brief, message, uid, ip);
  }

  /**
   * Error.
   * 
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @return the int
   */
  public static int error(String op, String brief, String message) {
    return error(op, brief, message, -1, null);
  }

  /**
   * Error.
   * 
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @param uid
   *          the uid
   * @param ip
   *          the ip
   * @return the int
   */
  public static int error(String op, String brief, String message, long uid, String ip) {
    return error(X.EMPTY, op, brief, message, uid, ip);
  }

  /**
   * Error.
   * 
   * @param module
   *          the module
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @return the int
   */
  public static int error(String module, String op, String brief, String message) {
    return error(module, op, brief, message, -1, null);
  }

  /**
   * Error.
   * 
   * @param module
   *          the module
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @return the int
   */
  public static int error(Class<?> module, String op, String brief, String message) {
    return error(module.getName(), op, brief, message, -1, null);
  }

  /**
   * Error.
   * 
   * @param module
   *          the module
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @param uid
   *          the uid
   * @param ip
   *          the ip
   * @return the int
   */
  public static int error(String module, String op, String brief, String message, long uid, String ip) {
    return error(Global.s("node", X.EMPTY), module, op, brief, message, uid, ip);
  }

  /**
   * Error.
   * 
   * @param module
   *          the module
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @param uid
   *          the uid
   * @param ip
   *          the ip
   * @return the int
   */
  public static int error(Class<?> module, String op, String brief, String message, long uid, String ip) {
    return error(Global.s("node", X.EMPTY), module.getName(), op, brief, message, uid, ip);
  }

  /**
   * Error.
   * 
   * @param system
   *          the system
   * @param module
   *          the module
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @param uid
   *          the uid
   * @param ip
   *          the ip
   * @return the int
   */
  public static int error(String system, Class<?> module, String op, String brief, String message, long uid,
      String ip) {
    return error(system, module.getName(), op, brief, message, uid, ip);
  }

  /**
   * Error.
   * 
   * @param system
   *          the system
   * @param module
   *          the module
   * @param op
   *          the op
   * @param brief
   *          the brief
   * @param message
   *          the message
   * @param uid
   *          the uid
   * @param ip
   *          the ip
   * @return the int
   */
  public static int error(String system, String module, String op, String brief, String message, long uid, String ip) {
    return _log(OpLog.TYPE_ERROR, system, module, op, brief, message, uid, ip);
  }

  public String getSystem() {
    return this.getString("system");
  }

  public String getModule() {
    return this.getString("module");
  }

  public String getOp() {
    return this.getString("op");
  }

  public String getMessage() {
    return this.getString("message");
  }

}
