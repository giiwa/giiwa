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
package org.giiwa.core.dle;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.DoubleStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.base.Exporter;
import org.giiwa.core.bean.TimeStamp;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Config;
import org.giiwa.core.json.JSON;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.Temp;
import org.giiwa.framework.web.Controller;
import org.giiwa.mq.IStub;
import org.giiwa.mq.MQ;
import org.giiwa.mq.MQ.Request;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPGenericVector;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPList;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPNull;
import org.rosuda.REngine.REXPRaw;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.REXPSymbol;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RConnection;

public class R extends IStub {

	public R(String name) {
		super(name);
	}

	static Log log = LogFactory.getLog(R.class);

	public static R inst = new R("r");
	private static String ROOT;
	private static boolean inited = false;

	public static void serve() {

		ROOT = Controller.GIIWA_HOME + "/temp/_R/";
		new File(ROOT).mkdirs();

		String host = Config.getConf().getString("r.host", X.EMPTY);

		if (X.isIn(host, "127.0.0.1", X.EMPTY) && !inited) {
			// local
			try {
				inst.bind();
				inited = true;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				Task.schedule(() -> {
					serve();
				}, 3000);
			}
		}

	}

	@SuppressWarnings("rawtypes")
	public JSON run(String code) throws Exception {
		return run(code, null, (List) null, false);
	}

	@SuppressWarnings("rawtypes")
	public JSON run(String code, String dataname, List data) throws Exception {
		return run(code, dataname, data, false);
	}

	/**
	 * run the R code in sanbox
	 * 
	 * @param code
	 * @param name
	 * @param data
	 * @param header
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	public JSON run(String code, String dataname, List data, boolean head) throws Exception {

		String host = Config.getConf().getString("r.host", X.EMPTY);

		if (X.isIn(host, "127.0.0.1", X.EMPTY)) {
			// local
			return _run(code, dataname, data, head);

		} else {

			JSON j1 = JSON.create();
			j1.append("c", code).append("dn", dataname).append("d", data).append("h", head ? 1 : 0);
			return MQ.call(inst.name, Request.create().put(j1), X.AMINUTE * 60);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private JSON _run(String code, String dataname, List data, boolean head) throws Exception {

		_check();

		RConnection c = conn;

		if (c != null) {

			String func = "f" + c.hashCode();

			// save to file
			List<Temp> l1 = new ArrayList<Temp>();
			try {
				StringBuilder sb = new StringBuilder();
				sb.append(func + "<-function(){");

				if (!X.isEmpty(data)) {
					Temp t = Temp.create("data");
					l1.add(t);

					String s1 = _export(dataname, (List<Object>) data, head, t);
					if (!X.isEmpty(s1)) {
						sb.append(s1);
					}
				}

				sb.append(code).append("};" + func + "();");

//				System.out.println(sb);

				if (log.isDebugEnabled())
					log.debug("R.run, code=" + sb);

				REXP x = c.eval(sb.toString());

				if (x == null || x.isNull()) {
					return JSON.create();
				}

				Object s1 = r2J(x);
				return JSON.create().append("data", s1);

			} finally {

				c.eval("rm(" + func + ")");

				if (l1 != null) {
					for (Temp t : l1)
						t.delete();
				}
			}
		}
		return null;

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private String _export(String dataname, List data, boolean head, Temp t) throws Exception {

		if (dataname == null || data == null || data.isEmpty()) {
			return X.EMPTY;
		}

		StringBuilder sb = new StringBuilder();

		Object o1 = data.get(0);

		Object[] hh = (o1 instanceof Map) ? (((Map) o1).keySet().toArray()) : null;

		Exporter<Object> ex = t.export("UTF-8", Exporter.FORMAT.plain);
		if (head && hh != null) {
			ex.print(hh);
		}

		ex.createSheet(e -> {
			if (e == null)
				return null;
			if (e.getClass().isArray())
				return (Object[]) e;

			if (e instanceof List)
				return ((List) e).toArray();

			if (e instanceof Map) {
				Map m = (Map) e;
				Object[] o = new Object[hh.length];
				for (int i = 0; i < hh.length; i++) {
					o[i] = m.get(hh[i]);
				}

				return o;
			}
			return new Object[] { e };
		});

		ex.print((List<Object>) data);
		ex.close();

		sb.append(dataname + " <- read.csv('" + t.getFile().getCanonicalPath() + "',");
		if (head) {
			sb.append("header=T,");
		} else {
			sb.append("header=F,");
		}
		sb.append("stringsAsFactors=TRUE);");

		return sb.toString();

	}

	private Object r2J(REXP x) throws REXPMismatchException {

		if (x instanceof REXPDouble) {
			double[] d1 = x.asDoubles();

			if (d1.length == 1) {
				if (Double.isNaN(d1[0]))
					return x.asString();
				return d1[0];
			}

			return X.asList(d1, e -> e);
		}

		if (x instanceof REXPInteger) {
			int[] ii = x.asIntegers();
			if (ii.length == 1)
				return ii[0];

			return X.asList(ii, e -> e);
		}

		if (x instanceof REXPLogical || x instanceof REXPRaw || x instanceof REXPString || x instanceof REXPSymbol) {
			String[] ss = x.asStrings();
			if (ss == null || ss.length == 0) {
				return null;
			} else if (ss.length == 1) {
				return ss[0];
			} else {
				return Arrays.asList(ss);
			}
		}

		if (x instanceof REXPGenericVector) {
			REXPGenericVector x1 = (REXPGenericVector) x;

			RList r1 = x1.asList();
			List<Object> l2 = new ArrayList<Object>();
			for (int i = 0; i < r1.size(); i++) {
				Object o = r1.get(i);
				if (o instanceof REXP) {
					l2.add(r2J((REXP) o));
				}
			}
			return l2;
		}

		if (x instanceof REXPList) {
			REXPList x1 = (REXPList) x;

			RList r1 = x1.asList();
			List<Object> l2 = new ArrayList<Object>();
			for (int i = 0; i < r1.size(); i++) {
				Object o = r1.get(i);
//				System.out.println("o=" + o);
				if (o instanceof REXP) {
					l2.add(r2J((REXP) o));
				}
			}
			return l2;
		}

		if (x instanceof REXPNull) {
			return null;
		}

		String[] ss = x.asStrings();
		if (ss == null || ss.length == 0) {
			return null;
		} else if (ss.length == 1) {
			return ss[0];
		} else {
			return Arrays.asList(ss);
		}

	}

	private static RConnection conn = null;

//	private static Pool<RConnection> pool = null;

	synchronized void _check() {

		if (conn != null)
			return;

		try {
			String host = Config.getConf().getString("r.host", X.EMPTY);
			int port = Config.getConf().getInt("r.port", 6311);

			if (X.isEmpty(host)) {
				conn = new RConnection();
			} else {
				conn = new RConnection(host, port);
			}
			return;

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {

		Task.init(10);

		R.ROOT = "/Users/joe/d/temp/";

//		String s = "mean(b)";
		try {
			Map<String, List<Object[]>> p1 = new HashMap<String, List<Object[]>>();
			p1.put("b", Arrays.asList(X.split("10, 20, 30", "[, ]"), X.split("10, 20, 30", "[, ]"),
					X.split("10, 20, 30", "[, ]")));

			System.out.println(inst.run("summary(d);", "d", Arrays.asList(1, 2, 3, 100)));

			JSON j1 = inst.run(
					"f509376766<-function(){x <- c(214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,90,93,90,106,214,214,214,214,214,214);fivenum(x)};f509376766();");
			System.out.println(j1);

			TimeStamp t = TimeStamp.create();

			Object[] ll = new Object[10];
			for (int i = 0; i < ll.length; i++) {
				ll[i] = (long) (ll.length * Math.random());
			}

			t.reset();
			JSON r = inst.run("count(data)", ll);
			System.out.println(r + ", cost=" + t.past());

			r = inst.run("median(data)", ll);
			System.out.println(r + ", cost=" + t.past());

			for (int i = 0; i < ll.length; i++) {
				ll[i] = i + 1;
			}
			t.reset();
			double d1 = Arrays.asList(ll).stream().mapToInt(e -> {
				return X.toInt(e);
			}).average().getAsDouble();
			System.out.println(d1 + ", cost=" + t.past());

//			j1 = inst.run("ls()");
//			System.out.println(j1.get("data"));

			t.reset();
			List l2 = new ArrayList<Object>();
			l2.add(ll);
			r = inst.run("mean(c1)", "c1", Arrays.asList(ll), false);

			System.out.println(r + ", cost=" + t.past());

			List<JSON> l1 = JSON.createList();
			l1.add(JSON.create().append("a", 1).append("b", 2).append("c", 3).append("d", 4));
			l1.add(JSON.create().append("a", 1).append("b", 32).append("c", 3).append("d", 4));
			l1.add(JSON.create().append("a", 10).append("b", 22).append("c", 39).append("d", 4));
			l1.add(JSON.create().append("a", 1).append("b", 21).append("c", 3).append("d", 4));
			l1.add(JSON.create().append("a", 1).append("b", 42).append("c", 3).append("d", 4));

			t.reset();
			j1 = inst.run(
					"library(C50);d16<-C5.0(x=mtcars[, 1:5], y=as.factor(mtcars[,6]));save(d16, file=\"d16\");summary(d16);",
					null, null, false);
			System.out.println(j1 + ", cost=" + t.past());
//			System.out.println(j1.get("data"));

//			j1 = inst.run("ls()");
//			System.out.println("ls=" + j1.get("data"));

			t.reset();
			j1 = inst.run("load(file=\"d16\");summary(d16)");
			System.out.println("cost=" + t.past() + "//" + j1.get("data"));

//			System.out.println(((List) j1.get("data")).get(0));

//			System.out.println(t1.toPrettyString());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public JSON run(String code, Object[] data) throws Exception {

		if (X.isIn(code, "mean(data)", "avg(data)", "mean", "avg")) {
			return mean(data);
		} else if (X.isIn(code, "sum(data)", "sum")) {
			return sum(data);
		} else if (X.isIn(code, "max(data)", "max")) {
			return max(data);
		} else if (X.isIn(code, "min(data)", "min")) {
			return min(data);
		} else if (X.isIn(code, "count(data)", "count", "length(data)", "length")) {
			return count(data);
		} else if (X.isIn(code, "value_count(data)", "value_count")) {
			return value_count(data);
		} else if (X.isIn(code, "cv", "cv(data)")) {
			JSON sd = run("sd(data)", data);
			JSON mean = run("mean(data)", data);
			if (X.isEmpty(sd) || X.isEmpty(mean) || mean.getDouble("data") != 0)
				return null;

			return JSON.create().append("data", sd.getDouble("data") / mean.getDouble("data"));
		}

		_check();

		RConnection c = conn;

		if (c != null) {

			// save to file
			Temp t = Temp.create("data");
			try {
				StringBuilder sb = new StringBuilder();
				String func = "f" + c.hashCode();
				sb.append(func + "<-function(){");
				if (!X.isEmpty(data)) {
					File f = t.getFile();
					f.getParentFile().mkdirs();
					FileWriter f1 = new FileWriter(f);
					for (Object o : data) {
						f1.write(o + " ");
					}
					f1.close();
					sb.append("data <- scan('" + f.getAbsolutePath() + "');");
				}

				sb.append(code).append("};" + func + "();");

				if (log.isDebugEnabled())
					log.debug("R.run, code=" + sb);

				REXP x = c.eval(sb.toString());

				String[] ss = x.asStrings();
				if (ss == null || ss.length == 0) {
					return JSON.create();
				} else if (ss.length == 1) {
					return JSON.create().append("data", ss[0]);
				} else {
					return JSON.create().append("data", ss);
				}

			} finally {

				t.delete();
			}
		}

		return null;

	}

	public JSON mean(Object[] data) throws Exception {
		double d = Arrays.asList(data).parallelStream().mapToDouble(e -> {
			return X.toDouble(e);
		}).average().getAsDouble();
		return JSON.create().append("data", d);
	}

	public JSON sum(Object[] data) throws Exception {
		double d = Arrays.asList(data).parallelStream().mapToDouble(e -> {
			return X.toDouble(e);
		}).sum();
		return JSON.create().append("data", d);
	}

	public JSON count(Object[] data) throws Exception {
		return JSON.create().append("data", data.length);
	}

	public JSON value_count(Object[] data) throws Exception {
		long d = Arrays.asList(data).parallelStream().distinct().count();
		return JSON.create().append("data", d);
	}

	public JSON range(Object[] data) throws Exception {
		DoubleStream d = Arrays.asList(data).parallelStream().mapToDouble(e -> {
			return X.toDouble(e);
		});
		return JSON.create().append("data", d.max().getAsDouble() - d.min().getAsDouble());
	}

	public JSON max(Object[] data) throws Exception {
		double d = Arrays.asList(data).parallelStream().mapToDouble(e -> {
			return X.toDouble(e);
		}).max().getAsDouble();
		return JSON.create().append("data", d);
	}

	public JSON min(Object[] data) throws Exception {
		double d = Arrays.asList(data).parallelStream().mapToDouble(e -> {
			return X.toDouble(e);
		}).min().getAsDouble();
		return JSON.create().append("data", d);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void onRequest(long seq, Request req) {

		try {

			JSON j1 = req.get();

			JSON j2 = this._run(j1.getString("c"), j1.getString("dn"), (List) j1.get("d"), j1.getInt("h") == 1);
			req.response(j2);

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}

}
