package org.giiwa.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.json.JSON;

public class Counter {

	private static Log log = LogFactory.getLog(Counter.class);

	String name;
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
				log.warn("slow [" + name + "], cost=" + cost, new Exception());
			}
		}
		if (min == -1 || cost < min) {
			min = cost;
		}
		this.cost += cost;
		this.times++;
	}

	public synchronized JSON get() {
		JSON j = JSON.create();
		j.append("max", max <= 0 ? 0 : max);
		j.append("min", min <= 0 ? 0 : min);
		j.append("times", times);
		j.append("avg", times > 0 ? cost / times : 0);

		reset();

		return j;
	}

	public synchronized void reset() {
		cost = 0;
		times = 0;
		max = -1;
		min = -1;
		loged = 0;
	}

}
