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
package org.giiwa.dao;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.dao.Helper.V;

public final class Counter implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private static Log log = LogFactory.getLog(Counter.class);

	public String name;
	long max = 0;
	long cost = -1;
	long times = 0;
	long loged = 0;

	public Counter(String name) {
		this.name = name;
	}

	public synchronized void add(long cost, String format, Object... args) {
		if (cost > max) {
			max = cost;
			if (cost > 1000 && loged == 0) {
				// log,
				loged = 1;

//				if (log.isInfoEnabled()) {
//					String memo = X.isEmpty(format) ? X.EMPTY : String.format(format, args);
//					log.info("slow [" + name + "], cost=" + cost, new Exception(memo));
//				}
			}
		}
		this.cost += cost;
		this.times++;

	}

	public synchronized Stat get() {

		Stat e = new Stat();

		e.name = name;
		e.max = max <= 0 ? 0 : max;
		e.times = times;
		e.avg = times > 0 ? cost / times : 0;

		reset();

		return e;
	}

	public synchronized void reset() {
		cost = 0;
		times = 0;
		max = -1;
		loged = 0;
	}

	public static class Stat implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public String name;
		public long max;
		public long times;
		public long avg;

		public V toV() {

			V v = V.create();

			v.append(X.NAME, name);
			v.append("max", max);
			v.append("times", times);
			v.append("avg", avg);

			return v;

		}

		public synchronized void merge(Stat s) {

			if (times + s.times > 0) {
				if (max < s.max) {
					max = s.max;
				}
				avg = (times * avg + s.times * s.avg) / (times + s.times);
				times = times + s.times;
			}

		}

	}

}
