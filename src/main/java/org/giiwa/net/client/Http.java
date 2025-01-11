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
package org.giiwa.net.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Temp;
import org.giiwa.conf.Global;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.misc.Html;
import org.giiwa.misc.MD5;
import org.giiwa.misc.StringFinder;
import org.giiwa.misc.Url;
import org.giiwa.task.Console;
import org.giiwa.web.QueryString;

import okhttp3.Authenticator;
import okhttp3.CookieJar;
import okhttp3.Credentials;
import okhttp3.Dns;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.MultipartBody.Part;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.Route;

/**
 * http utils
 * 
 * @author joe
 * 
 */
public final class Http {

	// private static final long MIN_ZIP_SIZE = 1024 * 1024 * 1024;
	static Log log = LogFactory.getLog(Http.class);

	private final static String UA[] = new String[] { "Mozilla/5.0 Macintosh; AppleWebKit/{n2}.{n3} Chrome/{n4}",
			"Mozilla/5.0 (Macintosh; Intel Mac {n1}) AppleWebKit/{n2} (KHTML, like Gecko) Version/{n4} Safari/{n3}" };

	Map<String, okhttp3.Cookie> cookies = null;

	private OkHttpClient client;
	private OkHttpClient.Builder builder;

	public String user = null;
	public String passwd = null;

	public Http proxy(String proxy, String user, String passwd) {
		if (X.isEmpty(proxy)) {
			return new Http(null, user, passwd, X.AMINUTE);
		} else {
			String[] ss = X.split(proxy, "[,;]");
			String s = ss[X.toInt(ss.length * Math.random())];
			ss = X.split(s, ":");
			Proxy p1 = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ss[0], X.toInt(ss[1])));
			return new Http(p1, user, passwd, X.AMINUTE);
		}
	}

	public Http proxy(String proxy) {
		return proxy(proxy, null, null);
	}

	private Http(Proxy proxy, String user, String passwd, long timeout) {

		this.user = user;
		this.passwd = passwd;

		try {
			X509TrustManager manager = new X509TrustManager() {
				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

				}

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[0];
				}
			};

			SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, new TrustManager[] { manager }, new SecureRandom());
			SSLSocketFactory socketFactory = sslContext.getSocketFactory();

			builder = new OkHttpClient().newBuilder().dns(new Dns() {

				@Override
				public List<InetAddress> lookup(String host) throws UnknownHostException {
					List<InetAddress> l1 = dns.get(host);
					if (l1 == null || l1.isEmpty()) {
						l1 = Arrays.asList(InetAddress.getByName(host));
					}
					return l1;
				}

			}).proxy(proxy).cookieJar(new CookieJar() {

				@Override
				public List<okhttp3.Cookie> loadForRequest(HttpUrl url) {
					if (cookies != null) {
						return new ArrayList<okhttp3.Cookie>(cookies.values());
					}
					return Arrays.asList();
				}

				@Override
				public void saveFromResponse(HttpUrl url, List<okhttp3.Cookie> cookie) {
					if (cookies == null) {
						cookies = new HashMap<String, okhttp3.Cookie>();
					}
					for (okhttp3.Cookie e : cookie) {
						if (e != null) {
							String name = e.name();
							cookies.put(name, e);
						}
					}
				}

			}).sslSocketFactory(socketFactory, manager).hostnameVerifier(new HostnameVerifier() {

				@Override
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}

			});

			if (!X.isEmpty(user)) {
				builder.proxyAuthenticator(new Authenticator() {

					@Override
					public Request authenticate(Route route, okhttp3.Response response) throws IOException {
						String credential = Credentials.basic(user, passwd);
						return response.request().newBuilder().header("Proxy-Authorization", credential).build();
					}

				});
			}

			if (timeout > 0) {
				builder.callTimeout(timeout, TimeUnit.MILLISECONDS);
				builder.connectTimeout(timeout, TimeUnit.MILLISECONDS);
				builder.writeTimeout(timeout, TimeUnit.MILLISECONDS);
				builder.readTimeout(timeout, TimeUnit.MILLISECONDS);
			}

			client = builder.build();

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}

	private static String _UA() {
		int i = (int) (UA.length * Math.random());
		return UA[i].replace("{n1}", UID.digital(8)).replace("{n2}", UID.digital(3))
				.replace("{n3}", UID.digital(2) + "." + UID.digital(1) + "." + UID.digital(3))
				.replace("{n4}", UID.digital(10));
	}

	/**
	 * create a default Http2 client
	 * 
	 * @return the Http2
	 */
	public static Http create() {
		return create(Global.getString("http.proxy", null), null, null);
	}

	public static Http create(String proxy) {
		return create(proxy, null, null);
	}

	/**
	 * create a Http2 with the proxy
	 * 
	 * @return the Http2
	 */
	public static Http create(String proxy, String user, String passwd) {
		return create(proxy, user, passwd, X.AMINUTE);
	}

	public static Http create(String proxy, String user, String passwd, long timeout) {
		if (X.isEmpty(proxy)) {
			return new Http(null, user, passwd, timeout);
		} else {
			String[] ss = X.split(proxy, "[,;]");
			String s = ss[(int) (ss.length * Math.random())];
			ss = X.split(s, ":");
			Proxy p1 = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ss[0], X.toInt(ss[1])));

			return new Http(p1, user, passwd, timeout);
		}
	}

	public Response get(String url) {
		return get(url, null);
	}

	public Response get(String url, JSON head) {

		Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent",
				_UA());
		request.addHeader("Connection", "close");

		if (head != null) {
			for (String name : head.keySet()) {
				request.addHeader(name, head.getString(name));
			}
		}

		okhttp3.Response response = null;

		TimeStamp t = TimeStamp.create();
		try {
			response = client.newCall(request.get().build()).execute();
			return Response.create(response).url(url);
		} catch (Exception e) {
			log.error("cost=" + t.past() + ", url=" + url, e);
			return Response.create(e);
		} finally {
			X.close(response);
		}
	}

	public Response delete(String url) {

		Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent",
				_UA());
		request.addHeader("Connection", "close");

		okhttp3.Response response = null;
		try {
			response = client.newCall(request.delete().build()).execute();
			return Response.create(response);
		} catch (Exception e) {
			return Response.create(e);
		} finally {
			X.close(response);
		}
	}

	public Response post(String url, JSON body) {
		return form(url, body);
	}

	public Response form(String url, JSON body) {

		Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent",
				_UA());

		request.addHeader("Connection", "close");

		if (body != null && !body.isEmpty()) {

			StringBuilder sb = new StringBuilder();
			for (String name : body.keySet()) {
				if (sb.length() > 0) {
					sb.append("&");
				}
				sb.append(name).append("=");
				Object o = body.get(name);
				if (o instanceof String) {
					sb.append(Url.encode((String) o));
				} else {
					sb.append(o);
				}
			}

			MediaType CC = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");

			RequestBody bb = RequestBody.create(sb.toString(), CC);
			request.post(bb);

		}

		okhttp3.Response response = null;

		try {
			response = client.newCall(request.build()).execute();
			return Response.create(response).url(url);
		} catch (Exception e) {
			log.error(url, e);
			return Response.create(e);
		} finally {
			X.close(response);
		}

	}

	public Response json(String url, JSON body) {

		Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent",
				_UA());

		if (body != null && !body.isEmpty()) {

			String s = "application/json; charset=utf-8";
			MediaType _JSON = MediaType.parse(s);

			request.addHeader("Content-Type", s);
			request.addHeader("Connection", "close");
			RequestBody bb = RequestBody.create(body.toString(), _JSON);
			request.post(bb);
		}

		okhttp3.Response response = null;

		try {
			response = client.newCall(request.build()).execute();
			return Response.create(response).url(url);
		} catch (Exception e) {
			log.error(url, e);
			return Response.create(e);
		} finally {
			X.close(response);
		}

	}

	public Response json(String url, JSON head, JSON body) {

		Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent",
				_UA());

		String s = "application/json; charset=utf-8";
		MediaType h = MediaType.parse(s);

		String bodystring = X.EMPTY;
		if (body != null && !body.isEmpty()) {
			bodystring = body.toString();
		}

		request.addHeader("Connection", "close");
		request.addHeader("Content-Type", s);
		if (head != null) {
			for (String name : head.keySet()) {
				request.addHeader(name, head.getString(name));
			}
		}

		RequestBody bb = RequestBody.create(bodystring, h);
		request.post(bb);

		okhttp3.Response response = null;

		try {
			response = client.newCall(request.build()).execute();
			return Response.create(response).url(url);
		} catch (Exception e) {
			log.error(url, e);
			return Response.create(e);
		} finally {
			X.close(response);
		}

	}

	public Response post(String url, JSON head, JSON body) {
		return post(url, head, body, null, null, null);
	}

	public Response post(String url, JSON head, JSON body, String field, String filename, InputStream in) {
		return post(url, head, body, field, filename, in, false);
	}

	/**
	 * post data, with resume upload binary
	 * 
	 * @param url      the server url
	 * @param head     the head of post
	 * @param body     the body parameter
	 * @param field    the binary field
	 * @param filename the binary filename
	 * @param in       the binary
	 * @param resume   true:resume
	 * @return
	 */
	public Response post(String url, JSON head, JSON body, String field, String filename, InputStream in,
			boolean resume) {

		if (resume && in != null) {
			try {
				if (X.isEmpty(field)) {
					throw new Exception("field param is null!");
				}

				String time = Long.toString(System.currentTimeMillis());
				byte[] buf = X.IO.read(in, false);
				String md5 = MD5.md5(buf);

				int offset = 0;
				Response r = _resume_post(url, head, body, field, filename, buf, offset, time, md5, 0);
				log.info("r=" + r.body);
//				System.out.println("r=" + r.body);

				JSON j1 = r.json();
				offset = j1.getInt("pos");
				while (offset < buf.length) {
					r = _resume_post(url, head, body, field, filename, buf, offset, time, md5, offset);
//					System.out.println("r=" + r.body);
					log.info("r=" + r.body);
					j1 = r.json();
					Object error = j1.get("error");
					if (!X.isNumber(error) || X.toInt(error) != 0) {
						throw new Exception(r.body);
					}

					offset = j1.getInt("pos");
				}
				return r;
			} catch (Exception e) {
				log.error(url, e);
//				e.printStackTrace();
				return Response.create(e);
			} finally {
				X.close(in);
			}
		} else {

			okhttp3.Response response = null;

			try {

				Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent")
						.addHeader("User-Agent", _UA());

				String contentype = null;
				if (head != null) {
					for (String name : head.keySet()) {
						if (X.isSame(name, "content-type")) {
							contentype = head.getString(name);
						}
						request.addHeader(name, head.getString(name));
					}
				}

				if (contentype != null && contentype.toLowerCase().startsWith("application/json") && in == null) {
					return json(url, head, body);
				}

				int n = 0;
				MultipartBody.Builder bb = new MultipartBody.Builder();
				bb.setType(MultipartBody.FORM);
				if (body != null && !body.isEmpty()) {
					for (String name : body.keySet()) {
						bb.addFormDataPart(name, body.getString(name));
						n++;
					}
				}
				if (!X.isEmpty(field)) {
					n++;
					byte[] buf = X.IO.read(in, false);
					bb.addFormDataPart(field, filename,
							RequestBody.create(buf, MediaType.parse("application/octet-stream")));
				}
				if (n > 0) {
					request.post(bb.build());
				}

				response = client.newCall(request.build()).execute();
				return Response.create(response).url(url);
			} catch (Exception e) {
				log.error(url, e);
				return Response.create(e);
			} finally {
				X.close(in, response);
			}
		}
	}

	private Response _resume_post(String url, JSON head, JSON body, String field, String filename, byte[] in,
			int offset, String time, String md5, int retries) throws Exception {

		okhttp3.Response response = null;

		try {

			Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent",
					_UA());
			request.addHeader("Connection", "close");

			if (head != null) {
				for (String name : head.keySet()) {
					request.addHeader(name, head.getString(name));
				}
			}

			MultipartBody.Builder bb = new MultipartBody.Builder();
			bb.setType(MultipartBody.FORM);
			if (body != null && !body.isEmpty()) {
				for (String name : body.keySet()) {
					bb.addFormDataPart(name, body.getString(name));
				}
			}

			int size = Math.min(in.length - offset, 1024 * 32);

			// Content-Range: bytes 0-32769/23550094
			request.addHeader("Content-Range", "bytes " + offset + "-" + (offset + size) + "/" + in.length);
			request.addHeader("lastModified", time);
			request.addHeader("identifier", md5);

			bb.addFormDataPart(field, filename,
					RequestBody.create(in, MediaType.parse("application/octet-stream"), offset, size));
			request.post(bb.build());

			response = client.newCall(request.build()).execute();

			return Response.create(response).url(url);
		} catch (Exception e) {
			log.error(url, e);
			if (retries < 10) {
				Thread.sleep(3000);
				return _resume_post(url, head, body, field, filename, in, offset, time, md5, retries + 1);
			} else {
				return Response.create(e);
			}
		} finally {
			X.close(response);
		}

	}

	public Response post(String url, JSON head, String body) {

		Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent",
				_UA());
		if (head != null) {
			for (String name : head.keySet()) {
				request.addHeader(name, head.getString(name));
			}
		}
		request.addHeader("Connection", "close");

		String contentype = "application/json";
		if (head != null && head.containsKey("Content-Type")) {
			contentype = head.getString("Content-Type");
		}

		MediaType CC = MediaType.parse(contentype);

		RequestBody bb = RequestBody.create(body, CC);

		request.post(bb);

		okhttp3.Response response = null;
		try {
			response = client.newCall(request.build()).execute();
			return Response.create(response).url(url);
		} catch (Exception e) {
			log.error(url, e);
			return Response.create(e);
		} finally {
			X.close(response);
		}

	}

	public Response post(String url, String body) {
		return post(url, "application/json", body);
	}

	public Http clear() {
		if (cookies != null) {
			cookies.clear();
		}
		return this;
	}

	public Response post(String url, String contentype, String body) {

		Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent",
				_UA());

		request.addHeader("Connection", "close");

		MediaType CC = MediaType.parse(contentype);

		RequestBody bb = RequestBody.create(body, CC);

		request.post(bb);

		okhttp3.Response response = null;
		try {
			response = client.newCall(request.build()).execute();
			return Response.create(response).url(url);
		} catch (Exception e) {
			log.error(url, e);
			return Response.create(e);
		} finally {
			X.close(response);
		}

	}

	public Response put(String url, JSON body) {

		Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent",
				_UA());
		request.addHeader("Connection", "close");

		if (body != null && !body.isEmpty()) {
			MultipartBody.Builder bb = new MultipartBody.Builder();
			for (String key : body.keySet()) {
				bb.addPart(Part.createFormData(key, body.getString(key)));
			}
			request.put(bb.build());
		}

		okhttp3.Response response = null;
		try {
			response = client.newCall(request.build()).execute();
			return Response.create(response).url(url);
		} catch (Exception e) {
			log.error(url, e);
			return Response.create(e);
		} finally {
			X.close(response);
		}
	}

	public Response put(String url, String body) {

		Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent",
				_UA());
		request.addHeader("Connection", "close");

		if (!X.isEmpty(body)) {
			MediaType CC = MediaType.parse("application/json");

			RequestBody bb = RequestBody.create(body, CC);

			request.put(bb);
		}

		okhttp3.Response response = null;
		try {
			response = client.newCall(request.build()).execute();
			return Response.create(response).url(url);
		} catch (Exception e) {
			log.error(url, e);
			return Response.create(e);
		} finally {
			X.close(response);
		}
	}

	public Response head(String url) {

		Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent",
				_UA());
		request.addHeader("Connection", "close");
		request.head();

		okhttp3.Response response = null;
		try {
			response = client.newCall(request.build()).execute();
			return Response.create(response).url(url);
		} catch (Exception e) {
			return Response.create(e);
		} finally {
			X.close(response);
		}
	}

	@SuppressWarnings("unchecked")
	public String cookie() {
		if (cookies != null) {
			try {
				return X.join(X.asList(cookies.values(), e -> {
					if (e == null) {
						return null;
					}

					Map<String, okhttp3.Cookie> e1 = (Map<String, okhttp3.Cookie>) e;
					return X.join(X.asList(e1.values(), e2 -> {
						if (e2 == null) {
							return null;
						}
						return ((okhttp3.Cookie) e2).name() + "=" + ((okhttp3.Cookie) e2).value();
					}), ";");
				}), ";");
			} catch (Exception e) {
				log.error(cookies.toString(), e);
			}
		}
		return X.EMPTY;
	}

	/**
	 * download the remote url to local file with the header.
	 *
	 * @param url       the remote resource url
	 * @param localfile the localfile
	 * @return the length of bytes
	 */
	public long download(String url, OutputStream out) {
		return download(url, null, null, out);
	}

	public long download(String url, JSON head, OutputStream out) {
		return download(url, head, null, out);
	}

	public long download(String url, JSON head, JSON body, OutputStream out) {

//		try {
		if (log.isDebugEnabled()) {
			log.debug("url=\"" + url + "\"");
		}

		Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent",
				_UA());
		request.addHeader("Connection", "close");

		if (head != null && !head.isEmpty()) {
			for (String name : head.keySet()) {
				request.addHeader(name, head.getString(name));
			}
		}

		if (body != null && !body.isEmpty()) {
			MultipartBody.Builder bb = new MultipartBody.Builder();
			for (String key : body.keySet()) {
				bb.addPart(Part.createFormData(key, body.getString(key)));
			}
			request.setBody$okhttp(bb.build());
		}

		okhttp3.Response response = null;

		try {

			response = client.newCall(request.build()).execute();
			ResponseBody responseBody = response.body();
			return X.IO.copy(responseBody.byteStream(), out, (pos, len) -> {
				if (Console._DEBUG) {
					Console.inst.log("downloading ... " + len);
				}
			});
		} catch (Exception e) {
			log.error(url, e);
		} finally {
			X.close(response);
		}
		return 0;
	}

	public Temp download(String url, boolean resume) {
		return download(url, (JSON) null, (JSON) null, resume);
	}

	public Temp download(String url) {
		return download(url, (JSON) null, (JSON) null, false);
	}

	public Temp download(String url, JSON head, boolean resume) {
		return download(url, head, (JSON) null, resume);
	}

	public Temp download(String url, JSON head) {
		return download(url, head, (JSON) null, false);
	}

	public Temp download(String url, JSON head, JSON body) {
		return download(url, head, body, false);
	}

	public Temp download(String url, JSON head, JSON body, boolean resume) {
		return _download(url, head, body, resume, null);
	}

	private Temp _download(String url, JSON head, JSON body, boolean resume, Temp t) {

		if (log.isDebugEnabled()) {
			log.debug("url=\"" + url + "\"");
		}

		if (resume) {

			Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent",
					_UA());
			request.addHeader("Connection", "close");

			if (head != null && !head.isEmpty()) {
				for (String name : head.keySet()) {
					request.addHeader(name, head.getString(name));
				}
			}

			// bytes=0-1
			request.addHeader("range", "bytes=0-");

			if (body != null && !body.isEmpty()) {
				MultipartBody.Builder bb = new MultipartBody.Builder();
				for (String key : body.keySet()) {
					bb.addPart(Part.createFormData(key, body.getString(key)));
				}
				request.setBody$okhttp(bb.build());
			}

			okhttp3.Response resp = null;

			try {
				resp = client.newCall(request.build()).execute();

				String filename = "a";

				String s = resp.header("content-disposition");
				if (X.isEmpty(s)) {
					s = url;

					for (String s1 : new String[] { "?", "#" }) {
						int i = s.indexOf(s1);
						if (i > 0) {
							s = s.substring(0, i);
						}
					}

					int i = s.lastIndexOf("/");
					if (i > 0) {
						s = s.substring(i + 1);
					}
					filename = s;

				} else {
					int i = s.indexOf("=");
					if (i >= 0) {
						// filename=....
						// filename=UTF8''.....
						filename = s.substring(i + 1).trim();
						i = filename.lastIndexOf("'");
						filename = filename.substring(i + 1);
					}
				}

				ResponseBody respbody = resp.body();
				if (t == null) {
					t = Temp.create(filename);
				}

//				long length = X.toLong(resp.header("Content-Length"));

				String range = resp.header("content-range");
				// bytes start-end/total

				if (!X.isEmpty(range)) {
					// support resume
					String[] ss = X.split(range, "[ -/]");
//					long start = X.toLong(ss[1]);
//					long end = X.toLong(ss[2]);
					long total = X.toLong(ss[3]);

					X.close(resp);
					resp = null;
					OutputStream out = t.getOutputStream();

					while (!_resume_download(t, out, url, head, body, total)) {
						// continue;
					}

					X.close(out);

				} else {
					OutputStream out = t.getOutputStream();
					X.IO.copy(respbody.byteStream(), out, (pos, len) -> {
						if (Console._DEBUG) {
							Console.inst.log("downloading ... " + len);
						}
					});
				}

				return t;
			} catch (Exception e) {
				log.error(url, e);
			} finally {
				X.close(resp);
			}
		} else {
			Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent",
					_UA());

			if (head != null && !head.isEmpty()) {
				for (String name : head.keySet()) {
					request.addHeader(name, head.getString(name));
				}
			}

			if (body != null && !body.isEmpty()) {
				MultipartBody.Builder bb = new MultipartBody.Builder();
				for (String key : body.keySet()) {
					bb.addPart(Part.createFormData(key, body.getString(key)));
				}
				request.setBody$okhttp(bb.build());
			}

			okhttp3.Response response = null;

			try {
				response = client.newCall(request.build()).execute();

//			System.out.println(response.headers());

				String filename = "a";

				String s = response.header("content-disposition");
				if (X.isEmpty(s)) {
					s = url;

					for (String s1 : new String[] { "?", "#" }) {
						int i = s.indexOf(s1);
						if (i > 0) {
							s = s.substring(0, i);
						}
					}

					int i = s.lastIndexOf("/");
					if (i > 0) {
						s = s.substring(i + 1);
					}
					filename = s;

				} else {
					int i = s.indexOf("=");
					if (i >= 0) {
						// filename=....
						// filename=UTF8''.....
						filename = s.substring(i + 1).trim();
						i = filename.lastIndexOf("'");
						filename = filename.substring(i + 1);
					}
				}
//			System.out.println(filename);

				ResponseBody responseBody = response.body();
				if (t == null) {
					t = Temp.create(filename);
				}
				OutputStream out = t.getOutputStream();
				X.IO.copy(responseBody.byteStream(), out, (pos, len) -> {
					if (Console._DEBUG) {
						Console.inst.log("downloading ... " + len);
					}
				});

				return t;
			} catch (Exception e) {
				log.error(url, e);
			} finally {
				X.close(response);
			}
		}

		return null;
	}

	private boolean _resume_download(Temp t, OutputStream out, String url, JSON head, JSON body, long total)
			throws IOException {

//		System.out.println("downloading: " + t.length());

		Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent",
				_UA());
		request.addHeader("Connection", "close");

		if (head != null && !head.isEmpty()) {
			for (String name : head.keySet()) {
				request.addHeader(name, head.getString(name));
			}
		}

		File f = t.getFile();
		request.addHeader("range", f.length() + "-" + (f.length() + 32 * 1024));

		if (body != null && !body.isEmpty()) {
			MultipartBody.Builder bb = new MultipartBody.Builder();
			for (String key : body.keySet()) {
				bb.addPart(Part.createFormData(key, body.getString(key)));
			}
			request.setBody$okhttp(bb.build());
		}

		okhttp3.Response resp = null;

		try {
			resp = client.newCall(request.build()).execute();

			ResponseBody respbody = resp.body();

			if (resp.code() != 200) {
				log.error("url=" + url);
				log.error("resp.code=" + resp.code() + ", head=\n" + resp.headers());
				Console.inst.log("url=" + url);
				Console.inst.log("resp.code=" + resp.code() + ", head=\n" + resp.headers());
			}

			String range = resp.header("Content-Range");
			// bytes start-end/total

			String[] ss = X.split(range, "[ -/]");
			if (ss.length >= 4) {
				total = X.toLong(ss[3]);
//			} else if (resp.code() == 206) {
//				//
//				log.error("bad content-range=" + range);
//				Console.inst.log("bad content-range=" + range);
//				return _resume_download(t, out, url, head, body, total);
			} else {
				log.error("bad content-range=" + range + ", url=" + url);
				Console.inst.log("bad content-range=" + range + ", url=" + url);
			}

			InputStream in = respbody.byteStream();
			X.IO.copy(in, out, false);
			X.close(in);
			out.flush();

			if (f.length() == 0) {
				// not fit resume download
				_download(url, head, body, false, t);
				return true;
			} else if (f.length() < total) {
				log.error(f.length() + " < " + total + ", content-range=" + range + ", url=" + url);
				Console.inst.log(f.length() + " < " + total + ", content-range=" + range + ", url=" + url);
				return false;
			} else {
				return true;
			}

		} finally {
			X.close(resp);
		}

	}

	private Map<String, List<InetAddress>> dns = new HashMap<String, List<InetAddress>>();

	public void dns(String host, String... ip) {
		List<InetAddress> l1 = new ArrayList<InetAddress>();
		for (String s : ip) {
			try {
				InetAddress e = InetAddress.getByName(s);
				l1.add(e);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		dns.put(host, l1);
	}

	/**
	 * the http response
	 * 
	 * @author joe
	 *
	 */
	public static class Response {

		/**
		 * 状态
		 */
		public int status;

		public String url;

		public String type; // Content-Type
		/**
		 * 返回数据body
		 */
		public String body;
		public byte[] _body;

		/**
		 * 返回头部信息
		 */
		private Headers headers;

		public static Response create(okhttp3.Response res) {
			Response e = new Response();

			try {
				e.status = res.code();
				e.body = res.body().string();
				e.headers = res.headers();
			} catch (Exception e1) {
				log.error(e1.getMessage(), e1);
			}
			return e;
		}

		public static Response create(int status, Map<String, String> head, String body) {
			Response e = new Response();

			try {
				e.status = status;
				e.body = body;
				e.headers = Headers.of(head);
			} catch (Exception e1) {
				log.error(e1.getMessage(), e1);
			}
			return e;
		}

		public static Response create(Exception err) {
			Response e = new Response();

			e.status = 500;
			e.body = err.getMessage();
			log.error(err.getMessage(), err);
			return e;
		}

		public Response url(String url) {
			this.url = url;
			return this;
		}

		/**
		 * 用body 生成StringFinder工具类
		 * 
		 * @return StringFinder工具类
		 */
		public StringFinder finder() {
			return StringFinder.create(body);
		}

		@Override
		public String toString() {
			return body;
		}

		/**
		 * 用body生成 Html工具类
		 * 
		 * @return Html工具类
		 */
		public Html html() {
			Html m = Html.create(body);
			m.url = url;
			return m;
		}

		public String cookie() {
			String ss = this.getHeaders("Set-Cookie");
			return ss;
//			return X.join(X.asList(ss, s -> {
//				String s1 = s.toString();
//				int i = s1.indexOf(";");
//				if (i > 0) {
//					return s1.substring(0, i);
//				}
//				return s1;
//			}), ";");
		}

		/**
		 * 用body 生成json对象
		 * 
		 * @return JSON对象
		 */
		public JSON json() {
			return JSON.fromObject(body);
		}

		/**
		 * 用body生成json对象
		 * 
		 * @return
		 */
		public JSON xml() {
			return JSON.fromXml(body);
		}

		public List<JSON> jsons() {
			return JSON.fromObjects(body);
		}

		/**
		 * get the header
		 * 
		 * @return Header[]
		 */
		public Set<String> getHeader() {
			return headers.names();
		}

		/**
		 * get the response header.
		 *
		 * @param name the name
		 * @return String
		 */
		public String getHeaders(String name) {
			return headers.get(name);
		}

		private String _protocol(String url) {
			int i = url.indexOf("/");
			if (i > 0) {
				return url.substring(0, i);
			}
			return url;
		}

		private String _server(String url) {
			int i = url.indexOf("/", 8);
			if (i > 0) {
				return url.substring(0, i);
			}
			return url;
		}

		private String _path2(String url) {
			int i = url.indexOf("?");
			if (i > 8) {
				return url.substring(0, i);
			}
			return url;
		}

		private String _path(String url) {
			int i = url.lastIndexOf("/");
			if (i > 8) {
				return url.substring(0, i + 1);
			}
			return url + "/";
		}

		public String format(String href) {

			String h1 = href.toLowerCase();
			if (h1.startsWith("http://") || h1.startsWith("https://")) {
				return href;
			} else if (href.startsWith("//")) {
				href = _protocol(url) + href;
			} else if (href.startsWith("/")) {
				href = _server(url) + href;
			} else if (href.startsWith("?")) {
				href = _path2(url) + href;
			} else {
				href = _path(url) + href;
			}
			int i = href.indexOf("#");
			if (i > 0) {
				href = href.substring(0, i);
			}

			if (X.isEmpty(href))
				return null;

			QueryString qs = new QueryString(href);
//			qs.remove(removals);

			return qs.toString();

		}

		public Temp save(String filename) {
			Temp t = Temp.create(filename);
			OutputStream out = null;

			try {
				out = t.getOutputStream();
				if (_body != null) {
					out.write(_body);
				} else if (body != null) {
					out.write(body.getBytes());
				}
				return t;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			} finally {
				X.close(out);
			}
			return null;
		}

	}

	/**
	 * add a cookie in Http. or replace the old one by (name, domain, path)
	 *
	 * @param name    the name
	 * @param value   the value
	 * @param domain  the domain
	 * @param path    the path
	 * @param expired the expired date
	 */
	public void addCookie(String name, String value, String domain, String path, Date expired) {
		if (cookies == null) {
			cookies = new HashMap<String, okhttp3.Cookie>();
		}
//sid=576a0b7e-0e72-4235-b107-75bbc27da982; Max-Age=604800; Expires=Wed Nov 30 09:44:29 CST 2022; Path=/;httponly
		String s = name + "=" + value;
		okhttp3.Cookie e = okhttp3.Cookie.parse(HttpUrl.get("http://" + domain + "/"), s);
		cookies.put(name, e);
	}

	/**
	 * batchCookies
	 * 
	 * @param cookiestring the cookie string, eg.:"a=b;c=a"
	 * @param domain       the domain
	 * @param path         the path
	 * @param expired      the expired date
	 */
	public void batchCookie(String cookiestring, String domain, String path, Date expired) {
		String[] ss = X.split(cookiestring, ";");
		for (String s : ss) {
			StringFinder sf = StringFinder.create(s);
			String name = sf.nextTo("=");
			String value = sf.remain();
			if (!X.isEmpty(name)) {
				addCookie(name, value, domain, path, expired);
			}
		}
	}

	/**
	 * Removes the cookie.
	 *
	 * @param name   the name
	 * @param domain the domain
	 * @param path   the path
	 */
	public void removeCookie(String name) {
		if (cookies != null) {
			cookies.remove(name);
		}
	}

	public static String format(String href, String... removals) {

		if (X.isEmpty(href))
			return null;

		String[] ss = X.split(href, "[?&]");
		if (ss.length < 2) {
			return _format(ss[0]);
		}
		TreeMap<String, String> p = new TreeMap<String, String>();
		for (int i = 1; i < ss.length; i++) {
			StringFinder f = StringFinder.create(ss[i]);
			String name = f.nextTo("=");
			f.skip(1);
			f.trim();
			String value = f.remain();
			if (!X.isEmpty(name)) {
				p.put(name, value);
			}
		}
		if (removals != null) {
			for (String s : removals) {
				p.remove(s);
			}
		}
		StringBuilder sb = new StringBuilder();
		for (String name : p.keySet()) {
			if (sb.length() > 0)
				sb.append("&");

			sb.append(name).append("=");
			if (!X.isEmpty(p.get(name))) {
				sb.append(p.get(name));
			}
		}
		if (sb.length() > 0) {
			return _format(ss[0]) + "?" + sb.toString();
		}
		return _format(ss[0]);
	}

	private static String _format(String url) {
		if (url.indexOf("/./") > 0 || url.indexOf("/../") > 0) {
			String s1 = url.substring(0, 8);
			url = url.substring(8);
			String[] ss = X.split(url, "/");
			List<String> l1 = new ArrayList<String>();
			for (String s : ss) {
				if (X.isSame(".", s)) {
					continue;
				}
				if (X.isSame("..", s)) {
					l1.remove(l1.size() - 1);
					continue;
				}
				l1.add(s);
			}
			StringBuilder sb = new StringBuilder();
			for (String s : l1) {
				if (sb.length() > 0)
					sb.append("/");
				sb.append(s);
			}
			url = s1 + sb.toString();
		}
		return url;
	}

	public static String server(String url) {
		int i = url.indexOf("/", 8);
		if (i > 0) {
			return url.substring(0, i);
		}
		return url;
	}

	public static String host(String url) {
		if (url.startsWith("http")) {
			String[] ss = X.split(url, "[/:?&.]");
			return ss != null && ss.length > 1 ? ss[1] : null;
		} else {
			String[] ss = X.split(url, "[/:?&.]");
			return ss != null && ss.length > 0 ? ss[0] : null;
		}
	}

	private static final String[] TOP = { "top", "cn", "com", "net", "love", "org", "biz", "info", "name", "tv", "me",
			"mobi", "asia", "eu", "in", "us", "cc", "com.cn", "net.cn", "org.cn", "gov.cn" };

	private static String _top(String host) {
		int len = 0;
		String s = null;
		for (String s1 : TOP) {
			if (host.endsWith(s1)) {
				if (s1.length() > len) {
					s = s1;
					len = s1.length();
				}
			}
		}
		return s;
	}

	public static String domain(String url, int subnum) {
		String host = host(url);
		String top = _top(host);
		String s = host.substring(0, host.length() - top.length() - 1);

		String[] ss = X.split(s, "\\.");

		StringBuilder sb = new StringBuilder();

		for (int i = Math.max(0, ss.length - subnum); i < ss.length; i++) {
			if (sb.length() > 0) {
				sb.append(".");
			}

			sb.append(ss[i]);
		}
		return sb.length() == 0 ? null : sb.append(".").append(top).toString();
	}

	public static String path(String url) {
		int i = url.lastIndexOf("/");
		if (i > 8) {
			return url.substring(0, i + 1);
		}
		return url + "/";
	}

	public static String uri(String url) {
		return X.split(url, "[.?&]")[0];
	}

	public static boolean inSite(String href, String[] domains) {
		String host = host(href);
		if (X.isEmpty(host))
			return false;
		for (String s : domains) {
			if (host.indexOf(s) > -1) {
				return true;
			}
		}
		return false;
	}

	public static String protocal(String url) {
		String[] ss = X.split(url, ":");
		return ss[0];
	}

	public void redirect(boolean b) {
		builder.setFollowRedirects$okhttp(b);
		builder.setFollowSslRedirects$okhttp(b);
		client = builder.build();
	}

	public static class Builder {

		String proxy = null;
		String user;
		String passwd;
		long timeout = -1;

		public Builder proxy(String proxy, String user, String passwd) {
			this.proxy = proxy;
			this.user = user;
			this.passwd = passwd;
			return this;
		}

		public Builder timeout(long ms) {
			this.timeout = ms;
			return this;
		}

		public Http create() {
			return Http.create(proxy, user, passwd, timeout);
		}

	}

}
