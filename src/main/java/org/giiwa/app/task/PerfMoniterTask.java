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
package org.giiwa.app.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.app.web.f;
import org.giiwa.bean.m._CPU;
import org.giiwa.bean.m._Cache;
import org.giiwa.bean.m._DB;
import org.giiwa.bean.m._Disk;
import org.giiwa.bean.m._DiskIO;
import org.giiwa.bean.m._File;
import org.giiwa.bean.m._MQ;
import org.giiwa.bean.m._Memory;
import org.giiwa.bean.m._Memory2;
import org.giiwa.bean.m._Net;
import org.giiwa.cache.Cache;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.Helper;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.misc.Host;
import org.giiwa.net.mq.MQ;
import org.giiwa.task.Task;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;

/**
 * The Class StateTask.
 */
public class PerfMoniterTask extends Task {

	/**
	 * The Constant serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The log.
	 */
	static Log log = LogFactory.getLog(PerfMoniterTask.class);

	/**
	 * The owner.
	 */
	public static PerfMoniterTask owner = new PerfMoniterTask();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.core.task.Task.getName()
	 */
	@Override
	public String getName() {
		return "gi.perf.moniter";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.worker.WorkerTask.onExecute()
	 */
	@Override
	public void onExecute() {
		try {
			if (Global.getInt("perf.moniter", 0) == 0)
				return;

			// cpu
			CpuPerc[] cc = Host.getCpuPerc();
			if (cc != null && cc.length > 0) {

				JSON jo = JSON.create();
				// summary all
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
				jo.append("user", user * 100 / cc.length);
				jo.append("sys", sys * 100 / cc.length);
				jo.append("usage", (int) (jo.getDouble("user") + jo.getDouble("sys")));
				jo.append("temp", Host.getCpuTemp());
				// log.debug("cpu=" + jo);

				jo.append("name", "cpu").append("cores", cc.length);

				_CPU.update(Local.id(), jo);
			}

			// mem
			Mem m = Host.getMem();
			if (m != null) {
				JSON jo = JSON.create();
				jo.append("total", m.getTotal());
				jo.append("used", m.getUsed());
				jo.append("free", m.getFree());
				jo.append("usage", (int) (m.getUsed() * 100 / m.getTotal()));

				_Memory.update(Local.id(), jo);
			}

			// disk
			Collection<JSON> l1 = Host.getDisks();
			if (l1 != null && !l1.isEmpty()) {

				List<JSON> l2 = new ArrayList<JSON>();
				List<JSON> l3 = new ArrayList<JSON>();
				for (JSON j1 : l1) {

					l2.add(JSON.create().append("path", j1.getString("dirname")).append("name", j1.getString("devname"))
							.copy(j1, "total", "free", "used"));

					l3.add(JSON.create().append("path", j1.getString("dirname")).append("name", j1.getString("devname"))
							.copy(j1, "readbytes", "writebytes", "reads", "writes", "queue"));

				}

				_Disk.update(Local.id(), l2);

				_DiskIO.update(Local.id(), l3);

				// log.debug("disk=" + l2);
			}
			// log.debug("disk=" + l1);

			// net
			Collection<JSON> n1 = Host.getIfstats();
			if (n1 != null && !n1.isEmpty()) {
				List<JSON> l2 = new ArrayList<JSON>();
				for (JSON j1 : n1) {
					l2.add(JSON.create().append("name", j1.get("name")).append("inet", j1.get("address")).copy(j1,
							"rxbytes", "rxdrop", "rxerr", "rxpackets", "txbytes", "txdrop", "txerr", "txpackets")
							.append("_type", "snapshot"));
				}
				_Net.update(Local.id(), l2);
			}

			{
				JSON jo = JSON.create();
				Runtime t = Runtime.getRuntime();
				jo.append("total", t.totalMemory());
				jo.append("used", t.totalMemory() - t.freeMemory());
				_Memory2.update(Local.id(), jo);
			}

			{
				JSON r = Helper.statRead();
				JSON w = Helper.statWrite();

				_DB.update(Local.id(), r.append("name", "read"));
				_DB.update(Local.id(), w.append("name", "write"));

			}

			{
				JSON r = MQ.statRead();
				JSON w = MQ.statWrite();

				_MQ.update(Local.id(), r.append("name", "read"));
				_MQ.update(Local.id(), w.append("name", "write"));

			}

			{
				JSON r = Cache.statRead();
				JSON w = Cache.statWrite();

				_Cache.update(Local.id(), r.append("name", "read"));
				_Cache.update(Local.id(), w.append("name", "write"));

			}

			{
				JSON r = f.statGet();
				JSON w = f.statDown();

				_File.update(Local.id(), r.append("name", "get"));
				_File.update(Local.id(), w.append("name", "down"));

			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.worker.WorkerTask.onFinish()
	 */
	@Override
	public void onFinish() {
		this.schedule(X.AMINUTE);
	}

}
