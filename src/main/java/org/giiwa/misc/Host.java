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

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.dao.Helper.V;
import org.giiwa.conf.Local;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.UID;
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
		return X.toLong(Shell.pid());
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
								sb.append(",");
							}
							sb.append(ia.getHostAddress());
						}
					}
				}

				if (sb.length() > 0) {
					return sb.toString();
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return "127.0.0.1";
	}

	public static double getCpuGHz() {

		_init();

		try {
			String lines = Shell.run("cat /proc/cpuinfo |grep \"model name\"", X.AMINUTE);
			String[] ss = X.split(lines, "\n");
			if (ss != null && ss.length > 0) {
				if (ss[0].indexOf("GHz") > 0) {
					int i = ss[0].lastIndexOf("@");
					if (i > 0) {
						String ghz = ss[0].substring(i + 1).trim();
						return X.toDouble(ghz);
					}
				}
			}

			lines = Shell.run("cat /proc/cpuinfo |grep \"cpu MHz\"", X.AMINUTE);
			ss = X.split(lines, "\n");
			if (ss != null && ss.length > 0) {
				double max = Double.MIN_VALUE;
				for (String s : ss) {
					int i = s.lastIndexOf(":");
					if (i > 0) {
						String ghz = s.substring(i + 1).trim();
						double d = X.toDouble(ghz) / 1000;
						if (d > max) {
							max = d;
						}
					}
				}
				return ((int) (max * 10)) / 10d;
			}

		} catch (Exception e) {
			// ignore
		}
		return 1;

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

//	private static List<String> _cpuid = null;
//
//	public static List<String> getCpuID() {
//
//		if (_cpuid == null) {
//			// linux: dmidecode -t 4 |grep ID
//			// win10: wmic cpu get processorid
//			String[] ss = X.split(Shell.bash("dmidecode -t 4|grep ID", 1000), "\r");
//			List<String> l1 = new ArrayList<String>();
//
//			for (String s : ss) {
//				s = s.replaceAll("ID:", "").trim();
//				if (!X.isEmpty(s)) {
//					l1.add(s);
//				}
//			}
//
//			_cpuid = l1;
//		}
//
//		return _cpuid;
//
//	}

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

		Collections.sort(l1, new Comparator<JSON>() {

			@Override
			public int compare(JSON o1, JSON o2) {
				double c1 = o1.getDouble("cpu");
				double c2 = o2.getDouble("cpu");
				if (c1 > c2) {
					return -1;
				} else if (c1 < c2) {
					return 1;
				}
				return 0;
			}

		});

		return l1;
	}

	public static List<JSON> getProcess(long ppid) {

		_init();

		try {
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
					if (ppid <= 0 || ppid != st.getPpid()) {
						continue;
					}
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
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
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

	public static List<_DS> getDisks() throws SigarException {

		_init();

		FileSystem[] fs = getFileSystem();

		List<_DS> l1 = new ArrayList<_DS>();

		for (FileSystem f : fs) {

			if (Arrays.asList(FileSystem.TYPE_LOCAL_DISK).contains(f.getType())) {
				FileSystemUsage p = sigar.getFileSystemUsage(f.getDirName());

				// log.debug("p.total=" + p.getTotal());
				if (p.getTotal() > 0) {
					l1.add(_DS.create(f, p));
				}
			}
		}
		return l1;
	}

	public static class _DS {

		public String path;
		public String name;
		public String typename;
		public long total;
		public long used;
		public long free;
		public long files;
		public double usepercent;
		public long reads;
		public long readbytes;
		public long writes;
		public long writebytes;
		public double queue;

		public static _DS create(FileSystem f, FileSystemUsage p) {

			_DS e = new _DS();
			e.name = f.getDevName();
			e.path = f.getDirName();
			e.typename = f.getTypeName();
			e.total = 1024 * p.getTotal();
			e.used = 1024 * p.getUsed();
			e.free = 1024 * p.getFree();
			e.files = p.getFiles();
			e.usepercent = p.getUsePercent();
			e.reads = p.getDiskReads();
			e.readbytes = p.getDiskReadBytes();
			e.writes = p.getDiskWrites();
			e.writebytes = p.getDiskWriteBytes();
			e.queue = p.getDiskQueue();
			return e;

		}

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

	public static List<_NS> getIfstats() throws SigarException {
		_init();

		String[] ifaces = sigar.getNetInterfaceList();

		List<_NS> l1 = new ArrayList<_NS>();
		for (int i = 0; i < ifaces.length; i++) {
			NetInterfaceConfig cfg = sigar.getNetInterfaceConfig(ifaces[i]);
			if ((cfg.getFlags() & 1L) <= 0L || X.isSame(cfg.getAddress(), "0.0.0.0")) {
				continue;
			}

			NetInterfaceStat s = sigar.getNetInterfaceStat(ifaces[i]);

			l1.add(_NS.create(cfg, s));

		}

		return l1;
	}

	public static class _NS {
		public String address;
		public String name;
		public String inet;
		public String inet6;
		public long rxbytes;
		public long rxdrop;
		public long rxerr;
		public long rxpackets;
		public long speed;
		public long txbytes;
		public long txdrop;
		public long txerr;
		public long txpackets;
		public long created;

		static _NS create(NetInterfaceConfig cfg, NetInterfaceStat s) {

			_NS e = new _NS();

			e.address = cfg.getAddress();
			e.name = cfg.getName();
			e.inet = cfg.getAddress();
			e.inet6 = cfg.getAddress();
			e.rxbytes = s.getRxBytes();
			e.rxdrop = s.getRxDropped();
			e.rxerr = s.getRxErrors();
			e.rxpackets = s.getRxPackets();
			e.speed = s.getSpeed();
			e.txbytes = s.getTxBytes();
			e.txdrop = s.getTxDropped();
			e.txerr = s.getTxErrors();
			e.txpackets = s.getTxPackets();
			e.created = System.currentTimeMillis();
			return e;
		}

		public V toV() {

			V e = V.create();

			e.append("address", address);
			e.append("name", name);
			e.append("inet", inet);
			e.append("inet6", inet6);
			e.append("rxbytes", rxbytes);
			e.append("rxdrop", rxdrop);
			e.append("rxerr", rxerr);
			e.append("rxpackets", rxpackets);
			e.append("speed", speed);
			e.append("txbytes", txbytes);
			e.append("txdrop", txdrop);
			e.append("txerr", txerr);
			e.append("txpackets", txpackets);
			e.append("created", created);

			return e;

		}

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

		TimeStamp t1 = TimeStamp.create();

		List<String> l1 = new ArrayList<String>();

		try {

			NetInterfaceConfig[] nn = Host.getIfaces();
			for (NetInterfaceConfig n : nn) {

				if (!X.isEmpty(n.getAddress()) && !X.isEmpty(n.getHwaddr())) {

//					log.warn("mac: " + n.getAddress() + ", " + n.getHwaddr());

					String ip = n.getAddress();
					String[] ss = X.split(ip, "\\.");
					if (!X.isIn(ss[0], "127", "0")) {

						if (!l1.contains(n.getHwaddr())) {
							l1.add(n.getHwaddr());
						}

//					} else {
//						log.warn("ignore, ss[0]=" + ss[0] + ", ip=" + ip);
					}
				}
			}
			Collections.sort(l1);

//			log.warn("mac: " + l1);

		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}

		if (log.isDebugEnabled()) {
			log.debug("cost=" + t1.past() + ", mac=" + l1);
		}

		if (l1.isEmpty()) {
			l1.add(UID.id(Local.id()));
		}

		return l1;
	}

	private static boolean _sensors = true;

	public static String getCpuTemp() {

		try {
			if (_sensors) {
				String s = Shell.run("sensors", 10 * 1000);
				StringFinder sf = StringFinder.create(s);

				String temp = sf.get("Core 0:", "(high");
				if (X.isEmpty(temp)) {
					temp = sf.reset().get("Composite:", "(low");
				}

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

	public static String getDockerID() {

		String cmd = "head -1 /proc/self/cgroup";
		String r = Shell.bash(cmd, 3000);
		if (!X.isEmpty(r) && r.indexOf("docker") > 0) {
			int i = r.lastIndexOf("/");
			if (i > 0) {
				return r.substring(i + 1).trim();
			}
		}
		return null;

	}

}
