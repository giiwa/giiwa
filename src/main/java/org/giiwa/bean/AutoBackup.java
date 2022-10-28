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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.app.web.admin.backup;
import org.giiwa.app.web.admin.backup.BackupTask;
import org.giiwa.dao.Bean;
import org.giiwa.dao.BeanDAO;
import org.giiwa.dao.Column;
import org.giiwa.dao.MongoHelper;
import org.giiwa.dao.RDSHelper;
import org.giiwa.dao.Table;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dfile.DFile;
import org.giiwa.misc.Url;
import org.giiwa.net.client.FTP;
import org.giiwa.net.client.SFTP;
import org.giiwa.web.Language;

/**
 * The AuthBackup bean. <br>
 * table="gi_authbackup"
 * 
 * @author wujun
 *
 */
@Table(name = "gi_autobackup", memo = "GI-自动备份")
public final class AutoBackup extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(AutoBackup.class);

	public static final BeanDAO<Long, AutoBackup> dao = BeanDAO.create(AutoBackup.class);

	@Column(memo = "唯一序号")
	public long id;

	@Column(memo = "备份名称")
	public String name;

	@Column(memo = "备份表")
	public String table;

	@Column(memo = "开启", value = "1=yes")
	public int enabled;

	@Column(memo = "状态", value = "1=running")
	public int state;

	@Column(memo = "下次运行时间点")
	public long nextime;

	@Column(memo = "备份时间点", value = "HH:mm")
	public String time;

	@Column(memo = "备份星期几，多个','分隔", value = "0,1,2,3,..")
	public String days;

	@Column(memo = "异地存储点")
	public String url;

	@Column(memo = "自动清除", value = "1=yes")
	public int clean;

	@Column(memo = "本地保留几个")
	public int keeps;

	public static long create(V v) {

		try {
			long id = dao.next();
			while (dao.exists(id)) {
				// update
				id = dao.next();
			}

			// insert
			dao.insert(v.force(X.ID, id));
			return id;
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
		}
		return -1;
	}

	public boolean isRemote() {
		return !X.isEmpty(url);
	}

	transient List<String> table_obj;

	public List<String> getTable_obj() {
		if (table_obj == null) {
			table_obj = Arrays.asList(X.split(table, ","));
		}
		return table_obj;
	}

	public boolean table(String table) {
		getTable_obj();
		return table_obj.contains(table);
	}

	transient List<Integer> day_obj;

	public List<Integer> getDay_obj() {
		if (day_obj == null) {
			day_obj = X.asList(X.split(days, ","), s -> X.toInt(s));
		}
		return day_obj;
	}

	public boolean day(int day) {
		getDay_obj();
		return day_obj.contains(day);
	}

	public void next(V v) {

		if (v == null) {
			v = V.create();
		}

		List<Integer> days = this.getDay_obj();

		if (days == null || days.isEmpty()) {
			dao.update(id, v.append("nextime", -1));
			return;
		}

		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());

		{
			String[] ss = X.split(time, ":");
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.HOUR_OF_DAY, X.toInt(ss[0]));
			cal.set(Calendar.MINUTE, X.toInt(ss[1]));
		}

		List<Long> l1 = new ArrayList<Long>();

		for (int d : days) {
			cal.set(Calendar.DAY_OF_WEEK, d);
			long t1 = cal.getTimeInMillis();
			if (t1 < System.currentTimeMillis()) {
				l1.add(t1 + X.AWEEK);
			} else {
				l1.add(t1);
			}
		}

		Collections.sort(l1);
		dao.update(id, v.append("nextime", l1.get(0)).ignore("updated"));

	}

	public static String ROOT = "/backup/";

	public void backup() {

		try {

			dao.update(id, V.create().append("state", 1));

			// clean up
			clean();

			// Module m = Module.home;
			String name = this.name + "_" + Language.getLanguage().format(System.currentTimeMillis(), "yyyyMMddHHmm")
					+ ".zip";

			try {

				Temp t = Temp.create(name);

				ZipOutputStream out = t.getZipOutputStream();

				try {

					String[] cc = this.getTable_obj().toArray(new String[this.getTable_obj().size()]);

					/**
					 * 1, backup db
					 */
					if (MongoHelper.inst.isConfigured()) {
						MongoHelper.inst.backup(out, cc);
					}
					if (RDSHelper.inst.isConfigured()) {
						RDSHelper.inst.backup(out, cc);
					}

					if (log.isDebugEnabled()) {
						log.debug("zipping, dir=" + out);
					}

					out.close();
				} finally {
					X.close(out);
				}

				Disk.seek(ROOT + name).upload(t.getInputStream());

				if (!X.isEmpty(url)) {
					// store in other
					if (url.startsWith("ftp://")) {

						Url u = Url.create(url);
						FTP f1 = FTP.create(u);
						if (f1 != null) {
							InputStream in = t.getInputStream();
							try {
								if (log.isDebugEnabled()) {
									log.debug("ftp put, filename=" + u.get("path") + "/" + name);
								}

								f1.put(u.get("path") + "/" + name, in);

								GLog.applog.info("backup", "auto", "backup success, name=" + name + ".zip", null, null);
							} finally {
								X.close(in);
								f1.close();
							}
						}

					} else if (url.startsWith("sftp://")) {

						InputStream in = t.getInputStream();
						SFTP s1 = null;
						try {
							Url u = Url.create(url);
							s1 = SFTP.create(u);
							s1.put(u.get("path") + "/" + name, in);
						} catch (Exception e) {
							log.error(e.getMessage(), e);
						} finally {
							X.close(in, s1);
						}

					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				GLog.oplog.error(backup.class, "backup", e.getMessage(), e, null, null);
			}

		} finally {
			this.next(V.create().append("state", 0));
		}

	}

	public void clean() {

		try {
			DFile f = Disk.seek(BackupTask.ROOT);
			List<DFile> l1 = new ArrayList<DFile>();
			if (f.exists()) {
				DFile[] ff = f.listFiles();
				if (ff != null && ff.length > 0) {
					for (DFile f1 : ff) {
						if (f1.getName().startsWith(name + "_")) {
							l1.add(f1);
						}
					}
				}
			}

			Collections.sort(l1, new Comparator<DFile>() {

				@Override
				public int compare(DFile o1, DFile o2) {
					return o2.getName().compareTo(o1.getName());
				}

			});

			while (l1.size() > keeps) {
				DFile f1 = l1.remove(keeps);
				f1.delete();
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

}
