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
package org.giiwa.misc;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetConnection;
import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.NetRoute;
import org.hyperic.sigar.NetStat;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.ProcCred;
import org.hyperic.sigar.ProcCredName;
import org.hyperic.sigar.ProcMem;
import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.ProcUtil;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.Swap;
import org.hyperic.sigar.Uptime;

/**
 * The Class Host.
 */
public class Host {

	/** The log. */
	static Log log = LogFactory.getLog(Host.class);

	public static long getPid() {
		RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
//		System.out.println(runtimeMXBean.getName());
		return X.toInt(runtimeMXBean.getName().split("@")[0]);
	}

	public static String getLocalip() {
		try {
			Enumeration<NetworkInterface> l1 = NetworkInterface.getNetworkInterfaces();
			if (l1 != null && l1.hasMoreElements()) {
				StringBuilder sb = new StringBuilder();
				while (l1.hasMoreElements()) {
					NetworkInterface e = l1.nextElement();
					if (!e.isLoopback()) {
						Enumeration<InetAddress> e2 = e.getInetAddresses();
						while (e2.hasMoreElements()) {
							InetAddress ia = e2.nextElement();
							if (ia instanceof Inet6Address)
								continue;
							if (sb.length() > 0) {
								sb.append(";");
							}
							// System.out.println(ia.getClass());

							sb.append(ia.getHostAddress());
						}
					}
				}

				if (sb.length() > 0)
					return sb.toString();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return "127.0.0.1";
	}

	public static CpuInfo[] getCpuInfo() throws SigarException {
		_init();
		return sigar.getCpuInfoList();
	}

	public static Uptime getUptime() throws SigarException {
		_init();

		return sigar.getUptime();
	}

	public static CpuPerc[] getCpuPerc() throws SigarException {
		_init();
		return sigar.getCpuPercList();
	}

	public static Mem getMem() throws SigarException {
		_init();
		return sigar.getMem();
	}

	public static Swap getSwap() throws SigarException {
		_init();
		return sigar.getSwap();
	}

	public static OperatingSystem getOS() throws SigarException {
		_init();

		OperatingSystem os = OperatingSystem.getInstance();

		return os;
	}

	public static List<JSON> getProcess() throws SigarException {
		_init();

		long[] pids = getPids();
		List<JSON> l1 = new ArrayList<JSON>();

		for (long pid : pids) {
			JSON jo = JSON.create().append("pid", pid);

			try {
				ProcCred ce = sigar.getProcCred(pid);
				jo.append("uid", ce.getUid());
			} catch (Exception e) {
				// ignore
			}
			try {
				// ProcExe p = sigar.getProcExe(pid);
				// jo.append("name", p.getName()).append("cwd", p.getCwd());

				String name = ProcUtil.getDescription(sigar, pid);
				jo.put("name", name);
			} catch (Exception e) {
				// ignore
			}
			try {

				ProcCredName cn = sigar.getProcCredName(pid);
				jo.append("user", cn.getUser());
			} catch (Exception e) {
				// ignore
			}
			try {

				ProcState st = sigar.getProcState(pid);
				jo.append("threads", st.getThreads()).append("ppid", st.getPpid());
			} catch (Exception e) {
				// ignore
			}
			try {

				ProcMem m = sigar.getProcMem(pid);
				jo.append("mem", m.getResident());
			} catch (Exception e) {
				// ignore
			}
			try {

				ProcCpu c = sigar.getProcCpu(pid);
				jo.append("cpu", c.getPercent()).append("cputotal", c.getTotal());

			} catch (Exception e) {
				// ignore
			}

			l1.add(jo);
		}
		return l1;
	}

	public static long[] getPids() throws SigarException {
		_init();

		return sigar.getProcList();
	}

	public static FileSystem[] getFileSystem() throws SigarException {
		_init();

		return sigar.getFileSystemList();
	}

	public static Map<String, FileSystemUsage> getFileSystemUsage() throws SigarException {
		_init();

		FileSystem[] fs = getFileSystem();
		Map<String, FileSystemUsage> l1 = new TreeMap<String, FileSystemUsage>();
		for (FileSystem f : fs) {
			l1.put(f.getDirName(), sigar.getFileSystemUsage(f.getDirName()));
		}
		return l1;
	}

	public static Collection<JSON> getDisks() throws SigarException {

		_init();

		FileSystem[] fs = getFileSystem();
		Map<String, JSON> m1 = new TreeMap<String, JSON>();
		for (FileSystem f : fs) {

			if (Arrays.asList(FileSystem.TYPE_LOCAL_DISK).contains(f.getType())) {
				FileSystemUsage p = sigar.getFileSystemUsage(f.getDirName());

				// log.debug("p.total=" + p.getTotal());
				if (p.getTotal() > 0) {

					m1.put(f.getDirName(),
							JSON.create().append("devname", f.getDevName()).append("dirname", f.getDirName())
									.append("typename", f.getTypeName()).append("total", 1024 * p.getTotal())
									.append("used", 1024 * p.getUsed()).append("free", 1024 * p.getFree())
									.append("files", p.getFiles()).append("usepercent", p.getUsePercent())
									.append("reads", p.getDiskReads()).append("readbytes", p.getDiskReadBytes())
									.append("writes", p.getDiskWrites()).append("writebytes", p.getDiskWriteBytes())
									.append("queue", p.getDiskQueue()));
				}
			}
		}
		return m1.values();
	}

	public static NetInterfaceConfig[] getIfaces() throws SigarException {
		_init();

		String[] ifaces = sigar.getNetInterfaceList();

		List<NetInterfaceConfig> l1 = new ArrayList<NetInterfaceConfig>();
		for (int i = 0; i < ifaces.length; i++) {
			NetInterfaceConfig cfg = sigar.getNetInterfaceConfig(ifaces[i]);
			if (NetFlags.LOOPBACK_ADDRESS.equals(cfg.getAddress()) || (cfg.getFlags() & NetFlags.IFF_LOOPBACK) != 0
					|| NetFlags.NULL_HWADDR.equals(cfg.getHwaddr())) {
				continue;
			}
			l1.add(cfg);
		}

		return l1.toArray(new NetInterfaceConfig[l1.size()]);
	}

	public static Collection<JSON> getIfstats() throws SigarException {
		_init();

		String[] ifaces = sigar.getNetInterfaceList();

		List<JSON> l1 = new ArrayList<JSON>();
		for (int i = 0; i < ifaces.length; i++) {
			NetInterfaceConfig cfg = sigar.getNetInterfaceConfig(ifaces[i]);
			if ((cfg.getFlags() & 1L) <= 0L || X.isSame(cfg.getAddress(), "0.0.0.0")) {
				continue;
			}

			NetInterfaceStat s = sigar.getNetInterfaceStat(ifaces[i]);
			l1.add(JSON.create().append("address", cfg.getAddress()).append("name", cfg.getName())
					.append("rxbytes", s.getRxBytes()).append("rxdrop", s.getRxDropped())
					.append("rxerr", s.getRxErrors()).append("rxpackets", s.getRxPackets())
					.append("speed", s.getSpeed()).append("txbytes", s.getTxBytes()).append("txdrop", s.getTxDropped())
					.append("txerr", s.getTxErrors()).append("txpackets", s.getTxPackets())
					.append("created", System.currentTimeMillis()));
		}
		return l1;
	}

	public static NetStat getNetStat() throws SigarException {
		_init();
		return sigar.getNetStat();
	}

	public static List<JSON> getNetStats() throws SigarException {
		_init();
		int flags = NetFlags.CONN_CLIENT | NetFlags.CONN_SERVER | NetFlags.CONN_TCP | NetFlags.CONN_UDP
				| NetFlags.CONN_PROTOCOLS;
		NetConnection[] nn = sigar.getNetConnectionList(flags);
		List<JSON> l1 = new ArrayList<JSON>();
		for (NetConnection n : nn) {
			if (n.getRemotePort() == 0 && n.getLocalPort() == 0)
				continue;

			JSON jo = JSON.create().append("remoteaddress", n.getRemoteAddress())
					.append("remoteport", n.getRemotePort()).append("localaddress", n.getLocalAddress())
					.append("localport", n.getLocalPort()).append("recvqueue", n.getReceiveQueue())
					.append("sendqueue", n.getSendQueue()).append("state", n.getState()).append("type", n.getType())
					.append("typename", n.getTypeString());
			if (n.getType() != NetFlags.CONN_UDP) {
				jo.append("statename", n.getStateString());
			}
			try {
				long pid = sigar.getProcPort(n.getType(), n.getLocalPort());
				if (pid != 0) {
					ProcState p = sigar.getProcState(pid);
					if (p != null) {
						jo.append("pid", pid + "/" + p.getName());
					}
				}
			} catch (Exception e) {
				// ignore
				log.warn("get pid error, type=" + n.getType() + ", port=" + n.getLocalPort());
			}
			l1.add(jo);
		}
		return l1;
	}

	private static synchronized void _init() {
		if (sigar != null)
			return;
		sigar = new Sigar();
	}

	private static Sigar sigar = null;

	public static NetRoute[] getRoutes() throws SigarException {
		_init();

		return sigar.getNetRouteList();
	}

	public static List<String> getMAC() {

		List<String> l1 = new ArrayList<String>();

		try {
			String s = Shell.run("ifconfig |grep ether", 10000);
			X.IO.lines(s, (line, re) -> {

				String[] ss = X.split(line.trim(), " ");
				if (ss.length > 1) {
					l1.add(ss[1]);
				}

			});

			Collections.sort(l1);
		} catch (Exception e) {
			// ignore
		}
		return l1;
	}

	private static boolean _sensors = true;

	public static String getCpuTemp() {

		try {
			if (_sensors) {
				String s = Shell.run("sensors", 10 * 1000);
				StringFinder sf = StringFinder.create(s);

				String temp = sf.get("Core 0:", "(high =");
				if (!X.isEmpty(temp)) {
					temp = temp.trim();
				}
				return temp;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			if (e.getMessage().indexOf("No such file or directory") > -1) {
				_sensors = false;
			}
		}
		return X.EMPTY;
	}

}
