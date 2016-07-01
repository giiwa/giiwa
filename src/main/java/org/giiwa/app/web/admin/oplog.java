/*
 *   giiwa, a java web foramewrok.
 *   Copyright (C) <2014>  <giiwa.org>
 *
 */
package org.giiwa.app.web.admin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.regex.Pattern;

import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.OpLog;
import org.giiwa.framework.bean.Session;
import org.giiwa.framework.bean.Temp;
import org.giiwa.framework.web.*;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.mongodb.BasicDBObject;

// TODO: Auto-generated Javadoc
/**
 * web api: /admin/log
 * <br>
 * used to manage oplog
 * 
 * @author joe
 *
 */
public class oplog extends Model {

	/**
   * Deleteall.
   */
	@Path(path = "deleteall", login = true, access = "access.config.admin")
	public void deleteall() {
		JSONObject jo = new JSONObject();
		OpLog.remove();
		jo.put(X.STATE, 200);
		this.response(jo);
	}

	/**
	 * Popup2.
	 */
	@Path(path = "popup2", login = true)
	public void popup2() {
		String type = this.getString("type");
		String cate = this.getString("cate");

		JSONObject jo = new JSONObject();

		List<String> list = null;// OpLog.loadCategory(type, cate);
		if (list != null && list.size() > 0) {
			JSONArray arr = new JSONArray();
			for (String e : list) {
				JSONObject j = new JSONObject();
				j.put("value", e);
				if ("module".equals(type)) {
					j.put("name", lang.get("log.module_" + e));
				} else if ("op".equals(type)) {
					j.put("name", lang.get("log.opt_" + e));
				} else {
					j.put("name", e);
				}
				arr.add(j);
			}
			jo.put("list", arr);
			jo.put(X.STATE, 200);

		} else {
			jo.put(X.STATE, 201);
		}

		this.response(jo);

	}

	private BasicDBObject getW(JSONObject jo) {

		BasicDBObject q = new BasicDBObject();

		if (!X.isEmpty(jo.get("op"))) {
			q.append("op", jo.get("op"));
		}
		if (!X.isEmpty(jo.get("ip"))) {
			q.append("ip", Pattern.compile(jo.getString("ip"), Pattern.CASE_INSENSITIVE));
		}
		if (!X.isEmpty(jo.get("uid"))) {
			q.append("uid", Bean.toInt(jo.get("uid")));
		}
		if (!X.isEmpty(jo.get("type"))) {
			q.append("type", Bean.toInt(jo.get("type")));
		}

		if (jo.has("_module") && !X.isEmpty(jo.getString("_module"))) {
			q.append("module", jo.getString("_module"));
		}

		if (jo.has("_system") && !X.isEmpty(jo.getString("_system"))) {
			q.append("system", jo.getString("_system"));
		}

		if (jo.has("starttime") && !X.isEmpty(jo.getString("starttime"))) {
			q.append("created",
					new BasicDBObject().append("$gte", lang.parse(jo.getString("starttime"), "yyyy-MM-dd")));

		} else {
			long today_2 = System.currentTimeMillis() - X.ADAY * 2;
			jo.put("starttime", lang.format(today_2, "yyyy-MM-dd"));
			q.append("created", new BasicDBObject().append("$gte", today_2));
		}

		if (jo.has("endtime") && !X.isEmpty(jo.getString("endtime"))) {
			q.append("created", new BasicDBObject().append("$lte", lang.parse(jo.getString("endtime"), "yyyy-MM-dd")));
		}

		this.set(jo);

		return q;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model#onGet()
	 */
	@Path(login = true, access = "access.log.admin")
	public void onGet() {

		int s = this.getInt("s");
		int n = this.getInt("n", 10, "number.per.page");

		this.set("currentpage", s);

		JSONObject jo = this.getJSON();
		BasicDBObject w = getW(jo);

		Beans<OpLog> bs = OpLog.load(w, s, n);
		this.set(bs, s, n);

		this.query.path("/admin/oplog");
		this.show("/admin/oplog.index.html");
	}

	/**
	 * Export.
	 */
	@Path(path = "export", login = true, access = "access.log.admin")
	public void export() {

		/**
		 * export the logs to "csv" file, and redirect to the cvs file
		 */

		final JSONObject jo = this.getJSON();
		final BasicDBObject q = getW(jo);

		String id = UID.id(login.get("name"), jo.toString());
		String name = "oplog_" + lang.format(System.currentTimeMillis(), "yyyMMdd") + ".csv";
		final File f = Temp.get(id, name);

		if (f.exists() && System.currentTimeMillis() - f.lastModified() > X.AHOUR) {
			f.delete();
		} else {
			f.getParentFile().mkdirs();
		}

		if (!f.exists()) {
			final Session session = this.getSession();
			session.set("oplog.exporting", 1).store();
			new Task() {

				@Override
				public void onExecute() {
					try {
						int s = 0;

						BufferedWriter out = new BufferedWriter(
								new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));

						/**
						 * output the header
						 */
						StringBuilder sb = new StringBuilder();
						sb.append("\"").append(lang.get("log.created")).append("\",\"");
						sb.append(lang.get("log.user")).append("\",\"");
						sb.append(lang.get("log.ip")).append("\",\"");
						sb.append(lang.get("log.system")).append("\",\"");
						sb.append(lang.get("log.module")).append("\",\"");
						sb.append(lang.get("log.op")).append("\", \"");
						sb.append(lang.get("log.message")).append("\"");
						out.write(sb.toString() + "\r\n");

						Beans<OpLog> bs = OpLog.load(q, s, 100);
						while (bs != null && bs.getList() != null && bs.getList().size() > 0) {
							for (OpLog p : bs.getList()) {
								sb = new StringBuilder();
								sb.append("\"").append(lang.format(p.getLong("created"), "yyyy-MM-dd hh:mm:ss"))
										.append("\",\"");

								if (p.getUser() != null) {
									sb.append(p.getUser().get("name")).append("\",\"");
								} else {
									sb.append("\",\"");
								}

								if (X.isEmpty(p.get("ip"))) {
									sb.append(p.get("ip")).append("\",\"");
								} else {
									sb.append("\",\"");
								}

								if (p.getSystem() != null) {
									sb.append(p.getSystem()).append("\",\"");
								} else {
									sb.append("\",\"");
								}
								if (p.getModule() != null) {
									sb.append(lang.get("log.module_" + p.getModule())).append("\",\"");
								} else {
									sb.append("\",\"");
								}

								if (p.getOp() != null) {
									sb.append(lang.get("log.opt_" + p.getOp())).append("\"");
								} else {
									sb.append("\",\"");
								}

								if (p.getMessage() != null) {
									sb.append(p.getMessage()).append("\"");
								} else {
									sb.append("\",\"");
								}

								out.write(sb.toString() + "\r\n");
							}
							s += bs.getList().size();
							bs = OpLog.load(q, s, 100);
						}

						out.close();

						OpLog.info(OpLog.class, "export", jo.toString(), (String) null, login.getId(),
								oplog.this.getRemoteHost());

					} catch (Exception e) {
						log.error(e.getMessage(), e);
					} finally {
						Session.load(session.sid()).remove("oplog.exporting").store();
					}
				}

			}.schedule(0);

		}

		JSONObject jo1 = new JSONObject();
		jo1.put(X.STATE, 200);
		jo1.put("file", "/temp/" + id + "/" + name);
		jo1.put("size", f.length());
		jo1.put("updated", f.lastModified());
		if (this.getSession().get("oplog.exporting") != null) {
			jo1.put("exporting", 1);
		} else {
			jo1.put("exporting", 0);
		}

		this.response(jo1);
	}
}
