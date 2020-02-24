package org.giiwa.app.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.giiwa.bean.Disk;
import org.giiwa.bean.Temp;
import org.giiwa.dao.X;
import org.giiwa.dfile.DFile;
import org.giiwa.misc.GImage;
import org.giiwa.misc.IOUtil;
import org.giiwa.misc.Url;
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

								File src = Temp.get(id, null);
								if (src.exists()) {
									src.delete();
								}
								src.getParentFile().mkdirs();

								OutputStream out = new FileOutputStream(src);
								IOUtil.copy(f1.getInputStream(), out, false);
								out.close();

								/**
								 * using scale3 to cut the middle of the image
								 */
								if (GImage.scale3(src.getAbsolutePath(), f.getAbsolutePath(), X.toInt(ss[0]),
										X.toInt(ss[1])) < 0) {
									failed = true;
									log.warn("scale3 image failed");
								}
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
				IOUtil.copy(f1.getInputStream(), this.getOutputStream());
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}

		}

	}

	@Path(path = "d/(.*)/(.*)")
	public void d(String id, String name) {

		DFile f1 = Disk.get(id);
		if (f1.isFile()) {

			String name1 = Url.encode(f1.getName());
			this.addHeader("Content-Disposition", "attachment; filename*=UTF-8''" + name1);
			this.setContentType(Controller.getMimeType(f1.getName()));

			try {
				IOUtil.copy(f1.getInputStream(), this.getOutputStream());
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}

		}

	}

}
