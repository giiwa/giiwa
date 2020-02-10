package org.giiwa.app.web;

import org.giiwa.core.base.IOUtil;
import org.giiwa.core.dfile.DFile;
import org.giiwa.framework.bean.Disk;
import org.giiwa.framework.web.Controller;
import org.giiwa.framework.web.Path;

public class file extends Controller {

	@Path(path = "get/(.*)/(.*)")
	public void get(String id, String name) {

		DFile f1 = Disk.get(id);
		if (f1.isFile()) {
			this.setContentType(Controller.getMimeType(f1.getName()));
			try {
				IOUtil.copy(f1.getInputStream(), this.getOutputStream());
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

	}

}
