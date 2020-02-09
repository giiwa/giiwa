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
import org.giiwa.core.base.Pool;
import org.giiwa.core.bean.TimeStamp;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Config;
import org.giiwa.core.json.JSON;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.Temp;
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

	private static int TIMEOUT = 10000;

	public static R inst = new R("r");
	private static boolean inited = false;

	public static void serve() {

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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public JSON run(String code) throws Exception {
		return run(code, null, (List) null, null);
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
	public JSON run(String code, String dataname, List<JSON> data, List<String> header) throws Exception {

		String host = Config.getConf().getString("r.host", X.EMPTY);

		if (X.isIn(host, "127.0.0.1", X.EMPTY)) {
			// local
			return _run(code, dataname, data, header);

		} else {

			JSON j1 = JSON.create();
			j1.append("c", code).append("dn", dataname).append("d", data).append("h", header);
			return MQ.call(inst.name, Request.create().put(j1), X.AMINUTE * 60);
		}

	}

	private JSON _run(String code, String dataname, List<JSON> data, List<String> header) throws Exception {

		RConnection c = pool.get(TIMEOUT);

		if (c != null) {

			String func = "f" + c.hashCode();

			// save to file
			List<Temp> l1 = new ArrayList<Temp>();
			try {
				StringBuilder sb = new StringBuilder();
				sb.append(func + "<-function(){");
				if (!X.isEmpty(data)) {

					if (dataname != null && data != null) {

						Temp t = Temp.create("data");

						Exporter<JSON> ex = t.export("UTF-8", Exporter.FORMAT.plain);
						ex.createSheet(e -> {
							Object[] o = new Object[header.size()];
							for (int i = 0; i < header.size(); i++) {
								o[i] = e.get(header.get(i));
							}
							return o;
						});
						ex.print(header);
						ex.print(data);
						ex.close();

						l1.add(t);
						sb.append(dataname + " <- read.csv('" + t.getFile().getCanonicalPath()
								+ "', header=T, stringsAsFactors=TRUE);");
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

				Object s1 = r2JSON(x);
				return JSON.create().append("data", s1);//.append("summary", s1);

			} finally {

				c.eval("rm(" + func + ")");

				pool.release(c);

				if (l1 != null) {
					for (Temp t : l1)
						t.delete();
				}
			}
		}
		return null;

	}

	/**
	 * run the R code in global and reside the mode as "name"
	 * 
	 * @param name
	 * @param code
	 * @param data
	 * @param header
	 * @return
	 * @throws Exception
	 */
	public JSON bind(String name, String code, String dataname, List<JSON> data, List<String> header) throws Exception {

		String host = Config.getConf().getString("r.host", X.EMPTY);

		if (X.isIn(host, "127.0.0.1", X.EMPTY)) {
			// local
			return _bind(name, code, dataname, data, header);

		} else {

			JSON j1 = JSON.create();
			j1.append("m", "bind").append("c", code).append("n", name).append("dn", dataname).append("d", data)
					.append("h", header);
			return MQ.call(inst.name, Request.create().put(j1), X.AMINUTE * 60);
		}
	}

	private JSON _bind(String name, String code, String dataname, List<JSON> data, List<String> header)
			throws Exception {

		RConnection c = pool.get(TIMEOUT);

		if (c != null) {

			// save to file
			List<Temp> l1 = new ArrayList<Temp>();
			try {
				StringBuilder sb = new StringBuilder();
				sb.append(name + "<-function(){");
				if (!X.isEmpty(data)) {

					Temp t = Temp.create("data");

					Exporter<JSON> ex = t.export("UTF-8", Exporter.FORMAT.plain);
					ex.createSheet(e -> {
						Object[] o = new Object[header.size()];
						for (int i = 0; i < header.size(); i++) {
							o[i] = e.get(header.get(i));
						}
						return o;
					});
					ex.print(header);
					ex.print(data);
					ex.close();

					l1.add(t);
					sb.append(dataname + " <- read.csv('" + t.getFile().getCanonicalPath()
							+ "', header=T, stringsAsFactors=TRUE);");

				}

				sb.append(code).append("};" + name + "();");

//				System.out.println(sb);

				if (log.isDebugEnabled())
					log.debug("R.run, code=" + sb);

				REXP x = c.eval(sb.toString());

				if (x == null || x.isNull()) {
					return JSON.create();
				}

				Object s1 = r2JSON(x);
				return JSON.create().append("data", s1).append("summary", s1);

			} finally {

				c.eval("rm(" + name + ")");

				pool.release(c);

				if (l1 != null) {
					for (Temp t : l1)
						t.delete();
				}
			}
		}
		return null;

	}

	private Object r2JSON(REXP x) throws REXPMismatchException {

		if (x instanceof REXPDouble) {
			REXPDouble d = (REXPDouble) x;
			return d.asDouble();
		}

		if (x instanceof REXPInteger) {
			REXPInteger d = (REXPInteger) x;
			return d.asInteger();
		}

		if (x instanceof REXPLogical) {
			REXPLogical d = (REXPLogical) x;
			return d.asString();
		}

		if (x instanceof REXPRaw) {
			REXPRaw d = (REXPRaw) x;
			return d.asString();
		}

		if (x instanceof REXPString) {
			REXPString d = (REXPString) x;
			return d.asString();
		}

		if (x instanceof REXPSymbol) {
			REXPSymbol d = (REXPSymbol) x;
			return d.asString();
		}

		if (x instanceof REXPGenericVector) {
			REXPGenericVector x1 = (REXPGenericVector) x;

			RList r1 = x1.asList();
			List<Object> l2 = new ArrayList<Object>();
			for (int i = 0; i < r1.size(); i++) {
				Object o = r1.get(i);
//				System.out.println("o=" + o);
				if (o instanceof REXP) {
					l2.add(r2JSON((REXP) o));
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
					l2.add(r2JSON((REXP) o));
				}
			}
			return l2;
		}

		String[] ss = x.asStrings();
		if (ss == null || ss.length == 0) {
			return JSON.create();
		} else if (ss.length == 1) {
			return ss[0];
		} else {
			return ss;
		}

	}

	private static Pool<RConnection> pool = Pool.create(Config.getConf().getInt("r.min", 1),
			Config.getConf().getInt("r.max", 2), new Pool.IPoolFactory<RConnection>() {

				@Override
				public RConnection create() {
					RConnection c = null;
					try {

						String host = Config.getConf().getString("r.host", X.EMPTY);
						int port = Config.getConf().getInt("r.port", 6311);

						if (X.isEmpty(host)) {
							c = new RConnection();
						} else {
							c = new RConnection(host, port);
						}

					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
					return c;
				}

				@Override
				public boolean cleanup(RConnection t) {
					return t.isConnected();
				}

				@Override
				public void destroy(RConnection t) {
					t.close();
				}

			});

	public static void main(String[] args) {

		Task.init(10);

//		String s = "mean(b)";
		try {
			Map<String, List<Object[]>> p1 = new HashMap<String, List<Object[]>>();
			p1.put("b", Arrays.asList(X.split("10, 20, 30", "[, ]"), X.split("10, 20, 30", "[, ]"),
					X.split("10, 20, 30", "[, ]")));

			System.out.println(inst.run("d<-c(1,2,3,4);mean(d);"));

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

			t.reset();
			r = inst.run("mean(data)", ll);
			System.out.println(r + ", cost=" + t.past());

			List<JSON> l1 = JSON.createList();
			l1.add(JSON.create().append("a", 1).append("b", 2).append("c", 3).append("d", 4));
			l1.add(JSON.create().append("a", 1).append("b", 32).append("c", 3).append("d", 4));
			l1.add(JSON.create().append("a", 10).append("b", 22).append("c", 39).append("d", 4));
			l1.add(JSON.create().append("a", 1).append("b", 21).append("c", 3).append("d", 4));
			l1.add(JSON.create().append("a", 1).append("b", 42).append("c", 3).append("d", 4));
			j1 = inst.run("library(C50);d1<-C5.0(x=mtcars[, 1:5], y=as.factor(mtcars[,6]));print(summary(d1))", null,
					l1, Arrays.asList("a", "b", "c", "d"));
			System.out.println(j1);
			System.out.println(j1.get("summary"));

//			System.out.println(((List) j1.get("data")).get(0));

//			System.out.println(t1.toPrettyString());

		} catch (Exception e) {
			// TODO Auto-generated catch block
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

		RConnection c = pool.get(TIMEOUT);

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

				pool.release(c);

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

	@SuppressWarnings("unchecked")
	@Override
	public void onRequest(long seq, Request req) {

		try {

			JSON j1 = req.get();
			String method = j1.getString("m");

			if (X.isSame(method, "run")) {

				JSON j2 = this._run(j1.getString("c"), j1.getString("dn"), j1.getList("d"), (List<String>) j1.get("h"));
				req.response(j2);

			} else if (X.isSame(method, "bind")) {

				JSON j2 = this._bind(j1.getString("n"), j1.getString("c"), j1.getString("dn"), j1.getList("d"),
						(List<String>) j1.get("h"));
				req.response(j2);

			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}

}
