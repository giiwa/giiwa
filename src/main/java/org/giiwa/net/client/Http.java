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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLContext;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContexts;
import org.giiwa.bean.Temp;
import org.giiwa.conf.Global;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.misc.Html;
import org.giiwa.misc.IOUtil;
import org.giiwa.misc.StringFinder;
import org.giiwa.task.Consumer;
import org.giiwa.web.QueryString;
import org.jsoup.nodes.Element;

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

	private static boolean DEBUG = false;

	private BasicCookieStore cookies = new BasicCookieStore();
	private SSLContext ctx = null;

	public String proxy = null;
	public String user = null;
	public String passwd = null;

	private CloseableHttpClient client = null;
	private HttpClientContext localContext = null;

	private static String _UA() {
		int i = (int) (UA.length * Math.random());
		return UA[i].replace("{n1}", UID.digital(8)).replace("{n2}", UID.digital(3))
				.replace("{n3}", UID.digital(2) + "." + UID.digital(1) + "." + UID.digital(3))
				.replace("{n4}", UID.digital(10));
	}

	/**
	 * create a default Http client
	 * 
	 * @return the http
	 */
	public static Http create() {
		return create(Global.getString("http.proxy", ""));
	}

	/**
	 * create a http with the proxy
	 * 
	 * @return the http
	 */
	public static Http create(String proxy) {
		Http p = new Http();
		p.proxy = proxy;
		return p;
	}

	public static Http create(HttpClientBuilder builder) {
		Http p = new Http();
		p.client = builder.build();
		return p;
	}

	/**
	 * Gets the.
	 *
	 * @param url the url
	 * @return the response
	 */
	public Response get(String url) {
		return get(url, X.AMINUTE);
	}

	/**
	 * GET response from a url.
	 *
	 * @param url     the url
	 * @param charset the charset
	 * @param timeout the timeout
	 * @return Response
	 */
	public Response get(String url, long timeout) {
		return get(url, null, timeout);
	}

	/**
	 * ping the url, throw exception if occur error.
	 *
	 * @param url the url
	 * @return int of response status
	 * @throws Exception throw exception when failed
	 */
	public int ping(String url) throws Exception {

		URL u = new URL(url);
		HttpURLConnection c = (HttpURLConnection) u.openConnection();
		c.connect();
		int code = c.getResponseCode();
		if (log.isDebugEnabled())
			log.debug("ping=" + url + ", response.code=" + code);
		c.disconnect();
		return code;
	}

	/**
	 * close the client and release any resource associated with this client
	 */
	public void close() {
		if (client != null) {
			try {
				client.close();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * Post.
	 *
	 * @param url    the url
	 * @param params the params
	 * @return the response
	 */
	public Response post(String url, JSON params) {
		return post(url, params, X.AMINUTE);
	}

	public Response post(String url, Object head, Object params) {
		if (params instanceof String) {
			return post(url, head, params.toString());
		}
		return post(url, JSON.fromObject(head), JSON.fromObject(params), null, (InputStream) null, X.AMINUTE);
	}

	/**
	 * post body string
	 * 
	 * @param url
	 * @param body
	 * @return
	 */
	public Response post(String url, String body) {
		return post(url, null, body);
	}

	public Response post(String url, Object head, String body) {

		if (log.isDebugEnabled())
			log.debug("url=" + url);

		Response r = new Response();

		client = _client(url, null, X.AMINUTE);

		if (localContext == null) {
			localContext = HttpClientContext.create();
			localContext.setCookieStore(cookies);
		}

		if (client != null) {
			TimeStamp t = TimeStamp.create();

			HttpPost post = new HttpPost(url);
			CloseableHttpResponse resp = null;
			try {

				if (head != null) {
					JSON h1 = JSON.fromObject(head);
					for (String name : h1.keySet()) {
						post.addHeader(name, h1.getString(name));
					}
				} else {
					post.addHeader("Content-Type", "application/json");
				}

				if (log.isDebugEnabled())
					log.debug("post url=" + url);

				StringEntity e = new StringEntity(body, "UTF8");
				post.setEntity(e);

				resp = client.execute(post, localContext);
				r.status = resp.getStatusLine().getStatusCode();
				r.headers = resp.getAllHeaders();
				_parse(resp, r);

				if (log.isDebugEnabled())
					log.debug("post: cost=" + t.past() + ", status=" + r.status + ", body=" + r.body);

			} catch (Throwable e) {
				log.error("cost=" + t.past() + ", " + url, e);
				r.status = 600;
				r.body = "error: " + e.getMessage();
			} finally {
				if (resp != null)
					try {
						resp.close();
					} catch (IOException e) {
					}

			}

		} else {
			r.status = 600;
			r.body = "error: can not init a client";
		}

		return r;
	}

	/**
	 * put body string
	 * 
	 * @param url
	 * @param body
	 * @return
	 */
	public Response put(String url, String body) {
		return put(url, JSON.create().append("Content-Type", "text/plain"), body, X.AMINUTE);
	}

	/**
	 * put the params map
	 * 
	 * @param url
	 * @param params
	 * @return
	 */
	public Response put(String url, JSON params) {
		return put(url, params, X.AMINUTE);
	}

	public Response delete(String url, JSON params) {
		return delete(url, params, X.AMINUTE);
	}

	public Response head(String url, JSON params) {
		return head(url, params, X.AMINUTE);
	}

	/**
	 * POST response from a url.
	 *
	 * @param url     the url
	 * @param params  the params
	 * @param timeout the timeout
	 * @return Response
	 */
	public Response post(String url, JSON params, long timeout) {
		return post(url, null, params, timeout);
	}

	public Response put(String url, JSON params, long timeout) {
		return put(url, null, params, timeout);
	}

	public Response put(String url, JSON headers, JSON params, long timeout) {
		return put(url, "application/x-javascript; charset=UTF8", headers, params, timeout);
	}

	public Response delete(String url, JSON headers, long timeout) {
		return delete(url, "application/x-javascript; charset=UTF8", headers, timeout);
	}

	public Response head(String url, JSON headers, long timeout) {
		return head(url, "application/x-javascript; charset=UTF8", headers, timeout);
	}

	public Response get(String url, Object head) {
		return get(url, JSON.fromObject(head), X.AMINUTE);
	}

	/**
	 * Gets the.
	 * 
	 * @param url     the url
	 * @param headers the headers
	 * @return the response
	 */
	public Response get(String url, JSON head) {
		return get(url, head, X.AMINUTE);
	}

	public String cookie() {
		return X.join(X.asList(cookies.getCookies(), e -> {
			return ((Cookie) e).getName() + "=" + ((Cookie) e).getValue();
		}), ";");
	}

	/**
	 * GET method.
	 *
	 * @param url     the url
	 * @param charset the charset
	 * @param headers the headers
	 * @param timeout the timeout
	 * @return Response
	 */
	public Response get(String url, JSON head, long timeout) {

		TimeStamp t = TimeStamp.create();

		try {
			if (log.isDebugEnabled())
				log.debug("url=\"" + url + "\", head=" + head);

			String[] ss = url.split(" ");
			url = ss[ss.length - 1];

			client = _client(url, head, timeout);

			if (localContext == null) {
				localContext = HttpClientContext.create();
				localContext.setCookieStore(cookies);
			}

			if (client != null) {

				if (log.isDebugEnabled())
					log.debug("proxy = " + proxy);

				HttpGet get = null;

				try {
					get = new HttpGet(url);

					if (head != null && head.size() > 0) {
						for (String s : head.keySet()) {
							if (!X.isSame(s, "user-agent")) {
								get.addHeader(s, head.getString(s));
							}
						}
					}

					if (log.isDebugEnabled())
						log.debug("get url=" + url);

					Http.Response r = _get(client, localContext, get, url, timeout - t.pastms());
					return r;

				} catch (Throwable e) {
					log.error("\"" + url + "\"", e);

					Response r = new Response();
					r.status = 500;
					r.body = url + "\r\n" + X.toString(e);
					r.url = url;
					// e.printStackTrace();
					return r;
				}
			}
		} finally {
			if (log.isInfoEnabled())
				log.info("get, cost=" + t.past() + ", url=" + url);
		}
		return null;

	}

	private Response _get(CloseableHttpClient client, HttpClientContext context, HttpGet get, String url, long timeout)
			throws IOException {

		TimeStamp t = TimeStamp.create();

		Response r = new Response();

		CloseableHttpResponse resp = null;

		try {

			resp = client.execute(get, context);

			List<URI> l1 = context.getRedirectLocations();
			if (l1 != null && l1.size() > 0) {
				r.url = l1.get(l1.size() - 1).toASCIIString();
			} else {
				r.url = url;
			}
			r.status = resp.getStatusLine().getStatusCode();
			r.headers = resp.getAllHeaders();
			_parse(resp, r);

			if (timeout > t.pastms()) {
				r = _redirecting(r, client, context, get, timeout - t.pastms());
			}
		} catch (Exception e) {
			throw new IOException(e);
		} finally {
			X.close(resp);
		}

		return r;
	}

	private Response _redirecting(Response r, CloseableHttpClient client, HttpClientContext context, HttpGet get,
			long timeout) throws IOException {

		if (!X.isEmpty(r.body) && r.body.toLowerCase().contains("http-equiv=\"refresh\"")) {
			Html h = Html.create(r.body);

			// log.debug("body=" + r.body);
			List<Element> l1 = h.select("meta[http-equiv=refresh]");
			if (l1 != null && !l1.isEmpty()) {

				String url = l1.get(0).attr("content");

				StringFinder sf = StringFinder.create(url);
				if (sf.find("url") > -1) {
					sf.nextTo("=");
					sf.skip(1);
					url = sf.nextTo(";").trim();

					if (url.startsWith("'") || url.startsWith("\"")) {
						url = url.substring(1);
					}
					if (url.endsWith("'") || url.endsWith("\"")) {
						url = url.substring(0, url.length() - 1);
					}

					url = format(r.url, url);

					if (log.isDebugEnabled())
						log.debug("redirecting, url=" + url);

					HttpGet get1 = new HttpGet(url);

					Header[] headers = get.getAllHeaders();
					if (headers != null && headers.length > 0) {
						for (Header h1 : headers) {
							get1.addHeader(h1);
						}
					}

					r = _get(client, context, get1, url, timeout);
					return r;
				}
			}
		}
		return r;
	}

	/**
	 * download the file in the url to f.
	 *
	 * @param url       the file url
	 * @param localfile the destination file
	 * @return the length
	 */
	public long download(String url, OutputStream out) {
		return download(url, (JSON) null, out);
	}

	/**
	 * download the remote url to local file with the header.
	 *
	 * @param url       the remote resource url
	 * @param header    the header
	 * @param localfile the localfile
	 * @return the length of bytes
	 */
	public long download(String url, JSON head, OutputStream out) {

		TimeStamp t = TimeStamp.create();

		try {
			if (log.isDebugEnabled())
				log.debug("url=\"" + url + "\"");

			String[] ss = url.split(" ");
			url = ss[ss.length - 1];

			client = _client(url, head, X.AMINUTE);
			if (localContext == null) {
				localContext = HttpClientContext.create();
				localContext.setCookieStore(cookies);
			}
			if (client != null) {
				HttpGet get = null;
				CloseableHttpResponse resp = null;
				try {
					get = new HttpGet(url);

					if (head != null && head.size() > 0) {
						for (String name : head.keySet()) {
							get.addHeader(name, head.getString(name));
						}
					}

					if (log.isDebugEnabled())
						log.debug("get url=" + url);

					resp = client.execute(get, localContext);
					{
						List<URI> l1 = localContext.getRedirectLocations();
						if (l1 != null && l1.size() > 0) {
							url = l1.get(l1.size() - 1).toASCIIString();
						}
					}

					int status = resp.getStatusLine().getStatusCode();

					if (DEBUG) {
						Header[] hh = resp.getAllHeaders();
						if (hh != null) {
							for (Header h : hh) {
								if (log.isDebugEnabled())
									log.debug(h.getName() + ":" + h.getValue());
							}
						}
						if (log.isDebugEnabled())
							log.debug("status:" + status);
					}

					if (status == 200 || status == 206) {
						HttpEntity e = resp.getEntity();

						InputStream in = e.getContent();

						long l2 = IOUtil.copy(in, out);

						Header[] hh = resp.getHeaders("Content-Length");
						if (hh != null && hh.length > 0) {
							int l1 = X.toInt(hh[0].getValue());
							if (l1 != l2) {
								// size error
								log.error("download size error, expect=" + l1 + ", actual=" + l2, new Exception(url));
								// bad file size
								return 0;
							}
						}
						return l2;
					}
					return 0;
				} catch (Exception e) {
					log.error("\"" + url + "\"", e);
//					e.printStackTrace();
				} finally {
					if (resp != null)
						try {
							resp.close();
						} catch (IOException e) {
						}

				}
			}
		} finally {
			if (log.isInfoEnabled())
				log.info("download, cost=" + t.past() + ", url=" + url);
		}

		return 0;
	}

	/**
	 * using post to download
	 * 
	 * @param url
	 * @param head
	 * @param body
	 * @param file
	 * @return
	 */
	public long post_download(String url, JSON head, JSON body, OutputStream out) {

		if (log.isDebugEnabled())
			log.debug("url=" + url);

		Response r = new Response();

		client = _client(url, head, X.AMINUTE);

		if (localContext == null) {
			localContext = HttpClientContext.create();
			localContext.setCookieStore(cookies);
		}

		if (client != null) {
			TimeStamp t = TimeStamp.create();

			HttpPost post = new HttpPost(url);
			CloseableHttpResponse resp = null;
			try {

				if (head != null && head.size() > 0) {
					if (log.isDebugEnabled())
						log.debug("head: " + head);
					for (String s : head.keySet()) {
						post.addHeader(s, head.getString(s));
					}
				}

				if (log.isDebugEnabled())
					log.debug("post url=" + url);

				MultipartEntityBuilder b = MultipartEntityBuilder.create();
				if (body != null) {
					if (log.isDebugEnabled())
						log.debug("body: " + body);

					for (String s : body.keySet()) {
						b.addTextBody(s, new String(body.getString(s).getBytes("UTF-8"), "ISO-8859-1"));
					}

				}
				post.setEntity(b.build());

				resp = client.execute(post, localContext);
				{
					List<URI> l1 = localContext.getRedirectLocations();
					if (l1 != null && l1.size() > 0) {
						r.url = l1.get(l1.size() - 1).toASCIIString();
					} else {
						r.url = url;
					}
				}

				int status = resp.getStatusLine().getStatusCode();

				if (status == 200 || status == 206) {
					HttpEntity e = resp.getEntity();

					InputStream in = e.getContent();

					long l2 = IOUtil.copy(in, out);

					Header[] hh = resp.getHeaders("Content-Length");
					if (hh != null && hh.length > 0) {
						int l1 = X.toInt(hh[0].getValue());
						if (l1 != l2) {
							// size error
							log.error("download size error, expect=" + l1 + ", actual=" + l2, new Exception(url));
							// bad file size
							return 0;
						}
					}
					return l2;
				}
				return 0;

			} catch (Throwable e) {
				log.error("cost=" + t.past() + ", " + url, e);
				r.status = 600;
				r.body = "error: " + e.getMessage();
			} finally {
				if (resp != null)
					try {
						resp.close();
					} catch (IOException e) {
					}

			}

		}

		return 0;
	}

	/**
	 * POST method.
	 *
	 * @param url     the url
	 * @param head    the headers
	 * @param params  the params
	 * @param timeout the timeout
	 * @return Response
	 */
	public Response post(String url, JSON head, JSON params, long timeout) {
		return post(url, head, params, null, (InputStream) null, timeout);
	}

	public Response open(String url, Consumer<HttpURLConnection> func) {

		Response r = new Response();

		BufferedReader re = null;

		try {
			URL u = new URL(url);

			HttpURLConnection conn = (HttpURLConnection) u.openConnection();
			conn.setDoOutput(true);
			conn.setDoInput(true);
//			conn.setRequestMethod("POST");

			func.accept(conn);

			r.status = conn.getResponseCode();

			List<Header> l1 = new ArrayList<Header>();
			Map<String, List<String>> m1 = conn.getHeaderFields();
			for (String name : m1.keySet()) {
				if (!X.isEmpty(name)) {
					List<String> l2 = m1.get(name);
					l1.add(new BasicHeader(name, l2.get(0)));
				}
			}
			r.headers = l1.toArray(new Header[l1.size()]);

			re = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = re.readLine()) != null) {
				sb.append(line).append("\r\n");
			}
			r.body = sb.toString();

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			r.status = 500;
			r.body = "ERR: " + e.getMessage();
		} finally {
			X.close(re);
		}

		return r;

	}

	/**
	 * POST stream data to server
	 * 
	 * @param url     the url
	 * @param headers the headers
	 * @param body    the body
	 * @param in      the in
	 * @param timeout the timeout
	 * @return Response
	 */
	public Response post(String url, JSON head, JSON body, String name, InputStream in, long timeout) {

		TimeStamp t = TimeStamp.create();

		Response r = new Response();

		try {
			if (log.isDebugEnabled())
				log.debug("url=" + url + ", head=" + head);

			_client(url, head, timeout);

			if (localContext == null) {
				localContext = HttpClientContext.create();
				localContext.setCookieStore(cookies);
			}

			if (client != null) {

				HttpPost post = new HttpPost(url);
				CloseableHttpResponse resp = null;
				try {

					if (head != null && head.size() > 0) {
						if (log.isDebugEnabled())
							log.debug("head: " + head);
						for (String s : head.keySet()) {
							if (!X.isSame(s, "user-agent")) {
								post.addHeader(s, head.getString(s));
							}
						}
					}

					if (log.isDebugEnabled())
						log.debug("post url=" + url);

					if (body != null || in != null) {
						MultipartEntityBuilder b = MultipartEntityBuilder.create();
						if (body != null) {
							if (log.isDebugEnabled())
								log.debug("body: " + body);

							for (String s : body.keySet()) {
//						b.addTextBody(name, body.getString(s),
//								ContentType.create("text/plain", headers.getString("ContentType", "UTF-8")));
								b.addTextBody(s, new String(body.getString(s).getBytes("UTF-8"), "ISO-8859-1"));
							}

						}
						if (in != null) {
							b.addBinaryBody(name, in, ContentType.DEFAULT_BINARY,
									head == null ? null : head.getString("filename"));
						}
						post.setEntity(b.build());
					}

					resp = client.execute(post, localContext);
					List<URI> l1 = localContext.getRedirectLocations();
					if (l1 != null && l1.size() > 0) {
						r.url = l1.get(l1.size() - 1).toASCIIString();
					} else {
						r.url = url;
					}

					r.status = resp.getStatusLine().getStatusCode();

					r.headers = resp.getAllHeaders();
					_parse(resp, r);

					if (log.isDebugEnabled())
						log.debug("got: cost=" + t.past() + ", status=" + r.status + ", body=" + r.body);

				} catch (Throwable e) {
					log.error("cost=" + t.past() + ", " + url, e);
					r.status = 600;
					r.body = "error: " + e.getMessage();
				} finally {
					if (resp != null)
						try {
							resp.close();
						} catch (IOException e) {
						}
				}

			} else {
				r.status = 600;
				r.body = "error: can not init a client";
			}

		} finally {
			if (log.isInfoEnabled())
				log.info("post, cost=" + t.past() + ", url=" + url);
		}
		return r;
	}

	public Response post(String url, JSON head, JSON body, String name, List<File> files, long timeout) {

		TimeStamp t = TimeStamp.create();

		Response r = new Response();

		try {
			if (log.isDebugEnabled())
				log.debug("url=" + url);

			_client(url, head, timeout);

			if (localContext == null) {
				localContext = HttpClientContext.create();
				localContext.setCookieStore(cookies);
			}

			if (client != null) {

				HttpPost post = new HttpPost(url);
				CloseableHttpResponse resp = null;
				try {

					if (head != null && head.size() > 0) {
						if (log.isDebugEnabled())
							log.debug("header: " + head);
						for (String s : head.keySet()) {
							post.addHeader(s, head.getString(s));
						}
					}

					if (log.isDebugEnabled())
						log.debug("post url=" + url);

					MultipartEntityBuilder b = MultipartEntityBuilder.create();
					if (body != null) {
						if (log.isDebugEnabled())
							log.debug("body: " + body);

						for (String s : body.keySet()) {
//						b.addTextBody(name, body.getString(s),
//								ContentType.create("text/plain", headers.getString("ContentType", "UTF-8")));
							b.addTextBody(s, new String(body.getString(s).getBytes("UTF-8"), "ISO-8859-1"));
						}

					}
					if (files != null) {
						for (File f1 : files) {
							b.addBinaryBody(name, f1);
						}
					}
					post.setEntity(b.build());

					resp = client.execute(post, localContext);
					r.status = resp.getStatusLine().getStatusCode();
					r.url = url;
					r.headers = resp.getAllHeaders();
					_parse(resp, r);

					if (log.isDebugEnabled())
						log.debug("post: cost=" + t.past() + ", status=" + r.status + ", body=" + r.body);

				} catch (Throwable e) {
					log.error("cost=" + t.past() + ", " + url, e);
					r.status = 600;
					r.body = "error: " + e.getMessage();
				} finally {
					if (resp != null)
						try {
							resp.close();
						} catch (IOException e) {
						}
				}

			} else {
				r.status = 600;
				r.body = "error: can not init a client";
			}

		} finally {
			if (log.isInfoEnabled())
				log.info("post, cost=" + t.past() + ", url=" + url);
		}
		return r;
	}

	public Response post(String url, JSON head, JSON body, String name, FileItem file, long timeout) {
		TimeStamp t = TimeStamp.create();

		Response r = new Response();

		try {
			if (log.isDebugEnabled())
				log.debug("url=" + url);

			_client(url, head, timeout);

			if (localContext == null) {
				localContext = HttpClientContext.create();
				localContext.setCookieStore(cookies);
			}

			if (client != null) {

				HttpPost post = new HttpPost(url);
				CloseableHttpResponse resp = null;
				try {

					if (head != null && head.size() > 0) {
						if (log.isDebugEnabled())
							log.debug("header: " + head);
						for (String s : head.keySet()) {
							post.addHeader(s, head.getString(s));
						}
					}

					if (log.isDebugEnabled())
						log.debug("post url=" + url);

					MultipartEntityBuilder b = MultipartEntityBuilder.create();
					if (body != null) {
						if (log.isDebugEnabled())
							log.debug("body: " + body);

						for (String s : body.keySet()) {
//						b.addTextBody(name, body.getString(s),
//								ContentType.create("text/plain", headers.getString("ContentType", "UTF-8")));
							b.addTextBody(s, new String(body.getString(s).getBytes("UTF-8"), "ISO-8859-1"));
						}

					}
					if (file != null) {
						b.addBinaryBody(name, file.getInputStream(), ContentType.DEFAULT_BINARY, file.getName());
					}
					post.setEntity(b.build());

					resp = client.execute(post, localContext);
					r.status = resp.getStatusLine().getStatusCode();
					r.url = url;
					r.headers = resp.getAllHeaders();
					_parse(resp, r);

					if (log.isDebugEnabled())
						log.debug("post: cost=" + t.past() + ", status=" + r.status + ", body=" + r.body);

				} catch (Throwable e) {
					log.error("cost=" + t.past() + ", " + url, e);
					r.status = 600;
					r.body = "error: " + e.getMessage();
				} finally {
					if (resp != null)
						try {
							resp.close();
						} catch (IOException e) {
						}
				}

			} else {
				r.status = 600;
				r.body = "error: can not init a client";
			}

		} finally {
			if (log.isInfoEnabled())
				log.info("post, cost=" + t.past() + ", url=" + url);
		}
		return r;

	}

	public Response post(String url, JSON head, JSON body, String name, byte[] binary, long timeout) {
		TimeStamp t = TimeStamp.create();

		Response r = new Response();

		try {
			if (log.isDebugEnabled())
				log.debug("url=" + url);

			_client(url, head, timeout);

			if (localContext == null) {
				localContext = HttpClientContext.create();
				localContext.setCookieStore(cookies);
			}

			if (client != null) {

				HttpPost post = new HttpPost(url);
				CloseableHttpResponse resp = null;
				try {

					if (head != null && head.size() > 0) {
						if (log.isDebugEnabled())
							log.debug("head: " + head);
						for (String s : head.keySet()) {
							post.addHeader(s, head.getString(s));
						}
					}

					if (log.isDebugEnabled())
						log.debug("post url=" + url);

					MultipartEntityBuilder b = MultipartEntityBuilder.create();
					if (body != null) {
						if (log.isDebugEnabled())
							log.debug("body: " + body);

						for (String s : body.keySet()) {
							b.addTextBody(s, new String(body.getString(s).getBytes("UTF-8"), "ISO-8859-1"));
						}

					}
					if (binary != null) {
						b.addBinaryBody(name, new ByteArrayInputStream(binary), ContentType.DEFAULT_BINARY,
								head == null ? null : head.getString("filename"));
					}
					post.setEntity(b.build());

					resp = client.execute(post, localContext);
					r.status = resp.getStatusLine().getStatusCode();
					r.url = url;
					r.headers = resp.getAllHeaders();
					_parse(resp, r);

					if (log.isDebugEnabled())
						log.debug("post: cost=" + t.past() + ", status=" + r.status + ", body=" + r.body);

				} catch (Throwable e) {
					log.error("cost=" + t.past() + ", " + url, e);
					r.status = 600;
					r.body = "error: " + e.getMessage();
				} finally {
					if (resp != null)
						try {
							resp.close();
						} catch (IOException e) {
						}
				}

			} else {
				r.status = 600;
				r.body = "error: can not init a client";
			}

		} finally {
			if (log.isInfoEnabled())
				log.info("post, cost=" + t.past() + ", url=" + url);
		}
		return r;

	}

	public Response post(String url, JSON head, JSON body, String name, File file, long timeout) {

		TimeStamp t = TimeStamp.create();

		Response r = new Response();

		try {
			if (log.isDebugEnabled())
				log.debug("url=" + url);

			_client(url, head, timeout);

			if (localContext == null) {
				localContext = HttpClientContext.create();
				localContext.setCookieStore(cookies);
			}

			if (client != null) {

				HttpPost post = new HttpPost(url);
				CloseableHttpResponse resp = null;
				try {

					if (head != null && head.size() > 0) {
						if (log.isDebugEnabled())
							log.debug("head: " + head);
						for (String s : head.keySet()) {
							post.addHeader(s, head.getString(s));
						}
					}

					if (log.isDebugEnabled())
						log.debug("post url=" + url);

					MultipartEntityBuilder b = MultipartEntityBuilder.create();
					if (body != null) {
						if (log.isDebugEnabled())
							log.debug("body: " + body);

						for (String s : body.keySet()) {
//						b.addTextBody(name, body.getString(s),
//								ContentType.create("text/plain", headers.getString("ContentType", "UTF-8")));
							b.addTextBody(s, new String(body.getString(s).getBytes("UTF-8"), "ISO-8859-1"));
						}

					}
					if (file != null) {
						b.addBinaryBody(name, file);
					}
					post.setEntity(b.build());

					resp = client.execute(post, localContext);
					r.status = resp.getStatusLine().getStatusCode();
					r.url = url;
					r.headers = resp.getAllHeaders();
					_parse(resp, r);

					if (log.isDebugEnabled())
						log.debug("post: cost=" + t.past() + ", status=" + r.status + ", body=" + r.body);

				} catch (Throwable e) {
					log.error("cost=" + t.past() + ", " + url, e);
					r.status = 600;
					r.body = "error: " + e.getMessage();
				} finally {
					if (resp != null)
						try {
							resp.close();
						} catch (IOException e) {
						}
				}

			} else {
				r.status = 600;
				r.body = "error: can not init a client";
			}

		} finally {
			if (log.isInfoEnabled())
				log.info("post, cost=" + t.past() + ", url=" + url);
		}
		return r;
	}

	/**
	 * put body string
	 * 
	 * @param url
	 * @param headers
	 * @param body
	 * @param timeout
	 * @return
	 */
	public Response put(String url, JSON head, String body, long timeout) {
		if (log.isDebugEnabled())
			log.debug("url=" + url);
		Response r = new Response();

		_client(url, head, timeout);

		if (localContext == null) {
			localContext = HttpClientContext.create();
			localContext.setCookieStore(cookies);
		}
		if (client != null) {
			TimeStamp t = TimeStamp.create();

			HttpPut put = new HttpPut(url);
			CloseableHttpResponse resp = null;
			try {

				if (head != null && head.size() > 0) {
					if (log.isDebugEnabled())
						log.debug("head: " + head);
					for (String s : head.keySet()) {
						put.addHeader(s, head.getString(s));
					}
				}

				if (log.isDebugEnabled())
					log.debug("put url=" + url);

				StringEntity e = new StringEntity(body, "UTF-8");
				put.setEntity(e);

				resp = client.execute(put, localContext);
				r.status = resp.getStatusLine().getStatusCode();
				r.url = url;
				r.headers = resp.getAllHeaders();
				_parse(resp, r);

				if (log.isDebugEnabled())
					log.debug("put: cost=" + t.past() + ", status=" + r.status + ", body=" + r.body);

			} catch (Throwable e) {
				log.error("cost=" + t.past() + ", " + url, e);
				r.status = 600;
				r.body = "error: " + e.getMessage();
			} finally {
				if (resp != null)
					try {
						resp.close();
					} catch (IOException e) {
					}

			}

		} else {
			r.status = 600;
			r.body = "error: can not init a client";
		}

		return r;
	}

	public Response put(String url, String contenttype, JSON head, JSON body, long timeout) {

		if (log.isDebugEnabled())
			log.debug("url=" + url);
		Response r = new Response();

		_client(url, head, timeout);

		if (localContext == null) {
			localContext = HttpClientContext.create();
			localContext.setCookieStore(cookies);
		}
		if (client != null) {
			TimeStamp t = TimeStamp.create();

			HttpPut put = new HttpPut(url);
			CloseableHttpResponse resp = null;
			try {

				if (head != null && head.size() > 0) {
					if (log.isDebugEnabled())
						log.debug("head: " + head);
					for (String s : head.keySet()) {
						put.addHeader(s, head.getString(s));
					}
				}

				if (log.isDebugEnabled())
					log.debug("put url=" + url);

				if (body != null && body.size() > 0) {
					StringEntity e = new StringEntity(body.toString(), "UTF-8");
					put.setEntity(e);
				}

				resp = client.execute(put, localContext);
				r.status = resp.getStatusLine().getStatusCode();
				r.url = url;
				r.headers = resp.getAllHeaders();
				_parse(resp, r);

				if (log.isDebugEnabled())
					log.debug("put: cost=" + t.past() + ", status=" + r.status + ", body=" + r.body);

			} catch (Throwable e) {
				log.error("cost=" + t.past() + ", " + url, e);
				r.status = 600;
				r.body = "error: " + e.getMessage();
			} finally {
				if (resp != null)
					try {
						resp.close();
					} catch (IOException e) {
					}

			}

		} else {
			r.status = 600;
			r.body = "error: can not init a client";
		}

		return r;
	}

	public Response delete(String url, String contenttype, JSON head, long timeout) {

		if (log.isDebugEnabled())
			log.debug("url=" + url);
		Response r = new Response();

		_client(url, head, timeout);

		if (localContext == null) {
			localContext = HttpClientContext.create();
			localContext.setCookieStore(cookies);
		}

		if (client != null) {
			TimeStamp t = TimeStamp.create();

			HttpDelete delete = new HttpDelete(url);
			CloseableHttpResponse resp = null;
			try {

				if (head != null && head.size() > 0) {
					if (log.isDebugEnabled())
						log.debug("head: " + head);
					for (String s : head.keySet()) {
						delete.addHeader(s, head.getString(s));
					}
				}

				if (log.isDebugEnabled())
					log.debug("delete url=" + url);

				resp = client.execute(delete, localContext);
				r.status = resp.getStatusLine().getStatusCode();
				r.url = url;
				r.headers = resp.getAllHeaders();
				_parse(resp, r);

				if (log.isDebugEnabled())
					log.debug("delete: cost=" + t.past() + ", status=" + r.status + ", body=" + r.body);

			} catch (Throwable e) {
				log.error("cost=" + t.past() + ", " + url, e);
				r.status = 600;
				r.body = "error: " + e.getMessage();
			} finally {
				if (resp != null)
					try {
						resp.close();
					} catch (IOException e) {
					}

			}

		} else {
			r.status = 600;
			r.body = "error: can not init a client";
		}

		return r;
	}

	public Response head(String url, String contenttype, JSON head, long timeout) {

		if (log.isDebugEnabled())
			log.debug("url=" + url);
		Response r = new Response();

		_client(url, head, timeout);

		if (localContext == null) {
			localContext = HttpClientContext.create();
			localContext.setCookieStore(cookies);
		}

		if (client != null) {
			TimeStamp t = TimeStamp.create();

			HttpHead h1 = new HttpHead(url);
			CloseableHttpResponse resp = null;
			try {

				if (head != null && head.size() > 0) {
					if (log.isDebugEnabled())
						log.debug("header: " + head);
					for (String s : head.keySet()) {
						h1.addHeader(s, head.getString(s));
					}
				}

				if (log.isDebugEnabled())
					log.debug("head url=" + url);

				resp = client.execute(h1, localContext);
				r.status = resp.getStatusLine().getStatusCode();
				r.url = url;
				r.headers = resp.getAllHeaders();
				_parse(resp, r);

				if (log.isDebugEnabled())
					log.debug("head: cost=" + t.past() + ", status=" + r.status + ", body=" + r.body);

			} catch (Throwable e) {
				log.error("cost=" + t.past() + ", " + url, e);
				r.status = 600;
				r.body = "error: " + e.getMessage();
			} finally {
				if (resp != null)
					try {
						resp.close();
					} catch (IOException e) {
					}

			}

		} else {
			r.status = 600;
			r.body = "error: can not init a client";
		}

		return r;
	}

	private CloseableHttpClient _client(String url, JSON head, long timeout) {

		if (head != null && head.containsKey("user-agent")) {
			// close before
			if (client != null) {
				X.close(client);
				client = null;
			}
		}
		if (client == null) {
			String ua = head != null && head.containsKey("user-agent") ? head.getString("user-agent") : _UA();
			if (ctx == null) {
				try {
					if (url.toLowerCase().startsWith("https")) {
						ctx = SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();
					}
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}

			int t = (int) timeout;
			RequestConfig config = RequestConfig.custom().setConnectTimeout(t).setSocketTimeout(t)
					.setConnectionRequestTimeout(t).setCookieSpec(CookieSpecs.STANDARD).build();

			HttpClientBuilder builder = HttpClients.custom().setSSLContext(ctx).setDefaultRequestConfig(config)
					.setUserAgent(ua).setMaxConnTotal(200).setMaxConnPerRoute(10);
			if (!redirect) {
				builder.disableRedirectHandling();
			}

			builder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
//			builder.setSSLHostnameVerifier(new javax.net.ssl.HostnameVerifier() {
//				public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
//					return true;
//				}
//			});

			if (!X.isEmpty(proxy)) {
				String[] ss = X.split(proxy, ":");
				if (ss != null && ss.length > 1) {

					HttpHost h1 = new HttpHost(ss[0], X.toInt(ss[1]));
					log.warn("proxy=" + h1);

					builder.setProxy(h1);

					if (!X.isEmpty(user)) {
						CredentialsProvider credsProvider = new BasicCredentialsProvider();
						credsProvider.setCredentials(new AuthScope(ss[0], X.toInt(ss[1])),
								new UsernamePasswordCredentials(user, passwd));
						builder.setDefaultCredentialsProvider(credsProvider);
					}
				}
			}

			builder.setDnsResolver(dns);

			client = builder.build();
		}
		return client;
	}

	public void dns(String host, String ip) {
		dns.addResolve(host, ip);
	}

	private MyDnsResolver dns = new MyDnsResolver();

	private void _parse(HttpResponse response, Http.Response r) {

		String cs = "UTF-8";
		Header[] hh = response.getHeaders("Content-Type");
		if (hh != null) {
			for (Header h : hh) {
				r.type = h.getValue();
				if (r.type.contains("application/pdf")) {
					_stream(response, r);
					return;
				}
				StringFinder sf = StringFinder.create(r.type);
				String s = sf.get("charset=", ";");
				if (!X.isEmpty(s)) {
					cs = s;
				}
			}
		}

		r.body = null;

		HttpEntity entity = response.getEntity();
		if (entity != null) {
			try {

				/**
				 * fix the bug of http.util of apache
				 */
				String encoding = null;
				if (entity.getContentEncoding() != null) {
					encoding = entity.getContentEncoding().getValue();
				}

				if (encoding != null && encoding.indexOf("gzip") > -1) {

					BufferedHttpEntity bufferedEntity = new BufferedHttpEntity(entity);

					entity = bufferedEntity;

					ByteBuffer bb = ByteBuffer.allocate(2 * 1024 * 1024);

					GZIPInputStream in = null;

					try {
						in = new GZIPInputStream(bufferedEntity.getContent());

						byte[] buf = new byte[1024];
						int len = in.read(buf);
						while (len > 0) {
							bb.put(buf, 0, len);
							len = in.read(buf);
						}

						byte[] b1 = bb.array();
						StringFinder sf = StringFinder.create(new String(b1, 0, bb.position()));
						String cc = sf.get("charset=", "\"");
						if (X.isEmpty(cc)) {
							cc = cs;
						}
						r.body = new String(b1, 0, bb.position(), cc);

					} catch (Exception e) {
						log.error(e.getMessage(), e);
						X.close(in);
					}

				}

				if (r.body == null || r.body.length() == 0) {
					r.body = _getContext(entity, cs);
				}

				// log.debug(context);

			} catch (Exception e) {
				log.error(r.type, e);
			}

		}

	}

	private void _stream(HttpResponse response, Response r) {

		HttpEntity entity = response.getEntity();
		InputStream in = null;
		ByteBuffer bb = ByteBuffer.allocate(4 * 1024 * 1024);
		try {
			BufferedHttpEntity bufferedEntity = new BufferedHttpEntity(entity);
			in = bufferedEntity.getContent();
			byte[] buf = new byte[1024];
			int len = in.read(buf);
			while (len > 0) {
				bb.put(buf, 0, len);
				len = in.read(buf);
			}
			r._body = bb.array();

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			X.close(in);
		}

	}

	private String _getContext(HttpEntity entity, String cs) {

		_Buffer bb = _Buffer.create(32 * 1024);

		InputStream reader = null;
		String cc = null;

		try {

			reader = entity.getContent();

			byte[] buf = new byte[1024];
			int len = reader.read(buf);
			while (len > 0) {
				bb.append(buf, 0, len);
				len = reader.read(buf);
			}

			StringFinder sf = StringFinder.create(new String(bb.buf, 0, bb.pos));
			cc = sf.get("charset=", "\"");

			if (X.isEmpty(cc) || cc.length() > 10) {
				cc = cs;
			}

			return new String(bb.buf, 0, bb.pos, cc);

		} catch (Exception e) {
			log.error(cc, e);
		} finally {
			X.close(reader);
		}

		return null;

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
		private Header[] headers;

		public static Response create() {
			return new Response();
		}

		/**
		 * 用body 生成StringFinder工具类
		 * 
		 * @return StringFinder工具类
		 */
		public StringFinder finder() {
			return StringFinder.create(body);
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
			String[] ss = this.getHeaders("Set-Cookie");
			return X.join(X.asList(ss, s -> {
				String s1 = s.toString();
				int i = s1.indexOf(";");
				if (i > 0) {
					return s1.substring(0, i);
				}
				return s1;
			}), ";");
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
		public Header[] getHeader() {
			return headers;
		}

		/**
		 * get the response header.
		 *
		 * @param name the name
		 * @return String[]
		 */
		public String[] getHeaders(String name) {
			List<String> list = new ArrayList<String>();
			if (headers != null && headers.length > 0) {
				for (Header h : headers) {
					if (X.isSame(name, h.getName())) {
						list.add(h.getValue());
					}
				}
			}
			if (list.size() > 0) {
				return list.toArray(new String[list.size()]);
			}
			return null;
		}

		/**
		 * get the response header.
		 *
		 * @param name the name
		 * @return String
		 */
		public String getHeader(String name) {
			String o = null;
			if (headers != null && headers.length > 0) {
				for (Header h : headers) {
					if (X.isSame(name, h.getName())) {
						o = h.getValue();
					}
				}
			}
			return o;
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
	public synchronized void addCookie(String name, String value, String domain, String path, Date expired) {
		BasicClientCookie c = new BasicClientCookie(name, value);
		c.setDomain(domain);
		c.setPath(X.isEmpty(path) ? "/" : path);
		c.setExpiryDate(expired);
		cookies.addCookie(c);
	}

	/**
	 * batchCookies
	 * 
	 * @param cookiestring the cookie string, eg.:"a=b;c=a"
	 * @param domain       the domain
	 * @param path         the path
	 * @param expired      the expired date
	 */
	public synchronized void batchCookie(String cookiestring, String domain, String path, Date expired) {
		String[] ss = X.split(cookiestring, ";");
		for (String s : ss) {
			StringFinder sf = StringFinder.create(s);
			String name = sf.nextTo("=");
			String value = sf.remain();
			if (!X.isEmpty(name)) {
				removeCookie(name, domain, path);

				BasicClientCookie c = new BasicClientCookie(name, value);
				c.setDomain(domain);
				c.setPath(X.isEmpty(path) ? "/" : path);
				c.setExpiryDate(expired);
				cookies.addCookie(c);
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
	public synchronized void removeCookie(String name, String domain, String path) {
		boolean found = false;
		List<Cookie> l1 = getCookies();
		for (int i = l1.size() - 1; i >= 0; i--) {
			Cookie c = l1.get(i);
			if (X.isSame(c.getName(), name) && X.isSame(c.getDomain(), domain) && X.isSame(c.getPath(), path)) {
				l1.remove(i);
				found = true;
			}
		}
		if (found) {
			cookies.clear();
			for (Cookie c : l1) {
				cookies.addCookie(c);
			}
		}
	}

	public List<Cookie> getCookies() {
		return cookies.getCookies();
	}

	/**
	 * Clear cookies.
	 */
	public void clearCookies() {
		cookies.clear();
	}

	/**
	 * Clear.
	 *
	 * @param expired the expired
	 */
	public void clear(Date expired) {
		cookies.clearExpired(expired);
	}

	/**
	 * Gets the cookies.
	 *
	 * @param domain the domain
	 * @return the cookies
	 */
	public List<Cookie> getCookies(String domain) {
		List<Cookie> l1 = getCookies();
		List<Cookie> l2 = new ArrayList<Cookie>();
		if (!X.isEmpty(l1)) {
			for (Cookie c : l1) {
				if (X.isSame(c.getDomain(), domain))
					l2.add(c);
			}
		}
		return l2;
	}

	/**
	 * Gets the cookie.
	 *
	 * @param name   the name
	 * @param domain the domain
	 * @param path   the path
	 * @return the cookie
	 */
	public Cookie getCookie(String name, String domain, String path) {
		List<Cookie> l1 = getCookies();
		if (l1 != null) {
			for (Cookie c : l1) {
				if (X.isSame(c.getName(), name) && X.isSame(c.getDomain(), domain) && X.isSame(c.getPath(), path)) {
					return c;
				}
			}
		}
		return null;
	}

	/**
	 * Gets the cookie.
	 *
	 * @param name   the name
	 * @param domain the domain
	 * @return the cookie
	 */
	public Cookie getCookie(String name, String domain) {
		List<Cookie> l1 = getCookies();
		if (l1 != null) {
			for (Cookie c : l1) {
				if (X.isSame(c.getName(), name) && X.isSame(c.getDomain(), domain)) {
					return c;
				}
			}
		}
		return null;
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

	public static String getQuery(String url, String name) {

		String[] ss = X.split(url, "[.?&]");
		if (ss != null) {
			for (int i = 1; i < ss.length; i++) {
				StringFinder f = StringFinder.create(ss[i]);
				String s = f.nextTo("=");
				f.skip(1);
				String value = f.remain();
				if (X.isSame(name, s)) {
					return value;
				}
			}
		}
		return null;
	}

	/**
	 * post body string
	 * 
	 * @param url
	 * @param headers
	 * @param body
	 * @param timeout
	 * @return
	 */
	public Response post(String url, JSON head, String body, long timeout) {

		if (log.isDebugEnabled())
			log.debug("url=" + url);

		Response r = new Response();

		client = _client(url, head, timeout);

		if (localContext == null) {
			localContext = HttpClientContext.create();
			localContext.setCookieStore(cookies);
		}

		if (client != null) {
			TimeStamp t = TimeStamp.create();

			HttpPost post = new HttpPost(url);
			CloseableHttpResponse resp = null;
			try {

				if (head != null && head.size() > 0) {
					if (log.isDebugEnabled())
						log.debug("header: " + head);
					for (String s : head.keySet()) {
						post.addHeader(s, head.getString(s));
					}
				}

				if (log.isDebugEnabled())
					log.debug("post url=" + url);

				StringEntity e = new StringEntity(body, "UTF-8");
				post.setEntity(e);

				resp = client.execute(post, localContext);
				r.status = resp.getStatusLine().getStatusCode();
				r.url = url;
				r.headers = resp.getAllHeaders();
				_parse(resp, r);

//				if (log.isDebugEnabled())
//					log.debug("post: cost=" + t.past() + ", status=" + r.status + ", body=" + r.body);

			} catch (Throwable e) {
				log.error("cost=" + t.past() + ", " + url, e);
				r.status = 600;
				r.body = "error: " + e.getMessage();
			} finally {
				if (resp != null)
					try {
						resp.close();
					} catch (IOException e) {
					}

			}

		} else {
			r.status = 600;
			r.body = "error: can not init a client";
		}

		return r;
	}

	private class MyDnsResolver implements DnsResolver {

		private final Map<String, InetAddress[]> MAPPINGS = new HashMap<String, InetAddress[]>();

		private void addResolve(String host, String ip) {
			try {
				MAPPINGS.put(host, new InetAddress[] { InetAddress.getByName(ip) });
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}

		@Override
		public InetAddress[] resolve(String host) throws UnknownHostException {
			return MAPPINGS.containsKey(host) ? MAPPINGS.get(host) : new InetAddress[] { InetAddress.getByName(host) };
		}
	}

	static class _Buffer {

		byte[] buf;
		int pos = 0;

		public static _Buffer create(int capicity) {
			_Buffer b = new _Buffer();
			b.buf = new byte[capicity];
			return b;
		}

		void append(byte[] bb, int pos, int len) {
			if (this.buf.length - this.pos < len) {
				byte[] b1 = new byte[this.buf.length + len + 4 * 1024];
				System.arraycopy(this.buf, 0, b1, 0, this.pos);
				this.buf = b1;
			}
			System.arraycopy(bb, pos, this.buf, this.pos, len);
			this.pos += len;
		}

	}

	boolean redirect = true;

	public void redirect(boolean enable) {
		try {
			if (redirect != enable && client != null) {
				client.close();
				client = null;
			}
			redirect = enable;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public static Http owner = create();

}
