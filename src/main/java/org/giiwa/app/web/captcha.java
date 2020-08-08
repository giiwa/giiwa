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
package org.giiwa.app.web;

import java.io.FileOutputStream;

import org.giiwa.bean.Disk;
import org.giiwa.bean.GLog;
import org.giiwa.bean.Temp;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.dfile.DFile;
import org.giiwa.json.JSON;
import org.giiwa.misc.Captcha;
import org.giiwa.web.Controller;
import org.giiwa.web.Path;

/**
 * web api: <a href='/captcha' target='_blank'>/captcha</a><br>
 * provides web api to get the captcha image and verify which linked with sid
 * (session key)
 * 
 * @author wujun
 *
 */
public class captcha extends Controller {

	/**
	 * response the json with uri=[code.jpg]
	 */
	@Path()
	public void onGet() {

		JSON jo = new JSON();
		Temp t = Temp.create("code.jpg");
		try {

			t.getFile().getParentFile().mkdirs();

			Captcha.create(this.sid(true), System.currentTimeMillis() + 5 * X.AMINUTE, 200, 60,
					new FileOutputStream(t.getFile()), 4);

			String filename = "/temp/" + lang.format(System.currentTimeMillis(), "yyyy/MM/dd/HH/mm/")
					+ System.currentTimeMillis() + "_" + UID.random(10) + ".jpg";
			DFile f1 = Disk.seek(filename);
			t.save(f1);

			jo.put(X.STATE, 200);
			jo.put("sid", sid(false));
			jo.put("uri", "/f/g/" + f1.getId() + "/code.jpg?" + System.currentTimeMillis());

		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
			GLog.securitylog.error(captcha.class, "", e1.getMessage(), e1, login, this.ip());

			jo.put(X.STATE, 201);
			jo.put(X.MESSAGE, e1.getMessage());
		}

		this.send(jo);
	}

	/**
	 * verify the code
	 */
	@Path(path = "verify")
	public void verify() {
		String code = this.getString("code").toLowerCase();
		Captcha.Result r = Captcha.verify(this.sid(false), code);

		JSON jo = new JSON();
		if (Captcha.Result.badcode == r) {
			jo.put(X.STATE, 202);
			jo.put(X.MESSAGE, "bad code");
		} else if (Captcha.Result.expired == r) {
			jo.put(X.STATE, 201);
			jo.put(X.MESSAGE, "expired");
		} else {
			jo.put(X.STATE, 200);
			jo.put(X.MESSAGE, "ok");
		}

		this.send(jo);

	}

}
