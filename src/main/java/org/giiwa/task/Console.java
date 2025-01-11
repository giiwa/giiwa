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
package org.giiwa.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.GLog;
import org.giiwa.dao.Comment;
import org.giiwa.dao.X;
import org.giiwa.web.Language;

@Comment()
public class Console {

	private static Map<String, List<Function<String, Boolean>>> listener = new HashMap<String, List<Function<String, Boolean>>>();

	public static Console inst = new Console();

	public static boolean _DEBUG = false;

	private static Log log = LogFactory.getLog(Console.class);

	private static Language lang = Language.getLanguage("zh_cn");

	private static long last = -1;
	private static long timeout = -1;
	private static Consumer<Long> timeoutfunc;

	Stack<Long> marks = new Stack<Long>();

	@Comment()
	public int timeout(@Comment(text = "seconds") int secs) {
		marks.push(timeout);
		int s = X.toInt(timeout / 1000);
		timeout = secs * 1000;
		return s;
	}

	@Comment()
	public void reset() {
		if (!marks.isEmpty()) {
			timeout = marks.pop();
		}
	}

	@Comment()
	public void timeout(@Comment(text = "seconds") int secs, @Comment(text = "timeoutfunc") Consumer<Long> func) {
		timeout = secs * 1000;
		timeoutfunc = func;
		checker.schedule(timeout);
		System.out.println("timeout checking in [" + timeout + "]ms");
	}

	@Comment(text = "记录日志")
	public void log(@Comment(text = "message") Object message) {

		last = System.currentTimeMillis();

		if (_DEBUG) {
			String threadname = Thread.currentThread().getName();
			String message1 = "[" + lang.format(System.currentTimeMillis(), "HH:mm:ss.SSS") + "] - "
					+ X.toString(message) + " [" + threadname + "]";
			System.out.println(message1);
			return;
		}

		String threadname = Thread.currentThread().getName();

		if (log.isDebugEnabled()) {
			log.debug("[DEBUG], threadname=" + threadname + ", message=" + message);
		}

		if (listener.isEmpty()) {
			return;
		}

//
//		Task.schedule(() -> {

		List<Function<String, Boolean>> l1 = _get(threadname);
		if (l1.isEmpty()) {
			return;
		}

		String classname = X.toLine(new Exception(), 2);

		String message1 = "<span style='color: #d6d111;'>[" + lang.format(System.currentTimeMillis(), "HH:mm:ss.SSS")
				+ "]</span> - " + X.toString(message) + " <span style='color: #91b5e2;'>[" + classname + "]</span>";

		if (log.isDebugEnabled()) {
			log.debug(message1);
		}

		for (Function<String, Boolean> f1 : l1) {
			if (!f1.apply(message1)) {
				_remove(f1);
			}
		}

//		});

	}

	private void _remove(Function<String, Boolean> f1) {

		for (String name : listener.keySet().toArray(new String[listener.size()])) {
			List<Function<String, Boolean>> l1 = listener.get(name);
			if (l1.contains(f1)) {
				l1.remove(f1);
				if (l1.isEmpty()) {
					listener.remove(name);
				}
			}
		}

	}

	private List<Function<String, Boolean>> _get(String threadname) {

		List<Function<String, Boolean>> l1 = new ArrayList<Function<String, Boolean>>();

		for (String name : listener.keySet()) {
			if (threadname.startsWith(name)) {
				l1.addAll(listener.get(name));
			}
		}

		return l1;
	}

	public static void open(String[] threadname, Function<String, Boolean> func) {

		for (String name : threadname) {
			List<Function<String, Boolean>> l1 = listener.get(name);
			if (l1 == null) {
				l1 = new ArrayList<Function<String, Boolean>>();
				listener.put(name, l1);
			}
			l1.add(func);
		}

	}

	@Comment(hide = true)
	public void exit(int status) {

		log.warn("restart by console.", new Exception("restart"));
		GLog.applog.warn("sys", "restart", "restart by console. thread=" + Thread.currentThread().getName());

		Task.schedule(t -> {
			System.exit(status);
		}, 1000);
	}

	private static Task checker = new Task() {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void onExecute() {
			if (timeout > 0 && (System.currentTimeMillis() - last > timeout)) {

				if (timeoutfunc != null) {
					timeoutfunc.accept((System.currentTimeMillis() - last) / 1000);
				}
			}
		}

		@Override
		public String getName() {
			return "console.checker";
		}

		@Override
		public void onFinish() {
			this.schedule(10000);
		}

	};

}
