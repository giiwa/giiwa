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
package org.giiwa.engine;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.DoubleStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Temp;
import org.giiwa.conf.Config;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.misc.Exporter;
import org.giiwa.misc.IOUtil;
import org.giiwa.misc.StringFinder;
import org.giiwa.net.mq.IStub;
import org.giiwa.net.mq.MQ;
import org.giiwa.net.mq.MQ.Request;
import org.giiwa.task.Task;
import org.giiwa.web.Controller;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

/**
 * R utility
 * 
 * @author joe
 *
 */
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

	public void close(String sid) {

		try {
			JSON j1 = JSON.create();
			j1.append("sid", sid);
			MQ.send(inst.name, Request.create().put(j1));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void _close(String sessionid) {

		RConnection conn = conns.get(sessionid);
		if (conn != null) {
			conn.close();
			conns.remove(sessionid);
		}
	}

	/**
	 * run R code and close the session
	 * 
	 * @param code the R code
	 * @return the result
	 * @throws Exception error
	 */
	public Object run(String code) throws Exception {
		String sid = UID.random();
		try {
			return run(sid, code);
		} finally {
			close(sid);
		}
	}

	/**
	 * run the R code and keep the session
	 * 
	 * @param sid  the session id
	 * @param code the code
	 * @return the result
	 * @throws Exception error
	 */
	@SuppressWarnings("rawtypes")
	public Object run(String sid, String code) throws Exception {
		return run(sid, code, null, (List) null, false);
	}

	/**
	 * run the R code with the data and close the session
	 * 
	 * @param code     the R code
	 * @param dataname the data name
	 * @param data     the data
	 * @return the result
	 * @throws Exception error
	 */
	@SuppressWarnings("rawtypes")
	public Object run(String code, String dataname, List data) throws Exception {
		String sid = UID.random();
		try {
			return run(sid, code, dataname, data);
		} finally {
			close(sid);
		}
	}

	/**
	 * run R code with data, and keep the session
	 * 
	 * @param sid      the session id
	 * @param code     the R code
	 * @param dataname the data name
	 * @param data     the data
	 * @return the result
	 * @throws Exception error
	 */
	@SuppressWarnings("rawtypes")
	public Object run(String sid, String code, String dataname, List data) throws Exception {
		return run(sid, code, dataname, data, false);
	}

	/**
	 * run the R code with the data, and close the session
	 * 
	 * @param code,     the R code
	 * @param dataname, the data name
	 * @param data,     the data
	 * @param head,     the head
	 * @return the result
	 * @throws Exception error
	 */
	@SuppressWarnings("rawtypes")
	public Object run(String code, String dataname, List data, boolean head) throws Exception {
		String sid = UID.random();
		try {
			return run(sid, code, dataname, data, head);
		} finally {
			close(sid);
		}
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
	public Object run(String sid, String code, String dataname, List data, boolean head) throws Exception {
		return run(sid, code, new Object[] { dataname, data, head ? 1 : 0 });
	}

	/**
	 * 
	 * @param sid
	 * @param code
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public Object run(String sid, String code, Object[]... data) throws Exception {

//		String host = Config.getConf().getString("r.host", X.EMPTY);
//
//		if (X.isIn(host, "127.0.0.1", X.EMPTY)) {
//			// local
//			return _run(code, dataname, data, head);
//
//		} else {

		Object r = null;
		if (inited) {
			// run in local
			r = _run(sid, code, Arrays.asList(data));

		} else {
			JSON j1 = JSON.create();
			j1.append("sid", sid);
			j1.append("code", code).append("data", Arrays.asList(data));
			r = MQ.call(inst.name, Request.create().put(j1), X.AMINUTE * 60);
		}

		if (r instanceof String) {
			StringFinder sf = StringFinder.create((String) r);
			String error = sf.get("=error=", "=end=");
			if (!X.isEmpty(error))
				throw new Exception(error);

		}
		return r;

//		}

	}

	@SuppressWarnings({ "unchecked" })
	private Object _run(String sid, String code, List<Object[]> data) throws Exception {

//		_check();

		RConnection c = _get(sid);

		if (c != null) {

//			String func = "f" + c.hashCode();

			StringBuilder sb = new StringBuilder();
			sb.append("tryCatch({\n");

			// save to file
			try {

				Temp temp = null;
				if (!X.isEmpty(data)) {
					for (Object[] d : data) {
						if (d == null || d[0] == null || d.length != 3)
							continue;

						temp = Temp.create("data");

						String s1 = _export(d[0].toString(), (List<Object>) d[1], X.toInt(d[2]) == 1 ? true : false,
								temp);
						if (!X.isEmpty(s1)) {
							sb.append(s1).append("\n");
						}
					}
				}

				Temp t = Temp.create("a.txt");
				File f = t.getFile();
				f.getParentFile().mkdirs();
//				sb.append(func + "<-function(){\n");
				sb.append("sink(file=\"" + f.getAbsolutePath() + "\");\n");
				sb.append(code).append("\nsink(file=NULL)\n");

//				+ func + "();");

//				System.out.println(sb);
//
				sb.append("}, error=function(e){cat('=error=');print(e);cat('=end=');})");

				if (log.isDebugEnabled())
					log.debug("R.run, code=\n" + sb.toString());

				c.eval(sb.toString());

				if (temp != null) {
					// TODO
//					temp.delete();
				}

				String r = IOUtil.read(f, "UTF8");

				if (log.isDebugEnabled())
					log.debug("R.run, result=\n" + r);

				t.delete();

				return r;

//				System.out.println(r2J2(x));

//				Object s1 = r2J(x);
//				return JSON.create().append("data", s1);
			} catch (RserveException re) {

				log.error(re.getRequestErrorDescription(re.getRequestReturnCode()), re);
				throw re;
			} finally {

//				c.eval("rm(" + func + ")");
				for (Object[] d : data) {
					if (d != null && d[0] != null)
						c.eval("rm(" + d[0] + ")");
				}
//				c.eval("rm(" + func + ", " + dataname + ")");

			}
		} else {
			log.error("R.run, c=null");
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

		Exporter<Object> ex = t.export(Exporter.FORMAT.plain);
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
		sb.append("stringsAsFactors=FALSE);");

		return sb.toString();

	}

	private static Map<String, RConnection> conns = new HashMap<String, RConnection>();

	synchronized RConnection _get(String sid) {

		RConnection conn = conns.get(sid);
		if (conn != null && conn.isConnected())
			return conn;

		try {
			String host = Config.getConf().getString("r.host", X.EMPTY);
			int port = Config.getConf().getInt("r.port", 6311);

			if (X.isEmpty(host)) {
				conn = new RConnection();
			} else {
				conn = new RConnection(host, port);
			}

			conns.put(sid, conn);

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return conn;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void main(String[] args) {

		Task.init(10);

		R.ROOT = "/Users/joe/d/temp/";

//		String s = "mean(b)";
		try {
			Map<String, List<Object[]>> p1 = new HashMap<String, List<Object[]>>();
			p1.put("b", Arrays.asList(X.split("10, 20, 30", "[, ]"), X.split("10, 20, 30", "[, ]"),
					X.split("10, 20, 30", "[, ]")));

			System.out.println(inst.run("summary(d);", "d", Arrays.asList(1, 2, 3, 100)));

			Object j1 = inst.run(
					"f509376766<-function(){x <- c(214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,90,93,90,106,214,214,214,214,214,214);fivenum(x)};f509376766();");
			System.out.println(j1);

			TimeStamp t = TimeStamp.create();

			Object[] ll = new Object[10];
			for (int i = 0; i < ll.length; i++) {
				ll[i] = (long) (ll.length * Math.random());
			}

			t.reset();
			Object r = inst.run("count(data)", ll);
			System.out.println(r + ", cost=" + t.past());

			Object r1 = inst.run("median(data)", ll);
			System.out.println(r1 + ", cost=" + t.past());

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
//			r = inst.run("mean(c1)", "c1", Arrays.asList(ll), false);

			System.out.println(r + ", cost=" + t.past());

			List<JSON> l1 = JSON.createList();
			l1.add(JSON.create().append("a", 1).append("b", 2).append("c", 3).append("d", 4));
			l1.add(JSON.create().append("a", 1).append("b", 32).append("c", 3).append("d", 4));
			l1.add(JSON.create().append("a", 10).append("b", 22).append("c", 39).append("d", 4));
			l1.add(JSON.create().append("a", 1).append("b", 21).append("c", 3).append("d", 4));
			l1.add(JSON.create().append("a", 1).append("b", 42).append("c", 3).append("d", 4));

			t.reset();
//			j1 = inst.run(
//					"library(C50);d16<-C5.0(x=mtcars[, 1:5], y=as.factor(mtcars[,6]));save(d16, file=\"d16\");summary(d16);",
//					null, null, false);
			System.out.println(j1 + ", cost=" + t.past());
//			System.out.println(j1.get("data"));

//			j1 = inst.run("ls()");
//			System.out.println("ls=" + j1.get("data"));

			t.reset();
			j1 = inst.run("load(file=\"d16\");summary(d16)");
			System.out.println("cost=" + t.past() + "//" + j1);

//			System.out.println(((List) j1.get("data")).get(0));

//			System.out.println(t1.toPrettyString());

			StringBuilder sb = new StringBuilder();
			sb.append("m1 <- read.csv('/Users/joe/d/temp/data',header=T,stringsAsFactors=TRUE);\n");
			sb.append("library(vegan)\n");
			sb.append("a <- vegdist(m1, method = 'bray')\n");
			sb.append("a <- anosim(a, m1$mpg, permutations = 999)\n");
			sb.append("print(a)");

			j1 = inst.run(sb.toString());
			System.out.println(j1);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Object run(String code, Object[] data) throws Exception {

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
			Object sd = run("sd(data)", data);
			Object mean = run("mean(data)", data);
			if (X.isEmpty(sd) || X.isEmpty(mean) || X.toDouble(mean) != 0)
				return null;

			return X.toDouble(sd) / X.toDouble(mean);
		}

		return null;

	}

	public Object mean(Object[] data) throws Exception {
		double d = Arrays.asList(data).parallelStream().mapToDouble(e -> {
			return X.toDouble(e);
		}).average().getAsDouble();
		return d;
	}

	public Object sum(Object[] data) throws Exception {
		double d = Arrays.asList(data).parallelStream().mapToDouble(e -> {
			return X.toDouble(e);
		}).sum();
		return d;
	}

	public Object count(Object[] data) throws Exception {
		return data.length;
	}

	public Object value_count(Object[] data) throws Exception {
		long d = Arrays.asList(data).parallelStream().distinct().count();
		return d;
	}

	public Object range(Object[] data) throws Exception {
		DoubleStream d = Arrays.asList(data).parallelStream().mapToDouble(e -> {
			return X.toDouble(e);
		});
		return d.max().getAsDouble() - d.min().getAsDouble();
	}

	public Object max(Object[] data) throws Exception {
		double d = Arrays.asList(data).parallelStream().mapToDouble(e -> {
			return X.toDouble(e);
		}).max().getAsDouble();
		return d;
	}

	public Object min(Object[] data) throws Exception {
		double d = Arrays.asList(data).parallelStream().mapToDouble(e -> {
			return X.toDouble(e);
		}).min().getAsDouble();
		return d;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void onRequest(long seq, Request req) {

		try {

			JSON j1 = req.get();

			String sid = j1.getString("sid");
			String code = j1.getString("code");
			if (X.isEmpty(code)) {
				_close(sid);
				return;
//				req.response(200);
			} else {
				List<Object[]> data = (List) j1.get("data");
				Object j2 = this._run(sid, code, data);
				req.response(j2);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			try {
				req.response(e.getMessage());
			} catch (Exception e1) {
				// ignore
			}
		}

	}

}
