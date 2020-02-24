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
package org.giiwa.app.task;

import java.util.List;

import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.dao.bean.Node;
import org.giiwa.dao.bean.Stat;
import org.giiwa.dao.bean.Stat.SIZE;
import org.giiwa.task.StatTask;

/**
 * The Class NodeLoadStatTask.
 */
public class NodeLoadStatTask extends StatTask {

	/**
	 * The Constant serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;

	public static void init() {
		inst = new NodeLoadStatTask();
		inst.schedule((long) (X.AMINUTE * Math.random()), true);
	}

	@Override
	protected SIZE[] getSizes() {
		return new Stat.SIZE[] { Stat.SIZE.min };
	}

	@Override
	public void onFinish() {
		this.schedule(X.AMINUTE, true);
	}

	@Override
	public String getName() {
		return "gi.load";
	}

	@Override
	protected void onStat(SIZE size, long start, long end, Object cat) {
		if (cat == null)
			return;

		String id = cat.toString();
		Node e = Node.dao.load(id);
		long[] ff = new long[15];

		if (e.getUpdated() < System.currentTimeMillis() - Node.LOST) {
			ff[0] = -1;
			ff[1] = -1;
			ff[2] = -1;
			ff[3] = -1;
			ff[4] = -1;
			ff[5] = -1; // dfile.times
			ff[6] = -1; // dfile.max
			ff[7] = -1; // dfile.min
			ff[8] = -1; // dfile.avg
			ff[9] = -1; // dfile.times_c
			ff[10] = -1; // dfile.max_c
			ff[11] = -1; // dfile.min_c
			ff[12] = -1; // dfile.avg_c
			ff[13] = -1; // tcp_established
			ff[14] = -1; // tcp_closewait

		} else {
			ff[0] = e.getUsage();
			ff[1] = e.getLong("globaltasks");
			ff[2] = e.getLong("localthreads");
			ff[3] = e.getLong("localrunning");
			ff[4] = e.getLong("localpending");
			ff[5] = e.getLong("dfiletimes");
			ff[6] = e.getLong("dfilemaxcost");
			ff[7] = e.getLong("dfilemincost");
			ff[8] = e.getLong("dfileavgcost");
			ff[9] = e.getLong("dfiletimes_c");
			ff[10] = e.getLong("dfilemaxcost_c");
			ff[11] = e.getLong("dfilemincost_c");
			ff[12] = e.getLong("dfileavgcost_c");
			ff[13] = e.getLong("tcp_established");
			ff[14] = e.getLong("tcp_closewait");
		}

		Stat.snapshot(start, "node.load", size, W.create().and("dataid", id), V.create().append("dataid", id), ff);

	}

	@Override
	protected List<?> getCategories() {
		return Node.dao.distinct("id", W.create());
	}

}
