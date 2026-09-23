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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Temp;
import org.giiwa.cache.TimingCache;
import org.giiwa.conf.Global;
import org.giiwa.crypto.MD5;
import org.giiwa.dao.Comment;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.misc.Html;
import org.giiwa.misc.StringFinder;
import org.giiwa.misc.Url;
import org.giiwa.task.Console;
import org.giiwa.task.Consumer;
import org.giiwa.task.Task;
import org.giiwa.web.QueryString;
import org.jsoup.nodes.Element;

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
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.Route;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;

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

	/**
	 * 设置超时时间
	 * 
	 * @param timeout 毫秒
	 * @return
	 */
	public Http timeout(long timeout) {
		if (timeout <= 0) {
			timeout = X.AMINUTE;
		}
		builder.callTimeout(timeout, TimeUnit.MILLISECONDS);
		builder.connectTimeout(timeout, TimeUnit.MILLISECONDS);
		builder.writeTimeout(timeout, TimeUnit.MILLISECONDS);
		builder.readTimeout(timeout, TimeUnit.MILLISECONDS);

		client = builder.build();
		return this;
	}

	public Http protocol(Protocol protocol) {
		builder.protocols(java.util.Collections.singletonList(protocol));
		return this;
	}

	/**
	 * 设置代理
	 * 
	 * @param proxy  - ip:port
	 * @param user   - 代理账户
	 * @param passwd - 账户密码
	 * @return this
	 */
	public Http proxy(String proxy, String user, String passwd) {
		if (X.isEmpty(proxy)) {
			// clean proxy
			builder.proxy(null);
			client = builder.build();
			return this;
		} else {
			String[] ss = X.split(proxy, "[,;]");
			int i = X.toInt(ss.length * Math.random());
			if (i >= ss.length) {
				i = ss.length - 1;
			}
			String s = ss[i];
			ss = X.split(s, ":");
			Proxy p1 = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ss[0], X.toInt(ss[1])));

			builder.proxy(p1);
			if (!X.isEmpty(user)) {
				builder.proxyAuthenticator(new Authenticator() {

					@Override
					public Request authenticate(Route route, okhttp3.Response response) throws IOException {
						String credential = Credentials.basic(user, passwd);
						return response.request().newBuilder().header("Proxy-Authorization", credential).build();
					}

				});
			}
			client = builder.build();
			return this;
		}
	}

	/**
	 * 设置代理
	 * 
	 * @param proxy - ip:port
	 * @return this
	 */
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

			SSLContext sslContext = SSLContext.getInstance("TLS");
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

			});
			
			builder.sslSocketFactory(socketFactory, manager);

			if (!X.isEmpty(proxy)) {
				builder.proxy(proxy);
			}
//			ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
//					.cipherSuites("TLS_RSA_WITH_AES_256_CBC_SHA256").tlsVersions(TlsVersion.TLS_1_2).build();
//			builder.connectionSpecs(Arrays.asList(spec, ConnectionSpec.CLEARTEXT));
			builder.hostnameVerifier((hostname, session) -> true);

			builder.cookieJar(new CookieJar() {

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

			if (timeout <= 0) {
				timeout = X.AMINUTE;
			}
			if (timeout < 1000) {
				log.warn("参数错误, timeout=" + timeout, new Exception("参数错误，毫秒timeout=" + timeout));
			}

			builder.callTimeout(timeout, TimeUnit.MILLISECONDS);
			builder.connectTimeout(timeout, TimeUnit.MILLISECONDS);
			builder.writeTimeout(timeout, TimeUnit.MILLISECONDS);
			builder.readTimeout(timeout, TimeUnit.MILLISECONDS);

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
	 * 新建Http对象
	 * 
	 * @return 新Http
	 */
	public static Http create() {
		return create(Global.getString("http.proxy", null), null, null);
	}

	/**
	 * 新建对象，并使用代理
	 * 
	 * @param proxy - ip:port
	 * @return 新Http对象
	 */
	public static Http create(String proxy) {
		return create(proxy, null, null);
	}

	/**
	 * 使用代理新建Http对象
	 * 
	 * @param proxy  - ip:port
	 * @param user   - 代理账号
	 * @param passwd - 账号密码
	 * @return 新Http对象
	 */
	public static Http create(String proxy, String user, String passwd) {
		return create(proxy, user, passwd, X.AMINUTE);
	}

	/**
	 * 
	 * 使用代理新建Http对象
	 * 
	 * @param proxy   - ip:port
	 * @param user    - 代理账号
	 * @param passwd  - 账号密码
	 * @param timeout - 请求超时，毫秒
	 * @return 新Http对象
	 */
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

	/**
	 * 发送GET请求
	 * 
	 * @param url - 链接
	 * @return Response对象
	 */
	public Response get(String url) {
		return get(url, null);
	}

	public Response get(String url, JSON head, Consumer<String> func) {
		Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent");

		boolean hasAgent = false;

		if (head != null) {
			for (String name : head.keySet()) {
				if (X.isSame(name, "user-agent")) {
					hasAgent = true;
				}
				request.addHeader(name, head.getString(name));
			}
		}
		if (!hasAgent) {
			request.addHeader("User-Agent", _UA());
		}

		okhttp3.Response response = null;

		try {
			response = client.newCall(request.get().build()).execute();
			Response r = Response.create(response.code(), response.headers(), response);

			BufferedSource source = response.body().source();
			Task.schedule(t -> {
				try {
					// source.exhausted 有bug，3.16.4，死循环，高CPU
					String line = null;
					t.timeout = client.readTimeoutMillis();
					while ((line = source.readUtf8Line()) != null) {
						t.reset();
						log.info("http.get, url=" + url + ", line=" + line);
						func.accept(line);
					}
				} catch (Exception err) {
					// ignore
//					log.error(err.getMessage(), err);
				} finally {
					X.close(source);
				}
			});
			return r;
		} catch (Exception e) {
			// be care for: okhttp4.12有bug， close的时候
			log.error(url, e);
			X.close(response);
			return Response.create(e).url(url);
		}

	}

	/**
	 * 发送GET请求
	 * 
	 * @param url  - 链接
	 * @param head - 头信息
	 * @return Response对象
	 */
	public Response get(String url, JSON head) {

		Task curtask = Task.currentTask();
		long oldtimeout = -1;
		if (curtask != null) {
			oldtimeout = curtask.timeout;
			curtask.timeout = client.readTimeoutMillis();
		}

		boolean hasAgent = false;
		boolean hasConnection = false;

		okhttp3.Response response = null;

		TimeStamp t = TimeStamp.create();
		try {

			/**
			 * okhttp3 有bug， 这个地方可能出现高CPU，卡死， 不得不使用task.timeout 机制来防止卡死
			 */
			Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent");

			if (head != null) {
				for (String name : head.keySet()) {
					if (X.isSame(name, "user-agent")) {
						hasAgent = true;
					} else if (X.isSame(name, "connection")) {
						hasConnection = true;
					}
					request.addHeader(name, head.getString(name));
				}
			}
			if (!hasAgent) {
				request.addHeader("User-Agent", _UA());
			}
			if (!hasConnection) {
				request.addHeader("Connection", "close");
			}

			response = client.newCall(request.get().build()).execute();
			return Response.create(this, response).url(url);
		} catch (Exception e) {
			String s = TimingCache.get(String.class, UID.id(url));
			if (!X.isSame(s, url)) {
				log.error("url=" + url + ", cost=" + t.past(), e);
				TimingCache.set(String.class, UID.id(url), url, X.AMINUTE * 10);
			} else {
				log.error("url=" + url + ", error=" + e.getMessage());
			}
			return Response.create(e).url(url);
		} finally {
			X.close(response);
			if (curtask != null) {
				curtask.timeout = oldtimeout;
			}
		}
	}

	/**
	 * 发送DELETE请求
	 * 
	 * @param url - 链接
	 * @return Response对象
	 */
	public Response delete(String url) {
		return delete(url, null);
	}

	public Response delete(String url, JSON body) {

		Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent",
				_UA());

		String s = "application/json; charset=utf-8";
		MediaType _JSON = MediaType.parse(s);

		request.addHeader("Content-Type", s);
		request.addHeader("Connection", "close");
		if (body == null || body.isEmpty()) {
			request = request.delete();
		} else {
			RequestBody bb = RequestBody.create(body.toString(), _JSON);
			request = request.delete(bb);
		}

		okhttp3.Response response = null;
		try {
			response = client.newCall(request.build()).execute();
			return Response.create(this, response);
		} catch (Exception e) {
			return Response.create(e);
		} finally {
			X.close(response);
		}
	}

	/**
	 * 发送POST请求
	 * 
	 * @param url  - 链接
	 * @param body - 参数
	 * @return Response对象
	 */
	public Response post(String url, JSON body) {
		return form(url, body);
	}

	/**
	 * 发送POST请求
	 * 
	 * @param url  - 链接
	 * @param body - 参数
	 * @return Response对象
	 */
	public Response form(String url, JSON body) {

		Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent",
				_UA());

		if (keepalive) {
			request.addHeader("Connection", "keep-alive");
		} else {
			request.addHeader("Connection", "close");
		}

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
			return Response.create(this, response).url(url);
		} catch (Exception e) {
			log.error(url, e);
			return Response.create(e);
		} finally {
			X.close(response);
		}

	}

	/**
	 * 发送POST请求， 使用application/json
	 * 
	 * @param url - 请求链接
	 * @return
	 */
	public Response json(String url) {
		return json(url, null);
	}

	/**
	 * 发送POST请求， 使用application/json
	 * 
	 * @param url  - 链接
	 * @param body - 参数
	 * @return Response对象
	 */
	public Response json(String url, JSON body) {

		Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent",
				_UA());

		String s = "application/json; charset=utf-8";
		MediaType _JSON = MediaType.parse(s);

		request.addHeader("Content-Type", s);
		if (keepalive) {
			request.addHeader("Connection", "keep-alive");
		} else {
			request.addHeader("Connection", "close");
		}
		if (body != null && !body.isEmpty()) {
			RequestBody bb = RequestBody.create(body.toString(), _JSON);
			request.post(bb);
		} else {
			request.post(RequestBody.create(new byte[0]));
		}

		okhttp3.Response response = null;

		try {
			response = client.newCall(request.build()).execute();
			return Response.create(this, response).url(url);
		} catch (Exception e) {
			log.error(url, e);
			return Response.create(e);
		} finally {
			X.close(response);
		}

	}

	/**
	 * 发送POST请求，使用application/json
	 * 
	 * @param url  - 链接
	 * @param head - 头信息
	 * @param body - 参数
	 * @return Response对象
	 */
	public Response json(String url, JSON head, JSON body) {

		Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent");

		String s = "application/json; charset=utf-8";
		MediaType h = MediaType.parse(s);

		String bodystring = X.EMPTY;
		if (body != null && !body.isEmpty()) {
			bodystring = body.toString();
		}

		request.addHeader("Content-Type", s);
		boolean hasConnection = false;
		boolean useragent = false;
		if (head != null) {
			for (String name : head.keySet()) {
				if (X.isSame(name, "user-agent")) {
					useragent = true;
				} else if (X.isSame(name, "connection")) {
					hasConnection = true;
				}
				request.addHeader(name, head.getString(name));
			}
		}
		if (!useragent) {
			request.addHeader("User-Agent", _UA());
		}
		if (!hasConnection) {
			request.addHeader("Connection", "close");
		}

		RequestBody bb = RequestBody.create(bodystring, h);
		request.post(bb);

		okhttp3.Response response = null;

		try {
			response = client.newCall(request.build()).execute();
			return Response.create(this, response).url(url);
		} catch (Exception e) {
			log.error(url, e);
			return Response.create(e);
		} finally {
			X.close(response);
		}

	}

	/**
	 * 发送POST， 以application/json
	 * 
	 * @param url  - 链接
	 * @param head - 头信息
	 * @param body - 参数
	 * @return Response对象
	 */
	public Response jsons(String url, JSON head, List<JSON> body) {

		Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent",
				_UA());

		String s = "application/json; charset=utf-8";
		MediaType h = MediaType.parse(s);

		String bodystring = X.EMPTY;
		if (body != null && !body.isEmpty()) {
			bodystring = body.toString();
		}

		request.addHeader("Content-Type", s);
//		boolean hasConnection = false;
		boolean hasAgent = false;
		if (head != null) {
			for (String name : head.keySet()) {
				if (X.isSame(name, "user-agent")) {
					hasAgent = true;
//				} else if (X.isSame(name, "connection")) {
//					hasConnection = true;
				}
				request.addHeader(name, head.getString(name));
			}
		}
		if (keepalive) {
			request.addHeader("Connection", "keep-alive");
		} else {
			request.addHeader("Connection", "close");
		}
		if (!hasAgent) {
			request.addHeader("User-Agent", _UA());
		}

		RequestBody bb = RequestBody.create(bodystring, h);
		request.post(bb);

		okhttp3.Response response = null;

		try {
			response = client.newCall(request.build()).execute();
			return Response.create(this, response).url(url);
		} catch (Exception e) {
			log.error(url, e);
			return Response.create(e);
		} finally {
			X.close(response);
		}

	}

	/**
	 * 发送POST请求
	 * 
	 * @param url  - 链接
	 * @param head - 头信息
	 * @param body - 参数
	 * @return Response对象
	 */
	public Response post(String url, JSON head, JSON body) {
		return post(url, head, body, null, null, null);
	}

	/**
	 * 发送POST请求，并带附件
	 * 
	 * @param url      - 链接
	 * @param head     - 头信息
	 * @param body     - 参数
	 * @param field    - 附件字段名
	 * @param filename - 文件名
	 * @param in       - 附件流
	 * @return Response对象
	 */
	public Response post(String url, JSON head, JSON body, String field, String filename, InputStream in) {
		return post(url, head, body, field, filename, in, false);
	}

	/**
	 * 
	 * @param url
	 * @param contentype
	 * @param in         - 输入流
	 * @param timeout    - 分钟
	 * @param func       - 流式回调函数
	 * @return
	 */
	public Response post(String url, String contentype, InputStream in, int timeout, Consumer<String> func) {

		okhttp3.Response response = null;

		try {

			if (timeout > 60 * 24 * 7) {
				log.warn("参数错误, timeout=" + timeout, new Exception("参数错误, 分钟timeout=" + timeout));
				timeout /= X.AMINUTE;
			}
			if (timeout <= 0) {
				// 1 minutes
				timeout = 1;
			}

			OkHttpClient client = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
					.writeTimeout(timeout, TimeUnit.MINUTES).readTimeout(timeout, TimeUnit.MINUTES)
					.retryOnConnectionFailure(false).build();

			RequestBody requestBody = new RequestBody() {
				@Override
				public MediaType contentType() {
					return MediaType.get(contentype);
				}

				@Override
				public void writeTo(BufferedSink sink) throws IOException {
					try (Source source = Okio.source(in)) {
						sink.writeAll(source); // 流式写入
					}
				}
			};

			Request.Builder request = new Request.Builder().url(url).post(requestBody);

			if (keepalive) {
				request.addHeader("Connection", "keep-alive");
			} else {
				request.addHeader("Connection", "close");
			}

			response = client.newCall(request.build()).execute();
			Response r = Response.create(response.code(), response.headers(), response);

			Reader r1 = response.body().charStream();

			Task.schedule(t -> {
				BufferedReader re = new BufferedReader(r1);
				try {
					String line = re.readLine();
					while (line != null) {
						func.accept(line);
						line = re.readLine();
					}
				} catch (Exception err) {
					log.error(err.getMessage(), err);
				} finally {
					X.close(re);
				}
			});
			return r;
		} catch (Exception e) {
			log.error(url, e);
			X.close(response);
			return Response.create(e);
		} finally {
			X.close(in);
		}

	}

	/**
	 * 发送POST请求
	 * 
	 * @param url      - 链接
	 * @param head     - 头信息
	 * @param body     - 参数
	 * @param field    - 附件文件字段名
	 * @param filename - 文件名
	 * @param in       - 输入流
	 * @param resume   - True支持断点上传
	 * @return Response对象
	 */
	public Response post(String url, JSON head, JSON body, String field, String filename, InputStream in,
			boolean resume) {

		if (resume && in != null) {
			try {
				if (X.isEmpty(field)) {
					throw new Exception("field param is null!");
				}

				String time = Long.toString(Global.now());
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

				Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent");

				if (keepalive) {
					request.addHeader("Connection", "keep-alive");
				} else {
					request.addHeader("Connection", "close");
				}

				String contentype = null;
				boolean useragent = false;
				if (head != null) {
					for (String name : head.keySet()) {
						if (X.isSame(name, "content-type")) {
							contentype = head.getString(name);
						} else if (X.isSame(name, "user-agent")) {
							useragent = true;
						}
						request.addHeader(name, head.getString(name));
					}
				}
				if (!useragent) {
					request.addHeader("User-Agent", _UA());
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
				return Response.create(this, response).url(url);
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

			Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent");

			if (keepalive) {
				request.addHeader("Connection", "keep-alive");
			} else {
				request.addHeader("Connection", "close");
			}

//			boolean hasConnection = false;
			boolean useragent = false;
			if (head != null) {
				for (String name : head.keySet()) {
					if (X.isSame(name, "user-agent")) {
						useragent = true;
//					} else if (X.isSame(name, "connection")) {
//						hasConnection = true;
					}
					request.addHeader(name, head.getString(name));
				}
			}
			if (!useragent) {
				request.addHeader("User-Agent", _UA());
			}
//			if (!hasConnection) {
//				request.addHeader("Connection", "close");
//			}

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

			return Response.create(this, response).url(url);
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

	/**
	 * 发送POST请求， 如果头信息不包含Content-Type，则以application/json方式
	 * 
	 * @param url  - 链接
	 * @param head - 头信息
	 * @param body - 参数
	 * @return Response对象
	 */
	public Response post(String url, JSON head, String body) {

		Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent");

		boolean hasAgent = false;
//		boolean hasConnection = false;
		if (head != null) {
			for (String name : head.keySet()) {
				if (X.isSame(name, "user-agent")) {
					hasAgent = true;
//				} else if (X.isSame(name, "connection")) {
//					hasConnection = true;
				}
				request.addHeader(name, head.getString(name));
			}
		}
		if (keepalive) {
			request.addHeader("Connection", "keep-alive");
		} else {
			request.addHeader("Connection", "close");
		}
		if (!hasAgent) {
			request.addHeader("User-Agent", _UA());
		}

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
			return Response.create(this, response).url(url);
		} catch (Exception e) {
			log.error(url, e);
			return Response.create(e);
		} finally {
			X.close(response);
		}

	}

	/**
	 * 发送POST请求，以application/json方式
	 * 
	 * @param url  - 链接
	 * @param body - 参数
	 * @return Response对象
	 */
	public Response post(String url, String body) {
		return post(url, "application/json", body);
	}

	/**
	 * 清除所有临时Cookie
	 * 
	 * @return this
	 */
	public Http clear() {
		if (cookies != null) {
			cookies.clear();
		}
		return this;
	}

	/**
	 * 发送POST请求
	 * 
	 * @param url        - 链接
	 * @param contentype - Content-Type
	 * @param body       - 参数
	 * @return Response对象
	 */
	public Response post(String url, String contentype, String body) {

		Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent",
				_UA());

		if (keepalive) {
			request.addHeader("Connection", "keep-alive");
		} else {
			request.addHeader("Connection", "close");
		}

		MediaType CC = MediaType.parse(contentype);

		RequestBody bb = RequestBody.create(body, CC);

		request.post(bb);

		okhttp3.Response response = null;
		try {
			response = client.newCall(request.build()).execute();
			return Response.create(this, response).url(url);
		} catch (Exception e) {
			log.error(url, e);
			return Response.create(e);
		} finally {
			X.close(response);
		}

	}

	/**
	 * 发送PUT请求
	 * 
	 * @param url  - 链接
	 * @param body - 参数
	 * @return Response对象
	 */
	public Response put(String url, JSON body) {

		Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent",
				_UA());

		String s = "application/json; charset=utf-8";
		MediaType _JSON = MediaType.parse(s);

		if (keepalive) {
			request.addHeader("Connection", "keep-alive");
		} else {
			request.addHeader("Connection", "close");
		}

		if (body != null && !body.isEmpty()) {
			RequestBody bb = RequestBody.create(body.toString(), _JSON);
			request.put(bb);
		} else {
			request.put(RequestBody.create(new byte[0]));
		}

		okhttp3.Response response = null;
		try {
			response = client.newCall(request.build()).execute();
			return Response.create(this, response).url(url);
		} catch (Exception e) {
			log.error(url, e);
			return Response.create(e);
		} finally {
			X.close(response);
		}
	}

	/**
	 * 发送PUT请求
	 * 
	 * @param url  - 链接
	 * @param body - 参数
	 * @return Response对象
	 */
	public Response put(String url, String body) {

		Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent",
				_UA());
		if (keepalive) {
			request.addHeader("Connection", "keep-alive");
		} else {
			request.addHeader("Connection", "close");
		}

		if (!X.isEmpty(body)) {
			MediaType CC = MediaType.parse("application/json");
			RequestBody bb = RequestBody.create(body, CC);
			request.put(bb);
		}

		okhttp3.Response response = null;
		try {
			response = client.newCall(request.build()).execute();
			return Response.create(this, response).url(url);
		} catch (Exception e) {
			log.error(url, e);
			return Response.create(e);
		} finally {
			X.close(response);
		}
	}

	/**
	 * 发送HEAD请求
	 * 
	 * @param url - 链接
	 * @return Response对象
	 */
	public Response head(String url) {

		Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent",
				_UA());
		if (keepalive) {
			request.addHeader("Connection", "keep-alive");
		} else {
			request.addHeader("Connection", "close");
		}
		request.head();

		okhttp3.Response response = null;
		try {
			response = client.newCall(request.build()).execute();
			return Response.create(this, response).url(url);
		} catch (Exception e) {
			return Response.create(e);
		} finally {
			X.close(response);
		}
	}

	/**
	 * 获取当前会话Cookie
	 * 
	 * @return 字符串，以“;”分隔
	 */
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
	 * 下载文件
	 *
	 * @param url - 链接
	 * @param out - 输出流
	 * @return 下载文件长度
	 */
	public long download(String url, OutputStream out) {
		return download(url, null, null, out);
	}

	/**
	 * 下载文件
	 * 
	 * @param url  - 链接
	 * @param head - 头信息
	 * @param out  - 输出流
	 * @return 下载文件长度
	 */
	public long download(String url, JSON head, OutputStream out) {
		return download(url, head, null, out);
	}

	/**
	 * 下载文件
	 * 
	 * @param url  - 链接
	 * @param head - 头信息
	 * @param body - 参数
	 * @param out  - 输出流
	 * @return 下载文件长度
	 */
	public long download(String url, JSON head, JSON body, OutputStream out) {

//		try {
		if (log.isDebugEnabled()) {
			log.debug("url=\"" + url + "\"");
		}

		Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent");

		boolean hasAgent = false;
//		boolean hasConnection = false;
		if (head != null && !head.isEmpty()) {
			for (String name : head.keySet()) {
				if (X.isSame(name, "user-agent")) {
					hasAgent = true;
//				} else if (X.isSame(name, "connection")) {
//					hasConnection = true;
				}
				request.addHeader(name, head.getString(name));
			}
		}
		if (keepalive) {
			request.addHeader("Connection", "keep-alive");
		} else {
			request.addHeader("Connection", "close");
		}
		if (!hasAgent) {
			request.addHeader("User-Agent", _UA());
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

	/**
	 * 下载文件到临时文件中
	 * 
	 * @param url    - 链接
	 * @param resume - True支持断点续传
	 * @return 临时文件对象
	 * @throws IOException 
	 */
	public Temp download(String url, boolean resume) throws IOException {
		return download(url, (JSON) null, (JSON) null, resume);
	}

	/**
	 * 下载文件
	 * 
	 * @param url - 链接
	 * @return 临时文件对象
	 * @throws IOException 
	 */
	public Temp download(String url) throws IOException {
		return download(url, (JSON) null, (JSON) null, false);
	}

	/**
	 * 下载文件
	 * 
	 * @param url    - 链接
	 * @param head   - 头信息
	 * @param resume - True断点续传
	 * @return 临时文件对象
	 * @throws IOException 
	 */
	public Temp download(String url, JSON head, boolean resume) throws IOException {
		return download(url, head, (JSON) null, resume);
	}

	/**
	 * 下载文件
	 * 
	 * @param url  - 链接
	 * @param head - 头信息
	 * @return 临时文件对象
	 * @throws IOException 
	 */
	public Temp download(String url, JSON head) throws IOException {
		return download(url, head, (JSON) null, false);
	}

	/**
	 * 下载文件
	 * 
	 * @param url  - 链接
	 * @param head - 头信息
	 * @param body - 参数
	 * @return 临时文件对象
	 * @throws IOException 
	 */
	public Temp download(String url, JSON head, JSON body) throws IOException {
		return download(url, head, body, false);
	}

	/**
	 * 下载文件
	 * 
	 * @param url    - 链接
	 * @param head   - 头信息
	 * @param body   - 参数
	 * @param resume - True支持断点续传
	 * @return 临时文件对象
	 * @throws IOException 
	 */
	public Temp download(String url, JSON head, JSON body, boolean resume) throws IOException {
		return _download(url, head, body, resume, null);
	}

	private Temp _download(String url, JSON head, JSON body, boolean resume, Temp t) throws IOException {

		if (log.isDebugEnabled()) {
			log.debug("url=\"" + url + "\"");
		}

		if (resume) {
			// 断点续传
			Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent");

			boolean hasAgent = false;
//			boolean hasConnection = false;

			if (head != null && !head.isEmpty()) {
				for (String name : head.keySet()) {
					if (X.isSame(name, "user-agent")) {
						hasAgent = true;
//					} else if (X.isSame(name, "connection")) {
//						hasConnection = true;
					}
					request.addHeader(name, head.getString(name));
				}
			}

			if (!hasAgent) {
				request.addHeader("User-Agent", _UA());
			}
			if (keepalive) {
				request.addHeader("Connection", "keep-alive");
			} else {
				request.addHeader("Connection", "close");
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

					// 从链接中获取文件名
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
					// 从 content-disposition 中获取文件名
					int i = s.indexOf("=");
					if (i >= 0) {
						// filename=....
						// filename=UTF8''.....
						filename = s.substring(i + 1).trim();
						i = filename.lastIndexOf("'");
						filename = filename.substring(i + 1);
					}
				}
				filename = filename.replaceAll("\"", X.EMPTY);

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
			// 不支持断点续传
			okhttp3.Response response = null;

			try {
				Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent")
						.addHeader("User-Agent", _UA());

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

				response = client.newCall(request.build()).execute();

//			System.out.println(response.headers());

				String filename = "a";

				String s = response.header("content-disposition");
				if (X.isEmpty(s)) {
					// 从链接中获取文件名
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
					// 从 content-disposition 中获取文件名
					int i = s.indexOf("=");
					if (i >= 0) {
						// filename=....
						// filename=UTF8''.....
						filename = s.substring(i + 1).trim();
						i = filename.lastIndexOf("'");
						filename = filename.substring(i + 1);
					}
				}
				filename = filename.replaceAll("\"", X.EMPTY);

				// 把特殊编码转成中文
				int n = 0;
				while (filename.startsWith("%") && n < 10) {
					filename = Url.decode(filename);
					n++;
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
//			} catch (Exception e) {
//				log.error(url, e);
//				throw e;
			} finally {
				X.close(response);
			}
		}

		return null;
	}

	private boolean _resume_download(Temp t, OutputStream out, String url, JSON head, JSON body, long total)
			throws IOException {

//		System.out.println("downloading: " + t.length());

		Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent");

		boolean hasAgent = false;
		boolean hasConnection = false;

		if (head != null && !head.isEmpty()) {
			for (String name : head.keySet()) {
				if (X.isSame(name, "user-agent")) {
					hasAgent = true;
				} else if (X.isSame(name, "connection")) {
					hasConnection = true;
				}
				request.addHeader(name, head.getString(name));
			}
		}
		if (!hasAgent) {
			request.addHeader("User-Agent", _UA());
		}
		if (!hasConnection) {
			request.addHeader("Connection", "close");
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

	/**
	 * 设置临时DNS
	 * 
	 * @param host - 域名
	 * @param ip   - IP地址
	 */
	@Comment(text = "设置临时DNS", demo = "http.dns('www.giisoo.com', '192.168.0.1')")
	public void dns(@Comment(text = "host") String host, @Comment(text = X.IP) String... ip) {
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

	boolean keepalive = false;

	public Http keepalive(boolean keepalive) {
		this.keepalive = keepalive;
		return this;
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

		public Response charset(String charset) throws UnsupportedEncodingException {
			if (_body != null) {
				body = new String(_body, charset);
			}
			return this;
		}

		/**
		 * 返回头部信息
		 */
		private Headers headers;
		private okhttp3.Response response;
		private Http http;

		/**
		 * 下载资源文件
		 * 
		 * @param select   - 节点选择, 比如: img
		 * @param attrname - 节点属性名称, 比如: src
		 * @return 下载好的临时文件列表
		 * @throws IOException
		 */
		@SuppressWarnings("deprecation")
		@Comment(text = "下载")
		public List<Temp> download(String select, String attrname) throws IOException {
			Html h1 = this.html();
			List<Element> l1 = h1.find(select);
			if (l1 != null && !l1.isEmpty()) {
				List<Temp> l2 = new ArrayList<Temp>();
				for (Element e : l1) {
					String url = e.attr(attrname);
					if (!X.isEmpty(url)) {
						url = h1.format(url);
						Temp t1 = http.download(url);
						if (t1 != null && t1.length() > 0) {
							l2.add(t1);
						}
					}
				}
				return l2;
			}
			return null;
		}

		public List<Element> find(String select) {
			return find(select, null);
		}

		/**
		 * 
		 * @param select
		 * @param regex  - href=.*\/wiki\/.*
		 * @return
		 */
		@SuppressWarnings("deprecation")
		public List<Element> find(String select, String regex) {
			Html h1 = this.html();
			List<Element> l1 = h1.find(select);
			if (!X.isEmpty(regex)) {
				int j = regex.indexOf("=");
				if (j > 0) {
					String name = regex.substring(0, j).trim();
					String match = regex.substring(j + 1).trim();
					for (int i = l1.size() - 1; i >= 0; i--) {
						Element e = l1.get(i);
						String att = e.attr(name);
						if (att == null || !att.matches(match)) {
							l1.remove(i);
						}
					}
				}
			}
			return l1;
		}

		public List<JSON> a() throws Exception {
			return a(null);
		}

		public List<JSON> a(String regex) throws Exception {
			Html h1 = this.html();
			return h1.a(regex);
		}

		public static Response create(Http h, okhttp3.Response res) {
			Response e = new Response();

			try {
				e.http = h;
				e.status = res.code();
				e.headers = res.headers();
				e._body = res.body().bytes();
				e.body = new String(e._body, e.charset());
			} catch (Exception e1) {
				log.error(e1.getMessage(), e1);
			}
			return e;
		}

		private static Charset UTF_8 = Charset.forName(X.UTF8);

		private Charset charset() {
			MediaType s = contentType();
			if (s == null) {
				return UTF_8;
			}
			return s.charset(UTF_8);
		}

		private MediaType contentType() {
			String s = headers.get("content-type");
			if (s == null) {
				return null;
			}
			return MediaType.parse(s);

		}

		/**
		 * 创建 Http响应对象
		 * 
		 * @param status - 状态码
		 * @param head   - 响应头
		 * @param body   - 内容
		 * @return
		 */
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

		public static Response create(int status, Headers headers, okhttp3.Response r) {
			Response e = new Response();

			try {
				e.status = status;
				e.headers = headers;
				e.response = r;
			} catch (Exception e1) {
				log.error(e1.getMessage(), e1);
			}
			return e;
		}

		/**
		 * 创建Http响应对象
		 * 
		 * @param err - 错误栈
		 * @return
		 */
		public static Response create(Exception err) {
			Response e = new Response();

			e.status = 500;
			e.body = err.getMessage();
//			log.error(err.getMessage(), err);
			return e;
		}

		/**
		 * 设置 响应对象的 url
		 * 
		 * @param url - 原始请求链接
		 * @return
		 */
		public Response url(String url) {
			this.url = url;
			return this;
		}

		/**
		 * 用body 生成StringFinder工具类
		 * 
		 * @return StringFinder工具类
		 */
		@Comment(text = "StringFinder对象")
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
		@Comment(text = "HTML对象")
		public Html html() {
			if (_html == null) {
				_html = Html.create(body);
				_html.url = url;
			}
			return _html;
		}

		Html _html;

		@Comment(text = "纯文本")
		public String text() {
			Html m = html();
			return m.text();
		}

		@Comment(text = "返回文本")
		public String body() {
			return body;
		}

		@Comment(text = "带基本样式纯文本")
		public String text2() {
			Html m = html();
			return m.text2();
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
			try {
				return JSON.fromObject(body);
			} catch (Exception err) {
				log.error(err.getMessage(), err);
			}
			return null;
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
			try {
				return JSON.fromObjects(body);
			} catch (Exception err) {
				log.error(err.getMessage(), err);
			}
			return null;
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

		public void close() {
			if (response != null) {
				X.close(response);
			}
			response = null;
		}

		@Comment(text = "获取扩展名")
		public String ext(@Comment(text = "url") String url) {
			int i = url.lastIndexOf("/");
			if (i > -1) {
				url = url.substring(i + 1);
			}
			i = url.lastIndexOf(".");
			if (i > -1) {
				return url.substring(i + 1).toLowerCase();
			}
			return null;
		}

	}

	/**
	 * 设置临时Cookie
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
	 * 设置临时Cookie
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
	 * 删除临时Cookie.
	 *
	 * @param name - the name
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

	private static final String[] TOP = { "top", "cn", "com", "net", "love", "org", "biz", "info", X.NAME, "tv", "me",
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

	/**
	 * 设置支持传发链接
	 * 
	 * @param b - True支持转发链接
	 */
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

	/**
	 * 发送PUT请求
	 * 
	 * @param url  - 链接
	 * @param head - 头信息
	 * @param file - 本地文件
	 */
	public Http.Response put(String url, JSON head, File file) {

		okhttp3.Response response = null;

		try {

			Request.Builder request = new Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent",
					_UA());

			if (keepalive) {
				request.addHeader("Connection", "keep-alive");
			} else {
				request.addHeader("Connection", "close");
			}

			if (head != null) {
				for (String name : head.keySet()) {
					request.addHeader(name, head.getString(name));
				}
			}

			RequestBody body = RequestBody.create(file, MediaType.parse("application/octet-stream"));
			request.put(body).build();

			response = client.newCall(request.build()).execute();
			log.info("put, url=" + url + ", head=" + head + ", response=" + response);

			return Response.create(this, response);
		} catch (Exception e) {
			log.error(url, e);
			return Response.create(500, null, e.getMessage());
		} finally {
			X.close(response);
		}

	}

}
