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
package org.giiwa.app.web.admin;

import java.io.File;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.giiwa.bean.GLog;
import org.giiwa.bean.Node;
import org.giiwa.bean.Role;
import org.giiwa.conf.Config;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.Beans;
import org.giiwa.dao.Helper;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.misc.Digest;
import org.giiwa.misc.Host;
import org.giiwa.misc.IOUtil;
import org.giiwa.net.mq.MQ;
import org.giiwa.net.mq.MQ.Request;
import org.giiwa.snmp.SampleAgent;
import org.giiwa.task.Task;
import org.giiwa.web.*;
import org.giiwa.web.Module;

/**
 * web api: /admin/setting <br>
 * use to custom setting, all module configuration MUST inherit from this class,
 * and override the "set" and "get" method,<br>
 * required "access.config.admin"
 * 
 * @author joe
 *
 */
public class setting extends Controller {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static List<String> names = new ArrayList<String>();
	private static Map<String, Class<? extends setting>> settings = new HashMap<String, Class<? extends setting>>();

	/**
	 * Register.
	 *
	 * @param seq  the seq
	 * @param name the name
	 * @param m    the m
	 */
	final public static void register(int seq, String name, Class<? extends setting> m) {
		if (seq < 0 || seq >= names.size()) {
			names.add(name);
		} else {
			names.add(seq, name);
		}
		settings.put(name, m);
	}

	final public static void register(String name, Class<? extends setting> m) {
		register(-1, name, m);
	}

	/**
	 * Reset.
	 *
	 * @param name the name
	 * @return the object
	 */
	@Path(path = "reset/(.*)", login = true, access = "access.config.admin")
	final public void reset(String name) {
		Class<? extends setting> c = settings.get(name);
		if (log.isDebugEnabled())
			log.debug("/reset/" + c);

		if (c != null) {
			try {
				setting s = c.getDeclaredConstructor().newInstance();
				s.req = this.req;
				s.resp = this.resp;
				s.login = this.login;
				s.lang = this.lang;
				s.module = this.module;
				s.reset();

				GLog.oplog.warn("setting", "reset", "reset " + name, login, this.ip());

			} catch (Exception e) {
				log.error(name, e);
				GLog.oplog.error(setting.class, "reset", e.getMessage(), e, login, this.ip());
			}
		}
	}

	@Path(path = "getconf", login = true, access = "access.config.admin")
	final public void getconf() {

		File f1 = new File(Controller.GIIWA_HOME + "/giiwa.properties");
		if (f1.exists()) {
			String s = IOUtil.read(f1, "UTF-8");
			this.set("text", s);
		}

		this.send(200);

	}

	@Path(path = "editconf", login = true, access = "access.config.admin")
	final public void editconf() {

		File f1 = new File(Controller.GIIWA_HOME + "/giiwa.properties");
		String s = this.getHtml("text");

		IOUtil.write(f1, "UTF-8", s);
		GLog.oplog.warn(setting.class, "editconf", "update giiwa.properties", login, this.ip());

		this.set(X.MESSAGE, lang.get("save.success")).send(200);

	}

	/**
	 * Gets the.
	 *
	 * @param name the name
	 * @return the object
	 */
	@Path(path = "get/(.*)", login = true, access = "access.config.admin")
	public String get1(String name) {
		Class<? extends setting> c = settings.get(name);
		if (log.isDebugEnabled())
			log.debug("/get/" + c);

		if (c != null) {
			try {
				setting s = c.getDeclaredConstructor().newInstance();
				s.copy(this);
				s.get();

				s.set("lang", lang);
				s.set("module", module);
				s.set("name", name);
				s.set("__node", this.getString("__node"));
				s.set("settings", names);
				s.show("/admin/setting.html");

			} catch (Exception e) {
				log.error(name, e);
				GLog.oplog.error(setting.class, "get", e.getMessage(), e, login, this.ip());

				this.show("/admin/setting.html");
			}
		}

		return null;
	}

	/**
	 * Sets the.
	 *
	 * @param name the name
	 */
	@Path(path = "set/(.*)", login = true, access = "access.config.admin")
	final public void set(String name) {

		Class<? extends setting> c = settings.get(name);
		if (log.isDebugEnabled())
			log.debug("/set/" + c);

		if (c != null) {
			try {
				setting s = c.getDeclaredConstructor().newInstance();
				s.copy(this);
				s.set("lang", lang);
				s.set("module", module);
				s.set("__node", this.getString("__node"));
				s.set("name", name);
				s.set("settings", names);
				s.set();

				GLog.oplog.warn(name, "set", "v=" + this.json(), login, this.ip());

				// s.show("/admin/setting.html");
			} catch (Exception e) {
				log.error(name, e);
				GLog.oplog.error(name, "set", e.getMessage() + ", v=" + this.json(), e, login, this.ip());

				this.show("/admin/setting.html");
			}
		}
	}

	/**
	 * invoked when post setting form.
	 */
	public void set() {

	}

	/**
	 * invoked when reset called.
	 */
	public void reset() {
		this.set(X.MESSAGE, "ok").send(200);
	}

	public void settingPage(String view) {
		this.set("page", view);
	}

	/**
	 * invoked when get the setting form.
	 */
	public void get() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model.onGet()
	 */
	@Path(login = true, access = "access.config.admin")
	public final void onGet() {

		if (!names.isEmpty()) {
			String name = names.get(0);
			this.set("name", name);
			get1(name);
			return;
		}

		this.print("not find page");

	}

	public static class system extends setting {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.giiwa.app.web.admin.setting.set()
		 */
		@Override
		public void set() {

			String lang1 = this.getString("language");
			Global.setConfig("language", lang1);
			Language.setLocale(lang1);

			Global.setConfig("admin.ip", this.getHtml("admin.ip"));
			Global.setConfig("home.uri", this.getHtml("home_uri"));
			Local.setConfig("home.uri.1", this.getHtml("home.uri.1"));

			Global.setConfig("cluster.code", this.getLong("cluster.code"));
			Global.setConfig("uid.next.s1", this.getLong("uid.next.s1"));

			Global.setConfig("site.group", this.getString("site.group"));

			Global.setConfig("dfile.copies", this.getInt("dfile.copies", 0));
			Global.setConfig("f.upload.login", X.isSame(this.getString("f.upload.login"), "on") ? 1 : 0);
			Global.setConfig("f.g.login", X.isSame(this.getString("f.g.login"), "on") ? 1 : 0);
			Global.setConfig("f.g.online", X.isSame(this.getString("f.g.online"), "on") ? 1 : 0);

			Global.setConfig("zookeeper.server", this.getString("zookeeper.server"));

			Global.setConfig("user.captcha", X.isSame(this.getString("user_captcha"), "on") ? 1 : 0);
			Global.setConfig("user.token", X.isSame(this.getString("user_token"), "on") ? 1 : 0);
			Global.setConfig("user.passwd", X.isSame("on", this.getString("user.passwd")) ? 1 : 0);
			Global.setConfig("session.baseip", X.isSame("on", this.getString("session.baseip")) ? 1 : 0);
			long alive = this.getLong("session.alive");
			if (alive == 0) {
				alive = -1;
			}
			Global.setConfig("session.alive", alive);

			Global.setConfig("cookie.samesite", this.get("cookie.samesite"));

			Global.setConfig("user.login.failed.lock",
					X.isSame("on", this.getString("user.login.failed.lock")) ? 1 : 0);
			Global.setConfig("user.login.failed.times", this.getLong("user.login.failed.times"));

			long locktime = this.getLong("user.login.failed.lock.time");
			if (locktime < 1) {
				locktime = 1;
			}
			Global.setConfig("user.login.failed.lock.time", locktime);

			Global.setConfig("user.login.failed.mode", this.get("user.login.failed.mode"));

			Global.setConfig("user.name.rule", this.getHtml("user_name"));
			Global.setConfig("user.passwd.rule", this.getHtml("user_passwd"));

			Global.setConfig("user.system", this.getString("user_system"));
			Global.setConfig("user.role", this.getString("user_role"));
			Global.setConfig("user.name.rule.tips", this.getString("user.name.rule.tips"));
			Global.setConfig("user.passwd.rule.tips", this.getString("user.passwd.rule.tips"));

			Global.setConfig("cross.domain", this.getString("cross_domain"));
			Global.setConfig("cross.header", this.getString("cross_header"));
			Global.setConfig("html.source", this.getHtml("html.source"));
			Global.setConfig("user.passwd.expired", this.getInt("user.passwd.expired"));

			Global.setConfig("ntp.server", this.getString("ntpserver"));
			Global.setConfig("http.proxy", this.getString("http.proxy"));

			Global.setConfig("db.optimizer", X.isSame("on", this.getString("db.optimizer")) ? 1 : 0);
			Global.setConfig("security.task", X.isSame("on", this.getString("security.task")) ? 1 : 0);
			Global.setConfig("oplog.level", this.getInt("oplog.level"));
			Global.setConfig("perf.moniter", X.isSame("on", this.getString("perf.moniter")) ? 1 : 0);
			Global.setConfig("web.debug", X.isSame("on", this.getString("web.debug")) ? 1 : 0);
			Global.setConfig("glog.keep.days", this.getInt("glog.keep.days"));
			Global.setConfig("web.bg.watermark", X.isSame("on", this.getString("web.bg.watermark")) ? 1 : 0);
			Global.setConfig("iframe.options", this.getString("iframe.options"));

			Global.setConfig("glog.rsyslog", X.isSame("on", this.getString("glog.rsyslog")) ? 1 : 0);
			Global.setConfig("glog.rsyslog.host", this.getString("glog.rsyslog.host"));
			Global.setConfig("glog.rsyslog.port", this.getLong("glog.rsyslog.port"));

			String url = this.getString("site_url").trim();
			while (url.endsWith("/")) {
				url = url.substring(0, url.length() - 1);
			}
			Global.setConfig("site.url", url);
			Global.setConfig("site.image", this.getString("site.image"));

			Global.setConfig("user.login.sso", X.isSame("on", this.getString("user.login.sso")) ? 1 : 0);
			Global.setConfig("user.login.sso.mode", this.getString("user.login.sso.mode"));
			Global.setConfig("user.login.sso.role", this.getString("user.login.sso.role"));
			Global.setConfig("user.login.sso.expired", this.getLong("user.login.sso.expired"));

			Global.setConfig("module.center", X.isSame(this.getString("module_center"), "on") ? 1 : 0);

			Helper.enableOptmizer();

			try {

				MQ.topic(Task.MQNAME, Request.create().put(new Task() {

					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void onExecute() {
						Language.inst = null;
						Language.getLanguage();
					}

				}));

			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}

			this.send(JSON.create().append(X.MESSAGE, lang.get("save.success")).append(X.STATE, 201));
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.giiwa.app.web.admin.setting.get()
		 */
		@Override
		public void get() {

			this.set("language", Global.getString("language", "zh_cn"));
			this.set("level", Global.getString("run.level", "debug"));
			this.set("user_system", Global.getString("user.system", "close"));
			this.set("user_role", Global.getString("user.role", "N/A"));
			this.set("cross_domain", Global.getString("cross.domain", "no"));
			this.set("cross_header", Global.getString("cross.header", "Content-Type, accept, Origin"));

			this.set("cache_url", Config.getConf().getString("cache.url", null));
			this.set("db_url", Config.getConf().getString("db.url", null));
			this.set("db_user", Config.getConf().getString("db.user", null));

			this.set("roles", Role.load(0, 100));

			this.set("machineid", Digest.md5(Host.getMAC() + "/" + Local.id()));

			this.set("sso_role", Global.getString("user.login.sso.role", ""));

			try {
				Beans<Node> l1 = Node.dao.load(
						W.create().and("lastcheck", System.currentTimeMillis() - X.ADAY, W.OP.gte).sort("created"), 0,
						1024);
				String code = "";
//				for (Node o : l1) {
				Node o = Local.node();
				if (!X.isEmpty(o.mac)) {
					for (String mac : o.mac) {
						SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
						random.setSeed((o.id + "/" + mac).getBytes());
						byte s1 = (byte) (random.nextInt(117) + 10);
						if (!code.contains(Byte.toString(s1))) {
							code += s1 + "|";
						}
					}
				}
//				}
				if (X.isEmpty(code)) {
					this.set("serial", "nodes=" + l1.size());
				} else {
					this.set("serial", code.replaceAll("\\|", X.EMPTY).toString());
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				this.set("serial", e.getMessage());
			}

			this.settingPage("/admin/setting.system.html");

		}

	}

	public static class smtp extends setting {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.giiwa.app.web.admin.setting.set()
		 */
		@Override
		public void set() {

			Global.setConfig("mail.protocol", this.getString("mail.protocol"));
			Global.setConfig("mail.host", this.getString("mail.host"));
			Global.setConfig("mail.email", this.getString("mail.email"));
			Global.setConfig("mail.title", this.getString("mail.title"));
			Global.setConfig("mail.user", this.getString("mail.user"));
			Global.setConfig("mail.passwd", this.getString("mail.passwd"));

			this.send(JSON.create().append(X.MESSAGE, lang.get("save.success")).append(X.STATE, 201));
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.giiwa.app.web.admin.setting.get()
		 */
		@Override
		public void get() {

			this.set("page", "/admin/setting.smtp.html");
		}

	}

	public static class snmp extends setting {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.giiwa.app.web.admin.setting.set()
		 */
		@Override
		public void set() {

			Local.setConfig("snmp.enabled", X.isSame(this.getString("snmp.enabled"), "on") ? 1 : 0);
			Local.setConfig("snmp.listen", this.getString("snmp.listen"));
			Local.setConfig("snmp.port", this.getInt("snmp.port"));

			Global.setConfig("snmp.version", this.getInt("snmp.version"));
			Global.setConfig("snmp.username", this.getInt("snmp.username"));
			Global.setConfig("snmp.password", this.getInt("snmp.password"));

			SampleAgent.start();

			this.send(JSON.create().append(X.MESSAGE, lang.get("save.success")).append(X.STATE, 201));
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.giiwa.app.web.admin.setting.get()
		 */
		@Override
		public void get() {

			this.set("page", "/admin/setting.snmp.html");
		}

	}

	public static class counter extends setting {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.giiwa.app.web.admin.setting.set()
		 */
		@Override
		public void set() {
			Global.setConfig("site.counter", this.getHtml("counter"));

			this.send(JSON.create().append(X.MESSAGE, lang.get("save.success")).append(X.STATE, 201));
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.giiwa.app.web.admin.setting.get()
		 */
		@Override
		public void get() {
			// this.set("counter", ConfigGlobal.s("site.counter", X.EMPTY));
			this.set("page", "/admin/setting.counter.html");
		}
	}

	@Path(path = "list", login = true, access = "access.config.admin")
	public final void list() {

	}

	@Path(path = "get1", login = true, access = "access.config.admin")
	public void get1() {
		String name = this.get("name");
		Global e = Global.dao.load(name);
		if (e != null) {
			this.print(e.json());
		}
	}

	@Path(path = "delete1", login = true, access = "access.config.admin")
	public void delete1() {
		String name = this.get("name");
		Global.dao.delete(name);
		this.print("ok");
	}

}
