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

import java.io.File;
import java.util.List;

import org.giiwa.app.web.admin.autodeploy;
import org.giiwa.bean.Disk;
import org.giiwa.bean.GLog;
import org.giiwa.bean.Temp;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.X;
import org.giiwa.dfile.DFile;
import org.giiwa.json.JSON;
import org.giiwa.misc.MD5;
import org.giiwa.net.client.Http;
import org.giiwa.task.Task;
import org.giiwa.web.Language;
import org.giiwa.web.Module;

public class AutodeployTask extends Task {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static AutodeployTask inst = new AutodeployTask();

	long interval = X.AMINUTE;
	String upgradeurl;

	@Override
	public String getName() {
		return "auto.deploy";
	}

	private AutodeployTask() {

	}

	@Override
	public void onExecute() {

		interval = Local.getInt("autodeploy.interval", 60) * X.AMINUTE;

		if (Local.getInt("autodeploy.enabled", 0) == 0) {
			log.info("autodeploy.enabled=0, disabled!");
			return;
		}

		String timerange = Local.getString("autodeploy.timerange", "02:00-06:00");
		if (!X.timeIn(timerange)) {
			log.info("not in time range!");
			return;
		}

		Http h = Http.create();
		String modules = Local.getString("autodeploy.modules", null);
		upgradeurl = Global.getString("autodeploy.url", null);
		String url = upgradeurl;
		if (!X.isEmpty(url) && !X.isEmpty(modules)) {
			while (url.startsWith("/")) {
				url = url.substring(1);
			}
			while (url.endsWith("/")) {
				url = url.substring(0, url.length() - 1);
			}
			if (!url.endsWith("/admin/module/query")) {
				url += "/admin/module/query";
			}

			boolean restart = false;

			JSON jo = JSON.create();
			jo.put("name", modules);

			Http.Response r = h.post(url, jo);
			log.info("remote module=" + modules + ", resp=" + r.body);

			JSON j1 = JSON.fromObject(r.body);
			if (j1 != null && j1.getInt(X.STATE) == 200) {
				if (j1.has("list")) {
					List<JSON> list = j1.getList("list");
					for (JSON j2 : list) {
						if (_upgrade(url, j2)) {
							restart = true;
						}
					}
				}
			}

			if (restart) {

				// TODO, time is not OK? avoid to download again ?
//				if (!X.timeIn(timerange)) {
//					log.info("not in time range!");
//					return;
//				}

				GLog.applog.warn(autodeploy.class, "restart", "autodeploy shutdown the server", null, upgradeurl);

				Task.resume();

				Task.schedule(t -> {
					System.exit(0);
				}, 2000);
			}
		}
	}

	private boolean _upgrade(String url, JSON j1) {
		String name = j1.getString("name");
		Module m = Module.load(name);
		if (m == null || !X.isSame(m.getVersion(), j1.getString("version"))
				|| !X.isSame(m.getBuild(), j1.getString("build"))) {

			String uri = j1.getString("uri");
			int i = url.indexOf("/", 10);
			url = url.substring(0, i) + uri;

			File f = _download(url, j1.getString("md5"));
			if (f != null) {

				GLog.applog.info(autodeploy.class, "download", f.getName(), null, upgradeurl);

				if (_upgrade(f.getName(), f)) {
					return true;
				}
			} else {
				// download error
				interval = X.AMINUTE;
			}
		} else {
			log.info("ignore [" + name + "]");
		}

		return false;
	}

	private boolean _upgrade(String name, File f) {
		try {
			Language lang = Language.getLanguage();
			DFile f1 = Disk.seek(
					"/temp/" + lang.format(Global.now(), "yyyy/MM/dd/HH/mm/ss") + "/" + f.getName());
			f1.upload(f);
			boolean restart = Module.prepare(f1);
			GLog.applog.info(autodeploy.class, "upgrade", "success, name=" + name, null, null);
			return restart;
		} catch (Exception e) {
			interval = X.AMINUTE;
			GLog.applog.warn(autodeploy.class, "upgrade", "failed, name=" + name, null, null);
		}
		return false;
	}

	public File _download(String url, String md5) {

		Http h = Http.create();
		// System.out.println("url=" + url);
		try {

			Temp t = h.download(url, true);
			long len = t.length();
			if (len > 0) {
				String m1 = MD5.md5(t.getInputStream());
				if (X.isSame(md5, m1)) {
//					System.out.println("ok, url=" + url + ", md5=" + m1 + ", expected=" + md5 + ", len=" + len);
					return t.getFile();
				} else {
//					System.out.println("failed, url=" + url + ", md5=" + m1 + ", expected=" + md5 + ", len=" + len);
					log.error("failed, url=" + url + ", md5=" + m1 + ", expected=" + md5 + ", len=" + len + ", file="
							+ t.getFile().getAbsolutePath());
					GLog.applog.warn(autodeploy.class, "download",
							"failed, url=" + url + ", md5=" + m1 + ", expected=" + md5 + ", len=" + len, null, null);
				}
			} else {
				log.error("failed, url=" + url + ", size=0");
				GLog.applog.warn(autodeploy.class, "download", "failed, url=" + url + ", size=0", null, null);
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public void onFinish() {
		if (Local.getInt("autodeploy.enabled", 0) == 0) {
			log.info("autodeply.enabled=0, disabled!");
			return;
		}

		this.schedule(interval);
	}

	public static void main(String[] args) {

		Temp.ROOT = "/Users/joe/Downloads/temp";
		AutodeployTask t = new AutodeployTask();
		File f1 = t._download(
				"http://s01.giisoo.com/f/d/f52gk3lqf5zc6qzpoaxuclzyhe2tinrxgezdsnrzgy3daojyhazdcl3tobuwizlsl4ys4nk7giztanbrgiytcmzwfz5gs4a/spider_1.5_2304121136.zip",
				"8e0b640615ef13b4e5c2b37d3b37f281");
		System.out.println(f1.getAbsolutePath());

	}

}
