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
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.giiwa.core.bean.X;
import org.giiwa.core.json.JSON;

/**
 * http utils
 * 
 * @author joe
 * 
 */
@SuppressWarnings("deprecation")
public final class Http {

  static Log                 log     = LogFactory.getLog(Http.class);
  static int                 TIMEOUT = 10 * 1000;

  public static final String UA      = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36";

  /**
   * GET response from a url.
   *
   * @param url
   *          the url
   * @return Response
   */
  public static Response get(String url) {
    return get(url, null);
  }

  /**
   * ping the url, throw exception if occur error
   * 
   * @param url
   *          the url
   * @return int of response status
   * @throws Exception
   *           throw exception when failed
   */
  public static int ping(String url) throws Exception {

    URL u = new URL(url);
    HttpURLConnection c = (HttpURLConnection) u.openConnection();
    c.connect();
    int code = c.getResponseCode();
    log.debug("ping=" + url + ", response.code=" + code);
    c.disconnect();
    return code;
  }

  /**
   * POST response from a url.
   *
   * @param url
   *          the url
   * @param params
   *          the params
   * @return Response
   */
  public static Response post(String url, JSON params) {
    return post(url, "application/x-javascript; charset=UTF8", null, params);
  }

  /**
   * GET method.
   *
   * @param url
   *          the url
   * @param headers
   *          the headers
   * @return Response
   */
  public static Response get(String url, JSON headers) {

    log.debug("url=\"" + url + "\"");

    String[] ss = url.split(" ");
    url = ss[ss.length - 1];

    Response r = new Response();
    DefaultHttpClient client = getClient(url);

    if (client != null) {
      HttpGet get = null;

      try {
        get = new HttpGet(url);

        if (headers != null && headers.size() > 0) {
          for (String s : headers.keySet()) {
            get.addHeader(s, headers.getString(s));
          }
        }
        if (!get.containsHeader("User-Agent")) {
          get.addHeader("User-Agent", UA);
        }

        log.debug("get url=" + url);

        HttpResponse resp = client.execute(get);

        r.status = resp.getStatusLine().getStatusCode();
        r.body = getContext(resp);
        r.headers = resp.getAllHeaders();

      } catch (Exception e) {
        log.error("\"" + url + "\"", e);
      } finally {
        if (get != null)
          get.abort();
      }
    }

    return r;
  }

  /**
   * download the file in the url to f
   * 
   * @param url
   *          the file url
   * @param localfile
   *          the destination file
   * @return the length
   */
  public static int download(String url, File localfile) {
    return download(url, null, localfile);
  }

  /**
   * download the remote url to local file with the header
   * 
   * @param url
   *          the remote resource url
   * @param header
   *          the header
   * @param localfile
   *          the localfile
   * @return the length of bytes
   */
  public static int download(String url, JSON header, File localfile) {

    log.debug("url=\"" + url + "\"");

    String[] ss = url.split(" ");
    url = ss[ss.length - 1];

    DefaultHttpClient client = getClient(url);

    if (client != null) {
      HttpGet get = null;

      try {
        get = new HttpGet(url);

        get.addHeader("User-Agent", UA);
        if (header != null && header.size() > 0) {
          for (String name : header.keySet()) {
            get.addHeader(name, header.getString(name));
          }
        }

        log.debug("get url=" + url);

        HttpResponse resp = client.execute(get);
        Header[] hh = resp.getAllHeaders();
        if (hh != null) {
          for (Header h : hh) {
            System.out.println(h.getName() + ":" + h.getValue());
          }
        }
        int status = resp.getStatusLine().getStatusCode();
        System.out.println("status:" + status);

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
        if (get != null)
          get.abort();
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
   * @return Response
   */
  public static Response post(String url, String contenttype, JSON headers, JSON params) {
    return post(url, contenttype, headers, params, null);
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
   * @return Response
   */
  public static Response post(String url, String contenttype, JSON headers, JSON body, JSON attachments) {

    Response r = new Response();

    DefaultHttpClient client = getClient(url);
    log.debug("url=" + url);

    if (client != null) {
      HttpPost post = new HttpPost(url);
      try {

        if (headers != null && headers.size() > 0) {
          log.debug("header: " + headers);
          for (String s : headers.keySet()) {
            post.addHeader(s, headers.getString(s));
          }
        }

        if (!post.containsHeader("User-Agent")) {
          post.addHeader("User-Agent", UA);
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
            post.setEntity(new UrlEncodedFormEntity(paramList, HTTP.UTF_8));
          }
        } else {
          MultipartEntity entity = new MultipartEntity();
          for (String f : attachments.keySet()) {
            Object o = attachments.get(f);
            if (o instanceof File) {
              FileBody fileBody = new FileBody((File) o);
              entity.addPart(f, fileBody);
            }
          }

          if (body != null && body.size() > 0) {
            for (String s : body.keySet()) {
              StringBody stringBody = new StringBody(body.getString(s));
              entity.addPart(s, stringBody);
            }
          }
          post.setEntity(entity);
        }

        HttpResponse resp = client.execute(post);
        r.status = resp.getStatusLine().getStatusCode();
        r.body = getContext(resp);

        log.debug("got: status=" + r.status + ", body=" + r.body);

      } catch (Exception e) {
        log.error(url, e);
      } finally {
        post.abort();
      }

    }

    return r;
  }

  private static DefaultHttpClient getClient(String url) {

    DefaultHttpClient client = new DefaultHttpClient();

    if (url.toLowerCase().startsWith("https://")) {
      try {
        SSLContext ctx = SSLContext.getInstance("TLS");
        X509TrustManager tm = new X509TrustManager() {

          public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
            // TODO Auto-generated method stub

          }

          public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
            // TODO Auto-generated method stub

          }

          public X509Certificate[] getAcceptedIssuers() {
            // TODO Auto-generated method stub
            return null;
          }
        };
        ctx.init(null, new TrustManager[] { tm }, null);
        SSLSocketFactory ssf = new SSLSocketFactory(ctx);

        ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        ClientConnectionManager ccm = client.getConnectionManager();
        SchemeRegistry sr = ccm.getSchemeRegistry();
        sr.register(new Scheme("https", ssf, 443));
        HttpParams params = client.getParams();
        HttpConnectionParams.setConnectionTimeout(params, TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, TIMEOUT);

        client = new DefaultHttpClient(ccm, params);

        client.setKeepAliveStrategy(keepAliveStrat);

      } catch (Exception e) {
        log.error("\"" + url + "\"", e);
      }
    }

    return client;
  }

  private static ConnectionKeepAliveStrategy keepAliveStrat = new DefaultConnectionKeepAliveStrategy() {
    @Override
    public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
      long keepAlive = super.getKeepAliveDuration(response, context);
      if (keepAlive == -1) {
        keepAlive = 10000;
      }
      return keepAlive;
    }
  };

  private static String getContext(HttpResponse response) {
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

  private static String _getContext(HttpEntity entity, String charset) {
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
  public static class Response {
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

  public static void main(String[] args) {
    String url = "http://www.giiwa.org/repo/ct13zbxq3wgnl/giiwa-1.2-1611111820.zip";
    File f = new File("/Users/wujun/d/temp/repo.zip");
    JSON head = JSON.create();
    head.put("Range", "bytes=0-1032");
    int len = Http.download(url, head, f);
    System.out.println("repo, done, len=" + len);

    url = "http://www.giiwa.org/giiwa-1.2-1611111820.zip";
    File f1 = new File("/Users/wujun/d/temp/stat.zip");
    head = JSON.create();
    head.put("Range", "bytes=0-1032");
    len = Http.download(url, head, f1);

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
      e.printStackTrace();
    }
  }

}
