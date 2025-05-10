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
package org.giiwa.app.web.admin;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.fileupload2.core.FileItem;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.giiwa.bean.*;
import org.giiwa.conf.Config;
import org.giiwa.conf.Global;
import org.giiwa.dao.X;
import org.giiwa.dfile.DFile;
import org.giiwa.json.JSON;
import org.giiwa.misc.IOUtil;
import org.giiwa.misc.MD5;
import org.giiwa.misc.RSA;
import org.giiwa.misc.Shell;
import org.giiwa.task.Task;
import org.giiwa.web.Controller;
import org.giiwa.web.Module;
import org.giiwa.web.Path;

/**
 * web api: /admin/module <br>
 * used to manage modules, <br>
 * required "access.config.admin" except "create a module"
 * 
 * @author joe
 *
 */
public class module extends Controller {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static String ROOT = "/tmp/modules/";

	/**
	 * create a new module.
	 */
	@Path(path = "create", login = true, oplog = true)
	public void create() {
		if (method.isPost()) {
			/**
			 * create
			 */
			JSON jo = new JSON();

			try {
				String file = createmodule();
				jo.put(X.STATE, 200);
				jo.put("file", file);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				GLog.oplog.error(this, "create", e.getMessage(), e);

				this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE, e.getMessage()));
				return;

			}
			this.send(jo);
		} else {
			this.set("id", Global.getInt("module.id.next", 100));
			this.show("/admin/module.create.html");
		}
	}

	/**
	 * Createmodule.
	 *
	 * @return the string
	 * @throws Exception the exception
	 */
	public String createmodule() throws Exception {

		int id = this.getInt("id");
		String name = this.getString("name");
		String package1 = this.getString("package");
		String lifelistener = package1 + "." + name.substring(0, 1).toUpperCase() + name.substring(1) + "Listener";
		String readme = this.getString("readme");

		Temp t = Temp.create(name + ".zip");

		ZipOutputStream out = null;

		try {
			out = t.getZipOutputStream();

			RSA.Key key = RSA.generate(1024);

			String modulecode = Base64.getEncoder().encodeToString(RSA.encode("giiwa".getBytes(), key.pub_key));

			/**
			 * create project info
			 */
			create(out, name + "/").closeEntry();
			create(out, name + "/.project");
			File f1 = module.getFile("/admin/demo/.project");
			if (f1.exists()) {
				// copy to out
				copy(out, f1, new String[] { "demo", name });
			}

			create(out, name + "/depends/").closeEntry();
			List<String> list = new ArrayList<String>();

			// boolean none = Jar.dao.count(W.create("module", "default")) == 0;
			f1 = new File(org.giiwa.web.Module.load("default").getPath() + "/WEB-INF/lib/");

			File[] ff1 = f1.listFiles();
			if (ff1 != null) {
				for (File f2 : ff1) {
					try {
						// if (includes || none || Jar.dao.exists(W.create("module",
						// "default").and("name", f2.getName()))) {
						if (f2.isFile()) {
							if (log.isDebugEnabled())
								log.debug("name=" + f2.getName());

							list.add(f2.getName());
							create(out, name + "/depends/" + f2.getName());
							copy(out, f2);
							out.closeEntry();
						}
						// }
					} catch (Exception e) {
						log.error(f2.getName(), e);
					}
				}
			}

			create(out, name + "/.classpath");
			f1 = module.getFile("/admin/demo/.classpath");
			copy(out, f1, list);
			out.closeEntry();

			create(out, name + "/build.gradle");
			f1 = module.getFile("/admin/demo/build.gradle");
			if (f1 != null) {
				copy(out, f1, new String[] { "demo", name }, new String[] { "readme", readme });
			}
			out.closeEntry();

			create(out, name + "/pubkey.txt");
			out.write(key.pub_key.getBytes());
			out.closeEntry();

			create(out, name + "/src/").closeEntry();
			create(out, name + "/README.md");
			f1 = module.getFile("/admin/demo/README.md");
			if (f1 != null) {
				copy(out, f1);
			}
			out.closeEntry();

			create(out, name + "/src/module.xml");

			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("module");
			root.addAttribute("version", "0");

			Element e = root.addElement("id");
			e.setText(Integer.toString(id));

			e = root.addElement("name");
			e.setText(name);

			e = root.addElement("package");
			e.setText(package1);

			e = root.addElement("version");
			e.setText("0");
			e = root.addElement("build");
			e.setText("0");
			e = root.addElement("enabled");
			e.setText("true");
			e = root.addElement("readme");
			e.setText(readme);
			e = root.addElement("listener");
			e.addComment("add module listener here, please refer IListener interface");
			e = e.addElement("class");
			e.setText(lifelistener);

			// e = root.addElement("filter");
			// e.addComment(
			// ", please refer web.IFilter, eg.
			// <pattern>/user/login</pattern>,<class>org.giiwa.demo.web.UserFiler</class>");

			e = root.addElement("required");
			e.addComment("add all required modules here");
			Element e1 = e.addElement("module");
			e1.addAttribute("name", "default");
			org.giiwa.web.Module m0 = org.giiwa.web.Module.load("default");
			e1.addAttribute("minversion", m0.getVersion() + "." + m0.getBuild());
			e1.addAttribute("maxversion", m0.getVersion() + ".*");

			e = root.addElement("key");
			e.setText(key.pri_key);

			OutputFormat format = OutputFormat.createPrettyPrint();
			format.setEncoding("UTF-8");
			XMLWriter writer = new XMLWriter(out, format);
			writer.write(doc);

			out.closeEntry();
			// end of project info

			/**
			 * create helpful folder
			 */
			create(out, name + "/src/WEB-INF/").closeEntry();
			create(out, name + "/src/WEB-INF/lib/").closeEntry();

			/**
			 * create i18n info
			 */
			create(out, name + "/src/i18n/").closeEntry();
			f1 = module.getFile("/admin/demo/src/i18n/");
			if (f1.exists()) {
				ff1 = f1.listFiles();
				if (ff1 != null) {
					for (File f2 : ff1) {
						if (f2.isFile()) {
							create(out, name + "/src/i18n/" + f2.getName());
							copy(out, f2);
							out.closeEntry();
						}
					}
				}
			}
			// end of i18n

			/**
			 * create resources info
			 */
			create(out, name + "/src/init/").closeEntry();
			f1 = module.getFile("/admin/demo/src/init/");
			if (f1.exists()) {
				ff1 = f1.listFiles();
				if (ff1 != null) {
					for (File f2 : ff1) {
						if (f2.isFile()) {
							create(out, name + "/src/init/" + f2.getName());
							copy(out, f2);
							out.closeEntry();
						} else if (f2.isDirectory()) {
							create(out, name + "/src/init/" + f2.getName() + "/").closeEntry();
							// copy all files
							File[] ff2 = f2.listFiles();
							if (ff2 != null) {
								for (File f3 : ff2) {
									if (f3.isDirectory()) {
										create(out, name + "/src/init/" + f2.getName() + "/" + f3.getName() + "/")
												.closeEntry();
										File[] ff3 = f3.listFiles();
										if (ff3 != null) {
											for (File f4 : ff3) {
												if (f4.isFile()) {
													create(out, name + "/src/init/" + f2.getName() + "/" + f3.getName()
															+ "/" + f4.getName());
													copy(out, f4);
													out.closeEntry();
												}
											}
										}
									} else {
										create(out, name + "/src/init/" + f2.getName() + "/" + f3.getName());
										copy(out, f3);
										out.closeEntry();
									}
								}
							}
						}
					}
				}
			}
			// end of resources

			/***
			 * create junit test
			 */
			{
				create(out, name + "/src/test/").closeEntry();
				create(out, name + "/src/test/java/").closeEntry();
				String[] ss = package1.split("\\.");
				String s1 = null;
				for (String s : ss) {
					if (s1 == null) {
						s1 = s;
					} else {
						s1 = s1 + "/" + s;
					}
					create(out, name + "/src/test/java/" + s1 + "/").closeEntry();
				}

				String p1 = package1.replaceAll("\\.", "/");
				create(out, name + "/src/test/java/" + p1.substring(0, p1.lastIndexOf("/")) + "/bean/").closeEntry();

				create(out, name + "/src/test/java/" + p1.substring(0, p1.lastIndexOf("/")) + "/bean/DemoTest.java");
				f1 = module.getFile("/admin/demo/src/test/bean/DemoTest.java");
				if (f1 != null) {
					copy(out, f1, new String[] { "org.giiwa.demo.bean",
							(p1.substring(0, p1.lastIndexOf("/")) + "/bean").replaceAll("/", ".") });
				}
				out.closeEntry();

			}

			/**
			 * create bean info
			 */
			create(out, name + "/src/model/").closeEntry();
			create(out, name + "/src/model/java/").closeEntry();
			String[] ss = package1.split("\\.");
			String s1 = null;
			for (String s : ss) {
				if (s1 == null) {
					s1 = s;
				} else {
					s1 = s1 + "/" + s;
				}
				create(out, name + "/src/model/java/" + s1 + "/").closeEntry();
			}

			// create bean
			String p1 = package1.replaceAll("\\.", "/");
			create(out, name + "/src/model/java/" + p1.substring(0, p1.lastIndexOf("/")) + "/bean/").closeEntry();

			create(out, name + "/src/model/java/" + p1.substring(0, p1.lastIndexOf("/")) + "/bean/Demo.java");
			f1 = module.getFile("/admin/demo/src/model/bean/Demo.java");
			if (f1 != null) {
				copy(out, f1, new String[] { "org.giiwa.demo.bean",
						(p1.substring(0, p1.lastIndexOf("/")) + "/bean").replaceAll("/", ".") });
			}
			out.closeEntry();

			// create tast
			create(out, name + "/src/model/java/" + p1.substring(0, p1.lastIndexOf("/")) + "/task/").closeEntry();

			create(out, name + "/src/model/java/" + p1.substring(0, p1.lastIndexOf("/")) + "/task/TestTask.java");
			f1 = module.getFile("/admin/demo/src/model/task/TestTask.java");
			if (f1 != null) {
				copy(out, f1, new String[] { "org.giiwa.demo.task",
						(p1.substring(0, p1.lastIndexOf("/")) + "/task").replaceAll("/", ".") });
			}
			out.closeEntry();

			// create server
			create(out, name + "/src/model/java/" + p1.substring(0, p1.lastIndexOf("/")) + "/server/").closeEntry();

			create(out, name + "/src/model/java/" + p1.substring(0, p1.lastIndexOf("/")) + "/server/DemoServer.java");
			f1 = module.getFile("/admin/demo/src/model/server/DemoServer.java");
			if (f1 != null) {
				copy(out, f1, new String[] { "org.giiwa.demo.server",
						(p1.substring(0, p1.lastIndexOf("/")) + "/server").replaceAll("/", ".") });
			}
			out.closeEntry();

			// copy demo model
			create(out, name + "/src/model/java/" + p1 + "/demo.java");
			f1 = module.getFile("/admin/demo/src/model/web/demo.java");
			if (f1 != null) {
				copy(out, f1, new String[] { "org.giiwa.demo.web", p1.replaceAll("/", ".") }, new String[] {
						"org.giiwa.demo.bean", (p1.substring(0, p1.lastIndexOf("/")) + "/bean").replaceAll("/", ".") });
			}
			out.closeEntry();

			if (!X.isEmpty(lifelistener)) {
				create(out, name + "/src/model/java/" + lifelistener.replaceAll("\\.", "/") + ".java");
				f1 = module.getFile("/admin/demo/src/model/web/DemoListener.java");
				if (f1 != null) {
					copy(out, f1,
							new String[] { "org.giiwa.demo.web",
									lifelistener.substring(0, lifelistener.lastIndexOf(".")) },
							new String[] { "DemoListener", lifelistener.substring(lifelistener.lastIndexOf(".") + 1) },
							new String[] { "demo", name }, new String[] { "modulecode", modulecode });
				}
				out.closeEntry();
			}

			// copy admin/demo model
			create(out, name + "/src/model/java/" + p1 + "/admin/").closeEntry();
			create(out, name + "/src/model/java/" + p1 + "/admin/demo.java");
			f1 = module.getFile("/admin/demo/src/model/web/admin/demo.java");
			if (f1 != null) {
				copy(out, f1, new String[] { "org.giiwa.demo.web", p1.replaceAll("/", ".") }, new String[] {
						"org.giiwa.demo.bean", (p1.substring(0, p1.lastIndexOf("/")) + "/bean").replaceAll("/", ".") });
			}
			out.closeEntry();

			create(out, name + "/src/model/java/" + p1 + "/admin/" + name + "setting.java");
			f1 = module.getFile("/admin/demo/src/model/web/admin/demosetting.java");
			if (f1 != null) {
				copy(out, f1, new String[] { "org.giiwa.demo.web", p1.replaceAll("/", ".") },
						new String[] { "demosetting", name + "setting" }, new String[] { "org.giiwa.demo.bean",
								(p1.substring(0, p1.lastIndexOf("/")) + "/bean").replaceAll("/", ".") });
			}
			out.closeEntry();

			// end of model

			/**
			 * create view info
			 */
			create(out, name + "/src/view/").closeEntry();
			f1 = module.getFile("/admin/demo/src/view/");
			if (f1.exists()) {
				ff1 = f1.listFiles();
				if (ff1 != null) {
					for (File f2 : ff1) {
						if (f2.isFile()) {
							create(out, name + "/src/view/" + f2.getName());
							copy(out, f2);
							out.closeEntry();
						}
					}
				}
			}
			create(out, name + "/src/view/admin/").closeEntry();
			f1 = module.getFile("/admin/demo/src/view/admin");
			if (f1 != null) {
				ff1 = f1.listFiles();
				if (ff1 != null) {
					for (File f2 : ff1) {
						if (f2.isFile()) {
							create(out, name + "/src/view/admin/" + f2.getName());
							copy(out, f2);
							out.closeEntry();
						}
					}
				}
			}
			create(out, name + "/src/view/js/").closeEntry();
			f1 = module.getFile("/admin/demo/src/view/js");
			if (f1 != null) {
				ff1 = f1.listFiles();
				if (ff1 != null) {
					for (File f2 : ff1) {
						if (f2.isFile()) {
							create(out, name + "/src/view/js/" + f2.getName());
							copy(out, f2);
							out.closeEntry();
						}
					}
				}
			}
			create(out, name + "/src/view/css/").closeEntry();
			f1 = module.getFile("/admin/demo/src/view/css");
			if (f1 != null) {
				ff1 = f1.listFiles();
				if (ff1 != null) {
					for (File f2 : ff1) {
						if (f2.isFile()) {
							create(out, name + "/src/view/css/" + f2.getName());
							copy(out, f2);
							out.closeEntry();
						}
					}
				}
			}
			create(out, name + "/src/view/images/").closeEntry();
			f1 = module.getFile("/admin/demo/src/view/images");
			if (f1 != null) {
				ff1 = f1.listFiles();
				if (ff1 != null) {
					for (File f2 : ff1) {
						if (f2.isFile()) {
							create(out, name + "/src/view/images/" + f2.getName());
							copy(out, f2);
							out.closeEntry();
						}
					}
				}
			}

			f1 = module.getFile("/admin/demo/src/view/install");
			if (f1 != null) {
				create(out, name + "/src/view/install/").closeEntry();
				ff1 = f1.listFiles();
				if (ff1 != null) {
					for (File f2 : ff1) {
						if (f2.isFile()) {
							create(out, name + "/src/view/install/" + f2.getName());
							copy(out, f2);
							out.closeEntry();
						}
					}
				}
			}
			// end of view

			int id1 = Global.getInt("module.id.next", 100);
			if (id >= id1) {
				Global.setConfig("module.id.next", id + 1);
			}

//			t.save(t.getFile());

		} finally {
			if (out != null) {
				out.close();
			}
		}

		return t.getUri(lang);
	}

	private ZipOutputStream copy(ZipOutputStream out, File f) throws Exception {
		InputStream in = null;
		try {
			in = new FileInputStream(f);
			IOUtil.copy(in, out, false);
		} finally {
			if (in != null) {
				in.close();
			}
		}

		return out;
	}

	private ZipOutputStream copy(ZipOutputStream out, File f, String[]... replacement) throws Exception {
		BufferedReader in = null;
		try {
			PrintStream o = new PrintStream(out);
			in = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
			String line = in.readLine();
			while (line != null) {
				if (replacement != null && replacement.length > 0) {
					for (String[] ss : replacement) {
						if (ss.length == 2) {
							if (X.isEmpty(ss[1])) {
								line = X.EMPTY;
							} else {
								line = line.replaceAll(ss[0], ss[1]);
							}
						}
					}
				}
				o.println(line);

				line = in.readLine();
			}
		} finally {
			if (in != null) {
				in.close();
			}
		}

		return out;
	}

	private ZipOutputStream copy(ZipOutputStream out, File f, List<String> classpath) throws Exception {
		BufferedReader in = null;
		try {
			PrintStream o = new PrintStream(out);
			in = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
			String line = in.readLine();
			while (line != null) {
				if (line.indexOf("webdemo.jar") > 0) {
					for (String s : classpath) {
						String s1 = line.replaceAll("webdemo.jar", s);
						o.println(s1);
					}
				} else {
					o.println(line);
				}
				line = in.readLine();
			}
		} finally {
			if (in != null) {
				in.close();
			}
		}

		return out;
	}

	private ZipOutputStream create(ZipOutputStream out, String filename) throws IOException {
		ZipEntry e = new ZipEntry(filename);
		out.putNextEntry(e);
		return out;
	}

//	@Path(path = "license", login = true, access = "access.config.admin")
//	public void license() {
//
//		String url = this.getString(X.URL);
//		Entity e = Repo.load(url);
//		BufferedReader in = null;
//
//		try {
//			in = new BufferedReader(new InputStreamReader(e.getInputStream()));
//			String name = in.readLine();
//
//			String code = in.readLine();
//			String content = in.readLine();
//
//			License a = new License();
//			a.set(X.ID, name);
//			a.set("code", code);
//			a.set("content", content);
//
//			if (a.decode()) {
//				a.store();
//
//				this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("save.success")));
//			} else {
//				this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE, lang.get("license.bad")));
//			}
//
//		} catch (Exception e1) {
//			log.error(e1.getMessage(), e1);
//			GLog.applog.error(module.class, "license", e1.getMessage(), e1, login, this.ip());
//
//			this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE, e1.getMessage()));
//		} finally {
//			X.close(in);
//			e.delete();
//		}
//	}

	/**
	 * Adds the.
	 */
	@Path(path = "add", login = true, access = "access.config.admin", oplog = true)
	public void add() {

		String url = this.getString(X.URL);

		JSON jo = new JSON();

		try {

			if (url == null) {
				this.set(X.MESSAGE, "参数错误, 请重试!").send(201);
				return;
			}

			boolean restart = false;
			DFile d = Disk.seek(url);
			if (d.getName().endsWith(".jar")) {
				// upgrade jar file only
				List<Module> l1 = Module.getAll(true);
				for (Module m : l1) {
					if (m.upgrade(d)) {
						restart = true;
					}
				}
			} else {
				restart = org.giiwa.web.Module.prepare(d);
			}

			jo.put("result", "ok");

			if (restart) {
				jo.put(X.STATE, 201);
				jo.put(X.WARN, lang.get("restarting.giiwa"));
			} else {
				jo.put(X.STATE, 200);
				jo.put(X.WARN, lang.get("restart.required"));
			}

			GLog.oplog.warn(this, "add", d.getName());

			if (restart) {
				Task.schedule(t -> {

					// cleanup first, otherwise may cause can not be startup
					// DefaultListener.cleanup(new File(Model.HOME), new HashMap<String,
					// FileVersion>());

					log.warn("jar has been merged, going to restart");

					Task.schedule(t1 -> {
						System.exit(0);
					}, 1000);

				}, 2000);
			}

		} catch (Exception e1) {

			log.error(url, e1);

			GLog.applog.error(module.class, "add", e1.getMessage(), e1, login, this.ip());

			jo.put(X.STATE, 404);
			jo.put(X.ERROR, e1.getMessage());

		}

		org.giiwa.web.Module.reset();

		this.send(jo);

	}

	/**
	 * Index.
	 */
	@Path(login = true, access = "access.config.admin")
	public void onGet() {

		List<org.giiwa.web.Module> actives = new ArrayList<org.giiwa.web.Module>();
		org.giiwa.web.Module m = org.giiwa.web.Module.home;
		while (m != null) {
			actives.add(m);
			m = m.floor();
		}

		Configuration conf = Config.getConf();
		this.set("node", conf.getString("node.name", ""));

		this.set("actives", actives);

		this.set("pid", Shell.pid());
		this.set("uptime", Controller.UPTIME);

		long time = Global.getLong("node.time", 0);
		if (time > 0) {
			this.set("atime", lang.format(time, "yyyy-MM-dd"));
		}

		this.set("list", org.giiwa.web.Module.getAll(false));

		this.show("/admin/module.index.html");

	}

	/**
	 * Download.
	 */
	@Path(path = "download", login = true, access = "access.config.admin", oplog = true)
	public void download() {

		String name = this.getString("name");

		/**
		 * zip module
		 */
		org.giiwa.web.Module m = org.giiwa.web.Module.load(name);
		String file = ROOT + name + ".zip";
		File f = m.zipTo(Controller.GIIWA_HOME + "/modules/" + file);
		if (f != null && f.exists()) {

			this.set("f", f);
			this.set("link", file);

			this.show("/admin/module.download.html");
			return;
		} else {
			this.set(X.MESSAGE, lang.get("message.fail"));
			onGet();
		}
	}

	/**
	 * Disable.
	 */
	@Path(path = "disable", login = true, access = "access.config.admin", oplog = true)
	public void disable() {
		String name = this.getString("name");

		org.giiwa.web.Module m = org.giiwa.web.Module.load(name);
		m.setEnabled(false);

		org.giiwa.web.Module.reset();

		GLog.oplog.warn(this, "disable", name);

		onGet();
	}

	/**
	 * Update.
	 */
	@Path(path = "update", login = true, access = "access.config.admin", oplog = true)
	public void update() {
		String name = this.getString("name");
		int id = this.getInt("id");

		JSON jo = new JSON();
		if (id > 0) {
			jo.put(X.STATE, 200);
			org.giiwa.web.Module m = org.giiwa.web.Module.load(name);
			m.id = id;
			m.store();
		} else {
			jo.put(X.STATE, 201);
			jo.put(X.MESSAGE, lang.get("module.id.gt0"));
		}

		this.send(jo);
	}

	/**
	 * Enable.
	 */
	@Path(path = "enable", login = true, access = "access.config.admin", oplog = true)
	public void enable() {
		String name = this.getString("name");

		org.giiwa.web.Module m = org.giiwa.web.Module.load(name);
		m.setEnabled(true);

		org.giiwa.web.Module.reset();

		GLog.oplog.warn(this, "enable", name);

		onGet();
	}

	@Path(path = "deletelicense", login = true, access = "access.config.admin", oplog = true)
	public void deletelicense() {
		String name = this.getString("name");
		License.dao.delete(name);
		License.remove(name);

		onGet();
	}

	/**
	 * Delete.
	 */
	@Path(path = "delete", login = true, access = "access.config.admin", oplog = true)
	public void delete() {
		String name = this.getString("name");
		org.giiwa.web.Module m = org.giiwa.web.Module.load(name);
		m.delete();

		org.giiwa.web.Module.reset();

		GLog.oplog.warn(this, "delete", name);

		onGet();
	}

	@SuppressWarnings({ "unused", "rawtypes" })
	private boolean validate(FileItem file) {
		return false;
	}

	@Path(path = "query")
	public void _query() {

		String repo = Config.getConf().getString("module.repo", "no");
		if (X.isIn(repo, "yes")) {
			String name = this.getString("name");

			String[] ss = X.split(name, "[,;]");

			TreeSet<String> l1 = new TreeSet<String>();
			List<String> ex = new ArrayList<String>();
			for (String s : ss) {
				if (X.isSame(s, "*")) {
					// all
					List<Module> m1 = org.giiwa.web.Module.getAll(true);
					for (Module e : m1) {
						l1.add(e.getName());
					}
				} else if (s.startsWith("-")) {
					ex.add(s.substring(1));
				} else {
					l1.add(s);
				}
			}
			for (String s : ex) {
				l1.remove(s);
			}
			l1.remove("stream");

			List<JSON> r1 = JSON.createList();

			for (String s : l1) {

				try {
					Module m = org.giiwa.web.Module.load(s);
					repo = Global.getString("module." + s + ".repo", null);

					if (!X.isEmpty(repo)) {

						DFile f1 = Disk.seek(repo);
						if (f1 != null && f1.exists()) {

							JSON j1 = JSON.create();
							j1.append("name", s);

							j1.append("version", m.getVersion());
							j1.append("build", m.getBuild());
							j1.append("uri", "/f/d/" + f1.getId() + "/" + f1.getName());
							String md5 = Global.getString("module." + s + ".md5", null);
							if (X.isEmpty(md5)) {
								md5 = MD5.md5(f1.getInputStream());
								Global.setConfig("module." + s + ".md5", md5);
							}
							j1.append("md5", md5);

							r1.add(j1);
						}
					}
				} catch (Throwable e1) {
					log.error(e1.getMessage(), e1);
				}
			}

//			log.warn("modules, 3=" + t.past() + ", r1=" + r1);
//			t.reset();

			this.set("list", r1).send(200);

		} else {
			this.set(X.ERROR, "module.repo disabled!").send(201);
			GLog.applog.warn(this, "module", "module.repo disabled!");
		}

	}

}