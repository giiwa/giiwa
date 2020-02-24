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

import java.lang.management.ManagementFactory;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.conf.Local;
import org.giiwa.dao.Bean;
import org.giiwa.dao.BeanDAO;
import org.giiwa.dao.Column;
import org.giiwa.dao.Helper;
import org.giiwa.dao.Table;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.dfile.FileClient;
import org.giiwa.dfile.FileServer;
import org.giiwa.misc.Host;
import org.giiwa.task.Task;
import org.giiwa.web.Controller;
import org.giiwa.web.Module;
import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.NetStat;

/**
 * The code bean, used to store special code linked with s1 and s2 fields
 * table="gi_code"
 * 
 * @author wujun
 *
 */
@Table(name = "gi_node")
public class Node extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(Node.class);

	public static final BeanDAO<String, Node> dao = BeanDAO.create(Node.class);

	public static final long LOST = 10 * 1000;

	@Column(name = X.ID, index = true)
	private String id;

	@Column(name = "pid")
	private String pid;

	@Column(name = "ip")
	private String ip;

	@Column(name = "label")
	private String label;

	@Column(name = "url")
	private String url;

	@Column(name = "localthreads")
	private int localthreads;

	@Column(name = "localpending")
	private int localpending;

	@Column(name = "localrunning")
	private int localrunning;

	@Column(name = "uptime")
	private long uptime;

	@Column(name = "cores")
	private int cores; // cpu cores

	@Column(name = "usage")
	private int usage; // cpu usage

	@Column(name = "tcp_established")
	private int tcp_established; // sockets count

	@Column(name = "tcp_closewait")
	private int tcp_closewait; // sockets close wait

	@Column(name = "giiwa")
	private String giiwa;

	@Column(name = "modules")
	private List<String> modules;

	@Column(name = "dfiletimes")
	private long dfiletimes;

	@Column(name = "dfileavgcost")
	private long dfileavgcost;

	@Column(name = "dfilemaxcost")
	private long dfilemaxcost;

	@Column(name = "dfilemincost")
	private long dfilemincost;

	public List<String> getModules() {
		return modules;
	}

	public int getTcp_established() {
		return tcp_established;
	}

	public int getTcp_closewait() {
		return tcp_closewait;
	}

	public int getState() {
		if (Task.powerstate == 0)
			return 0;

		if (System.currentTimeMillis() - this.getUpdated() > LOST)
			return 0;

		return 1;
	}

	public int getUsage() {
		return usage;
	}

	public String getId() {
		return id;
	}

	public String getUrl() {
		return url;
	}

	public String getLabel() {
		return label;
	}

	public long getUptime() {
		return uptime;
	}

	public String getIp() {
		return ip;
	}

	public boolean isLocal() {
		return X.isSame(id, Local.id());
	}

	public static void touch(boolean force) {
		try {
			if (!Helper.isConfigured()) {
				return;
			}

			if (dao.exists(Local.id())) {
				// update
				V v = V.create();
				v.append("localthreads", Task.activeThread());
				v.append("localrunning", Task.tasksInRunning());
				v.append("localpending", Task.tasksInQueue());

				NetStat ns = Host.getNetStat();
				if (ns != null) {
					v.append("tcp_established", ns.getTcpEstablished());
					v.append("tcp_closewait", ns.getTcpCloseWait() + ns.getTcpTimeWait());
				}

				FileServer.measures(v);
				FileClient.measures(v);

				// v.append("lasttime", System.currentTimeMillis());

				if (force) {
					FileServer.resetm();
					FileClient.resetm();

					v.append("modules", X.asList(Module.getAll(true), e -> {
						if (X.isSame("default", ((Module) e).getName())) {
							return null;
						}
						return ((Module) e).getName();
					}));

					String process = ManagementFactory.getRuntimeMXBean().getName();
					dao.update(Local.id(),
							getNodeInfo().append("pid", process.substring(0, process.indexOf("@"))).append(v));
				} else {

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
					v.append("usage", (int) ((user + sys) * 100 / cc.length));

					dao.update(Local.id(), v);
				}

				// update the disk
				Disk.dao.update(W.create("node", Local.id()), V.create("bad", 0));
			} else {
				// create
				dao.insert(getNodeInfo().append(X.ID, Local.id()));
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private static V getNodeInfo() {
		V v = V.create("uptime", Controller.UPTIME).append("ip", Host.getLocalip());

		try {
			CpuInfo[] cc = Host.getCpuInfo();
			if (cc != null && cc.length > 0) {
				v.append("cores", cc[0].getTotalCores());
			}
			v.append("giiwa", Module.load("default").getVersion() + "." + Module.load("default").getBuild());
			v.append("os", Host.getOS().getName());
			v.append("mem", Host.getMem().getTotal());

			v.append("url", FileServer.URL.replace("0.0.0.0", Host.getLocalip()));

			if (cc != null) {
				double user = 0;
				double sys = 0;
				for (CpuPerc c : Host.getCpuPerc()) {
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
				v.append("usage", (int) ((user + sys) * 100 / cc.length));
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return v;
	}

	public void forward(String uri, HttpServletRequest req, HttpServletResponse resp, String method) {
		try {
			FileClient.get(url).http(uri, req, resp, method, id);
		} catch (Exception e) {
			log.error(url, e);
		}
	}

}
