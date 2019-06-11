package org.giiwa.core.dfile;

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
import org.apache.mina.core.buffer.IoBuffer;
import org.giiwa.core.json.JSON;
import org.giiwa.core.nio.Client;
import org.giiwa.framework.web.Model;

public class FileClient implements IRequestHandler {

	private static Log log = LogFactory.getLog(FileClient.class);

	private static final long TIMEOUT = 10000;

	private Map<Long, Request[]> pending = new HashMap<Long, Request[]>();

	private AtomicLong seq = new AtomicLong(0);

	private Client client;
	private String url;

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
		FileClient c = new FileClient();
		c.client = Client.connect(url, new FileServer.RequestHandler(url, c));
		return c;
	}

	public boolean delete(String path, String filename, long age) {
		if (client == null)
			return false;

		Response r = Response.create(seq.incrementAndGet(), Request.SMALL);
		try {

			r.writeByte(ICommand.CMD_DELETE);
			r.writeString(path);
			r.writeString(filename);
			r.writeLong(age);

			Request[] aa = new Request[1];
			pending.put(r.seq, aa);
			synchronized (aa) {
				_send(r);
				if (aa[0] == null) {
					aa.wait(TIMEOUT);
				}
			}

			if (aa[0] != null) {
				Request a = aa[0];
				return a.readByte() == 1 ? true : false;
			}

		} catch (Exception e) {
			close();
		} finally {
			pending.remove(r.seq);
		}
		return false;

	}

	private void close() {
		cached.remove(this.url);
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

		Response r = Response.create(seq.incrementAndGet(), Request.SMALL);

		try {
			r.writeByte(ICommand.CMD_GET);
			r.writeString(path);
			r.writeString(filename);
			r.writeLong(offset);
			r.writeInt(len);

			Request[] aa = new Request[1];
			pending.put(r.seq, aa);
			synchronized (aa) {
				_send(r);
				if (aa[0] == null) {
					aa.wait(TIMEOUT);
				}
			}

			if (aa[0] != null) {
				Request a = aa[0];
				return a.readBytes();
			}

		} catch (Exception e) {
			close();
		} finally {
			pending.remove(r.seq);
		}
		return null;
	}

	public long put(String path, String filename, long offset, byte[] bb, int len) {

		if (client == null)
			return -1;

		Response r = Response.create(seq.incrementAndGet(), Request.BIG);

		try {
			r.writeByte(ICommand.CMD_PUT);
			r.writeString(path);
			r.writeString(filename);
			r.writeLong(offset);
			r.writeBytes(bb, len);

			Request[] aa = new Request[1];
			pending.put(r.seq, aa);
			synchronized (aa) {
				_send(r);
				if (aa[0] == null) {
					aa.wait(TIMEOUT);
				}
			}

			if (aa[0] != null) {
				Request a = aa[0];
				return a.readLong();
			}

		} catch (Exception e) {
			close();
		} finally {
			pending.remove(r.seq);
		}
		return 0;
	}

	@Override
	public void process(Request r, IResponseHandler ch) {
		Request[] aa = pending.get(r.seq);
		if (aa != null) {
			synchronized (aa) {
				aa[0] = r;
				aa.notify();
			}
		}
	}

	public boolean mkdirs(String path, String filename) {
		if (client == null)
			return false;

		Response r = Response.create(seq.incrementAndGet(), Request.SMALL);

		try {

			r.writeByte(ICommand.CMD_MKDIRS);
			r.writeString(path);
			r.writeString(filename);

			Request[] aa = new Request[1];
			pending.put(r.seq, aa);
			synchronized (aa) {
				_send(r);
				if (aa[0] == null) {
					aa.wait(TIMEOUT);
				}
			}

			if (aa[0] != null) {
				Request a = aa[0];
				return a.readByte() == 1 ? true : false;
			}
		} catch (Exception e) {
			close();
		} finally {
			pending.remove(r.seq);
		}
		return false;

	}

	public List<FileInfo> list(String path, String filename) {

		if (client == null)
			return null;

		Response r = Response.create(seq.incrementAndGet(), Request.MID);

		try {
			r.writeByte(ICommand.CMD_LIST);
			r.writeString(path);
			r.writeString(filename);

			Request[] aa = new Request[1];
			pending.put(r.seq, aa);
			synchronized (aa) {
				_send(r);
				if (aa[0] == null) {
					aa.wait(TIMEOUT);
				}
			}

			if (aa[0] != null) {
				Request a = aa[0];

				List<FileInfo> l1 = new ArrayList<FileInfo>();

				while (a.hasRemaining()) {
					// JSON j1 = JSON.create();
					FileInfo info = new FileInfo();
					info.name = a.readString();
					info.exists = a.readInt() == 1;
					info.isfile = a.readInt() == 1;
					info.length = a.readLong();
					info.lastmodified = a.readLong();
					l1.add(info);
				}

				return l1;
			}

		} catch (Exception e) {
			close();
		} finally {
			pending.remove(r.seq);
		}
		return null;

	}

	public FileInfo info(String path, String filename) {

		if (client == null)
			return null;

		Response r = Response.create(seq.incrementAndGet(), Request.SMALL);

		try {

			r.writeByte(ICommand.CMD_INFO);
			r.writeString(path);
			r.writeString(filename);

			Request[] aa = new Request[1];
			pending.put(r.seq, aa);
			synchronized (aa) {
				_send(r);
				if (aa[0] == null) {
					aa.wait(TIMEOUT);
				}
			}

			if (aa[0] != null) {
				Request a = aa[0];

				FileInfo info = new FileInfo();
				info.exists = a.readInt() == 1;
				info.isfile = a.readInt() == 1;
				info.length = a.readLong();
				info.lastmodified = a.readLong();

				return info;
			}

		} catch (Exception e) {
			close();
		} finally {
			pending.remove(r.seq);

		}
		return null;
	}

	@Override
	public void closed(String name) {
		FileClient c = cached.remove(name);
		if (c != null && c.client != null) {
			c.client.close();
			c.client = null;
		}
	}

	public boolean move(String path, String filename, String path2, String filename2) {

		if (client == null)
			return false;

		Response r = Response.create(seq.incrementAndGet(), Request.SMALL);

		try {

			r.writeByte(ICommand.CMD_MOVE);
			r.writeString(path);
			r.writeString(filename);
			r.writeString(path2);
			r.writeString(filename2);

			Request[] aa = new Request[1];
			pending.put(r.seq, aa);
			synchronized (aa) {
				_send(r);
				if (aa[0] == null) {
					aa.wait(TIMEOUT);
				}
			}

			if (aa[0] != null) {
				Request a = aa[0];
				return a.readByte() == 1 ? true : false;
			}
		} catch (Exception e) {
			close();
		} finally {
			pending.remove(r.seq);
		}
		return false;
	}

	public void http(String uri, HttpServletRequest req, HttpServletResponse resp, String method, String node) {

		if (client == null)
			return;

		Response r = Response.create(seq.incrementAndGet(), Request.MID);

		try {

			r.writeByte(ICommand.CMD_HTTP);
			r.writeString(method);
			r.writeString(uri);
			JSON head = JSON.create();
			Enumeration<String> h1 = req.getHeaderNames();
			if (h1 != null) {
				while (h1.hasMoreElements()) {
					String s = h1.nextElement();
					head.append(s, req.getHeader(s));
				}
			}

			JSON body = getJSON(req).append("__node", node);

			r.writeString(head.toString());
			r.writeString(body.toString());

			// r.writeBytes(null);

			Request[] aa = new Request[1];
			pending.put(r.seq, aa);
			synchronized (aa) {
				_send(r);
				if (aa[0] == null) {
					aa.wait(TIMEOUT);
				}
			}

			if (aa[0] != null) {
				Request a = aa[0];
				resp.setStatus(a.readInt());
				head = JSON.fromObject(a.readString());

				// log.debug("head=" + head);

				for (String s : head.keySet()) {
					resp.addHeader(s, head.getString(s));
				}
				byte[] bb = a.readBytes();
				if (bb != null) {
					OutputStream out = resp.getOutputStream();
					out.write(bb);
					out.flush();
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			close();
			pending.remove(r.seq);
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

				log.debug("params=" + sb.toString());

				JSON jo = JSON.fromObject(sb.toString());
				if (jo != null) {
					r.putAll(jo);
				}
			}

			if (ServletFileUpload.isMultipartContent(req)) {

				DiskFileItemFactory factory = new DiskFileItemFactory();

				// Configure a repository (to ensure a secure temp location is used)
				File repository = (File) Model.s️ervletContext.getAttribute("javax.servlet.context.tempdir");
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

	private void _send(Response resp) {
		resp.out.flip();
		IoBuffer b = IoBuffer.allocate(resp.out.remaining() + 4);
		b.putInt(resp.out.remaining());
		b.put(resp.out);
		b.flip();
		client.write(b);

		b.free();
		resp.out.free();

	}

}