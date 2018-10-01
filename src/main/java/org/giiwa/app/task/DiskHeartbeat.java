package org.giiwa.app.task;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.conf.Local;
import org.giiwa.core.bean.X;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.DFile;
import org.giiwa.framework.bean.Disk;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;
import net.contentobjects.jnotify.JNotifyListener;

public class DiskHeartbeat extends Task implements JNotifyListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(DiskHeartbeat.class);

	public static DiskHeartbeat inst = new DiskHeartbeat();

	private List<Disk> path = new ArrayList<Disk>();

	long interval = X.AHOUR;

	@Override
	public void onExecute() {
		interval = X.AHOUR;

		int mask = JNotify.FILE_CREATED | JNotify.FILE_DELETED | JNotify.FILE_RENAMED;

		W q = W.create().and("node", Local.id()).sort("created", 1);
		Beans<Disk> l1 = Disk.dao.load(q, 0, 10);
		if (l1 != null && !l1.isEmpty()) {
			interval = 3000;
			for (Disk d : l1) {

				Disk.dao.update(d.getId(), V.create("bad", 0).append("lasttime", System.currentTimeMillis()));

				if (System.currentTimeMillis() - d.getLong("checktime") > X.AMINUTE) {
					d.check();
				}

				if (!path.contains(d)) {
					try {
						int t = JNotify.addWatch(d.getPath(), mask, true, this);
						d.set("watchid", t);
						path.add(d);
					} catch (JNotifyException e) {
						log.error(e.getMessage(), e);
					}
				}

			}
		}

	}

	@Override
	public String getName() {
		return "dfile.disk.heartbeat";
	}

	@Override
	public void onFinish() {
		this.schedule(interval);
	}

	public static void init() {
		inst.schedule(0);
	}

	boolean add(String rootPath, String name) {
		String filename = X.getCanonicalPath(rootPath + "/" + name);
		for (Disk s : path) {
			if (filename.startsWith(s.getPath())) {
				filename = filename.replace(s.getPath(), "");

				try {
					if (!DFile.dao.exists(W.create("disk", s.getId()).and("filename", filename))) {
						DFile.create(V.create("disk", s.getId()).append("filename", filename));
					}
				} catch (SQLException e) {
					log.error(e.getMessage(), e);
				}

				return true;
			}
		}
		return false;
	}

	boolean delete(String rootPath, String name) {
		String filename = X.getCanonicalPath(rootPath + "/" + name);
		for (Disk s : path) {
			if (filename.startsWith(s.getPath())) {
				filename = filename.replace(s.getPath(), "");

				DFile.dao.delete(W.create("disk", s.getId()).and("filename", filename));
				return true;
			}
		}
		return false;
	}

	@Override
	public void fileCreated(int wd, String rootPath, String name) {
		if (!add(rootPath, name)) {
			try {
				JNotify.removeWatch(wd);
			} catch (JNotifyException e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	@Override
	public void fileDeleted(int wd, String rootPath, String name) {
		if (!delete(rootPath, name)) {
			try {
				JNotify.removeWatch(wd);
			} catch (JNotifyException e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	@Override
	public void fileModified(int wd, String rootPath, String name) {
		// ignore
	}

	@Override
	public void fileRenamed(int wd, String rootPath, String oldName, String newName) {
		if (!delete(rootPath, oldName)) {
			try {
				JNotify.removeWatch(wd);
			} catch (JNotifyException e) {
				log.error(e.getMessage(), e);
			}
		}
		add(rootPath, newName);
	}

}
