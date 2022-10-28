package org.giiwa.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.dao.Helper.V;

public final class Counter {

	private static Log log = LogFactory.getLog(Counter.class);

	public String name;
	long max = 0;
	long min = -1;
	long cost = -1;
	long times = 0;
	long loged = 0;

	public Counter(String name) {
		this.name = name;
	}

	public synchronized void add(long cost) {
		if (cost > max) {
			max = cost;
			if (cost > 1000 && loged == 0) {
				// log,
				loged = 1;

				if (log.isInfoEnabled())
					log.info("slow [" + name + "], cost=" + cost, new Exception());
			}
		}
		if (min == -1 || cost < min) {
			min = cost;
		}
		this.cost += cost;
		this.times++;
	}

	public synchronized Stat get() {

		Stat e = new Stat();

		e.name = name;
		e.max = max <= 0 ? 0 : max;
		e.min = min <= 0 ? 0 : min;
		e.times = times;
		e.avg = times > 0 ? cost / times : 0;

		reset();

		return e;
	}

	public synchronized void reset() {
		cost = 0;
		times = 0;
		max = -1;
		min = -1;
		loged = 0;
	}

	public static class Stat {

		public String name;
		public long max;
		public long min;
		public long times;
		public long avg;

		public V toV() {

			V v = V.create();

			v.append("name", name);
			v.append("max", max);
			v.append("min", min);
			v.append("times", times);
			v.append("avg", avg);

			return v;

		}

	}

}
