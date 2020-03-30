package org.giiwa.dfile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.Helper.V;
import org.giiwa.json.JSON;
import org.giiwa.misc.Pool;
import org.giiwa.net.nio.Client;
import org.giiwa.net.nio.IoRequest;
import org.giiwa.net.nio.IoResponse;
import org.giiwa.web.GiiwaServlet;

public class FileClient {

	private static Log log = LogFactory.getLog(FileClient.class);

	private static final long TIMEOUT = 10000;

	/**
	 * the number of call times
	 */
	public static AtomicLong times = new AtomicLong(0);

	/**
	 * the total cost of calling
	 */
	public static AtomicLong costs = new AtomicLong(0);

	/**
	 * the max cost
	 */
	public static long maxcost = Long.MIN_VALUE;

	/**
	 * the min cost
	 */
	public static long mincost = Long.MAX_VALUE;

	private Map<Long, IoRequest[]> pending = new HashMap<Long, IoRequest[]>();

	private AtomicLong seq = new AtomicLong(0);

	private Pool<Client> client;

	private static Map<String, FileClient> cached = new HashMap<String, FileClient>();

	public static FileClient get(String url) throws IOException {

		FileClient c = cached.get(url);
		if (c != null) {
			return c;
		}

		c = create(url);
		cached.put(url, c);
		return c;

	}

	private static FileClient create(String url) throws IOException {

		if (log.isInfoEnabled())
			log.info("create new client, url=" + url);

		FileClient c = new FileClient();
//		c.url = url;

		c.client = Pool.create(1, 10, new Pool.IPoolFactory<Client>() {

			@Override
			public Client create() {
				try {
					return Client.create().error(e -> {
						c.close();
					}).connect(url, resp -> {

						IoRequest r = FileServer.born(resp);
						while (r != null) {
							long seq = r.readLong();

							Object[] aa = c.pending.get(seq);
							if (aa != null) {
								synchronized (aa) {
									aa[0] = r;
									aa.notify();
								}
							} else {
								r.release();
							}

							r = FileServer.born(resp);
						}
					});
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
				return null;
			}

			@Override
			public boolean cleanup(Client t) {
				if (t.isClosed()) {
					return false;
				}
				return true;
			}

			@Override
			public void destroy(Client t) {
				t.close();
			}

		});

		return c;
	}

	public boolean delete(String path, String filename, long age) {
		if (client == null)
			return false;

		Client c = client.get(3000);

//		IoResponse r = IoResponse.create(seq.incrementAndGet(), IoRequest.SMALL);
		TimeStamp t = TimeStamp.create();
		times.incrementAndGet();

		long s = seq.incrementAndGet();

		try {

			IoResponse r = c.createResponse();

			r.write(ICommand.CMD_DELETE);
			byte[] b = path.getBytes();
			r.write(b.length);
			r.write(b);
			b = filename.getBytes();
			r.write(b.length);
			r.write(b);
			r.write(age);

			IoRequest[] aa = new IoRequest[1];
			pending.put(s, aa);
			synchronized (aa) {
				_send(s, r);
				if (aa[0] == null) {
					aa.wait(TIMEOUT);
				}
			}

			if (aa[0] != null) {
				IoRequest a = aa[0];
				try {
					return a.readByte() == 1 ? true : false;
				} finally {
					a.release();
				}
			} else {
				throw new IOException("Timeout");
			}

		} catch (Exception e) {
			close();
			log.error(e.getMessage(), e);
		} finally {

			client.release(c);

			pending.remove(s);

			costs.addAndGet(t.pastms());
			if (maxcost < t.pastms()) {
				maxcost = t.pastms();
			}
			if (mincost > t.pastms()) {
				mincost = t.pastms();
			}

		}
		return false;

	}

	private void close() {

		synchronized (cached) {

			String[] ss = cached.keySet().toArray(new String[cached.size()]);

			for (String name : ss) {
				FileClient c1 = cached.get(name);
				if (c1 == this) {
					cached.remove(name);

					log.debug("close the client, client=" + c1.client);

					c1.client.destroy();
					c1.client = null;
//					Client c2 = c1.client;
//					if (c2 != null) {
//						c1.client = null;
//						c2.close();
//					}
				}
			}

		}
	}

	/**
	 * Get the bytes from the filename
	 * 
	 * @param path     the path
	 * @param filename the filename
	 * @param offset   the offset
	 * @param len      the length
	 * @return the bytes, or null{@code null} if the client not ready
	 */
	public byte[] get(String path, String filename, long offset, int len) {

		if (client == null)
			return null;

		Client c = client.get(3000);
		TimeStamp t = TimeStamp.create();
		times.incrementAndGet();

		long s = seq.incrementAndGet();

		try {

			IoResponse r = c.createResponse();

			r.write(ICommand.CMD_GET);
			byte[] b = path.getBytes();
			r.write(b.length);
			r.write(b);
			b = filename.getBytes();
			r.write(b.length);
			r.write(b);
			r.write(offset);
			r.write(len);

			IoRequest[] aa = new IoRequest[1];
			pending.put(s, aa);
			synchronized (aa) {
				_send(s, r);
				if (aa[0] == null) {
					aa.wait(TIMEOUT);
				}
			}

			if (aa[0] != null) {
				IoRequest a = aa[0];
				try {
					return a.readBytes(a.readInt());
				} finally {
					a.release();
				}
			} else {
				throw new IOException("Timeout");
			}

		} catch (Exception e) {
			close();
			log.error(e.getMessage(), e);
		} finally {

			client.release(c);

			pending.remove(s);

			costs.addAndGet(t.pastms());
			if (maxcost < t.pastms()) {
				maxcost = t.pastms();
			}
			if (mincost > t.pastms()) {
				mincost = t.pastms();
			}

			log.debug("get, cost=" + t.past() + " filename=" + filename);

		}
		return null;
	}

	public long put(String path, String filename, long offset, byte[] bb, int len) throws IOException {

		if (client == null)
			throw new IOException("client not inited");

		TimeStamp t = TimeStamp.create();
		times.incrementAndGet();
		long s = seq.incrementAndGet();

		Client c = client.get(3000);

		try {

			IoResponse r = c.createResponse();
			r.write(ICommand.CMD_PUT);
			byte[] b = path.getBytes();
			r.write(b.length);
			r.write(b);
			b = filename.getBytes();
			r.write(b.length);
			r.write(b);
			r.write(offset);
			r.write(len);
			r.write(bb, 0, len);

			IoRequest[] aa = new IoRequest[1];
			pending.put(s, aa);
			synchronized (aa) {
				_send(s, r);
				if (aa[0] == null) {
					aa.wait(TIMEOUT);
				}
			}

			if (aa[0] != null) {
				IoRequest a = aa[0];
				try {
					return a.readLong();
				} finally {
					a.release();
				}
			} else {
				throw new IOException("Timeout");
			}

		} catch (Exception e) {
			close();
			throw new IOException(e);
		} finally {

			client.release(c);
			pending.remove(s);

			costs.addAndGet(t.pastms());
			if (maxcost < t.pastms()) {
				maxcost = t.pastms();
			}
			if (mincost > t.pastms()) {
				mincost = t.pastms();
			}

			log.debug("put, cost=" + t.past() + ", filename=" + filename);

		}
	}

	public boolean mkdirs(String path, String filename) {
		if (client == null)
			return false;

		TimeStamp t = TimeStamp.create();
		times.incrementAndGet();
		long s = seq.incrementAndGet();

		Client c = client.get(3000);
		try {

			IoResponse r = c.createResponse();

			r.write(ICommand.CMD_MKDIRS);
			byte[] b = path.getBytes();
			r.write(b.length);
			r.write(b);
			b = filename.getBytes();
			r.write(b.length);
			r.write(b);

			IoRequest[] aa = new IoRequest[1];
			pending.put(s, aa);
			synchronized (aa) {
				_send(s, r);
				if (aa[0] == null) {
					aa.wait(TIMEOUT);
				}
			}

			if (aa[0] != null) {
				IoRequest a = aa[0];
				try {
					return a.readByte() == 1 ? true : false;
				} finally {
					a.release();
				}
			} else {
				throw new IOException("Timeout");
			}
		} catch (Exception e) {
			close();
			log.error(e.getMessage(), e);
		} finally {

			client.release(c);
			pending.remove(s);

			costs.addAndGet(t.pastms());
			if (maxcost < t.pastms()) {
				maxcost = t.pastms();
			}
			if (mincost > t.pastms()) {
				mincost = t.pastms();
			}

		}
		return false;

	}

	public List<FileInfo> list(String path, String filename) {

		if (client == null)
			return null;

		TimeStamp t = TimeStamp.create();
		times.incrementAndGet();
		long s = seq.incrementAndGet();

		Client c = client.get(3000);

		try {

			IoResponse r = c.createResponse();

			r.write(ICommand.CMD_LIST);
			byte[] b = path.getBytes();
			r.write(b.length);
			r.write(b);
			b = filename.getBytes();
			r.write(b.length);
			r.write(b);

			IoRequest[] aa = new IoRequest[1];
			pending.put(s, aa);
			synchronized (aa) {
				_send(s, r);
				if (aa[0] == null) {
					aa.wait(TIMEOUT);
				}
			}

			if (aa[0] != null) {
				IoRequest a = aa[0];

				List<FileInfo> l1 = new ArrayList<FileInfo>();

				while (a.size() > 0) {
					// JSON j1 = JSON.create();
					FileInfo info = new FileInfo();
					info.name = new String(a.readBytes(a.readInt()));
					info.exists = a.readInt() == 1;
					info.isfile = a.readInt() == 1;
					info.length = a.readLong();
					info.lastmodified = a.readLong();
//					info.creation = a.readLong();
					l1.add(info);
				}

				a.release();

				return l1;
			} else {
				throw new IOException("Timeout");
			}

		} catch (Exception e) {
			close();
			log.error(e.getMessage(), e);
		} finally {

			client.release(c);
			pending.remove(s);

			costs.addAndGet(t.pastms());
			if (maxcost < t.pastms()) {
				maxcost = t.pastms();
			}
			if (mincost > t.pastms()) {
				mincost = t.pastms();
			}

		}
		return null;

	}

	public FileInfo info(String path, String filename) {

		if (client == null)
			return null;

		TimeStamp t = TimeStamp.create();
		times.incrementAndGet();
		long s = seq.incrementAndGet();

		Client c = client.get(3000);

		try {

			IoResponse r = c.createResponse();

			r.write(ICommand.CMD_INFO);
			byte[] b = path.getBytes();
			r.write(b.length);
			r.write(b);
			b = filename.getBytes();
			r.write(b.length);
			r.write(b);

			IoRequest[] aa = new IoRequest[1];
			pending.put(s, aa);
			synchronized (aa) {
				_send(s, r);
				if (aa[0] == null) {
					aa.wait(TIMEOUT);
				}
			}

			if (aa[0] != null) {
				IoRequest a = aa[0];

				FileInfo info = new FileInfo();
				info.exists = a.readInt() == 1;
				info.isfile = a.readInt() == 1;
				info.length = a.readLong();
				info.lastmodified = a.readLong();
//				info.creation = a.readLong();

				a.release();

				return info;
			} else {
				throw new IOException("Timeout");
			}

		} catch (Exception e) {
			close();
			log.error(e.getMessage(), e);
		} finally {

			client.release(c);
			pending.remove(s);

			costs.addAndGet(t.pastms());
			if (maxcost < t.pastms()) {
				maxcost = t.pastms();
			}
			if (mincost > t.pastms()) {
				mincost = t.pastms();
			}

		}
		return null;
	}

	public void close(String name) {
		FileClient c = cached.remove(name);
		if (c != null && c.client != null) {
			c.client.destroy();
			c.client = null;
		}
	}

	public boolean move(String path, String filename, String path2, String filename2) {

		if (client == null)
			return false;

		TimeStamp t = TimeStamp.create();
		times.incrementAndGet();
		long s = seq.incrementAndGet();

		Client c = client.get(3000);

		try {

			IoResponse r = c.createResponse();

			r.write(ICommand.CMD_MOVE);
			for (String s1 : new String[] { path, filename, path2, filename2 }) {
				byte[] b = s1.getBytes();
				r.write(b.length);
				r.write(b);
			}

			IoRequest[] aa = new IoRequest[1];
			pending.put(s, aa);
			synchronized (aa) {
				_send(s, r);
				if (aa[0] == null) {
					aa.wait(TIMEOUT);
				}
			}

			if (aa[0] != null) {
				IoRequest a = aa[0];
				try {
					return a.readByte() == 1 ? true : false;
				} finally {
					a.release();
				}
			} else {
				throw new IOException("Timeout");
			}
		} catch (Exception e) {
			close();
			log.error(e.getMessage(), e);
		} finally {

			client.release(c);
			pending.remove(s);

			costs.addAndGet(t.pastms());
			if (maxcost < t.pastms()) {
				maxcost = t.pastms();
			}
			if (mincost > t.pastms()) {
				mincost = t.pastms();
			}

		}
		return false;
	}

	public void http(String uri, HttpServletRequest req, HttpServletResponse resp, String method, String node) {

		if (client == null)
			return;

		TimeStamp t = TimeStamp.create();
		times.incrementAndGet();
		long s = seq.incrementAndGet();

		Client c = client.get(3000);

		try {

			IoResponse r = c.createResponse();

			r.write(ICommand.CMD_HTTP);

			JSON head = JSON.create();
			Enumeration<String> h1 = req.getHeaderNames();
			if (h1 != null) {
				while (h1.hasMoreElements()) {
					String s1 = h1.nextElement();
					head.append(s1, req.getHeader(s1));
				}
			}

			JSON body = getJSON(req).append("__node", node);

			for (String s1 : new String[] { method, uri, head.toString(), body.toString() }) {
				byte[] b = s1.getBytes();
				r.write(b.length);
				r.write(b);
			}

			IoRequest[] aa = new IoRequest[1];
			pending.put(s, aa);
			synchronized (aa) {
				_send(s, r);
				if (aa[0] == null) {
					aa.wait(TIMEOUT);
				}
			}

			if (aa[0] != null) {
				IoRequest a = aa[0];
				resp.setStatus(a.readInt());
				head = JSON.fromObject(a.readBytes(a.readInt()));

				// log.debug("head=" + head);

				for (String s1 : head.keySet()) {
					resp.addHeader(s1, head.getString(s1));
				}
				byte[] bb = a.readBytes(a.readInt());
				if (bb != null) {
					OutputStream out = resp.getOutputStream();
					out.write(bb);
					out.flush();
				}
				a.release();
			} else {
				throw new IOException("Timeout");
			}
		} catch (Exception e) {
			close();
			log.error(e.getMessage(), e);
		} finally {

			client.release(c);
			pending.remove(s);

			costs.addAndGet(t.pastms());
			if (maxcost < t.pastms()) {
				maxcost = t.pastms();
			}
			if (mincost > t.pastms()) {
				mincost = t.pastms();
			}

		}
		return;
	}

	private JSON getJSON(HttpServletRequest req) {

		JSON r = JSON.create();

		try {
			String c1 = req.getContentType();
			if (c1 != null && c1.indexOf("application/json") > -1) {
				BufferedReader in = req.getReader();

				StringBuilder sb = new StringBuilder();
				char[] buff = new char[1024];
				int len;
				while ((len = in.read(buff)) != -1) {
					sb.append(buff, 0, len);
				}

				if (log.isDebugEnabled())
					log.debug("params=" + sb.toString());

				JSON jo = JSON.fromObject(sb.toString());
				if (jo != null) {
					r.putAll(jo);
				}
			}

			if (ServletFileUpload.isMultipartContent(req)) {

				DiskFileItemFactory factory = new DiskFileItemFactory();

				// Configure a repository (to ensure a secure temp location is used)
				File repository = (File) GiiwaServlet.s️ervletContext.getAttribute("javax.servlet.context.tempdir");
				factory.setRepository(repository);

				// Create a new file upload handler
				ServletFileUpload upload = new ServletFileUpload(factory);

				// Parse the request
				try {
					List<FileItem> items = upload.parseRequest(req);
					if (items != null && items.size() > 0) {
						for (FileItem f : items) {

							if (f != null && f.isFormField()) {
								InputStream in = f.getInputStream();
								byte[] bb = new byte[in.available()];
								in.read(bb);
								in.close();
								r.append(f.getFieldName(),
										new String(bb, "UTF8").replaceAll("<", "&lt;").replaceAll(">", "&gt;").trim());
							}

						}
					} else {
						if (log.isWarnEnabled())
							log.warn("nothing got!!!");
					}
				} catch (FileUploadException e) {
					if (log.isErrorEnabled())
						log.error(e.getMessage(), e);
				}

			}

			Enumeration<?> e = req.getParameterNames();

			while (e.hasMoreElements()) {
				String name = e.nextElement().toString();
				r.append(name, req.getParameter(name));
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return r;
	}

	public static void main(String[] args) throws Exception {

		FileClient c = FileClient.get("tcp://g14:9099");

		// System.out.println(c.info("/Users/joe/d/temp", "/"));
		//
		// System.out.println(c.list("/Users/joe/d/temp", "/"));

		// c.mkdirs("/Users/joe/d/temp", "/tttttt");

		// c.delete("/Users/joe/d/temp", "/tttttt");

		// c.put("/Users/joe/d/temp", "/tttttt/t.txt", 0, "abcde".getBytes());

		// byte[] bb = c.get("/Users/joe/d/temp", "/tttttt/t.txt", 0, 100);
		// System.out.println(new String(bb));

		// DFileOutputStream out = DFileOutputStream.create("127.0.0.1", 9099,
		// "/Users/joe/d/temp", "/tttttt/t.txt");
		// out.write("asdasdasdasdasdasdasda".getBytes());
		// out.flush();
		// out.close();
		//
		// Disk d = new Disk();
		// d.set("path", "/Users/joe/d/temp");
		// Node n = new Node();
		// n.set("ip", "127.0.0.1");
		// n.set("port", 9099);
		// d.node_obj = n;

		// DFileInputStream in = DFileInputStream.create(d,
		// "/tttttt/WechatIMG3431.jpeg");
		// IOUtil.copy(in, new FileOutputStream("/Users/joe/d/temp/tttttt/a.jpg"));

		MockRequest req = new MockRequest();
		MockResponse resp = new MockResponse();

		c.http("/admin/device", req, resp, "get", "");

		System.out.println(resp);

		System.out.println("ok");
	}

	private void _send(long seq, IoResponse resp) {

		resp.send(resp.size() + 8, seq);

	}

	public static void measures(V v) {
		v.append("dfiletimes_c", FileClient.times.get());

		if (FileClient.times.get() > 0) {
			v.append("dfileavgcost_c", FileClient.costs.get() / FileClient.times.get());
			v.append("dfilemaxcost_c", FileClient.maxcost);
			v.append("dfilemincost_c", FileClient.mincost);
		} else {
			v.append("dfileavgcost_c", 0);
			v.append("dfilemaxcost_c", 0);
			v.append("dfilemincost_c", 0);
		}

	}

	public static void resetm() {

		FileClient.times.set(0);
		FileClient.costs.set(0);
		FileClient.maxcost = Long.MIN_VALUE;
		FileClient.mincost = Long.MAX_VALUE;

	}

}