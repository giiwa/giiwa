package org.giiwa.app.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.giiwa.bean.Disk;
import org.giiwa.bean.GLog;
import org.giiwa.bean.Repo;
import org.giiwa.bean.Temp;
import org.giiwa.conf.Global;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.dfile.DFile;
import org.giiwa.json.JSON;
import org.giiwa.misc.GImage;
import org.giiwa.misc.IOUtil;
import org.giiwa.task.Task;
import org.giiwa.web.Controller;
import org.giiwa.web.Path;

public class f extends Controller {

	/**
	 * get file
	 * 
	 * @param id
	 * @param name
	 */
	@Path(path = "g/(.*)/(.*)")
	public void g(String id, String name) {

		DFile f1 = Disk.get(id);
		if (f1.isFile()) {

			String mime = Controller.getMimeType(f1.getName());
			log.debug("mime=" + mime);

			if (mime != null && mime.startsWith("image/")) {
				try {
					String size = this.getString("size");
					if (!X.isEmpty(size)) {
						String[] ss = size.split("x");

						if (ss.length == 2) {
							File f = Temp.get(id, "s_" + size);
							boolean failed = false;

							if (!f.exists()) {

								f.getParentFile().mkdirs();

								GImage.scale1(f1.getInputStream(), new FileOutputStream(f), X.toInt(ss[0]),
										X.toInt(ss[1]));

							} else {
								if (log.isDebugEnabled())
									log.debug("load the image from the temp cache, file=" + f.getCanonicalPath());
							}

							if (f.exists() && !failed) {
								if (log.isDebugEnabled())
									log.debug("load the scaled image from " + f.getCanonicalPath());

								this.setContentType(Controller.getMimeType("a.png"));

								_send(new FileInputStream(f), f.length());

								return;
							}
						}
					}
				} catch (Exception e1) {
					log.error(e1.getMessage(), e1);
				}
			}
			this.setContentType(mime);

			try {
				_send(f1.getInputStream(), f1.length());

			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}

		}

	}

	/**
	 * download file
	 * 
	 * @param id
	 * @param name
	 */
	@Path(path = "d/(.*)/(.*)")
	public void d(String id, String name) {

		DFile f1 = Disk.get(id);
		if (f1.isFile()) {

			this.send(f1.getName(), f1.getInputStream(), f1.length());
//			String name1 = Url.encode(f1.getName());
//			this.addHeader("Content-Disposition", "attachment; filename*=UTF-8''" + name1);
//			this.setContentType(Controller.getMimeType(f1.getName()));
//
//			try {
//				IOUtil.copy(f1.getInputStream(), this.getOutputStream());
//			} catch (Exception e) {
//				log.error(e.getMessage(), e);
//			}

		}

	}

	/**
	 * upload file
	 */
	@Path(path = "upload", login = true)
	public void upload() {

		JSON jo = new JSON();

		// String access = Module.home.get("upload.require.access");

		if (Task.powerstate == 0) {
			this.send(JSON.create().append(X.STATE, HttpServletResponse.SC_BAD_REQUEST)
					.append(X.ERROR, HttpServletResponse.SC_BAD_REQUEST)
					.append(X.MESSAGE, lang.get("upload.node.state_0")));
			return;
		}

		FileItem file = this.file("file");
		if (file != null) {
			String filename = this.getString("filename");
			if (X.isEmpty(filename)) {
				filename = file.getName();
			}
			store(file, filename, jo);
		} else {
			jo.append(X.STATE, HttpServletResponse.SC_BAD_REQUEST).append(X.ERROR, HttpServletResponse.SC_BAD_REQUEST)
					.append(X.MESSAGE, lang.get("upload.notfound"));
		}

		// /**
		// * test
		// */
		// jo.put("error", "error");
		this.send(jo);

	}

	private boolean store(FileItem file, String filename, JSON jo) {
//		String tag = this.getString("tag");

		try {
			String range = this.head("Content-Range");
			if (range == null) {
				range = this.getString("Content-Range");
			}
			long position = 0;
			long total = 0;
			String lastModified = this.head("lastModified");
			if (X.isEmpty(lastModified)) {
				lastModified = this.getString("lastModified");
			}
			if (X.isEmpty(lastModified)) {
				lastModified = this.getString("lastModifiedDate");
			}

			if (range != null) {

				// bytes 0-9999/22775650
				String[] ss = range.split(" ");
				if (ss.length > 1) {
					range = ss[1];
				}
				ss = range.split("-|/");
				if (ss.length == 3) {
					position = X.toLong(ss[0]);
					total = X.toLong(ss[2]);
				}

				// log.debug(range + ", " + position + "/" + total);
			}

			String id = UID.id(login.getId(), filename, total, lastModified);

			if (log.isDebugEnabled())
				log.debug("storing, id=" + id + ", name=" + filename + ", total=" + total + ", last=" + lastModified);

			long pos = Repo.append(id, filename, position, total, file.getInputStream(), login.getId(), this.ip());
			if (pos >= 0) {
				if (jo == null) {
					this.put("url", "/f/repo/" + id + "/" + filename);
					this.put(X.ERROR, 0);
					this.put("repo", id);
					if (total > 0) {
						this.put("name", filename);
						this.put("pos", pos);
						this.put("size", total);
					}
				} else {
					jo.put("url", "/f/repo/" + id + "/" + filename);
					jo.put("repo", id);
					jo.put(X.ERROR, 0);
					if (total > 0) {
						jo.put("name", filename);
						jo.put("pos", pos);
						jo.put("size", total);
					}
					jo.put(X.STATE, 200);
					jo.put("site", Global.getString("site.url", X.EMPTY));
				}

				// Session.load(sid()).set("access.repo." + id, 1).store();
			} else {
				if (jo == null) {
					this.put(X.ERROR, HttpServletResponse.SC_BAD_REQUEST);
					this.put(X.MESSAGE, lang.get("repo.locked"));
					this.put(X.STATE, HttpServletResponse.SC_BAD_REQUEST);
				} else {
					jo.put(X.ERROR, HttpServletResponse.SC_BAD_REQUEST);
					jo.put(X.MESSAGE, lang.get("repo.locked"));
					jo.put(X.STATE, HttpServletResponse.SC_BAD_REQUEST);
				}
				return false;
			}
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			GLog.oplog.error(f.class, "upload", e.getMessage(), e, login, this.ip());

			if (jo == null) {
				this.put(X.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				this.put(X.MESSAGE, lang.get(e.getMessage()));
				this.put(X.STATE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			} else {
				jo.put(X.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				jo.put(X.MESSAGE, lang.get(e.getMessage()));
				jo.put(X.STATE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		}

		return false;
	}

	/**
	 * get temp file
	 */
	@Path(path = "temp/(.*)/(.*)", login = true)
	public void temp(String id, String name) {

//		if (log.isDebugEnabled())
//			log.debug("temp: " + this.path);

		if (this.path == null) {
			this.notfound();
			return;
		}

//		String[] ss = X.split(Url.decode(this.path), "[/]");
//		if (ss.length != 2) {
//			this.notfound();
//			return;
//		}

		try {

//			String name = ss[1];
			File f1 = Temp.get(id, name);
			if (!f1.exists()) {
				this.notfound();
				return;
			} else {
				this.send(name, new FileInputStream(f1), f1.length());
			}

		} catch (Exception e) {
			log.error(path, e);
			GLog.oplog.error(f.class, "temp", e.getMessage(), e, login, this.ip());
		}

		this.notfound();
	}

	@Path(path = "alive")
	public void alive() {
		JSON jo = new JSON();
		jo.put(X.STATE, 200);
		jo.put("uptime", System.currentTimeMillis() - Controller.UPTIME);

		this.send(jo);
	}

	@Path(path = "echo")
	public void echo() {
		StringBuilder sb = new StringBuilder();
		for (NameValue s : this.getHeaders()) {
			sb.append(s.name).append("=").append(s.value).append("<br>");
		}

		this.print(sb.toString());
	}

	void _send(InputStream in, long total) {

		try {
			String range = this.head("range");

			long start = 0;
			long end = total;
			if (!X.isEmpty(range)) {
				String[] ss = range.split("(=|-)");
				if (ss.length > 1) {
					start = X.toLong(ss[1]);
				}

				if (ss.length > 2) {
					end = Math.min(total, X.toLong(ss[2]));
				}
			}

			if (end <= start) {
				end = start + 1024 * 1024;
			}

			if (end > total) {
				end = total;
			}

			long length = end - start;

			if (end < total) {
				this.status(206);
			}

			if (start == 0) {
				this.head("Accept-Ranges", "bytes");
			}
			this.head("Content-Length", Long.toString(length));
			this.head("Content-Range", "bytes " + start + "-" + (end - 1) + "/" + total);

			log.info("response.stream, bytes " + start + "-" + (end - 1) + "/" + total);
			if (length > 0) {
				OutputStream out = this.getOutputStream();

				IOUtil.copy(in, out, start, end, false);
				out.flush();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			X.close(in);
		}
	}
}