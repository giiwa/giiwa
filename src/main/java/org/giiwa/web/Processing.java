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
package org.giiwa.web;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.json.JSON;

public class Processing {

	private static Log log = LogFactory.getLog(Processing.class);

	public static void remove(Controller mo) {
		synchronized (_cache) {
			_cache.remove(mo.id);
		}
	}

	private static HashMap<Integer, Controller> _cache = new HashMap<Integer, Controller>();

	public static void add(Controller mo) {
		mo.thread = Thread.currentThread();
		synchronized (_cache) {
			_cache.put(mo.id, mo);
		}
	}

	public static List<JSON> getAll() {

		List<JSON> l1 = JSON.createList();

		Integer[] ii = null;

		synchronized (_cache) {
			ii = _cache.keySet().toArray(new Integer[_cache.size()]);
		}

		if (ii != null && ii.length > 0) {
			for (Integer i : ii) {
				Controller mo = _cache.get(i);
				if (mo != null) {
					JSON j1 = JSON.create();
					j1.append("uri", mo.uri);
					j1.append("browser", mo.browser());
					j1.append("ip", mo.ipPath());
					j1.append("id", mo.id);
					j1.append("mo", mo.getClass().getName());
					j1.append("cost", System.currentTimeMillis() - mo.created);
					j1.append("thread", mo.thread.getName());

					l1.add(j1);
				}
			}
		}

		Collections.sort(l1, new Comparator<JSON>() {

			@Override
			public int compare(JSON o1, JSON o2) {
				long cost1 = o1.getLong("cost");
				long cost2 = o2.getLong("cost");
				if (cost1 > cost2) {
					return -1;
				} else if (cost1 < cost2) {
					return 1;
				}
				return 0;
			}

		});

		return l1;

	}

	@SuppressWarnings("deprecation")
	public static void kill(Integer id) {
		Controller mo = _cache.get(id);
		if (mo != null && mo.thread != null) {

//			mo.thread.interrupt();
			mo.thread.stop();

		} else {
			log.warn("kill [" + id + "] failed. mo=" + mo + ", mo.thread=" + (mo == null ? null : mo.thread.getName()));
		}
	}

	public static Controller get(Integer id) {
		return _cache.get(id);
	}

//	private static Task checker = new Task() {
//
//		/**
//		 * 
//		 */
//		private static final long serialVersionUID = 1L;
//
//		@Override
//		public String getName() {
//			return "mo.checker";
//		}
//
//		@Override
//		public void onFinish() {
//			this.schedule(X.AMINUTE);
//		}
//
//		@Override
//		public void onExecute() {
//			if (_cache.isEmpty()) {
//				return;
//			}
//
//			Integer[] ii = null;
//
//			synchronized (_cache) {
//				ii = _cache.keySet().toArray(new Integer[_cache.size()]);
//			}
//			if (ii != null) {
//				for (Integer i : ii) {
//					Controller mo = _cache.get(i);
//					if(mo != null) {
////						kill(i);
//					}
//				}
//			}
//		}
//
//	};

}
