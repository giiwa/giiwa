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
package org.giiwa.core.base;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.X;
import org.giiwa.core.json.JSON;
import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.ProcCred;
import org.hyperic.sigar.ProcCredName;
import org.hyperic.sigar.ProcExe;
import org.hyperic.sigar.ProcStat;
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
		_init();
		return sigar.getPid();
	}

	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {

		System.out.println(Host.getLocalip());

		System.out.println(getPid());

	}

	public static String getLocalip() {
		try {
			Enumeration<NetworkInterface> l1 = NetworkInterface.getNetworkInterfaces();
			if (l1 != null) {
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
							System.out.println(ia.getClass());

							sb.append(ia.getHostAddress());
						}
					}
				}
				return sb.toString();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return X.EMPTY;
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
		return OperatingSystem.getInstance();
	}

	public static List<JSON> getProcess() throws SigarException {
		_init();

		long[] pids = getPids();
		List<JSON> l1 = new ArrayList<JSON>();
		for (long pid : pids) {
			try {
				ProcCred ce = sigar.getProcCred(pid);
				ProcExe p = sigar.getProcExe(pid);
				ProcCredName cn = sigar.getProcCredName(pid);

				l1.add(JSON.create().append("pid", pid).append("name", p.getName()).append("cwd", p.getCwd())
						.append("uid", ce.getUid()).append("user", cn.getUser()));
			} catch (Exception e) {
				// ignore
			}
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

	public static List<JSON> getDisks() throws SigarException {
		_init();

		FileSystem[] fs = getFileSystem();
		List<JSON> l1 = new ArrayList<JSON>();
		for (FileSystem f : fs) {

			FileSystemUsage p = sigar.getFileSystemUsage(f.getDirName());
			l1.add(JSON.create().append("devname", f.getDevName()).append("dirname", f.getDirName())
					.append("typename", f.getTypeName()).append("total", p.getTotal()).append("used", p.getUsed())
					.append("free", p.getFree()).append("files", p.getFiles()).append("usepercent", p.getUsePercent()));

		}
		return l1;
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

	public static NetInterfaceStat[] getIfstats() throws SigarException {
		_init();

		String[] ifaces = sigar.getNetInterfaceList();

		List<NetInterfaceStat> l1 = new ArrayList<NetInterfaceStat>();
		for (int i = 0; i < ifaces.length; i++) {
			NetInterfaceConfig ifconfig = sigar.getNetInterfaceConfig(ifaces[i]);
			if ((ifconfig.getFlags() & 1L) <= 0L) {
				continue;
			}

			NetInterfaceStat cfg = sigar.getNetInterfaceStat(ifaces[i]);

			l1.add(cfg);
		}

		return l1.toArray(new NetInterfaceStat[l1.size()]);
	}

	private static synchronized void _init() {
		if (sigar != null)
			return;
		sigar = new Sigar();
	}

	private static Sigar sigar = null;

}
