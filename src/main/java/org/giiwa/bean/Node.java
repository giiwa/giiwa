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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.m._CPU;
import org.giiwa.bean.m._DiskIO;
import org.giiwa.bean.m._Net;
import org.giiwa.conf.Config;
import org.giiwa.conf.Local;
import org.giiwa.dao.Bean;
import org.giiwa.dao.BeanDAO;
import org.giiwa.dao.Beans;
import org.giiwa.dao.Column;
import org.giiwa.dao.Helper;
import org.giiwa.dao.Table;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.misc.Host;
import org.giiwa.misc.IOUtil;
import org.giiwa.misc.Shell;
import org.giiwa.misc.Url;
import org.giiwa.net.client.SFTP;
import org.giiwa.net.client.SSH;
import org.giiwa.net.mq.IStub;
import org.giiwa.net.mq.MQ;
import org.giiwa.net.mq.MQ.Request;
import org.giiwa.node.MockRequest;
import org.giiwa.node.MockResponse;
import org.giiwa.task.BiConsumer;
import org.giiwa.task.Task;
import org.giiwa.web.Controller;
import org.giiwa.web.Language;
import org.giiwa.web.Controller.NameValue;
import org.giiwa.web.Module;
import org.giiwa.web.RequestHelper;
import org.hyperic.sigar.CpuPerc;

/**
 * The code bean, used to store special code linked with s1 and s2 fields
 * table="gi_code"
 * 
 * @author wujun
 *
 */
@Table(name = "gi_node", memo = "GI-集群节点")
public final class Node extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(Node.class);

	public static String NAME = "node." + Local.id();;

	public static final BeanDAO<String, Node> dao = BeanDAO.create(Node.class);

	public static final long LOST = 20 * 1000;

	@Column(memo = "唯一序号")
	public String id;

	public String pid;

	@Column(memo = "IP地址")
	public String ip;

	@Column(memo = "标签")
	public String label;

	@Column(memo = "节点URL")
	public String url;

	@Column(memo = "线程数")
	public int localthreads;

	@Column(memo = "等待的任务数")
	public int localpending;

	@Column(memo = "运行的任务数")
	public int localrunning;

	@Column(memo = "开机时间")
	public long uptime;

	@Column(memo = "本地时间戳", value = "yyyy-MM-dd HH:mm:ss")
	public String timestamp;

	@Column(memo = "内核数")
	public int cores; // cpu cores

	@Column(name = "_usage")
	public int usage; // cpu usage

	@Column(memo = "链接数量")
	public int tcp_established; // sockets count

	@Column(memo = "关闭等待的链接数")
	public int tcp_closewait; // sockets close wait

	@Column(memo = "内存", value = "字节")
	public long mem;

	@Column(memo = "版本号")
	public String giiwa;

	@Column(memo = "应用列表")
	public List<String> apps;

	@Column(memo = "模块列表")
	public List<String> modules;

	@Column(memo = "文件系统读写次数")
	public long dfiletimes;

	@Column(memo = "平均耗时")
	public long dfileavgcost;

	@Column(memo = "最大耗时")
	public long dfilemaxcost;

	@Column(memo = "最小耗时")
	public long dfilemincost;

	public List<String> mac;

	public String color = "green";

	public long lastcheck;

	public String getModules() {
		if (modules != null) {
			Collections.sort(modules);
		}
		return X.join(modules, "\r\n");
	}

	public void setModules(List<String> modules) {
		this.modules = modules;
	}

	public int getState() {

		if (System.currentTimeMillis() - this.lastcheck > LOST)
			return 0;

		return 1;
	}

	public boolean isLocal() {
		return X.isSame(id, Local.id());
	}

	public static void touch(boolean force) {

		Language lang = Language.getLanguage();

		try {
			if (!Helper.isConfigured()) {
				return;
			}

			String id = Local.id();
			Node n = dao.load(id);
			if (n != null) {
				// check ip
				List<String> s1 = Arrays.asList(X.split(n.ip, ","));
				List<String> s2 = Arrays.asList(X.split(Host.getLocalip(), ","));

				boolean found = false;
				for (String s : s2) {
					if (s1.contains(s)) {
						found = true;
						break;
					}
				}

				if (!found) {
					// the node id is bad
					GLog.applog.error("node", "init", "bad node", null);
//					id = UID.uuid();
//
//					Configuration conf = Config.getConf();
//					conf.setProperty("node.id", id);
//
//					Config.save();
				}
			}

			// update
			V v = V.create();
			v.append("localthreads", Task.activeThread());
			v.append("localrunning", Task.tasksInRunning());
			v.append("localpending", Task.tasksInQueue());
			v.append("timestamp", lang.format(System.currentTimeMillis(), "HH:mm:ss") + "<br>"
					+ lang.format(System.currentTimeMillis(), "yyyy-MM-dd"));
			v.append("lastcheck", System.currentTimeMillis());

//				NetStat ns = Host.getNetStat();
//				if (ns != null) {
//					v.append("tcp_established", ns.getTcpEstablished());
//					v.append("tcp_closewait", ns.getTcpCloseWait() + ns.getTcpTimeWait());
//				}
//
			// v.append("lasttime", System.currentTimeMillis());

			CpuPerc[] cc = Host.getCpuPerc();
			double user = 0;
			double sys = 0;
			for (CpuPerc c : cc) {
				/**
				 * user += c1.sys; <br/>
				 * user += c1.user;<br/>
				 * wait += c1.wait;<br/>
				 * nice += c1.nice;<br/>
				 * idle += c1.idle;<br/>
				 */
				user += c.getUser();
				sys += c.getSys();
			}
			v.append("_usage", (int) ((user + sys) * 100 / cc.length));

			if (dao.exists(id)) {
				if (force) {
					getNodeInfo(v);
				}
				dao.update(id, v);

			} else {
				// create
				getNodeInfo(v);
				dao.insert(v.append(X.ID, id).append("label", Config.getConf().getString("node.name")));
			}

			long time = Stat.tomin();

			Node e = dao.load(id);
			if (e == null) {
				return;
			}

			// stat
			long[] ff = new long[15];
			ff[0] = e.usage;
			ff[1] = e.getLong("globaltasks");
			ff[2] = e.getLong("localthreads");
			ff[3] = e.getLong("localrunning");
			ff[4] = e.getLong("localpending");
			ff[5] = e.getLong("dfiletimes");
			ff[6] = e.getLong("dfilemaxcost");
			ff[7] = e.getLong("dfilemincost");
			ff[8] = e.getLong("dfileavgcost");
			ff[9] = e.getLong("dfiletimes_c");
			ff[10] = e.getLong("dfilemaxcost_c");
			ff[11] = e.getLong("dfilemincost_c");
			ff[12] = e.getLong("dfileavgcost_c");
			ff[13] = e.getLong("tcp_established");
			ff[14] = e.getLong("tcp_closewait");

			Stat.snapshot(time, "node.load", W.create().and("dataid", id), V.create().append("dataid", id), ff);

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private static void getNodeInfo(V v) {

		try {

			v.append("uptime", Controller.UPTIME).append("ip", Host.getLocalip());

			v.append("pid", Shell.pid());

			List<org.giiwa.web.Module> actives = new ArrayList<org.giiwa.web.Module>();
			org.giiwa.web.Module m = org.giiwa.web.Module.home;
			while (m != null) {
				actives.add(m);
				m = m.floor();
			}

			v.append("modules", X.asList(actives, e -> {
				if (X.isSame("default", ((Module) e).getName())) {
					return null;
				}
				Module e1 = ((Module) e);
				return e1.getName() + ":" + e1.getVersion() + ":" + e1.getBuild();
			}));

			String dockerid = Host.getDockerID();
			if (!X.isEmpty(dockerid)) {
				v.append("mac", dockerid);
			} else {
				v.append("mac", Host.getMAC());
			}

			OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

			v.append("cores", os.getAvailableProcessors());

			v.append("giiwa", Module.load("default").getVersion() + "." + Module.load("default").getBuild());
			v.append("os", Host.getOS().getName());
			v.append("mem", Host.getMem().getTotal());

//			v.append("url", FileServer.URL.replace("0.0.0.0", Host.getLocalip()));

			v.append("_usage", (int) (_CPU.usage()));

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public static boolean add(String host, String user, String passwd, String modules, String alias,
			BiConsumer<Integer, String> func) {

		// ssh
		SSH ssh = SSH.create();
		SFTP sf = SFTP.create();
		try {
			// TODO
			File jdk = new File("/home/jdk-9.0.4");
			if (!jdk.exists() || !jdk.isDirectory()) {
				func.accept(201, "no JDK found in local!");
				return false;
			}

			File giiwa = new File("/home/giiwa");
			if (!giiwa.exists() || !giiwa.isDirectory()) {
				func.accept(201, "no giiwa found in local!");
				return false;
			}

			func.accept(0, "connecting [" + host + "] ...");
			sf.open("sftp://" + host + "?username=" + user + "&passwd=" + passwd);
			ssh.open("ssh://" + host + "?username=" + user + "&passwd=" + passwd);

			{
				String s1 = ssh.run("python2 -V");
				if (X.isEmpty(s1) || !s1.contains("Python 2.7")) {
					func.accept(201, "no python2");
					return false;
				}
			}

			ssh.run("rm -rf /home/jdk-9.0.4");
			ssh.run("rm -rf /home/giiwa");
			ssh.run("rm -rf /etc/appdog");

			// copy JDK
			func.accept(0, "copying JDK ...");
			sf.put(jdk, "/home/", f -> true);
			ssh.run("chmod ugo+x /home/jdk-9.0.4/bin/*");

			// config /etc/profile
			Temp t1 = sf.get("/etc/profile");

			String prof = IOUtil.read(t1.getInputStream(), "UTF-8");
			if (prof.indexOf("JAVA_HOME") == -1) {
				prof = prof.replaceAll("\r", X.EMPTY);
				prof += "\nexport JAVA_HOME=/home/jdk-9.0.4";
				prof += "\nexport PATH=$JAVA_HOME/bin:$PATH\n";

				IOUtil.write(t1.getOutputStream(), "UTF-8", prof);
				sf.put(new File("/etc/profile"), t1.getInputStream());
			}

			// copy giiwa
			// check modules
			{
				String[] ss = X.split(modules, "[,; ]");
				Set<String> s1 = new HashSet<String>(X.asList(ss, s -> s.toString()));
				s1.add("default");
				s1.add("WEB-INF");
				func.accept(0, "copying giiwa ...");
				sf.put(giiwa, "/home/", f -> {
					if (f.startsWith("/home/giiwa/modules/")) {
						String s = f.replace("/home/giiwa/modules/", X.EMPTY);
						int i = s.indexOf("/");
						if (i > 0) {
							s = s.substring(0, i);
						}
						if (!s1.contains(s)) {
							return false;
						}
					}
					return true;
				});
			}

			ssh.run("chmod ugo+x /home/giiwa/giiwa");
			ssh.run("chmod ugo+x /home/giiwa/appdog/appdog");
			ssh.run("chmod ugo+x /home/giiwa/bin/*");

			// create giiwa.properties
			func.accept(0, "setup giiwa ...");
			PropertiesConfiguration prop = new PropertiesConfiguration();
			{
				Reader in = new InputStreamReader(new FileInputStream("/home/giiwa/giiwa.properties"), "UTF-8");
				try {
					prop.read(in);
				} finally {
					X.close(in);
				}
			}
			// node.id
			// node.name
			// dfile.bind
			prop.setProperty("node.id", UID.uuid());
			prop.setProperty("node.name", alias);
			prop.setProperty("dfile.bind", "tcp://" + Url.create("sftp://" + host).getIp() + ":9091");
			{
				Temp t = Temp.create("giiwa.properties");
				Writer out = t.getWriter();
				try {
					prop.getLayout().save(prop, out);
				} finally {
					X.close(out);
				}
				sf.put(t.getInputStream(), "/home/giiwa");
			}

			// etc/hosts
			{
				InputStream in = new FileInputStream("/etc/hosts");
				String s = IOUtil.read(in, "UTF-8");
				X.close(in);
				int i = s.indexOf("##giiwa");
				if (i > 0) {
					s = s.substring(i);
					t1 = sf.get("/etc/hosts");
					String s1 = IOUtil.read(t1.getInputStream(), "UTF-8");
					i = s1.indexOf("##giiwa");
					if (i > 0) {
						s = s1.substring(0, i) + s;
					} else {
						s = s1 + "\n\n" + s;
					}
				}
				IOUtil.write(t1.getOutputStream(), "UTF-8", s);
				sf.put(t1.getInputStream(), "/etc/hosts");
			}

			// install appdog
			func.accept(0, "installing giiwa ...");
			ssh.run("cd /home/giiwa; ./giiwa install");

			// start the giiwa
			func.accept(0, "starting giiwa ...");
			ssh.run("service appdog start");
			func.accept(200, "started giiwa");

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			func.accept(201, e.getMessage());
			return false;
		} finally {
			X.close(sf, ssh);
		}

		return true;
	}

	public List<?> name(String tag) {
		// tag=diskio, net
		if (X.isSame(tag, "diskio")) {
			List<?> l1 = _DiskIO.dao.distinct("path", W.create().and("node", id)
					.and("updated", System.currentTimeMillis() - X.AMINUTE * 10, W.OP.gte).sort("path", 1));
			l1.remove(null);
			l1.remove(X.EMPTY);
			return l1;
		} else if (X.isSame(tag, "net")) {
			List<?> l1 = _Net.dao.distinct("name", W.create().and("node", id)
					.and("updated", System.currentTimeMillis() - X.AMINUTE * 10, W.OP.gte).sort("inet", 1));
			l1.remove(null);
			l1.remove(X.EMPTY);
			return l1;
		}
		return null;
	}

	public boolean isAlive() {
		return this.getUpdated() > System.currentTimeMillis() - Node.LOST;
	}

	public void forward(String uri, RequestHelper req, HttpServletResponse resp, String method) throws Exception {

		JSON j1 = JSON.create();
		j1.append("uri", uri);
		j1.append("m", method);

		JSON head = JSON.create();
		NameValue[] h1 = req.heads();
		if (h1 != null) {
			for (NameValue e : h1) {
				String s1 = e.name;
				head.append(s1, e.value);
			}
		}

		JSON body = req.json().append("__node", id);

		j1.append("head", head);
		j1.append("body", body);

		if (log.isDebugEnabled()) {
			log.debug("forwarding: " + j1.toPrettyString());
		}

		JSON r1 = MQ.callQueue("node." + id, MQ.Request.create().put(j1), X.AMINUTE);

//		log.warn("resp=" + r1);

		resp.setStatus(r1.getInt("status"));
		head = JSON.fromObject(r1.get("head"));
		if (head != null) {
			for (String s1 : head.keySet()) {
				resp.addHeader(s1, head.getString(s1));
			}
		}

		OutputStream out = resp.getOutputStream();
		byte[] bb = r1.getBytes("out");
		if (bb != null) {
			out.write(bb);
		}
		out.flush();

	}

	public static void init() {
		try {
			stub.bindAs(MQ.Mode.QUEUE);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Task.schedule(t -> {
				init();
			}, 3000);
		}

	}

	private static IStub stub = new IStub(NAME) {

		@Override
		public void onRequest(long seq, Request req) {

//			log.warn("got message, ");

			try {
				JSON r1 = req.get();

				if (log.isDebugEnabled()) {
					log.debug("got forward: " + r1.toPrettyString());
				}

				String m = r1.getString("m");
				String uri = r1.getString("uri");
				JSON head = JSON.fromObject(r1.get("head"));
				JSON body = JSON.fromObject(r1.get("body"));

//				log.warn("uri=" + uri + ", m=" + m + ", head=" + head + ", body=" + body);

				TimeStamp t = TimeStamp.create();

				MockResponse resp1 = MockResponse.create();
				Controller.process(uri, MockRequest.create(uri, head, body), resp1, m, t);
				X.close(resp1);

				JSON r2 = JSON.create();
				r2.put("status", resp1.status);
				r2.put("head", resp1.head);
				r2.put("out", resp1.out.toByteArray());

				Request r3 = Request.create().put(r2);

//				log.warn("resp=" + r1 + ", size=" + r3.data.length + ", r3=" + r3.get());

				req.reply(r3);

			} catch (Throwable e1) {
				log.error(e1.getMessage(), e1);
			}

		}

	};

	public static Beans<Node> alive() {
		return dao.load(W.create().and("lastcheck", System.currentTimeMillis() - LOST, W.OP.gte), 0, 1024);
	}

}
