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
