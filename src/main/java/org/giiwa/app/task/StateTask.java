package org.giiwa.app.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.base.Host;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.core.conf.Local;
import org.giiwa.core.json.JSON;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.m._CPU;
import org.giiwa.framework.bean.m._Disk;
import org.giiwa.framework.bean.m._Memory;
import org.giiwa.framework.bean.m._Net;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;

public class StateTask extends Task {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static Log log = LogFactory.getLog(StateTask.class);

	public static StateTask owner = new StateTask();

	@Override
	public String getName() {
		return "state.task";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.worker.WorkerTask#onExecute()
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
				jo.append("usage", jo.getDouble("user") + jo.getDouble("sys"));
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

				_Memory.update(Local.id(), jo);
			}

			// disk
			Collection<JSON> l1 = Host.getDisks();
			if (l1 != null && !l1.isEmpty()) {
				List<JSON> l2 = new ArrayList<JSON>();
				for (JSON j1 : l1) {
					l2.add(JSON.create().append("path", j1.getString("dirname")).append("name", j1.getString("devname"))
							.copy(j1, "total", "free", "used"));
				}
				_Disk.update(Local.id(), l2);
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
							.append("type", "snapshot"));
				}
				_Net.update(Local.id(), l2);
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.worker.WorkerTask#onFinish()
	 */
	@Override
	public void onFinish() {
		this.schedule(X.AMINUTE);
	}

}
