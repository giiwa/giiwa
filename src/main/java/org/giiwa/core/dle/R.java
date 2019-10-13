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
import org.giiwa.core.base.Pool;
import org.giiwa.core.bean.TimeStamp;
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public JSON run(String code) throws Exception {
		return run(code, (List) null);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public JSON run(String code, List<Object[]> data) throws Exception {

		RConnection c = pool.get(TIMEOUT);

		if (c != null) {

			// save to file
			List<Temp> l1 = new ArrayList<Temp>();
			try {
				StringBuilder sb = new StringBuilder();
				String func = "f" + c.hashCode();
				sb.append(func + "<-function(){");
				if (!X.isEmpty(data)) {
					Temp t = Temp.create("data");
					Temp.Exporter<Object> ex = t.export("UTF-8", Temp.Exporter.FORMAT.csv).createSheet((e) -> {
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

}
