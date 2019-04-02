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

import org.giiwa.core.base.Captcha;
import org.giiwa.core.bean.X;
import org.giiwa.core.json.JSON;
import org.giiwa.framework.bean.GLog;
import org.giiwa.framework.bean.Temp;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;

/**
 * web api: <a href='/captcha' target='_blank'>/captcha</a><br>
 * provides web api to get the captcha image and verify which linked with sid
 * (session key)
 * 
 * @author wujun
 *
 */
public class captcha extends Model {

	/**
	 * response the json with uri=[code.jpg]
	 */
	@Path()
	public void onGet() {

		JSON jo = new JSON();
		Temp t = Temp.create("code.jpg");
		try {

			Captcha.create(this.sid(), System.currentTimeMillis() + 5 * X.AMINUTE, 200, 60,
					t.getFile().getOutputStream(), 4);

			jo.put(X.STATE, 200);
			jo.put("sid", sid());
			jo.put("uri", t.getUri() + "?" + System.currentTimeMillis());
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
			GLog.securitylog.error(captcha.class, "", e1.getMessage(), e1, login, this.getRemoteHost());

			jo.put(X.STATE, 201);
			jo.put(X.MESSAGE, e1.getMessage());
		}

		this.response(jo);
	}

	/**
	 * verify the code
	 */
	@Path(path = "verify")
	public void verify() {
		String code = this.getString("code").toLowerCase();
		Captcha.Result r = Captcha.verify(this.sid(), code);

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

		this.response(jo);

	}

}
