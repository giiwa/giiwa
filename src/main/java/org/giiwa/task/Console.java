package org.giiwa.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.dao.X;
import org.giiwa.web.Language;

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

	public int timeout(int secs) {
		marks.push(timeout);
		int s = X.toInt(timeout / 1000);
		timeout = secs * 1000;
		return s;
	}

	public void reset() {
		if (!marks.isEmpty()) {
			timeout = marks.pop();
		}
	}

	public void timeout(int secs, Consumer<Long> func) {
		timeout = secs * 1000;
		timeoutfunc = func;
		checker.schedule(timeout);
		System.out.println("timeout checking in [" + timeout + "]ms");
	}

	public void log(Object message) {

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
			log.debug("threadname=" + threadname + ", message=" + message);
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

	public void exit(int status) {

		log.warn("restart by console.", new Exception("restart"));

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
