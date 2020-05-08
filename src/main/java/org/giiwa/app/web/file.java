package org.giiwa.app.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.giiwa.bean.Disk;
import org.giiwa.bean.Temp;
import org.giiwa.dao.X;
import org.giiwa.dfile.DFile;
import org.giiwa.misc.GImage;
import org.giiwa.misc.IOUtil;
import org.giiwa.misc.Url;
import org.giiwa.task.Task;
import org.giiwa.web.Controller;
import org.giiwa.web.Path;

/**
 * @deprecated
 * @author joe
 *
 */
public class file extends Controller {

	@Path(path = "get/(.*)/(.*)")
	public void get(String id, String name) {

		try {
			DFile f1 = Disk.get(id);
			if (f1.isFile()) {

				String mime = Controller.getMimeType(f1.getName());
				log.debug("mime=" + mime);

				if (mime != null && mime.startsWith("image/")) {
					String size = this.getString("size");
					if (!X.isEmpty(size)) {
						String[] ss = size.split("x");

						if (ss.length == 2) {
							File f = Temp.get(id, "s_" + size);

							log.debug("file=" + f.getAbsolutePath());

							boolean failed = false;

							if (!f.exists() || f.length() == 0) {

								f.getParentFile().mkdirs();

								Temp t2 = Temp.create("a.jpg");
								File f2 = t2.getFile();
								f2.getParentFile().mkdirs();
								long len = IOUtil.copy(f1.getInputStream(), new FileOutputStream(f2));
								if (len != f1.length())
									throw new IOException("get dfile error");

								GImage.scale3(new FileInputStream(f2), new FileOutputStream(f), X.toInt(ss[0]),
										X.toInt(ss[1]));

							} else {
								if (log.isDebugEnabled())
									log.debug("load the image from the temp cache, file=" + f.getCanonicalPath());
							}

							if (f.exists() && !failed) {
								if (log.isDebugEnabled())
									log.debug("load the scaled image from " + f.getCanonicalPath());

								this.setContentType(Controller.getMimeType("a.png"));

								InputStream in = new FileInputStream(f);
								OutputStream out = this.getOutputStream();

								IOUtil.copy(in, out, false);
								in.close();

								if (!f1.getFilename().startsWith("/file/get/")) {
									Task.schedule(() -> {
										// save the file to nginx
										try {

											DFile f2 = Disk.seek("/file/get/" + id + "/" + name, Disk.TYPE_NGINX);
											if (f2 != null) {
												f2.upload(f);
											}

										} catch (Exception e) {
											log.error(e.getMessage(), e);
										}
									});
								}

								return;
							}
						}
					}
				}
				this.setContentType(mime);

				IOUtil.copy(f1.getInputStream(), this.getOutputStream());

				if (!f1.getFilename().startsWith("/file/get/")) {
					Task.schedule(() -> {
						// save the file to nginx
						try {

							DFile f2 = Disk.seek("/file/get/" + id + "/" + name, Disk.TYPE_NGINX);
							if (f2 != null) {
								f2.upload(f1.getInputStream());
							}

						} catch (Exception e) {
							log.error(e.getMessage(), e);
						}
					});
				}

			}
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
		}

	}

	@Path(path = "d/(.*)/(.*)")
	public void d(String id, String name) {

		try {
			DFile f1 = Disk.get(id);
			if (f1.isFile()) {

				String name1 = Url.encode(f1.getName());
				this.head("Content-Disposition", "attachment; filename*=UTF-8''" + name1);
				this.setContentType(Controller.getMimeType(f1.getName()));

				IOUtil.copy(f1.getInputStream(), this.getOutputStream());

			}
		} catch (Exception e) {
			this.error(e);
		}

	}

}
