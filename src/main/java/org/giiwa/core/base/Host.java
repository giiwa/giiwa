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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.exec.OS;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.X;

/**
 * The Class Host.
 */
public class Host {

  /** The log. */
  static Log log = LogFactory.getLog(Host.class);

  /** The eth. */
  String     eth;

  /** The ip. */
  String     ip;

  /** The name. */
  String     name;

  /** The netmask. */
  String     netmask;

  /** The gateway. */
  String     gateway;

  /**
   * Instantiates a new host.
   * 
   * @param eth
   *          the eth
   */
  public Host(String eth) {
    this.eth = eth;
  }

  /**
   * Gets the ip.
   * 
   * @return the ip
   */
  public String getIp() {
    return ip;
  }

  /**
   * Sets the ip.
   * 
   * @param ip
   *          the new ip
   */
  public void setIp(String ip) {
    this.ip = ip;
  }

  /**
   * Gets the name.
   * 
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name.
   * 
   * @param name
   *          the new name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets the netmask.
   * 
   * @return the netmask
   */
  public String getNetmask() {
    return netmask;
  }

  /**
   * Sets the netmask.
   * 
   * @param netmask
   *          the new netmask
   */
  public void setNetmask(String netmask) {
    this.netmask = netmask;
  }

  /**
   * Gets the gateway.
   * 
   * @return the gateway
   */
  public String getGateway() {
    return gateway;
  }

  /**
   * Sets the gateway.
   * 
   * @param gateway
   *          the new gateway
   */
  public void setGateway(String gateway) {
    this.gateway = gateway;
  }

  /**
   * get pid
   * 
   * @return
   */
  public static String getPid() {
    RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
    String info = runtime.getName();
    // System.out.println("info=" + info);
    int index = info.indexOf("@");
    if (index != -1) {
      return info.substring(0, index);
      // System.out.println(info);
      // System.out.println(pid);
    }
    return null;
  }

  /**
   * Sets the.
   * 
   * @param h
   *          the h
   */
  public static void set(Host h) {
    try {
      if (Shell.isLinux()) {

        String uname = Shell.run("uname -a");
        if (uname.indexOf("Ubuntu") > 0) {
          h.ubuntu_set();
        } else {
          h.centos_set();
        }
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Local.
   * 
   * @return the host
   */
  public static Host local() {
    Host h = new Host("eth0");
    try {
      if (Shell.isLinux()) {

        String uname = Shell.run("uname -a");
        if (uname.indexOf("Ubuntu") > 0) {
          h.ubuntu_load(h.eth);
        } else {
          h.centos_load(h.eth);
        }
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return h;
  }

  /**
   * Local.
   * 
   * @param eth
   *          the eth
   * @return the host
   */
  public static Host local(String eth) {
    Host h = new Host(eth);

    try {
      if (Shell.isLinux()) {

        String uname = Shell.run("uname -a");
        if (uname.indexOf("Ubuntu") > 0) {
          h.ubuntu_load(eth);
        } else {
          h.centos_load(eth);
        }
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return h;
  }

  /**
   * The main method.
   * 
   * @param args
   *          the arguments
   */
  public static void main(String[] args) {
    Host h = Host.local();
    System.out.println(h);

    System.out.println(Host.getLocalip());

    System.out.println(getPid());

  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return name + "=[" + ip + ", " + netmask + ", " + gateway + "]";
  }

  private void ubuntu_load(String eth) {
    BufferedReader in = null;

    try {
      File f = new File("/etc/hostname");
      in = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
      name = in.readLine();

      in.close();
      in = null;
      f = new File("/etc/network/interfaces");

      in = new BufferedReader(new InputStreamReader(new FileInputStream(f)));

      /**
       * find the auto eth0
       */
      String line = in.readLine();
      while (line != null) {
        if (line.toLowerCase().startsWith("iface " + eth)) {
          line = in.readLine();
          while (line != null) {
            String[] ss = line.toLowerCase().trim().split(" ");
            if (ss.length == 2) {
              if ("address".equals(ss[0])) {
                ip = ss[1];
              } else if ("netmask".equals(ss[0])) {
                netmask = ss[1];
              } else if ("gateway".equals(ss[0])) {
                gateway = ss[1];
              } else if ("network".equals(ss[0]) || "broadcask".equals(ss[0])) {
                // ignore
              } else {
                break;
              }
            } else {
              break;
            }

            line = in.readLine();
          }
          break;
        }
        line = in.readLine();
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
        }
      }
    }
  }

  private void centos_load(String eth) {
    BufferedReader in = null;

    try {
      String r = Shell.run("cat /etc/sysconfig/network |grep HOSTNAME");

      String[] ss = r.split("=");
      if (ss.length > 1) {
        name = ss[1].trim();
      }

      File f = new File("/etc/sysconfig/network-scripts/ifcfg-" + eth);
      log.debug(f.getAbsolutePath() + ", " + f.exists());

      in = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
      String line = in.readLine();
      while (line != null) {
        /**
         * DEVICE=eth0 BOOTPROTO=none BROADCAST=118.126.4.255
         * HWADDR=bc:30:5b:da:74:b9 IPADDR=118.126.4.234 NETMASK=255.255.255.0
         * NETWORK=118.126.4.0 ONBOOT=yes GATEWAY=118.126.4.193 TYPE=Ethernet
         */
        ss = line.split("=");
        if (ss.length == 2) {
          String key = ss[0].toUpperCase().trim();
          if ("IPADDR".equals(key)) {
            ip = ss[1];
          } else if ("NETMASK".equals(key)) {
            netmask = ss[1];
          } else if ("GATEWAY".equals(key)) {
            gateway = ss[1];
          }
        }

        line = in.readLine();
      }
      in.close();
      in = null;

    } catch (Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
        }
      }
    }

  }

  private void centos_set() {

    PrintStream out = null;
    BufferedReader in = null;

    try {
      if (!X.isEmpty(name)) {
        File f = new File("/etc/sysconfig/network");
        Map<String, String> map = new HashMap<String, String>();
        in = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
        String line = in.readLine();
        while (line != null) {
          String[] ss = line.split("=");
          if (ss.length == 2) {
            map.put(ss[0], ss[1]);
          }
          line = in.readLine();
        }
        in.close();
        in = null;
        map.put("HOSTNAME", name);

        out = new PrintStream(new FileOutputStream(f));
        for (String name : map.keySet()) {
          out.println(name + "=" + map.get(name));
        }
        out.close();
        out = null;
      }

      File f = new File("/etc/sysconfig/network-scripts/ifcfg-eth0");
      in = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
      String line = in.readLine();
      Map<String, String> map = new HashMap<String, String>();

      while (line != null) {
        /**
         * DEVICE=eth0 BOOTPROTO=none BROADCAST=118.126.4.255
         * HWADDR=bc:30:5b:da:74:b9 IPADDR=118.126.4.234 NETMASK=255.255.255.0
         * NETWORK=118.126.4.0 ONBOOT=yes GATEWAY=118.126.4.193 TYPE=Ethernet
         */
        String[] ss = line.split("=");
        if (ss.length == 2) {
          map.put(ss[0].toUpperCase().trim(), ss[1].trim());
        }
        line = in.readLine();
      }
      in.close();
      in = null;

      if (!X.isEmpty(ip)) {
        map.put("IPADDR", ip);
        if (!X.isEmpty(netmask)) {
          map.put("NETMASK", netmask);
        }
        if (!X.isEmpty(gateway)) {
          map.put("GATEWAY", gateway);
        }
        map.put("ONBOOT", "yes");
        map.remove("NETWORK");
      }
      out = new PrintStream(new FileOutputStream(f));
      for (String name : map.keySet()) {
        out.println(name + "=" + map.get(name));
      }
      out.close();
      out = null;

      Shell.run("/etc/init.d/network restart");

    } catch (Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      if (out != null) {
        out.close();
      }

      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
        }
      }
    }

  }

  private void ubuntu_set() {

    Host h = "eth0".equals(eth) ? Host.local("eth1") : Host.local("eth0");

    PrintStream out = null;

    try {
      if (!X.isEmpty(name)) {
        File f = new File("/etc/hostname");
        out = new PrintStream(new FileOutputStream(f));
        out.println(name);
        out.close();
        out = null;
      }

      if (!X.isEmpty(ip)) {
        File f = new File("/etc/network/interfaces");

        out = new PrintStream(new FileOutputStream(f));
        out.println("auto lo");
        out.println("iface lo inet loopback");
        out.println("auto " + eth);
        out.println("iface " + eth + " inet static");
        out.println("\taddress " + ip);
        if (!X.isEmpty(netmask)) {
          out.println("\tnetmask " + netmask);
        }
        if (!X.isEmpty(gateway)) {
          out.println("\tgateway " + gateway);
        }

        if (!X.isEmpty(h.ip)) {
          out.println("auto " + h.eth);
          out.println("iface " + h.eth + " inet static");
          out.println("\taddress " + h.ip);

          if (!X.isEmpty(h.netmask)) {
            out.println("\tnetmask " + h.netmask);
          }
          if (!X.isEmpty(h.gateway)) {
            out.println("\tgateway " + h.gateway);
          }
        }
        out.close();
        out = null;

        Shell.run("/etc/init.d/networking restart");
      }

    } catch (Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      if (out != null) {
        out.close();
      }
    }

  }

  /**
   * Gets the eths.
   * 
   * @return the eths
   */
  public static List<Eth> getEths() {
    try {
      if (Shell.isLinux()) {

        String uname = Shell.run("uname -a");
        if (uname.indexOf("Ubuntu") > 0) {
          List<Eth> list = new ArrayList<Eth>();
          try {
            String r = Shell.run("cat /etc/udev/rules.d/70-persistent-net.rules |grep eth");
            String[] ss = r.split("\r\n");

            for (String s : ss) {
              String[] ss1 = s.split(",");
              log.debug(Helper.toString(ss1));

              for (String s1 : ss1) {
                if (s1.trim().startsWith("NAME=")) {
                  String[] ss2 = s1.split("\"");
                  String name = ss2[1];
                  Eth e = Eth.get(name);
                  if (e != null) {
                    list.add(e);
                  }
                }
              }
            }

            // log.debug(list);

          } catch (Exception e) {
            log.error(e.getMessage(), e);
          }
          return list;
        } else {
          List<Eth> list = new ArrayList<Eth>();
          try {
            String r = Shell.run("ls -l /etc/sysconfig/network-scripts/ifcfg-eth*");
            String[] ss = r.split("\r\n");

            for (String s : ss) {
              if (X.isEmpty(s))
                continue;

              log.debug("s=" + s);

              int i = s.lastIndexOf("-");
              if (i > 0) {
                String name = s.substring(i + 1).trim();
                log.debug("name=" + name);
                if (name.startsWith("eth")) {
                  Eth e = Eth.get(name);
                  if (e != null) {
                    list.add(e);
                  }
                }
              }
            }

            // log.debug(list);

          } catch (Exception e) {
            log.error(e.getMessage(), e);
          }
          return list;
        }
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    return null;
  }

  /**
   * Gets the cpu usage.
   * 
   * @return the cpu usage
   */
  public static float getCpuUsage() {
    // Cpu(s): 17.5%us, 7.6%sy, 0.0%ni, 73.6%id, 1.1%wa, 0.0%hi, 0.1%si,
    // 0.0%st
    try {
      if (Shell.isLinux()) {

        Entity e = cache.get("cpu");
        if (e == null || System.currentTimeMillis() - e.time > 1000) {
          synchronized (cache) {
            e = cache.get("cpu");

            if (e == null || System.currentTimeMillis() - e.time > 1000) {

              String r = Shell.run("top -bn 2 -d 1 |grep Cpu");
              // log.debug("cpu=" + r);
              String[] lines = r.split("\r\n");
              if (lines.length > 1) {
                String[] ss = lines[1].split(",");
                for (String s : ss) {
                  if (X.isEmpty(s))
                    continue;
                  if (s.endsWith("id")) {
                    int i = s.indexOf('%');
                    if (i > 0) {
                      e = new Entity(System.currentTimeMillis(),
                          ((int) (100 - X.toFloat(s.substring(0, i), 0)) * 10) / 10f);
                    }
                  }
                }
              }
              cache.put("cpu", e);
            }
          }
        }

        return e == null ? 0 : X.toFloat(e.value, 0);
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    return 0;
  }

  /**
   * Gets the CP us.
   * 
   * @return the CP us
   */
  public static List<CPU> getCPUs() {
    List<CPU> list = new ArrayList<CPU>();
    try {
      if (Shell.isLinux()) {

        String r = Shell.run("cat /proc/stat |grep \"cpu \"");
        String[] s1 = r.split("\r\n");
        for (String line : s1) {
          String[] ss = line.split(" ");
          List<String> l1 = new ArrayList<String>();
          for (String s : s1) {
            if (X.isEmpty(s))
              continue;
            l1.add(s);
          }

          // log.debug(l1);
          list.add(new CPU(ss[0], X.toLong(ss[1], 0), X.toLong(ss[2], 0), X.toLong(ss[3], 0), X.toLong(ss[4], 0),
              X.toLong(ss[5], 0)));
        }
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    Collections.sort(list);
    return list;
  }

  /**
   * Gets the mem total.
   * 
   * @return the mem total
   */
  public static float getMemTotal() {
    try {

      if (Shell.isLinux()) {

        String r = Shell.run("cat /proc/meminfo");
        String[] ss1 = r.split("\r");
        String r1 = X.EMPTY;
        if (ss1 != null && ss1.length > 0) {
          for (String s1 : ss1) {
            if (s1.contains("MemTotal")) {
              r1 = s1;
              break;
            }
          }
        }

        String[] ss = r1.split(" ");
        List<String> l1 = new ArrayList<String>();
        for (String s : ss) {
          if (X.isEmpty(s))
            continue;
          l1.add(s);
        }

        return (10 * X.toLong(l1.get(1), 0) / 1024 / 1024) / 10f;
      } else if (OS.isFamilyMac()) {
        String cmd = "top -l 1 | awk '/PhysMem/'";
        String r = Shell.bash(cmd);
        // PhysMem: 8584M used (2821M wired), 7797M unused.
        String[] ss = X.split(r, "[: (),]");
        long used = 0;
        long unused = 0;
        if (ss.length > 1) {
          used = X.toLong(ss[1]);
          char c = ss[1].toUpperCase().charAt(ss[1].length() - 1);
          if (c == 'K') {
            used *= 1024;
          } else if (c == 'M') {
            used *= 1024 * 1024;
          } else if (c == 'G') {
            used *= 1024 * 1024 * 1024;
          }
        }
        if (ss.length > 5) {
          unused = X.toLong(ss[5]);
          char c = ss[5].toUpperCase().charAt(ss[5].length() - 1);
          if (c == 'K') {
            unused *= 1024;
          } else if (c == 'M') {
            unused *= 1024 * 1024;
          } else if (c == 'G') {
            unused *= 1024 * 1024 * 1024;
          }
        }
        return (used + unused) * 10 / 1024 / 1024 / 1024 / 10f;
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    return 0;
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

  /**
   * Gets the mem used.
   * 
   * @return the mem used
   */
  public static float getMemUsed() {
    try {

      if (Shell.isLinux()) {

        Entity e = cache.get("mem");

        if (e == null || System.currentTimeMillis() - e.time > 1000) {

          synchronized (cache) {
            if (e == null || System.currentTimeMillis() - e.time > 1000) {

              String r = Shell.run("cat /proc/meminfo");
              String[] ss1 = r.split("\n");
              String r1 = X.EMPTY, r2 = X.EMPTY;
              if (ss1 != null && ss1.length > 0) {
                for (String s1 : ss1) {
                  if (s1.contains("MemTotal")) {
                    r1 = s1;
                  } else if (s1.contains("MemFree")) {
                    r2 = s1;
                  }
                }
              }

              // log.debug("r=" + r);
              // log.debug("r1=" + r1);
              // log.debug("r2=" + r2);

              String[] ss = r1.split(" ");
              List<String> l1 = new ArrayList<String>();
              for (String s : ss) {
                if (X.isEmpty(s))
                  continue;
                l1.add(s);
              }
              int total = X.toInt(l1.get(1), 0);

              ss = r2.split(" ");
              l1 = new ArrayList<String>();
              for (String s : ss) {
                if (X.isEmpty(s))
                  continue;
                l1.add(s);
              }
              int free = X.toInt(l1.get(1), 0);

              e = new Entity(System.currentTimeMillis(), ((int) 10 * (total - free) / 1024 / 1024) / 10f);

              cache.put("mem", e);
            }
          }
        }

        return e == null ? 0 : X.toFloat(e.value, 0);
      } else if (OS.isFamilyMac()) {
        String cmd = "top -l 1 | awk '/PhysMem/'";
        String r = Shell.bash(cmd);
        // PhysMem: 8584M used (2821M wired), 7797M unused.
        String[] ss = X.split(r, "[: (),]");
        long used = 0;
        if (ss.length > 1) {
          used = X.toLong(ss[1]);
          char c = ss[1].toUpperCase().charAt(ss[1].length() - 1);
          if (c == 'K') {
            used *= 1024;
          } else if (c == 'M') {
            used *= 1024 * 1024;
          } else if (c == 'G') {
            used *= 1024 * 1024 * 1024;
          }
        }
        return (used * 10) / 1024 / 1024 / 1024 / 10f;

      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    return 0;
  }

  /**
   * Gets the memories.
   * 
   * @return the memories
   */
  public static List<Memory> getMemories() {
    List<Memory> list = new ArrayList<Memory>();

    try {
      if (Shell.isLinux()) {

        String r = Shell.run("cat /proc/meminfo");
        String[] ss1 = r.split("\r");
        String r1 = X.EMPTY, r2 = X.EMPTY;
        if (ss1 != null && ss1.length > 0) {
          for (String s1 : ss1) {
            if (s1.contains("MemTotal")) {
              r1 = s1;
            } else if (s1.contains("MemFree")) {
              r2 = s1;
            }

          }
        }

        String[] ss = r1.split(" ");
        List<String> l1 = new ArrayList<String>();
        for (String s : ss) {
          if (X.isEmpty(s))
            continue;
          l1.add(s);
        }
        list.add(new Memory("memtotal", X.toInt(l1.get(1))));

        ss = r2.split(" ");
        l1 = new ArrayList<String>();
        for (String s : ss) {
          if (X.isEmpty(s))
            continue;
          l1.add(s);
        }
        list.add(new Memory("memfree", X.toInt(l1.get(1))));
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    Collections.sort(list);
    return list;
  }

  /**
   * Gets the disks.
   * 
   * @return the disks
   */
  public static List<Disk> getDisks() {
    List<Disk> list = new ArrayList<Disk>();

    try {
      if (Shell.isLinux()) {

        String r = Shell.run("df -m");
        // log.debug("df -m=" + r);

        String[] ss = r.split("\n");
        for (int i = 0; i < ss.length; i++) {
          String s = ss[i].trim();

          // log.debug("s=" + s);

          if (s.startsWith("/dev")) {
            String[] s1 = X.split(s, " ");
            if (s1.length == 1 && i < ss.length - 1) {
              // get next line
              s1 = X.split(s + ss[i + 1], " ");
              i++;
            }

            if (s1.length > 5) {
              list.add(new Disk(s1[5], X.toInt(s1[2]), X.toInt(s1[3])));
            }
          }
        }

      } else if (OS.isFamilyMac()) {
        String r = Shell.run("df -m");
        // log.debug("df -m=" + r);

        String[] ss = r.split("\n");
        for (int i = 0; i < ss.length; i++) {
          String s = ss[i].trim();

          // log.debug("s=" + s);

          if (s.startsWith("/dev")) {
            String[] s1 = X.split(s, " ");
            if (s1.length == 1 && i < ss.length - 1) {
              // get next line
              s1 = X.split(s + ss[i + 1], " ");
              i++;
            }

            if (s1.length > 8) {
              // list.add(new Disk(s1[5], X.toInt(s1[2]), X.toInt(s1[3])));
              list.add(new Disk(s1[8], X.toInt(s1[2]), X.toInt(s1[3])));
            }
          }
        }

      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    Collections.sort(list);
    return list;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((gateway == null) ? 0 : gateway.hashCode());
    result = prime * result + ((ip == null) ? 0 : ip.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((netmask == null) ? 0 : netmask.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Host other = (Host) obj;
    if (gateway == null) {
      if (other.gateway != null)
        return false;
    } else if (!gateway.equals(other.gateway))
      return false;
    if (ip == null) {
      if (other.ip != null)
        return false;
    } else if (!ip.equals(other.ip))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (netmask == null) {
      if (other.netmask != null)
        return false;
    } else if (!netmask.equals(other.netmask))
      return false;
    return true;
  }

  /**
   * The Class CPU.
   */
  public static class CPU implements Comparable<CPU> {
    private static Map<String, CPU> prev = new HashMap<String, CPU>();
    private static Map<String, CPU> last = new HashMap<String, CPU>();

    /**
     * /proc/stat
     */
    String                          name;

    /** The user. */
    long                            user;

    /** The nice. */
    long                            nice;

    /** The system. */
    long                            system;

    /** The idle. */
    long                            idle;

    /** The irq. */
    long                            irq;

    /** The time. */
    long                            time = System.currentTimeMillis();

    // public float idle;

    /**
     * Gets the level.
     * 
     * @return the level
     */
    public int getLevel() {
      int i = getIdle();
      if (i < 10) {
        return 3;
      } else if (i < 50) {
        return 2;
      } else {
        return 1;
      }
    }

    /**
     * Instantiates a new cpu.
     * 
     * @param name
     *          the name
     * @param user
     *          the user
     * @param nice
     *          the nice
     * @param system
     *          the system
     * @param idle
     *          the idle
     * @param irq
     *          the irq
     */
    public CPU(String name, long user, long nice, long system, long idle, long irq) {
      this.name = name;
      this.user = user;
      this.nice = nice;
      this.system = system;
      this.idle = idle;
      this.irq = irq;
      CPU c = last.get(name);
      if (c != null) {
        prev.put(name, c);
      }
      last.put(name, this);
    }

    /**
     * Gets the name.
     * 
     * @return the name
     */
    public String getName() {
      return name;
    }

    /**
     * Gets the use.
     * 
     * @return the use
     */
    public int getUse() {
      return 100 - getIdle();
    }

    /**
     * Gets the idle.
     * 
     * @return the idle
     */
    public int getIdle() {
      CPU c = prev.get(name);
      if (c != null && c.time != time) {
        /**
         * 
         */
        return (int) ((idle - c.idle) * 100 / ((idle + system + user) - (c.idle + c.system + c.user)));

      }

      return 100;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(CPU o) {
      return name.compareTo(o.name);
    }
  }

  /**
   * The Class Memory.
   */
  public static class Memory implements Comparable<Memory> {
    /**
     * /proc/meminfo
     */
    String name;

    /** The amount. */
    int    amount;

    /**
     * Instantiates a new memory.
     * 
     * @param name
     *          the name
     * @param amount
     *          the amount
     */
    public Memory(String name, int amount) {
      this.name = name;
      this.amount = amount;
    }

    /**
     * Gets the name.
     * 
     * @return the name
     */
    public String getName() {
      return name;
    }

    /**
     * Gets the amount.
     * 
     * @return the amount
     */
    public String getAmount() {
      return amount * 10 / 1024 / 1024 / 10f + "GB";
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Memory o) {
      return name.compareTo(o.name);
    }
  }

  /**
   * The Class Eth.
   */
  public static class Eth implements Comparable<Eth> {

    /** The name. */
    String     name;

    /** The list. */
    List<Host> list;

    /**
     * Gets the name.
     * 
     * @return the name
     */
    public String getName() {
      return name;
    }

    /**
     * Adds the.
     * 
     * @param ip
     *          the ip
     * @param netmask
     *          the netmask
     * @param gateway
     *          the gateway
     */
    public void add(String ip, String netmask, String gateway) {
      Host h = new Host(name);
      h.ip = ip;
      h.netmask = netmask;
      h.gateway = gateway;
      if (list == null) {
        list = new ArrayList<Host>();
      }
      list.add(h);
    }

    /**
     * Gets the.
     * 
     * @param name
     *          the name
     * @return the eth
     */
    public static Eth get(String name) {
      Eth e = new Eth();
      e.name = name;
      return e;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Eth o) {
      return name.compareTo(o.name);
    }

  }

  /**
   * The Class Disk.
   */
  public static class Disk implements Comparable<Disk> {
    /**
     * df
     */
    String name;

    /** The used. */
    int    used;

    /** The free. */
    int    free;

    // int free;
    // float use;

    /**
     * Instantiates a new disk.
     * 
     * @param name
     *          the name
     * @param used
     *          the used
     * @param free
     *          the free
     */
    public Disk(String name, int used, int free) {
      this.name = name;
      this.free = free;
      this.used = used;
    }

    /**
     * Gets the name.
     * 
     * @return the name
     */
    public String getName() {
      return name;
    }

    /**
     * Gets the free.
     * 
     * @return the free
     */
    public float getFree() {
      return free * 10 / 1024 / 10f;
    }

    /**
     * Gets the used.
     * 
     * @return the used
     */
    public float getUsed() {
      return used * 10 / 1024 / 10f;
    }

    public float getTotal() {
      return (used + free) * 10 / 1024 / 10f;
    }

    /**
     * Gets the level.
     * 
     * @return the level
     */
    public int getLevel() {
      int i = getUse();
      if (i > 90) {
        return 3;
      } else if (i > 50) {
        return 2;
      } else {
        return 1;
      }
    }

    /**
     * Gets the use.
     * 
     * @return the use
     */
    public int getUse() {
      return used * 100 / (used + free);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Disk o) {
      return name.compareTo(o.name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
      return name + "=" + getUsed() + "/" + getTotal() + "G";
    }
  }

  /**
   * The Class Proc.
   */
  public static class Proc {
    // VmRSS
    /**
     * cat /proc/[pid]/status cat /proc/[pid]/stat
     */
    String name;

    /** The cpu. */
    int    cpu;

    /** The mem. */
    int    mem;

    /**
     * Gets the level.
     * 
     * @return the level
     */
    public int getLevel() {
      int i = cpu;
      if (i > 50) {
        return 3;
      } else if (i > 20) {
        return 2;
      } else {
        return 1;
      }
    }

    /**
     * Instantiates a new proc.
     * 
     * @param name
     *          the name
     * @param cpu
     *          the cpu
     * @param mem
     *          the mem
     */
    public Proc(String name, String cpu, int mem) {
      this.name = name;
      this.cpu = X.toInt(cpu);
      this.mem = mem;
    }

    // private static String getPid() {
    // String name = ManagementFactory.getRuntimeMXBean().getName();
    // String pid = name.split("@")[0];
    // return pid;
    // }

    /**
     * Gets the name.
     * 
     * @return the name
     */
    public String getName() {
      return name;
    }

    /**
     * Gets the mem.
     * 
     * @return the mem
     */
    public String getMem() {
      return mem * 10 / 1024 / 10f + "MB";
    }

    /**
     * Gets the cpu.
     * 
     * @return the cpu
     */
    public int getCpu() {
      return cpu;
    }

  }

  static Map<String, Entity> cache = new HashMap<String, Entity>();

  private static class Entity {
    long   time = 0;
    Object value;

    public Entity(long time, Object value) {
      this.time = time;
      this.value = value;
    }
  }

}
