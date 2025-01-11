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

import java.util.HashMap;
import java.util.Map;

import org.giiwa.conf.Global;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.misc.Base32;
import org.giiwa.misc.RSA;
import org.giiwa.web.Module;

public class Key {

	private static Map<String, String> _code = new HashMap<String, String>();

	public synchronized static String get(String name, int len) {

		String s = _code.get(name);
		if (X.isEmpty(s)) {
			s = Global.getString("key." + name, null);
			if (X.isEmpty(s)) {
				s = UID.random(len);
				byte[] bb = RSA.encode(s.getBytes(), "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCNom9BdQZ4z6YyiqijBpR3LsG9Q2Pqd3KyMX/zzBMrDbe5gEKiocHB2R86pH6TiU6LXxK4BRF7RtYrtiw5scgNs2xjBJi7pTQzKqHF04jkyjtwbCnc5edkUFcez3awHVX0ntBphVd07CwLJVgKHUdEb4UClluqGv3ocXe6Of5c/QIDAQAB");

				s = Base32.encode(bb);
				Global.setConfig("key." + name, s);
			}
			String pri = Module.load("default").getKey();
			byte[] bb = Base32.decode(s);
			
			
			s = new String(RSA.decode(bb, pri));
			_code.put(name, s);
		}
		return s;

	}

}
