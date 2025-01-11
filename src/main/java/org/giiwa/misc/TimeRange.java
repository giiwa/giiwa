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
package org.giiwa.misc;

import org.giiwa.dao.X;
import org.giiwa.task.Function;
import org.giiwa.web.Language;

public class TimeRange {

	private Function<String, String> func;
	private long last = 0;
	private boolean _ok = false;

	public static TimeRange create(Function<String, String> timefunc) {
		TimeRange t = new TimeRange();
		t.func = timefunc;
		return t;
	}

	public boolean ok() {
		if (System.currentTimeMillis() > last) {
			if (System.currentTimeMillis() - last > X.AMINUTE) {
				String time = func.apply(null);
				String[] ss = X.split(time, "-");
				if (ss.length == 2) {
					int c = ss[0].compareTo(ss[1]);
					time = Language.getLanguage().format(System.currentTimeMillis(), "HH:mm");
					if (c > 0) {
						if (time.compareTo(ss[0]) >= 0 || time.compareTo(ss[1]) <= 0) {
							_ok = true;
						} else {
							_ok = false;
						}
					} else if (c < 0) {
						if (time.compareTo(ss[0]) >= 0 && time.compareTo(ss[1]) <= 0) {
							_ok = true;
						} else {
							_ok = false;
						}
					} else {
						_ok = false;
					}
				} else {
					_ok = true;
				}
				last = System.currentTimeMillis();
			}
			return _ok;

		}
		return _ok;
	}

}
