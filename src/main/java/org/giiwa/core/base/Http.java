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
package org.giiwa.core.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Column;
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.Table;
import org.giiwa.core.bean.TimeStamp;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.json.JSON;

/**
 * http utils
 * 
 * @author joe
 * 
 */
@SuppressWarnings("deprecation")
public final class Http {

  // private static final long MIN_ZIP_SIZE = 1024 * 1024 * 1024;
  static Log                  log     = LogFactory.getLog(Http.class);

  private final static String UA      = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36";

  private static boolean      DEBUG   = false;
  public BasicCookieStore     cookies = new BasicCookieStore();
  private SSLContext          ctx     = null;
  private String              proxy;

  public static Http          owner   = create();

  /**
   * create a default Http client
   * 
   * @return the http
   */
  public static Http create() {
    return new Http();
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

  /**
   * Gets the.
   *
   * @param url
   *          the url
   * @return the response
   */
  public Response get(String url) {
    return get(url, X.AMINUTE);
  }

  /**
   * Gets the.
   *
   * @param url
   *          the url
   * @param timeout
   *          the milliseconds of timeout
   * @return the response
   */
  public Response get(String url, long timeout) {
    return get(url, null, null, timeout);
  }

  /**
   * GET response from a url.
   *
   * @param url
   *          the url
   * @param charset
   *          the charset
   * @param timeout
   *          the timeout
   * @return Response
   */
  public Response get(String url, String charset, long timeout) {
    return get(url, charset, null, timeout);
  }

  /**
   * ping the url, throw exception if occur error.
   *
   * @param url
   *          the url
   * @return int of response status
   * @throws Exception
   *           throw exception when failed
   */
  public int ping(String url) throws Exception {

    URL u = new URL(url);
    HttpURLConnection c = (HttpURLConnection) u.openConnection();
    c.connect();
    int code = c.getResponseCode();
    log.debug("ping=" + url + ", response.code=" + code);
    c.disconnect();
    return code;
  }

  /**
   * Post.
   *
   * @param url
   *          the url
   * @param params
   *          the params
   * @return the response
   */
  public Response post(String url, JSON params) {
    return post(url, params, X.AMINUTE);
  }

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
   * @param url
   *          the url
   * @param params
   *          the params
   * @param timeout
   *          the timeout
   * @return Response
   */
  public Response post(String url, JSON params, long timeout) {
    return post(url, null, params, timeout);
  }

  public Response put(String url, JSON params, long timeout) {
    return put(url, null, params, timeout);
  }

  /**
   * port to the url with the custom header
   * 
   * @param url
   *          the url
   * @param headers
   *          the headers
   * @param params
   *          the params
   * @param timeout
   *          the timeout
   * @return the Response
   */
  public Response post(String url, JSON headers, JSON params, long timeout) {
    return post(url, "application/x-javascript; charset=UTF8", headers, params, timeout);
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

  /**
   * Gets the.
   *
   * @param url
   *          the url
   * @param headers
   *          the headers
   * @return the response
   */
  public Response get(String url, JSON headers) {
    return get(url, null, headers, X.AMINUTE);
  }

  /**
   * connect to the url and return the HttpResponse directly.
   *
   * @param url
   *          the url
   * @param timeout
   *          the timeout milliseconds
   * @return the HttpURLConnection
   * @throws Exception
   *           the exception
   */
  public HttpURLConnection connect(String url, long timeout) throws Exception {

    URL u = new URL(url);
    HttpURLConnection c = (HttpURLConnection) u.openConnection();
    c.setConnectTimeout((int) timeout);
    c.setReadTimeout((int) timeout);
    c.connect();
    return c;
  }

  /**
   * GET method.
   *
   * @param url
   *          the url
   * @param charset
   *          the charset
   * @param headers
   *          the headers
   * @param timeout
   *          the timeout
   * @return Response
   */
  public Response get(String url, String charset, JSON headers, long timeout) {

    log.debug("url=\"" + url + "\"");

    String[] ss = url.split(" ");
    url = ss[ss.length - 1];

    Response r = new Response();
    String ua = headers != null && headers.containsKey("user-agent") ? headers.getString("user-agent") : UA;
    CloseableHttpClient client = getClient(url, ua, timeout);

    if (client != null) {
      HttpGet get = null;
      CloseableHttpResponse resp = null;
      try {
        get = new HttpGet(url);

        if (headers != null && headers.size() > 0) {
          for (String s : headers.keySet()) {
            get.addHeader(s, headers.getString(s));
          }
        }

        log.debug("get url=" + url);

        HttpClientContext localContext = HttpClientContext.create();
        localContext.setCookieStore(cookies);
        resp = client.execute(get, localContext);

        r.status = resp.getStatusLine().getStatusCode();
        r.body = getContext(resp, charset);
        r.headers = resp.getAllHeaders();

      } catch (Throwable e) {
        log.error("\"" + url + "\"", e);

        r.status = 500;
        r.body = X.toString(e);
        // e.printStackTrace();
      } finally {
        try {
          if (resp != null)
            resp.close();
        } catch (IOException e) {
        }
        try {
          client.close();
        } catch (IOException e) {
        }
      }
    }

    return r;
  }

  /**
   * download the file in the url to f.
   *
   * @param url
   *          the file url
   * @param localfile
   *          the destination file
   * @return the length
   */
  public int download(String url, File localfile) {
    return download(url, null, localfile);
  }

  /**
   * download the remote url to local file with the header.
   *
   * @param url
   *          the remote resource url
   * @param header
   *          the header
   * @param localfile
   *          the localfile
   * @return the length of bytes
   */
  public int download(String url, JSON header, File localfile) {

    log.debug("url=\"" + url + "\"");

    String[] ss = url.split(" ");
    url = ss[ss.length - 1];

    String ua = header != null && header.containsKey("user-agent") ? header.getString("user-agent") : UA;
    CloseableHttpClient client = getClient(url, ua, X.AMINUTE);

    if (client != null) {
      HttpGet get = null;
      CloseableHttpResponse resp = null;
      try {
        get = new HttpGet(url);

        if (header != null && header.size() > 0) {
          for (String name : header.keySet()) {
            get.addHeader(name, header.getString(name));
          }
        }

        log.debug("get url=" + url);

        HttpClientContext localContext = HttpClientContext.create();
        localContext.setCookieStore(cookies);
        resp = client.execute(get, localContext);
        int status = resp.getStatusLine().getStatusCode();

        if (DEBUG) {
          Header[] hh = resp.getAllHeaders();
          if (hh != null) {
            for (Header h : hh) {
              System.out.println(h.getName() + ":" + h.getValue());
            }
          }
          System.out.println("status:" + status);
        }

        if (status == 200 || status == 206) {
          HttpEntity e = resp.getEntity();
          InputStream in = e.getContent();

          localfile.getParentFile().mkdirs();

          FileOutputStream out = new FileOutputStream(localfile);
          return IOUtil.copy(in, out);
        }
        return 0;
      } catch (Exception e) {
        log.error("\"" + url + "\"", e);
      } finally {
        if (resp != null)
          try {
            resp.close();
          } catch (IOException e) {
          }

        try {
          client.close();
        } catch (IOException e) {
        }
      }
    }

    return 0;
  }

  /**
   * POST method.
   *
   * @param url
   *          the url
   * @param contenttype
   *          the contenttype
   * @param headers
   *          the headers
   * @param params
   *          the params
   * @param timeout
   *          the timeout
   * @return Response
   */
  public Response post(String url, String contenttype, JSON headers, JSON params, long timeout) {
    return post(url, contenttype, headers, params, null, timeout);
  }

  /**
   * POST.
   *
   * @param url
   *          the url
   * @param contenttype
   *          the contenttype
   * @param headers
   *          the headers
   * @param body
   *          the body
   * @param attachments
   *          the attachments
   * @param timeout
   *          the timeout
   * @return Response
   */
  public Response post(String url, String contenttype, JSON headers, JSON body, JSON attachments, long timeout) {

    Response r = new Response();

    String ua = headers != null && headers.containsKey("user-agent") ? headers.getString("user-agent") : UA;
    CloseableHttpClient client = getClient(url, ua, timeout);
    log.debug("url=" + url);

    if (client != null) {
      TimeStamp t = TimeStamp.create();

      HttpPost post = new HttpPost(url);
      CloseableHttpResponse resp = null;
      try {

        if (headers != null && headers.size() > 0) {
          log.debug("header: " + headers);
          for (String s : headers.keySet()) {
            post.addHeader(s, headers.getString(s));
          }
        }

        log.debug("post url=" + url);

        if (attachments == null || attachments.size() == 0) {
          if (body != null && body.size() > 0) {
            log.debug("body: " + body);
            List<NameValuePair> paramList = new ArrayList<NameValuePair>();

            for (String s : body.keySet()) {
              BasicNameValuePair param = new BasicNameValuePair(s, body.getString(s));
              paramList.add(param);
            }
            StringEntity e = new UrlEncodedFormEntity(paramList, HTTP.UTF_8);
            // if (e.getContentLength() > MIN_ZIP_SIZE) {
            // post.setHeader("Content-Encoding", "gzip");
            // ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // GZIPOutputStream gzipOut = new GZIPOutputStream(baos);
            // e.writeTo(gzipOut);
            // gzipOut.finish();
            // post.setEntity(new ByteArrayEntity(baos.toByteArray()));
            // } else {
            post.setEntity(e);

            // }
          }
        } else {
          MultipartEntity e = new MultipartEntity();
          for (String f : attachments.keySet()) {
            Object o = attachments.get(f);
            if (o instanceof File) {
              FileBody fileBody = new FileBody((File) o);
              e.addPart(f, fileBody);
            }
          }

          if (body != null && body.size() > 0) {
            for (String s : body.keySet()) {
              StringBody stringBody = new StringBody(body.getString(s));
              e.addPart(s, stringBody);
            }
          }
          // if (e.getContentLength() > MIN_ZIP_SIZE) {
          // post.setHeader("Content-Encoding", "gzip");
          // ByteArrayOutputStream baos = new ByteArrayOutputStream();
          // GZIPOutputStream gzipOut = new GZIPOutputStream(baos);
          // e.writeTo(gzipOut);
          // gzipOut.finish();
          // post.setEntity(new ByteArrayEntity(baos.toByteArray()));
          // } else {
          post.setEntity(e);
          // }
        }

        HttpClientContext localContext = HttpClientContext.create();
        localContext.setCookieStore(cookies);

        resp = client.execute(post, localContext);
        r.status = resp.getStatusLine().getStatusCode();
        r.body = getContext(resp, null);
        r.headers = resp.getAllHeaders();

        log.debug("post: cost=" + t.past() + "ms, status=" + r.status + ", body=" + r.body);

      } catch (Throwable e) {
        log.error("cost=" + t.past() + "ms, " + url, e);
        r.status = 600;
        r.body = "error: " + e.getMessage();
      } finally {
        if (resp != null)
          try {
            resp.close();
          } catch (IOException e) {
          }

        try {
          client.close();
        } catch (IOException e) {
        }
      }

    } else {
      r.status = 600;
      r.body = "error: can not init a client";
    }

    return r;
  }

  public Response put(String url, String contenttype, JSON headers, JSON body, long timeout) {

    Response r = new Response();

    String ua = headers != null && headers.containsKey("user-agent") ? headers.getString("user-agent") : UA;
    CloseableHttpClient client = getClient(url, ua, timeout);
    log.debug("url=" + url);

    if (client != null) {
      TimeStamp t = TimeStamp.create();

      HttpPut put = new HttpPut(url);
      CloseableHttpResponse resp = null;
      try {

        if (headers != null && headers.size() > 0) {
          log.debug("header: " + headers);
          for (String s : headers.keySet()) {
            put.addHeader(s, headers.getString(s));
          }
        }

        log.debug("put url=" + url);

        if (body != null && body.size() > 0) {
          StringEntity e = new StringEntity(body.toString());
          put.setEntity(e);
        }

        HttpClientContext localContext = HttpClientContext.create();
        localContext.setCookieStore(cookies);

        resp = client.execute(put, localContext);
        r.status = resp.getStatusLine().getStatusCode();
        r.body = getContext(resp, null);
        r.headers = resp.getAllHeaders();

        log.debug("put: cost=" + t.past() + "ms, status=" + r.status + ", body=" + r.body);

      } catch (Throwable e) {
        log.error("cost=" + t.past() + "ms, " + url, e);
        r.status = 600;
        r.body = "error: " + e.getMessage();
      } finally {
        if (resp != null)
          try {
            resp.close();
          } catch (IOException e) {
          }

        try {
          client.close();
        } catch (IOException e) {
        }
      }

    } else {
      r.status = 600;
      r.body = "error: can not init a client";
    }

    return r;
  }

  public Response delete(String url, String contenttype, JSON headers, long timeout) {

    Response r = new Response();

    String ua = headers != null && headers.containsKey("user-agent") ? headers.getString("user-agent") : UA;
    CloseableHttpClient client = getClient(url, ua, timeout);
    log.debug("url=" + url);

    if (client != null) {
      TimeStamp t = TimeStamp.create();

      HttpDelete delete = new HttpDelete(url);
      CloseableHttpResponse resp = null;
      try {

        if (headers != null && headers.size() > 0) {
          log.debug("header: " + headers);
          for (String s : headers.keySet()) {
            delete.addHeader(s, headers.getString(s));
          }
        }

        log.debug("delete url=" + url);

        HttpClientContext localContext = HttpClientContext.create();
        localContext.setCookieStore(cookies);

        resp = client.execute(delete, localContext);
        r.status = resp.getStatusLine().getStatusCode();
        r.body = getContext(resp, null);
        r.headers = resp.getAllHeaders();

        log.debug("delete: cost=" + t.past() + "ms, status=" + r.status + ", body=" + r.body);

      } catch (Throwable e) {
        log.error("cost=" + t.past() + "ms, " + url, e);
        r.status = 600;
        r.body = "error: " + e.getMessage();
      } finally {
        if (resp != null)
          try {
            resp.close();
          } catch (IOException e) {
          }

        try {
          client.close();
        } catch (IOException e) {
        }
      }

    } else {
      r.status = 600;
      r.body = "error: can not init a client";
    }

    return r;
  }

  public Response head(String url, String contenttype, JSON headers, long timeout) {

    Response r = new Response();

    String ua = headers != null && headers.containsKey("user-agent") ? headers.getString("user-agent") : UA;
    CloseableHttpClient client = getClient(url, ua, timeout);
    log.debug("url=" + url);

    if (client != null) {
      TimeStamp t = TimeStamp.create();

      HttpHead head = new HttpHead(url);
      CloseableHttpResponse resp = null;
      try {

        if (headers != null && headers.size() > 0) {
          log.debug("header: " + headers);
          for (String s : headers.keySet()) {
            head.addHeader(s, headers.getString(s));
          }
        }

        log.debug("head url=" + url);

        HttpClientContext localContext = HttpClientContext.create();
        localContext.setCookieStore(cookies);

        resp = client.execute(head, localContext);
        r.status = resp.getStatusLine().getStatusCode();
        r.body = getContext(resp, null);
        r.headers = resp.getAllHeaders();

        log.debug("head: cost=" + t.past() + "ms, status=" + r.status + ", body=" + r.body);

      } catch (Throwable e) {
        log.error("cost=" + t.past() + "ms, " + url, e);
        r.status = 600;
        r.body = "error: " + e.getMessage();
      } finally {
        if (resp != null)
          try {
            resp.close();
          } catch (IOException e) {
          }

        try {
          client.close();
        } catch (IOException e) {
        }
      }

    } else {
      r.status = 600;
      r.body = "error: can not init a client";
    }

    return r;
  }

  private CloseableHttpClient getClient(String url, String ua, long timeout) {

    if (ctx == null) {
      try {
        ctx = SSLContexts.custom().loadTrustMaterial(null, new TrustStrategy() {

          @Override
          public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
            // TODO Auto-generated method stub
            return true;
          }
        }).build();

      } catch (Exception e) {
        log.error(e.getMessage(), e);
        // e.printStackTrace();
      }
    }
    int t = (int) timeout;
    RequestConfig config = RequestConfig.custom().setConnectTimeout(t).setSocketTimeout(t)
        .setConnectionRequestTimeout(t).build();
    HttpClientBuilder builder = HttpClients.custom().setSSLContext(ctx).setDefaultRequestConfig(config)
        .setUserAgent(ua);
    
    if (!X.isEmpty(proxy)) {
      String[] ss = X.split(proxy, ":");
      if (ss != null && ss.length > 1) {
        return builder.setProxy(new HttpHost(ss[0], X.toInt(ss[1]))).build();
      }
    }
    return builder.build();
  }

  private String getContext(HttpResponse response, String charset) {
    String context = null;

    if (response.getEntity() != null) {
      try {
        HttpEntity entity = response.getEntity();
        String ccs = EntityUtils.getContentCharSet(entity);

        /**
         * fix the bug of http.util of apache
         */
        String encoding = null;
        if (entity.getContentEncoding() != null) {
          encoding = entity.getContentEncoding().getValue();
        }

        if (ccs == null) {
          ccs = charset;
        }
        if (ccs == null) {
          ccs = "UTF-8";
        }
        if (encoding != null && encoding.indexOf("gzip") > -1) {

          BufferedHttpEntity bufferedEntity = new BufferedHttpEntity(entity);

          entity = bufferedEntity;

          StringBuilder sb = new StringBuilder();

          try {
            GZIPInputStream in = new GZIPInputStream(bufferedEntity.getContent());

            Reader reader = new InputStreamReader(in, ccs);

            // String s = reader.readLine();
            char[] buf = new char[2048];
            int len = reader.read(buf);
            while (len > 0) {
              sb.append(buf, 0, len);
              // sb.append(s).append("\r\n");
              len = reader.read(buf);
              // s = reader.readLine();
            }
          } catch (Exception e) {
            log.error(e.getMessage(), e);
          }

          if (sb.length() > 0) {
            context = sb.toString();
          }
        }

        if (context == null || context.length() == 0) {
          context = _getContext(entity, ccs);
        }

        // log.debug(context);

      } catch (Exception e) {
        log.error(e.getMessage());// , e);
      }
    }
    return context;

  }

  private String _getContext(HttpEntity entity, String charset) {
    StringBuilder sb = new StringBuilder();

    InputStreamReader reader = null;

    try {
      if (charset == null) {
        reader = new InputStreamReader(entity.getContent());
      } else {
        reader = new InputStreamReader(entity.getContent(), charset);
      }

      char[] buf = new char[1024];
      int len = reader.read(buf);
      while (len > 0) {
        sb.append(buf, 0, len);
        len = reader.read(buf);
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          log.error(e.getMessage(), e);
        }
      }
    }

    return sb.toString();

  }

  /**
   * the http response
   * 
   * @author joe
   *
   */
  public class Response {
    public int       status;
    public String    body;
    private Header[] headers;

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
     * @param name
     *          the name
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
     * @param name
     *          the name
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
  }

  /**
   * The main method.
   *
   * @param args
   *          the arguments
   */
  public static void main(String[] args) {
    Http.DEBUG = true;

    Http h = Http.create();
    System.out.println("response=" + h.get("http://giiwa.org").body);

    String url = "http://www.giiwa.org/repo/3vfusptnmaoeu";
    File f = new File("/Users/wujun/d/temp/repo.zip");
    JSON head = JSON.create();
    head.put("Range", "bytes=0-1");
    int len = h.download(url, head, f);
    System.out.println("repo, done, len=" + len);

    head = JSON.create();
    head.put("Range", "bytes=1-10");
    len = h.download(url, head, f);
    System.out.println("repo, done, len=" + len);

    url = "http://www.giiwa.org/giiwa-1.2-1611111820.zip";
    File f1 = new File("/Users/wujun/d/temp/stat.zip");
    head = JSON.create();
    head.put("Range", "bytes=0-1");
    len = h.download(url, head, f1);

    System.out.println("static done, len=" + len);

    try {
      FileInputStream i1 = new FileInputStream(f);
      byte[] b1 = new byte[10];
      i1.read(b1);
      System.out.print("repo=");
      for (int i = 0; i < b1.length; i++) {
        System.out.print(b1[i] + " ");
      }
      i1.close();

      FileInputStream i2 = new FileInputStream(f1);
      byte[] b2 = new byte[10];
      i2.read(b2);
      System.out.print("\r\nstat=");
      for (int i = 0; i < b2.length; i++) {
        System.out.print(b2[i] + " ");
      }
      i2.close();
    } catch (Exception e) {
      // e.printStackTrace();
    }

  }

  /**
   * add a cookie in Http. or replace the old one by (name, domain, path)
   *
   * @param name
   *          the name
   * @param value
   *          the value
   * @param domain
   *          the domain
   * @param path
   *          the path
   * @param expired
   *          the expired date
   */
  public void addCookie(String name, String value, String domain, String path, Date expired) {
    BasicClientCookie c = new BasicClientCookie(name, value);
    c.setDomain(domain);
    c.setPath(X.isEmpty(path) ? "/" : path);
    c.setExpiryDate(expired);
    cookies.addCookie(c);
  }

  /**
   * batchCookies
   * 
   * @param cookiestring
   *          the cookie string, eg.:"a=b;c=a"
   * @param domain
   *          the domain
   * @param path
   *          the path
   * @param expired
   *          the expired date
   */
  public void batchCookie(String cookiestring, String domain, String path, Date expired) {
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
   * @param name
   *          the name
   * @param domain
   *          the domain
   * @param path
   *          the path
   */
  public void removeCookie(String name, String domain, String path) {
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
   * @param expired
   *          the expired
   */
  public void clear(Date expired) {
    cookies.clearExpired(expired);
  }

  /**
   * Gets the cookies.
   *
   * @param domain
   *          the domain
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
   * @param name
   *          the name
   * @param domain
   *          the domain
   * @param path
   *          the path
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
   * @param name
   *          the name
   * @param domain
   *          the domain
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

  /**
   * Save cookies to database.
   */
  public void saveCookies() {
    Helper.delete(W.create(), _C.class);

    // clear(new Date(System.currentTimeMillis()));
    List<Cookie> l1 = getCookies();
    if (!X.isEmpty(l1)) {
      for (Cookie c : l1) {
        V v = V.create(X.ID, UID.id(c.getName(), c.getDomain(), c.getPath()));
        v.set("name", c.getName()).set("value", c.getValue()).set("path", c.getPath()).set("domain", c.getDomain())
            .set("expired", c.getExpiryDate().getTime());
        Helper.insert(v, _C.class);
      }
    }
  }

  /**
   * Load cookies from databases.
   */
  public void loadCookies() {
    clearCookies();
    int s = 0;
    W q = W.create();
    Beans<_C> bs = Helper.load(q, s, 10, _C.class);

    while (bs != null && bs.getList() != null && bs.getList().size() > 0) {
      for (_C c : bs.getList()) {
        addCookie(c.name, c.value, c.domain, c.path, new Date(c.expired));
      }
      s += bs.getList().size();
      bs = Helper.load(q, s, 10, _C.class);
    }
  }

  @Table(name = "gi_httpcookie")
  private static class _C extends Bean {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Column(name = "name")
    String                    name;

    @Column(name = "value")
    String                    value;

    @Column(name = "domain")
    String                    domain;

    @Column(name = "path")
    String                    path;

    @Column(name = "expired")
    String                    expired;

  }

}
