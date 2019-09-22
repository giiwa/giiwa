package org.giiwa.core.dle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.base.Pool;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Config;
import org.giiwa.core.json.JSON;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.Temp;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.Rserve.RConnection;

public class R {

	static Log log = LogFactory.getLog(R.class);

	private static int TIMEOUT = 10000;

	public static R inst = new R();

	public JSON run(String code) throws Exception {
		return run(code, (List) null);
	}

//	public String run(String code, Object...vars ) throws Exception {
//		jdk.nashorn.api.scripting.ScriptObjectMirror m = (jdk.nashorn.api.scripting.ScriptObjectMirror) json;
//	}

	public JSON run(String code, String var, Object[] data) throws Exception {

		RConnection c = pool.get(TIMEOUT);

		if (c != null) {

			try {
				StringBuilder sb = new StringBuilder();
				String func = "f" + c.hashCode();
				sb.append(func + "<-function(){");
				sb.append(var + " <- c(" + X.join(Arrays.asList(data), ",") + ");");

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

				// remove the file
//				return x.asStrings();
			} finally {
				pool.release(c);
			}
		}
		throw new Exception("timeout wait=" + TIMEOUT);

	}

	public JSON run(String code, String[] cols, List<JSON> data) throws Exception {

		RConnection c = pool.get(TIMEOUT);

		if (c != null) {

			try {
				StringBuilder sb = new StringBuilder();

				String func = "f" + c.hashCode();

				sb.append(func + "<-function(){");

				for (String name : cols) {
					List<Object> l1 = X.toArray(data, e -> {
						return e.get(name);
					});
					sb.append(name + " <- c(" + X.join(l1, ",") + ");");
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
				// remove the file
//				return x.asString();
			} finally {
				pool.release(c);
			}
		}

		throw new Exception("timeout wait=" + TIMEOUT);

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public JSON run(String code, List<Object[]> data) throws Exception {

		RConnection c = pool.get(10000);

		if (c != null) {

			// save to file
			List<Temp> l1 = new ArrayList<Temp>();
			try {
				StringBuilder sb = new StringBuilder();
				String func = "f" + c.hashCode();
//				sb.append("setwd('/')\r\n");
				sb.append(func + "<-function(){");
				if (!X.isEmpty(data)) {
					Temp t = Temp.create(data + ".csv");
					Temp.Exporter<Object> ex = t.export("UTF-8", Temp.Exporter.FORMAT.csv).createSheet((s, e) -> {
						return (Object[]) e;
					});
					ex.print((List) data);
					ex.close();

					l1.add(t);

					sb.append("data <- read.csv('" + t.getFile().getCanonicalPath() + "', header=T);");

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
//				return x.asString();

			} finally {

				pool.release(c);

				if (l1 != null) {
					for (Temp t : l1)
						t.delete();
				}
			}
		}
		return null;

	}

	static Pool<RConnection> pool = Pool.create(Config.getConf().getInt("r.min", 1),
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

//			System.out.println(run(s, p1));

			JSON r = inst.run("mean(a)", "a", new Object[] { 1, 2, 3 });
			System.out.println(r);

			JSON j1 = inst.run(
					"f509376766<-function(){x <- c(214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,214,90,93,90,106,214,214,214,214,214,214);fivenum(x)};f509376766();");
			System.out.println(j1);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
