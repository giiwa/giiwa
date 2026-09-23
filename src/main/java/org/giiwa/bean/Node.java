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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Stat.SIZE;
import org.giiwa.bean.m._DiskIO;
import org.giiwa.bean.m._Net;
import org.giiwa.cache.Cache;
import org.giiwa.conf.Config;
import org.giiwa.conf.Global;
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
import org.giiwa.task.Function;
import org.giiwa.task.Runner;
import org.giiwa.task.Task;
import org.giiwa.web.Controller;
import org.giiwa.web.Language;
import org.giiwa.web.Controller.NameValue;
import org.giiwa.web.GiiwaServlet;
import org.giiwa.web.Module;
import org.giiwa.web.RequestHelper;

import com.sun.management.OperatingSystemMXBean;

import jakarta.servlet.http.HttpServletResponse;

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

	public static String NAME = "node." + Local.id();

	public static final BeanDAO<String, Node> dao = BeanDAO.create(Node.class);

	public static final long LOST = 20 * 1000;

	@Column(memo = "主键", unique = true, size = 64)
	public String id;

	@Column(memo = "进程号", size = 50)
	public String pid;

	@Column(memo = "IP地址", size = 50)
	public String ip;

	@Column(memo = "标签", size = 100)
	public String label;

	@Column(memo = "节点URL", size = 100)
	public String url;

	@Column(memo = "标签", size = 10)
	public String tag;

	@Column(memo = "线程数")
	public int localthreads;

	@Column(memo = "等待的任务数")
	public int localpending;

	@Column(memo = "运行的任务数")
	public int localrunning;

	@Column(memo = "延迟的任务数")
	public int localdelay;

	@Column(memo = "总请求数")
	public long totalrequest;

	@Column(memo = "在线请求数")
	public int online;

	@Column(memo = "类型", value = "1: 远程节点, 0: 本地节点")
	public int type;

	@Column(memo = "TPS")
	public int tps;

	@Column(memo = "开机时间")
	public long uptime;

	@Column(memo = "本地时间戳", value = "HH:mm:ss yyyy-MM-dd", size = 50)
	public String timestamp;

	@Column(memo = "内核数")
	public int cores; // cpu cores

	@Column(memo = "内核数")
	public int computingpower; // cpu cores

	@Column(memo = "CPU主频")
	public double ghz;

	@Column(name = "_usage", memo = "CPU使用率")
	public int usage; // cpu usage

	@Column(memo = "内存使用率")
	public int mem_usage;

	@Column(memo = "链接数量")
	public int tcp_established; // sockets count

	@Column(memo = "关闭等待的链接数")
	public int tcp_closewait; // sockets close wait

	@Column(memo = "内存", value = "字节")
	public long mem;

	@Column(memo = "版本号", size = 50)
	public String giiwa;

	@Column(memo = "应用列表")
	public List<String> apps;

	@Column(memo = "模块列表")
	public List<String> modules;

	@Column(memo = "文件系统读写次数")
	public long dfiletimes;

	@Column(memo = "磁盘使用率, >90红", value = "/ - 60%<br>/data - 60%", size = 100)
	public String disk;

	@Column(memo = "平均耗时")
	public long dfileavgcost;

	@Column(memo = "最大耗时")
	public long dfilemaxcost;

	@Column(memo = "最小耗时")
	public long dfilemincost;

	public List<String> mac;

	@Column(memo = "颜色", size = 10)
	public String color = "green";

	@Column(memo = "缓存系统", size = 20)
	public String cache = "local";

	@Column(memo = "缓存系统错误", value = "1:yes")
	public int cache_error;

	@Column(memo = "MQ系统", size = 20)
	public String mq = "local";

	@Column(memo = "MQ错误", value = "1:yes")
	public int mq_error;

	@Column(memo = "最后检查时间")
	public long lastcheck;

	/**
	 * 静态本节点CPU使用率
	 */
	private static transient long _lastcheck = 0;
	public static int cpusage = 0;

	/**
	 * 获取所有模块名
	 * 
	 * @return
	 */
	public String getModules() {
		if (modules != null) {
			Collections.sort(modules);
		}
		return X.join(modules, "\r\n");
	}

	public void setModules(List<String> modules) {
		this.modules = modules;
	}

	/**
	 * 获取节点状态
	 * 
	 * @return 0 - 离线， 1 = 在线
	 */
	public int getState() {

		if (Global.now() - this.lastcheck > LOST)
			return 0;

		return 1;
	}

	/**
	 * 是否本地节点
	 * 
	 * @return
	 */
	public boolean isLocal() {
		return X.isSame(id, Local.id());
	}

	/**
	 * 更新节点状态
	 * 
	 * @param force - true: 完全更新
	 */
	public static void touch(boolean force) {

		Language lang = Language.getLanguage();

		try {
			if (!Helper.isConfigured()) {
				return;
			}

			String id = Local.id();
			Node n = dao.load(id);
			if (n != null) {
				// 检查IP地址
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
					GLog.applog.error(X.NODE, "init", "bad node", null);
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
			v.append("localdelay", Task.tasksDelay());
			v.append("timestamp",
					lang.format(Global.now(), "HH:mm:ss") + "<br>" + lang.format(Global.now(), "yyyy-MM-dd"));
			v.append("lastcheck", Global.now());
			v.append("online", GiiwaServlet.online());
			v.append("tps", GiiwaServlet.tps());

//				NetStat ns = Host.getNetStat();
//				if (ns != null) {
//					v.append("tcp_established", ns.getTcpEstablished());
//					v.append("tcp_closewait", ns.getTcpCloseWait() + ns.getTcpTimeWait());
//				}
//
			// v.append("lasttime", Global.now());

			v.append("mq_error", MQ.error);
			v.append("cache_error", Cache.error);

//			v.append("type", Config.getConf().getInt("node.type", 0)); // 默认本地节点
			v.append("cores", Runner.cores);
			v.append("computingpower", Runner.computingpower);
			v.append("ghz", Runner.ghz);
			v.append("mq", MQ.type());
			v.append("cache", Cache.type());

			try {
				/**
				 * CPU使用率
				 */
				cpusage();
				v.append("_usage", cpusage);
			} catch (Throwable e) {
//				ignore
				// log.error(e.getMessage(), e);

			}

//			_raft(id, v);

			if (_created || dao.exists(id)) {
				if (force) {
					getNodeInfo(v, lang);
				}
				dao.update(id, v);

			} else {
				// create
				getNodeInfo(v, lang);
				dao.insert(v.append(X.ID, id).append("label", Config.getConf().getString("node.name")));
				_created = true;
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

			Stat.snapshot(time, "node.load", SIZE.min, W.create().and("dataid", id), V.create().append("dataid", id),
					ff);

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private static transient boolean _created = false;

//	/**
//	 * 基于Raft的角色设置
//	 * 
//	 * @param v
//	 */
//	transient static boolean _optimized_raft = false;

//	private static void _raft(String id, V v) {
//		// TODO Auto-generated method stub
//		/**
//		 * 1， 检查Leader是否在线？
//		 */
//		W q = W.create().and("lastcheck", Global.now() - LOST, W.OP.gte).and("role", "Leader");
//		if (!_optimized_raft) {
//			dao.optimize(q);
//			_optimized_raft = true;
//		}
//		Node e = dao.load(q);
//		if (e != null) {
//			// 存在
//			if (!X.isSame(e.id, id)) {
//				/**
//				 * 不是本节点
//				 */
//				v.append("role", "Follower");
//			}
//			return;
//		}
//
//		/**
//		 * 2, 检查是否存在 Candidate
//		 */
//		q = W.create().and("lastcheck", Global.now() - LOST, W.OP.gte).and("role", "Candidate").sort(X.ID);
//		e = dao.load(q);
//		if (e != null) {
//			// 存在
//			if (X.isSame(e.id, id)) {
//				v.append("role", "Leader");
//			} else {
//				// 推举为Leader
//				dao.update(e.id, V.create().append("role", "Leader"));
//				v.append("role", "Follower");
//			}
//			return;
//		}
//
//		/**
//		 * 3, 推举自己为 Candidate
//		 */
//		v.append("role", "Candidate");
//
//	}

	@SuppressWarnings("deprecation")
	private static void getNodeInfo(V v, Language lang) {

		try {

			v.append("giiwa", Module.load("default").getVersion() + "." + Module.load("default").getBuild());
			v.append("pid", Shell.pid());
			v.append("tag", "giiwa");

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

			v.append("uptime", Controller.UPTIME);
			v.append("_usage", cpusage());
			v.append(X.IP, Host.getLocalip());

			String dockerid = Host.getDockerID();
			if (!X.isEmpty(dockerid)) {
				v.append("mac", dockerid);
			} else {
				v.append("mac", Host.getMAC());
			}

			v.append("os", System.getProperty("os.name"));

			// 获取物理内存总量（以字节为单位）
			OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
			long total = ((com.sun.management.OperatingSystemMXBean) osBean).getTotalPhysicalMemorySize();
			v.append("mem", total);

			// 获取磁盘使用率
			StringBuilder sb = new StringBuilder();
			for (String path : new String[] { "/", "/data" }) {
				File file = new File(path);

				// 总空间、已用、可用（单位Byte）
				long totalByte = file.getTotalSpace();
				long usableByte = file.getUsableSpace();
				long usedByte = totalByte - usableByte;

				// 使用率
				int usageRate = (int) (totalByte == 0 ? 0 : usedByte * 100 / totalByte);
				if (!sb.isEmpty()) {
					sb.append("<br>");
				}
				if (usageRate > 90) {
					sb.append(
							"<i style='color:red'>" + path + ":" + lang.size(totalByte) + " - " + usageRate + "%</i>");
				} else {
					sb.append(path + ":" + lang.size(totalByte) + " - " + usageRate + "%");
				}
			}
			v.append("disk", sb.toString());

//			v.append("url", FileServer.URL.replace("0.0.0.0", Host.getLocalip()));

		} catch (Throwable e) {
			// ignore
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * 添加新节点
	 * 
	 * @param host    - 主机地址
	 * @param user    - ssh用户名
	 * @param passwd  - ssh用户密码
	 * @param modules - 模块名
	 * @param alias   - 别名
	 * @param func    - 会调函数
	 * @return
	 */
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

			String prof = IOUtil.read(t1.getInputStream(), X.UTF8);
			if (prof.indexOf("JAVA_HOME") == -1) {
				prof = prof.replaceAll("\r", X.EMPTY);
				prof += "\nexport JAVA_HOME=/home/jdk-9.0.4";
				prof += "\nexport PATH=$JAVA_HOME/bin:$PATH\n";

				IOUtil.write(t1.getOutputStream(), X.UTF8, prof);
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
				Reader in = new InputStreamReader(new FileInputStream("/home/giiwa/giiwa.properties"), X.UTF8);
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
				String s = IOUtil.read(in, X.UTF8);
				X.close(in);
				int i = s.indexOf("##giiwa");
				if (i > 0) {
					s = s.substring(i);
					t1 = sf.get("/etc/hosts");
					String s1 = IOUtil.read(t1.getInputStream(), X.UTF8);
					i = s1.indexOf("##giiwa");
					if (i > 0) {
						s = s1.substring(0, i) + s;
					} else {
						s = s1 + "\n\n" + s;
					}
				}
				IOUtil.write(t1.getOutputStream(), X.UTF8, s);
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
			List<?> l1 = _DiskIO.dao.distinct("path",
					W.create().and(X.NODE, id).and("updated", Global.now() - X.AMINUTE * 10, W.OP.gte).sort("path", 1));
			l1.remove(null);
			l1.remove(X.EMPTY);
			return l1;
		} else if (X.isSame(tag, "net")) {
			List<?> l1 = _Net.dao.distinct(X.NAME,
					W.create().and(X.NODE, id).and("updated", Global.now() - X.AMINUTE * 10, W.OP.gte).sort("inet", 1));
			l1.remove(null);
			l1.remove(X.EMPTY);
			return l1;
		}
		return null;
	}

	public boolean isAlive() {
		return lastcheck > Global.now() - Node.LOST;
	}

	/**
	 * Forward请求
	 * 
	 * @param uri    - 链接
	 * @param req    - 请求
	 * @param resp   - 返回结果
	 * @param method - 方法
	 * @throws Exception
	 */
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

		head.put("X-Real-IP", req.ip());
		j1.append("head", head);
		j1.append("body", body);

		if (log.isDebugEnabled()) {
			log.debug("forwarding: " + j1.toPrettyString());
		}

		JSON r1 = call("node." + id, MQ.Request.create().cmd("forward").put(j1), X.AMINUTE);

//		log.warn("resp=" + r1);
		resp.setStatus(r1.getInt("status"));
		head = JSON.fromObject(r1.get("head"));
		if (head != null) {
			for (String s1 : head.keySet()) {
				resp.addHeader(s1, head.getString(s1));
			}
		}

		OutputStream out = resp.getOutputStream();
		byte[] bb = Base64.getDecoder().decode(r1.getString("out"));
		if (bb != null) {
			out.write(bb);
		}
		out.flush();

	}

	/**
	 * 初始化
	 */
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

	/**
	 * 节点Forward服务
	 */
	private final static IStub stub = new IStub(NAME) {

		@Override
		public void onRequest(long seq, Request req) {

//			log.warn("got message, ");

			try {
				String cmd = req.cmd;
				if (X.isSame(cmd, "poweroff")) {
					JSON r1 = req.get();

					int power = r1.getInt("power");
					if (power == 1) {
						// 重启服务
						log.warn("restart by admin [" + req.from + "]");
						Task.schedule(t -> {
							System.exit(0);
						}, 1000);
					} else if (power == 2) {
						// 重启系统
						log.warn("poweroff by admin [" + req.from + "]");
						Task.schedule(t -> {
							try {
								Shell.run("halt", X.AMINUTE);
							} catch (Exception e) {
								log.error(e.getMessage(), e);
							}
						}, 1000);
					}
					return;
				} else if (X.isSame(req.cmd, "reply")) {
					// rpc 回复
					Stack<Request> l1 = waiter.get(seq);
//					log.info("got reply, seq=" + seq + ", node=" + NAME + ", l1=" + l1);

					if (l1 != null) {
						synchronized (l1) {
							l1.push(req);
							l1.notifyAll();
						}
					} else {
						// ignore, may caller return once got one result
//						log.warn("MQ1, not waiter! seq=" + seq + ", " + waiter.keySet());
					}
					return;
				} else if (X.isSame(req.cmd, "getalltask")) {
					var l1 = Task.getAll();
					var r1 = X.asList(l1, d -> {
						Task t = (Task) d;
						JSON j = JSON.create();
						j.put("name", t.getName());
						j.put("class", t.getClass().getName());
						j.put("state", t.getState().name());
						j.put("pool", t.getPool());
						j.put("remain", t.getRemain());
						j.put("delay", t.getDelay() > 0 ? t.getDelay() : 0);
						j.put("cputime", t.getRuntime() > 0 ? t.getRuntime() : 0);
						j.put("realcputime", t.getCosting() > 0 ? t.getCosting() : 0);
						j.put("runtimes", t.getRuntimes() > 0 ? t.getRuntimes() : 0);
						return j;
					});
					req.reply(r1);
					return;
				}

				JSON r1 = req.get();

				if (log.isInfoEnabled()) {
					log.info("got node.forward: " + r1.toPrettyString());
				}

				// http 转发
				String m = r1.getString("m");
				String uri = r1.getString("uri");
				JSON head = JSON.fromObject(r1.get("head"));
				JSON body = JSON.fromObject(r1.get("body"));
				if (log.isInfoEnabled()) {
					log.info("uri=" + uri + ", m=" + m + ", head=" + head + ", body=" + body);
				}

				TimeStamp t = TimeStamp.create();

				MockResponse resp1 = MockResponse.create();
				Controller.process(uri, MockRequest.create(uri, head, body), resp1, m, t);
				X.close(resp1);

				JSON r2 = JSON.create();
				r2.put("status", resp1.status);
				r2.put("head", resp1.head);
				r2.put("out", new String(Base64.getEncoder().encode(resp1.out.toByteArray())));

//				Request r3 = Request.create().put(r2);
//				if (log.isInfoEnabled()) {
//					log.info("node.forward, resp=" + r1 + ", size=" + r3.data.length + ", r3=" + r3.get());
//				}

				req.reply(r2);

			} catch (Throwable e1) {
				log.error("node.forward", e1);
			}

		}

	};

	/**
	 * 获取节点的CPU使用率
	 * 
	 * @return
	 */
	public static int cpusage() {
		if (System.currentTimeMillis() - _lastcheck > 3000) {
			// 超过3秒钟，检测了
			OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
			cpusage = X.toInt(os.getCpuLoad() * 100);
		}
		return cpusage;
	}

	/**
	 * 获取 在线节点， 最大10K个
	 * 
	 * @return
	 */
	public static Beans<Node> alive() {
		return dao.load(W.create().and("tag", "giiwa").and("lastcheck", Global.now() - LOST, W.OP.gte).sort("label"), 0,
				10240);
	}

	/**
	 * 关闭/重启节点
	 * 
	 * @param power - 1:重启服务，2:关闭节点
	 * @throws Exception
	 */
	public void power(int power) throws Exception {
		if (this.isReadonly()) {
			// 只读，禁止节点操作
			return;
		}
		/**
		 * 发送消息
		 */
		MQ.send(mq, org.giiwa.net.mq.MQ.Request.create().cmd("poweroff").put(JSON.create().append("power", power)));
	}

	public String mq() {
		return "node." + id;
	}

	/**
	 * 消息编号
	 */
	private final static AtomicLong seq = new AtomicLong(0);

	/**
	 * 发送消息，并等待结果
	 * 
	 * @param <T>     - 结果类型
	 * @param name    - queue name
	 * @param req     - 请求参数
	 * @param timeout - 超时毫秒
	 * @return
	 * @throws Exception
	 */
	public static <T> T call(String name, Request req, final long timeout) throws Exception {

		TimeStamp t = TimeStamp.create();

		req.from = NAME;
		req.seq = seq.incrementAndGet();

		Stack<Request> l1 = new Stack<Request>();
		waiter.put(req.seq, l1);

		/**
		 * 发送消息
		 */
		MQ.send(name, req);

		try {
			while (timeout > t.pastms()) {
				synchronized (l1) {
					if (l1.isEmpty()) {
						l1.wait(timeout - t.pastms());
					}
				}
				if (!l1.isEmpty()) {
					Request r = l1.pop();
					return r.get();
				}
			}
		} catch (Exception e) {
			GLog.applog.error("mq", "call", "call failed", e);
			throw e;
		} finally {
			waiter.remove(req.seq);
		}

		throw new Exception("timeout(" + timeout + "ms)");
	}

	/**
	 * 广播消息，异步收集结果
	 * 
	 * @param name    - topic name
	 * @param req     - 请求参数
	 * @param timeout - 超时毫秒
	 * @param func    - 异步结果处理函数， 返回true，终止后续结果处理
	 * @return
	 * @throws Exception
	 */
	public static boolean call(String name, Request req, final long timeout, Function<Request, Boolean> func)
			throws Exception {

		req.from = NAME;
		req.seq = seq.incrementAndGet();

		Stack<Request> l1 = null;
		if (func != null) {
			l1 = new Stack<Request>();
			waiter.put(req.seq, l1);
		}

		/**
		 * 发送广播消息
		 */
		MQ.topic(name, req);

		try {

			TimeStamp t = TimeStamp.create();

			if (func != null) {
				Request e = null;

				while (timeout > t.pastms()) {

					synchronized (l1) {
						if (l1.isEmpty()) {
							l1.wait(timeout - t.pastms());
						}
						if (!l1.isEmpty()) {
							e = l1.pop();
						}
					}

					if (e != null) {
						boolean r = func.apply(e);
						if (r) {
							// 完成， 结束
							return true;
						} // 继续获取下一个
					}
				}

				throw new Exception("Timeout: " + t.past() + ", name=" + name + ", seq=" + req.seq + ", from="
						+ req.from + ", e=" + e);

			} else {
				return true;
			}
		} finally {
			waiter.remove(req.seq);
		}
	}

	/**
	 * 回调消息
	 */
	private final static Map<Long, Stack<Request>> waiter = new HashMap<Long, Stack<Request>>();

}
