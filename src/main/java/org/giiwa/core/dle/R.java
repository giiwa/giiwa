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
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.Temp;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.Rserve.RConnection;

public class R {

	static Log log = LogFactory.getLog(R.class);

	public static Object run(String code) throws Exception {
		return run(code, null);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static String run(String code, Map<String, List<Object[]>> params) throws Exception {

		RConnection c = pool.get(10000);

		if (c != null) {

			StringBuilder sb = new StringBuilder();
//			sb.append("setwd('/')\r\n");

			// save to file
			List<Temp> l1 = new ArrayList<Temp>();
			try {
				if (!X.isEmpty(params)) {
					for (String name : params.keySet()) {
						Temp t = Temp.create(name + ".csv");
						Temp.Exporter<Object> ex = t.export("UTF-8", Temp.Exporter.FORMAT.csv).createSheet((s, e) -> {
							return (Object[]) e;
						});
						List l2 = params.get(name);
						ex.print(l2);
						ex.close();

						l1.add(t);

						sb.append(name + " <- read.csv('" + t.getFile().getCanonicalPath() + "', header=T)\r\n");

					}
				}

				sb.append(code);
				if (log.isDebugEnabled())
					log.debug("R.run, code=" + sb);

//				System.out.println(sb);

				REXP x = c.eval(sb.toString());

				// remove the file
//				System.out.println(x);
				return x.asString();

			} finally {
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
				public void cleanup(RConnection t) {

				}

				@Override
				public void destroy(RConnection t) {
					t.close();
				}

			});

	public static void main(String[] args) {

		Task.init(10);

		String s = "mean(b)";
		try {
			Map<String, List<Object[]>> p1 = new HashMap<String, List<Object[]>>();
			p1.put("b", Arrays.asList(X.split("10, 20, 30", "[, ]"), X.split("10, 20, 30", "[, ]"),
					X.split("10, 20, 30", "[, ]")));

			System.out.println(run("mean(1,2,4)", null));

//			System.out.println(run(s, p1));

			Object r = calculate("2*10");
			System.out.println(r);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param f the string such as: 10*20
	 * @return the Object
	 * @throws Exception the Exception
	 */
	public static String calculate(String f) throws Exception {
		return run(f, null);
	}
}
